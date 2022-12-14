/********************************************************************************/
/*                                                                              */
/*              CatprogProgram.java                                             */
/*                                                                              */
/*      Basic implementation of a program                                       */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.catre.catprog;

import edu.brown.cs.catre.catre.CatreSubSavableBase;
import edu.brown.cs.catre.catre.CatreTriggerContext;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreWorld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import edu.brown.cs.catre.catre.CatreAction;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionException;
import edu.brown.cs.catre.catre.CatreConditionListener;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreException;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreRule;
import edu.brown.cs.catre.catre.CatreStore;

class CatprogProgram extends CatreSubSavableBase implements CatreProgram, CatprogConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private SortedSet<CatreRule>	rule_list;
private CatreUniverse		for_universe;
private Set<CatreCondition>	active_conditions;
private Map<CatreCondition,RuleConditionHandler> cond_handlers;
private Map<CatreWorld,Updater> active_updates;
private Map<String,CatreWorld>	known_worlds;
private boolean                 is_valid;
private Map<String,CatreCondition> known_conditions;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatprogProgram(CatreUniverse uu)
{
   super("PROG_");
   
   for_universe = uu;
   rule_list = new ConcurrentSkipListSet<CatreRule>(new RuleComparator());
   active_conditions = new HashSet<CatreCondition>();
   active_updates = new HashMap<CatreWorld,Updater>();
   known_worlds = new HashMap<String,CatreWorld>();
   CatreWorld cw = for_universe.getCurrentWorld();
   known_worlds.put(cw.getUID(),cw);
   known_conditions = new WeakHashMap<>();
   cond_handlers = new WeakHashMap<>();
   is_valid = true;
}


public CatprogProgram(CatreUniverse uu,CatreStore cs,Map<String,Object> map)
{
   this(uu);
   
   fromJson(cs,map);
   
   updateConditions();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public List<CatreRule> getRules()
{
   return new ArrayList<CatreRule>(rule_list);
}


@Override public CatreRule findRule(String id)
{
   if (id == null) return null;
   
   for (CatreRule ur : rule_list) {
      if (ur.getDataUID().equals(id) || ur.getName().equals(id)) 
         return ur;
    }
   return null;
}


@Override public synchronized void addRule(CatreRule ur)
{
   rule_list.add(ur);
   updateConditions();
}


@Override public synchronized void removeRule(CatreRule ur)
{
   rule_list.remove(ur);
   updateConditions();
}


@Override public CatreUniverse getUniverse()
{
   return for_universe;
}



/********************************************************************************/
/*                                                                              */
/*      Factory methods                                                         */
/*                                                                              */
/********************************************************************************/

@Override public CatreCondition createParameterCondition(CatreDevice device,
      CatreParameter param,Object value,boolean istrigger)
{
   CatreCondition cc = new CatprogConditionParameter(this,device,param,value,istrigger);
        
   return cc;
}


@Override public CatreCondition createCondition(CatreStore cs,Map<String,Object> map)
{
   String uid = getSavedString(map,"UID",null);
   if (uid != null) {
      CatreCondition cc = known_conditions.get(uid);
      if (cc != null) return cc;
    }
   
   CatreCondition cc = null;
   try {
      String typ = getSavedString(map,"TYPE","");
      switch (typ) {
         case "CalendarEvent" :
            cc = new CatprogConditionCalendarEvent(this,cs,map);
            break;
         case "Duration" :
            cc = new CatprogConditionDuration(this,cs,map);
            break;
         case "And" :
            cc = new CatprogConditionLogical.And(this,cs,map);
            break;
         case "Or" :
            cc = new CatprogConditionLogical.Or(this,cs,map);
            break;
         case "Parameter" :
            cc = new CatprogConditionParameter(this,cs,map);
            break;
         case "Range" :
            cc = new CatprogConditionRange(this,cs,map);
            break;
         case "Time" :
            cc = new CatprogConditionTime(this,cs,map);
            break;
         case "TriggerTime" :
            cc = new CatprogConditionTriggerTime(this,cs,map);
            break;
       }
    }
   catch (CatreConditionException e) {
      CatreLog.logE("CATPROG","Problem creating condition",e);
    }
   
   if (cc != null) {
      uid = cc.getConditionUID();
      known_conditions.put(uid,cc);
    }
   
   return cc;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   rslt.put("RULES",getSubObjectArrayToSave(rule_list));
   return rslt;
}



@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   List<CatreRule> rls = new ArrayList<>();
   rls = getSavedSubobjectList(cs,map,"RULES",
         this::createRule,rls);
   rule_list.clear();
   rule_list.addAll(rls);
}


/********************************************************************************/
/*										*/
/*	Maintain active conditions						*/
/*										*/
/********************************************************************************/

private void updateConditions()
{
   Set<CatreCondition> del = new HashSet<CatreCondition>(active_conditions);
   
   for (CatreRule ur : rule_list) {
      CatreCondition uc = ur.getCondition();
      del.remove(uc);
      if (!active_conditions.contains(uc)) {
	 active_conditions.add(uc);
         RuleConditionHandler rch = new RuleConditionHandler(uc);
         cond_handlers.put(uc,rch);
	 uc.addConditionHandler(rch);
       }
    }
   
   for (CatreCondition uc : del) {
      RuleConditionHandler rch = cond_handlers.get(uc);
      if (rch != null) uc.removeConditionHandler(rch);
      active_conditions.remove(uc);
    }
}



