/********************************************************************************/
/*                                                                              */
/*              CatmodelUniverse.java                                           */
/*                                                                              */
/*      Container for universe -- everything for one location for one user      */
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.catre.catdev.CatdevFactory;
import edu.brown.cs.catre.catprog.CatprogFactory;
import edu.brown.cs.catre.catre.CatreActionValues;
import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreTimeSlotEvent;
import edu.brown.cs.catre.catre.CatreTransitionRef;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterRef;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreReferenceListener;
import edu.brown.cs.catre.catre.CatreSavable;
import edu.brown.cs.catre.catre.CatreSavedDescribableBase;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTriggerContext;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUniverseListener;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.catre.catre.CatreWorld;

public class CatmodelUniverse extends CatreSavedDescribableBase implements CatreUniverse, CatmodelConstants, CatreSavable
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SwingEventListenerList<CatreUniverseListener> universe_callbacks;

private CatreController catre_control;
private CatreUser for_user;
private Set<CatreDevice> all_devices;
private CatreProgram universe_program;
private CatreWorld current_world;
private CatdevFactory device_factory;
private Map<String,CatreBridge> known_bridges;
private Map<String,CatreWorld> known_worlds;
private CatprogFactory program_factory;

private boolean is_started;


      

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatmodelUniverse(CatreController cc,String name,CatreUser cu)
{
   super(UNIVERSE_PREFIX);
   
   initialize(cc);
   
   for_user = cu;
   setName(name);
   universe_program = program_factory.createProgram();
   
   setupBridges();      // user must be set for this to be used
   
   for (CatreBridge cb : known_bridges.values()) {
      updateDevices(cb);
    }
   
   update();
}



CatmodelUniverse(CatreController cc,CatreStore cs,Map<String,Object> map)
{
   super(cs);
   
   initialize(cc);
   
   fromJson(cs,map);
}


private void initialize(CatreController cc)
{
   catre_control = cc;
   for_user = null;
   current_world = null;
   device_factory = new CatdevFactory(this);
   program_factory = new CatprogFactory(this);
   
   all_devices = new LinkedHashSet<>();
   is_started = false;
   universe_callbacks = new SwingEventListenerList<CatreUniverseListener>(
	 CatreUniverseListener.class);
   
   known_bridges = new HashMap<>();
   known_worlds = new HashMap<>();
}



private void setupBridges()
{   
   for (CatreBridge cb : catre_control.getAllBridges(this)) {
      known_bridges.put(cb.getName(),cb);
    } 
}







/********************************************************************************/
/*                                                                              */
/*      Access methods for Describable                                          */
/*                                                                              */
/********************************************************************************/

@Override public CatreController getCatre()     { return catre_control; }

@Override public synchronized CatreWorld getCurrentWorld()
{
   if (current_world == null) {
      current_world = new CatmodelWorldCurrent(this);
    }
   return current_world;
}


@Override public CatreWorld findWorld(String id)
{
   if (id == null) return getCurrentWorld();
   
   return known_worlds.get(id);
}


@Override public CatreWorld createWorld(CatreWorld base)
{
   if (base == null) base = getCurrentWorld();
   
   CatmodelWorld cw = (CatmodelWorld) base;
   
   CatreWorld newcw = cw.createClone();
   
   known_worlds.put(newcw.getUID(),cw);
   
   return newcw;
}


@Override public CatreWorld removeWorld(CatreWorld w)
{
   if (w == null) return null;
   
   return known_worlds.remove(w.getUID());
}


@Override public CatreUser getUser()            { return for_user; }


@Override public CatreProgram getProgram()      { return universe_program; }



/********************************************************************************/
/*                                                                              */
/*      Access methods for Saveable                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   rslt.put("NAME",getName());
   rslt.put("LABEL",getLabel());
   rslt.put("DESCRIPTION",getDescription());
   rslt.put("DEVICES",getSubObjectArrayToSave(all_devices));
   if (universe_program == null) rslt.put("PROGRAM",null);
   else rslt.put("PROGRAM",universe_program.toJson());
   rslt.put("USER_ID",getUIDToSave(for_user));
   rslt.put("BRIDGES",known_bridges.keySet());
   
   return rslt;
}


@Override public void fromJson(CatreStore store,Map<String,Object> map)
{ 
   super.fromJson(store,map);
   
   for_user = getSavedObject(store,map,"USER_ID",for_user);
   
   setupBridges();
    
   // load devices first
   if (all_devices == null) all_devices = new LinkedHashSet<>();
   Set<CatreDevice> devs = getSavedSubobjectSet(store,map,"DEVICES",this::createAnyDevice, all_devices);
   for (CatreDevice dev : devs) {
      addDevice(dev);
    }
   
   // then load the program
   universe_program = getSavedSubobject(store,map,"PROGRAM",this::createProgram,universe_program);
   if (universe_program == null) {
      universe_program = program_factory.createProgram();
    }
   
   for (CatreBridge cb : known_bridges.values()) {
      updateDevices(cb);
    }
   
   update();
}



/********************************************************************************/
/*                                                                              */
/*      Update devices from bridges                                             */
/*                                                                              */
/********************************************************************************/

