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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.catre.catmodel.CatmodelConditionParameter;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceHandler;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreSubSavable;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreWorld;

class CatdevSensorOr extends CatdevDevice implements CatdevConstants
{



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  sensor_label;
private String  sensor_name;
private List<OrCondition> sensor_conditions;
private CatreParameter result_parameter;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public CatdevSensorOr(String id,CatreDevice base,CatreParameter p,Object state) 
{
   super(base.getUniverse());
   
   sensor_label = id;
   sensor_conditions = new ArrayList<OrCondition>();
   addCondition(base,p,state);
   
   setup();
}


public CatdevSensorOr(String id,CatreCondition c)
{
   super(c.getUniverse());
   sensor_label = id;
   sensor_conditions = new ArrayList<OrCondition>();   
   addCondition(c);
   
   setup();
}



public CatdevSensorOr(CatreUniverse uu,CatreStore cs,Map<String,Object> map)
{
   super(uu);
   sensor_conditions = new ArrayList<OrCondition>();  
   fromJson(cs,map);
}




private void setup()
{
   CatreParameter bp = for_universe.createBooleanParameter(getDataUID(),true,getLabel());
   result_parameter = addParameter(bp);
   setValueInWorld(bp,Boolean.FALSE,null);
   
   String nml = sensor_label.replace(" ",WSEP);
   sensor_name = getUniverse().getName() + NSEP + nml;
   
   CatreCondition uc = getCondition(result_parameter,Boolean.TRUE);
   uc.setLabel(sensor_label);
}




/********************************************************************************/
/*                                                                              */
/*      Definition methods                                                      */
/*                                                                              */
/********************************************************************************/

public void addCondition(CatreDevice d,CatreParameter p,Object v) 
{
   OrCondition c = new OrCondition(d,p,v);
   sensor_conditions.add(c);
}


public void addCondition(CatreCondition c)
{
   if (c instanceof CatmodelConditionParameter) {
      CatmodelConditionParameter bp = (CatmodelConditionParameter) c;
      CatreDevice ud = bp.getDevice();
      CatreParameter up = bp.getParameter();
      Object v = bp.getState();
      addCondition(ud,up,v);
    }
   else {
      CatreLog.logE("CATDEV","Can't add or condition for " + c);
    }
}



@Override protected void localStartDevice()
{
   for (OrCondition c : sensor_conditions) {
      c.getDevice().addDeviceHandler(new SensorChanged());
    }
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public String getDescription()
{
   StringBuffer buf = new StringBuffer();
   
   for (OrCondition c : sensor_conditions) {
      String ctag = c.getLabel();
      if (buf.length() > 0) buf.append(" OR ");
      buf.append(ctag);
    }
   
   return buf.toString();
}




@Override public String getName()
{
   return sensor_name;
}



@Override public String getLabel()
{
   return sensor_label;
}


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



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   rslt.put("CONDITIONS",getSubObjectArrayToSave(sensor_conditions));
   return rslt;
}

@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   sensor_label = getSavedString(map,"LABEL",sensor_label);
   sensor_name = getSavedString(map,"NAME",sensor_name);
   sensor_conditions = getSavedSubobjectList(cs,map,"CONDITIONS",this::createCondition,sensor_conditions);
}


private OrCondition createCondition(CatreStore cs,Map<String,Object> map)
{
   String pnm = getSavedString(map,"PARAM",null);
   CatreDevice dev = getSavedSubobject(cs,map,"DEVICE",for_universe::findDevice,null);
   CatreParameter par = dev.findParameter(pnm);
   Object state = getSavedValue(map,"STATE",null);
   return new OrCondition(dev,par,state);
}



/********************************************************************************/
/*                                                                              */
/*      Representation of a condition                                           */
/*                                                                              */
/********************************************************************************/

private static class OrCondition implements CatreSubSavable {

   private CatreDevice base_sensor;
   private CatreParameter base_parameter;
   private Object     base_state;
   
   OrCondition(CatreDevice ud,CatreParameter up,Object v) {
      base_sensor = ud;
      base_parameter = up;
      base_state = base_parameter.normalize(v);
    }
   
   CatreDevice getDevice()               { return base_sensor; }
   
   String getLabel() {
      return base_parameter.getLabel() + " = " + base_state;
    }
   
   boolean checkInWorld(CatreWorld w) {
      Object ov = base_sensor.getValueInWorld(base_parameter,w);
      if (base_state.equals(ov)) return true;
      return false;
    }
   
   boolean isDependentOn(CatreDevice d) {
      return d == base_sensor;
    }
   
   @Override public Map<String,Object> toJson() {
      Map<String,Object> rslt = new HashMap<String,Object>();
      rslt.put("SET",base_state);
      rslt.put("PARAM",base_parameter.getName());
      rslt.put("DEVICE",getUIDToSave(base_sensor));
      return rslt;
    }
   
   @Override public void fromJson(CatreStore cs,Map<String,Object> map) {
      CatreUniverse cu = base_sensor.getUniverse();
      String pnm = getSavedString(map,"PARAM",null);
      base_sensor = getSavedSubobject(cs,map,"DEVICE",cu::findDevice,null);
      base_parameter = base_sensor.findParameter(pnm);
      base_state = getSavedValue(map,"STATE",null);
    }
   
}       // end of inner class Condition



/********************************************************************************/
/*                                                                              */
/*      Sensor update methods                                                   */
/*                                                                              */
/********************************************************************************/

private class SensorChanged implements CatreDeviceHandler {
   
   @Override public void stateChanged(CatreWorld w,CatreDevice s) {
      handleStateChanged(w);
    }

}       // end of inner class SensorChanged



}       // end of class CatdevSensorOr




/* end of CatdevSensorOr.java */

