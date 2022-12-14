/********************************************************************************/
/*                                                                              */
/*              CatdevSensorLatch.java                                          */
/*                                                                              */
/*      Implementation of a latch-type-sensor device                            */
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



package edu.brown.cs.catre.catdev;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceListener;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterRef;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTransition;
import edu.brown.cs.catre.catre.CatreTransitionType;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreWorld;

class CatdevSensorLatch extends CatdevDevice implements CatdevConstants,
      CatreDeviceListener
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreParameter	state_parameter;

// private CatreDevice	base_sensor;
// private CatreParameter	base_parameter;
private CatreParameterRef base_ref;
private Object		base_state;
private Calendar	reset_time;
private long		reset_after;
private long		off_after;
private CatreDevice     last_device;

private Map<CatreWorld,StateRepr> active_states;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatdevSensorLatch(CatreUniverse uu,CatreStore cs,Map<String,Object> map)
{
   super(uu);
   
   fromJson(cs,map);
   
   setup();
}



private void setup()
{
   CatreParameter bp = for_universe.createBooleanParameter("Latched",true,getLabel());
   
   last_device = null;
   
   active_states = new HashMap<>();
   
   state_parameter = addParameter(bp);
   
   CatdevTransition ct = new CatdevTransition(this,CatreTransitionType.STATE_CHANGE,null);
   ct.setName("Reseter");
   ct.setLabel("Reset " + getLabel());
   addTransition(ct);
}



/********************************************************************************/
/*                                                                              */
/*      Startup methods                                                         */
/*                                                                              */
/********************************************************************************/

@Override protected boolean isDeviceValid()             { return base_ref.isValid(); }

@Override protected void localStartDevice()
{
   CatreDevice d = base_ref.getDevice();
   d.addDeviceListener(this);
   last_device = d;
   
   handleStateChanged(getCurrentWorld());
}


@Override protected void localStopDevice()
{
   if (last_device != null) last_device.removeDeviceListener(this);
   last_device = null;
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public boolean isDependentOn(CatreDevice d)
{
   CatreDevice cd = base_ref.getDevice();
   
   if (d == this || d == cd) return true;
   
   if (cd != null) return cd.isDependentOn(d);
   
   return false;
}


/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("VTYPE","Latch");
   
   if (reset_time != null) rslt.put("RESET",reset_time.getTimeInMillis());
   rslt.put("AFTER",reset_after);
   rslt.put("OFFAFTER",off_after);
   
   rslt.put("BASESET",base_state);
   rslt.put("BASEREF", base_ref.toJson());
   
   return rslt;
}
   
   
@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   reset_time = null;
   long reset = getSavedLong(map,"RESET",0);
   if (reset > 0) {
      reset_time = Calendar.getInstance();
      reset_time.setTimeInMillis(reset);
    }
   reset_after = getSavedLong(map,"AFTER",reset_after);
   off_after = getSavedLong(map,"OFFAFTER",off_after);
   
   base_state = getSavedString(map,"BASESET",null);
   base_ref = getSavedSubobject(cs,map,"BASEREF",this::createParamRef,base_ref);
   
   active_states = new HashMap<>();
}


private CatreParameterRef createParamRef(CatreStore cs,Map<String,Object> map)
{
   return getUniverse().createParameterRef(this,cs,map);
}



/********************************************************************************/
/*										*/
/*	Handle state changes in underlying sensor				*/
/*										*/
/********************************************************************************/

private void handleStateChanged(CatreWorld w)
{
   StateRepr sr = active_states.get(w);
   if (sr == null) {
      sr = new StateRepr(w);
      active_states.put(w,sr);
    }
   
   sr.handleChange();
}


   
@Override public void stateChanged(CatreWorld w)
{
   handleStateChanged(w);
}



/********************************************************************************/
/*										*/
/*	Handle resets								*/
/*										*/
/********************************************************************************/