@Override public void updateDevices(CatreBridge cb)
{
   List<CatreDevice> toadd = new ArrayList<>();
   List<CatreDevice> toenable = new ArrayList<>();
   List<CatreDevice> todisable = new ArrayList<>();
   Map<String,CatreDevice> check = new HashMap<>();
   
   for (CatreDevice cd : all_devices) {
      if (cd.getBridge() == cb) check.put(cd.getDeviceId(),cd);
    }
   Collection<CatreDevice> bdevs = cb.findDevices();
   if (bdevs == null) return;
   
   for (CatreDevice cd : bdevs) {
      if (check.remove(cd.getDeviceId()) == null) toadd.add(cd);
      else if (!cd.isEnabled()) toenable.add(cd);
    }
   todisable.addAll(check.values());
   
   for (CatreDevice cd : todisable) {
      CatreLog.logD("CATMODEL","Disable device " + cd.getName());
      cd.setEnabled(false);
      fireDeviceRemoved(cd);
    }
   
   for (CatreDevice cd : toenable) {
      CatreLog.logD("CATMODEL","Enable device " + cd.getName());
      cd.setEnabled(true);
      fireDeviceRemoved(cd);
    }
   
   for (CatreDevice cd : toadd) {
      CatreLog.logD("CATMODEL","Add device " + cd.getName() + " " + cd.getDataUID());
      addDevice(cd);
    }
   
   update();
}



/********************************************************************************/
/*                                                                              */
/*      Model Access methods                                                    */
/*                                                                              */
/********************************************************************************/

@Override public Collection<CatreDevice> getDevices()
{
   return new ArrayList<>(all_devices);
}



@Override public CatreDevice findDevice(String id)
{
   for (CatreDevice cd : all_devices) {
      CatreLog.logD("FIND DEVICE " + id + " " + cd.getDeviceId());
      if (cd.getDataUID().equals(id) || cd.getName().equalsIgnoreCase(id) || cd.getDeviceId().equals(id)) return cd;
    }
   return null;
}

@Override public void addDevice(CatreDevice cd)
{
   if (cd == null || all_devices.contains(cd)) return;
   
   CatreDevice olddev = findDevice(cd.getDeviceId());
   if (olddev != null) return;
   
   all_devices.add(cd);
   
   cd.startDevice();
   
   fireDeviceAdded(cd);
}


@Override public void removeDevice(CatreDevice cd)
{
   if (!all_devices.remove(cd)) return;
   
   fireDeviceRemoved(cd);
}


@Override public CatreBridge findBridge(String name)
{
   return known_bridges.get(name);
}


@Override public void addBridge(String name)
{
    CatreBridge cb = catre_control.createBridge(name,this);
    if (cb != null) {
       known_bridges.put(name,cb);
       updateDevices(cb);
     }
}



