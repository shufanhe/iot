/********************************************************************************/
/*										*/
/*		CatserveSessionManager.java					*/
/*										*/
/*	Session manager for our web server					*/
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

import java.util.HashMap;
import java.util.Map;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.content.Cookie;
import org.nanohttpd.protocols.http.content.CookieHandler;
import org.nanohttpd.protocols.http.response.Response;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreLog;
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

Response setupSession(IHTTPSession s)
{
   CookieHandler cookies = s.getCookies();
   String sessionid = cookies.read(SESSION_COOKIE);
   if (sessionid == null) {
      CatreLog.logD("CATSERVE","Parameters " + s.getParameters() + " " + s.getUri());
      for (String k : s.getParameters().keySet()) {
         CatreLog.logD("CATSERVE","Param " + k + " " + CatserveServer.getParameter(s,k));
       }
      sessionid = CatserveServer.getParameter(s,SESSION_PARAMETER);
    }
   else {
      CatserveServer.setParameter(s,SESSION_PARAMETER,sessionid);
    }

   CatreLog.logD("CATSERVE","SESSION ID " + sessionid);

   CatreSession cs = null;
   if (sessionid == null) {
      cs = beginSession(s);
    }
   else {
      cs = findSession(sessionid);
    }

   if (cs != null) cs.saveSession(catre_control);

   return null;
}





/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

CatreSession beginSession(IHTTPSession s)
{
   CatserveSessionImpl cs = new CatserveSessionImpl();
   String sid = cs.getDataUID();
   session_set.put(sid,cs);

   CatserveServer.setParameter(s,SESSION_PARAMETER,sid);
   CookieHandler cookies = s.getCookies();
   Cookie ck = new Cookie(SESSION_COOKIE,sid);
   cookies.set(ck);

   cs.saveSession(catre_control);

   return cs;
}



String validateSession(IHTTPSession s,String sid)
{
   return sid;
}



void endSession(String sid)
{
   CatserveSessionImpl csi = session_set.remove(sid);
   if (csi != null) csi.removeSession(catre_control);
}



CatreSession findSession(IHTTPSession s)
{
   String sid = CatserveServer.getParameter(s,SESSION_PARAMETER);
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

