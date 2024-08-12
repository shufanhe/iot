/********************************************************************************/
/*										*/
/*	CatserveServer.java							*/
/*										*/
/*	HTTP server for CATRE						       */
/*										*/
/********************************************************************************/
/* Copyright 2023 Brown University -- Steven P. Reiss, Molly E. McHenry 	*/
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				*
 *										*
 *	   All Rights Reserved							*
 *										*
 *  Permission to use, coy, and distribute this software and its       *
 *  documentation for any purpose other than its incorporation into a		*
 *  commercial product is hereby granted without fee, provided that the 	*
 *  above copyright notice appear in all copies and that both that		*
 *  copyright notice and this permission notice appear in supporting		*
 *  documentation, and that the name of Brown University not be used in 	*
 *  advertising or publicity pertaining to distribution of the software 	*
 *  without specific, written prior permission. 				*
 *										*
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		*
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		*
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	*
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	*
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		*
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		*
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	*
 *  OF THIS SOFTWARE.								*
 *										*
 ********************************************************************************/


package edu.brown.cs.catre.catserve;

import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
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
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.karma.KarmaUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;

import javax.annotation.Tainted;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.List;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyStore;


public class CatserveServer implements CatserveConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CatreController catre_control;
private CatserveSessionManager session_manager;
private CatserveAuth auth_manager;
private HttpServer http_server;

private ArrayList<Route> route_interceptors;
private int preroute_index;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatserveServer(CatreController cc)
{
   // Create an HTTPS server (secure)
   try {
      SSLContext sslContext = SSLContext.getInstance("TLS");

      catre_control = cc;
      session_manager = new CatserveSessionManager(catre_control);
      auth_manager = new CatserveAuth(catre_control, session_manager);

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
      String keystore_pwd = p.getProperty("jkspwd");

      System.err.println("HOST: " + IvyExecQuery.getHostName());
      if (IvyExecQuery.getHostName().contains("geode.local")) keystore_pwd = null;
      if (IvyExecQuery.getHostName().contains("Brown-")) keystore_pwd = null;

      if (keystore_pwd != null) {
	 HttpsServer server = HttpsServer.create(new InetSocketAddress(HTTPS_PORT), 0);
	 http_server = server;
	 // makeSecure(getSSLFactory(f3,keystore_pwd, sslContext),null);
	 char[] keystorePassword = keystore_pwd.toCharArray();
	 KeyStore keyStore = KeyStore.getInstance("JKS");
	 FileInputStream keystoreInputStream = new FileInputStream(f3);
	 keyStore.load(keystoreInputStream, keystorePassword);
	
	 KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
	 keyManagerFactory.init(keyStore, keystorePassword);
	
	 sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
	
	 // Set up SSL/TLS properties
	 server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
	    public void configure(HttpsParameters params) {
	       params.setSSLParameters(sslContext.getDefaultSSLParameters());
	     }
	  });
       }
      else {
	 http_server = HttpServer.create(new InetSocketAddress(HTTPS_PORT), 0);
       }
    }
   catch (Exception e) {
      CatreLog.logE("CATSERVE","Problem starting https server",e);
      System.exit(1);
    }

   route_interceptors = new ArrayList<>();
   preroute_index = 0;

   addRoute("ALL","/ping",this::handlePing);
   addRoute("ALL",this::handleParameters);
   addRoute("ALL",session_manager::setupSession);
   addRoute("ALL",this::handleLogging);
   
   addRoute("ALL","/static",this::handleStatic);

   addRoute("GET","/login",this::handlePrelogin);
   addRoute("POST","/login",auth_manager::handleLogin);
   addRoute("POST","/register",auth_manager::handleRegister);
   addRoute("GET","/logout",this::handleLogout);
   addRoute("POST","/forgotpassword",this::handleForgotPassword);

   // might want to handle favicon

   addRoute("ALL",this::handleAuthorize);
   addRoute("POST","/changepassword",auth_manager::handleChangePassword);

   addRoute("ALL",this::handleUserAuthorize);

   addRoute("ALL","/keypair",this::handleKeyPair);
   addRoute("POST","/removeuser",this::handleRemoveUser);

   addRoute("POST","/bridge/add",this::handleAddBridge);
   addRoute("GET","/bridge/list",this::handleListBridges);
   addRoute("GET","/universe",this::handleGetUniverse);
   addRoute("POST","/universe/discover",this::handleDiscover);
   addRoute("POST","/universe/addvirtual",this::handleAddVirtualDevice);
   addRoute("POST","/universe/addweb",this::handleAddWebDevice);
   addRoute("POST","/universe/removedevice",this::handleRemoveDevice);
   addRoute("POST","/universe/enabledevice",this::handleEnableDevice);
   addRoute("POST","/universe/deviceStates",this::handleDeviceStates);
   addRoute("POST","/universe/shareCondition",this::handleShareCondition);
   addRoute("POST","/universe/unshareCondition",this::handleUnshareCondition);
   addRoute("GET","/rules",this::handleListRules);
   addRoute("POST","/rule/add",this::handleAddRule);
   addRoute("POST","/rule/edit",this::handleEditRule);
   addRoute("POST","/rule/validate",this::handleValidateRule);
   addRoute("POST","/rule/remove",this::handleRemoveRule);

   addRoute("POST","/rule/:ruleid/edit",this::handleEditRule);
   addRoute("POST","/rule/:ruleid/remove",this::handleRemoveRule);
   addRoute("POST","/rule/:ruleid/priority",this::handleSetRulePriority);
   

   http_server.createContext("/", new CatreHandler());

   http_server.setExecutor(new ServerExecutor());

   cc.register(new SessionTable());
}


