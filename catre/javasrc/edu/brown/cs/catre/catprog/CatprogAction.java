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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreAction;
import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreDescribableBase;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatreProgram;
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
private CatreTransition for_transition;
private CatreParameterSet parameter_set;
private boolean 	is_trigger;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogAction(CatreProgram p,CatreStore cs,Map<String,Object> map)
{
   super("ACTION_");
   
   for_universe = p.getUniverse();
   
   fromJson(cs,map);
   
   setActionName();
   
   // validate action definition, set enabled/disabled based on transition ref
}



private void setActionName()
{
   if (getName() != null && !getName().equals("")) return;
   
   if (for_transition == null) {
      // user transition reference values
      setName("Unknown Device/transition");
      return;
    }
   
   CatreDevice cd = for_transition.getDevice();
   
   StringBuffer buf = new StringBuffer();
   buf.append(cd.getName());
   buf.append(".");
   buf.append(for_transition.getName());
   buf.append("(");
   int ct = 0;
   for (CatreParameter cp : parameter_set.getValidParameters()) {
      Object o = parameter_set.getValue(cp);
      String s = cp.unnormalize(o);
      if (ct++ > 0) buf.append(",");
      buf.append(cp.getName());
      buf.append("=");
      buf.append(s);
    }
   buf.append(")");
   setName(buf.toString());
      
   // setLabel and setDescription
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


@Override public CatreDevice getDevice() 		{ return for_transition.getDevice(); }

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

   rslt.put("DEVICE",getDevice().getDeviceId());
   rslt.put("TRANSITION",for_transition.getName());
   if (getParameters() != null) {
      Map<String,String> vals = new HashMap<>();
      for (CatreParameter cp : parameter_set.getValidParameters()) {
         Object val = parameter_set.getValue(cp);
         if (val != null) {
            vals.put(cp.getName(),cp.unnormalize(val));
          }
       }
      rslt.put("PARAMETERS",vals);
    }
   
   return rslt;
}


@SuppressWarnings("unchecked")
@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   for_transition = null;
   CatreDevice cd = getSavedSubobject(cs,map,"DEVICE",for_universe::findDevice,null);
   if (cd != null) {
      for_transition = getSavedSubobject(cs,map,"TRANSITION",
            cd::createTransition,for_transition);
    }
   
   parameter_set = for_transition.getDefaultParameters();
   Object obj = map.get("PARAMETERS");
   Map<String,Object> pmap = null;
   if (obj instanceof Map) {
      pmap = (Map<String,Object>) obj;
   }
   else if (obj instanceof JSONObject) {
      pmap = ((JSONObject) obj).toMap();
    }
   for (Map.Entry<String,Object> ent : pmap.entrySet()) {
      String key = ent.getKey();
      parameter_set.putValue(key,ent.getValue());
    }
}



}       // end of class CatprogAction




/* end of CatprogAction.java */

