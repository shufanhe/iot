/********************************************************************************/
/*                                                                              */
/*              CatserveSessionImpl.java                                        */
/*                                                                              */
/*      Implementation of local session                                         */
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



package edu.brown.cs.catre.catserve;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreSavableBase;
import edu.brown.cs.catre.catre.CatreSession;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;

import java.util.Map;

import org.nanohttpd.protocols.http.response.Response;

import java.util.Date;
import java.util.HashMap;

class CatserveSessionImpl extends CatreSavableBase implements CatreSession, CatserveConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          user_id;
private String          universe_id;
private Date            last_used;
private long            expires_at;
private Map<String,String> value_map;

private static final long EXPIRE_DELTA = 1000*60*60*24*30;

      


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatserveSessionImpl()
{
   super(SESSION_PREFIX);
   
   user_id = null;
   universe_id = null;
   last_used = new Date();
   expires_at = 0;
   value_map = new HashMap<>();
}


CatserveSessionImpl(CatreStore store,Map<String,Object> data)
{
   super(store,data);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override 
public CatreUser getUser(CatreController cc)
{
   if (user_id == null || expires_at == 0) return null;
   
   return (CatreUser) cc.getDatabase().loadObject(user_id);
}


@Override
public CatreUniverse getUniverse(CatreController cc)
{
   if (universe_id == null || expires_at == 0) return null;
   
   return (CatreUniverse) cc.getDatabase().loadObject(universe_id);
}



@Override 
public String getSessionId()
{
   return getDataUID();
}



@Override
public void setupSession(CatreUser user)
{
   CatreUniverse univ = user.getUniverse();
   user_id = (user == null ? null : user.getDataUID());
   universe_id = (univ == null ? null : univ.getDataUID());
   last_used = new Date();
   expires_at = last_used.getTime() + EXPIRE_DELTA;
}



/********************************************************************************/
/*                                                                              */
/*      Response methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public Response jsonResponse(Object ... val)
{
   return CatserveServer.jsonResponse(this,val);
}


@Override public Response errorResponse(String msg)
{
   return CatserveServer.errorResponse(msg);
}

/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override 
public Map<String,Object> toJson()
{
   Map<String,Object> rslt = new HashMap<>();
   rslt.put("USER_ID",user_id);
   rslt.put("UNIVERSE_ID",universe_id);
   rslt.put("LAST_USED", new Date());
   for (Map.Entry<String,String> ent : value_map.entrySet()) {
      rslt.put("VALUE_" + ent.getKey(),ent.getValue());
    }
   
   return rslt;
}



@Override 
public void fromJson(CatreStore cs,Map<String,Object> o)
{
   user_id = getSavedString(o,"USER_ID",user_id);
   universe_id = getSavedString(o,"universe_id",universe_id);
   last_used = getSavedDate(o,"LAST_USED",last_used);
   expires_at = last_used.getTime() + EXPIRE_DELTA;
   for (String k : o.keySet()) {
      if (k.startsWith("VALUE_")) {
         String kk = k.substring(6);
         String ov = value_map.get(kk);
         value_map.put(kk,getSavedString(o,k,ov));
       }
    }
}




void removeSession(CatreController cc)
{ 
   cc.getDatabase().removeObject(getDataUID());
}



/********************************************************************************/
/*                                                                              */
/*      Value maintenance                                                       */
/*                                                                              */
/********************************************************************************/

@Override public void setValue(String key,String val)
{
   value_map.put(key,val);
}

@Override public String getValue(String key)
{
   return value_map.get(key);
}



@Override public void saveSession(CatreController cc)
{
   if (expires_at == 0) return;
   last_used = new Date();
   cc.getDatabase().saveObject(this);
}



}       // end of class CatserveSessionImpl




/* end of CatserveSessionImpl.java */

