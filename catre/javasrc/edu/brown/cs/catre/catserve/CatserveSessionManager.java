/********************************************************************************/
/*										*/
/*		CatserveSessionManager.java					*/
/*										*/
/*	Session manager for our web server					*/
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

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreSession;

class CatserveSessionManager implements CatserveConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,CatserveSessionImpl> session_set;
private CatreController catre_control;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatserveSessionManager(CatreController cc)
{
   session_set = new HashMap<>();
   catre_control = cc;
}



/********************************************************************************/
/*										*/
/*	Working methods 							*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unchecked")
String setupSession(HttpExchange e)
{
   // CookieHandler cookies = e.getCookies();
   // String sessionid = cookies.read(SESSION_COOKIE);

   Headers requestHeaders = e.getRequestHeaders();
   List<String> cookieHeaders = requestHeaders.get("Cookie"); 
   
   //parse for SESSION_COOKIE
   String sessionid = null;
   Map<String,HttpCookie> cookies = parseCookies(cookieHeaders);
   HttpCookie cookie = cookies.get(SESSION_COOKIE);
   String c = (cookie == null ? null : cookie.toString());
   if (c != null) {
      if (c.substring(0, c.indexOf('=')).equals(SESSION_COOKIE)) {
         sessionid = c.substring(c.indexOf('=') + 1, c.length() - 1);
      }
   }
   if (sessionid == null) {
      Map<String,List<String>> params = (Map<String,List<String>>) e.getAttribute("paramMap");
      if (params != null) {
         List<String> sparams = params.get(SESSION_PARAMETER);
         if (sparams != null) sessionid = sparams.get(0);
       }
   }
   else {
      CatserveServer.setParameter(e,SESSION_PARAMETER,sessionid);
   }

   CatreSession cs = null;
   if (sessionid != null) cs = findSession(sessionid);
   if (cs != null && !cs.isValid()) cs = null;
   if (cs == null) cs = beginSession(e);

   if (cs != null) cs.saveSession(catre_control);

   return null;
}

private static Map<String, HttpCookie> parseCookies(List<String> cookieHeaders){
   Map<String, HttpCookie> returnMap = new HashMap<>();
   if (cookieHeaders != null) {
      for (String h : cookieHeaders) {
         String[] headers = h.split(";\\s");
         for (String header : headers) {
            List<HttpCookie> cookies = HttpCookie.parse(header);
            
            for (HttpCookie cookie : cookies) {
               returnMap.put(cookie.getName(), cookie);
            }
         }
      }
  }
   return returnMap;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

CatreSession beginSession(HttpExchange e)
{
   CatserveSessionImpl cs = new CatserveSessionImpl();
   String sid = cs.getDataUID();
   session_set.put(sid,cs);
   CatserveServer.setParameter(e,SESSION_PARAMETER,sid);

   int maxAge = 31536000; // Set the cookie to expire in one year
   String cookie = String.format("%s=%s; Path=%s; Max-Age=%d", SESSION_COOKIE, sid, "/", maxAge);
   e.getResponseHeaders().add("Set-Cookie", cookie);

   cs.saveSession(catre_control);

   return cs;
}



String validateSession(HttpExchange e,String sid)
{
   return sid;
}



void endSession(String sid)
{
   CatserveSessionImpl csi = session_set.remove(sid);
   if (csi != null) csi.removeSession(catre_control);
}



CatreSession findSession(HttpExchange e)
{
   String sid = CatserveServer.getParameter(e,SESSION_PARAMETER);
   if (sid == null) return null;

   return findSession(sid);
}



private CatserveSessionImpl findSession(String sid)
{
   if (sid == null) return null;

   CatserveSessionImpl csi = session_set.get(sid);
   if (csi != null) return csi;

   csi = (CatserveSessionImpl) catre_control.getDatabase().loadObject(sid);
   return csi;
}

/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

}	// end of class CatserveSessionManager


/* end of CatserveSessionManager.java */

