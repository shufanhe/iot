/********************************************************************************/
/*                                                                              */
/*              CatprogConditionDuration.java                                   */
/*                                                                              */
/*      Condition based on a condition holding for a given time                 */
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
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreWorld;


class CatprogConditionDuration extends CatprogCondition
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatprogCondition	base_condition;
private long		min_time;
private long		max_time;
private boolean 	is_trigger;
private Map<CatreWorld,StateRepr> active_states;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogConditionDuration(CatreProgram pgm,CatreCondition c,long start,long end,boolean trigger)
{
   super(pgm,getUniqueName(c,start,end,trigger));
   
   active_states = new HashMap<>();
   
   base_condition = (CatprogCondition) c;
   min_time = start;
   max_time = end;
   is_trigger = trigger;
   
   setName(base_condition.getName() + "@" + min_time + "-" + max_time);
      
   base_condition.addConditionHandler(new CondChanged());
}


CatprogConditionDuration(CatreProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
   
   active_states = new HashMap<>();
   
   base_condition.addConditionHandler(new CondChanged());
}


private static String getUniqueName(CatreCondition c,long start,long end,boolean trigger)
{
   return "DURATION_" + c.getConditionUID() + "_" + start + "_" + end + "_" + trigger;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public void getDevices(Collection<CatreDevice> rslt)
{
   base_condition.getDevices(rslt);
}


@Override public boolean isTrigger()		{ return is_trigger; }



/********************************************************************************/
/*										*/
/*	Status setting methods							*/
/*										*/
/********************************************************************************/

@Override public void setTime(CatreWorld w)
{
   StateRepr sr = getState(w);
   sr.checkAgain(0);
}



private void setError(CatreWorld w,Throwable c)
{
   StateRepr sr = getState(w);
   sr.setError(c);
}


private void setOn(CatreWorld w,CatrePropertySet ps)
{
   StateRepr sr = getState(w);
   sr.setOn(ps);
}


private void setOff(CatreWorld w)
{
   StateRepr sr = getState(w);
   sr.setOff();
}


private synchronized StateRepr getState(CatreWorld w)
{
   StateRepr sr = active_states.get(w);
   if (sr == null) {
      if (is_trigger) sr = new StateReprTrigger(w);
      else  sr = new StateReprTimed(w);
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
   
   CatreCondition cc = getSavedSubobject(cs,map,"CONDITION",for_program::createCondition, base_condition);
   base_condition = (CatprogCondition) cc;
   min_time = getSavedLong(map,"MINTIME",min_time);
   max_time = getSavedLong(map,"MAXTIME",max_time);
   is_trigger = getSavedBool(map,"TRIGGER",is_trigger);
   
   setUID(getUniqueName(base_condition,min_time,max_time,is_trigger));
}



/********************************************************************************/
/*										*/
/*	Handle changes to the condition 					*/
/*										*/
/********************************************************************************/

private class CondChanged implements CatreConditionListener {
   
   @Override public void conditionError(CatreWorld w,Throwable t) {
      setError(w,t);
    } 
   
   @Override public void conditionOn(CatreWorld w,
         CatrePropertySet ps) {
      setOn(w,ps);
    }
   
   @Override public void conditionOff(CatreWorld w) {
      setOff(w);
    }
   
   @Override public void conditionTrigger(CatreWorld w,CatrePropertySet ps) {
      setOn(w,ps);
    }

}	// end of inner class CondChanged



/********************************************************************************/
/*										*/
/*	Generic state representation for a world				*/
/*										*/
/********************************************************************************/

private abstract class StateRepr {
   
   protected CatreWorld for_world;
   protected Throwable error_cause;
   protected TimerTask timer_task;
   protected CatrePropertySet on_params;
   
   StateRepr(CatreWorld w) {
      for_world = w;
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
      if (for_world.isCurrent()) recheck(when);
      else updateStatus();
    }
   
   protected void recheck(long when) {
      if (timer_task != null) timer_task.cancel();
      timer_task = null;
      if (for_world.isCurrent()) {
         if (when <= 0) return;
         timer_task = new TimeChanged(for_world);
         getCatre().schedule(timer_task,when);
       }
    }
   
}	// end of inner class StateRepr




/********************************************************************************/
/*										*/
/*	State Representation for timed condition				*/
/*										*/
/********************************************************************************/

private class StateReprTimed extends StateRepr {
   
   private long start_time;
   
   StateReprTimed(CatreWorld w) {
      super(w);
      start_time = 0;
    }
   
   void updateStatus() {
      if (error_cause != null) {
         start_time = 0;
         recheck(0);
         fireError(for_world,error_cause);
         return;
       }
      
      if (on_params == null) {
         start_time = 0;
         recheck(0);
         fireOff(for_world);
         return;
       }
      
      if (start_time == 0) start_time = for_world.getTime();
      long now = for_world.getTime();
      if (now - start_time < min_time) {
         fireOff(for_world);
         recheck(min_time - (now-start_time));
       }
      else if (max_time > 0 &&	now-start_time > max_time) {
         fireOff(for_world);
       }
      else {
         fireOn(for_world,on_params);
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
   
   StateReprTrigger(CatreWorld w) {
      super(w);
      last_time = 0;
      save_params = null;
    }
   
   void updateStatus() {
      if (error_cause != null) {
         last_time = 0;
         save_params = null;
         checkAgain(0);
         fireError(for_world,error_cause);
         return;
       }
      
      if (on_params == null && last_time == 0) {
         checkAgain(0);
         fireOff(for_world);
         return;
       }
      if (on_params != null) {
         last_time = for_world.getTime();
         save_params = on_params;
         fireOn(for_world,on_params);
         return;
       }
      
      long now = for_world.getTime();
      if (now - last_time < max_time) {
         fireOn(for_world,save_params);
         checkAgain(max_time - (now-last_time));
       }
      else {
         save_params = null;
         last_time = 0;
         fireOff(for_world);
       }
    }

}       // end of inner class StateReprTrigger



/********************************************************************************/
/*										*/
/*	Timer task to cause recheck						*/
/*										*/
/********************************************************************************/

private class TimeChanged extends TimerTask {
   
   private CatreWorld for_world;
   
   TimeChanged(CatreWorld w) {
      for_world = w;
    }
   
   @Override public void run() {
      StateRepr sr = active_states.get(for_world);
      if (sr != null) sr.updateStatus();
    }
   
}       // end of inner class TimeChanged


}       // end of class CatprogConditionDuration




/* end of CatprogConditionDuration.java */

