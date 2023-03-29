/********************************************************************************/
/*                                                                              */
/*              CatreRule.java                                                  */
/*                                                                              */
/*      Definition of a programming rule                                        */
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

import java.util.List;
import java.util.Set;

/**
 *	A rule consists of a condition implying an action.   Rules have priorities
 *	which serve to disambiguate conflicting rules.
 **/

public interface CatreRule extends CatreDescribable, CatreSubSavable, CatreIdentifiable
{


/**
 *	Return the condition associated with a rule
 **/

CatreCondition getCondition();



/**
 *	Return the action associated with a rule
 **/

List<CatreAction> getActions();

/**
 *      Return the set of devices associated with this rule.  This is used to
 *      see if the rule conflicts with an already chosen rule or if a rule 
 *      should be aborted
 **/

Set<CatreDevice> getTargetDevices();









/**
 *	Return the priority associated with the rule.  Priorities range from 0
 *	to 1000 with 0 being the lowest and 1000 the highest.  Typically priorities
 *	lower than 200 or higher than 800 will not be used (except for true
 *	background or emergency events).  Higher priority events will override
 *	lower priority ones that have overlapping actions.  Priorities less than
 *	10 should be used for manual override rules.
 **/

double getPriority();



/**
 *      Get rule creation time
 **/

long getCreationTime();


/**
 *	Set the priority associated with this rule.
 **/

void setPriority(double p);



/**
 *      Indicate if a rule is explicitly defined
 **/

boolean isExplicit();

/**
 *      Check if this is a trigger rule
 **/
boolean isTrigger();

/**
 *	Apply a rule without waiting for the action to complete.  This
 *      returns false if the rule is not applicable (the condition doesn't
 *      hold).  It can throw condition exception or action exception if
 *      there are errors.  The rule is applied asynchronously if the rule
 *      is asynchronous and the world is current.
 **/

boolean apply(CatreTriggerContext ctx) 
        throws CatreConditionException, CatreActionException;

/**
 *      Abort the rule if it is currently active
 **/

void abort();


}       // end of interface CatreRule




/* end of CatreRule.java */

