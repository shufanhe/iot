/********************************************************************************/
/*                                                                              */
/*              CatmodelTransitionRef.java                                      */
/*                                                                              */
/*      Reference to a device transition                                        */
/*                                                                              */
/********************************************************************************/
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/




package edu.brown.cs.catre.catmodel;

import edu.brown.cs.catre.catre.CatreSubSavableBase;
import edu.brown.cs.catre.catre.CatreTransition;
import edu.brown.cs.catre.catre.CatreTransitionRef;
import edu.brown.cs.catre.catre.CatreUniverse;

import java.util.Map;

import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceListener;
import edu.brown.cs.catre.catre.CatreReferenceListener;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverseListener;

class CatmodelTransitionRef extends CatreSubSavableBase implements CatmodelConstants, CatreTransitionRef, CatreDeviceListener, CatreUniverseListener
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CatreUniverse for_universe;
private CatreReferenceListener ref_listener;

private String  device_id;
private String  transition_name;
private CatreDevice for_device;
private CatreTransition for_transition;
private boolean is_valid;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatmodelTransitionRef(CatreUniverse cu,CatreReferenceListener rl,String devid,String transition)
{
   super("TRANSREF_");
   
   for_universe = cu;
   ref_listener = rl;
   
   device_id = devid;
   transition_name = transition;
   
   is_valid = false;
   for_device = null;
   for_transition = null;
   validate();
   
   for_universe.addUniverseListener(this);
}


CatmodelTransitionRef(CatreUniverse cu,CatreReferenceListener rl,CatreStore cs,Map<String,Object> map)
{
   super("TRANSREF_");
   
   for_universe = cu;
   ref_listener = rl;
   
   fromJson(cs,map);
   
   is_valid = false;
   for_device = null;
   for_transition = null;
   
   for_universe.addUniverseListener(this);
}



@Override public void initialize()
{
   validate();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public boolean isValid()                      { return is_valid; }

@Override public CatreDevice getDevice()                
{
   if (!is_valid) return null;
   
   return for_device;
}

@Override public CatreTransition getTransition()          
{ 
   if (!is_valid) return null;
   
   return for_transition;
}

@Override public String getDeviceId()                   { return device_id; }

@Override public String getTransitionName()              { return transition_name; }




/********************************************************************************/
/*                                                                              */
/*      Validation methods                                                      */
/*                                                                              */
/********************************************************************************/

private void validate()
{
   if (for_device == null) {
      for_device = for_universe.findDevice(device_id);
      if (for_device != null) {
         for_device.addDeviceListener(this);
       }
    }
   
   if (for_device != null && for_transition == null) {
      for_transition = for_device.findTransition(transition_name);
    }
   
   boolean valid = (for_device != null && for_transition != null && for_device.isEnabled());
   
   if (valid != is_valid) {
      is_valid = valid;
      if (ref_listener != null) ref_listener.referenceValid(is_valid);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Callback methods to update the reference                                */
/*                                                                              */
/********************************************************************************/

@Override public void deviceEnabled(CatreDevice d,boolean enable)
{
   validate();
}


@Override public void deviceAdded(CatreDevice d)
{
   if (d.getDeviceId().equals(device_id)) validate();
}


@Override public void deviceRemoved(CatreDevice d)
{
   if (for_device == d) {
      for_device = null;
      for_transition = null;
      validate();
    }
}



/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("DEVICE",device_id);
   rslt.put("TRANSITION",transition_name);
   
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   device_id = getSavedString(map,"DEVICE",device_id);
   transition_name = getSavedString(map,"TRANSITION",transition_name);
   for_device = null;
   for_transition = null;
}


}       // end of class CatmodelTransitionRef




/* end of CatmodelTransitionRef.java */

