/********************************************************************************/
/*                                                                              */
/*              CatreFactory.java                                               */
/*                                                                              */
/*      Continuous and Trigger-based Rule Environment Factory methods           */
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
 *	This interface serves as a factory for the common entities that need
 *	to be created by the application.
 **/

public interface CatreFactory
{



/**
 *	Return the current world.  This is the default world that reflects
 *	what is really happening at this instant.
 **/

CatreWorld getCurrentWorld(CatreUniverse univ);






/**
 *	Create an action for a particular entity and transition.  This
 *	will throw an exception if the transition does not apply to the
 *	entity.
 **/

CatreAction createNewAction(CatreDevice ent,CatreTransition t)
        throws CatreActionException;



/**
 *	Create a new rule specifying the given action when the given
 *	condition holds.  The new rule has the specified priority.
 **/

CatreRule createNewRule(CatreCondition cond,
      List<CatreAction> act,double priority);



/**
 *	Create a logical condition that is the AND of the given set of
 *	conditions.
 ***/

CatreCondition createAndCondition(CatreCondition ... act);



/**
 *      Create a logical condition thta tis the OR of the given set or 
 *      conditions
 **/

CatreCondition createOrCondition(CatreCondition ... act);









/**
 *	Create a time-based condition.	The parameters should allow creation
 *	or arbitrary calendar-type events (i.e. one shot or repeated, day-based,
 *	trigger or time slot, etc.)
 **/

CatreCondition createTimeCondition(CatreUniverse uu,String nm,Calendar from,Calendar to)
        throws CatreConditionException;



/**
 *	Create a condition reflecting a particular condition being on for a
 *	given amount of time.
 **/

CatreCondition createTimedCondition(CatreCondition cond,long starttime,long endtime)
        throws CatreConditionException;




/**
 *	Create a sensor reflecting another sensor being in a given state for a
 *	given amount of time
 **/

CatreDevice createTimedSensor(String id,CatreDevice base,CatreParameter p,Object state,long start,long end);


CatreDevice createTimedSensor(String id,CatreCondition cond,
      long start,long end);


/**
 *	Create a sensor which acts as a latch for a particular device setting
 **/

CatreDevice createLatchSensor(String id,CatreDevice base,
      CatreParameter p,Object state,Calendar reset);


CatreDevice createLatchSensor(String id,CatreDevice base,
      CatreParameter p,Object state,long reset,long offafter);

CatreDevice createLatchSensor(String id,CatreCondition cond,long reset,long offafter);

CatreDevice createLatchSensor(String id,CatreCondition cond,Calendar reset);



/**
 *      Create an empty sequential (automata) sensor
 **/

CatreDevice createAutomataSensor(CatreUniverse uu,String id);

/**
 *      Create an single condition OR sensor
 **/

CatreDevice createOrSensor(String id,CatreDevice base,CatreParameter p,Object s);




}       // end of interface CatreFactory




/* end of CatreFactory.java */

