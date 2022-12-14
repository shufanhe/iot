/********************************************************************************/
/*                                                                              */
/*              CatprogConditionTime.java                                       */
/*                                                                              */
/*      Time-based condition that can have repeats                              */
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


import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import edu.brown.cs.catre.catre.CatreCalendarEvent;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreWorld;


class CatprogConditionTime extends CatprogCondition
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreCalendarEvent	calendar_event;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogConditionTime(CatreProgram pgm,String name,CatreCalendarEvent evt)
{
   super(pgm,getUniqueName(name,evt));
   calendar_event = evt;
   
   setName(name);
   setLabel(evt.getDescription());
   setDescription(name + " @ " + calendar_event.getDescription());
   
   setupTimer();
   setCurrent();
}


CatprogConditionTime(CatreProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);
   
   setUID(getUniqueName(getName(),calendar_event));
   
   setupTimer();
   setCurrent();
}


private static String getUniqueName(String name,CatreCalendarEvent evt)
{
   return name + "_" + evt.getName();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public void getDevices(Collection<CatreDevice> rslt)   { }



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void setTime(CatreWorld w)
{
   if (calendar_event.isActive(w.getTime())) {
      CatreLog.logI("CATPROG","CONDITION " + getLabel() + " ACTIVE");
      fireOn(w,null);
    }
   else {
      fireOff(w);
      CatreLog.logI("CATPROG","CONDITION " + getLabel() + " INACTIVE");
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
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("TYPE","Timer");
   rslt.put("EVENT",calendar_event.toJson());
   
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   calendar_event = getSavedSubobject(cs,map,"EVENT",
         getUniverse()::createCalendarEvent,calendar_event);
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



}       // end of class CatprogConditionTime




/* end of CatprogConditionTime.java */

