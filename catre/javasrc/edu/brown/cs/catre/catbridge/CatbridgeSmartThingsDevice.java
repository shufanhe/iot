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

abstract class CatbridgeSmartThingsDevice extends CatdevDevice implements CatreDevice
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  device_label;
private String  device_id; 



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

static CatbridgeSmartThingsDevice createDevice(CatbridgeSmartThings bridge, 
      String label,String id,String cap)
{
   return null;
}
      

protected CatbridgeSmartThingsDevice(CatbridgeSmartThings bridge,String lbl,String id)
{
   super(bridge.getUniverse(),bridge);
   device_label = lbl;
   device_id = id;
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public String getName()               
{
   return device_label.replace(" ","_");
   
}

@Override public String getDataUID()            { return device_id; }


abstract protected String getAccessName();


boolean hasCapability(String name)
{
   return false;
}


void addCapability(String name)
{ }



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
      CatreLog.logD("CATBRIDGE","SET VALUE FROM SMARTTHINGS: " + getAccessName() + " " +
         v.getClass().getName() + " " + getName() + " " + v);
    }
}


String getParameterName()               { return getAccessName(); }




}       // end of class CatbridgeSmartThingsDevice




/* end of CatbridgeSmartThingsDevice.java */

