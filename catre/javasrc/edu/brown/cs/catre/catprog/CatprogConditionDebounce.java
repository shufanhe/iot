/********************************************************************************/
/*                                                                              */
/*              CatprogConditionDebounce.java                                   */
/*                                                                              */
/*      description of class                                                    */
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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TimerTask;


import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionListener;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;

class CatprogConditionDebounce extends CatprogCondition
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CatreCondition  base_condition;
private long            min_ontime;
private long            min_offtime;
private StateRepr       active_state;
private CondChanged     cond_handler;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatprogConditionDebounce(CatprogProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
   
   active_state = new StateRepr();
   
   cond_handler = null;
}



private CatprogConditionDebounce(CatprogConditionDebounce cc)
{
   super(cc);
   cc.base_condition = base_condition.cloneCondition();
   min_ontime = cc.min_ontime;
   min_offtime = cc.min_offtime;
   active_state = null;
   cond_handler = null;
}


@Override public CatreCondition cloneCondition()
{
   return new CatprogConditionDebounce(this);
}


@Override public void activate()
{
   if (active_state == null) active_state = new StateRepr();
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void noteUsed(boolean fg)
{
   base_condition.noteUsed(fg);
}


protected Collection<CatreCondition> getSubconditions() 
{
   return Collections.singletonList(base_condition);
}



/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("TYPE","Debounce");
   rslt.put("CONDITION",base_condition.toJson());
   rslt.put("ONTIME",min_ontime);
   rslt.put("OFFTIME",min_offtime);
   
   return rslt;
}


@Override 
public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   base_condition = getSavedSubobject(cs,map,"CONDITION",for_program::createCondition, base_condition);
   min_ontime = getSavedLong(map,"ONTIME",-1);
   min_offtime = getSavedLong(map,"OFFTIME",-1);
   if (min_ontime < 0 && min_offtime >= 0) min_ontime = min_offtime;
   if (min_offtime < 0) min_offtime = min_ontime;
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
      active_state.noteError(t);
    } 
   
   @Override public void conditionOn(CatreCondition cc,CatrePropertySet ps) {
      active_state.noteOn(ps);
    }
   
   @Override public void conditionOff(CatreCondition cc) {
      active_state.noteOff();
    }
   
   @Override public void conditionTrigger(CatreCondition cc,CatrePropertySet ps) {
      active_state.noteOn(ps);
    }
   
   @Override public void conditionValidated(CatreCondition cc,boolean valid) {
      setValid(valid);
      if (!valid) active_state.noteOff();
    }

}	// end of inner class CondChanged



/********************************************************************************/
/*										*/
/*	Handle State Modifications						*/
/*										*/
/********************************************************************************/

private class StateRepr {
   
   private TimerTask timer_task;
   private Throwable error_cause;
   private CatrePropertySet on_params;
   private long start_time;
   private long end_time;
   
   StateRepr() {
      timer_task = null;
      start_time = 0;
      end_time = 0;
      on_params = null;
      error_cause = null;
    }
   
   void noteError(Throwable t) {
      if (t == null) t = new Error("Unknown error");
      error_cause = t;
      on_params = null;
      start_time = 0;
      end_time = 0;
      fireError(error_cause);
    }
   
   void noteOn(CatrePropertySet ps) {
      if (on_params != null) return;
      on_params = ps;
      start_time = 0;
      computeEndTime();
    }
   
   void noteOff() { 
      if (on_params == null) return;
      on_params = null;
      start_time = 0;
      computeEndTime();
    }
   
   void checkCommit() {
      long now = getUniverse().getTime();
      if (start_time > 0) {
         if (now >= end_time) {
            if (on_params != null) {
               fireOn(on_params);
             }
            else {
               fireOff();
             }
            start_time = 0;
            end_time = 0;
            error_cause = null;
          }
         else computeEndTime();
       }
    }
   
   private void computeEndTime() {
      long now = getUniverse().getTime();
      if (start_time == 0) start_time = now;
      
      long end = end_time;
      if (on_params != null) end = start_time + min_ontime;
      else end = start_time + min_offtime;
      
      end_time = end;
      if (end_time > now) {
         long delay = end_time - now;
         timer_task = new TimeChanged();
         getCatre().schedule(timer_task,delay);
       }
      else checkCommit();
    }
   
}       // end of innter class StateRepr



private class TimeChanged extends TimerTask {
   
   TimeChanged() { }
   
   @Override public void run() {
      StateRepr sr = active_state;
      if (sr != null) sr.checkCommit();
    }
   
}       // end of inner class TimeChanged


}       // end of class CatprogConditionDebounce




/* end of CatprogConditionDebounce.java */

