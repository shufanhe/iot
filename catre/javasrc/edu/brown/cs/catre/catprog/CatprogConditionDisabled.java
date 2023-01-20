/********************************************************************************/
/*                                                                              */
/*              CatprogConditionDisabled.java                                   */
/*                                                                              */
/*      Condition for a device being enabled/disabled                           */
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




package edu.brown.cs.catre.catprog;

import java.util.Map;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceListener;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverseListener;

class CatprogConditionDisabled extends CatprogCondition 
      implements CatreDeviceListener, CatreUniverseListener
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  device_id;
private boolean check_enabled;
private CatreDevice last_device;
private boolean needs_name;
private Boolean last_set;
private boolean has_listener;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatprogConditionDisabled(CatreProgram pgn,CatreStore cs,Map<String,Object> map)
{
   super(pgn,cs,map);
   
   check_enabled = false;
   last_device = null;
   needs_name = false;
   last_set = null;
   
   setDisabledName();
   has_listener = false;
}



private CatprogConditionDisabled(CatprogConditionDisabled cc)
{
   super(cc);
   device_id = cc.device_id;
   
   last_device = null;
   check_enabled = false;
   needs_name = false;
   last_set = null;
   has_listener = false; 
}


@Override public CatreCondition cloneCondition()
{
   return new CatprogConditionDisabled(this);
}


@Override public void activate()
{
   if (has_listener) return;
   
   setValid(isDeviceEnabled());
   
   has_listener = false;
   
   getUniverse().addUniverseListener(this);  
   
}

private void setDisabledName()
{
   if (!needs_name && getName() != null && !getName().equals("")) return;
   
   needs_name = false;
   
   String dnm = device_id;
   CatreDevice cd = getDevice();
   if (cd != null) dnm = cd.getName();
   else needs_name = true;
   
   setName(dnm + (check_enabled ? " Enabled" : " Disabled"));
}




/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

private CatreDevice getDevice()
{
   return getUniverse().findDevice(device_id);
}









/********************************************************************************/
/*                                                                              */
/*      Validity checking methods                                               */
/*                                                                              */
/********************************************************************************/

private boolean isDeviceEnabled()
{
   CatreDevice cd = getDevice();
   if (cd == null) return false;
   return cd.isEnabled();
}


private void validate()
{
   boolean fg0 = isDeviceEnabled();
   boolean fg = (check_enabled ? fg0 : !fg0);
   
   if (last_set != null && last_set == fg) return;
  
   last_set = fg;
   
   if (fg) fireOn(null);
   else fireOff();
   
   if (last_device != null) {
      last_device.removeDeviceListener(this);
    }
   if (fg0) { 
      last_device = getDevice();
      last_device.addDeviceListener(this); 
    }
}
  


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
   if (getDevice() == d) validate();
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("TYPE","Disabled");
   rslt.put("DEVICE",device_id);
   rslt.put("ENABLED",check_enabled);
   
   return rslt;
}

@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   device_id = getSavedString(map,"DEVICE",device_id);
   check_enabled = getSavedBool(map,"ENABLED",check_enabled);
}



}       // end of class CatprogConditionDisabled




/* end of CatprogConditionDisabled.java */

