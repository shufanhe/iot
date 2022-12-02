/********************************************************************************/
/*                                                                              */
/*              CatreSavableBase.java                                           */
/*                                                                              */
/*      Base class for all savable items                                        */
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



package edu.brown.cs.catre.catre;

import java.util.HashMap;
import java.util.Map;

abstract public class CatreSavableBase implements CatreSavable
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  data_uid;
private boolean is_stored;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected CatreSavableBase(String pfx)
{
   data_uid = pfx + CatreUtil.randomString(24);
   is_stored = false;
}

protected CatreSavableBase(CatreStore store)
{
   data_uid = null;
   is_stored = true;
}

protected CatreSavableBase(CatreStore store,Map<String,Object> map)
{
   data_uid = map.get("_id").toString();
   is_stored = true;
   store.recordObject(this);
   
   fromJson(store,map);
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getDataUID()                    { return data_uid; }

public boolean isStored()                               { return is_stored; }

public void setStored()                                 { is_stored = true; }


public void setDataUID(String uid) throws CatreException
{
   throw new CatreException("Can't set uid");
}


@Override public Map<String,Object> toJson()
{
   Map<String,Object> obj = new HashMap<>();
   obj.put("_id",data_uid);
   return obj;
}

@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   data_uid = map.get("_id").toString();
}




}       // end of class CatreSavableBase




/* end of CatreSavableBase.java */