private void conditionChange(CatreWorld w)
{
   conditionChange(w,null,null);
}


private void conditionChange(CatreWorld w,CatreCondition c,CatrePropertySet ps)
{
   w.updateLock();
   try {
      if (c != null) w.addTrigger(c,ps);
      Updater upd = active_updates.get(w);
      if (upd != null) {
	 upd.runAgain();
       }
      else {
	 upd = new Updater(w);
	 active_updates.put(w,upd);
         for_universe.getCatre().submit(upd);
       }
    }
   finally {
      w.updateUnlock();
    }
}



private class Updater implements Runnable {
   
   private CatreWorld for_world;
   private boolean run_again;
   
   Updater(CatreWorld w) {
      for_world = w;
      run_again = true;
    }
   
   void runAgain() {
      for_world.updateLock();
      try {
         run_again = true;
       }
      finally {
         for_world.updateUnlock();
       }
    }
   
   @Override public void run() {
      for_world.updateLock();
      try {
         run_again = false;
       }
      finally {
         for_world.updateUnlock();
       }
      for ( ; ; ) {
         CatreTriggerContext ctx = null;
         for_world.updateLock();
         try {
            ctx = for_world.waitForUpdate();
            run_again = false;
          }
         finally {
            for_world.updateUnlock();
          }
         
         runOnce(for_world,ctx);
         
         for_world.updateLock();
         try {
            if (!run_again) {
               active_updates.remove(for_world);
               resetTriggers();
               break;
             }
          }
         finally {
            for_world.updateUnlock();
          }
       }
      
    }

}	// end of inner class Updater




private class RuleConditionHandler implements CatreConditionListener {
   
   private CatreCondition for_condition;
   
   RuleConditionHandler(CatreCondition cc) {
      for_condition = cc;
    }
   
   @Override public void conditionOn(CatreWorld w,CatrePropertySet p) {
      conditionChange(w);
    }
   
   @Override public void conditionOff(CatreWorld w) {
      conditionChange(w);
    }
   
   @Override public void conditionError(CatreWorld w,Throwable cause) { }
   
   @Override public void conditionTrigger(CatreWorld w,
         CatrePropertySet p) {
      if (p == null) p = for_universe.createPropertySet();
      conditionChange(w,for_condition,p);
    }

}	// end of inner class RuleConditionHandler




/********************************************************************************/
/*										*/
/*	Program run methods							*/
/*										*/
/********************************************************************************/

@Override public synchronized boolean runOnce(CatreWorld w,CatreTriggerContext ctx)
{
   boolean rslt = false;
   if (!is_valid) return false;
   
   Set<CatreDevice> entities = new HashSet<CatreDevice>();
   if (w == null) w = for_universe.getCurrentWorld();
   
   Collection<CatreRule> rules = new ArrayList<CatreRule>(rule_list);
   
   CatreLog.logI("CATPROG","CHECK RULES at " + new Date());
   
   for (CatreRule r : rules) {
      Set<CatreDevice> rents = r.getDevices();
      if (containsAny(entities,rents)) continue;
      try {
	 if (startRule(r,w,ctx)) {
	    rslt = true;
	    entities.addAll(rents);
	  }
       }
      catch (CatreException e) {
	 CatreLog.logE("CATPROG","Problem switch rule " + r.getName(),e);
       }
    }
   
   return rslt;
}



private boolean startRule(CatreRule r,CatreWorld w,CatreTriggerContext ctx) 
        throws CatreException
{
   return r.apply(w,ctx);
}


private void resetTriggers()
{
   // reset any triggers after rules run completely
}





private boolean containsAny(Set<CatreDevice> s1,Set<CatreDevice> s2)
{
   for (CatreDevice u2 : s2) {
      if (s1.contains(u2)) return true;
    }
   return false;
}




/********************************************************************************/
/*										*/
/*	Rule priority comparator						*/
/*										*/
/********************************************************************************/

private static class RuleComparator implements Comparator<CatreRule> {
   
   @Override public int compare(CatreRule r1,CatreRule r2) {
      double v = r1.getPriority() - r2.getPriority();
      if (v > 0) return -1;
      if (v < 0) return 1;
      if (r1.getPriority() >= 100) {
         long t1 = r1.getCreationTime() - r2.getCreationTime();
         if (t1 > 0) return -1;
         if (t1 < 0) return 1;
       }
      int v1 = r1.getName().compareTo(r2.getName());
      if (v1 != 0) return v1;
      return r1.getDataUID().compareTo(r2.getDataUID());
    }

}	// end of inner class RuleComparator



/********************************************************************************/
/*										*/
/*	Input methods								*/
/*										*/
/********************************************************************************/

@Override public CatreRule createRule(CatreStore cs,Map<String,Object> map)
{
// String id = IvyXml.getAttrString(xml,"ID");
// if (id != null) {
//    for (CatreRule ur : getRules()) {
// 	 if (ur.getUID().equals(id)) return ur;
//     }
//  }
// 
// if (id == null || IvyXml.getAttrString(xml,"CLASS") == null) {
//    return new CatprogRule(this,xml);
//  }
// 
// return (CatreRule) loadXmlElement(xml);
   
   return null;
}


CatreAction createAction(CatreStore cs,Map<String,Object> map)
{
   return null;
}




}       // end of class CatprogProgram




/* end of CatprogProgram.java */

