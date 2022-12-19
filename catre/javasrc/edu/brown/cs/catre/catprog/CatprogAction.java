/********************************************************************************/
/*                                                                              */
/*              CatprogAction.java                                              */
/*                                                                              */
/*      Base class for actions                                                  */
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



package edu.brown.cs.catre.catprog;

import edu.brown.cs.catre.catre.CatreTransition;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreWorld;

import java.util.Map;

import edu.brown.cs.catre.catre.CatreAction;
import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreDescribableBase;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;

class CatprogAction extends CatreDescribableBase implements CatreAction, CatprogConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreUniverse   for_universe;
private CatreDevice     for_device;
private CatreTransition	for_transition;
private CatreParameterSet parameter_set;
private boolean 	is_trigger;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatprogAction(CatreDevice e,CatreTransition t)
{
   super("ACTION_");
   
   for_universe = e.getUniverse();
   for_device = e;
   for_transition = t;
   parameter_set = e.getUniverse().createParameterSet();
   if (t != null) parameter_set.putValues(t.getDefaultParameters());
   is_trigger = false;
   
   setName(e.getName() + "^" + t.getName());
   setLabel("Apply " + for_transition.getName() + " to " + for_device.getName());
}



public CatprogAction(CatreUniverse universe,CatreStore cs,Map<String,Object> map)
{
   super("ACTION_");
   
   for_universe = universe;
   
   fromJson(cs,map);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public boolean isTriggerAction()
{
   return is_trigger;
}


@Override public void setIsTriggerAction(boolean fg)
{
   is_trigger = fg;
}


@Override public CatreDevice getDevice() 		{ return for_device; }

@Override public CatreTransition getTransition() 	{ return for_transition; }



/********************************************************************************/
/*										*/
/*	Parameter methods							*/
/*										*/
/********************************************************************************/

@Override public void setParameters(CatreParameterSet ps)
{
   parameter_set.clearValues();
   parameter_set.putValues(ps);
}

@Override public void addParameters(CatreParameterSet ps)
{
   parameter_set.putValues(ps);
}

@Override public CatreParameterSet getParameters()
{
   return parameter_set;
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void perform(CatreWorld w,CatrePropertySet ps)
        throws CatreActionException
{
   if (for_transition != null) for_transition.perform(w,parameter_set,ps);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();

   if (for_device != null) rslt.put("DEVICE",for_device.getDeviceId());
   if (for_transition != null) rslt.put("TRANSITION",for_transition.getName());
   if (getParameters() != null) rslt.put("PARAMETERS",getParameters().toJson());
   
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   for_device = null;
   for_transition = null;
   for_device = getSavedSubobject(cs,map,"DEVICE",for_universe::findDevice,for_device);
   for_transition = getSavedSubobject(cs,map,"TRANSITION",
         for_device::findTransition,for_transition);
   CatreParameterSet ps = null;
   if (for_transition != null) ps = for_transition.getDefaultParameters();
   parameter_set = getSavedSubobject(cs,map,"PARAMETERS",for_universe::createParameterSet,parameter_set);
   if (ps != null) {
      for (CatreParameter cp : ps.getValidParameters()) {
         if (parameter_set.getValue(cp) == null) {
            parameter_set.putValue(cp,ps.getValue(cp));
          }
       }
    }
}



}       // end of class CatprogAction




/* end of CatprogAction.java */

