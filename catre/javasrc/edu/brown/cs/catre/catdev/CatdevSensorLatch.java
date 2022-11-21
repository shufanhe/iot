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

import edu.brown.cs.catre.catmodel.CatmodelConditionParameter;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreConditionHandler;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceHandler;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTransition;
import edu.brown.cs.catre.catre.CatreTransitionType;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUniverseListener;
import edu.brown.cs.catre.catre.CatreWorld;

class CatdevSensorLatch extends CatdevDevice implements CatdevConstants
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
private Calendar	reset_time;
private long		reset_after;
private long		off_after;

private Map<CatreWorld,StateRepr> active_states;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatdevSensorLatch(String id,CatreDevice base,CatreParameter param,Object state,Calendar reset)
{
   this(id,base,param,state,0,0,reset);
}



public CatdevSensorLatch(String id,CatreDevice base,CatreParameter param,Object state,long after,long offafter)
{
   this(id,base,param,state,after,offafter,null);
}



private CatdevSensorLatch(String id,CatreDevice base,CatreParameter param,Object state,long after,long offafter,Calendar time)
{
   super(base.getUniverse());
   
   base_sensor = base;
   base_state = state;
   sensor_label = id;
   reset_time = time;
   reset_after = after;
   off_after = offafter;
   
   base_parameter = param;
   
   if (base_parameter == null) {
      base_parameter = base_sensor.findParameter(base_sensor.getDataUID());
    }
   
   setup();
}



CatdevSensorLatch(CatreUniverse uu,CatreStore cs,Map<String,Object> map)
{
   super(uu);
   fromJson(cs,map);
   
   for_universe.addUniverseListener(new UniverseChanged());
   
   setup();
}



private void setup()
{
   CatreParameter bp = for_universe.createBooleanParameter(getDataUID(),true,getLabel());
   
   String nm1 = sensor_label.replace(" ",WSEP);
   
   sensor_name = getUniverse().getName() + NSEP + nm1;
   
   active_states = new HashMap<>();
   
   state_parameter = addParameter(bp);

   CatreCondition uc = getCondition(state_parameter,Boolean.TRUE);
   uc.setLabel(sensor_label);
   CatreCondition ucf = getCondition(state_parameter,Boolean.FALSE);
   ucf.setLabel("Not " + sensor_label);
   
   Reseter rst = new Reseter();
   addTransition(rst);
}


@Override protected void localStartDevice()
{
   if (base_sensor != null) {
      base_sensor.addDeviceHandler(new SensorChanged());
    }
   
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
   if (base_sensor != null) {
      s1 = "Latch " + base_sensor.getLabel() + "=" + base_state;
    }
   else s1 = "Latch";
   
   return s1;
}


@Override protected CatreCondition createParameterCondition(CatreParameter p,Object v,boolean trig)
{
   return new LatchCondition(p,v,trig);
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
   if (reset_time != null) rslt.put("RESET",reset_time.getTimeInMillis());
   rslt.put("AFTER",reset_after);
   rslt.put("OFFAFTER",off_after);
   
   rslt.put("BASESET",base_state);
   rslt.put("BASEPARAM",base_parameter.getName());
   rslt.put("BASESENSOR",base_sensor.getDataUID());
   
   return rslt;
}
   
   
@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   sensor_label = getSavedString(map,"LABEL",sensor_label);
   sensor_name = getSavedString(map,"NAME",sensor_name);
   
   reset_time = null;
   long reset = getSavedLong(map,"RESET",0);
   if (reset > 0) {
      reset_time = Calendar.getInstance();
      reset_time.setTimeInMillis(reset);
    }
   reset_after = getSavedLong(map,"AFTER",reset_after);
   off_after = getSavedLong(map,"OFFAFTER",off_after);
   
   base_state = getSavedString(map,"BASESET",null);
   String bid = getSavedString(map,"BASESENSOR",null);
   base_sensor = getUniverse().findDevice(bid);
   String pnm = getSavedString(map,"BASEPARAM",null);
   if (pnm == null) pnm = base_sensor.getDataUID();
   base_parameter = base_sensor.findParameter(pnm);
   
   active_states = new HashMap<CatreWorld,StateRepr>();
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



private class SensorChanged implements CatreDeviceHandler, CatreConditionHandler {
   
   @Override public void stateChanged(CatreWorld w,CatreDevice s) {
      handleStateChanged(w);
    }
   
   @Override public void conditionOn(CatreWorld w,CatreCondition c,CatrePropertySet ps) {
      handleStateChanged(w);
    }
   
   @Override public void conditionOff(CatreWorld w,CatreCondition c) {
      handleStateChanged(w);
    }
   
   @Override public void conditionTrigger(CatreWorld w,CatreCondition c,CatrePropertySet ps) {
      handleStateChanged(w);
    }
   
   @Override public void conditionError(CatreWorld w,CatreCondition c,Throwable t) {
    }
   
}	// end of inner class SensorChanged




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




@Override public void apply(CatreTransition t,CatrePropertySet ps,CatreWorld w)
{
   if (t instanceof Reseter) resetLatch(w);
}




private class UniverseChanged implements CatreUniverseListener {
   
   @Override public void deviceRemoved(CatreUniverse u,CatreDevice s) {
      if (for_universe == u && base_sensor == s) {
         // remove this sensor as well
       }
    }
   
}       // end of inner class UniverseChanged



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
      boolean fg = false;
      try {
         Object ov = base_sensor.getValueInWorld(base_parameter,for_world);
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
      
      try {
         boolean fg = false;
         Object ov = base_sensor.getValueInWorld(base_parameter,for_world);
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



/********************************************************************************/
/*										*/
/*	Reset Transition							*/
/*										*/
/********************************************************************************/

private class Reseter extends CatdevTransition {
   
   Reseter() { 
      super(for_universe);
    }
   
   @Override public CatreTransitionType getTransitionType() {
      return CatreTransitionType.STATE_CHANGE; 
    }   

   @Override public String getName() {
      return CatdevSensorLatch.this.getName() + NSEP + "Reset";
    }
   
   @Override public String getDescription() {
      return "Reset " + CatdevSensorLatch.this.getLabel();
    }
   
   @Override public String getLabel() {
      return "Reset " + CatdevSensorLatch.this.getLabel();
    }
   
}	// end of inner class Reseter



/********************************************************************************/
/*										*/
/*	Parameter condition for a latch 					*/
/*										*/
/********************************************************************************/

private class LatchCondition extends CatmodelConditionParameter {
   
   LatchCondition(CatreParameter p,Object v,boolean trig) {
      super(CatdevSensorLatch.this,p,v,trig);
    }
   
   @Override public boolean isConsistentWith(CatreCondition bc) {
      if (!super.isConsistentWith(bc)) return false;
      if (bc instanceof CatmodelConditionParameter) {
         CatmodelConditionParameter sbc = (CatmodelConditionParameter) bc;
         if (sbc.getDevice() == base_sensor && sbc.getParameter() == base_parameter) {
            if (getState() == Boolean.TRUE) {
               if (sbc.getState() != base_state) return false;
             }
            else {
               if (sbc.getState() == base_state) return false;
             }
          }
       }
      
      return true;
    }
   
}	// end of inner class LatchCondition


}       // end of class CatdevSensorLatch




/* end of CatdevSensorLatch.java */

