/********************************************************************************/
/*                                                                              */
/*              CatreParameter.java                                             */
/*                                                                              */
/*      A parameter that can be associated with an device, transition, action   */
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
import java.util.List;

public interface CatreParameter extends CatreSubSavable, CatreDescribable
{



/**
 *	Supported parameter types
 **/

enum ParameterType {
   STRING,
   BOOLEAN,
   INTEGER,
   REAL,
   TIME,
   DATE,
   DATETIME,
   ENUM,                // one of a set of values
   SET,                 // set of values from enum or set of strings
   COLOR,
};



/**
 *	Return the type of the parameter
 **/

ParameterType getParameterType();



/**
 *	Range values.  These are used for both real and integer types.
 *	They are also used for dates/datetimes using time in millis.
 *	This function returns the minimum value.
 **/

double getMinValue();



/**
 *	Range values.  These are used for both real and integer types.
 *	They are also used for dates/datetimes using time in millis.
 *	This function returns the maximum value.
 **/

double getMaxValue();

/**
 *      For a parameter that is constrained to a set of values, return the
 *      set of potential values
 **/

List<Object> getValues();


/**
 *      Normalize a value for this type of parameters.  The input can be a string
 *      or any valid type for the parameter.  The output should be the desired
 *      type that is stored for the parameter.  
 **/

Object normalize(Object o);


/**
 *      Unnormalize returns a string that can be used as a value globall
 **/

String unnormalize(Object o);


/**
 *      Indicate if a parameter is a sensor parameter.  A sensor is set by
 *      outside factors (e.g. the environment) and generally has an associated
 *      condition that can be used to define rules.
 **/

boolean isSensor();





void setIsSensor(boolean fg);



String getDefaultUnits();

Collection<String> getAllUnits();

void setLabel(String label);
void setDescription(String description);

}       // end of interface CatreParameter




/* end of CatreParameter.java */

