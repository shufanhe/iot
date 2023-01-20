/********************************************************************************/
/*										*/
/*		CattestUser.java						*/
/*										*/
/*	Tests for user accounting						*/
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




package edu.brown.cs.catre.cattest;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import edu.brown.cs.catre.catre.CatreUtil;

public class CattestUser implements CattestConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CattestUser()
{
   CattestUtil.startCatre();
}


/********************************************************************************/
/*										*/
/*	User authentication testing						*/
/*										*/
/********************************************************************************/

@Test
public void testRegisterLoginRemove()
{
   String user = "sprtest";
   String pwd = "testPassword";
   String v1 = CatreUtil.secureHash(pwd);
   String v2 = v1 + user;
   String v3 = CatreUtil.secureHash(v2);

   JSONObject rslt = CattestUtil.sendJson("POST","/register",
	 "username",user,
	 "email","spr@cs.brown.edu",
	 "password",v3,
	 "universe","MyWorld");
   Assert.assertEquals("OK",rslt.getString("STATUS"));
   String sid = rslt.getString("CATRESESSION");

   JSONObject rslt1 = CattestUtil.sendGet("/logout","CATRESESSION",sid);
   Assert.assertEquals("OK",rslt1.getString("STATUS"));

   JSONObject rslt2 = CattestUtil.sendGet("/login");
   Assert.assertEquals("OK",rslt2.getString("STATUS"));
   sid = rslt2.getString("CATRESESSION");
   String salt = rslt2.getString("SALT");

   String v4 = v3 + salt;
   String v5 = CatreUtil.secureHash(v4);
   JSONObject rslt3 = CattestUtil.sendJson("POST","/login",
	 "CATRESESSION",sid,"SALT",salt,
	 "username",user,"password",v5);
   Assert.assertEquals("OK",rslt3.getString("STATUS"));
   sid = rslt3.getString("CATRESESSION");

   JSONObject rslt4 = CattestUtil.sendJson("POST","/removeuser",
	 "CATRESESSION",sid);
   Assert.assertEquals("OK",rslt4.getString("STATUS"));
}








}	// end of class CattestUser




/* end of CattestUser.java */

