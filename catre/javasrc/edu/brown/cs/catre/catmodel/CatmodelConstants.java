/********************************************************************************/
/*                                                                              */
/*              CatmodelConstants.java                                          */
/*                                                                              */
/*      Constants for universe/world models                                     */
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

public interface CatmodelConstants
{

String UNIVERSE_PREFIX = "UNIV_";




/********************************************************************************/
/*										*/
/*	Time Constants								*/
/*										*/
/********************************************************************************/

long T_SECOND = 1000;
long T_MINUTE = 60 * T_SECOND;
long T_HOUR = 60 * T_MINUTE;
long T_DAY = 24 * T_HOUR;




/********************************************************************************/
/*										*/
/*	Decoding methods							*/
/*										*/
/********************************************************************************/

class Coder {
   
   public static String unescape(String s) {
      if (s == null) return null;
      try {
         return URLDecoder.decode(s,"UTF-8");
       }
      catch (UnsupportedEncodingException e) {
         return s;
       }
    }
   
   public static String escape(String s) {
      if (s == null) return null;
      try {
         return URLEncoder.encode(s,"UTF-8");
       }
      catch (UnsupportedEncodingException e) {
         return s;
       }
    }
   
}	// end of inner class Coder





/********************************************************************************/
/*										*/
/*	Calendar Event								*/
/*										*/
/********************************************************************************/

interface CalendarEvent {
   
   long getStartTime();
   long getEndTime();
   Map<String,String> getProperties();
   
}       // end of inner interface CalendarEvent


}       // end of interface CatmodelConstants




/* end of CatmodelConstants.java */

