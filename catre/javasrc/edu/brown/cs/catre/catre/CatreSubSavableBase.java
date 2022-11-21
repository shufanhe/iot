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
   data_uid = pfx + CatreUtil.randomString(24);
}



protected CatreSubSavableBase(CatreStore store,Map<String,Object> doc)
{
   data_uid = doc.get("_id").toString();
   fromJson(store,doc);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getDataUID()                    { return data_uid; }




@Override public Map<String,Object> toJson()
{
   Map<String,Object> obj = new HashMap<>();
   obj.put("_id",data_uid);
   return obj;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   data_uid = getSavedString(map,"_id",null);
}

}       // end of class CatreSubSavableBase




/* end of CatreSubSavableBase.java */

