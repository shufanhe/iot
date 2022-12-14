/********************************************************************************/
/*                                                                              */
/*              CatprogCalendarChecker.java                                     */
/*                                                                              */
/*      Code to periodically check user calendars                               */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2022 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2022, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.catre.catprog;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.WeakHashMap;

import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreWorld;


class CatprogCalendarChecker implements CatprogConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private WeakHashMap<CatprogCondition,Boolean>	check_conditions;
private Set<CatreWorld>				active_worlds;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatprogCalendarChecker()
{
   check_conditions = new WeakHashMap<CatprogCondition,Boolean>();
   active_worlds = new HashSet<CatreWorld>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

synchronized void addCondition(CatprogCondition bc)
{
   CatreLog.logT("CATMODEL","Add Calendar condition " + bc);
   
   check_conditions.put(bc,false);
   setTime();
}


/********************************************************************************/
/*										*/
/*	Set up for next check							*/
/*										*/
/********************************************************************************/

private void setTime()
{
   Set<CatreWorld> worlds = new HashSet<CatreWorld>();
   List<CatprogCondition> tocheck = new ArrayList<CatprogCondition>();
   synchronized(this) {
      tocheck.addAll(check_conditions.keySet());
    }
   for (CatprogCondition bc : tocheck) {
      CatreUniverse uu = bc.getUniverse();
      worlds.add(uu.getCurrentWorld());
    }
   
   for (CatreWorld uw : worlds) {
      setTime(uw);
    }
   
}


private void setTime(CatreWorld uw)
{
   CatreLog.logT("CATMODEL","Check Calendar world " + uw);
   
   synchronized (this) {
      if (active_worlds.contains(uw)) return;
      active_worlds.add(uw);
    }
   
   long delay = T_HOUR; 		// check at least each hour to allow new events
   long now = System.currentTimeMillis();	
   
   CatprogGoogleCalendar gc = CatprogGoogleCalendar.getCalendar(uw);
   if (gc == null) {
      CatreLog.logI("CATMODEL","No Calendar found for world " + uw);
      removeActive(uw);
      return;
    }
   
   Collection<CalendarEvent> evts = gc.getActiveEvents(now);
   for (CalendarEvent ce : evts) {
      long tim = ce.getStartTime();
      if (tim <= now) tim = ce.getEndTime();
      CatreLog.logI("CATMODEL","Calendar Event " + tim + " " + now + " " + ce);
      if (tim <= now) continue;
      delay = Math.min(delay,tim-now);
    }
   delay = Math.max(delay,10000);
   CatreLog.logI("CATMODEL","Schedule Calendar check for " + uw + " " + delay + " at " + (new Date(now+delay).toString()));
   
   uw.getUniverse().getCatre().schedule(new CheckTimer(uw),delay);
}



private synchronized void removeActive(CatreWorld uw)
{
   CatreLog.logI("CATMODEL","Remove Calendar world " + uw);
   active_worlds.remove(uw);
}




/********************************************************************************/
/*										*/
/*	Recheck pending events							*/
/*										*/
/********************************************************************************/

private void recheck(CatreWorld uw)
{
   List<CatprogCondition> tocheck = new ArrayList<CatprogCondition>();
   synchronized(this) {
      tocheck.addAll(check_conditions.keySet());
    }
   
   for (CatprogCondition bc : tocheck) {
      CatreUniverse uu = bc.getUniverse();
      CatreWorld cw = uu.getCurrentWorld(); 
      if (uw == cw) bc.setTime(cw);
    }
}



/********************************************************************************/
/*										*/
/*	TimerTask for checking							*/
/*										*/
/********************************************************************************/

private class CheckTimer extends TimerTask {
   
   private CatreWorld for_world;
   
   CheckTimer(CatreWorld uw) {
      for_world = uw;
    }
   
   @Override public void run() {
      CatreLog.logI("CATMODEL","Checking google Calendar for " + for_world + " at " + (new Date().toString()));
      removeActive(for_world);
      recheck(for_world);
      setTime();
    }

}	// end of inner class CheckTimer




}       // end of class CatprogCalendarChecker




/* end of CatprogCalendarChecker.java */

