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

import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreBridgeAuthorization;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;

abstract class CatbridgeBase implements CatreBridge
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<CatreUniverse,CatbridgeBase> known_instances;

protected CatreUniverse         for_universe;
protected Map<String,CatreDevice> device_map;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected CatbridgeBase()
{
   for_universe = null;
   device_map = null;
}

protected CatbridgeBase(CatbridgeBase base,CatreUniverse cu)
{
   for_universe = cu;
   device_map = new HashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

CatreUniverse getUniverse()             { return for_universe; }



/********************************************************************************/
/*                                                                              */
/*      Methods to create an instance                                           */
/*                                                                              */
/********************************************************************************/

@Override public CatreBridge createBridge(CatreUniverse u)
{
   if (for_universe == null) return null;
   
   CatreBridge cb = known_instances.get(u);
   
   CatreUser cu = u.getUser();
   CatreBridgeAuthorization ba = cu.getAuthorization("SmartThings");
   if (ba == null) {
      if (cb != null) known_instances.remove(u);
      return null;
    }
   
   if (cb == null) cb = createInstance(u);
   
   return cb;
}


abstract protected CatbridgeBase createInstance(CatreUniverse u);



/********************************************************************************/
/*                                                                              */
/*      Methods to update devices                                               */
/*                                                                              */
/********************************************************************************/


protected CatreDevice findDevice(String id)
{
   return device_map.get(id);
}







}       // end of class CatbridgeBase




/* end of CatbridgeBase.java */

