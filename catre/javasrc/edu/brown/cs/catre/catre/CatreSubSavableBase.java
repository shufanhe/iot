/********************************************************************************/
/*                                                                              */
/*              CatreSubSavableBase.java                                        */
/*                                                                              */
/*      Base class for identifiable subsavable object                           */
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

abstract public class CatreSubSavableBase implements CatreSubSavable, CatreIdentifiable
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  data_uid;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected CatreSubSavableBase(String pfx)
{
   if (pfx != null) data_uid = pfx + CatreUtil.randomString(24);
}



protected CatreSubSavableBase(CatreStore store,Map<String,Object> map)
{
   data_uid = getSavedString(map,"_id",null);
   fromJson(store,map);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getDataUID()                    { return data_uid; }


@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = new HashMap<>();
   if (data_uid != null) rslt.put("_id",data_uid);
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   data_uid = getSavedString(map,"_id",data_uid);
}

}       // end of class CatreSubSavableBase




/* end of CatreSubSavableBase.java */