/********************************************************************************/
/*                                                                              */
/*      Creation methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public CatreParameterSet createParameterSet()
{
   return new CatmodelParameterSet(this);
}


@Override public CatreParameterSet createParameterSet(CatreStore cs,Map<String,Object> map)
{
   CatmodelParameterSet ps = new CatmodelParameterSet(this);
   
   ps.fromJson(cs,map);
   
   return ps;
}


@Override public CatreParameterSet createSavedParameterSet(CatreStore cs,Map<String,Object> map)
{
   CatmodelParameterSet pset = new CatmodelParameterSet(this);
   pset.fromJson(cs,map);
   return pset;
}


@Override public CatrePropertySet createPropertySet()
{
   return new CatmodelPropertySet();
}

@Override public CatreActionValues createActionValues(CatreParameterSet params)
{
   CatreActionValues cav = new CatmodelActionValues(params);
   return cav;
}



private CatreProgram createProgram(CatreStore cs,Map<String,Object> map)
{
   return program_factory.createProgram(cs,map);
}

@Override public CatreTriggerContext createTriggerContext()
{
   return new CatmodelTriggerContext();
}

@Override public CatreTimeSlotEvent createCalendarEvent(CatreStore cs,Map<String,Object> map)
{
   return new CatmodelCalendarEvent(cs,map); 
}


@Override public CatreParameterRef createParameterRef(CatreReferenceListener ref,String device,String param)
{
   return new CatmodelParameterRef(this,ref,device,param);
}

@Override public CatreParameterRef createParameterRef(CatreReferenceListener ref,CatreStore cs,Map<String,Object> map)
{
   return new CatmodelParameterRef(this,ref,cs,map);
}


@Override public CatreTransitionRef createTransitionRef(CatreReferenceListener ref,String device,String transition)
{
   return new CatmodelTransitionRef(this,ref,device,transition);
}

@Override public CatreTransitionRef createTransitionRef(CatreReferenceListener ref,CatreStore cs,Map<String,Object> map)
{
   return new CatmodelTransitionRef(this,ref,cs,map);
}


@Override public CatreDevice createVirtualDevice(CatreStore cs,Map<String,Object> map)
{
   CatreDevice cd = null;
   Object bridge = map.get("BRIDGE");
   if (bridge != null) return null;
   
   CatreLog.logD("CREATE VIRTUAL DEVICE " + map);
   
   try {
      cd = device_factory.createDevice(cs,map);
    }
   catch (Throwable t) {
      CatreLog.logE("CATMODEL","Problem creating device",t);
    }
   
   if (cd != null) addDevice(cd);
   
   return cd;
}

private CatreDevice createAnyDevice(CatreStore cs,Map<String,Object> map)
{
   CatreDevice cd = null;
   String bridge = (String) map.get("BRIDGE");
   if (bridge != null) {
      CatreBridge cb = known_bridges.get(bridge);
      if (cb != null) cd = cb.createDevice(cs,map);
    }
   else {
      cd = device_factory.createDevice(cs,map);
    }
   
   if (cd != null && !cd.validateDevice()) cd = null;
   
   return cd;
}



@Override public CatreParameter createParameter(CatreStore cs,Map<String,Object> map)
{
   CatreLog.logD("CATMODEL","Create parameter " + map);
   return CatmodelParameter.createParameter(this,cs,map);
}




@Override public CatreParameter createBooleanParameter(String uid,boolean sensor,String label)
{
   CatmodelParameter cm = CatmodelParameter.createBooleanParameter(uid);
   cm.setIsSensor(sensor);
   cm.setLabel(label);
   return cm;
}

@Override public CatreParameter createEnumParameter(String name,Enum<?> e)
{
   return CatmodelParameter.createEnumParameter(name,e);
}


@Override public CatreParameter createEnumParameter(String name,Iterable<String> vals)
{
   return CatmodelParameter.createEnumParameter(name,vals);
}


@Override public CatreParameter createSetParameter(String name,Iterable<String> vals)
{
   return CatmodelParameter.createSetParameter(name,vals);
}


@Override public CatreParameter createEnumParameter(String name,String [] v)
{
   return CatmodelParameter.createEnumParameter(name,v);
}


@Override public CatreParameter createIntParameter(String name,int min,int max)
{
   return CatmodelParameter.createIntParameter(name,min,max);
}


@Override public CatreParameter createRealParameter(String name,double min,double max)
{
   return CatmodelParameter.createRealParameter(name,min,max);
}


@Override public CatreParameter createRealParameter(String name)
{
   return CatmodelParameter.createRealParameter(name);
}


@Override public CatreParameter createColorParameter(String name)
{
   return CatmodelParameter.createColorParameter(name);
}


@Override public CatreParameter createStringParameter(String name)
{
   return CatmodelParameter.createStringParameter(name);
}


@Override public CatreParameter createEventsParameter(String name)
{
   return CatmodelParameter.createEventsParameter(name);
}


/********************************************************************************/
/*                                                                              */
/*      Run methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public void start()
{
   if (is_started) return;
   is_started = true;
   
   //TODO: check program to ensure it remains valid
   
   for (CatreDevice cd : all_devices) {
      cd.startDevice();
    }
}


private void update()
{ 
   catre_control.getDatabase().saveObject(this);
}



/********************************************************************************/
/*                                                                              */
/*      Listener methods                                                        */
/*                                                                              */
/********************************************************************************/


@Override public void addUniverseListener(CatreUniverseListener l)
{
   universe_callbacks.add(l);
}


@Override public void removeUniverseListener(CatreUniverseListener l)
{
   universe_callbacks.remove(l);
}


protected void fireDeviceAdded(CatreDevice e)
{
   for (CatreUniverseListener ul : universe_callbacks) {
      ul.deviceAdded(e);
    }
}

protected void fireDeviceRemoved(CatreDevice e)
{
   for (CatreUniverseListener ul : universe_callbacks) {
      ul.deviceRemoved(e);
    }
}




}       // end of class CatmodelUniverse




/* end of CatmodelUniverse.java */

