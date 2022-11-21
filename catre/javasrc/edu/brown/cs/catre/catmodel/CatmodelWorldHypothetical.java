/********************************************************************************/
/*                                                                              */
/*              CatmodelWorldHypothetical.java                                  */
/*                                                                              */
/*      Hypothetical world                                                      */
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

import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreException;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.catre.catre.CatreWorld;

class CatmodelWorldHypothetical extends CatmodelWorld implements CatreWorld, CatmodelConstants
{



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Calendar                current_time;
private String                  unique_id;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatmodelWorldHypothetical(CatreWorld w)
{
   super(w.getUniverse());
   current_time = Calendar.getInstance();
   current_time.setTimeInMillis(w.getTime());
   unique_id = CatreUtil.randomString(24);
   
   for (CatreDevice ud : getUniverse().getDevices()) {
      if (ud.isEnabled()) {
         for (CatreParameter up : ud.getParameters()) {
            if (up.isSensor()) {
               Object val = ud.getValueInWorld(up,w);
               if (val != null) {
                  try {
                     ud.setValueInWorld(up,val,this);
                   }
                  catch (CatreException e) { }
                }
             }
          }
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public boolean isCurrent()            { return false; }


@Override public void setTime(Calendar time) 
{
   if (time != null) current_time = time;
}





@Override public long getTime()
{
   if (current_time == null) return System.currentTimeMillis();
   
   return current_time.getTimeInMillis();
}


public String getUID()
{
   return unique_id;
}


@Override public Calendar getCurrentTime()
{
   if (current_time != null) return current_time;
   return Calendar.getInstance();
}






}       // end of class CatmodelWorldHypothetical




/* end of CatmodelWorldHypothetical.java */

