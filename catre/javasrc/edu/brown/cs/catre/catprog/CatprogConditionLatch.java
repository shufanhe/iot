/********************************************************************************/
/*                                                                              */
/*              CatprogConditionLatch.java                                      */
/*                                                                              */
/*      Condition reflecting a latch of another condition                       */
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

import java.util.Calendar;
import java.util.Map;
import java.util.TimerTask;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionListener;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;


/********************************************************************************/
/*										*/
/*	Status setting methods							*/
/*										*/
/********************************************************************************/


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
private StateRepr       active_state;
private CondChanged     cond_handler;
private Calendar        begin_interval;
private Calendar        end_interval;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatprogConditionLatch(CatprogProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
   
   active_state = new StateRepr();
   
   cond_handler = null;
}


private CatprogConditionLatch(CatprogConditionLatch cc)
{
   super(cc);
   base_condition = cc.base_condition;
   reset_time = cc.reset_time;
   reset_after = cc.reset_after;
   off_after = cc.off_after;
   active_state = null;
   cond_handler = null;
}


@Override public CatreCondition cloneCondition()
{
   return new CatprogConditionLatch(this);
}


@Override public void activate()
{
   if (active_state != null) return;
   base_condition.activate();
   
   active_state = new StateRepr();
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
   
   if (cond_handler != null && !hasConditionHandlers()) { 
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
   private long off_time;
   
   StateRepr() {
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
      if (begin_interval != null && end_interval != null) {
         Calendar c = Calendar.getInstance();
         c.setTimeInMillis(System.currentTimeMillis());
         int h = c.get(Calendar.HOUR_OF_DAY);
         int m = c.get(Calendar.MINUTE);
         int h0 = begin_interval.get(Calendar.HOUR_OF_DAY);
         int m0 = begin_interval.get(Calendar.MINUTE);
         int h1 = end_interval.get(Calendar.HOUR_OF_DAY);
         int m1 = end_interval.get(Calendar.MINUTE);
         int t = h*60 + m;
         int t0 = h0*60 + m0;
         int t1 = h1*60 + m1;
         if (t < t0 || t > t1) return;
       }
      if (on_params != null) {
         computeOffTime();
         return;
       }
      else {
         on_params = ps;
         computeOffTime();
         fireOn(on_params);
       }
    }
   
   void noteOff() { }
   
   void checkReset() {
      long now = getUniverse().getTime();
      if (start_time > 0 || on_params != null) {
         if (now >= off_time) {
            on_params = null;
            start_time = 0;
            off_time = 0;
            error_cause = null;
            fireOff();
          }
         else computeOffTime();
       }
    }
   
   private void computeOffTime() {
      long now = getUniverse().getTime();
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
      long delay = off - now;
      if (delay <= 0) return;
      timer_task = new TimeChanged();
      getCatre().schedule(timer_task,delay);
    }
   
   private void updateStatus() {
      if (error_cause != null) {
         start_time = 0;
         fireError(error_cause);
         return;
       }
      if (on_params == null) {
         start_time = 0;
         fireOff();
       }
    }
   
}       // end of innter class StateRepr



private class TimeChanged extends TimerTask {

   @Override public void run() {
      if (active_state != null) active_state.checkReset();
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
   if (begin_interval != null) rslt.put("BEGININTERVAL",begin_interval.getTimeInMillis());
   if (end_interval != null) rslt.put("ENDINTERVAL",end_interval.getTimeInMillis());
   
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
   
   long t1 = getSavedLong(map,"BEGININTERVAL",-1);
   if (t1 >= 0) {
      begin_interval = Calendar.getInstance();
      begin_interval.setTimeInMillis(t1);
    }
   else {
      begin_interval = null;
    }
   long t2 = getSavedLong(map,"ENDINTERVAL",-1);
   if (t1 >= 0 && t2 >= 0) {
      end_interval = Calendar.getInstance();
      end_interval.setTimeInMillis(t2);
    }
   else {
      end_interval = null;
    }
   
}




}       // end of class CatprogConditionLatch




/* end of CatprogConditionLatch.java */

