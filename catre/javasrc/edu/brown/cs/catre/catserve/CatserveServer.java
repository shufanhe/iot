/********************************************************************************/
/*										*/
/*		CatserveServer.java						*/
/*										*/
/*	description of class							*/
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
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreSavable;
import edu.brown.cs.catre.catre.CatreSession;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTable;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;

import org.nanohttpd.util.IHandler;
import org.nanohttpd.protocols.http.threading.IAsyncRunner;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.ClientHandler;
import org.json.JSONObject;import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import java.io.IOException;


public class CatserveServer extends NanoHTTPD implements CatserveConstants, IAsyncRunner
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreController catre_control;
private CatserveSessionManager session_manager;
private CatserveAuth auth_manager;

private static Random rand_gen = new Random();
private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatserveServer(CatreController cc) 
{
   super(HTTP_PORT);

   catre_control = cc;
   session_manager = new CatserveSessionManager(catre_control);
   auth_manager = new CatserveAuth(catre_control);

// makeSecure(NanoHTTPD.makeSSLSocketFactory("keystore","pwd".toCharArray()),null);
   addRoute("ALL","/ping",this::handlePing);  
   addRoute("ALL",this::handleParameters);
   addRoute("ALL",session_manager::setupSession);
   addRoute("ALL",this::handleLogging);
   
   addRoute("GET","/login",this::handlePrelogin);
   addRoute("POST","/login",auth_manager::handleLogin);
   addRoute("POST","/register",auth_manager::handleRegister);
   addRoute("GET","/logout",this::handleLogout);
   addRoute("POST","/removeuser",this::handleRemoveUser);
   
   addRoute("ALL",this::handleAuthorize);
   
   addRoute("POST","/bridge/add",this::handleAddBridge);
   addRoute("GET","/universe",this::handleGetUniverse);
   addRoute("POST","/universe/discover",this::handleDiscover);
   addRoute("GET","/rules",this::handleListRules);
   addRoute("POST","/rule/add",this::handleAddRule);
   addRoute("POST","/rule/:ruleid/edit",this::handleEditRule);
   addRoute("POST","/rule/:ruleid/remove",this::handleRemoveRule);
   addRoute("POST","/rule/:ruleid/priority",this::handleSetRulePriority);
   
   cc.register(new SessionTable());
}



/********************************************************************************/
/*                                                                              */
/*      Run methods                                                             */
/*                                                                              */
/********************************************************************************/

public void start() throws IOException
{
   super.start(NanoHTTPD.SOCKET_READ_TIMEOUT,false);
}
   

/********************************************************************************/
/*										*/
/*	Session management							*/
/*										*/
/********************************************************************************/

private Response handleLogging(IHTTPSession s) 
{
   CatreLog.logI("CATSERVE",String.format("REST %s %s %s %s",s.getMethod(),
   s.getUri(),s.getQueryParameterString(),s.getRemoteIpAddress()));

   return null;
}



private Response handleParameters(IHTTPSession s)
{
   if (s.getMethod() != Method.GET) {
      try {
	 Map<String,String> filemap = new HashMap<>();
	 s.parseBody(filemap);
         String jsonstr = filemap.remove("postData");
         Map<String,List<String>> parms = s.getParameters();
         if (jsonstr != null) {
            JSONObject jobj = new JSONObject(jsonstr);
            for (Object keyo : jobj.keySet()) {
               String key = keyo.toString();
               Object val = jobj.get(key);
               List<String> lparm = parms.get(key);
               if (lparm == null) {
                  lparm = new ArrayList<>();
                  parms.put(key,lparm);
                }
               lparm.add(val.toString());
             }
          }
	 if (!filemap.isEmpty()) {
	    for (Map.Entry<String,String> ent : filemap.entrySet()) {
	       String key = "FILE@" + ent.getKey();
	       List<String> vals = parms.get(key);
	       if (vals == null) {
		  vals = new ArrayList<>();
		  parms.put(key,vals);
		}
	       vals.add(ent.getValue());
	     }
	  }
         
       }
      catch (IOException e) {
	 return Response.newFixedLengthResponse(Status.INTERNAL_ERROR,
	       TEXT_MIME,
	       "Server Internal Error: Umonm" + e.getMessage());
       }
      catch (ResponseException e) {
	 return Response.newFixedLengthResponse(e.getStatus(),
	       TEXT_MIME,
	       e.getMessage());
       }
    }

   return null;
}



private Response handlePing(IHTTPSession s) 
{
   String resp = "{ 'pong' : true }";
   return Response.newFixedLengthResponse(Status.OK,JSON_MIME,resp);
}