private class CatreHandler implements HttpHandler {

   @Override public void handle(HttpExchange e) throws IOException {
      for(Route interceptor : route_interceptors){
         String resp = interceptor.handle(e);
         if (resp != null) {
            sendResponse(e, resp);
            return;
          }
       }
      String resp = jsonError(null,404,"ILLEGAL - not an endpoint");
      sendResponse(e, resp);
    }

}	// end of inner class CatreHandler


private String handlePing(HttpExchange e)
{
   return "{ 'pong' : true }";
}


private String handleStatic(HttpExchange ex)
{
   URI uri = ex.getRequestURI();
   String path = uri.getPath();
   if (path.startsWith("/static/")) {
      path = path.substring(8);
    }
   if (path.isEmpty()) path = "home.html";
   
   File f1 = catre_control.findBaseDirectory();
   File f2 = new File(f1,"catre");
   File f3 = new File(f2,"web");
   File f4 = new File(f3,path);
   if (f4.exists()) {
      try {
         return IvyFile.loadFile(f4);
       }
      catch (IOException e) {
         // let system return 404
       }
    }
   
   return null;
}


/********************************************************************************/
/*										*/
/*	Run methods								*/
/*										*/
/********************************************************************************/

public void start() throws IOException
{
   http_server.start();

   CatreLog.logI("CATSERVE","CATRE SERVER STARTED ON " + HTTPS_PORT);
}


/********************************************************************************/
/*										*/
/*    Basic Handlers     							*/
/*										*/
/********************************************************************************/

private String handleLogging(HttpExchange e)
{
   @SuppressWarnings("unchecked")
   Map<String,List<String>> params = (Map<String,List<String>>) e.getAttribute("paramMap");
   String plist = null;
   if (params != null) {
      synchronized (params) {
         plist = params.toString();
       }
    }
   CatreLog.logI("CATSERVE",String.format("REST %s %s %s %s %s %s %s",
         e.getRequestMethod(),
         e.getRequestURI().toString(),
         plist,
         e.getLocalAddress(),e.getPrincipal(),e.getProtocol(),
         e.getRemoteAddress().getAddress().getHostAddress()));
   
   return null;
}


