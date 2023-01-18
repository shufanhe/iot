/********************************************************************************/
/*                                                                              */
/*              CatreProgram.java                                               */
/*                                                                              */
/*      Representation of a runnable program                                    */
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

import java.util.List;
import java.util.Map;

/**
 *	The user interface might want to create hypothetical situations to
 *	determine if there are conflicts or to show the user what would happen
 *	under different conditions.  This interface represents such a state.
 **/

public interface CatreProgram extends CatreSubSavable
{




/**
 *	Return the set of rules in this program in priority order.
 **/

List<CatreRule> getRules();


/**
 *      Return a particular rule by id or name
 **/

CatreRule findRule(String id);

/**
 *      Add a new rule
 **/
void addRule(CatreRule ur);

/**
 *      Remove a rule
 **/
void removeRule(CatreRule ur);






/**
 *	Run the current program once on the current world.  This function
 *	returns true if a rule is triggered.
 **/

boolean runOnce(CatreTriggerContext ctx);


CatreUniverse getUniverse();



CatreRule createRule(CatreStore cs,Map<String,Object> map);

CatreCondition createCondition(CatreStore cs,Map<String,Object> map);


}       // end of interface CatreProgram




/* end of CatreProgram.java */

