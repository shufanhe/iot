/********************************************************************************/
/*                                                                              */
/*              CatmodelCondition.java                                          */
/*                                                                              */
/*      description of class                                                    */
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

import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.catre.catre.CatreWorld;
import edu.brown.cs.ivy.swing.SwingEventListenerList;

import java.util.HashMap;
import java.util.Map;


import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionException;
import edu.brown.cs.catre.catre.CatreConditionHandler;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatrePropertySet;


abstract public class CatmodelCondition implements CatreCondition, CatmodelConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

enum State { OFF, ON, ERROR };

private Map<CatreWorld,CondState>	state_map;
private SwingEventListenerList<CatreConditionHandler> condition_handlers;
private String			condition_label;
protected CatmodelUniverse	for_universe;
private String                  data_uid;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatmodelCondition(CatreUniverse uu)
{
   for_universe = (CatmodelUniverse) uu;
   state_map = new HashMap<CatreWorld,CondState>();
   condition_handlers = new SwingEventListenerList<CatreConditionHandler>(
	 CatreConditionHandler.class);
   condition_label = null;
   data_uid = "COND_" + CatreUtil.randomString(24);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override abstract public String getName();
@Override abstract public String getDescription();

@Override public String getDataUID()                    { return data_uid; }

@Override public CatreUniverse getUniverse()		{ return for_universe; }

CatreController getCatre() 
{ 
   return ((CatmodelUniverse) for_universe).getCatre();
}

@Override public String getLabel()
{
   return condition_label;
}

@Override public void setLabel(String s)
{
   condition_label = s;
}


@Override public CatreParameterSet getDefaultParameters()	{ return null; }

@Override public CatreParameterSet getParameters()		{ return null; }



@Override public void addConditionHandler(CatreConditionHandler hdlr)
{
   condition_handlers.add(hdlr);
}

@Override public void removeConditionHandler(CatreConditionHandler hdlr)
{
   condition_handlers.remove(hdlr);
}


@Override public boolean isTrigger()				{ return false; }



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

public void fireOn(CatreWorld w,CatrePropertySet input)
{
   if (input == null) input = new CatmodelPropertySet();
   
   w.startUpdate();
   try {
      CondState cs = getState(w);
      if (!cs.setOn(input)) return;
      
      for (CatreConditionHandler ch : condition_handlers) {
	 try {
	    ch.conditionOn(w,this,input);
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


public void fireTrigger(CatreWorld w,CatrePropertySet input)
{
   if (input == null) input = new CatmodelPropertySet();
   
   w.startUpdate();
   try {
      for (CatreConditionHandler ch : condition_handlers) {
	 try {
	    ch.conditionTrigger(w,this,input);
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

public void fireOff(CatreWorld w)
{
   w.startUpdate();
   try {
      CondState cs = getState(w);
      if (!cs.setOff()) return;
      
      for(CatreConditionHandler ch : condition_handlers) {
	 try {
	    ch.conditionOff(w,this);
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



public void fireError(CatreWorld w,Throwable cause)
{
   w.startUpdate();
   try {
      CondState cs = getState(w);
      if (!cs.setError(cause)) return;
      
      for (CatreConditionHandler ch : condition_handlers) {
	 try {
	    ch.conditionError(w,this,cause);
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



/********************************************************************************/
/*										*/
/*	   conflict checking							*/
/*										*/
/********************************************************************************/

@Override public boolean canOverlap(CatreCondition uc)
{
   if (uc == null) return true;
   
   CatmodelCondition bc = (CatmodelCondition) uc;
   
   return bc.checkOverlapConditions(this);
}


protected boolean checkOverlapConditions(CatmodelCondition uc)
{
   return uc.isConsistentWith(this);
}



protected abstract boolean isConsistentWith(CatreCondition uc);


@Override public abstract void addImpliedProperties(CatrePropertySet ups);




/********************************************************************************/
/*										*/
/*	State Tracking								*/
/*										*/
/********************************************************************************/

private static class CondState {
   
   private CatmodelPropertySet on_parameters;
   private CatreConditionException   error_condition;
   
   CondState() {
      on_parameters = null;
      error_condition = null;
    }
   
   boolean setOn(CatrePropertySet ps) {
      if (on_parameters != null && on_parameters.equals(ps)) return false;
      error_condition = null;
      on_parameters = new CatmodelPropertySet(ps);
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



}       // end of class CatmodelCondition




/* end of CatmodelCondition.java */

