/********************************************************************************/
/*                                                                              */
/*              CatreConditionHandler.java                                      */
/*                                                                              */
/*      description of class                                                    */
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

import java.util.EventListener;

public interface CatreConditionListener extends EventListener
{



/**
 *      Invoked when a condition turns on.  The parameter set passed in
 *      may be null; if not it contains values describing the condition.
 **/

default void conditionOn(CatreCondition cc,CatrePropertySet p)              { }

/**
 *      Invoked when a condition triggers.
 **/

default void conditionTrigger(CatreCondition cc,CatrePropertySet p)         { }


/**
 *      Invoked when a condition turns off
 **/

default void conditionOff(CatreCondition cc)                                { }


/**
 *      Handle errors in checking the condition
 **/

default void conditionError(CatreCondition cc,Throwable cause)              { }


default void conditionValidated(CatreCondition cc,boolean valid)            { }



}       // end of interface CatreConditionHandler




/* end of CatreConditionHandler.java */

