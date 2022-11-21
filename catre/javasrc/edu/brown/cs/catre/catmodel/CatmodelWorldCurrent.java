/********************************************************************************/
/*                                                                              */
/*              CatmodelWorldCurrent.java                                       */
/*                                                                              */
/*      Current world                                                           */
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



package edu.brown.cs.catre.catmodel;

import java.util.Calendar;

import edu.brown.cs.catre.catre.CatreWorld;

class CatmodelWorldCurrent extends CatmodelWorld implements CatreWorld
{


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatmodelWorldCurrent(CatmodelUniverse uu)
{
   super(uu);
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public boolean isCurrent()		{ return true; }





@Override public void setTime(Calendar time)
{
   throw new IllegalArgumentException("Attempt to set time of current world");
}


@Override public long getTime()
{
   return System.currentTimeMillis();
}

@Override public String getUID()
{
   return "WORLD_CURRENT";
}





@Override public Calendar getCurrentTime()
{
   return Calendar.getInstance();
}

}       // end of class CatmodelWorldCurrent




/* end of CatmodelWorldCurrent.java */

