/********************************************************************************/
/*                                                                              */
/*              CatmodelConditionTriggerTime.java                               */
/*                                                                              */
/*      Triggerr condition at a particular time or set of times                 */
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

import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TimerTask;


import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.catre.catre.CatreWorld;

class CatmodelConditionTriggerTime extends CatmodelCondition 
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BitSet		minute_check;
private BitSet		hour_check;
private BitSet		day_check;
private BitSet		month_check;
private BitSet		weekday_check;

private String		condition_name;
private TimerTask	cur_timer;

private static HashMap<String,Integer> value_map;
private static long	MAX_TIME =  T_DAY;


static {
   value_map = new HashMap<String,Integer>();
   value_map.put("SUN",0);
   value_map.put("MON",1);
   value_map.put("TUE",2);
   value_map.put("WED",3);
   value_map.put("THU",4);
   value_map.put("FRI",5);
   value_map.put("SAT",6);
   value_map.put("JAN",1);
   value_map.put("FEB",2);
   value_map.put("MAR",3);
   value_map.put("APR",4);
   value_map.put("MAY",5);
   value_map.put("JUN",6);
   value_map.put("JUL",7);
   value_map.put("AUG",8);
   value_map.put("SEP",9);
   value_map.put("OCT",10);
   value_map.put("NOV",11);
   value_map.put("DEC",12);
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatmodelConditionTriggerTime(CatmodelUniverse uu,String name,String desc)
{
   super(uu);
   
   condition_name = name;
   cur_timer = null;
   
   StringTokenizer tok = new StringTokenizer(desc);
   minute_check = decodeSet(tok.nextToken(),0,59);
   hour_check = decodeSet(tok.nextToken(),0,23);
   day_check = decodeSet(tok.nextToken(),1,31);
   month_check = decodeSet(tok.nextToken(),1,12);
   weekday_check = decodeSet(tok.nextToken(),0,7);
   if (weekday_check.get(7)) {
      weekday_check.set(0);
      weekday_check.clear(7);
    }
   
   setupTimer();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public boolean isConsistentWith(CatreCondition t)
{
   return true;
}


@Override public String getDescription()
{
   if (condition_name == null) return toString();
   // get more accurate descirption
   return condition_name + " @ " + toString();
}


@Override public String getName()
{
   if (condition_name == null || condition_name.equals("") ||
	 condition_name.equals("null")) {
      condition_name = CatreUtil.randomString(24);
    }
   
   return condition_name;
}


@Override public String getLabel()
{
   String s = super.getLabel();
   if (s == null) s = condition_name;
   if (s == null) s = toString();
   return s;
}

@Override public void getSensors(Collection<CatreDevice> rslt)	{ }




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

@Override public void setTime(CatreWorld w)
{
   long when = w.getTime();
   
   if (!w.isCurrent()) {
      long dnext = computeNext(when);
      if (dnext == when) fireTrigger(w,null);
      return;
    }
   
   if (cur_timer != null) {
      cur_timer.cancel();
      cur_timer = null;
    }
   
   long d0 = when + T_MINUTE;
   long next = computeNext(d0);
   long delta = -1;
   if (next > when) delta = next - when;
   if (delta < 0 || delta > MAX_TIME) {
      cur_timer = new RecheckTimer();
      delta = MAX_TIME;
    }
   else {
      cur_timer = new TriggerTimer();
    }
   getCatre().schedule(cur_timer,delta);
}




@Override public void addImpliedProperties(CatrePropertySet ups)
{
}



private void setupTimer()
{
   setTime(getUniverse().getCurrentWorld());
}



@Override public boolean isTrigger()		{ return true; }


@Override public boolean isBaseCondition()      { return true; }



/********************************************************************************/
/*										*/
/*	Time Computation methods						*/
/*										*/
/********************************************************************************/

private long computeNext(long start)
{
   Calendar c = Calendar.getInstance();
   c.setTimeInMillis(start);
   
   for ( ; ; ) {
      if (checkMonth(c)) continue;
      if (checkDay(c)) continue;
      if (checkHour(c)) continue;
      if (!checkMinute(c)) break;
    }
   
   return c.getTimeInMillis();
}



private boolean checkMonth(Calendar c)
{
   int mon = c.get(Calendar.MONTH) + 1;
   if (month_check.get(mon)) return false;
   c.add(Calendar.MONTH,1);
   c.set(Calendar.DAY_OF_MONTH,1);
   c.set(Calendar.HOUR,0);
   c.set(Calendar.MINUTE,0);
   c.set(Calendar.SECOND,0);
   c.set(Calendar.MILLISECOND,0);
   
   return true;
}


private boolean checkDay(Calendar c)
{
   int dow = c.get(Calendar.DAY_OF_WEEK) - 1;
   int dom = c.get(Calendar.DAY_OF_MONTH);
   int ctr = 0;
   while (!weekday_check.get(dow) || !day_check.get(dom)) {
      c.add(Calendar.DAY_OF_YEAR,1);
      c.set(Calendar.HOUR_OF_DAY,0);
      c.set(Calendar.MINUTE,0);
      c.set(Calendar.SECOND,0);
      c.set(Calendar.MILLISECOND,0);
      dow = c.get(Calendar.DAY_OF_WEEK) - 1;
      dom = c.get(Calendar.DAY_OF_MONTH);
      if (++ctr > 366) break;
    }
   
   return ctr == 0;
}


private boolean checkHour(Calendar c)
{
   int ctr = 0;
   int hod = c.get(Calendar.HOUR_OF_DAY);
   while (!hour_check.get(hod)) {
      c.add(Calendar.HOUR_OF_DAY,1);
      c.set(Calendar.MINUTE,0);
      c.set(Calendar.SECOND,0);
      c.set(Calendar.MILLISECOND,0);
      hod = c.get(Calendar.HOUR_OF_DAY);
      if (++ctr > 24) return true;
    }
   
   return ctr > 0;
}



private boolean checkMinute(Calendar c)
{
   int ctr = 0;
   int min = c.get(Calendar.MINUTE);
   while (!hour_check.get(min)) {
      c.add(Calendar.MINUTE,1);
      c.set(Calendar.SECOND,0);
      c.set(Calendar.MILLISECOND,0);
      min = c.get(Calendar.MINUTE);
      if (++ctr > 60) return true;
    }
   
   return ctr > 0;
}




/********************************************************************************/
/*										*/
/*	Set encode and decode methods						*/
/*										*/
/********************************************************************************/

private BitSet decodeSet(String what,int min,int max)
{
   BitSet rslt = new BitSet();
   
   if (what == null || what.equals("*")) {
      rslt.set(min,max+1);
    }
   else {
      StringTokenizer tok = new StringTokenizer(what,",-/",true);
      int last = min;
      int from = -1;
      int to = -1;
      String next = null;
      while (tok.hasMoreTokens()) {
	 String t = next;
	 if (t == null) t = tok.nextToken();
	 else next = null;
         
	 if (t.equals(",")) {
	    from = -1;
	    continue;
	  }
	 else if (t.equals("-")) {
	    from = last;
	    continue;
	  }
	 else if (Character.isDigit(t.charAt(0))) {
	    try {
	       last = Integer.parseInt(t);
	     }
	    catch (NumberFormatException e) {
	       continue;
	     }
	    if (from < 0) rslt.set(last);
	    else if (to > 0) {
	       int d = last;
	       last = from;
	       for (int i = from; i <= to; i += d) {
		  rslt.set(i);
		  last = i;
		}
	       from = to = -1;
	     }
	    else {
	       if (tok.hasMoreTokens()) next = tok.nextToken();
	       if (next != null && next.equals("/")) {
		  to = last;
		  continue;
		}
	       rslt.set(from,last+1);
	     }
	    from = -1;
	  }
	 else {
	    if (t.length() > 3) t = t.substring(0,3);
	    t = t.toUpperCase();
	    Integer v = value_map.get(t);
	    if (v != null) rslt.set(v);
	  }
       }
    }
   
   return rslt;
}



private String encodeSet(BitSet s)
{
   StringBuffer buf = new StringBuffer();
   if (s.cardinality() == 0) return "*";
   
   int last = -1;
   int from = -1;
   for ( ; ; ) {
      int next = s.nextSetBit(last+1);
      if (next == last+1 && last >= 0) {
	 if (from < 0) from = last;
       }
      else {
	 if (from >= 0) {
	    buf.append("-");
	    buf.append(last);
	    from = -1;
	  }
	 if (next >= 0) {
	    if (buf.length() > 0) buf.append(",");
	    buf.append(next);
	  }
       }
      if (next < 0) break;
      last = next;
    }
   
   return buf.toString();
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append(encodeSet(minute_check));
   buf.append(" ");
   buf.append(encodeSet(hour_check));
   buf.append(" ");
   buf.append(encodeSet(day_check));
   buf.append(" ");
   buf.append(encodeSet(month_check));
   buf.append(" ");
   buf.append(encodeSet(weekday_check));
   
   return buf.toString();
}



/********************************************************************************/
/*										*/
/*	Timer Tasks								*/
/*										*/
/********************************************************************************/

private class RecheckTimer extends TimerTask {
   
   @Override public void run() {
      setupTimer();
    }
   
}	// end of inner class RecheckTimer



private class TriggerTimer extends TimerTask {
   
   @Override public void run() {
      fireTrigger(getUniverse().getCurrentWorld(),null);
      setupTimer();
    }
   
}	// end of inner class TriggerTimer




}       // end of class CatmodelConditionTriggerTime




/* end of CatmodelConditionTriggerTime.java */

