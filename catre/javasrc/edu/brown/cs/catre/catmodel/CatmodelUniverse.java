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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreSavable;
import edu.brown.cs.catre.catre.CatreSavableBase;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUniverseListener;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.catre.catre.CatreWorld;

public class CatmodelUniverse extends CatreSavableBase implements CatreUniverse, CatmodelConstants, CatreSavable
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SwingEventListenerList<CatreUniverseListener> universe_callbacks;

private CatreController catre_control;
private CatreUser for_user;
private String  universe_name;
private String  universe_description;
private String  universe_label;
private Set<CatreDevice> all_devices;
private List<CatreCondition> all_conditions;
private CatreProgram universe_program;
private CatreWorld current_world;
private Map<String,CatreBridge> known_bridges;


private boolean is_started;


      

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatmodelUniverse(CatreController cc,String name)
{
   super(UNIVERSE_PREFIX);
   
   initialize(cc);
   
   universe_name = name;
   universe_description = name;
   universe_label = name;
}



CatmodelUniverse(CatreController cc,Map<String,Object> map)
{
   super(cc.getDatabase());
   
   initialize(cc);
   
   fromJson(cc.getDatabase(),map);
   
   setupBridges();
}


private void initialize(CatreController cc)
{
   catre_control = cc;
   for_user = null;
   universe_name = null;
   universe_description = null;
   universe_label = null;
   current_world = null;
   
   all_devices = new LinkedHashSet<>();
   all_conditions = new ArrayList<>();
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
/*                                                                              */
/*      Access methods for Describable                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getName()               { return universe_name; }

@Override public String getDescription()        { return universe_description; }

@Override public String getLabel()              { return universe_label; }

@Override public void setLabel(String label)    { universe_label = label; }

@Override public void setDescription(String d)  { universe_description = d; }

@Override public CatreController getCatre()     { return catre_control; }

@Override public synchronized CatreWorld getCurrentWorld()
{
   if (current_world == null) {
      current_world = new CatmodelWorldCurrent(this);
    }
   return current_world;
}


@Override public CatreUser getUser()            { return for_user; }

@Override public void setUser(CatreUser cu)     
{
   if (for_user == null) for_user = cu;
}



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
   universe_name = getSavedString(map,"NAME",universe_name);
   universe_label = getSavedString(map,"LABEL",universe_label);
   universe_description = getSavedString(map,"DESCRIPTION",universe_description);
   
   // load bridges
   List<String> bnames = getSavedStringList(map,"BRIDGES",new ArrayList<String>());
   for (String bname : bnames) {
      CatreBridge cb = catre_control.createBridge(bname,this);
      if (cb != null) known_bridges.put(bname,cb);
    }
    
   // load devices first
   if (all_devices == null) all_devices = new LinkedHashSet<>();
   all_devices = getSavedSubobjectSet(store,map,"DEVICES",this::createDevice, all_devices);
   // finally load the program
   universe_program = getSavedSubobject(store,map,"PROGRAM",this::createProgram,universe_program);
   for_user = getSavedObject(store,map,"USER_ID",for_user);
   
   updateDevices();
}



/********************************************************************************/
/*                                                                              */
/*      Update devices from bridges                                             */
/*                                                                              */
/********************************************************************************/

public void updateDevices()
{
   List<CatreDevice> toadd = new ArrayList<>();
   List<CatreDevice> todel = new ArrayList<>();
   
   for (CatreBridge cb : known_bridges.values()) {
      Set<CatreDevice> check = new HashSet<>();
      for (CatreDevice cd : all_devices) {
         if (cd.getBridge() == cb) check.add(cd);
       }
      Collection<CatreDevice> bdevs = cb.findDevices();
      if (bdevs == null) continue;
      for (CatreDevice cd : bdevs) {
         if (!check.remove(cd)) toadd.add(cd);
       }
      todel.addAll(check);
    }
   
   for (CatreDevice cd : todel) {
      all_devices.remove(cd);
      fireDeviceRemoved(cd);
    }
   
   for (CatreDevice cd : toadd) {
      all_devices.add(cd);
      fireDeviceAdded(cd);
    }
}


@Override public void updateDevices(CatreBridge cb)
{
   List<CatreDevice> toadd = new ArrayList<>();
   List<CatreDevice> todel = new ArrayList<>();
   
   Set<CatreDevice> check = new HashSet<>();
   for (CatreDevice cd : all_devices) {
      if (cd.getBridge() == cb) check.add(cd);
    }
   Collection<CatreDevice> bdevs = cb.findDevices();
   if (bdevs == null) return;
   
   for (CatreDevice cd : bdevs) {
      if (!check.remove(cd)) toadd.add(cd);
    }
   todel.addAll(check);
   
   for (CatreDevice cd : todel) {
      all_devices.remove(cd);
      fireDeviceRemoved(cd);
    }
   
   for (CatreDevice cd : toadd) {
      all_devices.add(cd);
      fireDeviceAdded(cd);
    }
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
      if (cd.getDataUID().equals(id) || cd.getName().equalsIgnoreCase(id)) return cd;
    }
   return null;
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
       updateDevices();
     }
}


