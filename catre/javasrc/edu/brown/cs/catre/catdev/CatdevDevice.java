/********************************************************************************/
/*										*/
/*		CatdevDevice.java						*/
/*										*/
/*	Basic device implementation						*/
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.brown.cs.catre.catre.CatreActionException;
import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreDescribableBase;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreDeviceListener;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreReferenceListener;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTransition;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.ivy.swing.SwingEventListenerList;



public abstract class CatdevDevice extends CatreDescribableBase implements CatreDevice,
      CatdevConstants, CatreReferenceListener
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected CatreUniverse for_universe;
private Boolean 	is_enabled;
private SwingEventListenerList<CatreDeviceListener> device_handlers;
private List<CatreParameter> parameter_set;
private List<CatreTransition> transition_set;
private CatreBridge	for_bridge;
private String		device_uid;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatdevDevice(CatreUniverse uu)
{
   this(uu,null);
}


protected CatdevDevice(CatreUniverse uu,CatreBridge bridge)
{
   super("DEV_");
   initialize(uu);
   for_bridge = bridge;
   
   CatreLog.logD("CATDEV","Create device " + device_uid + " " +
         getBridge());
}



private void initialize(CatreUniverse uu)
{
   for_universe = uu;
   device_handlers = new SwingEventListenerList<>(CatreDeviceListener.class);

   device_uid = CatreUtil.randomString(24);
   is_enabled = true;
   parameter_set = new ArrayList<>();
   transition_set = new ArrayList<>();
   for_bridge = null;
}


