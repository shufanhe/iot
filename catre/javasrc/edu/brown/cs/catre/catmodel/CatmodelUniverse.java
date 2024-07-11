/********************************************************************************/
/*										*/
/*		CatmodelUniverse.java						*/
/*										*/
/*	Container for universe -- everything for one location for one user	*/
/*										*/
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




package edu.brown.cs.catre.catmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONObject;

import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.catre.catdev.CatdevFactory;
import edu.brown.cs.catre.catprog.CatprogFactory;
import edu.brown.cs.catre.catre.CatreActionValues;
import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreTimeSlotEvent;
import edu.brown.cs.catre.catre.CatreTransitionRef;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterRef;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatreProgramListener;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreReferenceListener;
import edu.brown.cs.catre.catre.CatreSavable;
import edu.brown.cs.catre.catre.CatreSavedDescribableBase;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTriggerContext;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUniverseListener;
import edu.brown.cs.catre.catre.CatreUser;

public class CatmodelUniverse extends CatreSavedDescribableBase implements CatreUniverse, CatmodelConstants, 
        CatreSavable, CatreProgramListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private SwingEventListenerList<CatreUniverseListener> universe_callbacks;

private CatreController catre_control;
private CatreUser for_user;
private Set<CatreDevice> all_devices;
private CatreProgram universe_program;
private CatdevFactory device_factory;
private Map<String,CatreBridge> known_bridges;
private CatprogFactory program_factory;

private CatreParameterSet parameter_values;
private CatreTriggerContext trigger_context;
private int		  update_counter;
private ReentrantLock	  update_lock;
private Condition	  update_condition;


private boolean is_started;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatmodelUniverse(CatreController cc,String name,CatreUser cu)
{
   super(UNIVERSE_PREFIX);

   initialize(cc);

   for_user = cu;
   setName(name);
   universe_program = program_factory.createProgram();
   universe_program.addProgramListener(this);

   setupBridges();	// user must be set for this to be used

   for (CatreBridge cb : known_bridges.values()) {
      updateDevices(cb,true);
    }

   updateStored();
}



CatmodelUniverse(CatreController cc,CatreStore cs,Map<String,Object> map)
{
   super(cs);

   initialize(cc);
   
   if (universe_program != null) {
      universe_program.removeProgramListener(this);
    }

   fromJson(cs,map);
   
   Map<String,Object> unimap = toJson();
   JSONObject obj = new JSONObject(unimap);
   CatreLog.logD("CATMODEL","Load universe " + obj.toString(2));
   
   universe_program.addProgramListener(this);
}


private void initialize(CatreController cc)
{
   catre_control = cc;
   for_user = null;

   parameter_values = new CatmodelParameterSet(this);
   trigger_context = null;
   update_counter = 0;
   update_lock = new ReentrantLock();
   update_condition = update_lock.newCondition();

   device_factory = new CatdevFactory(this);
   program_factory = new CatprogFactory(this);

   all_devices = new LinkedHashSet<>();
   is_started = false;
   universe_callbacks = new SwingEventListenerList<CatreUniverseListener>(
	 CatreUniverseListener.class);

   known_bridges = new HashMap<>();
}



