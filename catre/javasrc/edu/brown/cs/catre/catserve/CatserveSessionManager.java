/********************************************************************************/
/*                                                                              */
/*              CatserveSessionManager.java                                     */
/*                                                                              */
/*      Session manager for our web server                                      */
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



package edu.brown.cs.catre.catserve;

import java.util.HashMap;
import java.util.Map;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.content.Cookie;
import org.nanohttpd.protocols.http.content.CookieHandler;
import org.nanohttpd.protocols.http.response.Response;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreSession;

class CatserveSessionManager implements CatserveConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,CatserveSessionImpl> session_set;
private CatreController catre_control;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatserveSessionManager(CatreController cc)
{
   session_set = new HashMap<>();
   catre_control = cc;
} 



/********************************************************************************/
/*                                                                              */
/*      Working methods                                                         */
/*                                                                              */
/********************************************************************************/

Response setupSession(IHTTPSession s)
{
   CookieHandler cookies = s.getCookies();
   String sessionid = cookies.read(SESSION_COOKIE);
   if (sessionid == null) {
      sessionid = CatserveServer.getParameter(s,SESSION_PARAMETER);
    }
   else {
      CatserveServer.setParameter(s,SESSION_PARAMETER,sessionid);
    }
   
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
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
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
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

}       // end of class CatserveSessionManager




/* end of CatserveSessionManager.java */

