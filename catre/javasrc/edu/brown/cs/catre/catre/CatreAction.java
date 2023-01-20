/********************************************************************************/
/*                                                                              */
/*              CatreAction.java                                                */
/*                                                                              */
/*      Action definitions for user-programming of devices                      */
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
 *	This interface defines an action that can be triggered by a set of user
 *	specified conditions.
 *
 *	Actions typically releate to setting a particular state of a particular
 *	entity.  The Action interface needs to define what is meant by an entity,
 *	a state, a state change, etc.  It also has to define how we determine when
 *	two actions may be in conflict with one another.
 *
 *	There are a set of standard action implementations.  One is a combination
 *	which implies a set of actions that should be taken simultaneously.
 *
 **/

public interface CatreAction extends CatreDescribable, CatreSavable
{


/**
 *	Return the entity associated with this action.	Each action refers to
 *	setting a particular state of a particular entity.
 **/

CatreDevice getDevice();
CatreTransition getTransition();




/**
 *	Indicate if this is a trigger action
 **/

boolean isTriggerAction();
void setIsTriggerAction(boolean fg);

boolean isValid();


default void addImpliedProperties(CatrePropertySet props)               { }

/**
 *	Get the current parameters.  The returned map is live in that it can
 *	be changed by the caller to change the parameter set
 **/

CatreParameterSet getParameters() throws CatreActionException;


/**
 *	Perform an action in a hypothetical world.  If the action fails for
 *	some reason (e.g. bad parameters, can't be done), an exception is
 *	returned.  Perform will return once the action is complete.  This
 *	routine is passed an optional set of initial parameters that are
 *	derived from the condition.  This may be null.
 **/

void perform(CatrePropertySet inputs) throws CatreActionException;




}       // end of interface CatreAction




/* end of CatreAction.java */

