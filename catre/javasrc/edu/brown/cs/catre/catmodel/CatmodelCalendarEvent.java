/********************************************************************************/
/*                                                                              */
/*              CatmodelCalendarEvent.java                                      */
/*                                                                              */
/*      Implementation of a possibly recurring calendar-based event             */
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

private Calendar	from_date;
private Calendar	to_date;
private Calendar	from_time;
private Calendar	to_time;
private BitSet		day_set;
private int		repeat_interval;
private Set<Calendar>	exclude_dates;

private static DateFormat date_format = DateFormat.getDateInstance(DateFormat.SHORT);
private static DateFormat time_format = DateFormat.getTimeInstance(DateFormat.SHORT);


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Create a simple one-shot event given its specific times
 **/

public CatmodelCalendarEvent(Calendar from,Calendar to)
{
   super(null);
   
   CatreLog.logD("CATMODEL","CREATE CALENDAR EVENT: " + from + " -> " + to);
   
   from_date = CatreTimeSlotEvent.startOfDay(from);
   to_date = CatreTimeSlotEvent.startOfNextDay(to);
   from_time = from;
   to_time = to;
   day_set = null;
   repeat_interval = 0;
   exclude_dates = null;
   
   normalizeTimes();
   
   setName(toString());
   String rslt = "";
   if (from_date != null) {
      rslt += calDate(from_date);
      if (to_date != null) rslt += " - " + calDate(to_date);
      else rslt += " ON";
    }
   else if (to_date != null) {
      rslt += "UNTIL " + calDate(to_date);
    }
   if (from_time == null && to_time == null) {
      rslt += " All Day";
    }
   else if (from_time != null) {
      rslt += " from ";
      rslt += calTime(from_time);
      if (to_time != null) {
	 rslt += " - ";
	 rslt += calTime(to_time);
       }
    }
   else {
      rslt += " until " + calTime(to_time);
    }
   setDescription(rslt);
}


CatmodelCalendarEvent(CatreStore cs,Map<String,Object> map)
{ 
   super(cs,map);
}