private String handleParameters(HttpExchange e)
{
   Map<String,List<String>> params = parseQueryParameters(e);
   if (!e.getRequestMethod().equals("GET")) {
      synchronized(params) {
         try {
            // Parse the request body and populate the filemap
            parsePostParameters(e,params);
          }
         catch (IOException e_IO) {
            return "Server Internal Error: Umonm" + e_IO.getMessage();
          }
       }
    }

   return null;
}


/********************************************************************************/
/*										*/
/*	Authorization functions 						*/
/*										*/
/********************************************************************************/

private String handlePrelogin(HttpExchange e,CatreSession cs)
{
   if (cs == null) return jsonError(cs,"Bad session");
   String salt = CatreUtil.randomString(32);
   cs.setValue("SALT",salt);
   return jsonResponse(cs,"SALT",salt);
}


private String handleAuthorize(HttpExchange e,CatreSession cs)
{
   CatreLog.logD("CATSERVE","AUTHORIZE " + getParameter(e,SESSION_PARAMETER));
   if (cs == null || cs.getUser(catre_control) == null ||
	 cs.getUniverse(catre_control) == null) {
      return jsonError(cs,"Unauthorized access");
    }

   KarmaUtils.event("PREAUTHORIZED");

   return null;
}


private String handleUserAuthorize(HttpExchange e,CatreSession cs)
{
   CatreLog.logD("CATSERVE","AUTHORIZE " + getParameter(e,SESSION_PARAMETER));
   if (cs == null || cs.getUser(catre_control) == null ||
	 cs.getUniverse(catre_control) == null || cs.getUser(catre_control).isTemporary()) {
      return jsonError(cs,"Unauthorized access");
    }

   KarmaUtils.event("AUTHORIZED");

   return null;
}


private String handleLogout(HttpExchange e,CatreSession cs)
{
   if (cs != null) {
      session_manager.endSession(cs.getSessionId());
    }

   cs = null;

   return jsonResponse(cs);
}


private String handleRemoveUser(HttpExchange e,CatreSession cs)
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

   return handleLogout(e,cs);
}


private String handleForgotPassword(HttpExchange exchange,CatreSession cs)
{
   // should send email here if needed
   return jsonResponse(cs,"unimplemented feature");
}