/********************************************************************************/
/*										*/
/*	Authorization functions 						*/
/*										*/
/********************************************************************************/

private Response handlePrelogin(IHTTPSession s,CatreSession cs)
{
   String salt = randomString(32);
   cs.setValue("SALT",salt);
   return jsonResponse(cs,"SALT",salt);
}







private Response handleAuthorize(IHTTPSession s,CatreSession cs)
{
   if (cs.getUser(catre_control) == null || 
         cs.getUniverse(catre_control) == null) {
      return errorResponse(Status.FORBIDDEN,"Unauthorized");
    }

   return null;
}



private Response handleLogout(IHTTPSession s,CatreSession cs)
{
   if (cs != null) {
      session_manager.endSession(cs.getSessionId());
    }
   
   cs = null;
   
   return jsonResponse(cs);
}


private Response handleRemoveUser(IHTTPSession s,CatreSession cs)
{
   CatreUser cu = cs.getUser(catre_control);
   
   if (cu == null) return jsonError(cs,"User doesn't exist");
   
   CatreUniverse cuv = cs.getUniverse(catre_control);
   if (cuv != null) {
      catre_control.getDatabase().removeObject(cuv.getDataUID());
    }
   if (cu != null) {
      catre_control.getDatabase().removeObject(cu.getDataUID());
    }
   
   return handleLogout(s,cs);
}



/********************************************************************************/
/*                                                                              */
/*      Handle model setup requests                                             */
/*                                                                              */
/********************************************************************************/

private Response handleAddBridge(IHTTPSession s,CatreSession cs)
{ 
   Map<String,String> keys = new HashMap<>();
   String bridge = null;
   
   for (Map.Entry<String,List<String>> ent : s.getParameters().entrySet()) {
      if (ent.getValue() == null || ent.getValue().size() != 1) continue;
      String val = ent.getValue().get(0);
      if (ent.getKey().equalsIgnoreCase("BRIDGE")) bridge = val;
      else if (ent.getKey().startsWith("AUTH")) keys.put(ent.getKey(),val);
    }
   
   boolean fg = cs.getUser(catre_control).addAuthorization(bridge,keys);
   
   if (!fg) {
      return errorResponse("No bridge given");
    }
   
   return jsonResponse(cs,"STATUS","OK");
}



private Response handleDiscover(IHTTPSession s,CatreSession cs)
{ 
   return null;
}


private Response handleGetUniverse(IHTTPSession s,CatreSession cs)
{
   return null;
}


private Response handleListRules(IHTTPSession s,CatreSession cs)
{ 
   return null;
}


private Response handleAddRule(IHTTPSession s,CatreSession cs)
{ 
   return null;
}


private Response handleEditRule(IHTTPSession s,CatreSession cs)
{ 
   return null;
}



private Response handleSetRulePriority(IHTTPSession s,CatreSession cs)
{ 
   return null;
}

private Response handleRemoveRule(IHTTPSession s,CatreSession cs)
{ 
   return null;
}



/********************************************************************************/
/*										*/
/*	Routing methods 							*/
/*										*/
/********************************************************************************/

public void addRoute(String method,String url,IHandler<IHTTPSession,Response> h)
{
   addHTTPInterceptor(new Route(method,url,h));
}


public void addRoute(String method,IHandler<IHTTPSession,Response> h)
{
   addHTTPInterceptor(new Route(method,null,h));
}


public void addRoute(String method,String url,
      BiFunction<IHTTPSession,CatreSession,Response> h)
{
   addHTTPInterceptor(new Route(method,url,h));
}


public void addRoute(String method,
      BiFunction<IHTTPSession,CatreSession,Response> h)
{
   addHTTPInterceptor(new Route(method,null,h));
}




/********************************************************************************/
/*										*/
/*	Handle Routing implementation						*/
/*										*/
/********************************************************************************/

private class Route implements IHandler<IHTTPSession,Response> {

   private int check_method;
   private String check_url;
   private Pattern check_pattern;
   private List<String> check_names;
   private IHandler<IHTTPSession,Response> route_handle;
   private BiFunction<IHTTPSession,CatreSession,Response> route_function;

   Route(String method,String url,IHandler<IHTTPSession,Response> handler) {
      this(method,url);
      route_handle = handler;
    }

   Route(String method,String url,
         BiFunction<IHTTPSession,CatreSession,Response> handler) {
      this(method,url);
      route_function = handler;
    }


