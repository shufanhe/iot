/********************************************************************************/
/*										*/
/*		CatserveServer.java						*/
/*										*/
/*	description of class							*/
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
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreProgram;
import edu.brown.cs.catre.catre.CatreRule;
import edu.brown.cs.catre.catre.CatreSavable;
import edu.brown.cs.catre.catre.CatreSession;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreTable;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.catre.catre.CatreUtil;
import edu.brown.cs.ivy.exec.IvyExecQuery;

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

import javax.annotation.Tainted;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;

import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;


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
private int preroute_index;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatserveServer(CatreController cc)
{
   super(HTTPS_PORT);

   catre_control = cc;
   session_manager = new CatserveSessionManager(catre_control);
   auth_manager = new CatserveAuth(catre_control);

   File f1 = cc.findBaseDirectory();
   File f2 = new File(f1,"secret");
   File f3 = new File(f2,"catre.jks");
   File f4 = new File(f2,"catre.props");
   Properties p = new Properties();
   p.put("jkspwd","XXX");
   try (FileInputStream fis = new FileInputStream(f4)) {
      p.loadFromXML(fis);
    }
   catch (IOException e) { }
   String pwd = p.getProperty("jkspwd");

   System.err.println("HOST: " + IvyExecQuery.getHostName());
   if (IvyExecQuery.getHostName().contains("geode.local")) pwd = null;

   try {
      if (pwd != null) {
	 makeSecure(getSSLFactory(f3,pwd),null);
       }
    }
   catch (Exception e) {
      CatreLog.logE("CATSERVE","Problem starting https server",e);
      System.exit(1);
    }

   addRoute("ALL","/ping",this::handlePing);
   addRoute("ALL",this::handleParameters);
   addRoute("ALL",session_manager::setupSession);
   addRoute("ALL",this::handleLogging);

   addRoute("GET","/login",this::handlePrelogin);
   addRoute("POST","/login",auth_manager::handleLogin);
   addRoute("POST","/register",auth_manager::handleRegister);
   addRoute("GET","/logout",this::handleLogout);
   addRoute("GET","/forgotpassword",this::handleForgotPassword);
   addRoute("POST","/removeuser",this::handleRemoveUser);

   preroute_index = interceptors.size();

   addRoute("ALL",this::handleAuthorize);

   addRoute("ALL","/keypair",this::handleKeyPair);

   addRoute("POST","/bridge/add",this::handleAddBridge);
   addRoute("GET","/universe",this::handleGetUniverse);
   addRoute("POST","/universe/discover",this::handleDiscover);
   addRoute("POST","/universe/addvirtual",this::handleAddVirtualDevice);
   addRoute("POST","/universe/addweb",this::handleAddWebDevice);
   addRoute("POST","/universe/removedevice",this::handleRemoveDevice);
   addRoute("POST","/universe/enabledevice",this::handleEnableDevice);
   addRoute("GET","/rules",this::handleListRules);
   addRoute("POST","/rule/add",this::handleAddRule);
   addRoute("POST","/rule/:ruleid/edit",this::handleEditRule);
   addRoute("POST","/rule/:ruleid/remove",this::handleRemoveRule);
   addRoute("POST","/rule/:ruleid/priority",this::handleSetRulePriority);

   cc.register(new SessionTable());
}



/********************************************************************************/
/*										*/
/*	HTTPS methods								*/
/*										*/
/********************************************************************************/

private SSLServerSocketFactory getSSLFactory(File jks,String pwd)
{
   char [] pass = pwd.toCharArray();
   try {
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      InputStream ins = new FileInputStream(jks);
      keystore.load(ins,pass);
      KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(keystore,pass);
      return makeSSLSocketFactory(keystore,kmf);
    }
   catch (Exception e) { }

   return null;
}



/********************************************************************************/
/*										*/
/*	Run methods								*/
/*										*/
/********************************************************************************/

public void start() throws IOException
{
   super.start(NanoHTTPD.SOCKET_READ_TIMEOUT,false);

   CatreLog.logI("CATSERVE","CATRE SERVER STARTED ON " + HTTPS_PORT);
}


/********************************************************************************/
/*										*/
/*	Session management							*/
/*										*/
/********************************************************************************/

private Response handleLogging(IHTTPSession s)
{
   CatreLog.logI("CATSERVE",String.format("REST %s %s %s %s",s.getMethod(),
   s.getUri(),s.getParameters(),s.getRemoteIpAddress()));

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
	    String k0 = "postData";
	    List<String> lparm0 = parms.get(k0);
	    if (lparm0 == null) {
	       lparm0 = new ArrayList<>();
	       parms.put(k0,lparm0);
	       lparm0.add(jsonstr);
	     }
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
   String salt = CatreUtil.randomString(32);
   cs.setValue("SALT",salt);
   return jsonResponse(cs,"SALT",salt);
}




private Response handleAuthorize(IHTTPSession s,CatreSession cs)
{
   CatreLog.logD("CATSERVE","AUTHORIZE " + getParameter(s,SESSION_PARAMETER));
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


private Response handleForgotPassword(IHTTPSession s,CatreSession cs)
{
   // should send email here if needed
   return jsonResponse(null);
}



/********************************************************************************/
/*										*/
/*	Handle model setup requests						*/
/*										*/
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
      return jsonError(cs,"No bridge given");
    }

   return jsonResponse(cs,"STATUS","OK");
}



private Response handleKeyPair(IHTTPSession s,CatreSession cs)
{
   String uid = CatreUtil.randomString(16);
   String pat = CatreUtil.randomString(24);

   return jsonResponse(cs,"STATUS","OK","UID",uid,"PAT",pat);
}



