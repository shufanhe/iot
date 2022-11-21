/********************************************************************************/
/*                                                                              */
/*              CatmodelConditionTime.java                                      */
/*                                                                              */
/*      Time based condition that can have repeats                              */
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

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;


import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.catre.catre.CatreWorld;

class CatmodelConditionTime extends CatmodelCondition implements CatmodelConstants, CatreCondition
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatmodelCalendarEvent	calendar_event;
private String			condition_name;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatmodelConditionTime(CatmodelUniverse uu,String name,CatmodelCalendarEvent evt)
{
   super(uu);
   condition_name = name;
   calendar_event = evt;
   setupTimer();
   setCurrent();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()
{
   if (condition_name == null || condition_name.equals("") ||
	 condition_name.equals("null")) {
      condition_name = CatreUtil.randomString(16);
    }
   
   return condition_name;
}

@Override public String getLabel()
{
   String s = super.getLabel();
   if (s == null) s = condition_name;
   if (s == null) s = calendar_event.getDescription();
   return s;
}

@Override public String getDescription()
{
   if (condition_name == null) return calendar_event.getDescription();
   return condition_name + " @ " + calendar_event.getDescription();
}


@Override public boolean isBaseCondition()              { return true; }


@Override public void getSensors(Collection<CatreDevice> rslt)   { }




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void setTime(CatreWorld w)
{
   if (calendar_event.isActive(w.getTime())) {
      CatreLog.logI("CATMODEL","CONDITION " + getLabel() + " ACTIVE");
      fireOn(w,null);
    }
   else {
      fireOff(w);
      CatreLog.logI("CATMODEL","CONDITION " + getLabel() + " INACTIVE");
    }
}



private void setupTimer()
{
   long delay = T_DAY;			// check at least every day
   long now = System.currentTimeMillis();
   Calendar c0 = Calendar.getInstance();
   c0.setTimeInMillis(now);
   Calendar c1 = Calendar.getInstance();
   c1.setTimeInMillis(now + delay);
   List<Calendar> slots = calendar_event.getSlots(c0,c1);
   if (slots != null && slots.size() > 0) {
      Calendar s0 = slots.get(0);
      long t0 = s0.getTimeInMillis();
      long t1 = 0;
      if (slots.size() > 1) {
	 Calendar s1 = slots.get(1);
	 t1 = s1.getTimeInMillis();
       }
      if (t0 == 0 || t0 <= now) {
	 setCurrent();
	 delay = t1-now;
       }
      else delay = t0-now;
    }
   getCatre().schedule(new CondChecker(),delay);
}



private void setCurrent()
{
   CatreWorld cw = getUniverse().getCurrentWorld();
   setTime(cw);
}



/********************************************************************************/
/*										*/
/*	Conflict detection							*/
/*										*/
/********************************************************************************/

protected boolean isConsistentWith(CatreCondition uc)
{
   if (!(uc instanceof CatmodelConditionTime)) return true;
   CatmodelConditionTime bcc = (CatmodelConditionTime) uc;
   
   if (calendar_event.canOverlap(bcc.calendar_event)) return true;
   
   return false;
}



@Override public void addImpliedProperties(CatrePropertySet ups)
{
   calendar_event.addImpliedProperties(ups);
}



/********************************************************************************/
/*										*/
/*	Timer-based condition checker						*/
/*										*/
/********************************************************************************/

private class CondChecker extends TimerTask {
   
   @Override public void run() {
      setCurrent();
      setupTimer();
    }
   
}	// end of inner class CondChecker



}       // end of class CatmodelConditionTime




/* end of CatmodelConditionTime.java */