private void resetLatch(CatreWorld w)
{
   StateRepr sr = active_states.get(w);
   if (sr != null) sr.handleReset();
}



@Override public void apply(CatreTransition t,Map<String,Object> vals,CatreWorld w)
{
   if (t.getName().equals("Reseter")) resetLatch(w);
}



/********************************************************************************/
/*										*/
/*	Handle State Modifications						*/
/*										*/
/********************************************************************************/

private class StateRepr {
   
   private CatreWorld for_world;
   private TimerTask timer_task;
   private long start_time;
   private long off_time;
   
   StateRepr(CatreWorld w) {
      for_world = w;
      timer_task = null;
      start_time = 0;
      off_time = 0;
    }
   
   void handleChange() {
      recheck(0);
      updateStatus(true);
    }
   
   void handleReset() {
      if (!isEnabled()) return;
      boolean fg = false;
      try {
         Object ov = base_ref.getDevice().getValueInWorld(base_ref.getParameter(),for_world);
         fg = base_state.equals(ov);
         
         if (fg) {
            handleRecheck();
          }
         else  {
            start_time = 0;
            off_time = 0;
            setValueInWorld(state_parameter,Boolean.FALSE,for_world);
          }
       }
      catch (Throwable e) {
         CatreLog.logE("CATDEV","Problem with sensor latch state change",e);
       }
    }
   
   protected void recheck(long when) {
      if (for_world.isCurrent()) {
         if (timer_task != null) timer_task.cancel();
         timer_task = null;
         CatreLog.logI("CATDEV","RECHECK LATCH DURATION " + getLabel() + " " +  when);
         if (when <= 0) return;
         timer_task = new TimeChanged(for_world);
         getCatre().schedule(timer_task,when);
       }
    }
   
   private void updateStatus(boolean set) {
      if (!for_world.isCurrent()) return;
     if (!isEnabled()) return;
     
      try {
         boolean fg = false;
         Object ov = base_ref.getDevice().getValueInWorld(base_ref.getParameter(),for_world);
         fg = base_state.equals(ov);
         
         if (!fg) {
            Object nv = getValueInWorld(state_parameter,for_world);
            if (nv == null) setValueInWorld(state_parameter,Boolean.FALSE,for_world);
            
            if (off_time <= 0) {
               if (reset_after == 0) start_time = 0;
               off_time = for_world.getTime();
               handleRecheck();
             }
            return;
          }
         
         off_time = 0;
         start_time = for_world.getTime();
         setValueInWorld(state_parameter,Boolean.TRUE,for_world);
         handleRecheck();
       }
      catch (Throwable e) {
         CatreLog.logE("CATDEV","Problem updating status",e);
       }
    }
   
   private void handleRecheck() {
      long now = System.currentTimeMillis();
      
      if (reset_time != null) {
         long rt = reset_time.getTimeInMillis();
         rt = rt % T_DAY;
         long n1 = now % T_DAY;
         long d = rt - n1;
         while (d <= 0) d += T_DAY;
         recheck(d);
       }
      else if (off_after > 0 && off_time > 0) {
         long when = off_time + off_after;
         long d = when - now;
         recheck(d);
       }
      else if (reset_after > 0 && start_time > 0) {
         long when = start_time + reset_after;
         long d = when - now;
         recheck(d);
       }
    }
   
   
}	// end of inner class StateRepr




/********************************************************************************/
/*										*/
/*	Timer Task to auto update						*/
/*										*/
/********************************************************************************/

private class TimeChanged extends TimerTask {
   
   private CatreWorld for_world;
   
   TimeChanged(CatreWorld w) {
      for_world = w;
    }
   
   @Override public void run() {
      CatreLog.logD("CATDEV","LATCH RESET at " + new Date().toString());
      StateRepr sr = active_states.get(for_world);
      if (sr != null) sr.handleReset();
    }
   
}	// end of inner class TimeChanged




}       // end of class CatdevSensorLatch




/* end of CatdevSensorLatch.java */

