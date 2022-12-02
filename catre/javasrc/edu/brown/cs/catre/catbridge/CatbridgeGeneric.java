/********************************************************************************/
/*                                                                              */
/*              CatbridgeGeneric.java                                           */
/*                                                                              */
/*      Bridge to handle generic devices                                        */
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreBridgeAuthorization;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUtil;

class CatbridgeGeneric extends CatbridgeBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          auth_uid;
private String          auth_pat;
private List<CatreDevice> known_devices;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatbridgeGeneric(CatreController cc)
{
   auth_uid = null;
   auth_pat = null;
}


CatbridgeGeneric(CatbridgeBase base,CatreUniverse u,CatreBridgeAuthorization ba)
{
   super(base,u);
   auth_uid = ba.getValue("AUTH_UID");
   auth_pat = ba.getValue("AUTH_PAT");
   known_devices = null;
}



protected CatbridgeBase createInstance(CatreUniverse u,CatreBridgeAuthorization ba)
{
   return new CatbridgeGeneric(this,u,ba);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getName()               { return "generic"; }


@Override public List<CatreDevice> findDevices()
{
   // devices are found asynchronously
   return known_devices;
}


@Override protected void handleDevicesFound(JSONArray devs)
{
   CatreController cc = for_universe.getCatre();
   CatreStore cs = cc.getDatabase();
   
   List<CatreDevice> devset = new ArrayList<>();
   for (int i = 0; i < devs.length(); ++i) {
      JSONObject devobj = devs.getJSONObject(i);
      Map<String,Object> devmap = devobj.toMap();
      CatreDevice cd = for_universe.createDevice(cs,devmap);
      if (cd != null) devset.add(cd);
    }
   known_devices = devset;
   // TODO:  tell universe to update devices for this bridge
}


@Override protected void handleEvent(JSONObject evt)
{
   String typ = evt.getString("TYPE");
   CatreDevice dev = for_universe.findDevice(evt.getString("DEVICE"));
   
   switch (typ) {
      case "PARAMETER" :
         CatreParameter param = dev.findParameter(evt.getString("PARAMETER"));
         Object val = evt.get("VALUE");
         try {
            dev.setValueInWorld(param,val,null);
          }
         catch (CatreActionException e) {
            CatreLog.logE("CATBRIDGE","Problem with parameter event",e);
          }
         break;
      default :
         break;
    }
   // handle events
}

@Override protected Map<String,Object> getAuthData()
{
   Map<String,Object> rslt = super.getAuthData();
   
   rslt.put("uid",auth_uid);
   String p0 = CatreUtil.secureHash(auth_pat);
   String p1 = CatreUtil.secureHash(p0 + auth_uid);
   rslt.put("pat",p1);
   
   return rslt;
}



}       // end of class CatbridgeGeneric




/* end of CatbridgeGeneric.java */

