/********************************************************************************/
/*										*/
/*		CatreUtil.java							*/
/*										*/
/*	Utility functions for Catre						*/
/*										*/
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

import java.awt.Desktop;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Random;

public class CatreUtil
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/


private static Random rand_gen = new Random();
private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";



/********************************************************************************/
/*										*/
/*	Generate random strings 						*/
/*										*/
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
/*										*/
/*	Encoding methods							*/
/*										*/
/********************************************************************************/

static public String secureHash(String s)
{
   try {
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      byte [] dvl = md.digest(s.getBytes());
      String rslt = Base64.getEncoder().encodeToString(dvl);
      return rslt;
    }
   catch (Exception e) {
      throw new Error("Problem with sha-512 encoding of " + s);
    }
}


static public String shortHash(String s)
{
   try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte [] dvl = md.digest(s.getBytes());
      String rslt = Base64.getEncoder().encodeToString(dvl);
      if (rslt.length() > 16) rslt = rslt.substring(0,16);
      return rslt;
    }
   catch (Exception e) {
      throw new Error("Problem with sha-512 encoding of " + s);
    }
}



static public String unescape(String s) {
   if (s == null) return null;
   try {
      return URLDecoder.decode(s,"UTF-8");
    }
   catch (Throwable e) {
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



/********************************************************************************/
/*                                                                              */
/*      Email methods                                                           */
/*                                                                              */
/********************************************************************************/

static public boolean sendEmail(String sendto,String subj,String body)
{
   if (sendto == null || subj == null && body == null) return false;
   
   try {
      if (subj != null) subj = URLEncoder.encode(subj,"UTF-8");
    }
   catch (UnsupportedEncodingException e) { }
   try {
      if (body != null) body = URLEncoder.encode(body,"UTF-8");
    }
   catch (UnsupportedEncodingException e) { }
   
   String full = "mailto:" + sendto;
   String pfx = "?";
   try {
      if (subj != null) {
         full += pfx + "subject=" + subj;
         pfx = "&";
       }
      if (body != null) {
         full +=  pfx + "body=" + body;
         pfx = "&";
       }
      URI u = new URI(full);
      Desktop.getDesktop().mail(u);  
    }
   catch (Throwable e) {
      return false;
    }
   
   return true;
}

}	// end of class CatreUtil




/* end of CatreUtil.java */

