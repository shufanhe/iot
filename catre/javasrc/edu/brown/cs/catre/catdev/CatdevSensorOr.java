/********************************************************************************/
/*                                                                              */
/*              CatdevSensorOr.java                                             */
/*                                                                              */
/*      A sensor device that is the OR of a set of conditions                   */
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
import edu.brown.cs.catre.catre.CatreWorld;

class CatdevSensorOr extends CatdevDevice implements CatdevConstants,
      CatreDeviceListener
{



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<OrCondition> sensor_conditions;
private CatreParameter result_parameter;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public CatdevSensorOr(CatreUniverse uu,CatreStore cs,Map<String,Object> map)
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
   setValueInWorld(bp,Boolean.FALSE,null);
   
   String nml = getLabel().replace(" ",WORD_SEP);
   setName(getUniverse().getName() + NAME_SEP + nml);
}



/********************************************************************************/
/*                                                                              */
/*      Device setup/shutdown                                                   */
/*                                                                              */
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
   handleStateChanged(getUniverse().getCurrentWorld());
}


@Override protected void localStopDevice()
{
   for (OrCondition c : sensor_conditions) {
      c.removeDeviceListener();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/


@Override public boolean isDependentOn(CatreDevice d)
{
   for (OrCondition c : sensor_conditions) {
      if (c.isDependentOn(d)) return true;
    }
   
   return false;
}


/********************************************************************************/
/*                                                                              */
/*      State update methods                                                    */
/*                                                                              */
/********************************************************************************/

private void handleStateChanged(CatreWorld w)
{
   boolean fg = false;
   for (OrCondition c : sensor_conditions) {
      fg |= c.checkInWorld(w);
      if (fg) break;
    }
   
   System.err.println("SET OR SENSOR " + getLabel() + " = " + fg);
   
   setValueInWorld(result_parameter,fg,w);
}



@Override public void stateChanged(CatreWorld w) 
{
   handleStateChanged(w);
}




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
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
/*                                                                              */
/*      Representation of a condition                                           */
/*                                                                              */
/********************************************************************************/

private class OrCondition extends CatreSubSavableBase {

   private CatreParameterRef base_ref;
   private Object     base_state;
   private CatreDevice last_device;
   
   OrCondition(CatreStore cs,Map<String,Object> map) {
      super(null);
    }
   
   boolean isValid()                    { return base_ref.isValid(); }
   
   void addDeviceListener() {
      last_device = base_ref.getDevice();
      last_device.addDeviceListener(CatdevSensorOr.this);
    }
   
   void removeDeviceListener() {
      if (last_device != null) {
         last_device.removeDeviceListener(CatdevSensorOr.this);
       }
      last_device = null;
    }
   
   boolean checkInWorld(CatreWorld w) {
      if (!isValid()) return true;
      Object ov = base_ref.getDevice().getValueInWorld(base_ref.getParameter(),w);
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
      base_ref = getSavedSubobject(cs,map,"BASEREF",CatdevSensorOr.this::createParamRef,base_ref);
      base_state = getSavedValue(map,"STATE",null);
    }
   
}       // end of inner class Condition



}       // end of class CatdevSensorOr




/* end of CatdevSensorOr.java */

