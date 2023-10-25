/********************************************************************************/
/*										*/
/*		CatserveAuth.java						*/
/*										*/
/*	Handle login/register							*/
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




package edu.brown.cs.catre.catserve;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreException;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreSession;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUser;
import org.nanohttpd.protocols.http.response.Response;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;


class CatserveAuth implements CatserveConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreStore	data_store;
private CatreController catre_control;
private CatserveSessionManager session_manager; 



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatserveAuth(CatreController cc, CatserveSessionManager sm)
{
   catre_control = cc;
   data_store = cc.getDatabase();
   session_manager = sm;
}



/********************************************************************************/
/*										*/
/*	Handle register 							*/
/*										*/
/********************************************************************************/
public class RegisterHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      if (cs.getUser(catre_control) != null) {
         CatserveServer.sendResponse(e, CatserveServer.jsonError(cs,"Can't register while logged in"));
     }

      String userid = CatserveServer.getParameter(e,"username");
      String email = CatserveServer.getParameter(e,"email");
      String pwd = CatserveServer.getParameter(e,"password");
      String unm = CatserveServer.getParameter(e,"universe");

      try {
         CatreUser cu = data_store.createUser(userid,email,pwd);
         catre_control.createUniverse(unm,cu);
         cs.setupSession(cu);
         cs.saveSession(catre_control);
         CatserveServer.sendResponse(e, CatserveServer.jsonResponse(cs));
         CatreLog.logD("CATAUTH","register " + userid + " " + email + " " + pwd + " " + unm);
      }
      catch (CatreException err) {
         String msg = err.getMessage();
         CatserveServer.sendResponse(e, CatserveServer.jsonError(cs,msg));
         CatreLog.logD("ERROR CATAUTH","register " + userid + " " + email + " " + pwd + " " + unm);
      }
   }
}


/********************************************************************************/
/*										*/
/*	Handle Login								*/
/*										*/
/********************************************************************************/
public void handleLogin(HttpExchange e, CatreSession cs){
   String username = CatserveServer.getParameter(e,"username");
   String pwd = CatserveServer.getParameter(e,"password");
   String salt = CatserveServer.getParameter(e,"SALT");
   String salt1 = cs.getValue("SALT");
   CatreLog.logD("CATSERVE","LOGIN " + username + " " + pwd + " " + salt);

   if (username == null || pwd == null) {
      CatserveServer.sendResponse(e, CatserveServer.jsonError(cs,"Missing username or password"));
   }
   else if (salt == null || salt1 == null || !salt.equals(salt1)) {
      CatserveServer.sendResponse(e, CatserveServer.jsonError(cs,"Bad setup"));
   }
   else{
      CatreUser cu = catre_control.getDatabase().findUser(username,pwd,salt);
      if (cu == null) {
         CatserveServer.sendResponse(e, CatserveServer.jsonError(cs,"Bad user name or password"));
      } else {
         cs.setupSession(cu);
         cs.saveSession(catre_control);
         CatserveServer.sendResponse(e, CatserveServer.jsonResponse(cs));
      }
   }
}

}	// end of class CatserveAuth




/* end of CatserveAuth.java */

