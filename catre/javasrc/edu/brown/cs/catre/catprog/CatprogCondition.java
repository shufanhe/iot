/********************************************************************************/
/*										*/
/*		CatprogCondition.java						*/
/*										*/
/*	Abstract condition implementation					*/
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

import java.util.Collection;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionException;
import edu.brown.cs.catre.catre.CatreConditionListener;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDescribableBase;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.ivy.swing.SwingEventListenerList;

abstract class CatprogCondition extends CatreDescribableBase implements CatreCondition, CatprogConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CondState	cond_state;
private SwingEventListenerList<CatreConditionListener> condition_handlers;
protected CatprogProgram for_program;
private boolean 	is_valid;
private boolean         is_shared;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatprogCondition(CatprogProgram pgm)
{
   super(null);
   for_program = pgm;
   cond_state = new CondState();
   condition_handlers = new SwingEventListenerList<>(CatreConditionListener.class);
   is_valid = true;
   is_shared = false;
}



protected CatprogCondition(CatprogCondition cc)
{
   super(null,cc);
   for_program = cc.for_program;
   cond_state = new CondState();
   condition_handlers = new SwingEventListenerList<>(CatreConditionListener.class);
   is_valid = cc.is_valid;
   is_shared = cc.is_shared;
}


protected CatprogCondition(CatprogProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(null);
   for_program = pgm;
   cond_state = new CondState();
   is_valid = false;

   condition_handlers = new SwingEventListenerList<>(CatreConditionListener.class);

   fromJson(cs,map);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public final CatreUniverse getUniverse()		{ return for_program.getUniverse(); }

CatreController getCatre()
{
   return getUniverse().getCatre();
}

@Override public boolean isTrigger()				{ return false; }

@Override public boolean isValid()				{ return is_valid; }

@Override public boolean isShared()                             { return is_shared; } 


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


protected void localStartCondition()				{ }

protected void localStopCondition()				{ }



/********************************************************************************/
/*										*/
/*	Condition handling							*/
/*										*/
/********************************************************************************/

@Override public void activate()				{ }



@Override public void addConditionHandler(CatreConditionListener hdlr)
{
   condition_handlers.add(hdlr);
}

@Override public void removeConditionHandler(CatreConditionListener hdlr)
{
   condition_handlers.add(hdlr);
}

protected boolean hasConditionHandlers() 
{
   return condition_handlers.getListenerCount() > 0;
}


protected Collection<CatreCondition> getSubconditions() 	      { return null; }


/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public final CatrePropertySet getCurrentStatus()
	throws CatreConditionException
{
   setTime();

   CondState cs = cond_state;

   CatreConditionException cex = cs.getError();
   if (cex != null) throw cex;

   return cs.getProperties();
}


protected void setTime()			{ }


@Override public void noteUsed(boolean fg)	{ }



/********************************************************************************/
/*										*/
/*	Trigger methods 							*/
/*										*/
/********************************************************************************/

protected void fireOn(CatrePropertySet input)
{
   if (input == null) input = getUniverse().createPropertySet();
   
   CatreLog.logD("CATPROG","On firing for condition " + getName() +
         " " + condition_handlers.getListenerCount());
   
   getUniverse().startUpdate();
   try {
      CondState cs = cond_state;
      if (!cs.setOn(input)) return;

      for (CatreConditionListener ch : condition_handlers) {
	 try {
	    ch.conditionOn(this,input);
	  }
	 catch (Throwable t) {
	    CatreLog.logE("CATMODEL","Problem with condition handler",t);
	  }
       }
    }
   finally {
      getUniverse().endUpdate();
    }
}


protected void fireTrigger(CatrePropertySet input)
{
   if (input == null) input = getUniverse().createPropertySet();
   
   CatreLog.logD("CATPROG","Trigger firing for condition " + getName());

   getUniverse().startUpdate();
   try {
      for (CatreConditionListener ch : condition_handlers) {
	 try {
	    ch.conditionTrigger(this,input);
	  }
	 catch (Throwable t) {
	    CatreLog.logE("CATMODEL","Problem with condition handler",t);
	  }
       }
    }
   finally {
      getUniverse().endUpdate();
    }
}


protected void fireOff()
{
   CatreLog.logD("CATPROG","Off firing for condition " + getName());
   
   getUniverse().startUpdate();
   try {
      CondState cs = cond_state;
      if (!cs.setOff()) return;

      for(CatreConditionListener ch : condition_handlers) {
	 try {
	    ch.conditionOff(this);
	  }
	 catch (Throwable t) {
	    CatreLog.logE("CATMODEL","Problem with condition handler",t);
	  }
       }
    }
   finally {
      getUniverse().endUpdate();
    }
}


protected void fireError(Throwable cause)
{
   getUniverse().startUpdate();
   try {
      CondState cs = cond_state;
      if (!cs.setError(cause)) return;

      for (CatreConditionListener ch : condition_handlers) {
	 try {
	    ch.conditionError(this,cause);
	  }
	 catch (Throwable t) {
	    CatreLog.logE("CATMODEL","Problem with condition handler",t);
	  }
       }
    }
   finally {
      getUniverse().endUpdate();
    }
}


protected void fireValidated()
{
   boolean valid = isValid();

   for (CatreConditionListener ch : condition_handlers) {
      try {
	 ch.conditionValidated(this,valid);
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

@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   is_shared = getSavedBool(map,"SHARED",is_shared);
}


@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("SHARED",is_shared);

   return rslt;
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




}	// end of class CatprogCondition




/* end of CatprogCondition.java */