private void normalizeTimes() 
{
   if (from_time == null && from_date != null) {
      from_time = (Calendar) from_date.clone();
    }
   if (to_time == null && to_date != null) {
      to_time = (Calendar) to_date.clone();
    }
   if (to_time == null && from_date != null) {
      to_time = CatreTimeSlotEvent.startOfNextDay(to_date);
    }
   
   if (from_date != null) from_date = CatreTimeSlotEvent.startOfDay(from_date);
   if (to_date != null) to_date = CatreTimeSlotEvent.startOfNextDay(to_date);
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


public void makeAllDay()
{
   from_time = null;
   to_time = null;
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
   if (from_date != null) {
      Date d0 = new Date(from_date.getTimeInMillis());
      ups.put("*FROMDATE",date_format.format(d0));
    }
   if (from_time != null){
      Date d0 = new Date(from_time.getTimeInMillis());
      ups.put("*FROMTIME",time_format.format(d0));
    }  
   if (to_date != null) {
      Date d0 = new Date(to_date.getTimeInMillis());
      ups.put("*TODATE",date_format.format(d0));
    }
   if (to_time != null) {
      Date d0 = new Date(to_time.getTimeInMillis());
      ups.put("*TOTIME",time_format.format(d0));
    }
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
   if (to_date != null && from.after(to_date)) return rslt;
   if (from_date != null && to.before(from_date)) return rslt;
   
   Calendar fday = CatreTimeSlotEvent.startOfDay(from);
   Calendar tday = CatreTimeSlotEvent.startOfNextDay(to);
   
   boolean usetimes = false;
   if (day_set != null && !day_set.isEmpty()) usetimes = true;
   if (repeat_interval > 0) usetimes = true;
   if (exclude_dates != null) usetimes = true;
   
   for (Calendar day = fday; day.before(tday); day.add(Calendar.DAY_OF_YEAR,1)) {
      if (!isDayRelevant(day)) continue;
      if (from_date != null && day.before(from_date)) continue;
      if (to_date != null && !day.before(to_date)) continue;
      // the day is relevant and in range at this point
      // compute the start and stop time on this day
      Calendar start = null;
      Calendar end = null;
      if (sameDay(from,day)) start = setDateAndTime(day,from);
      else start = CatreTimeSlotEvent.startOfDay(day);
      if (sameDay(to,day)) end = setDateAndTime(day,to);
      else end = CatreTimeSlotEvent.startOfNextDay(day);
      if (from_time != null) {
	 boolean usefromtime = usetimes;
	 if (from_date == null || sameDay(from_date,day)) usefromtime = true;
	 if (usefromtime) {
	    Calendar estart = setDateAndTime(day,from_time);
	    if (estart.after(start)) start = estart;
	  }
       }
      if (to_time != null) {
	 boolean usetotime = usetimes;
	 if (to_date == null || isNextDay(day,to_date)) usetotime = true;
	 if (usetotime) {
	    Calendar endt = setDateAndTime(day,to_time);
	    if (endt.before(end)) end = endt;
	  }
       }
      
      if (end.compareTo(start) <= 0) continue;
      rslt.add(start);
      rslt.add(end);
    }
   
   return rslt;
}

List<Calendar> getSlotsOLD(Calendar from,Calendar to)
{
   List<Calendar> rslt = new ArrayList<Calendar>();
   if (to_date != null && from.after(to_date)) return rslt;
   if (from_date != null && to.before(from_date)) return rslt;
   
   Calendar fday = CatreTimeSlotEvent.startOfDay(from);
   Calendar tday = CatreTimeSlotEvent.startOfNextDay(to);
   
   for (Calendar day = fday; day.before(tday); day.add(Calendar.DAY_OF_YEAR,1)) {
      fday = day;
      if (isDayRelevant(day)) break;
    }
   if (!fday.before(tday)) return rslt;
   if (from_date != null && fday.before(from_date)) fday = (Calendar) from_date.clone();
   
   for (Calendar day = CatreTimeSlotEvent.startOfDay(fday);
      day.before(tday);
      day.add(Calendar.DAY_OF_YEAR,1)) {
      if (!isDayRelevant(day)) {
	 tday = day;
	 break;
       }
    }
   if (to_date != null && tday.after(to_date)) tday = (Calendar) to_date.clone();
   
   if (from_time == null || to_time == null || !sameDay(from_time,to_time)) {
      Calendar start = fday;
      if (from_time != null) start = from_time;
      if (start.before(from)) start = from;
      rslt.add(start);
      Calendar end = tday;
      for (Calendar day = fday; day.before(tday); day.add(Calendar.DAY_OF_YEAR,1)) {
	 if (!isDayRelevant(day)) {
	    end = CatreTimeSlotEvent.startOfDay(day);
	    break;
	  }
       }
      if (to_time != null) end = to_time;
      if (end.after(to)) end = to;
      if (end.compareTo(start) <= 0) end.add(Calendar.HOUR,24);
      rslt.add(to);
    }
   else {
      for (Calendar day = fday;
	 day.before(tday);
	 day.add(Calendar.DAY_OF_YEAR,1)) {
	 if (!isDayRelevant(day)) continue;
	 // normal time slot events
	 Calendar start = setDateAndTime(day,from_time);
	 Calendar end = setDateAndTime(day,to_time);
	 if (start.after(to)) continue;
	 if (end.before(from)) continue;
	 if (start.before(from)) start = from;
	 if (end.after(to)) end = to;
	 rslt.add(start);
	 rslt.add(end);
       }
    }
   
   return rslt;
}

@Override public boolean isActive(long when)
{
   Calendar cal = Calendar.getInstance();
   cal.setTimeInMillis(when);
   if (to_date != null && cal.after(to_date)) return false;
   if (from_date != null && cal.before(from_date)) return false;
   Calendar day = CatreTimeSlotEvent.startOfDay(cal);
   if (!isDayRelevant(day)) return false;
   Calendar dstart = CatreTimeSlotEvent.startOfDay(day);
   Calendar dend = CatreTimeSlotEvent.startOfNextDay(day);
   
   boolean usetimes = false;
   if (day_set != null && !day_set.isEmpty()) usetimes = true;
   if (repeat_interval > 0) usetimes = true;
   if (exclude_dates != null) usetimes = true;	
   if (from_time != null) {
      boolean usefromtime = usetimes;
      if (from_date == null || sameDay(from_date,day)) usefromtime = true;
      if (usefromtime) {
	 dstart = setDateAndTime(day,from_time);
       }
    }
   if (to_time != null) {
      boolean usetotime = usetimes;
      if (to_date == null || isNextDay(day,to_date)) usetotime = true;
      if (usetotime) {
	 Calendar endt = setDateAndTime(day,to_time);
	 if (endt.before(dend)) dend = endt;
       }
    }
   
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
   
   if (repeat_interval > 0 && from_date != null) {
      long d0 = from_date.getTimeInMillis();
      long d1 = day.getTimeInMillis();
      long delta = (d1-d0 + 12*T_HOUR);
      delta /= T_DAY;
      if (day_set != null) delta = (delta / 7) * 7;
      if ((delta % repeat_interval) != 0) return false;
    }
   else if (repeat_interval < 0) {
      if (day_set != null && from_date != null) {
	 if (day.get(Calendar.WEEK_OF_MONTH) != from_date.get(Calendar.WEEK_OF_MONTH))
	    return false;
       }
      else if (from_date != null) {
	 if (day.get(Calendar.DAY_OF_MONTH) != from_date.get(Calendar.DAY_OF_MONTH))
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
   
   if (from_date != null && evt.to_date != null &&
	 evt.to_date.after(from_date)) return false;
   if (to_date != null && evt.from_date != null &&
	 evt.from_date.before(to_date)) return false;
   if (evt.to_time.after(from_time)) return false;
   if (evt.from_time.before(to_time)) return false;
   
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
   
   if (from_date != null) rslt.put("FROMDATE",from_date.getTimeInMillis());
   if (from_time!= null) rslt.put("FROMTIME",from_time.getTimeInMillis());
   if (to_date != null) rslt.put("TODATE",to_date.getTimeInMillis());
   if (to_time != null) rslt.put("TOTIME",to_time.getTimeInMillis());
   rslt.put("DAYS",getDays());
   rslt.put("INTERVAL",repeat_interval);
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
   
   long fdv = getSavedLong(map,"FROMDATE",0);
   if (fdv > 0) {
      from_date = Calendar.getInstance();
      from_date.setTimeInMillis(fdv);
    }
   else from_date = null;
   
   long tdv = getSavedLong(map,"TODATE",0);
   if (tdv > 0) {
      to_date = Calendar.getInstance();
      to_date.setTimeInMillis(tdv);
    }
   else to_date = null;
   
   long ftv = getSavedLong(map,"FROMTIME",0);
   if (ftv > 0) {
      from_time = Calendar.getInstance();
      from_time.setTimeInMillis(ftv);
    }
   else from_time = null;
   
   long ttv = getSavedLong(map,"TOTIME",0);
   if (ttv > 0) {
      to_time = Calendar.getInstance();
      to_time.setTimeInMillis(ttv);
    }
   else to_time = null;
   
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
   if (from_date != null) {
      buf.append(DateFormat.getDateInstance().format(from_date));
      buf.append(" ");
    }
   if (from_time != null) {
      buf.append(DateFormat.getTimeInstance().format(from_time));
    }
   buf.append(":");
   if (to_date != null) {
      buf.append(DateFormat.getDateInstance().format(to_date));
      buf.append(" ");
    }
   if (to_time != null) {
      buf.append(DateFormat.getTimeInstance().format(to_time));
    }
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
