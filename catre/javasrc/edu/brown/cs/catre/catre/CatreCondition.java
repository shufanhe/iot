/********************************************************************************/
/*                                                                              */
/*              CatreCondition.java                                             */
/*                                                                              */
/*      Condition definitions for user-programming of devices                   */
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

import java.util.Collection;

/**
 *	A condition describes when a particular action should be applied.
 *
 *	This describes a standard condition.  There are several special types
 *	of conditions that are supported by the system.  One is time-based
 *	where the condition reflects either a single time (for a trigger) or
 *	a time period.	A second is logical.  This represents an AND (or OR?)
 *	of other conditions.  A third is positional.  This represents the
 *	location of an object or user (and hence can assume properties based
 *	on location).  A fourth is state-based.  This reflects real-world condition
 *	being in a particular state (e.g. the telephone is in use).  A fifth is
 *	time+<condition> based, i.e. this condition has been true (or false) for
 *	a specified amount of time.
 *
 **/

public interface CatreCondition extends CatreDescribable, CatreIdentifiable
{



/**
 *	poll to check if the condition holds in a given state.	This routine
 *	should return null if the condition does not hold.  If the condition
 *	does hold, it should return a ParameterSet.  The parameter set may
 *	contain values associated with the condition that can later be used
 *	inside the action.
 **/

CatrePropertySet getCurrentStatus(CatreWorld world) throws CatreConditionException;



/**
 *	Register a callback to detect when condition changes
 **/

void addConditionHandler(CatreConditionHandler hdlr);



/**
 *	Remove a registered callback.
 **/

void removeConditionHandler(CatreConditionHandler hdlr);



/**
 *	Return a parameter set describing what parameters if any are needed to
 *	describe this condition.  These will be used to construct a
 *	parameter set (based on user input) that will be passed to the
 *	proper Condition constructor.  The values associated with each
 *	parameter are the defaults for that parameter
 **/

CatreParameterSet getDefaultParameters();



/**
 *	Return the parameters defining this condition if any
 **/

CatreParameterSet getParameters();


/**
 *	Detect if this condition can be true when the given condition is true
 **/

boolean canOverlap(CatreCondition uc);


/**
 *	Get the set of sensor devices used by this condition
 **/

void getSensors(Collection<CatreDevice> rslt);


/**
 *	Check if the condition is a trigger or	not
 **/

boolean isTrigger();

boolean isBaseCondition();

CatreUniverse getUniverse();
void setTime(CatreWorld w);
void addImpliedProperties(CatrePropertySet ups);




}       // end of interface CatreCondition




/* end of CatreCondition.java */

