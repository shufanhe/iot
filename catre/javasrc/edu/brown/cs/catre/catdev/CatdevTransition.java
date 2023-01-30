/********************************************************************************/
/*										*/
/*		CatdevTransition.java						*/
/*										*/
/*	Basic implementation of a transition for a device			*/
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

import java.util.Collection;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreActionValues;
import edu.brown.cs.catre.catre.CatreDescribableBase;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreParameterSet;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTransition;
import edu.brown.cs.catre.catre.CatreTransitionType;
import edu.brown.cs.catre.catre.CatreUniverse;

public class CatdevTransition extends CatreDescribableBase
      implements CatreTransition, CatdevConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreParameterSet	default_parameters;
private CatreDevice		for_device;
private CatreTransitionType	transition_type;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatdevTransition(CatreDevice cd,CatreStore cs,Map<String,Object> map)
{
   super(null);
   
   for_device = cd;
   default_parameters = cd.getUniverse().createParameterSet();

   fromJson(cs,map);
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

protected void addParameter(CatreParameter p,Object defaultvalue)
{
   if (defaultvalue != null) {
      default_parameters.putValue(p,defaultvalue);
    }
   else {
      default_parameters.addParameter(p);
    }
}

public void setName(String nm)			{ super.setName(nm); }

public void setLabel(String lbl)		{ super.setLabel(lbl); }

public void setDescription(String desc) 	{ super.setDescription(desc); }



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public CatreDevice getDevice()	{ return for_device; }

@Override public CatreUniverse getUniverse()	{ return for_device.getUniverse(); }


@Override public CatreTransitionType getTransitionType()
{
   return transition_type;
}


@Override public CatreParameterSet getDefaultParameters()
{
   CatreParameterSet cps = getUniverse().createParameterSet();
   cps.putValues(default_parameters);
   return cps;
}

@Override public Collection<CatreParameter> getParameterSet()
{
   return default_parameters.getValidParameters();
}


@Override public CatreParameter getEntityParameter()	{ return null; }


@Override public CatreParameter findParameter(String nm)
{
   String nm1 = nm;
   String nm2 = nm;
   if (!nm.startsWith(getName() + NAME_SEP)) {
      nm1 = getName() + NAME_SEP + nm;
      nm2 = getName() + NAME_SEP + "SET" + NAME_SEP + nm;
    }
   for (CatreParameter up : default_parameters.getValidParameters()) {
      if (up.getName().equals(nm)) return up;
      if (up.getName().equals(nm1)) return up;
      if (up.getName().equals(nm2)) return up;
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public final void perform(CatreParameterSet params,CatrePropertySet props)
	throws CatreActionException
{
   CatreDevice device = getDevice();

   if (device == null) throw new CatreActionException("No device to act on");

   CatreActionValues avals = getUniverse().createActionValues(default_parameters);
   if (params != null) {
      for (CatreParameter cp : params.getValidParameters()) {
	 avals.put(cp.getName(),params.getValue(cp));
       }
    }
   if (props != null) {
      for (Map.Entry<String,String> ent : props.entrySet()) {
	 CatreParameter cp = findParameter(ent.getKey());
	 if (cp != null) {
	    avals.put(cp.getName(),cp.normalize(ent.getValue()));
	  }
       }
    }
   try {
      device.apply(this,avals);
    }
   catch (CatreActionException ex) {
      throw ex;
    }
   catch (Throwable t) {
      throw new CatreActionException("Action aborted",t);
    }
}



/********************************************************************************/
/*										*/
/*	IO Methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();

   rslt.put("DEFAULTS",default_parameters.toJson());

   CatreLog.logD("CATDEV","Transition yields " + rslt);

   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);

   default_parameters = getSavedSubobject(cs,map,"DEFAULTS",
	 getUniverse()::createSavedParameterSet,default_parameters);

   CatreLog.logD("CATDEV","Transition parameters " + default_parameters);

}


}	// end of class CatdevTransition




/* end of CatdevTransition.java */

