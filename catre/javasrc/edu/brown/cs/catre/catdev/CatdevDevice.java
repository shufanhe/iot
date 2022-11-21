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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceHandler;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreSubSavableBase;
import edu.brown.cs.catre.catre.CatreTransition;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.catre.catre.CatreWorld;
import edu.brown.cs.ivy.swing.SwingEventListenerList;



public abstract class CatdevDevice extends CatreSubSavableBase implements CatreDevice, CatdevConstants
{



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected CatreUniverse	for_universe;
private boolean 	is_enabled;
private SwingEventListenerList<CatreDeviceHandler> device_handlers;
private String		device_uid;
private List<CatreParameter> parameter_set;
private List<CatreTransition> transition_set;
private Map<CatreParameter,Map<Object,CatreCondition>> cond_map;
private CatreBridge     for_bridge;




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
   device_handlers =
      new SwingEventListenerList<CatreDeviceHandler>(CatreDeviceHandler.class);
   
   device_uid = CatreUtil.randomString(24);
   is_enabled = true;
   parameter_set = new ArrayList<CatreParameter>();
   cond_map = new HashMap<CatreParameter,Map<Object,CatreCondition>>();
   transition_set = new ArrayList<CatreTransition>();
   for_bridge = null;
}




/********************************************************************************/
/*										*/
/*	Access methods						        	*/
/*										*/
/********************************************************************************/

@Override public CatreUniverse getUniverse()                   { return for_universe; }

@Override public abstract String getName();
@Override public String getDescription()        { return getName(); }


@Override public String getLabel()		{ return getName(); }

@Override public CatreBridge getBridge()        { return for_bridge; }


protected CatreWorld getCurrentWorld()
{
   return for_universe.getCurrentWorld();
}


@Override public String getCurrentStatus()
{
   return getCurrentStatus(getCurrentWorld());
}


@Override public String getCurrentStatus(CatreWorld w)
{
   CatreParameter bp = findParameter(getDataUID());
   if (bp != null) {
      Object val = w.getValue(bp);
      if (val != null) return val.toString();
    }
   
   if (parameter_set.size() == 1) {
      bp = parameter_set.get(0);
      Object val = w.getValue(bp);
      if (val != null) return val.toString();
    }
   
   if (parameter_set.size() > 1) {
      StringBuffer buf = new StringBuffer();
      for (CatreParameter up : parameter_set) {
	 Object val = w.getValue(up);
	 if (val != null) {
	    if (buf.length() > 0) buf.append(", ");
	    buf.append(up.getName());
	    buf.append("=");
	    buf.append(val.toString());
	  }
       }
      if (buf.length() > 0) return buf.toString();
    }
   
   return null;
}



@Override public boolean isDependentOn(CatreDevice d)
{
   return false;
}


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


public CatreParameter addParameter(CatreParameter p)
{
   for (CatreParameter up : parameter_set) {
      if (up.getName().equals(p.getName())) return up;
    }
   
   parameter_set.add(p);
   
   if (p.isSensor()) addConditions(p);
   
   return p;
}


@Override public CatreCondition getCondition(CatreParameter p,Object v)
{
   Map<Object,CatreCondition> m1 = cond_map.get(p);
   if (m1 == null) return null;
   v = p.normalize(v);
   return m1.get(v);
}


private void addConditions(CatreParameter p)
{
   List<Object> vals = p.getValues();
   if (vals != null) {
      for (Object v : vals) {
	 addCondition(p,v,false);
       }
    }
}


protected void addTriggerConditions(CatreParameter p)
{
   List<Object> vals = p.getValues();
   if (vals != null) {
      for (Object v : vals) {
	 addCondition(p,v,true);
       }
    }
}


protected CatreCondition addCondition(CatreParameter p,Object v,boolean trig)
{
   CatreCondition c = getCondition(p,v);
   if (c == null) {
      c = createParameterCondition(p,v,trig);
      Map<Object,CatreCondition> m1 = cond_map.get(p);
      if (m1 == null) {
	 m1 = new HashMap<Object,CatreCondition>();
	 cond_map.put(p,m1);
       }
      m1.put(v,c);
    }
   return c;
}



protected CatreCondition createParameterCondition(CatreParameter p,Object v,boolean trig)
{
   return for_universe.createParameterCondition(this,p,v,trig);
}


@Override public Collection<CatreCondition> getConditions()
{
   List<CatreCondition> rslt = new ArrayList<CatreCondition>();
   for (Map<Object,CatreCondition> m1 : cond_map.values()) {
      rslt.addAll(m1.values());
    }
   return rslt;
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


@Override public CatreTransition getTransition(CatreParameter p)
{
   return null;
}


CatreTransition createTransition(CatreStore cs,Map<String,Object> map)
{
   return null;
}


/********************************************************************************/
/*										*/
/*	Device handler commands 						*/
/*										*/
/********************************************************************************/

@Override public void addDeviceHandler(CatreDeviceHandler hdlr)
{
   device_handlers.add(hdlr);
}



@Override public void removeDeviceHandler(CatreDeviceHandler hdlr)
{
   device_handlers.remove(hdlr);
}


protected void fireChanged(CatreWorld w)
{
   w.startUpdate();
   try {
      for (CatreDeviceHandler hdlr : device_handlers) {
	 try {
	    hdlr.stateChanged(w,this);
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


/********************************************************************************/
/*										*/
/*	State update methods							*/
/*										*/
/********************************************************************************/

@Override public final void startDevice()
{
   localStartDevice();
}



protected void localStartDevice()
{ }



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
   
   fireChanged(w);
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




@Override public void enable(boolean fg)
{
   if (fg == is_enabled) return;
   
   CatreWorld w = getUniverse().getCurrentWorld();
   is_enabled = fg;
   fireChanged(w);
   
   if (fg) updateCurrentState();
}

@Override public boolean isEnabled()		{ return is_enabled ; }



/********************************************************************************/
/*										*/
/*	Transition methods							*/
/*										*/
/********************************************************************************/

@Override public void apply(CatreTransition t,CatrePropertySet ps,
      CatreWorld w) throws CatreActionException
   {
   throw new CatreActionException("Transition not allowed");
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
   is_enabled = getSavedBool(map,"ENABLED",is_enabled);
   
   if (parameter_set.isEmpty()) {
      parameter_set = getSavedSubobjectList(cs,map,"PARAMETERS",
            for_universe::createParameter,parameter_set);
    }
   
   String bnm = getSavedString(map,"BRIDGE",null);
   if (bnm != null) for_bridge = for_universe.findBridge(bnm);
}




@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("CLASS",getClass().getName());
   rslt.put("NAME",getName());
   rslt.put("DESC",getDescription());
   rslt.put("LABEL",getLabel());
   rslt.put("ENABLED",isEnabled());
   rslt.put("PARAMETERS",getSubObjectArrayToSave(parameter_set));
   if (for_bridge != null) rslt.put("BRIDGE",for_bridge.getName());
   
   return rslt;
}


}       // end of class CatdevDevice




/* end of CatdevDevice.java */

