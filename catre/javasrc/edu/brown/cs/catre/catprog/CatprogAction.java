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
import edu.brown.cs.catre.catre.CatreTransitionRef;
import edu.brown.cs.catre.catre.CatreUniverse;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreAction;
import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreDescribableBase;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreReferenceListener;
import edu.brown.cs.catre.catre.CatreStore;

class CatprogAction extends CatreDescribableBase
      implements CatreAction, CatprogConstants, CatreReferenceListener
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreUniverse   for_universe;
private CatreTransitionRef transition_ref;
private Map<String,Object> parameter_values;
private boolean 	is_trigger;
private boolean         is_valid;
private boolean         needs_name;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogAction(CatreProgram p,CatreStore cs,Map<String,Object> map)
{
   super("ACTION_");
   
   needs_name = false;
   is_valid = false;
   parameter_values = new HashMap<>();
   
   for_universe = p.getUniverse();
   
   fromJson(cs,map);
   
   setActionName();
   
   setValid(transition_ref.isValid());
}


private void setActionName()
{
   if (!needs_name && getName() != null && !getName().equals("")) return;
   
   needs_name = false;
   String dnm = transition_ref.getDeviceId();
   if (transition_ref.isValid()) {
      dnm = transition_ref.getDevice().getName();
    }
   else needs_name = true;
   
   StringBuffer buf = new StringBuffer();
   buf.append(dnm);
   buf.append(".");
   buf.append(transition_ref.getTransitionName());
   buf.append("(");
   int ct = 0;
   for (String pnm : parameter_values.keySet()) {
      if (ct++ > 0) buf.append(",");
      buf.append(pnm);
      buf.append("=");
      buf.append(parameter_values.get(pnm));
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


@Override public CatreDevice getDevice() 		{ return transition_ref.getDevice(); }

@Override public CatreTransition getTransition() 	{ return transition_ref.getTransition(); }

@Override public boolean isValid()                      { return is_valid; }

protected void setValid(boolean fg)
{
   is_valid = fg;
}


/********************************************************************************/
/*										*/
/*	Parameter methods							*/
/*										*/
/********************************************************************************/

@Override public CatreParameterSet getParameters() throws CatreActionException
{
   if (!isValid()) throw new CatreActionException("Invalid Action");
   
   CatreTransition ct = transition_ref.getTransition();
   CatreParameterSet params = ct.getDefaultParameters();
   for (String cp : parameter_values.keySet()) {
      params.setParameter(cp,parameter_values.get(cp));
    }
   return params;
}



/********************************************************************************/
/*                                                                              */
/*      Handle state changes                                                    */
/*                                                                              */
/********************************************************************************/

@Override public void referenceValid(boolean fg)
{
   if (fg == isValid()) return;
   
   if (needs_name) setActionName();
   
   setValid(fg);
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void perform(CatrePropertySet ps)
        throws CatreActionException
{
   if (!isValid()) throw new CatreActionException("Invalid Action");
 
   CatreParameterSet params = getParameters();
   
   transition_ref.getTransition().perform(params,ps);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();

   rslt.put("TRANSITION",transition_ref.toJson());
   
   rslt.put("PARAMETERS",parameter_values);
   
   return rslt;
}


@SuppressWarnings("unchecked")
@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   transition_ref = getSavedSubobject(cs,map,"TRANSITION",this::createTransitionRef,transition_ref);
   
   Object obj = map.get("PARAMETERS");
   Map<String,Object> pmap = null;
   parameter_values = new HashMap<>();
   if (obj instanceof Map) {
      pmap = (Map<String,Object>) obj;
    }
   else if (obj instanceof JSONObject) {
      pmap = ((JSONObject) obj).toMap();
    }
   for (String k : pmap.keySet()) {
      Object v = pmap.get(k);
      parameter_values.put(k,v);
    }
}


private CatreTransitionRef createTransitionRef(CatreStore cs,Map<String,Object> map)
{
   return for_universe.createTransitionRef(this,cs,map);
}


}       // end of class CatprogAction




/* end of CatprogAction.java */

