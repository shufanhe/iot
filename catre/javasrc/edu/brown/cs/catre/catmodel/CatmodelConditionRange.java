/********************************************************************************/
/*                                                                              */
/*              CatmodelConditionRange.java                                     */
/*                                                                              */
/*      Check the range of a value-based parameter                              */
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



package edu.brown.cs.catre.catmodel;

import edu.brown.cs.catre.catre.CatreDeviceHandler;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreWorld;

import java.util.Collection;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreDevice;

class CatmodelConditionRange extends CatmodelCondition 
      implements CatreDeviceHandler, CatreCondition, CatmodelConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreDevice	for_device;
private CatreParameter	cond_param;
private Number		low_value;
private Number		high_value;
private Boolean 	is_on;
private boolean 	is_trigger;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatmodelConditionRange(CatreDevice device,CatreParameter p,Number low,Number high,boolean trigger)
{
   super(device.getUniverse());
   low_value = low;
   high_value = high;
   for_device = device;
   cond_param = p;
   for_device.addDeviceHandler(this);
   is_on = null;
   is_trigger = trigger;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()
{
   StringBuffer buf = new StringBuffer();
   
   buf.append(for_device.getName());
   if (low_value != null && high_value != null) {
      buf.append("BETWEEN " + low_value + " AND " + high_value);
    }
   else if (low_value != null) buf.append(" ABOVE " + low_value);
   else if (high_value != null) buf.append(" BELOW " + high_value);
   
   return buf.toString();
}

@Override public String getDescription()
{
   return getName();
}


@Override public void getSensors(Collection<CatreDevice> rslt)
{
   if (for_device != null) rslt.add(for_device);
}



@Override public void setTime(CatreWorld world)
{
   if (!for_device.isEnabled()) return;
}


@Override public boolean isTrigger()			{ return is_trigger; }

@Override public boolean isBaseCondition()              { return true; }



@Override public void addImpliedProperties(CatrePropertySet ps)
{ }



/********************************************************************************/
/*										*/
/*	Handle state changes							*/
/*										*/
/********************************************************************************/

@Override public void stateChanged(CatreWorld w,CatreDevice s)
{
   if (!s.isEnabled()) {
      if (is_on == null) return;
      if (is_on == Boolean.TRUE) fireOff(w);
      is_on = null;
    }
   
   Object cvl = s.getValueInWorld(cond_param,w);
   boolean rslt = false;
   if (cvl != null && cvl instanceof Number) {
      Number nvl = (Number) cvl;
      double vl = nvl.doubleValue();
      if (low_value == null || vl >= low_value.doubleValue()) {
	 if (high_value == null || vl <= high_value.doubleValue()) {
	    rslt = true;
	  }
       }
    }
   
   // don't trigger on initial setting
   if (is_on == null && is_trigger) is_on = rslt;
   
   if (is_on != null && rslt == is_on && w.isCurrent()) return;
   is_on = rslt;
   
   CatreLog.logI("CATMODEL","CONDITION: " + getName() + " " + is_on);
   
   if (is_trigger) {
      fireTrigger(w,getResultProperties(cvl));
    }
   else if (rslt) {
      fireOn(w,getResultProperties(cvl));
    }
   else {
      fireOff(w);
    }
}



private CatmodelPropertySet getResultProperties(Object val) {
   CatmodelPropertySet ps = new CatmodelPropertySet();
   ps.put(cond_param.getName(),val.toString());
   return ps;
}




@Override public boolean isConsistentWith(CatreCondition bc)
{
   if (!(bc instanceof CatmodelConditionRange)) return true;
   CatmodelConditionRange sbc = (CatmodelConditionRange) bc;
   if (low_value != null && sbc.high_value != null &&
	 low_value.doubleValue() < sbc.high_value.doubleValue()) return false;
   if (high_value != null && sbc.low_value != null &&
	 high_value.doubleValue() > sbc.low_value.doubleValue()) return false;
   return true;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString() {
   return getName();
}


}       // end of class CatmodelConditionRange




/* end of CatmodelConditionRange.java */

