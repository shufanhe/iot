/********************************************************************************/
/*                                                                              */
/*              CatmodelConditionLogical.java                                   */
/*                                                                              */
/*      Logical combinatiion of conditions                                      */
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



package edu.brown.cs.catre.catmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionException;
import edu.brown.cs.catre.catre.CatreConditionHandler;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreWorld;

abstract class CatmodelConditionLogical extends CatmodelCondition implements CatreCondition, CatmodelConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/


protected List<CatreCondition>	arg_conditions;
private CondUpdater		cond_updater;
private boolean 		first_time;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatmodelConditionLogical(CatreCondition ... cond) throws CatreConditionException
   {
   super(cond[0].getUniverse());
   
   arg_conditions = new ArrayList<CatreCondition>();
   boolean havetrigger = false;
   
   for (CatreCondition c : cond) {
      if (c.isTrigger()) {
	 if (havetrigger)
	    throw new CatreConditionException("Trigger must be first condition");
	 havetrigger = true;
       }
      arg_conditions.add((CatmodelCondition) c);
    }
   
   first_time = true;
   
   setupTriggers();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public void getSensors(Collection<CatreDevice> rslt)
{
   for (CatreCondition bc : arg_conditions) {
      bc.getSensors(rslt);
    }
}



@Override public boolean isTrigger()
{
   for (CatreCondition bc : arg_conditions) {
      if (bc.isTrigger()) return true;
    }
   
   return false;
}


@Override public boolean isBaseCondition()
{
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



private void setupTriggers()
{
   cond_updater = new CondUpdater();
   for (CatreCondition c : arg_conditions) {
      c.addConditionHandler(cond_updater);
    }
}



private void checkState(CatreWorld w,CatmodelTriggerContext ctx)
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


abstract protected CatrePropertySet recompute(CatreWorld w,CatmodelTriggerContext ctx)
throws CatreConditionException;


private class CondUpdater implements CatreConditionHandler {
   
   private Set<CatreWorld> hold_worlds;
   private Map<CatreWorld,CatmodelTriggerContext> change_worlds;
   
   CondUpdater() {
      hold_worlds = new HashSet<>();
      hold_worlds = new HashSet<>();
    }
   
   synchronized void beginHold(CatreWorld w) {
      hold_worlds.add(w);
      hold_worlds.remove(w);
    }
   
   void endHold(CatreWorld w) {
      CatmodelTriggerContext ctx = null;
      synchronized (this) {
         hold_worlds.remove(w);
         ctx = change_worlds.remove(w);
         if (ctx == null) return;
       }
      checkState(w,ctx);
    }
   
   void update(CatreWorld w,CatmodelTriggerContext ctx) {
      synchronized (this) {
         if (hold_worlds.contains(w)) {
            CatmodelTriggerContext octx = change_worlds.get(w);
            if (octx != null && ctx != null) octx.addContext(ctx);
            else if (octx == null) {
               if (ctx == null) ctx = new CatmodelTriggerContext();
               change_worlds.put(w,ctx);
             }
            return;
          }
       }
      checkState(w,ctx);
    }
   
   @Override public void conditionError(CatreWorld w,CatreCondition c,Throwable t) {
      update(w,null);
    }
   
   @Override public void conditionOff(CatreWorld w,CatreCondition c) {
      update(w,null);
    }
   
   @Override public void conditionOn(CatreWorld w,CatreCondition c,
         CatrePropertySet ps) {
      update(w,null);
    }
   
   @Override public void conditionTrigger(CatreWorld w,CatreCondition c,
         CatrePropertySet ps) {
      CatmodelTriggerContext ctx = new CatmodelTriggerContext(c,ps);
      update(w,ctx);
    }
   
}	// end of inner class CondUpdater





/********************************************************************************/
/*										*/
/*	AND implementation							*/
/*										*/
/********************************************************************************/

static public class And extends CatmodelConditionLogical {
   
   And(CatreCondition ... cond) throws CatreConditionException {
      super(cond);
      int tct = 0;
      for (CatreCondition bc : arg_conditions) {
         if (bc.isTrigger()) ++tct;
       }
      if (tct > 1) throw new CatreConditionException("Can't AND multiple triggers");
    }
   
   @Override public String getName() {
      StringBuffer buf = new StringBuffer();
      for (CatreCondition c : arg_conditions) {
         if (buf.length() > 0) buf.append("&&");
         buf.append(c.getName());
       }
      return buf.toString();
    }
   
   @Override public String getLabel() {
      StringBuffer buf = new StringBuffer();
      for (CatreCondition c : arg_conditions) {
         if (buf.length() > 0) buf.append(" AND ");
         buf.append(c.getLabel());
       }
      return buf.toString();
    }
   
   @Override public String getDescription() {
      StringBuffer buf = new StringBuffer();
      for (CatreCondition c : arg_conditions) {
         if (buf.length() > 0) buf.append("&&");
         buf.append(c.getDescription());
       }
      return buf.toString();
    }
   
   @Override protected	CatrePropertySet recompute(CatreWorld world,CatmodelTriggerContext ctx)
   throws CatreConditionException {
      CatrePropertySet ups = new CatmodelPropertySet();
      for (CatreCondition c : arg_conditions) {
         CatrePropertySet ns = null;
         if (ctx != null) ns = ctx.checkCondition(c);
         if (ns == null) ns = c.getCurrentStatus(world);
         if (ns == null) return null;
         ups.putAll(ns);
       }
      return ups;
    }
   
   @Override protected boolean checkOverlapConditions(CatmodelCondition bc) {
      for (CatreCondition ac : arg_conditions) {
         CatmodelCondition cac = (CatmodelCondition) ac;
         if (!cac.checkOverlapConditions(bc)) return false;
       }
      return true;
    }
   
   @Override public void addImpliedProperties(CatrePropertySet ups)
{
      for (CatreCondition ac : arg_conditions) {
         ac.addImpliedProperties(ups);
       }
    }
   
   @Override protected boolean isConsistentWith(CatreCondition bc) {
      for (CatreCondition ac : arg_conditions) {
         CatmodelCondition cac = (CatmodelCondition) ac;
         if (!cac.isConsistentWith(bc)) return false;
       }
      return true;
    }
   
}	// end of inner class And




/********************************************************************************/
/*										*/
/*	OR implementation							*/
/*										*/
/********************************************************************************/

static public class Or extends CatmodelConditionLogical {
   
   Or(CatreCondition ... cond) throws CatreConditionException {
      super(cond);
      int tct = 0;
      for (CatreCondition bc : arg_conditions) {
         if (bc.isTrigger()) ++tct;
       }
      if (tct != 0 && tct != arg_conditions.size())
         throw new CatreConditionException("OR must be either all triggers or no triggers");
    }
   
   
   @Override public String getName() {
      StringBuffer buf = new StringBuffer();
      for (CatreCondition c : arg_conditions) {
         if (buf.length() > 0) buf.append("||");
         buf.append(c.getName());
       }
      return buf.toString();
    }
   
   @Override public String getLabel() {
      StringBuffer buf = new StringBuffer();
      for (CatreCondition c : arg_conditions) {
         if (buf.length() > 0) buf.append(" OR ");
         buf.append(c.getLabel());
       }
      return buf.toString();
    }
   
   @Override public String getDescription() {
      StringBuffer buf = new StringBuffer();
      for (CatreCondition c : arg_conditions) {
         if (buf.length() > 0) buf.append("||");
         buf.append(c.getDescription());
       }
      return buf.toString();
    }
   
   @Override protected	CatrePropertySet recompute(CatreWorld world,CatmodelTriggerContext ctx)
   throws CatreConditionException {
      CatrePropertySet ups = null;
      for (CatreCondition c : arg_conditions) {
         CatrePropertySet ns = null;
         if (ctx != null) ns = ctx.checkCondition(c);
         if (ns == null) ns = c.getCurrentStatus(world);
         if (ns != null) {
            if (ups == null) ups = new CatmodelPropertySet();
            ups.putAll(ns);
          }
       }
      return ups;
    }
   
   @Override protected boolean checkOverlapConditions(CatmodelCondition bc) {
      for (CatreCondition ac : arg_conditions) {
         CatmodelCondition cac = (CatmodelCondition) ac;
         if (!cac.checkOverlapConditions(bc)) return false;
       }
      return true;
    }
   
   @Override public void addImpliedProperties(CatrePropertySet ups) {
      for (CatreCondition ac : arg_conditions) {
         ac.addImpliedProperties(ups);
       }
    }
   
   @Override protected boolean isConsistentWith(CatreCondition bc) {
      for (CatreCondition ac : arg_conditions) {
         CatmodelCondition cac = (CatmodelCondition) ac;
         if (cac.isConsistentWith(bc)) return true;
       }
      return true;
    }
   
}	// end of inner class Or


}       // end of class CatmodelConditionLogical




/* end of CatmodelConditionLogical.java */