@Override public boolean validateDevice()
{
   if (getName() == null || getName().equals("")) return false;
   if (getDeviceId() == null || getDeviceId().equals("")) return false;

   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Handle new version of device available                                  */
/*                                                                              */
/********************************************************************************/

@Override public void update(CatreDevice cd)
{
   CatreLog.logD("CATDEV","Update device " + cd.getName());
   
   boolean chng = false;
   if (!getName().equals(cd.getName())) {
      setName(cd.getName());
      chng = true;
    }
   if (!getLabel().equals(cd.getLabel())) {
      setLabel(cd.getName());
      chng = true;
    }
   if (!getDescription().equals(cd.getDescription())) {
      setDescription(cd.getName());
      chng = true;
    }
   
   List<CatreParameter> nset = new ArrayList<>(cd.getParameters());
   for (CatreParameter cp : nset) {
      CatreParameter oldcp = findParameter(cp.getName());
      if (oldcp == null) chng = true;
    }
   if (nset.size() != parameter_set.size()) chng = true;
   parameter_set = nset;
   
   List<CatreTransition> ntrn = new ArrayList<>(cd.getTransitions());
   for (CatreTransition ct : ntrn) {
      CatreTransition oldct = findTransition(ct.getName());
      if (oldct == null) chng = true;
    }
   if (ntrn.size() != transition_set.size()) chng = true;
   transition_set = ntrn;
   
   setEnabled(cd.isEnabled());
   
   if (chng) fireUpdated();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public CatreUniverse getUniverse()	{ return for_universe; }
@Override public CatreBridge getBridge()	{ return for_bridge; }



@Override public boolean isDependentOn(CatreDevice d)
{
   return false;
}


@Override public String getDeviceId()		{ return device_uid; }

protected void setDeviceId(String did)		{ device_uid = did; }


@Override public boolean isCalendarDevice()	{ return false; }



/********************************************************************************/
/*										*/
/*	Parameter methods							*/
/*										*/
/********************************************************************************/

@Override public Collection<CatreParameter> getParameters()
{
   return parameter_set;
}


@Override public CatreParameter findParameter(String id)
{
   if (id == null) return null;

   for (CatreParameter up : parameter_set) {
      if (up.getName().equals(id)) return up;
      if (up.getLabel().equals(id)) return up;
    }

   return null;
}


@Override public CatreTransition findTransition(String id)
{
   for (CatreTransition ct : transition_set) {
      if (ct.getName().equals(id))
	 return ct;
    }
   return null;
}



public CatreParameter addParameter(CatreParameter p)
{
   for (CatreParameter up : parameter_set) {
      if (up.getName().equals(p.getName())) return up;
    }

   parameter_set.add(p);

   return p;
}



public CatreTransition addTransition(CatreTransition t)
{
// for (CatreTransition ut : transition_set) {
//    if (ut.getName().equals(t.getName())) return ut;
//  }
   int idx = 0;
   for (Iterator<CatreTransition> it = transition_set.iterator(); it.hasNext(); ) {
      CatreTransition ct = it.next();
      if (ct.getName().equals(t.getName())) {
	 it.remove();
	 transition_set.add(idx,t);
	 return t;
       }
      ++idx;
    }

   transition_set.add(t);
   return t;
}


@Override public Collection<CatreTransition> getTransitions()
{
   return transition_set;
}


@Override public boolean hasTransitions()
{
   if (transition_set == null || transition_set.size() == 0) return false;
   return true;
}





@Override public CatreTransition createTransition(CatreStore cs,Map<String,Object> map)
{
   CatreTransition cd = null;
   if (for_bridge != null) {
      cd = for_bridge.createTransition(this,cs,map);
      if (cd != null) return cd;
    }

   cd = new CatdevTransition(this,cs,map);

   return cd;
}



/********************************************************************************/
/*										*/
/*	Device handler commands 						*/
/*										*/
/********************************************************************************/

@Override public void addDeviceListener(CatreDeviceListener hdlr)
{
   device_handlers.add(hdlr);
}


@Override public void removeDeviceListener(CatreDeviceListener hdlr)
{
   device_handlers.remove(hdlr);
}


protected void fireChanged(CatreParameter p)
{
   for_universe.startUpdate();
   try {
      for (CatreDeviceListener hdlr : device_handlers) {
	 try {
	    hdlr.stateChanged(p);
	  }
	 catch (Throwable t) {
	    CatreLog.logE("CATMODEL","Problem with device handler",t);
	  }
       }
    }
   finally {
      for_universe.endUpdate();
    }
}



protected void fireEnabled()
{
   for (CatreDeviceListener hdlr : device_handlers) {
      try {
	 hdlr.deviceEnabled(this,is_enabled);
       }
      catch (Throwable t) {
	 CatreLog.logE("CATMODEL","Problem with device handler",t);
       }
    }
}


protected void fireUpdated()
{
   for (CatreDeviceListener hdlr : device_handlers) {
      try {
	 hdlr.deviceUpdated(this);
       }
      catch (Throwable t) {
	 CatreLog.logE("CATMODEL","Problem with device handler",t);
       }
    }
}


/********************************************************************************/
/*										*/
/*	State update methods							*/
/*										*/
/********************************************************************************/

@Override public final void startDevice()
{
   is_enabled = null;

   setEnabled(isDeviceValid());
}

protected boolean isDeviceValid()			{ return true; }

protected void localStartDevice()			{ }

protected void localStopDevice()			{ }



@Override public Object getParameterValue(CatreParameter p)
{
   if (!isEnabled()) return null;

   checkCurrentState();
   return for_universe.getValue(p);
}



@Override public void setParameterValue(CatreParameter p,Object val)
{
   CatreLog.logD("CATDEV","SET PARAMETER " + p.getName() + " " + val + " " +
         (val == null ? "?" : val.getClass()) + " " + isEnabled());
   
   if (!isEnabled()) return;

   val = p.normalize(val);

   Object prev = getParameterValue(p);
   if ((val == null && prev == null) || (val != null && val.equals(prev))) {
      return;
    }

   for_universe.setValue(p,val);

   CatreLog.logI("CATMODEL","Set " + getName() + "." + p + " = " + val);

   fireChanged(p);
}



protected void checkCurrentState()		{ updateCurrentState(); }
protected void updateCurrentState()		{ }








@Override public void setEnabled(boolean fg)
{
   if (is_enabled != null && fg == is_enabled) return;
   
   CatreLog.logD("CATDEV","Device ENABLED " + device_uid + " " + fg);

   is_enabled = fg;

   if (fg) {
      localStartDevice();
    }
   else {
      localStopDevice();
    }

   fireEnabled();
}


@Override public boolean isEnabled()		{ return is_enabled; }


@Override public void referenceValid(boolean fg)
{
   setEnabled(fg);
}



/********************************************************************************/
/*										*/
/*	Transition methods							*/
/*										*/
/********************************************************************************/

@Override public void apply(CatreTransition t,Map<String,Object> vals)
      throws CatreActionException
{
   if (for_bridge != null) {
      for_bridge.applyTransition(this,t,vals);
    }
   else {
      throw new CatreActionException("Transition not allowed");
    }
}



/********************************************************************************/
/*										*/
/*	Database methods							*/
/*										*/
/********************************************************************************/

@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);

   device_uid = getSavedString(map,"UID",device_uid);

   is_enabled = true;
   String bnm = getSavedString(map,"BRIDGE",null);
   if (bnm != null && for_universe != null) {
      for_bridge = for_universe.findBridge(bnm);
      if (for_bridge == null) {
         CatreLog.logD("CATDEV","Device disabled because of bridge " + for_bridge + " " +
               device_uid);
         is_enabled = false;
       }
    }

   List<CatreParameter> plst = getSavedSubobjectList(cs,map,"PARAMETERS",
	 for_universe::createParameter,parameter_set);
   for (CatreParameter p : plst) {
      addParameter(p);
    }

   transition_set = getSavedSubobjectList(cs,map,"TRANSITIONS",
	 this::createTransition,transition_set);
}




@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();

   if (for_bridge != null) rslt.put("BRIDGE",for_bridge.getName());

   rslt.put("UID",device_uid);
   rslt.put("ENABLED",isEnabled());
   rslt.put("PARAMETERS",getSubObjectArrayToSave(parameter_set));
   rslt.put("TRANSITIONS",getSubObjectArrayToSave(transition_set));
   rslt.put("ISCALENDAR",isCalendarDevice());

   return rslt;
}


}	// end of class CatdevDevice




/* end of CatdevDevice.java */

