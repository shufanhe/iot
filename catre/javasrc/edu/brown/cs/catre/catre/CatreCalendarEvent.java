/********************************************************************************/
/*                                                                              */
/*              CatreCalendarEvent.java                                         */
/*                                                                              */
/*      Information for calendar access                                         */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
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



package edu.brown.cs.catre.catre;

import java.util.Calendar;
import java.util.List;

/**
 *	A calendar event represents a series of time slots corresponding to
 *	an event specification.  It might be a one-shot event (i.e. 1/20/2013
 *	from 2:00pm to 4:00pm), or a repeated event (i.e. every Monday at noon,
 *	possibly with exceptions and with a start/stop date).  The basic
 *	idea is that however it is specified, it should be possible to deduce
 *	for a given time period, what time slots are covered by this event.
 *	This can be used both to find overlapping or conflicting events and
 *	to create triggers when events start/stop.
 **/

public interface CatreCalendarEvent extends CatreDescribable, CatreSubSavable
{


/**
 *      Test if the event is active at a given time.
 **/

boolean isActive(long when);


List<Calendar> getSlots(Calendar from,Calendar to);

boolean canOverlap(CatreCalendarEvent evt);


public static Calendar startOfDay(Calendar c)
{
   if (c == null) {
      c = Calendar.getInstance();
    }
   Calendar c1 = (Calendar) c.clone();
   c1.set(Calendar.HOUR_OF_DAY,0);
   c1.set(Calendar.MINUTE,0);
   c1.set(Calendar.SECOND,0);
   c1.set(Calendar.MILLISECOND,0);
   return c1;
}




public static Calendar startOfNextDay(Calendar c)
{
   Calendar c1 = startOfDay(c);
   c1.add(Calendar.DAY_OF_YEAR,1);
   return c1;
}


}       // end of interface CatreCalendarEvent




/* end of CatreCalendarEvent.java */

