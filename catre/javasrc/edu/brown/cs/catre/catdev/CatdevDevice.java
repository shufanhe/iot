/********************************************************************************/
/*                                                                              */
/*              CatdevDevice.java                                               */
/*                                                                              */
/*      Basic device implementation                                             */
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreDescribableBase;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceListener;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreReferenceListener;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTransition;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.catre.catre.CatreWorld;
import edu.brown.cs.ivy.swing.SwingEventListenerList;



public abstract class CatdevDevice extends CatreDescribableBase implements CatreDevice, 
      CatdevConstants, CatreReferenceListener
{



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected CatreUniverse	for_universe;
private Boolean 	is_enabled;
private SwingEventListenerList<CatreDeviceListener> device_handlers;
private List<CatreParameter> parameter_set;
private List<CatreTransition> transition_set;
private CatreBridge     for_bridge;
private String		device_uid;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatdevDevice(CatreUniverse uu)
{
   this(uu,null);
}


protected CatdevDevice(CatreUniverse uu,CatreBridge bridge)
{
   super("DEV_");
   initialize(uu);
   for_bridge = bridge;
}



private void initialize(CatreUniverse uu)
{
   for_universe = uu;
   device_handlers = new SwingEventListenerList<>(CatreDeviceListener.class);
   
   device_uid = CatreUtil.randomString(24);
   setName("Unknown device");
   is_enabled = true;
   parameter_set = new ArrayList<>();
   transition_set = new ArrayList<>();
   for_bridge = null;
}




/********************************************************************************/
/*										*/
/*	Access methods						        	*/
/*										*/
/********************************************************************************/

@Override public CatreUniverse getUniverse()    { return for_universe; }
@Override public CatreBridge getBridge()        { return for_bridge; }


protected CatreWorld getCurrentWorld()
{
   return for_universe.getCurrentWorld();
}



@Override public boolean isDependentOn(CatreDevice d)
{
   return false;
}


@Override public String getDeviceId()                  { return device_uid; }

protected void setDeviceId(String did)          { device_uid = did; }



/********************************************************************************/
/*										*/
/*	Parameter methods							*/
/*										*/
/********************************************************************************/

@Override public Collection<CatreParameter> getParameters()
{
   return parameter_set;
}


@Override public CatreParameter findParameter(String id)
{
   if (id == null) return null;
   
   for (CatreParameter up : parameter_set) {
      if (up.getName().equals(id)) return up;
      if (up.getLabel().equals(id)) return up;
    }
   
   return null;
}


@Override public CatreTransition findTransition(String id)
{
   for (CatreTransition ct : transition_set) {
      if (ct.getName().equals(id))
         return ct;
    }
   return null;
}



public CatreParameter addParameter(CatreParameter p)
{
   for (CatreParameter up : parameter_set) {
      if (up.getName().equals(p.getName())) return up;
    }
   
   parameter_set.add(p);
   
   return p;
}



public CatreTransition addTransition(CatreTransition t)
{
   for (CatreTransition ut : transition_set) {
      if (ut.getName().equals(t.getName())) return ut;
    }
   transition_set.add(t);
   return t;
}


@Override public Collection<CatreTransition> getTransitions()
{
   return transition_set;
}


@Override public boolean hasTransitions()
{
   if (transition_set == null || transition_set.size() == 0) return false;
   return true;
}





@Override public CatreTransition createTransition(CatreStore cs,Map<String,Object> map)
{
   CatreTransition cd = null;
   if (for_bridge != null) {
      cd = for_bridge.createTransition(this,cs,map);
      if (cd != null) return cd;
    }
   
   cd = new CatdevTransition(this,cs,map);
   
   return cd; 
}



/********************************************************************************/
/*										*/
/*	Device handler commands 						*/
/*										*/
/********************************************************************************/

@Override public void addDeviceListener(CatreDeviceListener hdlr)
{
   device_handlers.add(hdlr);
}


@Override public void removeDeviceListener(CatreDeviceListener hdlr)
{
   device_handlers.remove(hdlr);
}


protected void fireChanged(CatreWorld w,CatreParameter p)
{
   w.startUpdate();
   try {
      for (CatreDeviceListener hdlr : device_handlers) {
	 try {
	    hdlr.stateChanged(w);
	  }
	 catch (Throwable t) {
	    CatreLog.logE("CATMODEL","Problem with device handler",t);
	  }
       }
    }
   finally {
      w.endUpdate();
    }
}



protected void fireEnabled()
{
   for (CatreDeviceListener hdlr : device_handlers) {
      try {
         hdlr.deviceEnabled(this,is_enabled);
       }
      catch (Throwable t) {
         CatreLog.logE("CATMODEL","Problem with device handler",t);
       }
    }
}


/********************************************************************************/
/*										*/
/*	State update methods							*/
/*										*/
/********************************************************************************/

@Override public final void startDevice()
{
   setEnabled(isDeviceValid());
}

protected boolean isDeviceValid()                       { return true; }

protected void localStartDevice()                       { }

protected void localStopDevice()                        { }



@Override public Object getValueInWorld(CatreParameter p,CatreWorld w)
{
   if (!isEnabled()) return null;
   if (w == null) w = getCurrentWorld();
   
   if (w.isCurrent()) {
      checkCurrentState();
      return w.getValue(p);
    }
   else {
      updateWorldState(w);
      return w.getValue(p);
    }
}



@Override public void setValueInWorld(CatreParameter p,Object val,CatreWorld w)
{
   if (!isEnabled()) return;
   if (w == null) w = getCurrentWorld();
   
   val = p.normalize(val);
   
   CatreParameter timep = getTimeParameter(p);
   
   Object prev = getValueInWorld(p,w);
   if ((val == null && prev == null) || (val != null && val.equals(prev))) {
      if (w.isCurrent()) return;
      if (timep == null) return;
      Object v = w.getValue(timep);
      if (v == null) return;
      Calendar c = (Calendar) v;
      long tm = c.getTimeInMillis();
      if (tm <= w.getTime()) return;
    }
   
   w.setValue(p,val);
   if (timep != null) w.setValue(timep,w.getTime());
   
   CatreLog.logI("CATMODEL","Set " + getName() + "." + p + " = " + val);
   
   fireChanged(w,p);
}



protected void checkCurrentState()		{ updateCurrentState(); }
protected void updateCurrentState()		{ }
protected void updateWorldState(CatreWorld w)	{ }

protected CatreParameter getTimeParameter(CatreParameter p)
{
   String nm = p.getName() + "_TIME";
   for (CatreParameter up : parameter_set) {
      if (up.getName().equals(nm)) return up;
    }
   
   return null;
}

protected CatreParameter createTimeParameter(CatreParameter p)
{
   String nm = p.getName() + "_TIME";
   CatreParameter up = for_universe.createDateTimeParameter(nm);
   up = addParameter(up);
   return up;
}




@Override public void setEnabled(boolean fg)
{
   if (fg == is_enabled) return;
   
   is_enabled = fg;
   
   if (fg) {
      localStartDevice();
    }
   else {
      localStopDevice();
    }
   
   fireEnabled();
}


@Override public boolean isEnabled()		{ return is_enabled; }


@Override public void referenceValid(boolean fg)
{
   setEnabled(fg);
}



/********************************************************************************/
/*										*/
/*	Transition methods							*/
/*										*/
/********************************************************************************/

@Override public void apply(CatreTransition t,Map<String,Object> vals,
      CatreWorld w) throws CatreActionException
{
   if (for_bridge != null) {
      for_bridge.applyTransition(this,t,vals,w);
    }
   else {
      throw new CatreActionException("Transition not allowed");
    }
}



/********************************************************************************/
/*										*/
/*	Database methods 							*/
/*										*/
/********************************************************************************/

@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   device_uid = getSavedString(map,"UID",device_uid);
  
   is_enabled = true;
   String bnm = getSavedString(map,"BRIDGE",null);
   if (bnm != null) {
      for_bridge = for_universe.findBridge(bnm);
      if (for_bridge == null) is_enabled = false;
    }
   
   List<CatreParameter> plst = getSavedSubobjectList(cs,map,"PARAMETERS",
         for_universe::createParameter,parameter_set);
   for (CatreParameter p : plst) {
      addParameter(p);  
    }
   
   transition_set = getSavedSubobjectList(cs,map,"TRANSITIONS",
         this::createTransition,transition_set);
}




@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   if (for_bridge != null) rslt.put("BRIDGE",for_bridge.getName());
   else rslt.put("CLASS",getClass().getName());
   
   rslt.put("UID",device_uid);
   rslt.put("ENABLED",isEnabled());
   rslt.put("PARAMETERS",getSubObjectArrayToSave(parameter_set));
   rslt.put("TRANSITIONS",getSubObjectArrayToSave(transition_set));
   
   CatreLog.logD("CATDEV","Device yields " + rslt);
   
   return rslt;
}


}       // end of class CatdevDevice




/* end of CatdevDevice.java */