/********************************************************************************/
/*										*/
/*	Handle model setup requests						*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unchecked")
private String handleAddBridge(HttpExchange e,CatreSession cs)
{
   Map<String,String> keys = new HashMap<>();
   String bridge = null;
   
   Map<String,List<String>> params = (Map<String,List<String>>) e.getAttribute("paramMap");
   for (Map.Entry<String,List<String>> ent : params.entrySet()) {
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



private String handleListBridges(HttpExchange e,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   
   Collection<CatreBridge> basebrs = catre_control.getAllBridges(null);
   Collection<CatreBridge> userbrs = catre_control.getAllBridges(cu);
   
   JSONArray rslt = new JSONArray();
   for (CatreBridge cb : basebrs) {
      for (CatreBridge ub1 : userbrs) {
         if (ub1.getName().equals(cb.getName())) {
            cb = ub1;
            break;
          }
       }
      JSONObject obj = cb.getBridgeInfo();  
      rslt.put(obj);
    }
   
   return jsonResponse(cs,"STATUS","OK","BRIDGES",rslt);
}

private String handleKeyPair(HttpExchange e,CatreSession cs)
{
   String uid = CatreUtil.randomString(16);
   String pat = CatreUtil.randomString(24);

   return jsonResponse(cs,"STATUS","OK","UID",uid,"PAT",pat);
}


private String handleDiscover(HttpExchange e,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   
   cu.updateDevices(false); 
   
   return jsonResponse(cs,"STATUS","OK");
}


private String handleAddVirtualDevice(HttpExchange e,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);

   JSONObject dev = getJson(e,"DEVICE"); //TODO -- convert this!
// String dev = getParameter(e, "DEVICE"); //.toJson;
// Map<String,Object> map = dev.toMap();
//
   CatreDevice cd = cu.createVirtualDevice(cu.getCatre().getDatabase(),dev.toMap());

   if (cd == null) {
      return jsonError(cs,"Bad device definition");
    }
   else {
      return jsonResponse(cs,"STATUS","OK",
	    "DEVICE",cd.toJson(),
	    "DEVICEID",cd.getDeviceId());
    }
}



private String handleAddWebDevice(HttpExchange e,CatreSession cs)
{
   // TODO : implement new web device

   return jsonResponse(cs, "unimplemented");
}


private String handleRemoveDevice(HttpExchange e,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);

   String devid = getParameter(e, "DEVICEID"); //.toJson;
   CatreDevice cd = cu.findDevice(devid);
   if (cd == null) {
      return jsonError(cs,"Device not found");
    }

   if (cd.getBridge() != null && cd.isEnabled()) {
      return jsonError(cs,"Can't remove active device");
    }

   cu.removeDevice(cd);
   return jsonResponse(cs,"STATUS","OK");
}


private String handleEnableDevice(HttpExchange e,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);

   String devid = getParameter(e, "DEVICEID");
   CatreDevice cd = cu.findDevice(devid);
   if (cd == null) {
      return jsonError(cs,"Device not found");
    }

   String flag = getParameter(e, "ENABLE");
   if (flag == null || flag == ""){
      return jsonError(cs,"Enable/disable not given");
    }

   char c0 = flag.charAt(0);
   boolean fg;
   if ("d0fn".indexOf(c0) >= 0) fg = false;
   else if ("e1ty".indexOf(c0) >= 0) fg = true;
   else {
      return jsonError(cs,"Bad enable value");
    }

   cd.setEnabled(fg);

   return jsonResponse(cs,"STATUS","OK");
}


private String handleDeviceStates(HttpExchange e,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   String devid = getParameter(e,"DEVICEID");
   CatreDevice cd = cu.findDevice(devid);
   if (cd == null) {
      return jsonError(cs,"Device not found");
    }
   
   JSONObject rslt = new JSONObject();
   for (CatreParameter cp : cd.getParameters()) {
      if (cp.isSensor()) {
         rslt.put(cp.getName(),cd.getParameterValue(cp));
       }
    }
   
   return jsonResponse(cs,rslt);
}




private String handleGetUniverse(HttpExchange e,CatreSession cs)
{
   Map<String,Object> unimap = cs.getUniverse(catre_control).toJson();
   //TODO - remove any private information from unimap
   
   CatreLog.logD("CATSERVE","Return universe map " + unimap);
   
   JSONObject obj = new JSONObject(unimap);
   
   CatreLog.logD("CATSERVE","Return universe " + obj.toString(2));

   return jsonResponse(obj);
}


private String handleShareCondition(HttpExchange e,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   
   String condtest = getParameter(e,"CONDITION");
   JSONObject jobj = new JSONObject(condtest);
   Map<String,Object> condmap = jobj.toMap();
   
   CatreLog.logI("CATSERVE","Share condition: " + jobj.toString(2));
   
   CatreCondition cc = cp.createCondition(cu.getCatre().getDatabase(),condmap);
   
   if (cc == null) {
      return jsonError(cs,"Bad condition definition");
    }
   
   cp.addSharedCondition(cc);
   
   return jsonResponse(cs);
}


private String handleUnshareCondition(HttpExchange e,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   String condname = getParameter(e,"CONDNAME");
   
   CatreLog.logI("CATSERVE","Unshare condition " + condname);
   cp.removeSharedCondition(condname);
   
   return jsonResponse(cs);
}




private String handleListRules(HttpExchange e,CatreSession cs)
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


private String handleAddRule(HttpExchange e,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();

   String ruletext = getParameter(e, "RULE");
   JSONObject jobj = new JSONObject(ruletext);
   Map<String,Object> rulemap = jobj.toMap();

   CatreLog.logI("CATSERVE","Create rule: " + jobj.toString(2));

   CatreRule cr = cp.createRule(cu.getCatre().getDatabase(),rulemap);

   if (cr == null) {
      return jsonError(cs,"Bad rule definition");
    }
   

   cp.addRule(cr);

   return jsonResponse(cs,"STATUS","OK","RULE",cr.toJson());

}


private String handleValidateRule(HttpExchange e,CatreSession cs)
{ 
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   String ruletext = getParameter(e,"RULE");
   JSONObject jobj = new JSONObject(ruletext);
   Map<String,Object> rulemap = jobj.toMap();
   CatreLog.logI("CATSERVER","Validate rule: " + jobj.toString(2));
   CatreRule cr = cp.createRule(cu.getCatre().getDatabase(),rulemap);
   
   if (cr == null) {
      return jsonError(cs,"Bad rule definition"); 
    }
   
   JSONObject rslt = cp.validateRule(cr); 
   
   return jsonResponse(cs,"STATUS","OK","VALIDATION",rslt);
}

private String handleEditRule(HttpExchange e,CatreSession cs)
{
   return handleAddRule(e,cs);
}


private String handleSetRulePriority(HttpExchange e,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   String rid = getParameter(e, "RULEID");
   CatreRule cr = cp.findRule(rid);
   if (cr == null) {
      return jsonError(cs,"No such rule");
    }

   String pstr = getParameter(e, "PRIORITY");
   if (pstr == null) {
      return jsonError(cs,"No priority given");
    }

   try {
      double p = Double.valueOf(pstr);
      if (p > 0) {
	 cr.setPriority(p);
	 return jsonResponse(cs,"STATUS","OK");
       }
    }
   catch (NumberFormatException err) { }

   return jsonError(cs,"Bad priority value");
}



private String handleRemoveRule(HttpExchange e,CatreSession cs)
{
   CatreUniverse cu = cs.getUniverse(catre_control);
   CatreProgram cp = cu.getProgram();
   String rid = getParameter(e, "RULEID");
   CatreRule cr = cp.findRule(rid);
   if (cr == null) {
      return jsonError(cs,"No such rule");
    }
   cp.removeRule(cr);

   return jsonResponse(cs,"STATUS","OK");
}


/********************************************************************************/
/*										*/
/*	Response methods							*/
/*										*/
/********************************************************************************/

