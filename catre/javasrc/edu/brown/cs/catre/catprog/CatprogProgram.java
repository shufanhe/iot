/********************************************************************************/
/*										*/
/*		CatprogProgram.java						*/
/*										*/
/*	Basic implementation of a program					*/
/*										*/
/********************************************************************************/
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/




package edu.brown.cs.catre.catprog;

import edu.brown.cs.catre.catre.CatreSubSavableBase;
import edu.brown.cs.catre.catre.CatreTriggerContext;
import edu.brown.cs.catre.catre.CatreUniverse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
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
private Updater 		active_updates;
private boolean 		is_valid;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatprogProgram(CatreUniverse uu)
{
   super("PROG_");

   for_universe = uu;
   rule_list = new ConcurrentSkipListSet<>(new RuleComparator());
   active_conditions = new HashSet<>();
   active_updates = null;
   cond_handlers = new WeakHashMap<>();
   is_valid = true;
}


CatprogProgram(CatreUniverse uu,CatreStore cs,Map<String,Object> map)
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
   return new ArrayList<>(rule_list);
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
   CatreRule oldcr = findRule(ur.getDataUID());
   if (oldcr != null) rule_list.remove(oldcr);
	
   rule_list.add(ur);
   updateConditions();

   CatreLog.logD("CATPROG","Add rule " + ur.toJson());
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
/*										*/
/*	Factory methods 							*/
/*										*/
/********************************************************************************/

@Override public CatreCondition createCondition(CatreStore cs,Map<String,Object> map)
{
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
	 case "Latch" :
	    cc = new CatprogConditionLatch(this,cs,map);
	    break;
	 case "Debounce" :
	    cc = new CatprogConditionDebounce(this,cs,map);
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
	 case "Disabled" :
	    cc = new CatprogConditionDisabled(this,cs,map);
	    break;
       }
    }
   catch (CatreConditionException e) {
      CatreLog.logE("CATPROG","Problem creating condition",e);
    }

   if (cc != null) cc.activate();

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
   Set<CatreCondition> del = new HashSet<>(active_conditions);

   for (CatreRule ur : rule_list) {
      CatreCondition uc = ur.getCondition();
      del.remove(uc);
      if (!active_conditions.contains(uc)) {
	 active_conditions.add(uc);
	 RuleConditionHandler rch = new RuleConditionHandler();
	 cond_handlers.put(uc,rch);
	 uc.addConditionHandler(rch);
	 uc.noteUsed(true);
       }
    }

   for (CatreCondition uc : del) {
      RuleConditionHandler rch = cond_handlers.get(uc);
      if (rch != null) uc.removeConditionHandler(rch);
      active_conditions.remove(uc);
      uc.noteUsed(false);
    }
}



private void conditionChange(CatreCondition c)
{
   conditionChange(c,false,null);
}


private void conditionChange(CatreCondition c,boolean istrig,CatrePropertySet ps)
{
   for_universe.updateLock();
   try {
      if (istrig) for_universe.addTrigger(c,ps);
      Updater upd = active_updates;
      if (upd != null) {
	 upd.runAgain();
       }
      else {
	 upd = new Updater();
	 active_updates = upd;
	 for_universe.getCatre().submit(upd);
       }
    }
   finally {
      for_universe.updateUnlock();
    }
}



private class Updater implements Runnable {

   private boolean run_again;
   private long last_request;

   Updater() {
      run_again = true;
      last_request = 0;
    }

   void runAgain() {
      for_universe.updateLock();
      try {
	 run_again = true;
         last_request = System.currentTimeMillis();
       }
      finally {
	 for_universe.updateUnlock();
       }
    }

   @Override public void run() {
      for_universe.updateLock();
      try {
         for ( ; ; ) {
            long now = System.currentTimeMillis();
            if (now - last_request > RUN_DELAY) break;
            try {
               wait(now-last_request);
             }
            catch (InterruptedException e) { }
          }
         run_again = false;
       }
      finally {
         for_universe.updateUnlock();
       }
      
      for ( ; ; ) {
         CatreTriggerContext ctx = null;
         for_universe.updateLock();
         try {
            ctx = for_universe.waitForUpdate();
            run_again = false;
          }
         finally {
            for_universe.updateUnlock();
          }
        
         runOnce(ctx);
        
         for_universe.updateLock();
         try {
            if (!run_again) {
               active_updates = null;
               resetTriggers();
               break;
             }
          }
         finally {
            for_universe.updateUnlock();
          }
       }
   
    }

}	// end of inner class Updater




private class RuleConditionHandler implements CatreConditionListener {

   @Override public void conditionOn(CatreCondition cc,CatrePropertySet p) {
      conditionChange(cc);
    }

   @Override public void conditionOff(CatreCondition cc) {
      conditionChange(cc);
    }

   @Override public void conditionTrigger(CatreCondition cc,CatrePropertySet p) {
      if (p == null) p = for_universe.createPropertySet();
      conditionChange(cc,true,p);
    }

}	// end of inner class RuleConditionHandler




/********************************************************************************/
/*										*/
/*	Program run methods							*/
/*										*/
/********************************************************************************/

@Override public synchronized boolean runOnce(CatreTriggerContext ctx)
{
   boolean rslt = false;
   if (!is_valid) return false;

   Set<CatreDevice> entities = new HashSet<>();

   Collection<CatreRule> rules = new ArrayList<>(rule_list);

   CatreLog.logI("CATPROG","CHECK RULES at " + new Date());

   for (CatreRule r : rules) {
      Set<CatreDevice> rents = r.getTargetDevices();
      if (containsAny(entities,rents)) continue;
      try {
	 if (startRule(r,ctx)) {
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



private boolean startRule(CatreRule r,CatreTriggerContext ctx)
	throws CatreException
{
   return r.apply(ctx);
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
      // what is this for?
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
   try {
      return new CatprogRule(this,cs,map);
    }
   catch (Throwable t) {
      CatreLog.logE("Problem with rule definition",t);
      return null;
    }
}


CatreAction createAction(CatreStore cs,Map<String,Object> map)
{
   return new CatprogAction(this,cs,map);
}




}	// end of class CatprogProgram




/* end of CatprogProgram.java */

