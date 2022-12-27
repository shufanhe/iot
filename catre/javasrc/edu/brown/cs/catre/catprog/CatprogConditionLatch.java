/********************************************************************************/
/*                                                                              */
/*              CatprogConditionLatch.java                                      */
/*                                                                              */
/*      Condition reflecting a latch of another condition                       */
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

import java.util.Calendar;
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

class CatprogConditionLatch extends CatprogCondition
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CatreCondition  base_condition;
private Calendar        reset_time;
private long            reset_after;
private long            off_after;
private Map<CatreWorld,StateRepr> active_states;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatprogConditionLatch(CatreProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
   
   active_states = new HashMap<>();
   
   base_condition.addConditionHandler(new CondChanged());
}


private String getUniqueName()
{
   return "LATCH" + base_condition.getConditionUID() + "_" +
      reset_time + "_" + reset_after + "_" + off_after;
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
   private long off_time;
   
   StateRepr(CatreWorld w) {
      for_world = w;
      timer_task = null;
      start_time = 0;
      off_time = 0;
      on_params = null;
      error_cause = null;
    }
   
   void noteError(Throwable t) {
      if (t == null) t = new Error("Unknown error");
      error_cause = t;
      on_params = null;
      updateStatus();
    }
   
   void noteOn(CatrePropertySet ps) {
      if (on_params != null) {
         computeOffTime();
         return;
       }
      else {
         on_params = ps;
         computeOffTime();
         fireOn(for_world,on_params);
       }
    }
   
   void noteOff() { }
   
   void checkReset() {
      long now = for_world.getCurrentTime().getTimeInMillis();
      if (start_time > 0 || on_params != null) {
         if (now >= off_time) {
            on_params = null;
            start_time = 0;
            off_time = 0;
            error_cause = null;
            fireOff(for_world);
          }
         else computeOffTime();
       }
    }
   
   private void computeOffTime() {
      long now = for_world.getCurrentTime().getTimeInMillis();
      if (start_time == 0) start_time = now;
      
      long off = off_time;
      if (reset_time != null) {
         long rt = reset_time.getTimeInMillis();
         rt = rt % T_DAY;
         long n1 = now % T_DAY;
         long n0 = now - n1;            // start of day
         off = n0 + rt;
         while (off < now) off += T_DAY;
       }
      else if (off_after > 0) {
         off = now + off_after;
       }
      else if (reset_after > 0) {
         off = start_time + reset_after;
       }
      if (off == off_time) return;
      
      if (timer_task != null) timer_task.cancel();
      timer_task = null;
      if (for_world.isCurrent()) {
         long delay = off - now;
         if (delay <= 0) return;
         timer_task = new TimeChanged(for_world);
         getCatre().schedule(timer_task,delay);
       }
    }
   
   private void updateStatus() {
      if (error_cause != null) {
         start_time = 0;
         fireError(for_world,error_cause);
         return;
       }
      if (on_params == null) {
         start_time = 0;
         fireOff(for_world);
       }
    }
   
}       // end of innter class StateRepr



private class TimeChanged extends TimerTask {

   private CatreWorld for_world;
   
   TimeChanged(CatreWorld w) {
      for_world = w;
    }
   
   @Override public void run() {
      StateRepr sr = active_states.get(for_world);
      if (sr != null) sr.checkReset();
    }

}       // end of inner class TimeChanged



/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("TYPE","Latch");
   rslt.put("CONDITION",base_condition.toJson());
   if (reset_time != null) rslt.put("RESETTIME",reset_time.getTimeInMillis());
   rslt.put("RESETAFTER",reset_after);
   rslt.put("OFFAFTER",off_after);
   
   return rslt;
}


@Override 
public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   base_condition = getSavedSubobject(cs,map,"CONDITION",for_program::createCondition, base_condition);
   long t0 = getSavedLong(map,"RESETTIME",0);
   if (t0 == 0) reset_time = null;
   else {
      reset_time = Calendar.getInstance();
      reset_time.setTimeInMillis(t0);
    }
   reset_after = getSavedLong(map,"RESETAFTER",0);
   off_after = getSavedLong(map,"OFFAFTER",0);
   
   setUID(getUniqueName());
}




}       // end of class CatprogConditionLatch




/* end of CatprogConditionLatch.java */