   private Route(String method,String url) {
      if (method == null || method.equals("ALL")) check_method = -1;
      else {
	 check_method = 0;
	 String[] ms = method.split(" ,;");
	 for (String mm : ms) {
	    Method m = Method.lookup(mm);
	    check_method |= (1 << m.ordinal());
	  }
       }
      check_url = url;
      route_handle = null;
      route_function = null;
      check_pattern = null;
      check_names = null;
      setupPatterns();
    }

   @Override public Response handle(IHTTPSession sess) {
      int v = 1 << (sess.getMethod().ordinal());
      if ((v & check_method) == 0) return null;
      
      if (check_pattern != null) {
         Matcher m = check_pattern.matcher(sess.getUri());
         if (!m.matches()) return null;
         int idx = 1;
         for (String s : check_names) {
            setParameter(sess,s,m.group(idx++));
          }
       }
      else if (check_url != null && !sess.getUri().startsWith(check_url)) return null;
      
      if (route_handle != null) {
         return route_handle.handle(sess);
       }
      else if (route_function != null) {
         CatreSession s = session_manager.findSession(sess);
         return route_function.apply(sess,s);
       }
   
      return null;
    }
   
   private void setupPatterns() {
      if (check_url == null || !check_url.contains(":")) return;
      check_names = new ArrayList<>();
      String u = check_url;
      String pat = "";
      for (int i = u.indexOf(":"); i >= 0; i = u.indexOf(":")) {
         int j = u.indexOf("/",i);
         pat += u.substring(0,i); 
         pat += "([A-Za-z_]+)";
         String nm = null;
         if (j < 0) {
            nm = u.substring(i+1);
            u = "";
          }
         else {
            nm = u.substring(i+1,j);
            u = u.substring(j);
          }
         check_names.add(nm);
       }
      pat += u;
      check_pattern = Pattern.compile(pat);
    }

}	// end of inner class Handler




/********************************************************************************/
/*										*/
/*	Response methods							*/
/*										*/
/********************************************************************************/

static Response jsonResponse(CatreSession cs,Object ...val)
{
   Map<String,Object> map = new HashMap<>();
   if (cs != null) map.put(SESSION_PARAMETER,cs.getSessionId());

   for (int i = 0; i+1 < val.length; i += 2) {
      String key = val[i].toString();
      Object v = val[i+1];
      map.put(key,v);
    }

   if (map.get("STATUS") == null) map.put("STATUS","OK");

   JSONObject jo = new JSONObject(map);
   return jsonResponse(jo);
}


static Response jsonResponse(JSONObject jo)
{
   String jstr = jo.toString(2);
   
   CatreLog.logD("CATSERVE","RETURN " + jstr);
   
   Response r = Response.newFixedLengthResponse(Status.OK,JSON_MIME,jstr);

   return r;
}


static Response jsonError(CatreSession cs,String msg)
{
   return jsonResponse(cs,"STATUS","ERROR","MESSAGE",msg);
}


static Response errorResponse(String msg)
{
   return errorResponse(Status.BAD_REQUEST,msg);
}


static Response errorResponse(Status sts,String msg)
{
   CatreLog.logD("CATSERVE","ERROR " + sts + " " + msg);
   
   return Response.newFixedLengthResponse(sts,TEXT_MIME,msg);
}


/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

static String getParameter(IHTTPSession s,String name)
{
   List<String> v = s.getParameters().get(name);
   if (v == null) return null;
   return v.get(0);
}


static void setParameter(IHTTPSession s,String name,String val)
{
   if (val == null) s.getParameters().remove(name);
   else {
      s.getParameters().put(name,Collections.singletonList(val));
    }
}


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
/*	Threading methods							*/
/*										*/
/********************************************************************************/

@Override public void closeAll()
{ }

@Override public void closed(ClientHandler ch)
{ }

@Override public void exec(ClientHandler ch)
{ }


/********************************************************************************/
/*                                                                              */
/*      Table for storing sessions                                              */
/*                                                                              */
/********************************************************************************/

private static class SessionTable implements CatreTable {
   
   @Override public String getTableName()               { return "CatreSessions"; }
   
   @Override public String getTablePrefix()             { return SESSION_PREFIX; }
   
   @Override public boolean useFor(CatreSavable cs) {
      return cs instanceof CatreSession;
    }
   
   @Override public CatserveSessionImpl create(CatreStore store,Map<String,Object> data) {
      return new CatserveSessionImpl(store,data);
    }
}       // end of inner class SessionTable



}	// end of class CatserveServer




/* end of CatserveServer.java */

