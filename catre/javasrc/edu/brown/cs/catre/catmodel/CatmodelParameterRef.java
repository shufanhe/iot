/********************************************************************************/
/*                                                                              */
/*              CatmodelParameterRef.java                                       */
/*                                                                              */
/*      Reference to a device parameter                                         */
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
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUniverseListener;

import java.util.Map;

import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceListener;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterRef;
import edu.brown.cs.catre.catre.CatreReferenceListener;
import edu.brown.cs.catre.catre.CatreStore;

class CatmodelParameterRef extends CatreSubSavableBase 
      implements CatmodelConstants, CatreParameterRef, 
      CatreDeviceListener, CatreUniverseListener
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CatreUniverse for_universe;
private CatreReferenceListener ref_listener;

private String  device_id;
private String  parameter_name;
private CatreDevice for_device;
private CatreParameter for_parameter;
private String ref_label;
private boolean is_valid;
private int use_count;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatmodelParameterRef(CatreUniverse cu,CatreReferenceListener rl,String devid,String parameter)
{
   super(null);
   
   for_universe = cu;
   ref_listener = rl;
   use_count = 0;
   ref_label = null;
   
   device_id = devid;
   parameter_name = parameter;
   
   is_valid = false;
   for_device = null;
   for_parameter = null;
   validate();
   
   for_universe.addUniverseListener(this);
}


CatmodelParameterRef(CatreUniverse cu,CatreReferenceListener rl,CatreStore cs,Map<String,Object> map)
{
   super(null);
   
   for_universe = cu;
   use_count = 0;
   ref_label = null;
   
   fromJson(cs,map);
   
   is_valid = false;
   for_device = null;
   for_parameter = null;
   
   ref_listener = rl;
   
   for_universe.addUniverseListener(this);
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

@Override public CatreParameter getParameter()          
{ 
   if (!is_valid) return null;
   
   return for_parameter;
}

@Override public String getDeviceId()                   { return device_id; }

@Override public String getParameterName()              { return parameter_name; }

@Override public void initialize()
{
   validate();
}


@Override public void noteUsed(boolean fg)
{
   if (for_parameter != null) {
      for_parameter.noteUse(fg);
    }
   else {
      if (fg) use_count += 1;
      else use_count -= 1;
    }
}



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
   
   if (for_device != null && for_parameter == null) {
      for_parameter = for_device.findParameter(parameter_name);
    }
   
   if (ref_label == null && for_device != null) {
      ref_label = for_device.getLabel() + "." + parameter_name;
    }
   
   boolean valid = (for_device != null && for_parameter != null && for_device.isEnabled());
   
   if (valid != is_valid) {
      is_valid = valid;
      if (ref_listener != null) ref_listener.referenceValid(is_valid);
      if (valid) {
         while (use_count > 0) {
            for_parameter.noteUse(true);
            --use_count;
          }
         while (use_count < 0) {
            for_parameter.noteUse(false);
            ++use_count;
          }
         use_count = 0;
       }
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
      for_parameter = null;
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
   rslt.put("PARAMETER",parameter_name);
   if (ref_label != null) rslt.put("LABEL",ref_label);
   
   
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   device_id = getSavedString(map,"DEVICE",device_id);
   parameter_name = getSavedString(map,"PARAMETER",parameter_name);
   ref_label = getSavedString(map,"LABEL",ref_label);
   for_device = null;
   for_parameter = null;
}


}       // end of class CatmodelParameterRef




/* end of CatmodelParameterRef.java */

