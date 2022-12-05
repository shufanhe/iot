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

import edu.brown.cs.catre.catre.CatreSubSavableBase;
import edu.brown.cs.catre.catre.CatreTransition;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreWorld;

import java.util.Map;

import edu.brown.cs.catre.catre.CatreAction;
import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;

class CatprogAction extends CatreSubSavableBase implements CatreAction, CatprogConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreUniverse   for_universe;
private CatreDevice     for_entity;
private CatreTransition	for_transition;
private CatreParameterSet parameter_set;
private String		action_description;
private String		action_label;
private boolean 	is_trigger;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatprogAction(CatreDevice e,CatreTransition t)
{
   super("ACTION_");
   
   for_universe = e.getUniverse();
   for_entity = e;
   for_transition = t;
   parameter_set = e.getUniverse().createParameterSet();
   if (t != null) parameter_set.putAll(t.getDefaultParameters());
   action_description = null;
   action_label = null;
   is_trigger = false;
}



public CatprogAction(CatreUniverse universe,CatreStore cs,Map<String,Object> map)
{
   super("ACTION_");
   
   for_universe = universe;
   
   fromJson(cs,map);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()
{
   if (for_transition == null) return for_entity.getName() + "^no_action";
   
   return for_entity.getName() + "^" + for_transition.getName();
}

@Override public String getDescription()
{
   if (action_description != null) return action_description;
   
   if (for_transition == null) {
      return "Do nothing to " + for_entity.getName();
    }
   
   return "Apply " + for_transition.getName() + " to " + for_entity.getName();
}

@Override public String getLabel()
{
   if (action_label != null) return action_label;
   
   return getDescription();
}

@Override public void setLabel(String l)
{
   action_label = l;
}


@Override public boolean isTriggerAction()
{
   return is_trigger;
}


@Override public void setIsTriggerAction(boolean fg)
{
   is_trigger = fg;
}



@Override public void setDescription(String d)
{
   action_description = d;
}

@Override public CatreDevice getDevice() 		{ return for_entity; }

@Override public CatreTransition getTransition() 	{ return for_transition; }



/********************************************************************************/
/*										*/
/*	Parameter methods							*/
/*										*/
/********************************************************************************/

@Override public void setParameters(CatreParameterSet ps)
{
   parameter_set.clear();
   parameter_set.putAll(ps);
}

@Override public void addParameters(CatreParameterSet ps)
{
   parameter_set.putAll(ps);
}

@Override public CatreParameterSet getParameters()
{
   return parameter_set;
}


@Override public void addImpliedProperties(CatrePropertySet ups)
{
   CatrePropertySet aps = for_universe.createPropertySet(parameter_set);
   ups.putAll(aps);
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void perform(CatreWorld w,CatrePropertySet ps)
throws CatreActionException
{
   CatrePropertySet ups = w.getUniverse().createPropertySet();
   if (ps != null && !ps.isEmpty()) {
      ups.putAll(ps);
    }
   
   if (for_transition != null) for_transition.perform(w,for_entity,ups);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();

   rslt.put("CLASS",getClass().getName());
   rslt.put("NAME",getName());
   rslt.put("LABEL",getLabel());
   rslt.put("DESC",getDescription());
   if (for_entity != null) rslt.put("DEVICE",for_entity.getDataUID());
   if (for_transition != null) rslt.put("TRANSITION",for_transition.toJson());
   if (getParameters() != null) rslt.put("PARAMETERS",getParameters().toJson());
   
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   for_entity = null;
   for_transition = null;
   for_entity = getSavedSubobject(cs,map,"DEVICE",for_universe::findDevice,for_entity);
   for_transition = getSavedSubobject(cs,map,"TRANSITION",
         for_entity::createTransition,for_transition);
   CatreParameterSet ps = null;
   if (for_transition != null) ps = for_transition.getDefaultParameters();
   parameter_set = getSavedSubobject(cs,map,"PARAMETERS",for_universe::createParameterSet,parameter_set);
   if (ps != null) {
      //TODO: set default parameters in parameter_set if not otherwise set
    }
   action_description = getSavedString(map,"DESCRIPTION",action_description);
   action_label = getSavedString(map,"LABEL",action_label);
}



}       // end of class CatprogAction




/* end of CatprogAction.java */

