/********************************************************************************/
/*                                                                              */
/*              CatreCondition.java                                             */
/*                                                                              */
/*      Condition definitions for user-programming of devices                   */
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




package edu.brown.cs.catre.catre;


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

public interface CatreCondition extends CatreDescribable, CatreIdentifiable, CatreSubSavable
{





/**
 *	poll to check if the condition holds in a given state.	This routine
 *	should return null if the condition does not hold.  If the condition
 *	does hold, it should return a ParameterSet.  The parameter set may
 *	contain values associated with the condition that can later be used
 *	inside the action.
 **/

CatrePropertySet getCurrentStatus() throws CatreConditionException;



/**
 *	Register a callback to detect when condition changes
 **/

void addConditionHandler(CatreConditionListener hdlr);



/**
 *	Remove a registered callback.
 **/

void removeConditionHandler(CatreConditionListener hdlr);













/**
 *	Check if the condition is a trigger or	not
 **/

boolean isTrigger();

boolean isValid();

boolean isShared();
void noteIsShared();

CatreUniverse getUniverse();


/**
 *      Clone a saved condition to get a working one (or v.v.)
 **/

CatreCondition cloneCondition();

/**
 *      Ensure the condition is active
 **/

void activate();







}       // end of interface CatreCondition




/* end of CatreCondition.java */