static void sendResponse(HttpExchange exchange,String response)
{
   int rcode = 200;
   if (response.startsWith("{") && response.contains("ERROR")) {
      try {
         JSONObject jresp = new JSONObject(response);
         rcode = jresp.optInt("RETURNCODE",500);
       }
      catch (Throwable t) { }
    }
   sendResponse(exchange,response,rcode);
}



static void sendResponse(HttpExchange exchange, String response,int rcode)
{
   CatreLog.logD("CATSERVE","Sending response: " + response);
   
   try{
      exchange.sendResponseHeaders(rcode, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
   catch (IOException e){
      System.err.println("(2) Error sending response to server, message: " + e.getMessage());
    }
}


static String jsonResponse(JSONObject jo)
{
   if (jo.optString("STATUS",null) == null) jo.put("STATUS","OK");

   return jo.toString(2);
}


static String jsonResponse(CatreSession cs,JSONObject jo)
{
   if (jo.optString("STATUS",null) == null) {
      jo.put("STATUS","OK");
    }
   if (jo.optString(SESSION_PARAMETER,null) == null) {
      jo.put(SESSION_PARAMETER,cs.getSessionId());
    }
   
   return jo.toString(2);
}


static String jsonResponse(CatreSession cs, Object... val)
{
   Map<String,Object> map = new HashMap<>();
   if (cs != null) map.put(SESSION_PARAMETER, cs.getSessionId());

   for (int i = 0; i+1 < val.length; i += 2) {
      String key = val[i].toString();
      Object v = val[i+1];
      map.put(key,v);
    }

   if (map.get("STATUS") == null) map.put("STATUS","OK");

   JSONObject jo = new JSONObject(map);
   return jo.toString(2);
}


static String jsonError(CatreSession cs,String msg)
{
   CatreLog.logD("CATSERVE","ERROR " + msg);
   return jsonResponse(cs,"STATUS","FAIL","MESSAGE",msg);
}


static String jsonError(CatreSession cs, int status, String msg)
{
   CatreLog.logD("CATSERVE","ERROR " + status + " " + msg);
   return jsonResponse(cs,"STATUS","ERROR",
        "RETURNCODE",status,
         "MESSAGE",errorResponse(status, msg));
}

static String errorResponse(int status,String msg)
{
   CatreLog.logD("CATSERVE","ERROR " + status + " " + msg);

   return status + ": " + msg;
}




/********************************************************************************/
/*										*/
/*	Routing methods 							*/
/*										*/
/********************************************************************************/

public void addRoute(String method,String url,IHandler<HttpExchange,String> h)
{
   addHTTPInterceptor(new Route(method,url,h));
}


public void addRoute(String method,IHandler<HttpExchange,String> h)
{
   addHTTPInterceptor(new Route(method,null,h));
}


public void addRoute(String method,String url,
      BiFunction<HttpExchange,CatreSession,String> h)
{
   addHTTPInterceptor(new Route(method,url,h));
}


public void addPreRoute(String method,String url,
      BiFunction<HttpExchange,CatreSession,String> h)
{
   route_interceptors.add(preroute_index++,new Route(method,url,h));
}


public void addRoute(String method,
      BiFunction<HttpExchange,CatreSession,String> h)
{
   addHTTPInterceptor(new Route(method,null,h));
}

public void addHTTPInterceptor(Route r)
{
   route_interceptors.add(r);
}


/********************************************************************************/
/*										*/
/*	Handle Routing implementation						*/
/*										*/
/********************************************************************************/

private interface IHandler<I, O> {
   O handle(I input);
}

private class Route {

   private int check_method;
   private String check_url;
   private Pattern check_pattern;
   private List<String> check_names;
   private IHandler<HttpExchange,String> route_handle;
   private BiFunction<HttpExchange,CatreSession,String> route_function;

   Route(String method,String url,IHandler<HttpExchange,String> handler) {
      this(method,url);
      route_handle = handler;
    }

   Route(String method,String url,
	 BiFunction<HttpExchange,CatreSession,String> handler) {
      this(method,url);
      route_function = handler;
    }

   private Route(String method,String url) {
      if (method == null || method.equals("ALL")) check_method = -1;
      else {
	 check_method = 0;
	 String[] ms = method.split(" ,;");
	 for (String mm : ms) {
	    int ordinal = getHttpMethodOrdinal(mm);
	    if (ordinal >= 0) check_method |= (1 << ordinal);
	  }
       }
      check_url = url;
      route_handle = null;
      route_function = null;
      check_pattern = null;
      check_names = null;
      setupPatterns();
    }

   public String handle(HttpExchange exchange) {
      int ordinal = getHttpMethodOrdinal(exchange.getRequestMethod());
      int v = 1 << ordinal;
   
      if ((v & check_method) == 0) return null;
   
      if (check_pattern != null) {
         Matcher m = check_pattern.matcher(exchange.getRequestURI().toString());
         if (!m.matches()) return null;
        
         int idx = 1;
         for (String s : check_names) {
            @Tainted String p = m.group(idx++);
            setParameter(exchange,s,p);
          }
       }
      else if (check_url != null && !exchange.getRequestURI().toString().startsWith(check_url)) return null;	
   
   
      try {
         if (route_handle != null) {
            return route_handle.handle(exchange);
          }
         else if (route_function != null) {
            CatreSession cs = session_manager.findSession(exchange);
            return route_function.apply(exchange,cs);
          }
       }
      catch (Throwable t) {
         CatreLog.logE("CATSERVE","Problem handling input",t);
         return jsonError(null,500,"Problem handling input: " + t);
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

   //gets the ordinal value for a given HTTP string;
   private int getHttpMethodOrdinal(String method) {
      switch (method) {
	 case "GET":
	    return 0;
	 case "POST":
	    return 1;
	 case "PUT":
	    return 2;
	 case "DELETE":
	    return 3;
	 case "HEAD":
	    return 4;
	 case "OPTIONS":
	    return 5;
	 case "TRACE":
	    return 6;
	 case "CONNECT":
	    return 7;
	 case "PATCH":
	    return 8;
	 default:
	    return -1;
       }
    }

}  // end of inner class Route



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/	
/********************************************************************************/

public Map<String,List<String>> parseQueryParameters(HttpExchange exchange) {

   Map<String, List<String>> parameters = new HashMap<>();

   String query = exchange.getRequestURI().getQuery();
   if (query != null) {
      String[] pairs = query.split("&");
      for (String pair : pairs) {
	 String[] keyvalue = pair.split("=");
	 if (keyvalue.length != 2) continue;
	 String key = keyvalue[0];
	 @Tainted String value = keyvalue[1];
         value = CatreUtil.unescape(value);
	
	 // Check if the key already exists in the parameters map
	 List<String> values = parameters.getOrDefault(key, new ArrayList<String>());
	 values.add(value);
	
	 parameters.put(key, values);
       }
    }

   exchange.setAttribute("paramMap", parameters);

   return parameters;
}



public boolean parsePostParameters(HttpExchange exchange,Map<String,List<String>> params) throws IOException
{
   Headers rqhdr = exchange.getRequestHeaders();
   String cnttype = rqhdr.getFirst("Content-Type");
   int cntlen = getBodySize(rqhdr);
   if (cnttype == null) return false;
   if (cntlen > 2*1024*1024) return false;
   String boundary = null;
   boolean json = false;
   if (cnttype.toLowerCase().startsWith("multipart/form-data")) {
      boundary = cnttype.split("boundary=")[1];
    }
   else if (cnttype.toLowerCase().contains("/json")) {
      json = true;
    }
   InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
   BufferedReader br = new BufferedReader(isr);
   if (boundary != null) {		   // multipart
      // TODO: handle multipart form data
    }
   else if (json) {
      if (cntlen == 0) cntlen = 2*1024*1024;
      String cnts = "";
      char buf[] = new char[512];
      while (cntlen > 0) {
	 int rln = Math.min(cntlen,512);
	 int aln = br.read(buf,0,rln);
	 if (aln <= 0) break;
	 cntlen -= aln;
	 cnts += new String(buf,0,aln);
       }
      cnts = cnts.trim();
      if (!cnts.startsWith("{")) return false;
      JSONObject obj = null;
      try {
         obj = new JSONObject(cnts);
       }
      catch (JSONException e) {
         CatreLog.logD("CATSERVE","Problem parsing json: " + e + ":\n" + cnts);
         return false;
       }
      for (Map.Entry<String,Object> ent : obj.toMap().entrySet()) {
	 @Tainted List<String> lparam = params.get(ent.getKey());
	 Object val = ent.getValue();
         if (val == null) continue;
	 if (lparam == null) {
	    lparam = new ArrayList<>();
	    params.put(ent.getKey(),lparam);
	  }
	 if (val instanceof JSONArray) {
	    JSONArray arr = (JSONArray) val;
	    for (int i = 0; i < arr.length(); ++i) {
	       lparam.add(CatreUtil.unescape(arr.getString(i)));
	     }
	  }
	 else if (val instanceof Map<?,?>) {
	    Map<?,?> data = (Map<?,?>) val;
	    JSONObject mobj = new JSONObject(data);
	    String txt = mobj.toString();
            txt = CatreUtil.unescape(txt);
	    lparam.add(txt);
	  }
	 else if (val instanceof Iterable<?>) {
	    Iterable<?> ival = (Iterable<?>) val;
	    try {
	       JSONArray arr = new JSONArray(ival);
	       for (int i = 0; i < arr.length(); ++i) {
                  String v = arr.getString(i);
                  v = CatreUtil.unescape(v);
		  lparam.add(v);
		}
	     }
	    catch (JSONException e) {
	       String txt = val.toString();
	       lparam.add(txt);
	     }
	  }
	 else {
	    String txt = val.toString();
            txt = CatreUtil.unescape(txt);
	    lparam.add(txt);
	  }
       }
    }
   else {
      // handle application/x-www-form-urlencoded
      String query = br.readLine();
      if (query == null) return true;
      String[] keyValuePairs = query.split("&");
      for (String keyValue : keyValuePairs) {
	 String[] parts = keyValue.split("=");
	 if (parts.length == 2) {
	    String key = parts[0];
	    String value = parts[1];
            value = CatreUtil.unescape(value);
	    List<String> lparam = params.get(key);
	    if (lparam == null) {
	       lparam = new ArrayList<>();
	       lparam = KarmaUtils.taint(lparam);
	       params.put(key,lparam);
	     }
	    lparam.add(value);
	  }
       }
    }

   return true;
}


private int getBodySize(Headers hdrs)
{
   if (hdrs.containsKey("content-length")) {
      return Integer.parseInt(hdrs.getFirst("content-length"));
    }
   return 0;
}


@SuppressWarnings("unchecked")
public static @Tainted String getParameter(HttpExchange e,String name)
{
   try {
      Map<String, List<String>> map = (Map<String, List<String>>) e.getAttribute("paramMap");
      return (map).get(name).get(0);
    }
   catch (Exception err){
      return null;
    }
}


@SuppressWarnings("unchecked")
static void setParameter(HttpExchange exchange,String name,String val)
{
   Map<String,List<String>> parameters = (Map<String,List<String>>) exchange.getAttribute("paramMap");
   synchronized(parameters){
      if (val == null) {
         parameters.remove(name);
       }
      else {
         parameters.put(name, Collections.singletonList(val));
       }
    }

   exchange.setAttribute("paramMap", parameters);
}


static @Tainted JSONObject getJson(HttpExchange exchange)
{
   String jsonstr = getParameter(exchange,"postData");
   if (jsonstr == null) return null;
   return new JSONObject(jsonstr);
}


static @Tainted JSONObject getJson(HttpExchange exchange,String fld)
{
   String jsonstr = getParameter(exchange,fld);
   if (jsonstr == null) return null;
   return new JSONObject(jsonstr);
}



/********************************************************************************/
/*										*/
/*	Threading methods							*/
/*										*/
/********************************************************************************/

private class ServerExecutor implements Executor {

   @Override public void execute(Runnable r) {
      catre_control.submit(r);
    }

}	// end of inner class ServerExecutor


/********************************************************************************/
/*										*/
/*	Table for storing sessions						*/
/*										*/
/********************************************************************************/

private static class SessionTable implements CatreTable {

   @Override public String getTableName()    { return "CatreSessions"; }

   @Override public String getTablePrefix()	{ return SESSION_PREFIX; }

   @Override public boolean useFor(CatreSavable cs) {
      return cs instanceof CatreSession;
    }

   @Override public CatserveSessionImpl create(CatreStore store,Map<String,Object> data) {
      return new CatserveSessionImpl(store,data);
    }

}  // end of inner class SessionTable



}  // end of class CatserveServer



/* end of CatserveServer.java */




