/********************************************************************************/
/*										*/
/*		CatserveAuth.java						*/
/*										*/
/*	Handle login/register							*/
/*										*/
/********************************************************************************/
/* Copyright 2023 Brown University -- Steven P. Reiss, Molly E. McHenry         */
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




package edu.brown.cs.catre.catserve;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreException;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreSession;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.catre.catre.CatreUtil;

import com.sun.net.httpserver.HttpExchange;



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

CatserveAuth(CatreController cc, CatserveSessionManager sm)
{
   catre_control = cc;
   data_store = cc.getDatabase();
}



/********************************************************************************/
/*										*/
/*	Handle register 							*/
/*										*/
/********************************************************************************/

public String handleRegister(HttpExchange e, CatreSession cs) 
{
   CatreLog.logT("CATSERVE","Handle register entered");
   
   if (cs == null) return CatserveServer.jsonError(cs,"Bad session");
   if (cs.getUser(catre_control) != null) {
      return CatserveServer.jsonError(cs,"Can't register while logged in");
   }

   String userid = CatserveServer.getParameter(e,"username");
   String email = CatserveServer.getParameter(e,"email");
   String pwd = CatserveServer.getParameter(e,"password");
   String unm = CatserveServer.getParameter(e,"universe");

   CatreLog.logD("AUTH", "userid: " + userid + " email: " + email + " pwd: " + pwd + " unm: " + unm);

   try {
      CatreUser cu = data_store.createUser(userid,email,pwd);

      if (catre_control.createUniverse(unm,cu) == null) {
         return CatserveServer.jsonError(cs,"problem creating universe");
      }
      
      cs.setupSession(cu);
      cs.saveSession(catre_control);
      return CatserveServer.jsonResponse(cs);
   }
   catch (CatreException err) {
      String msg = err.getMessage();
      return CatserveServer.jsonError(cs,msg);
   }
}



/********************************************************************************/
/*                                                                              */
/*      Handle change password                                                  */
/*                                                                              */
/********************************************************************************/

public String handleChangePassword(HttpExchange e,CatreSession cs) 
{
   CatreLog.logD("CATSERVE","handleChangePasswrd entered");
   
   String npwd = CatserveServer.getParameter(e,"password");
   if (npwd == null || npwd.isEmpty()) {
      return CatserveServer.jsonError(cs,"Bad password");
    }
   
   CatreUser cu = cs.getUser(catre_control);
   if (cu == null) {
      return CatserveServer.jsonError(cs,"Unauthorized access");
    }
   
   cu.setNewPassword(npwd);
   
   return CatserveServer.jsonResponse(cs,"STATUS","OK","TEMPORARY",false);
}


/********************************************************************************/
/*                                                                              */
/*      Handle FORGOT PASSWORD request                                          */
/*                                                                              */
/********************************************************************************/

public String handleForgotPassword(HttpExchange e,CatreSession cs)
{
   CatreLog.logD("CATSERVE","handleForgotPasswrd entered");
   
   String email = CatserveServer.getParameter(e,"email");
   if (email == null || email.isEmpty()) {
      return CatserveServer.jsonError(cs,"No email given");
    }
   
   CatreUser cu = catre_control.getDatabase().findUserByEmail(email);
   if (cu == null) return CatserveServer.jsonError(cs,"Unknown email given");
   
   String npwd = CatreUtil.randomString(12);
   
   cu.setTemporaryPassword(npwd);
   
   String body = "Sherpa has set up a temporary, one-time password that you ";
   body += "can use to log-in and change your password. ";
   body += "\n\n";
   body += "To do so, login to sherpa with your username (" + cu.getUserName() + ") ";
   body += "and the password " + npwd + ".";
         
   if (CatreUtil.sendEmail(email,"SHERPA password request",body)) {
      return CatserveServer.jsonResponse(cs);
    }
   
   return CatserveServer.jsonError(cs,"Email failed");
}




/********************************************************************************/
/*										*/
/*	Handle Login								*/
/*										*/
/********************************************************************************/
public String handleLogin(HttpExchange e, CatreSession cs)
{
   if (cs == null) return CatserveServer.jsonError(cs,"Bad session");
   
   String username = CatserveServer.getParameter(e,"username");
   String pwd = CatserveServer.getParameter(e,"password");
   pwd = pwd.replace(' ','+');
   String salt = CatserveServer.getParameter(e,"SALT");
   String salt1 = cs.getValue("SALT");
   CatreLog.logD("CATSERVE","LOGIN " + username + " " + pwd + " " + salt);

   if (username == null || pwd == null) {
      return CatserveServer.jsonError(cs,"Missing username or password");
   }
   else if (salt == null || salt1 == null || !salt.equals(salt1)) {
      return CatserveServer.jsonError(cs,"Bad setup");
   }
   else{
      CatreUser cu = catre_control.getDatabase().findUser(username,pwd,salt);
      if (cu == null) {
         return CatserveServer.jsonError(cs,"Bad user name or password");
       }
      else {
         cs.setupSession(cu);
         cs.saveSession(catre_control);
         return CatserveServer.jsonResponse(cs,"TEMPORARY",cu.isTemporary());
       }
   }
}


}	// end of class CatserveAuth




/* end of CatserveAuth.java */

