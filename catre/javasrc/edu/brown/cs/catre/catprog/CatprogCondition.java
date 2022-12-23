/********************************************************************************/
/*                                                                              */
/*              CatprogCondition.java                                           */
/*                                                                              */
/*      Abstract condition implementation                                       */
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

import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionException;
import edu.brown.cs.catre.catre.CatreConditionListener;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDescribableBase;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.catre.catre.CatreWorld;
import edu.brown.cs.ivy.swing.SwingEventListenerList;

abstract class CatprogCondition extends CatreDescribableBase implements CatreCondition, CatprogConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<CatreWorld,CondState>	state_map;
private SwingEventListenerList<CatreConditionListener> condition_handlers;
protected CatreProgram	for_program;
private String          condition_uid;
private boolean         is_valid;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatprogCondition(CatreProgram pgm,String uid)
{
   super("COND_");
   for_program = pgm;
   state_map = new HashMap<>();
   condition_handlers = new SwingEventListenerList<>(CatreConditionListener.class);
   condition_uid = CatreUtil.shortHash(uid);
   is_valid = true;
}


protected CatprogCondition(CatreProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super("COND_");
   for_program = pgm;
   state_map = new HashMap<>();
   condition_uid = null;
   is_valid = false;
   
   condition_handlers = new SwingEventListenerList<>(CatreConditionListener.class);
   
   fromJson(cs,map);
}


protected void setUID(String uid)    
{
   if (condition_uid == null) {
      condition_uid = uid;
    }
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public final String getConditionUID()
{
   return condition_uid;
}


@Override public final CatreUniverse getUniverse()		{ return for_program.getUniverse(); }

CatreController getCatre() 
{ 
   return getUniverse().getCatre();
}


@Override public void addConditionHandler(CatreConditionListener hdlr)
{
   condition_handlers.add(hdlr);
}

@Override public void removeConditionHandler(CatreConditionListener hdlr)
{
   condition_handlers.remove(hdlr);
}


@Override public boolean isTrigger()				{ return false; }

@Override public boolean isValid()                              { return is_valid; }


protected void setValid(boolean fg)
{
   if (fg) {
      is_valid = true;
      localStartCondition();
    }
   else {
      is_valid = false;
      localStopCondition();
    }
}


protected void localStartCondition()                            { }

protected void localStopCondition()                             { }


/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public final CatrePropertySet getCurrentStatus(CatreWorld world)
        throws CatreConditionException
{
   setTime(world);
   
   CondState cs = getState(world);
   
   CatreConditionException cex = cs.getError();
   if (cex != null) throw cex;
   
   return cs.getProperties();
}


@Override public abstract void setTime(CatreWorld w);



private synchronized CondState getState(CatreWorld w)
{
   CondState cs = state_map.get(w);
   if (cs == null) {
      cs = new CondState();
      state_map.put(w,cs);
    }
   return cs;
}


/********************************************************************************/
/*										*/
/*	Trigger methods 							*/
/*										*/
/********************************************************************************/

protected void fireOn(CatreWorld w,CatrePropertySet input)
{
   if (input == null) input = getUniverse().createPropertySet();
   
   w.startUpdate();
   try {
      CondState cs = getState(w);
      if (!cs.setOn(input)) return;
      
      for (CatreConditionListener ch : condition_handlers) {
	 try {
	    ch.conditionOn(w,input);
	  }
	 catch (Throwable t) {
	    CatreLog.logE("CATMODEL","Problem with condition handler",t);
	  }
       }
    }
   finally {
      w.endUpdate();
    }
}


protected void fireTrigger(CatreWorld w,CatrePropertySet input)
{
   if (input == null) input = getUniverse().createPropertySet();
   
   w.startUpdate();
   try {
      for (CatreConditionListener ch : condition_handlers) {
	 try {
	    ch.conditionTrigger(w,input);
	  }
	 catch (Throwable t) {
	    CatreLog.logE("CATMODEL","Problem with condition handler",t);
	  }
       }
    }
   finally {
      w.endUpdate();
    }
}

protected void fireOff(CatreWorld w)
{
   w.startUpdate();
   try {
      CondState cs = getState(w);
      if (!cs.setOff()) return;
      
      for(CatreConditionListener ch : condition_handlers) {
	 try {
	    ch.conditionOff(w);
	  }
	 catch (Throwable t) {
	    CatreLog.logE("CATMODEL","Problem with condition handler",t);
	  }
       }
    }
   finally {
      w.endUpdate();
    }
}



protected void fireError(CatreWorld w,Throwable cause)
{
   w.startUpdate();
   try {
      CondState cs = getState(w);
      if (!cs.setError(cause)) return;
      
      for (CatreConditionListener ch : condition_handlers) {
	 try {
	    ch.conditionError(w,cause);
	  }
	 catch (Throwable t) {
	    CatreLog.logE("CATMODEL","Problem with condition handler",t);
	  }
       }
    }
   finally {
      w.endUpdate();
    }
}


protected void fireValidated()
{
   boolean valid = isValid();
   
   for (CatreConditionListener ch : condition_handlers) {
      try {
         ch.conditionValidated(valid);
       }
      catch (Throwable t) {
         CatreLog.logE("CATMODEL","Problem with condition handler",t);
       }
    }
}














/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("UID",condition_uid);
   
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   condition_uid = getSavedString(map,"UID",condition_uid);
}



/********************************************************************************/
/*										*/
/*	State Tracking								*/
/*										*/
/********************************************************************************/

private class CondState {
   
   private CatrePropertySet on_parameters;
   private CatreConditionException error_condition;
   
   CondState() {
      on_parameters = null;
      error_condition = null;
    }
   
   boolean setOn(CatrePropertySet ps) {
      if (on_parameters != null && on_parameters.equals(ps)) return false;
      error_condition = null;
      on_parameters = getUniverse().createPropertySet();
      on_parameters.putAll(ps);
      return true;
    }
   
   boolean setError(Throwable t) {
      if (error_condition != null && error_condition.equals(t)) return false;
      if (t instanceof CatreConditionException)
         error_condition = (CatreConditionException) t;
      else
         error_condition = new CatreConditionException("Condition aborted",t);
      on_parameters = null;
      return true;
    }
   
   boolean setOff() {
      if (error_condition == null && on_parameters == null) return false;
      error_condition = null;
      on_parameters = null;
      return true;
    }
   
   
   CatrePropertySet getProperties()		{ return on_parameters; }
   
   CatreConditionException getError()		{ return error_condition; }
   
}	// end of inner class CondState




}       // end of class CatprogCondition




/* end of CatprogCondition.java */

