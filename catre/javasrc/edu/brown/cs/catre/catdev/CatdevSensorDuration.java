/********************************************************************************/
/*                                                                              */
/*              CatdevSensorDuration.java                                       */
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

import edu.brown.cs.catre.catmodel.CatmodelConditionParameter;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionHandler;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceHandler;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUniverseListener;
import edu.brown.cs.catre.catre.CatreWorld;

public class CatdevSensorDuration extends CatdevDevice implements CatdevConstants, CatreDevice
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		sensor_label;
private String		sensor_name;
private CatreParameter	state_parameter;

private CatreDevice	base_sensor;
private CatreParameter	base_parameter;
private Object		base_state;
private long		min_time;
private long		max_time;

private Map<CatreWorld,StateRepr> active_states;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatdevSensorDuration(String id,CatreDevice base,CatreParameter param,Object state,long start,long end)
{
   super(base.getUniverse());
   
   base_sensor = base;
   base_state = (state == null ? Boolean.TRUE : state);
   sensor_label = id;
   min_time = start;
   max_time = end;
   base_parameter = param;
   
   if (base_parameter == null && base_sensor != null) {
      base_parameter = base_sensor.findParameter(base_sensor.getDataUID());
    }
   
   setup();
}



public CatdevSensorDuration(CatreUniverse uu,CatreStore cs,Map<String,Object> map)
{
   super(uu);
   
   fromJson(cs,map);
 
   uu.addUniverseListener(new UniverseChanged());
   
   setup();
}




private void setup()
{
   CatreParameter bp = for_universe.createBooleanParameter(getDataUID(),true,getLabel());
   
   if (min_time < 0) min_time = 0;
   if (min_time > 0 && max_time < min_time) max_time = 0;
   if (min_time == 0 && max_time <= 0) max_time = 100;
   
   String nm1 = sensor_label.replace(" ",WSEP);
   
   sensor_name = getUniverse().getName() + NSEP + nm1;
   
   active_states = new HashMap<CatreWorld,StateRepr>();
   
   state_parameter = addParameter(bp);
   
   CatreCondition uc = getCondition(state_parameter,Boolean.TRUE);
   uc.setLabel(sensor_label);
   CatreCondition ucf = getCondition(state_parameter,Boolean.FALSE);
   ucf.setLabel("Not " + sensor_label);
}



@Override protected void localStartDevice()
{
   base_sensor.addDeviceHandler(new SensorChanged());
   
   handleStateChanged(getCurrentWorld());
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()		{ return sensor_name; }

@Override public String getLabel()		{ return sensor_label; }

@Override public String getDescription()
{
   String s1 = null;
   s1 = "Sensor " + base_sensor.getLabel() + "=" + base_state;
   
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
   
   return s1;
}


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


@Override protected CatreCondition createParameterCondition(CatreParameter p,Object v,boolean trig)
{
   return new DurationCondition(p,v,trig);
}



@Override public boolean isDependentOn(CatreDevice d)
{
   if (d == base_sensor) return true;
   
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
   rslt.put("MIN",min_time);
   rslt.put("MAX",max_time);
   rslt.put("BASESET",base_state);
   rslt.put("BASEPARAM",base_parameter.getName());
   rslt.put("BASESENSOR",getUIDToSave(base_sensor));
   
   return rslt;
}



@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   min_time = getSavedLong(map,"MIN",0);
   max_time = getSavedLong(map,"MAX",-1);
   
   sensor_label = getSavedString(map,"LABEL",sensor_label);
   sensor_name = getSavedString(map,"NAME",sensor_name);
   
   base_state = getSavedValue(map,"BASESET",null);
   String bid = getSavedString(map,"BASESENSOR",null);
   base_sensor = getUniverse().findDevice(bid);
   String bpnm = getSavedString(map,"BASEPARAM",null);
   if (bpnm != null) base_parameter = base_sensor.findParameter(bpnm);
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



private class SensorChanged implements CatreDeviceHandler, CatreConditionHandler {
   
   @Override public void stateChanged(CatreWorld w,CatreDevice s) {
      handleStateChanged(w);
    }
   
   @Override public void conditionOn(CatreWorld w,CatreCondition c,CatrePropertySet p) {
      handleStateChanged(w);
    }
   
   @Override public void conditionTrigger(CatreWorld w,CatreCondition c,CatrePropertySet p) {
      if (w.isCurrent()) {
         handleStateChanged(w);
       }
    }
   
   @Override public void conditionOff(CatreWorld w,CatreCondition c) {
      handleStateChanged(w);
    }
   
}	// end of inner class SensorChanged



private class UniverseChanged implements CatreUniverseListener {
   
   @Override public void deviceRemoved(CatreUniverse u,CatreDevice s) {
      if (for_universe == u && base_sensor == s) {
         // remove this sensor as well
       }
    }
   
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
      if (!for_world.isCurrent()) return;
      
      try {
         boolean fg = false;
         Object ov = base_sensor.getValueInWorld(base_parameter,for_world);
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



/********************************************************************************/
/*										*/
/*	Parameter condition for a duration sensor				*/
/*										*/
/********************************************************************************/

private class DurationCondition extends CatmodelConditionParameter {
   
   DurationCondition(CatreParameter p,Object v,boolean trig) {
      super(CatdevSensorDuration.this,p,v,trig);
    }
   
   @Override protected boolean isConsistentWith(CatreCondition bc) {
      if (!super.isConsistentWith(bc)) return false;
      if (bc instanceof CatmodelConditionParameter) {
         CatmodelConditionParameter sbc = (CatmodelConditionParameter) bc;
         if (sbc.getDevice() == base_sensor && sbc.getParameter() == base_parameter) {
            if (getState() == Boolean.TRUE) {
               if (sbc.getState() != base_state) return false;
             }
          }
       }
      
      return true;
    }
   
}	// end of inner class DurationCondition



}       // end of class CatdevSensorDuration




/* end of CatdevSensorDuration.java */

