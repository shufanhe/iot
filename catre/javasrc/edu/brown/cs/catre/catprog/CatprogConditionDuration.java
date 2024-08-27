/********************************************************************************/
/*                                                                              */
/*              CatprogConditionDuration.java                                   */
/*                                                                              */
/*      Condition based on a condition holding for a given time                 */
/*                                                                              */
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


import java.util.Map;
import java.util.TimerTask;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionListener;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;


class CatprogConditionDuration extends CatprogCondition
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatprogCondition base_condition;
private long		min_time;
private long		max_time;
private boolean 	is_trigger;
private StateRepr       active_state;
private CondChanged     cond_handler;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogConditionDuration(CatprogProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
   
   active_state = null;
   
   cond_handler = null;
}


private CatprogConditionDuration(CatprogConditionDuration cc)
{
   super(cc);
   base_condition = (CatprogCondition) cc.base_condition.cloneCondition();
   min_time = cc.min_time;
   max_time = cc.max_time;
   is_trigger = cc.is_trigger;
   active_state = null;
   cond_handler = null;
}


@Override public CatreCondition cloneCondition()
{
   return new CatprogConditionDuration(this);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override protected CatprogCondition getSubcondition() 
{
   return base_condition;
}



@Override public boolean isTrigger()		{ return is_trigger; }



/********************************************************************************/
/*										*/
/*	Status setting methods							*/
/*										*/
/********************************************************************************/

@Override public void setTime()
{
   StateRepr sr = getState();
   sr.checkAgain(0);
}



private void setError(Throwable c)
{
   StateRepr sr = getState();
   sr.setError(c);
}


private void setOn(CatrePropertySet ps)
{
   StateRepr sr = getState();
   sr.setOn(ps);
}


private void setOff()
{
   StateRepr sr = getState();
   sr.setOff();
}


private synchronized StateRepr getState()
{
   StateRepr sr = active_state;
   if (sr == null) {
      if (is_trigger) sr = new StateReprTrigger();
      else  sr = new StateReprTimed();
      active_state = sr;
    }
   
   return sr;
}



/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("TYPE","Duration");
   rslt.put("CONDITION",base_condition.toJson());
   rslt.put("MINTIME",min_time);
   rslt.put("MAXTIME",max_time);
   rslt.put("TRIGGER",is_trigger);
   
   return rslt;
}


@Override 
public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   base_condition = getSavedSubobject(cs,map,"CONDITION",for_program::createCondition, base_condition);
   min_time = getSavedLong(map,"MINTIME",min_time);
   max_time = getSavedLong(map,"MAXTIME",max_time);
   is_trigger = getSavedBool(map,"TRIGGER",is_trigger);
}


@Override boolean isUndefined() 
{
   if (base_condition == null) return true;
   
   return super.isUndefined();
}



/********************************************************************************/
/*										*/
/*	Handle changes to the condition 					*/
/*										*/
/********************************************************************************/

@Override public void addConditionHandler(CatreConditionListener hdlr) 
{
   super.addConditionHandler(hdlr);
   
   if (cond_handler == null) {
      cond_handler = new CondChanged();
      base_condition.addConditionHandler(cond_handler);
    }
}


@Override public void removeConditionHandler(CatreConditionListener hdlr)
{
   super.removeConditionHandler(hdlr);
   if (cond_handler != null) {
      base_condition.removeConditionHandler(cond_handler);
      cond_handler = null;
    }
}



private class CondChanged implements CatreConditionListener {
   
   @Override public void conditionError(CatreCondition cc,Throwable t) {
      setError(t);
    } 
   
   @Override public void conditionOn(CatreCondition cc,CatrePropertySet ps) {
      setOn(ps);
    }
   
   @Override public void conditionOff(CatreCondition cc) {
      setOff();
    }
   
   @Override public void conditionTrigger(CatreCondition cc,CatrePropertySet ps) {
      setOn(ps);
    }

   @Override public void conditionValidated(CatreCondition cc,boolean valid) {
      setValid(valid);
      if (!valid) setOff();
    }
   
}	// end of inner class CondChanged



/********************************************************************************/
/*										*/
/*	Generic state representation for a world				*/
/*										*/
/********************************************************************************/

private abstract class StateRepr {
   
   protected Throwable error_cause;
   protected TimerTask timer_task;
   protected CatrePropertySet on_params;
   
   StateRepr() {
      error_cause = null;
      timer_task = null;
      on_params = null;
    }
   
   abstract void updateStatus();
   
   void setError(Throwable t) {
      if (t == null) t = new Error("Unknown error");
      error_cause = t;
      on_params = null;
      updateStatus();
    }
   
   void setOn(CatrePropertySet ps) {
      on_params = ps;
      checkAgain(0);
      updateStatus();
    }
   
   void setOff() {
      on_params = null;
      checkAgain(0);
      updateStatus();
    }
   
   protected void checkAgain(long when) {
      recheck(when);
    }
   
   protected void recheck(long when) {
      if (timer_task != null) timer_task.cancel();
      timer_task = null;
      if (when <= 0) return;
      timer_task = new TimeChanged();
      getCatre().schedule(timer_task,when);
    }
   
}	// end of inner class StateRepr




/********************************************************************************/
/*										*/
/*	State Representation for timed condition				*/
/*										*/
/********************************************************************************/

private class StateReprTimed extends StateRepr {
   
   private long start_time;
   
   StateReprTimed() {
      start_time = 0;
    }
   
   void updateStatus() {
      if (error_cause != null) {
         start_time = 0;
         recheck(0);
         fireError(error_cause);
         return;
       }
      
      if (on_params == null) {
         start_time = 0;
         recheck(0);
         fireOff();
         return;
       }
      
      long now = getUniverse().getTime();
      if (start_time == 0) start_time = now;
      if (now - start_time < min_time) {
         fireOff();
         recheck(min_time - (now-start_time));
       }
      else if (max_time > 0 &&	now-start_time > max_time) {
         fireOff();
       }
      else {
         fireOn(on_params);
       }
    }
   
}	// end of inner class StateReprTimed




/********************************************************************************/
/*										*/
/*	State Representation for timed condition				*/
/*										*/
/********************************************************************************/

private class StateReprTrigger extends StateRepr {
   
   private long last_time;
   private CatrePropertySet save_params;
   
   StateReprTrigger() {
      last_time = 0;
      save_params = null;
    }
   
   void updateStatus() {
      if (error_cause != null) {
         last_time = 0;
         save_params = null;
         checkAgain(0);
         fireError(error_cause);
         return;
       }
      
      if (on_params == null && last_time == 0) {
         checkAgain(0);
         fireOff();
         return;
       }
      if (on_params != null) {
         last_time = getUniverse().getTime();
         save_params = on_params;
         fireOn(on_params);
         return;
       }
      
      long now = getUniverse().getTime();
      if (now - last_time < max_time) {
         fireOn(save_params);
         checkAgain(max_time - (now-last_time));
       }
      else {
         save_params = null;
         last_time = 0;
         fireOff();
       }
    }

}       // end of inner class StateReprTrigger



/********************************************************************************/
/*										*/
/*	Timer task to cause recheck						*/
/*										*/
/********************************************************************************/

private class TimeChanged extends TimerTask {
   
   TimeChanged() { }
   
   @Override public void run() {
      StateRepr sr = active_state;
      if (sr != null) sr.updateStatus();
    }
   
}       // end of inner class TimeChanged


}       // end of class CatprogConditionDuration




/* end of CatprogConditionDuration.java */