@Override public Collection<CatreCondition> getBasicConditions()
{
   return new ArrayList<>(all_conditions);
}



@Override public CatreCondition createParameterCondition(CatreDevice device,
      CatreParameter parameter,Object value,boolean istrigger)
{
   return new CatmodelConditionParameter(device,parameter,value,istrigger);
}


@Override public CatreCondition findCondition(String id)
{
   for (CatreCondition cd : all_conditions) {
      if (cd.getDataUID().equals(id) || cd.getName().equalsIgnoreCase(id)) return cd;
    }
   
   return null;
}



public CatreCondition addCondition(CatreCondition newcc) 
{
   CatreCondition cc = findCondition(newcc.getDataUID());
   if (cc != null) return cc;
   
   all_conditions.add(newcc);
   
   fireConditionAdded(newcc);
   
   return newcc;
}


@Override public CatreParameterSet createParameterSet()
{
   return new CatmodelParameterSet(this);
}


@Override public CatreParameterSet createSavedParameterSet(CatreStore cs,Map<String,Object> map)
{
   CatmodelParameterSet pset = new CatmodelParameterSet(this);
   pset.fromJson(cs,map);
   return pset;
}


public CatrePropertySet createPropertySet()
{
   return new CatmodelPropertySet();
}


/********************************************************************************/
/*                                                                              */
/*      Creation methods                                                        */
/*                                                                              */
/********************************************************************************/

private CatreProgram createProgram(CatreStore cs,Map<String,Object> map)
{
   return null;
}





public CatreDevice createDevice(CatreStore cs,Map<String,Object> map)
{
   CatreDevice cd = null;
   String bridge = map.get("BRIDGE").toString();
   if (bridge != null) {
      CatreBridge cb = findBridge(bridge);
      if (cb == null) return null;
      cd = cb.createDevice(cs,map);
      if (cd != null) return cd;
    }
   
   try {
      String cnm = map.get("CLASS").toString();
      Class<?> c = Class.forName(cnm);
      try {
         Constructor<?> cnst = c.getConstructor(CatreUniverse.class,
               CatreStore.class,Map.class);
         cd = (CatreDevice) cnst.newInstance(this,cs,map);
       }
      catch (Exception e) { }
      if (cd == null) {
         try {
            Constructor<?> cnst = c.getConstructor(CatreUniverse.class);
            cd = (CatreDevice) cnst.newInstance(this);
            cd.fromJson(cs,map);
          }
         catch (Exception e) { }
       }
    }
   catch (Exception e) { }
   
   return cd;
}



@Override public CatreParameter createParameter(CatreStore cs,Map<String,Object> map)
{
   return CatmodelParameter.createParameter(cs,map);
}

@Override public CatreParameter createDateTimeParameter(String nm)
{
   return CatmodelParameter.createDateTimeParameter(nm);
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






/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public void discover()
{
   
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
   
   for (CatreDevice cd : all_devices) {
      for (CatreCondition cc : cd.getConditions()) {
         addCondition(cc);
       }
      cd.startDevice();
    }
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
      ul.deviceAdded(this,e);
    }
}

protected void fireDeviceRemoved(CatreDevice e)
{
   for (CatreUniverseListener ul : universe_callbacks) {
      ul.deviceRemoved(this,e);
    }
}


protected void fireConditionAdded(CatreCondition c)
{
   for (CatreUniverseListener ul : universe_callbacks) {
      ul.conditionAdded(this,c);
    }
}

protected void fireConditionRemoved(CatreCondition c)
{
   for (CatreUniverseListener ul : universe_callbacks) {
      ul.conditionRemoved(this,c);
    }
}



}       // end of class CatmodelUniverse




/* end of CatmodelUniverse.java */

