/********************************************************************************/
/*                                                                              */
/*              CatmodelCalendarEvent.java                                      */
/*                                                                              */
/*      Implementation of a possibly recurring calendar-based event             */
/*                                                                              */
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



package edu.brown.cs.catre.catmodel;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import edu.brown.cs.catre.catre.CatreTimeSlotEvent;
import edu.brown.cs.catre.catre.CatreDescribableBase;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;

 
class CatmodelCalendarEvent extends CatreDescribableBase implements CatreTimeSlotEvent, CatmodelConstants
{ 
 


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Calendar        from_datetime;
private Calendar        to_datetime;
private BitSet		day_set;
private int		repeat_interval;
private Set<Calendar>	exclude_dates;
private boolean         all_day;

private static DateFormat date_format = DateFormat.getDateInstance(DateFormat.SHORT);
private static DateFormat time_format = DateFormat.getTimeInstance(DateFormat.SHORT);


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatmodelCalendarEvent(CatreStore cs,Map<String,Object> map)
{ 
   super(cs,map);
   
   String rslt = "";
   rslt += calDate(from_datetime);
   if (to_datetime != null) rslt += " - " + calDate(to_datetime);
   else rslt += " ON";
   rslt += "UNTIL " + calDate(to_datetime);
   rslt += " from ";
   rslt += calTime(from_datetime);
   if (to_datetime != null) {
      rslt += " - ";
      rslt += calTime(to_datetime);
    }
   setGeneratedDescription(rslt);
   setLabel(rslt);
   setName(rslt.replace(" ","_"));
}





private void normalizeTimes() 
{
   if (to_datetime == null && from_datetime != null) {
      to_datetime = (Calendar) from_datetime.clone();
      to_datetime.add(Calendar.HOUR,1);
    }
}


/********************************************************************************/
/*										*/
/*	Access Methods								*/
/*										*/
/********************************************************************************/

private String calDate(Calendar c)
{
   return date_format.format(c.getTime());
}


private String calTime(Calendar c)
{
   return time_format.format(c.getTime());
}


public void makeAllDay(boolean fg)
{
   all_day = fg;
   if (all_day) {
      from_datetime = CatreTimeSlotEvent.startOfDay(from_datetime);
      to_datetime = CatreTimeSlotEvent.startOfNextDay(from_datetime);
    }
}



public void setRepeat(int dayinterval)
{
   repeat_interval = dayinterval;
}


public void setDays(String days)
{
   day_set = getDaySet(days);
}


public void setDays(BitSet days)
{
   days = (BitSet) days.clone();
}



public static BitSet getDaySet(String days)
{
   if (days == null || days.length() == 0) return null;
   
   BitSet dayset = new BitSet();
   days = days.toUpperCase();
   if (days.contains("MON")) dayset.set(Calendar.MONDAY);
   if (days.contains("TUE")) dayset.set(Calendar.TUESDAY);
   if (days.contains("WED")) dayset.set(Calendar.WEDNESDAY);
   if (days.contains("THU")) dayset.set(Calendar.THURSDAY);
   if (days.contains("FRI")) dayset.set(Calendar.FRIDAY);
   if (days.contains("SAT")) dayset.set(Calendar.SATURDAY);
   if (days.contains("SUN")) dayset.set(Calendar.SUNDAY);
   if (dayset.isEmpty()) dayset = null;
   
   return dayset;
}

String getDays()
{
   if (day_set == null) return null;
   StringBuffer buf = new StringBuffer();
   if (day_set.get(Calendar.MONDAY)) buf.append("MON,");
   if (day_set.get(Calendar.TUESDAY)) buf.append("TUE,");
   if (day_set.get(Calendar.WEDNESDAY)) buf.append("WED,");
   if (day_set.get(Calendar.THURSDAY)) buf.append("THU,");
   if (day_set.get(Calendar.FRIDAY)) buf.append("FRI,");
   if (day_set.get(Calendar.SATURDAY)) buf.append("SAT,");
   if (day_set.get(Calendar.SUNDAY)) buf.append("SUN,");
   
   String s = buf.toString();
   int idx = s.lastIndexOf(",");
   if (idx > 0) s = s.substring(0,idx);
   return s;
}



void addImpliedProperties(CatrePropertySet ups)
{
   Date d0 = new Date(from_datetime.getTimeInMillis());
   ups.put("*FROMDATE",date_format.format(d0));
   ups.put("*FROMTIME",time_format.format(d0));
   d0 = new Date(to_datetime.getTimeInMillis());
   ups.put("*TODATE",date_format.format(d0));
   ups.put("*TOTIME",time_format.format(d0));
   if (day_set != null) ups.put("*DAYS",getDays());
}



public void addExcludedDate(Calendar date)
{
   if (date == null) exclude_dates = null;
   else {
      date = CatreTimeSlotEvent.startOfDay(date);
      if (exclude_dates == null) exclude_dates = new HashSet<>();
      exclude_dates.add(date);
    }
}



/********************************************************************************/
/*										*/
/*	Methods to query the event						*/
/*										*/
/********************************************************************************/

@Override public List<Calendar> getSlots(Calendar from,Calendar to)
{
   List<Calendar> rslt = new ArrayList<Calendar>();
   if (from.after(to_datetime)) return rslt;
   if (to.before(from_datetime)) return rslt;
   
   Calendar fday = CatreTimeSlotEvent.startOfDay(from);
   Calendar tday = CatreTimeSlotEvent.startOfNextDay(to);
   
   boolean usetimes = false;
   if (day_set != null && !day_set.isEmpty()) usetimes = true;
   if (repeat_interval > 0) usetimes = true;
   if (exclude_dates != null) usetimes = true;
   
   for (Calendar day = fday; day.before(tday); day.add(Calendar.DAY_OF_YEAR,1)) {
      if (!isDayRelevant(day)) continue;
      if (day.before(from_datetime)) continue;
      if (!day.before(to_datetime)) continue;
      // the day is relevant and in range at this point
      // compute the start and stop time on this day
      Calendar start = null;
      Calendar end = null;
      if (sameDay(from,day)) start = setDateAndTime(day,from);
      else start = CatreTimeSlotEvent.startOfDay(day);
      if (sameDay(to,day)) end = setDateAndTime(day,to);
      else end = CatreTimeSlotEvent.startOfNextDay(day);
      
      boolean usefromtime = usetimes;
      if (sameDay(from_datetime,day)) usefromtime = true;
      if (usefromtime) {
         Calendar estart = setDateAndTime(day,from_datetime);
         if (estart.after(start)) start = estart;
       }
      
      boolean usetotime = usetimes;
      if (isNextDay(day,to_datetime)) usetotime = true;
      if (usetotime) {
         Calendar endt = setDateAndTime(day,to_datetime);
         if (endt.before(end)) end = endt;
       }
      
      if (end.compareTo(start) <= 0) continue;
      rslt.add(start);
      rslt.add(end);
    }
   
   return rslt;
}
@Override public boolean isActive(long when)
{
   CatreLog.logD("CATMODEL","Check time active: " +
         when + " " + to_datetime + " " + from_datetime + " " +
         day_set + " " + repeat_interval);
   
   Calendar cal = Calendar.getInstance();
   cal.setTimeInMillis(when);
   if (cal.after(to_datetime)) return false;
   if (cal.before(from_datetime)) return false;
   Calendar day = CatreTimeSlotEvent.startOfDay(cal);
   if (!isDayRelevant(day)) return false;
   Calendar dstart = CatreTimeSlotEvent.startOfDay(day);
   Calendar dend = CatreTimeSlotEvent.startOfNextDay(day);
   
   boolean usetimes = false;
   if (day_set != null && !day_set.isEmpty()) usetimes = true;
   if (repeat_interval > 0) usetimes = true;
   if (exclude_dates != null) usetimes = true;	
   boolean usefromtime = usetimes;
   if (sameDay(from_datetime,day)) usefromtime = true;
   if (usefromtime) {
      dstart = setDateAndTime(day,from_datetime);
    }
   boolean usetotime = usetimes;
   if (isNextDay(day,to_datetime)) usetotime = true;
   if (usetotime) {
      Calendar endt = setDateAndTime(day,to_datetime);
      if (endt.before(dend)) dend = endt;
    }
   
   CatreLog.logD("CATMODEL","Check times " + dend + " " +
         dstart + " " + cal + " " + usetimes + " " + usefromtime +
         " " + usetotime);
   
   if (dend.compareTo(dstart) <= 0) return false;
   if (cal.before(dstart)) return false;
   if (cal.after(dend)) return false;
   
   return true;
}



private boolean isDayRelevant(Calendar day)
{
   // assume that day has time cleared
   
   if (day_set != null) {
      int dow = day.get(Calendar.DAY_OF_WEEK);
      if (!day_set.get(dow)) return false;
    }
   
   if (repeat_interval > 0) {
      long d0 = from_datetime.getTimeInMillis();
      long d1 = day.getTimeInMillis();
      long delta = (d1-d0 + 12*T_HOUR);
      delta /= T_DAY;
      if (day_set != null) delta = (delta / 7) * 7;
      if ((delta % repeat_interval) != 0) return false;
    }
   else if (repeat_interval < 0) {
      if (day_set != null) {
	 if (day.get(Calendar.WEEK_OF_MONTH) != from_datetime.get(Calendar.WEEK_OF_MONTH))
	    return false;
       }
      else {
	 if (day.get(Calendar.DAY_OF_MONTH) != from_datetime.get(Calendar.DAY_OF_MONTH))
	    return false;
       }
    }
   
   if (exclude_dates != null) {
      if (exclude_dates.contains(day)) return false;
    }
   
   return true;
}



/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

private Calendar setDateAndTime(Calendar date,Calendar time)
{
   Calendar c1 = (Calendar) date.clone();
   c1.set(Calendar.HOUR_OF_DAY,time.get(Calendar.HOUR_OF_DAY));
   c1.set(Calendar.MINUTE,time.get(Calendar.MINUTE));
   c1.set(Calendar.SECOND,time.get(Calendar.SECOND));
   c1.set(Calendar.MILLISECOND,time.get(Calendar.MILLISECOND));
   return c1;
}

private boolean sameDay(Calendar c0,Calendar c1)
{
   return c0.get(Calendar.YEAR) == c1.get(Calendar.YEAR) &&
      c0.get(Calendar.DAY_OF_YEAR) == c1.get(Calendar.DAY_OF_YEAR);
}


private boolean isNextDay(Calendar c0,Calendar c1)
{
   Calendar c0a = CatreTimeSlotEvent.startOfNextDay(c0);
   Calendar c1a = CatreTimeSlotEvent.startOfDay(c1);
   return c0a.equals(c1a);
}




/********************************************************************************/
/*										*/
/*	Check for conflicts							*/
/*										*/
/********************************************************************************/

@Override public boolean canOverlap(CatreTimeSlotEvent evtc)
{
   CatmodelCalendarEvent evt = (CatmodelCalendarEvent) evtc;
   
   if (evt.to_datetime != null &&
	 evt.to_datetime.after(from_datetime)) return false;
   if (evt.from_datetime != null &&
	 evt.from_datetime.before(to_datetime)) return false;
   if (evt.to_datetime.after(from_datetime)) return false;
   if (evt.from_datetime.before(to_datetime)) return false;
   
   if (evt.day_set != null && day_set != null) {
      if (!day_set.intersects(evt.day_set)) return false;
    }
   
   // Need to handle repeat interval, excluded dates
   
   return true;
}



/********************************************************************************/
/*										*/
/*	Output Methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("FROMDATETIME",from_datetime.getTimeInMillis());
   rslt.put("TODATETIME",to_datetime.getTimeInMillis());
   rslt.put("DAYS",getDays());
   rslt.put("INTERVAL",repeat_interval);
   rslt.put("ALLDAY",all_day);
   if (exclude_dates != null) {
      List<Number> exc = new ArrayList<>();
      for (Calendar c : exclude_dates) {
         exc.add(c.getTimeInMillis());
       }
      rslt.put("EXCLUDE",exc);
    }

   return rslt;
}

@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   long now = System.currentTimeMillis();
   long fdv = getSavedLong(map,"FROMDATETIME",now);
   from_datetime = Calendar.getInstance();
   from_datetime.setTimeInMillis(fdv);
   now += 1000*60*60;   
   long tdv = getSavedLong(map,"TODATETIME",now);
   to_datetime = Calendar.getInstance();
   to_datetime.setTimeInMillis(tdv);
   all_day = getSavedBool(map,"ALLDAY",false);
   
   day_set = null;
   repeat_interval = getSavedInt(map,"INTERVAL",0);
   exclude_dates = null;
   String days = getSavedString(map,"DAYS",null);
   setDays(days);
   List<?> exc = (List<?>) getSavedValue(map,"EXCLUDE",null);
   if (exc != null) {
      for (Object o : exc) {
         Number n = (Number) o;
         Calendar cal = Calendar.getInstance();
         cal.setTimeInMillis(n.longValue());
         addExcludedDate(cal);
       }
    }
   
   normalizeTimes();
}



@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append(calDate(from_datetime));
   if (!all_day) {
      buf.append(" ");
      buf.append(calTime(from_datetime));
    }
   buf.append(":");
   buf.append(calDate(to_datetime));
   if (!all_day) {
      buf.append(" ");
      buf.append(calTime(to_datetime));
    }
   else buf.append(" All Day");
   if (getDays() != null) {
      buf.append(" ");
      buf.append(getDays());
    }
   if (repeat_interval != 0) {
      buf.append(" R");
      buf.append(repeat_interval);
    }
   if (exclude_dates != null) {
      for (Calendar c : exclude_dates) {
	 Date d = new Date(c.getTimeInMillis());
	 buf.append("-");
	 buf.append(DateFormat.getDateInstance().format(d));
       }
    }
   return buf.toString();
}



}       // end of class CatmodelCalendarEvent
