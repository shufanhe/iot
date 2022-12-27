/********************************************************************************/
/*                                                                              */
/*              CatprogConditionDebounce.java                                   */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2022 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2022, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.catre.catprog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionListener;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreWorld;

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
private Map<CatreWorld,StateRepr> active_states;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatprogConditionDebounce(CatprogProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
   
   active_states = new HashMap<>();
   
   base_condition.addConditionHandler(new CondChanged());
}


private String getUniqueName()
{
   return "DEBOUNCE" + base_condition.getConditionUID() + "_" +
      min_ontime + "_" + min_offtime;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void getDevices(Collection<CatreDevice> rslt)
{
   base_condition.getDevices(rslt);
}


/********************************************************************************/
/*										*/
/*	Status setting methods							*/
/*										*/
/********************************************************************************/

@Override public void setTime(CatreWorld w)             { }


private synchronized StateRepr getState(CatreWorld w)
{
   if (w == null) w = getUniverse().getCurrentWorld();
   
   StateRepr sr = active_states.get(w);
   if (sr == null) {
      sr = new StateRepr(w);
      active_states.put(w,sr);
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
   
   setUID(getUniqueName());
}


/********************************************************************************/
/*										*/
/*	Handle changes to the condition 					*/
/*										*/
/********************************************************************************/

private class CondChanged implements CatreConditionListener {
   
   @Override public void conditionError(CatreWorld w,Throwable t) {
      getState(w).noteError(t);
    } 
   
   @Override public void conditionOn(CatreWorld w,CatrePropertySet ps) {
      getState(w).noteOn(ps);
    }
   
   @Override public void conditionOff(CatreWorld w) {
      getState(w).noteOff();
    }
   
   @Override public void conditionTrigger(CatreWorld w,CatrePropertySet ps) {
      getState(w).noteOn(ps);
    }
   
   @Override public void conditionValidated(boolean valid) {
      setValid(valid);
      if (!valid) getState(null).noteOff();
    }

}	// end of inner class CondChanged



/********************************************************************************/
/*										*/
/*	Handle State Modifications						*/
/*										*/
/********************************************************************************/

private class StateRepr {
   
   private CatreWorld for_world;
   private TimerTask timer_task;
   private Throwable error_cause;
   private CatrePropertySet on_params;
   private long start_time;
   private long end_time;
   
   StateRepr(CatreWorld w) {
      for_world = w;
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
      fireError(for_world,error_cause);
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
      long now = for_world.getCurrentTime().getTimeInMillis();
      if (start_time > 0) {
         if (now >= end_time) {
            if (on_params != null) {
               fireOn(for_world,on_params);
             }
            else {
               fireOff(for_world);
             }
            start_time = 0;
            end_time = 0;
            error_cause = null;
          }
         else computeEndTime();
       }
    }
   
   private void computeEndTime() {
      long now = for_world.getCurrentTime().getTimeInMillis();
      if (start_time == 0) start_time = now;
      
      long end = end_time;
      if (on_params != null) end = start_time + min_ontime;
      else end = start_time + min_offtime;
      
      end_time = end;
      if (end_time > now) {
         long delay = end_time - now;
         timer_task = new TimeChanged(for_world);
         getCatre().schedule(timer_task,delay);
       }
      else checkCommit();
    }
   
}       // end of innter class StateRepr



private class TimeChanged extends TimerTask {
   
   private CatreWorld for_world;
   
   TimeChanged(CatreWorld w) {
      for_world = w;
    }
   
   @Override public void run() {
      StateRepr sr = active_states.get(for_world);
      if (sr != null) sr.checkCommit();
    }
   
}       // end of inner class TimeChanged


}       // end of class CatprogConditionDebounce




/* end of CatprogConditionDebounce.java */

