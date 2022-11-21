/********************************************************************************/
/*                                                                              */
/*              CattestUser.java                                                */
/*                                                                              */
/*      Tests for user accounting                                               */
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



package edu.brown.cs.catre.cattest;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import edu.brown.cs.catre.catre.CatreUtil;

public class CattestUser implements CattestConstants
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

public CattestUser()
{
   CattestUtil.startCatre();
}


/********************************************************************************/
/*                                                                              */
/*      User authentication testing                                             */
/*                                                                              */
/********************************************************************************/

@Test 
public void testRegisterLoginRemove()
{
   String user = "sprtest";
   String pwd = "testPassword";
   String v1 = CatreUtil.sha256(pwd);
   String v2 = v1 + user;
   String v3 = CatreUtil.sha256(v2);
   
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
   String v5 = CatreUtil.sha256(v4);
   JSONObject rslt3 = CattestUtil.sendJson("POST","/login",
         "CATRESESSION",sid,"SALT",salt,
         "username",user,"password",v5);
   Assert.assertEquals("OK",rslt3.getString("STATUS")); 
   sid = rslt3.getString("CATRESESSION");
   
   JSONObject rslt4 = CattestUtil.sendJson("POST","/removeuser",
         "CATRESESSION",sid);
   Assert.assertEquals("OK",rslt4.getString("STATUS")); 
}








}       // end of class CattestUser




/* end of CattestUser.java */

