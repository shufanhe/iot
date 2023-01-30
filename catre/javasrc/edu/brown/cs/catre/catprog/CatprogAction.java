/********************************************************************************/
/*										*/
/*		CatprogAction.java						*/
/*										*/
/*	Base class for actions							*/
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

private CatreUniverse	for_universe;
private CatreTransitionRef transition_ref;
private Map<String,Object> parameter_values;
private boolean 	is_trigger;
private boolean 	is_valid;
private boolean 	needs_name;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogAction(CatreProgram p,CatreStore cs,Map<String,Object> map)
{
   super("ACTION_");

   needs_name = true;
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


@Override public CatreDevice getDevice()		{ return transition_ref.getDevice(); }

@Override public CatreTransition getTransition()	{ return transition_ref.getTransition(); }

@Override public boolean isValid()			{ return is_valid; }

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
/*										*/
/*	Handle state changes							*/
/*										*/
/********************************************************************************/

@Override public void referenceValid(boolean fg)
{
   if (fg == isValid()) return;

   if (fg && needs_name) setActionName();

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
   
   rslt.put("NEEDSNAME",needs_name);

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
   
   needs_name = getSavedBool(map,"NEEDSNAME",false);
   
   transition_ref.initialize();
}


private CatreTransitionRef createTransitionRef(CatreStore cs,Map<String,Object> map)
{
   return for_universe.createTransitionRef(this,cs,map);
}


}	// end of class CatprogAction




/* end of CatprogAction.java */

