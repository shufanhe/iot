/********************************************************************************/
/*                                                                              */
/*              CatreParameterSet.java                                          */
/*                                                                              */
/*      A mapping of parameters to values                                       */
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

import java.util.Collection;

public interface CatreParameterSet extends CatreSubSavable
{


/**
 *      Set a parameter by name
 **/

void setParameter(String nm,Object val);


/**
 *      Get the set of valid parameters
 **/

Collection<CatreParameter> getValidParameters();


/**
 *      Add a parameter to the set
 **/

void addParameter(CatreParameter p);


/**
 *      Set value of a single parameter
 **/

Object putValue(CatreParameter param,Object value);
Object putValue(String nm,Object value);
void putValues(CatreParameterSet ps);
void clearValues();

/**
 *      Get parameter value
 **/

Object getValue(CatreParameter parameter);
String getStringValue(CatreParameter parameter);


}       // end of interface CatreParameterSet




/* end of CatreParameterSet.java */