private void setupBridges()
{
   for (CatreBridge cb : catre_control.getAllBridges(this)) {
      known_bridges.put(cb.getName(),cb);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods for Describable						*/
/*										*/
/********************************************************************************/

@Override public CatreController getCatre()	{ return catre_control; }

@Override public CatreUser getUser()		{ return for_user; }


@Override public CatreProgram getProgram()	{ return universe_program; }



/********************************************************************************/
/*										*/
/*	Parameter and updating methods						*/
/*										*/
/********************************************************************************/


@Override public void updateLock()
{
   update_lock.lock();
}


@Override public void updateUnlock()
{
   update_lock.unlock();
}


@Override public void startUpdate()
{
   update_lock.lock();
   try {
      ++update_counter;
    }
   finally {
      update_lock.unlock();
    }
}



@Override public void endUpdate()
{
   update_lock.lock();
   try {
      --update_counter;
      if (update_counter == 0)
	 update_condition.signalAll();
    }
   finally {
      update_lock.unlock();
    }
}


@Override public CatreTriggerContext waitForUpdate()
{
   CatreLog.logD("CATMODEL","Wait for update " + 
         update_counter + " " + trigger_context);
   
   update_lock.lock();
   try {
      while (update_counter > 0) {
	 update_condition.awaitUninterruptibly();
       }
      CatreTriggerContext ctx = trigger_context;
      trigger_context = null;
      return ctx;
    }
   finally {
      update_lock.unlock();
    }
}


@Override public void setValue(CatreParameter p,Object val)
{
   parameter_values.putValue(p,val);
}


@Override public Object getValue(CatreParameter p)
{
   return parameter_values.getValue(p);
}


@Override public void addTrigger(CatreCondition c,CatrePropertySet ps)
{
   if (ps == null) ps = new CatmodelPropertySet();

   update_lock.lock();
   try {
      if (trigger_context == null) trigger_context = new CatmodelTriggerContext();
      trigger_context.addCondition(c,ps);
    }
   finally {
      update_lock.unlock();
    }
}



/********************************************************************************/
/*										*/
/*	Access methods for Saveable						*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
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

   // then get bridged devices
   for (CatreBridge cb : known_bridges.values()) {
      updateDevices(cb,true);
    }
   
   // then load the program
   universe_program = getSavedSubobject(store,map,"PROGRAM",this::createProgram,universe_program);
   if (universe_program == null) {
      universe_program = program_factory.createProgram();
    }

   updateStored();
}



/********************************************************************************/
/*										*/
/*	Update devices from bridges						*/
/*										*/
/********************************************************************************/

@Override public void updateDevices(boolean disable)
{
   for (CatreBridge cb : known_bridges.values()) {
      updateDevices(cb,disable);
    }
}


@Override public void updateDevices(CatreBridge cb,boolean disable)
{ 
   List<CatreDevice> toadd = new ArrayList<>();
   List<CatreDevice> toenable = new ArrayList<>();
   List<CatreDevice> todisable = new ArrayList<>();
   Map<String,CatreDevice> check = new HashMap<>();
   boolean chng = false;
   
   CatreLog.logD("CATMODEL","Start updating devices for " + cb.getName());

   for (CatreDevice cd : all_devices) {
      if (cd.getBridge() == cb) check.put(cd.getDeviceId(),cd);
    }
   Collection<CatreDevice> bdevs = cb.findDevices();
   if (bdevs == null) return;

   for (CatreDevice cd : bdevs) {
      CatreLog.logD("CATMODEL","Found device " + cd.getName() +  " " + cd.getDeviceId());
      if (check.remove(cd.getDeviceId()) == null) toadd.add(cd);
      else if (!cd.isEnabled()) toenable.add(cd);
    }
   todisable.addAll(check.values());

   for (CatreDevice cd : todisable) {
      if (disable) {
         CatreLog.logD("CATMODEL","Disable device " + cd.getName());
         if (cd.isEnabled()) chng = true;
         cd.setEnabled(false);
       }
      else {
         CatreLog.logD("CATMODEL","Remove device " + cd.getName());
         removeDevice(cd);
         fireDeviceRemoved(cd);
         chng = true;
       }
    }

   for (CatreDevice cd : toenable) {
      CatreLog.logD("CATMODEL","Enable device " + cd.getName());
      if (!cd.isEnabled()) chng = true;
      cd.setEnabled(true);
    }

   for (CatreDevice cd : toadd) {
      CatreLog.logD("CATMODEL","Add device " + cd.getName() + " " + cd.getDataUID());
      addDevice(cd);
      chng = true;
    }

   updateStored();
   
   CatreLog.logD("CATMODEL","Finish updating devices " + universe_program);
   
   if (universe_program != null) {
      universe_program.removeRule(null);           // triggers condition update for bridge
    }
   else if (chng) {
      fireUniverseSetup();
    }
}



/********************************************************************************/
/*										*/
/*	Model Access methods							*/
/*										*/
/********************************************************************************/

@Override public long getTime()
{
   return System.currentTimeMillis();
}



@Override public CatreDevice findDevice(String id)
{
   for (CatreDevice cd : all_devices) {
      if (cd.getDataUID().equals(id) || cd.getName().equalsIgnoreCase(id) || cd.getDeviceId().equals(id)) return cd;
    }
   return null;
}

@Override public void addDevice(CatreDevice cd)
{
   if (cd == null || all_devices.contains(cd)) return;

   CatreDevice olddev = findDevice(cd.getDeviceId());
   if (olddev != null) {
      CatreLog.logD("CATMODEL","Old device found: " + cd.getDeviceId());
      olddev.update(cd);
      return;
    }
   
   CatreLog.logD("CATMODEL","Add device " + cd.getName() + " to " + getName());

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
       updateDevices(cb,true);
     }
}



/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
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


@Override public CatreParameterRef createParameterRef(CatreReferenceListener ref,String deviceid,String param)
{
   return new CatmodelParameterRef(this,ref,deviceid,param);
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
   CatmodelParameter cm = CatmodelParameter.createBooleanParameter(this,uid);
   cm.setIsSensor(sensor);
   cm.setLabel(label);
   return cm;
}

@Override public CatreParameter createEnumParameter(String name,Enum<?> e)
{
   return CatmodelParameter.createEnumParameter(this,name,e);
}


@Override public CatreParameter createEnumParameter(String name,Iterable<String> vals)
{
   return CatmodelParameter.createEnumParameter(this,name,vals);
}


@Override public CatreParameter createSetParameter(String name,Iterable<String> vals)
{
   return CatmodelParameter.createSetParameter(this,name,vals);
}


@Override public CatreParameter createEnumParameter(String name,String [] v)
{
   return CatmodelParameter.createEnumParameter(this,name,v);
}


@Override public CatreParameter createIntParameter(String name,int min,int max)
{
   return CatmodelParameter.createIntParameter(this,name,min,max);
}


@Override public CatreParameter createRealParameter(String name,double min,double max)
{
   return CatmodelParameter.createRealParameter(this,name,min,max);
}


@Override public CatreParameter createRealParameter(String name)
{
   return CatmodelParameter.createRealParameter(this,name);
}


@Override public CatreParameter createColorParameter(String name)
{
   return CatmodelParameter.createColorParameter(this,name);
}


@Override public CatreParameter createStringParameter(String name)
{
   return CatmodelParameter.createStringParameter(this,name);
}


@Override public CatreParameter createEventsParameter(String name)
{
   return CatmodelParameter.createEventsParameter(this,name);
}


/********************************************************************************/
/*										*/
/*	Run methods								*/
/*										*/
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


private void updateStored()
{
   catre_control.getDatabase().saveObject(this);
}



/********************************************************************************/
/*										*/
/*	Listener methods							*/
/*										*/
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


protected void fireUniverseSetup() 
{
   CatreLog.logD("CATMODEL","Fire universe setup");
   
   for (CatreUniverseListener ul : universe_callbacks) {
      ul.universeSetup(); 
    }
}



@Override public void programUpdated()
{
   // save when program updated
   updateStored();
}


}	// end of class CatmodelUniverse




/* end of CatmodelUniverse.java */

