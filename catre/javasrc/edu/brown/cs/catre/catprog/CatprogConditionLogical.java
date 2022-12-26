/********************************************************************************/
/*                                                                              */
/*              CatprogConditionLogical.java                                    */
/*                                                                              */
/*      Logical (AND/OR) combination of conditions                              */
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionException;
import edu.brown.cs.catre.catre.CatreConditionListener;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTriggerContext;
import edu.brown.cs.catre.catre.CatreWorld;

abstract class CatprogConditionLogical extends CatprogCondition
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/


protected List<CatreCondition>	arg_conditions;
private CondUpdater             cond_updater;
private boolean 		first_time;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatprogConditionLogical(CatreProgram pgm,String name,CatreCondition ... cond) 
        throws CatreConditionException
{
   super(pgm,getUniqueName(name,cond));
   
   arg_conditions = new ArrayList<CatreCondition>();
   boolean havetrigger = false;
   cond_updater = null;
   
   for (CatreCondition c : cond) {
      if (c.isTrigger()) {
	 if (havetrigger)
	    throw new CatreConditionException("Trigger must be first condition");
	 havetrigger = true;
       }
      arg_conditions.add((CatprogCondition) c);
    }
   
   first_time = true;
   
   setupTriggers();
}


protected CatprogConditionLogical(CatreProgram pgm,CatreStore cs,Map<String,Object> map)
throws CatreConditionException
{
   super(pgm,cs,map);
   
   first_time = true;
   
   setupTriggers();
}


private static String getUniqueName(String type,CatreCondition [] conds)
{
   StringBuffer buf = new StringBuffer();
   buf.append(type);
   for (CatreCondition cc : conds) {
      buf.append("_");
      buf.append(cc.getConditionUID());
    }
   return buf.toString();
}


protected void setConditionName(String s1,String s2)
{
   if (getName() != null && !getName().equals("")) return;
   
   StringBuffer buf = new StringBuffer();
   for (CatreCondition c : arg_conditions) {
      if (buf.length() > 0) buf.append(s1);
      buf.append(c.getName());
    }
   setName(buf.toString());
   buf = new StringBuffer();
   for (CatreCondition c : arg_conditions) {
      if (buf.length() > 0) buf.append(" " + s2 + " ");
      buf.append(c.getLabel());
    }
   setLabel(buf.toString());
   buf = new StringBuffer();
   for (CatreCondition c : arg_conditions) {
      if (buf.length() > 0) buf.append(s1);
      buf.append(c.getDescription());
    }
   setDescription(buf.toString());
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public void getDevices(Collection<CatreDevice> rslt)
{
   for (CatreCondition bc : arg_conditions) {
      bc.getDevices(rslt);
    }
}



@Override public boolean isTrigger()
{
   for (CatreCondition bc : arg_conditions) {
      if (bc.isTrigger()) return true;
    }
   
   return false;
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override public void setTime(CatreWorld w)
{
   if (first_time) {
      first_time = false;
      checkState(w,null);
    }
   
   cond_updater.beginHold(w);
   for (CatreCondition c : arg_conditions) {
      if (c != null) c.setTime(w);
    }
   cond_updater.endHold(w);
}



private void setupTriggers() throws CatreConditionException
{
   checkTriggers();
   
   cond_updater = new CondUpdater();
   
   for (CatreCondition c : arg_conditions) {
      CondUpdateListener cul = new CondUpdateListener(c);
      c.addConditionHandler(cul);
    }
}

protected abstract void checkTriggers() throws CatreConditionException;


private void checkState(CatreWorld w,CatreTriggerContext ctx)
{
   try {
      CatrePropertySet ps = recompute(w,ctx);
      CatreLog.logI("CATMODEL","CONDITION " + getLabel() + " " + (ps != null));
      if (ps != null) {
	 if (ps.get("*TRIGGER*") != null) fireTrigger(w,ps);
	 else fireOn(w,ps);
       }
      else fireOff(w);
    }
   catch (Throwable t) {
      fireError(w,t);
    }
}


abstract protected CatrePropertySet recompute(CatreWorld w,CatreTriggerContext ctx)
throws CatreConditionException;



private class CondUpdater {
   
   private Set<CatreWorld> hold_worlds;
   private Map<CatreWorld,CatreTriggerContext> change_worlds;
   
   CondUpdater() {
      hold_worlds = new HashSet<>();
      hold_worlds = new HashSet<>();
    }
   
   synchronized void beginHold(CatreWorld w) {
      hold_worlds.add(w);
      hold_worlds.remove(w);
    }
   
   void endHold(CatreWorld w) {
      CatreTriggerContext ctx = null;
      synchronized (this) {
         hold_worlds.remove(w);
         ctx = change_worlds.remove(w);
         if (ctx == null) return;
       }
      checkState(w,ctx);
    }
   
   void update(CatreWorld w,CatreTriggerContext ctx) {
      synchronized (this) {
         if (hold_worlds.contains(w)) {
            CatreTriggerContext octx = change_worlds.get(w);
            if (octx != null && ctx != null) octx.addContext(ctx);
            else if (octx == null) {
               if (ctx == null) ctx = getUniverse().createTriggerContext();
               change_worlds.put(w,ctx);
             }
            return;
          }
       }
      checkState(w,ctx);
    }

}	// end of inner class CondUpdater



private class CondUpdateListener implements CatreConditionListener {

   private CatreCondition for_condition;
   
   CondUpdateListener(CatreCondition cc) {
      for_condition = cc;
    }
   
   @Override public void conditionError(CatreWorld w,Throwable t) {
      cond_updater.update(w,null);
    }
   
   @Override public void conditionOff(CatreWorld w) {
      cond_updater.update(w,null);
    }
   
   @Override public void conditionOn(CatreWorld w,CatrePropertySet ps) {
      cond_updater.update(w,null);
    }
   
   @Override public void conditionTrigger(CatreWorld w,CatrePropertySet ps) {
      CatreTriggerContext ctx = getUniverse().createTriggerContext();
      ctx.addCondition(for_condition,ps);
      cond_updater.update(w,ctx);
    }
   
}       // end of inner class CondUpdateListener




/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("CONDITIONS",getSubObjectArrayToSave(arg_conditions));
   
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   arg_conditions = getSavedSubobjectList(cs,map,"CONDITIONS",
         for_program::createCondition,arg_conditions);
}



/********************************************************************************/
/*										*/
/*	AND implementation							*/
/*										*/
/********************************************************************************/

static class And extends CatprogConditionLogical {
   
   And(CatreProgram pgm,CatreCondition ... cond) throws CatreConditionException {
      super(pgm,"AND",cond);
      setConditionName("&&","AND");
      checkTriggers();
    }
   
   And(CatreProgram pgm,CatreStore cs,Map<String,Object> map) throws CatreConditionException {
      super(pgm,cs,map);
      setConditionName("&&","AND");
      checkTriggers();
    }
   
   protected void checkTriggers() throws CatreConditionException {
      int tct = 0;
      for (CatreCondition bc : arg_conditions) {
         if (bc.isTrigger()) ++tct;
       }
      if (tct > 1) throw new CatreConditionException("Can't AND multiple triggers");
    }
   
   @Override protected	CatrePropertySet recompute(CatreWorld world,CatreTriggerContext ctx)
   throws CatreConditionException {
      CatrePropertySet ups = getUniverse().createPropertySet();
      for (CatreCondition c : arg_conditions) {
         CatrePropertySet ns = null;
         if (ctx != null) ns = ctx.checkCondition(c);
         if (ns == null) ns = c.getCurrentStatus(world);
         if (ns == null) return null;
         ups.putAll(ns);
       }
      return ups;
    }
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("TYPE","And");
      return rslt;
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map)
   {
      super.fromJson(cs,map);
      setUID(getUniqueName("AND",arg_conditions.toArray(new CatreCondition [0]))); 
   }

}	// end of inner class And




