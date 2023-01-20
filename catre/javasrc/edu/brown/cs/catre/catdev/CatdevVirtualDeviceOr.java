/********************************************************************************/
/*										*/
/*		CatdevVirtualDeviceOr.java					*/
/*										*/
/*	A sensor device that is the OR of a set of parameter conditions 	*/
/*										*/
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




package edu.brown.cs.catre.catdev;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceListener;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterRef;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreSubSavableBase;
import edu.brown.cs.catre.catre.CatreUniverse;

class CatdevVirtualDeviceOr extends CatdevDevice implements CatdevConstants,
      CatreDeviceListener
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<OrCondition> sensor_conditions;
private CatreParameter result_parameter;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatdevVirtualDeviceOr(CatreUniverse uu,CatreStore cs,Map<String,Object> map)
{
   super(uu);

   sensor_conditions = new ArrayList<>();
   fromJson(cs,map);

   setup();
}




private void setup()
{
   CatreParameter bp = for_universe.createBooleanParameter("OrValue",true,getLabel());
   result_parameter = addParameter(bp);
   setParameterValue(bp,Boolean.FALSE);

   String nml = getLabel().replace(" ",WORD_SEP);
   setName(getUniverse().getName() + NAME_SEP + nml);

   for (OrCondition oc : sensor_conditions) {
      oc.initialize();
    }
}



/********************************************************************************/
/*										*/
/*	Device setup/shutdown							*/
/*										*/
/********************************************************************************/

@Override protected boolean isDeviceValid()
{
   for (OrCondition oc : sensor_conditions) {
      if (!oc.isValid()) return false;
    }
   return true;
}



@Override protected void localStartDevice()
{
   for (OrCondition c : sensor_conditions) {
      c.addDeviceListener();
    }
   handleStateChanged();
}


@Override protected void localStopDevice()
{
   for (OrCondition c : sensor_conditions) {
      c.removeDeviceListener();
    }
}



/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/


@Override public boolean isDependentOn(CatreDevice d)
{
   for (OrCondition c : sensor_conditions) {
      if (c.isDependentOn(d)) return true;
    }

   return false;
}


/********************************************************************************/
/*										*/
/*	State update methods							*/
/*										*/
/********************************************************************************/

private void handleStateChanged()
{
   boolean fg = false;
   for (OrCondition c : sensor_conditions) {
      fg |= c.checkInWorld();
      if (fg) break;
    }

   System.err.println("SET OR SENSOR " + getLabel() + " = " + fg);

   setParameterValue(result_parameter,fg);
}



@Override public void stateChanged()
{
   handleStateChanged();
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();

   rslt.put("VTYPE","Or");

   rslt.put("CONDITIONS",getSubObjectArrayToSave(sensor_conditions));
   return rslt;
}

@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);

   sensor_conditions = getSavedSubobjectList(cs,map,"CONDITIONS",this::createCondition,sensor_conditions);
}


private OrCondition createCondition(CatreStore cs,Map<String,Object> map)
{
   return new OrCondition(cs,map);
}


private CatreParameterRef createParamRef(CatreStore cs,Map<String,Object> map)
{
   return getUniverse().createParameterRef(this,cs,map);
}



/********************************************************************************/
/*										*/
/*	Representation of a condition						*/
/*										*/
/********************************************************************************/

private class OrCondition extends CatreSubSavableBase {

   private CatreParameterRef base_ref;
   private Object     base_state;
   private CatreDevice last_device;

   OrCondition(CatreStore cs,Map<String,Object> map) {
      super(null);
    }

   void initialize()			{ base_ref.initialize(); }

   boolean isValid()			{ return base_ref.isValid(); }

   void addDeviceListener() {
      last_device = base_ref.getDevice();
      last_device.addDeviceListener(CatdevVirtualDeviceOr.this);
    }

   void removeDeviceListener() {
      if (last_device != null) {
	 last_device.removeDeviceListener(CatdevVirtualDeviceOr.this);
       }
      last_device = null;
    }

   boolean checkInWorld() {
      if (!isValid()) return true;
      Object ov = base_ref.getDevice().getParameterValue(base_ref.getParameter());
      if (base_state.equals(ov)) return true;
      return false;
    }

   boolean isDependentOn(CatreDevice d) {
      return d == base_ref.getDevice() || d == this;
    }

   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = super.toJson();
      rslt.put("SET",base_state);
      rslt.put("BASEREF",base_ref.toJson());
      return rslt;
    }

   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      super.fromJson(cs,map);
      base_ref = getSavedSubobject(cs,map,"BASEREF",CatdevVirtualDeviceOr.this::createParamRef,base_ref);
      base_state = getSavedValue(map,"STATE",null);
    }

}	// end of inner class Condition



}	// end of class CatdevVirtualDeviceOr




/* end of CatdevVirtualDeviceOr.java */

