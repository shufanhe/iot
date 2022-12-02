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

import java.util.Map;

import org.json.JSONObject;

import edu.brown.cs.catre.catdev.CatdevDevice;
import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreBridgeAuthorization;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTransition;
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


@Override protected void handleEvent(JSONObject evt)
{
   String typ = evt.getString("TYPE");
   CatreDevice dev = for_universe.findDevice(evt.getString("DEVICE"));
   CatreLog.logD("CATBRIDGE","EVENT " + typ + " " + dev);
   if (dev == null) return;
   
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


/********************************************************************************/
/*                                                                              */
/*      Generic bridge device                                                   */
/*                                                                              */
/********************************************************************************/

@Override public CatreDevice createDevice(CatreStore cs,Map<String,Object> map)
{
   if (map != null && map.get("_id") == null) {
      map.put("_id","DEV_" + map.get("UID"));
    }
   
   return new GenericDevice(this,cs,map);
}


@Override public CatreTransition createTransition(CatreDevice cd,CatreStore cs,Map<String,Object> map)
{
   // TODO: handle transitions
   
   return null;
}



private static class GenericDevice extends CatdevDevice {
   
   GenericDevice(CatbridgeBase bridge,CatreStore cs,Map<String,Object> map) {
      super(bridge.getUniverse(),bridge);
      fromJson(cs,map);
    }
   
}

}       // end of class CatbridgeGeneric




/* end of CatbridgeGeneric.java */

