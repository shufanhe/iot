/********************************************************************************/
/*                                                                              */
/*              CatmodelConditionDuration.java                                  */
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionHandler;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreWorld;

class CatmodelConditionDuration extends CatmodelCondition implements CatmodelConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatmodelCondition	base_condition;
private long		min_time;
private long		max_time;
private boolean 	is_trigger;
private Map<CatreWorld,StateRepr> active_states;
private boolean         is_setup;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatmodelConditionDuration(CatmodelCondition c,long start,long end,boolean trigger)
{
   super(c.for_universe);
   
   initialize();
   
   base_condition = (CatmodelCondition) c;
   min_time = start;
   max_time = end;
   is_trigger = trigger;
   
   setup();
}


private void initialize()
{
   base_condition = null;
   is_setup = false;
   min_time = 0;
   max_time = 0;
   active_states = new HashMap<CatreWorld,StateRepr>();
}


private void setup()
{
   if (is_setup) return;
   if (base_condition == null) return;
   base_condition.addConditionHandler(new CondChanged());
   is_setup = true;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()
{
   setup();
   return base_condition.getName() + "@" + min_time + "-" + max_time;
}

@Override public String getDescription()
{
   return getName();
}

@Override public void getSensors(Collection<CatreDevice> rslt)
{
   setup();
   base_condition.getSensors(rslt);
}


@Override public boolean isTrigger()		{ return is_trigger; }


@Override public boolean isBaseCondition()              { return false; }




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
/*										*/
/*	Conflict detection							*/
/*										*/
/********************************************************************************/

protected boolean isConsistentWith(CatreCondition uc)
{
   if (uc instanceof CatmodelConditionDuration) {
      setup();
      CatmodelConditionDuration bct = (CatmodelConditionDuration) uc;
      if (bct.base_condition == bct.base_condition) {
	 if (max_time <= bct.min_time || min_time >= bct.max_time) return false;
	 return true;
       }
    }
   
   return base_condition.isConsistentWith(uc);
}



@Override public void addImpliedProperties(CatrePropertySet ups)
{
   // add properties for start and end time and day of week
}



protected boolean checkOverlapConditions(CatmodelCondition bc)
{
   if (bc instanceof CatmodelConditionDuration) {
      setup();
      CatmodelConditionDuration bct = (CatmodelConditionDuration) bc;
      if (bct.base_condition == base_condition) {
	 if (max_time <= bct.min_time || min_time >= bct.max_time) return false;
	 return true;
       }
    }
   
   return bc.isConsistentWith(base_condition);
}



/********************************************************************************/
/*										*/
/*	Handle changes to the condition 					*/
/*										*/
/********************************************************************************/

private class CondChanged implements CatreConditionHandler {
   
   @Override public void conditionError(CatreWorld w,CatreCondition c,Throwable t) {
      setError(w,t);
    } 
   
   @Override public void conditionOn(CatreWorld w,CatreCondition c,
         CatrePropertySet ps) {
      setOn(w,ps);
    }
   
   @Override public void conditionOff(CatreWorld w,CatreCondition c) {
      setOff(w);
    }
   
   @Override public void conditionTrigger(CatreWorld w,CatreCondition c,CatrePropertySet ps) {
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


}


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
   
}



}       // end of class CatmodelConditionDuration




/* end of CatmodelConditionDuration.java */

