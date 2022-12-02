/********************************************************************************/
/*                                                                              */
/*              CatstoreUser.java                                               */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.catre.catstore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreBridgeAuthorization;
import edu.brown.cs.catre.catre.CatreSavableBase;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;

class CatstoreUser extends CatreSavableBase implements CatreUser, CatstoreConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          user_name;
private String          user_email;
private String          user_password;
private CatreUniverse   user_universe;
private Map<String,CatreBridgeAuthorization> bridge_auths;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatstoreUser(String name,String email,String pwd,CatreUniverse u)
{
   super(USERS_PREFIX);
   
   user_name = name;
   user_email = email;
   user_password = pwd;
   user_universe = u;
   bridge_auths = new HashMap<>();
}



CatstoreUser(CatreStore store,Map<String,Object> doc)
{
    super(store,doc);
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public CatreUniverse getUniverse()
{
   return user_universe;
}

@Override public String getUserName()
{
   return user_name;
}

@Override public CatreBridgeAuthorization getAuthorization(String name)
{
   return bridge_auths.get(name);
}


@Override public boolean addAuthorization(String name,Map<String,String> map)
{
   if (name == null) return false;
   
   if (map == null || map.isEmpty()) {
      bridge_auths.remove(name);
    }
   else {
      BridgeAuth ba = new BridgeAuth(name,map);
      bridge_auths.put(name,ba);
    }
   
   user_universe.addBridge(name);
   
   user_universe.getCatre().getDatabase().saveObject(this);
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      CatreStore methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("USERNAME",user_name);
   rslt.put("EMAIL",user_email);
   rslt.put("PASSWORD",user_password);
   rslt.put("UNIVERSE_ID",getUIDToSave(user_universe));;
   rslt.put("UNIVERSE_NAME",user_universe.getName());
   rslt.put("AUTHORIZATIONS",getSubObjectArrayToSave(bridge_auths.values()));
   
   return rslt;
}


@Override public void fromJson(CatreStore store,Map<String,Object> map)
{
   super.fromJson(store,map);
   user_name = getSavedString(map,"USERNAME",user_name);
   user_email = getSavedString(map,"EMAIL",user_email);
   user_password = getSavedString(map,"PASSWORD",user_password);
   
   bridge_auths = new HashMap<>();
   List<BridgeAuth> bal = new ArrayList<>();
   bal = getSavedSubobjectList(store,map,"AUTHORIZATIONS",
         BridgeAuth::new,bal);
   for (BridgeAuth ba : bal) {
      bridge_auths.put(ba.getBridgeName(),ba);
    }
   
   user_universe = getSavedObject(store,map,"UNIVERSE_ID",user_universe);
}    




/********************************************************************************/
/*                                                                              */
/*      Authorization methods                                                   */
/*                                                                              */
/********************************************************************************/

private static class BridgeAuth implements CatreBridgeAuthorization {
   
   private String bridge_name;
   private Map<String,String> value_map;
   
   BridgeAuth(String name,Map<String,String> values) {
      bridge_name = name;
      value_map = new HashMap<>(values);
    }
   
   BridgeAuth(CatreStore cs,Map<String,Object> map) {
      bridge_name = null;
      value_map = new HashMap<>();
      
      fromJson(cs,map);
    }
   
   @Override public String getBridgeName()              { return bridge_name; }
   
   @Override public String getValue(String key) {
      return value_map.get(key);
    }
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = new HashMap<>();
      rslt.put("NAME",bridge_name);
      for (Map.Entry<String,String> ent : value_map.entrySet()) {
         rslt.put("BAKEY_" + ent.getKey(),ent.getValue());
       }

      return rslt;
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      bridge_name = getSavedString(map,"NAME",null);
      if (value_map == null) value_map = new HashMap<>();
      for (String s : map.keySet()) {
         if (s.startsWith("BAKEY_")) {
            String k = s.substring(6);
            value_map.put(k,getSavedString(map,s,null));
          }
       }
    }
   
}


}       // end of class CatstoreUser




/* end of CatstoreUser.java */

