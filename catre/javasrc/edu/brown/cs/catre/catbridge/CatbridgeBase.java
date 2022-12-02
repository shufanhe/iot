/********************************************************************************/
/*                                                                              */
/*              CatbridgeBase.java                                              */
/*                                                                              */
/*      Base implementation of a CatreBridge                                    */
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



package edu.brown.cs.catre.catbridge;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreBridgeAuthorization;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTransition;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.catre.catre.CatreWorld;

abstract class CatbridgeBase implements CatreBridge, CatbridgeConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<CatreUniverse,CatbridgeBase> known_instances;

protected CatreUniverse         for_universe;
protected Map<String,CatreDevice> device_map;
protected String                bridge_id;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected CatbridgeBase()
{
   for_universe = null;
   device_map = null;
   known_instances = new HashMap<>();
   bridge_id = null;
}

protected CatbridgeBase(CatbridgeBase base,CatreUniverse cu)
{
   for_universe = cu;
   device_map = new HashMap<>();
   known_instances = null;
   bridge_id = CatreUtil.randomString(24);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

CatreUniverse getUniverse()             { return for_universe; }

@Override public String getBridgeId()   { return bridge_id; }



/********************************************************************************/
/*                                                                              */
/*      Methods to create an instance                                           */
/*                                                                              */
/********************************************************************************/

protected CatbridgeBase createBridge(CatreUniverse u)
{
   if (for_universe != null) return null;
   
   CatbridgeBase cb = known_instances.get(u);
   
   CatreUser cu = u.getUser();
   CatreBridgeAuthorization ba = cu.getAuthorization(getName());
   if (ba == null) {
      if (cb != null) known_instances.remove(u);
      return null;
    }
   
   if (cb == null) cb = createInstance(u,ba);
   
   return cb;
}


abstract protected CatbridgeBase createInstance(CatreUniverse u,CatreBridgeAuthorization auth);



/********************************************************************************/
/*                                                                              */
/*      Methods to talk to CEDES                                                */
/*                                                                              */
/********************************************************************************/

protected void registerBridge()
{ 
   Map<String,Object> authdata = getAuthData();
   Map<String,Object> data = new HashMap<>();
   
   data.put("bridgeid",getBridgeId());
   data.put("authdata",new JSONObject(authdata));
   data.put("bridge",getName().toLowerCase());
   
   sendCedesMessage("catre/addBridge",data);
}


protected Map<String,Object> getAuthData()
{
   return new HashMap<>();
}



protected JSONObject sendCedesMessage(String cmd,Map<String,Object> data)
{
   if (!cmd.contains("/")) {
      String nm = getName().toLowerCase();
      cmd = nm + "/" + cmd;
    }
   
   return CatbridgeFactory.sendCedesMessage(cmd,data,this);
}  



/********************************************************************************/
/*                                                                              */
/*      Methods to update devices                                               */
/*                                                                              */
/********************************************************************************/

protected void handleDevicesFound(JSONArray devs)
{ 
   CatreController cc = for_universe.getCatre();
   CatreStore cs = cc.getDatabase();
   
   Map<String,CatreDevice> alldevmap = new LinkedHashMap<>();
   for (int i = 0; i < devs.length(); ++i) {
      JSONObject devobj = devs.getJSONObject(i);
      Map<String,Object> devmap = devobj.toMap();
      CatreDevice cd = createDevice(cs,devmap);
      if (cd != null) {
         alldevmap.put(cd.getDeviceId(),cd);
       }
    }
   
   device_map = alldevmap;
   
   for_universe.updateDevices(this);
}


@Override public Collection<CatreDevice> findDevices()
{
   return device_map.values();
}


protected void handleEvent(JSONObject evt)
{ }



protected CatreDevice findDevice(String id)
{
   return device_map.get(id);
}



/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void applyTransition(CatreTransition t,CatrePropertySet ps,CatreWorld w)
        throws CatreActionException
{
   throw new CatreActionException("Transition not allowed");
}







}       // end of class CatbridgeBase




/* end of CatbridgeBase.java */

