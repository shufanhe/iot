/********************************************************************************/
/*                                                                              */
/*              CatdevVirtualDeviceDuration.java                                */
/*                                                                              */
/*      Sensor to be used as a trigger or timed condition                       */
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
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreWorld;

public class CatdevVirutalDeviceDuration extends CatdevDevice 
      implements CatdevConstants, CatreDevice, CatreDeviceListener
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreParameter	state_parameter;

private CatreParameterRef base_ref;
private Object		base_state;
private long		min_time;
private long		max_time;

private CatreDevice     last_device;

private Map<CatreWorld,StateRepr> active_states;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatdevVirutalDeviceDuration(CatreUniverse uu,CatreStore cs,Map<String,Object> map)
{
   super(uu);
   
   last_device = null;
   
   fromJson(cs,map);
 
   setup();
}




private void setup()
{
   CatreParameter bp = for_universe.createBooleanParameter("in_duration",true,getLabel());
   
   if (min_time < 0) min_time = 0;
   if (min_time > 0 && max_time < min_time) max_time = 0;
   if (min_time == 0 && max_time <= 0) max_time = 100;
   
   String nm1 = getLabel().replace(" ","-");
   setName(nm1);
   
   String devnm = base_ref.getParameterName();
   if (base_ref.getDevice() != null) devnm = base_ref.getDevice().getLabel() + "." + devnm;
   
   String s1 = null;
   s1 = "Sensor " + devnm + "=" + base_state;
   
   if (min_time == 0 && max_time > 0) {
      s1 += " ON for at most " + getTimeDescription(max_time);
    }
   else if (min_time > 0 && max_time < min_time) {
      s1 += " ON for at least " + getTimeDescription(min_time);
    }
   else if (min_time > 0 && max_time > 0) {
      s1 += " ON for between " + getTimeDescription(min_time) +
         " AND " + getTimeDescription(max_time);
    }
   setDescription(s1);
   
   active_states = new HashMap<>();
   
   state_parameter = addParameter(bp);
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

private String getTimeDescription(long t)
{
   double t0 = t/1000;
   String what = "seconds";
   if (t0 > 60*60) {
      t0 = t0 / 60 / 60;
      what = "hours";
    }
   else if (t0 > 60) {
      t0 = t0 / 60;
      what = "minutes";
    }
   
   return Double.toString(t0) + " " + what;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("VTYPE","Duration");
   
   rslt.put("MIN",min_time);
   rslt.put("MAX",max_time);
   rslt.put("BASESET",base_state);
   rslt.put("BASEREF",base_ref.toJson());
   
   return rslt;
}



@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   min_time = getSavedLong(map,"MIN",0);
   max_time = getSavedLong(map,"MAX",-1);
   
   base_state = getSavedValue(map,"BASESET",null);
   base_ref = getSavedSubobject(cs,map,"BASEREF",this::createBaseRef,base_ref);
}


private CatreParameterRef createBaseRef(CatreStore cs,Map<String,Object> map)
{
   return getUniverse().createParameterRef(this,cs,map);
}


/********************************************************************************/
/*										*/
/*	Handle stat changes in underlying sensor				*/
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
/*	Handle State Modifications						*/
/*										*/
/********************************************************************************/

private class StateRepr {
   
   private CatreWorld for_world;
   private TimerTask timer_task;
   private long start_time;
   
   StateRepr(CatreWorld w) {
      for_world = w;
      timer_task = null;
      start_time = 0;
    }
   
   void handleChange() {
      recheck(0);
      updateStatus();
    }
   
   protected void recheck(long when) {
      if (for_world.isCurrent()) {
         if (timer_task != null) timer_task.cancel();
         timer_task = null;
         if (when <= 0) return;
         CatreLog.logD("CATDEV","RECHECK DURATION " + getLabel() + " " + when);
         timer_task = new TimeChanged(for_world);
         getUniverse().getCatre().schedule(timer_task,when);
       }
    }
   
   private void updateStatus() {
      if (!for_world.isCurrent() || !isEnabled()) return;
      
      try {
         boolean fg = false;
         Object ov = base_ref.getDevice().getValueInWorld(base_ref.getParameter(),for_world);
         fg = base_state.equals(ov);
         
         if (!fg) {
            start_time = 0;
            recheck(0);
            setValueInWorld(state_parameter,Boolean.FALSE,for_world);
            return;
          }
         
         if (start_time == 0) start_time = for_world.getTime();
         long now = for_world.getTime();
         if (now - start_time < min_time) {
            setValueInWorld(state_parameter,Boolean.FALSE,for_world);
            recheck(min_time - (now-start_time));
          }
         else if (max_time > 0 && now-start_time > max_time) {
            setValueInWorld(state_parameter,Boolean.FALSE,for_world);
          }
         else {
            setValueInWorld(state_parameter,Boolean.TRUE,for_world);
            if (max_time > 0) {
               recheck(max_time - (now-start_time));
             }
          }
       }
      catch (Throwable e) {
         CatreLog.logE("CATDEV","Problem with status update",e);
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
      CatreLog.logD("CATDEV","DURATION CHECK at " + new Date().toString());
      StateRepr sr = active_states.get(for_world);
      if (sr != null) sr.updateStatus();
    }
   
}	// end of inner class TimeChanged




}       // end of class CatdevVirtualDeviceDuration




/* end of CatdevVirtualDeviceDuration.java */

