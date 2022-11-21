/********************************************************************************/
/*                                                                              */
/*              CatreConditionException.java                                    */
/*                                                                              */
/*      Exception for porblems executing a condition                            */
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



public class CatreConditionException extends CatreException
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public CatreConditionException(String msg)
{
   super(msg);
}


public CatreConditionException(String msg,Throwable cause)
{
   super(msg,cause);
}


}       // end of class CatreConditionException




/* end of CatreConditionException.java */