/********************************************************************************/
/*										*/
/*	OR implementation							*/
/*										*/
/********************************************************************************/

static class Or extends CatprogConditionLogical {
   
   Or(CatreProgram pgm,CatreCondition ... cond) throws CatreConditionException {
      super(pgm,"OR",cond);
      setConditionName("||","OR");
      checkTriggers();
    }
   
   Or(CatreProgram pgm,CatreStore cs,Map<String,Object> map) throws CatreConditionException {
      super(pgm,cs,map);
      setConditionName("||","OR");
      checkTriggers();
    }
   
   @Override protected void checkTriggers() throws CatreConditionException {
      int tct = 0;
      for (CatreCondition bc : arg_conditions) {
         if (bc.isTrigger()) ++tct;
       }
      if (tct != 0 && tct != arg_conditions.size())
         throw new CatreConditionException("OR must be either all triggers or no triggers");
    }
   
   @Override protected	CatrePropertySet recompute(CatreWorld world,CatreTriggerContext ctx)
   throws CatreConditionException {
      CatrePropertySet ups = null;
      for (CatreCondition c : arg_conditions) {
         CatrePropertySet ns = null;
         if (ctx != null) ns = ctx.checkCondition(c);
         if (ns == null) ns = c.getCurrentStatus(world);
         if (ns != null) {
            if (ups == null) ups = getUniverse().createPropertySet();
            ups.putAll(ns);
          }
       }
      return ups;
    }
   
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("TYPE","Or");
      return rslt;
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map)
   {
      super.fromJson(cs,map);
      
      setUID(getUniqueName("OR",arg_conditions.toArray(new CatreCondition [0])));
   }
   
   
}	// end of inner class Or


}       // end of class CatprogConditionLogical




/* end of CatprogConditionLogical.java */

