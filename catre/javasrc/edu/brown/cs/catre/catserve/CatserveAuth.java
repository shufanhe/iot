/********************************************************************************/
/*										*/
/*		CatserveAuth.java						*/
/*										*/
/*	Handle login/register							*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.catre.catserve;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreException;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreSession;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUser;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.IHTTPSession;

class CatserveAuth implements CatserveConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreStore	data_store;
private CatreController catre_control;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatserveAuth(CatreController cc)
{
   catre_control = cc;
   data_store = cc.getDatabase();
}



/********************************************************************************/
/*										*/
/*	Handle register 							*/
/*										*/
/********************************************************************************/

Response handleRegister(IHTTPSession s,CatreSession cs)
{
   if (cs.getUser(catre_control) != null) {
      return CatserveServer.jsonError(cs,"Can't register while logged in");
    } 

   String userid = CatserveServer.getParameter(s,"username");
   String email = CatserveServer.getParameter(s,"email");
   String pwd = CatserveServer.getParameter(s,"password");
   String unm = CatserveServer.getParameter(s,"universe");

   try {
      CatreUser cu = data_store.createUser(userid,email,pwd);
      catre_control.createUniverse(unm,cu);
      cs.setupSession(cu);
      cs.saveSession(catre_control);
      return CatserveServer.jsonResponse(cs);
    }
   catch (CatreException e) {
       String msg = e.getMessage();
       return CatserveServer.jsonError(cs,msg);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Handle Login                                                            */
/*                                                                              */
/********************************************************************************/

Response handleLogin(IHTTPSession s,CatreSession cs)
{
   String username = CatserveServer.getParameter(s,"username");
   String pwd = CatserveServer.getParameter(s,"password");
   String salt = CatserveServer.getParameter(s,"SALT");
   String salt1 = cs.getValue("SALT");
   CatreLog.logD("CATSERVE","LOGIN " + username + " " + pwd + " " + salt);
   
   if (username == null || pwd == null) {
      return CatserveServer.jsonError(cs,"Missing username or password");
    }
   if (salt == null || salt1 == null || !salt.equals(salt1)) {
      return CatserveServer.jsonError(cs,"Bad setup");
    }
   CatreUser cu = catre_control.getDatabase().findUser(username,pwd,salt);
   if (cu == null) {
      return CatserveServer.jsonError(cs,"Bad user name or password");
    }
   cs.setupSession(cu);
   cs.saveSession(catre_control);
   
   return CatserveServer.jsonResponse(cs);
}


}	// end of class CatserveAuth




/* end of CatserveAuth.java */

