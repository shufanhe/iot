/********************************************************************************/
/*                                                                              */
/*              CatdevDebouncer.java                                            */
/*                                                                              */
/*      Debounce a parameter value                                              */
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



package edu.brown.cs.catre.catdev;

import java.util.Map;

import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreWorld;

public class CatdevDebouncer extends CatdevDevice
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CatreDevice for_device;
private CatreParameter for_parameter;
// private CatreParameter result_parameter;
private Object  saved_value;
private long    debounce_time;
private long    start_time;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public CatdevDebouncer(String label,CatreDevice base,CatreParameter param,long stabletime)
{
   super(base.getUniverse());
   
   for_device = base;
   for_parameter = param;
   setLabel(label);
   setName(label.replace(" ",WSEP));
   setDescription("Debounce " + for_parameter.getLabel());
   debounce_time = stabletime;
   saved_value = null;
   start_time = 0;
   
   CatreLog.logD("CATDEV","Create debouncer " + label + " " + debounce_time + " " + start_time);
   
// CatreParameter bp = for_universe.cloneParameter(for_parameter);
// 
// result_parameter = addParameter(bp);
   // add conditions for result_parameter
   
   // add condition handler for base_device condition (or device handler?)
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public boolean isDependentOn(CatreDevice d)
{
   if (d == this || d == for_device) return true;
   return for_device.isDependentOn(d);
}


/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   return rslt;
}



@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
}



/********************************************************************************/
/*                                                                              */
/*      Handle state changes to underlying device                               */
/*                                                                              */
/********************************************************************************/

void handleUpdate(CatreWorld w)
{
   Object val = for_device.getValueInWorld(for_parameter,w);
   if (val == null) return;
   if (val.equals(saved_value)) return;
   saved_value = val;
   start_time = w.getTime();
   // cancel previous timer
   // start new timer for start_time + debounce_time
   
}




}       // end of class CatdevDebouncer




/* end of CatdevDebouncer.java */

