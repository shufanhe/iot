/********************************************************************************/
/*                                                                              */
/*              CatreUtil.java                                                  */
/*                                                                              */
/*      Utility functions for Catre                                             */
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

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Random;

public class CatreUtil
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/


private static Random rand_gen = new Random();
private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";



/********************************************************************************/
/*                                                                              */
/*      Generate random strings                                                 */
/*                                                                              */
/********************************************************************************/

static public String randomString(int len)
{
   StringBuffer buf = new StringBuffer();
   int cln = RANDOM_CHARS.length();
   for (int i = 0; i < len; ++i) {
      int idx = rand_gen.nextInt(cln);
      buf.append(RANDOM_CHARS.charAt(idx));
    }
   
   return buf.toString();
}


/********************************************************************************/
/*                                                                              */
/*      Encoding methods                                                        */
/*                                                                              */
/********************************************************************************/

static public String sha256(String s)
{
   try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte [] dvl = md.digest(s.getBytes());
      String rslt = Base64.getEncoder().encodeToString(dvl);
      return rslt;
    }
   catch (Exception e) {
      throw new Error("Problem with sha-256 encoding of " + s);
    }
}


}       // end of class CatreUtil




/* end of CatreUtil.java */

