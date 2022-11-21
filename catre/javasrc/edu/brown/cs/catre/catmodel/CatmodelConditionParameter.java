/********************************************************************************/
/*                                                                              */
/*              CatmodelConditionParameter.java                                 */
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



package edu.brown.cs.catre.catmodel;

import java.util.Collection;

import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceHandler;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreWorld;

public class CatmodelConditionParameter extends CatmodelCondition implements CatreDeviceHandler
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreDevice	for_device;
private Object		for_state;
private CatreParameter	cond_param;
private Boolean 	is_on;
private boolean 	is_trigger;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatmodelConditionParameter(CatreDevice device,CatmodelParameter p,Object s)
{
   this(device,p,s,false);
}


public CatmodelConditionParameter(CatreDevice device,CatreParameter p,Object s,boolean trig)
{
   super((CatmodelUniverse) device.getUniverse());
   for_state = s;
   for_device = device;
   cond_param = p;
   device.addDeviceHandler(this);
   is_on = null;
   is_trigger = trig;
   setLabel(cond_param.getLabel() + " = " + for_state);
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()
{
   if (is_trigger) {
      return for_device + "->" + for_state;
    }
   return for_device.getName() + "=" + for_state;
}



@Override public String getDescription()
{
   return getName();
}

@Override public void getSensors(Collection<CatreDevice> rslt)
{
   rslt.add(for_device);
}



@Override public void setTime(CatreWorld world)
{
   if (!for_device.isEnabled()) return;
}



@Override public void addImpliedProperties(CatrePropertySet ups)
{
   ups.put(cond_param.getName(),for_state);
}



@Override public boolean isTrigger()
{
   return is_trigger;
}


@Override public boolean isBaseCondition()
{
   return true;
}


/********************************************************************************/
/*										*/
/*	Handle state changes							*/
/*										*/
/********************************************************************************/

@Override public void stateChanged(CatreWorld w,CatreDevice s)
{
   if (!s.isEnabled()) {
      if (is_on == null) return;
      if (is_on == Boolean.TRUE && !is_trigger) fireOff(w);
      is_on = null;
    }
   Object cvl = s.getValueInWorld(cond_param,w);
   boolean rslt = for_state.equals(cvl);
   if (is_on != null && rslt == is_on && w.isCurrent()) return;
   is_on = rslt;
   
   CatreLog.logI("CATMODEL","CONDITION: " + getName() + " " + is_on);
   if (rslt) {
      if (is_trigger) fireTrigger(w,getResultProperties());
      else fireOn(w,getResultProperties());
    }
   else if (!is_trigger) fireOff(w);
}

public CatreDevice getDevice()                { return for_device; }

public Object getState()                      { return for_state; }

public CatreParameter getParameter()          { return cond_param; }


private CatmodelPropertySet getResultProperties() {
   CatmodelPropertySet ps = new CatmodelPropertySet();
   ps.put(cond_param.getName(),for_state);
   return ps;
}




@Override protected boolean isConsistentWith(CatreCondition bc)
{
   if (!(bc instanceof CatmodelConditionParameter)) return true;
   CatmodelConditionParameter sbc = (CatmodelConditionParameter) bc;
   if (sbc.for_device != for_device) return true;
   if (!sbc.for_state.equals(for_state)) return false;
   return true;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString() {
   String r = for_device.getName() + "==" + for_state;
   if (is_trigger) r += "(T)";
   return r;
}

}       // end of class CatmodelConditionParameter




/* end of CatmodelConditionParameter.java */

