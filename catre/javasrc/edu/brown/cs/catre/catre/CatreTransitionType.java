/********************************************************************************/
/*                                                                              */
/*              CatreTransitionType.java                                        */
/*                                                                              */
/*      Enum for the different types of transitions                             */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.catre.catre;



public enum CatreTransitionType
{

   STATE_CHANGE,                // changes state until another event
   TEMPORARY_CHANGE,            // changes state, device will reset by itselft
   TRIGGER,                     // triggers something, no state change

}       // end of enum CatreTransitionType




/* end of CatreTransitionType.java */

