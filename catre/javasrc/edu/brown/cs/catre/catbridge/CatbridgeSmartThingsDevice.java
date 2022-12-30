/********************************************************************************/
/*                                                                              */
/*              CatbridgeSmartThingsDevice.java                                 */
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



package edu.brown.cs.catre.catbridge;

import edu.brown.cs.catre.catdev.CatdevDevice;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreWorld;

class CatbridgeSmartThingsDevice extends CatdevDevice implements CatreDevice
{


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatbridgeSmartThingsDevice(CatbridgeSmartThings bridge,String lbl,String id,String name)
{
   super(bridge.getUniverse(),bridge);
   setName(name);
   setLabel(lbl);
   setDeviceId(id);
}


/********************************************************************************/
/*                                                                              */
/*      Handle actions                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void setValueInWorld(CatreParameter p,Object val,CatreWorld w)
{
   if (w == null) w = getUniverse().getCurrentWorld();
   super.setValueInWorld(p,val,w);
}



void issueCommand(String type,String field,String value)
{ }


void handleSmartThingsValue(Object v)
{
   CatreParameter p = findParameter(getParameterName());
   if (p != null) {
      setValueInWorld(p,v,null);
    }
   else {
      CatreLog.logD("CATBRIDGE","SET VALUE FROM SMARTTHINGS: " + getDeviceId() + " " +
         v.getClass().getName() + " " + getName() + " " + v);
    }
}


String getParameterName()               { return getDeviceId(); }




}       // end of class CatbridgeSmartThingsDevice




/* end of CatbridgeSmartThingsDevice.java */