private Response handleDiscover(IHTTPSession s,CatreSession cs)
{
   // TODO: implement discover

   return null;
}


private Response handleAddVirtualDevice(IHTTPSession s,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);

   JSONObject dev = getJson(s,"DEVICE");
   Map<String,Object> map = dev.toMap();

   CatreDevice cd = cu.createVirtualDevice(cu.getCatre().getDatabase(),map);

   if (cd == null) return jsonError(cs,"Bad device definition");

   return jsonResponse(cs,"STATUS","OK",
	 "DEVICE",cd.toJson(),
	 "DEVICEID",cd.getDeviceId());
}


private Response handleAddWebDevice(IHTTPSession s,CatreSession cs)
{
   // TODO : implement new web device

   return null;
}


private Response handleRemoveDevice(IHTTPSession s,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);

   String devid = cs.getParameter(s,"DEVICEID");
   CatreDevice cd = cu.findDevice(devid);
   if (cd == null) return jsonError(cs,"Device not found");

   if (cd.getBridge() != null && cd.isEnabled()) {
      return jsonError(cs,"Can't remove active device");
    }

   cu.removeDevice(cd);

   return jsonResponse(cs,"STATUS","OK");
}



private Response handleEnableDevice(IHTTPSession s,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);

   String devid = cs.getParameter(s,"DEVICEID");
   CatreDevice cd = cu.findDevice(devid);
   if (cd == null) return jsonError(cs,"Device not found");
   String flag = cs.getParameter(s,"ENABLE");
   if (flag == null || flag == "")
      return jsonError(cs,"Enable/disable not given");
   char c0 = flag.charAt(0);
   boolean fg;
   if ("d0fn".indexOf(c0) >= 0) fg = false;
   else if ("e1ty".indexOf(c0) >= 0) fg = true;
   else return jsonError(cs,"Bad enable value");

   cd.setEnabled(fg);

   return jsonResponse(cs,"STATUS","OK");
}



private Response handleGetUniverse(IHTTPSession s,CatreSession cs)
{
   Map<String,Object> unimap = cs.getUniverse(catre_control).toJson();
   // remove any private information from unimap

   JSONObject obj = new JSONObject(unimap);

   return jsonResponse(obj);
}


private Response handleListRules(IHTTPSession s,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   List<CatreRule> rules = cp.getRules();
   List<Map<String,Object>> ruleout = new ArrayList<>();
   for (CatreRule cr : rules) {
      ruleout.add(cr.toJson());
    }

   return jsonResponse(cs,"RULES",ruleout);
}


private Response handleAddRule(IHTTPSession s,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();

   String ruletext = cs.getParameter(s,"RULE");
   JSONObject jobj = new JSONObject(ruletext);
   Map<String,Object> rulemap = jobj.toMap();

   CatreLog.logI("CATSERVE","Create rule: " + rulemap);

   CatreRule cr = cp.createRule(cu.getCatre().getDatabase(),rulemap);

   if (cr == null) return jsonError(cs,"Bad rule definition");

   cp.addRule(cr);

   return jsonResponse(cs,"STATUS","OK","RULE",cr.toJson());
}


private Response handleEditRule(IHTTPSession s,CatreSession cs)
{
   return handleAddRule(s,cs);
}



private Response handleSetRulePriority(IHTTPSession s,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   String rid = cs.getParameter(s,"RULEID");
   CatreRule cr = cp.findRule(rid);
   if (cr == null) return jsonError(cs,"No such rule");

   String pstr = cs.getParameter(s,"PRIORITY");
   if (pstr == null) return jsonError(cs,"No priority given");

   try {
      double p = Double.valueOf(pstr);
      if (p > 0) {
	 cr.setPriority(p);
	 return jsonResponse(cs,"STATUS","OK");
       }
    }
   catch (NumberFormatException e) { }

   return jsonError(cs,"Bad priority value");
}



private Response handleRemoveRule(IHTTPSession s,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   String rid = cs.getParameter(s,"RULEID");
   CatreRule cr = cp.findRule(rid);
   if (cr == null) return jsonError(cs,"No such rule");
   cp.removeRule(cr);

   return jsonResponse(cs,"STATUS","OK");
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


public void addPreRoute(String method,String url,
      BiFunction<IHTTPSession,CatreSession,Response> h)
{
   interceptors.add(preroute_index++,new Route(method,url,h));
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

   if (jo.optString("STATUS",null) == null) jo.put("STATUS","OK");

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

public static @Tainted String getParameter(IHTTPSession s,String name)
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


static @Tainted JSONObject getJson(IHTTPSession s)
{
   String jsonstr = getParameter(s,"postData");
   if (jsonstr == null) return null;
   return new JSONObject(jsonstr);
}


static @Tainted JSONObject getJson(IHTTPSession s,String fld)
{
   String jsonstr = getParameter(s,fld);
   if (jsonstr == null) return null;
   return new JSONObject(jsonstr);
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
/*										*/
/*	Table for storing sessions						*/
/*										*/
/********************************************************************************/

private static class SessionTable implements CatreTable {

   @Override public String getTableName()		{ return "CatreSessions"; }

   @Override public String getTablePrefix()		{ return SESSION_PREFIX; }

   @Override public boolean useFor(CatreSavable cs) {
      return cs instanceof CatreSession;
    }

   @Override public CatserveSessionImpl create(CatreStore store,Map<String,Object> data) {
      return new CatserveSessionImpl(store,data);
    }
}	// end of inner class SessionTable



}	// end of class CatserveServer




/* end of CatserveServer.java */

