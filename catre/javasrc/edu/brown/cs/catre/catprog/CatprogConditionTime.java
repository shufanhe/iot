/********************************************************************************/
/*										*/
/*		CatprogConditionTime.java					*/
/*										*/
/*	Time-based condition that can have repeats				*/
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


import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import edu.brown.cs.catre.catre.CatreTimeSlotEvent;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatreStore;


class CatprogConditionTime extends CatprogCondition
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreTimeSlotEvent	calendar_event;
private boolean is_active;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogConditionTime(CatreProgram pgm,CatreStore cs,Map<String,Object> map)
{
   super(pgm,cs,map);

   is_active = false;
}


private CatprogConditionTime(CatprogConditionTime cc)
{
   super(cc);

   calendar_event = cc.calendar_event;
   is_active = false;
}


@Override public CatreCondition cloneCondition()
{
   return new CatprogConditionTime(this);
}


@Override public void activate()
{
   if (is_active) return;
   is_active = true;
   setupTimer();
   setTime();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void setTime()
{
   long now = getUniverse().getTime();
   if (calendar_event.isActive(now)) {
      CatreLog.logI("CATPROG","CONDITION " + getLabel() + " ACTIVE");
      fireOn(null);
    }
   else {
      fireOff();
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
	 setTime();
	 delay = t1-now;
       }
      else delay = t0-now;
    }
   getCatre().schedule(new CondChecker(),delay);
}



/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
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
      setTime();
      setupTimer();
    }

}	// end of inner class CondChecker



}	// end of class CatprogConditionTime




/* end of CatprogConditionTime.java */

