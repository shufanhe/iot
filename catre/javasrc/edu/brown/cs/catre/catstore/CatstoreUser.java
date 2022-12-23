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
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreSavableBase;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreSubSavableBase;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;

class CatstoreUser extends CatreSavableBase implements CatreUser, CatstoreConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CatreStore      catre_store;
private String          user_name;
private String          user_email;
private String          user_password;
private String          universe_id;
private CatreUniverse   user_universe;  
private Map<String,CatreBridgeAuthorization> bridge_auths;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatstoreUser(CatreStore cs,String name,String email,String pwd)
{
   super(USERS_PREFIX);
   
   catre_store = cs;
   
   user_name = name;
   user_email = email;
   user_password = pwd;
   user_universe = null;
   universe_id = null;
   bridge_auths = new HashMap<>();
}



CatstoreUser(CatreStore store,Map<String,Object> doc)
{
    super(store,doc);
    
    catre_store = store;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public CatreUniverse getUniverse()
{
   if (user_universe == null && universe_id != null) {
      user_universe = (CatreUniverse) catre_store.loadObject(universe_id);
      
    }
   return user_universe;
}

@Override public void setUniverse(CatreUniverse cu)
{
   if (universe_id == null) {
      universe_id = cu.getDataUID();
      user_universe = cu;
      catre_store.saveObject(this);
    }
   else {
      CatreLog.logE("CATSTORE","Attempt to change user universe");
    }
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
   
   getUniverse().addBridge(name);
   
   getUniverse().getCatre().getDatabase().saveObject(this);
   
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
   rslt.put("UNIVERSE_ID",universe_id);
   rslt.put("AUTHORIZATIONS",getSubObjectArrayToSave(bridge_auths.values()));
   
   return rslt;
}


@Override public void fromJson(CatreStore store,Map<String,Object> map)
{
   super.fromJson(store,map);
   user_name = getSavedString(map,"USERNAME",user_name);
   user_email = getSavedString(map,"EMAIL",user_email);
   user_password = getSavedString(map,"PASSWORD",user_password);
   universe_id = getSavedString(map,"UNIVERSE_ID",universe_id);
   user_universe = null;
   
   bridge_auths = new HashMap<>();
   List<BridgeAuth> bal = new ArrayList<>();
   bal = getSavedSubobjectList(store,map,"AUTHORIZATIONS",
         BridgeAuth::new,bal);
   for (BridgeAuth ba : bal) {
      bridge_auths.put(ba.getBridgeName(),ba);
    }
}    




/********************************************************************************/
/*                                                                              */
/*      Authorization methods                                                   */
/*                                                                              */
/********************************************************************************/

private static class BridgeAuth extends CatreSubSavableBase implements CatreBridgeAuthorization {
   
   private String bridge_name;
   private Map<String,String> value_map;
   
   BridgeAuth(String name,Map<String,String> values) {
      super(null);
      bridge_name = name;
      value_map = new HashMap<>(values);
    }
   
   BridgeAuth(CatreStore cs,Map<String,Object> map) {
      super(cs,map);
    }
   
   @Override public String getBridgeName()              { return bridge_name; }
   
   @Override public String getValue(String key) {
      return value_map.get(key);
    }
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("NAME",bridge_name);
      for (Map.Entry<String,String> ent : value_map.entrySet()) {
         rslt.put("BAKEY_" + ent.getKey(),ent.getValue());
       }
   
      return rslt;
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
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

