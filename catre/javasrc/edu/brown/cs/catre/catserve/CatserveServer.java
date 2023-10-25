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

import java.util.*;

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
import edu.brown.cs.catre.catserve.CatserveAuth.RegisterHandler;
import edu.brown.cs.ivy.exec.IvyExecQuery;

// import org.nanohttpd.util.IHandler;
// import org.nanohttpd.protocols.http.threading.IAsyncRunner;
import org.nanohttpd.protocols.http.response.Status;
// import org.nanohttpd.protocols.http.response.Response;
// import org.nanohttpd.protocols.http.request.Method;
// import org.nanohttpd.protocols.http.NanoHTTPD;
// import org.nanohttpd.protocols.http.IHTTPSession;
// import org.nanohttpd.protocols.http.ClientHandler;
import org.json.JSONObject;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import edu.brown.cs.catre.catserve.ResponseException;

import javax.annotation.Tainted;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;


// public class CatserveServer extends NanoHTTPD implements CatserveConstants, IAsyncRunner
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
private HttpsServer server;


/********************************************************************************/
/*										*
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatserveServer(CatreController cc)
{
   // Create an HTTPs server (secure)
   try{
      server = HttpsServer.create(new InetSocketAddress(HTTPS_PORT), 0);
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

  
      if (keystore_pwd != null) {
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
    }
   catch (Exception e) {
      CatreLog.logE("CATSERVE","Problem starting https server",e);
      System.exit(1);
    }

   server.createContext("/ping", new CatreHandler("GET", new PingHandler()));

   server.createContext("/login", new CatreHandler("", new LoginHandler()));
   server.createContext("/login", new CatreHandler("POST", new LoginHandler()));
   server.createContext("/register", new CatreHandler("POST", auth_manager.new RegisterHandler()));
   server.createContext("/logout", new CatreHandler("GET", new LogoutHandler()));
   server.createContext("/forgotpassword", new CatreHandler("GET", new ForgotPasswordHandler()));
   server.createContext("/removeuser", new CatreHandler("POST", new RemoveUserHandler()));

   server.createContext("/bridge/add", new CatreHandler("POST", new AddBridgeHandler()));
   server.createContext("/universe", new CatreHandler("GET", new GetUniverseHandler()));
   server.createContext("/universe/discover", new CatreHandler("GET", new DiscoverHandler()));
   server.createContext("/universe/addvirtual", new CatreHandler("POST", new AddVirtualDeviceHandler()));
   server.createContext("/universe/addweb", new CatreHandler("POST", new AddWebDeviceHandler()));
   server.createContext("/universe/removedevice", new CatreHandler("POST", new RemoveDeviceHandler()));
   server.createContext("/universe/enabledevice", new CatreHandler("POST", new EnableDeviceHandler()));
   server.createContext("/rules", new CatreHandler("GET", new ListRulesHandler()));
   server.createContext("/rule/add", new CatreHandler("POST", new AddRuleHandler()));
   server.createContext("/rule/:ruleid/edit", new CatreHandler("POST", new AddRuleHandler()));
   server.createContext("/rule/:ruleid/remove", new CatreHandler("POST", new RemoveRuleHandler()));
   server.createContext("/rule/:ruleid/priority", new CatreHandler("POST", new SetRulePriorityHandler()));

   cc.register(new SessionTable());
}


/********************************************************************************/
/*										*/
/*	HTTPS methods								*/
/*										*/
/********************************************************************************/

private SSLServerSocketFactory getSSLFactory(File jks,String pwd, SSLContext context)
{
   char [] pass = pwd.toCharArray();
   try {
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      InputStream ins = new FileInputStream(jks);
      keystore.load(ins,pass);
      KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(keystore,pass);
      return SSLContext.getInstance("TLS").getServerSocketFactory();
      // return makeSSLSocketFactory(keystore,kmf);
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
   this.server.start();

   CatreLog.logI("CATSERVE","CATRE SERVER STARTED ON " + HTTPS_PORT);
}


/********************************************************************************/
/*										*/
/*	Session management							*/
/*										*/
/********************************************************************************/

//this handler call on all of the handlers that should run with every query!
private class CatreHandler implements HttpHandler {
   ICatreHandler caseHandler;
   static String requestType;

   private CatreHandler(String type, ICatreHandler handleCaseMethod){
      requestType = type;
      this.caseHandler = handleCaseMethod;
   }

   @Override
   public void handle(HttpExchange e) throws IOException {
      try{
         handleParameters(e);
      } catch(Exception err){
         sendResponse(e, jsonError(null, 
            Status.INTERNAL_ERROR,
            "Server Internal Error: " + err.getMessage()));
         return;
      }

      try{
         session_manager.setupSession(e);
      } catch (Exception err){
         sendResponse(e, jsonError(null, "Internal Server Error: session setup"));
         return;
      }

      CatreSession cs = session_manager.findSession(e); //BUG: catre session not being loaded in correctly
      try{
         handleLogging(e, cs);
      } catch(Exception err){
         sendResponse(e, jsonError(cs, "Internal Server Error: logging"));
      }

      try{
         handleAuthorize(e, cs);
      } catch(Exception err){
         sendResponse(e, jsonError(cs, "Internal Server Error: authorization"));
         return;
      }

      try{
         handleKeyPair(e, cs);
      } catch(Exception err){
         sendResponse(e, jsonError(cs, "Internal Server Error: KeyPair"));
         return;
      }

      //run specific handler 
      if(e.getRequestMethod().equals(requestType) || requestType.equals("")){
         this.caseHandler.handle(cs, e);
      }
   }
}

private void handleLogging(HttpExchange e, CatreSession cs){
   try{
      CatreLog.logI("CATSERVE",String.format("REST %s %s %s %s",
      e.getRequestMethod(),
      e.getRequestURI().toString(),
      ((Map<String, List<String>>)e.getAttribute("paramMap")).toString(),
      e.getRemoteAddress().getAddress().getHostAddress()));
   } catch(Exception err){
      sendResponse(e, jsonError(cs, "Internal Server Error: logging"));
   }
    System.out.println("handle logging finished!");
}


private Map<String, String> handleParameters(HttpExchange e) throws IOException {
   parseQueryParameters(e);
   Map<String, String> filemap = new HashMap<>();
   if (!e.getRequestMethod().equals("GET")) {
      //  try {
            // Parse the request body and populate the filemap
           filemap = parsePostParameters(e);
           String jsonstr = filemap.remove("postData");

           CatreLog.logI("postData",String.format("%s",jsonstr));
           
           // Retrieve the existing parameters from the HTTP request
           Map<String, List<String>> params = (Map<String, List<String>>)e.getAttribute("paramMap");
           String query = e.getRequestURI().getQuery();

           // If "postData" field is not null, add it to parameters
           if (jsonstr != null) {
               String k0 = "postData";
               List<String> lparam0 = params.get(k0);
               
               if (lparam0 == null) {
                   lparam0 = new ArrayList<>();
                   params.put(k0, lparam0);
               }
               
               lparam0.add(jsonstr);
               
               // Parse the JSON data and add key-value pairs to parameters
               JSONObject jobj = new JSONObject(jsonstr);
               for (Object keyo : jobj.keySet()) {
                   String key = keyo.toString();
                   Object val = jobj.get(key);
                   List<String> lparam = params.get(key);
                   
                   if (lparam == null) {
                       lparam = new ArrayList<>();
                       params.put(key, lparam);
                   }
                   
                   lparam.add(val.toString());
               }
           }
           
           // If there are remaining fields in filemap, add them as parameters
           if (!filemap.isEmpty()) {
               for (Map.Entry<String, String> ent : filemap.entrySet()) {
                   String key = "FILE@" + ent.getKey();
                   List<String> vals = params.get(key);
                   
                   if (vals == null) {
                       vals = new ArrayList<>();
                       params.put(key, vals);
                   }
                   
                   vals.add(ent.getValue());
               }
           }

         // } catch (IOException err) {
         //    // Handle IOException by returning an internal server error response
         //    return Response.newFixedLengthResponse(Status.INTERNAL_ERROR,
         //       TEXT_MIME,
         //       "Server Internal Error: " + err.getMessage());
         // } catch (Exception err) {
         //    // Handle ResponseException by returning an error response
         //    return Response.newFixedLengthResponse(Status.INTERNAL_ERROR,
         //       TEXT_MIME,
         //       err.getMessage());
         // }
         
      }
      return filemap;
   }


private class PingHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      try{
         String response = "{ 'pong' : true }";
         sendResponse(e, response);
       } catch(Exception err){
         sendResponse(e, jsonError(session_manager.findSession(e), "Internal Server Error: Ping"));
      }
   }
}



// /********************************************************************************/
// /*										*/
// /*	Authorization functions 						*/
// /*										*/
// /********************************************************************************/


private class LoginHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      //prelogin
      if(e.getRequestMethod().equals("GET")){
         String salt = CatreUtil.randomString(32);

         cs.setValue("SALT",salt); //TODO - bug in this line!

         System.out.println();
         System.out.println(cs.getValue("SALT"));

         sendResponse(e, jsonResponse(cs,"SALT",salt));
      }

      //login
      else if(e.getRequestMethod().equals("POST")){
         auth_manager.handleLogin(e, cs);
      }    
   }
} 


private void handleAuthorize(HttpExchange e, CatreSession cs){
   CatreLog.logD("CATSERVE","AUTHORIZE " + getParameter(e, SESSION_PARAMETER)); 
   if (cs.getUser(catre_control) == null ||
      cs.getUniverse(catre_control) == null) {
      sendResponse(e, errorResponse(Status.FORBIDDEN,"Unauthorized"));
   }
}


private class LogoutHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {

      if (cs != null) {
         session_manager.endSession(cs.getSessionId());
      }

      cs = null;

      sendResponse(e, jsonResponse(cs));
   }
} 


private class RemoveUserHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      CatreUser cu = cs.getUser(catre_control);

      if (cu == null) sendResponse(e, jsonError(cs,"User doesn't exist"));

      CatreUniverse cuv = cs.getUniverse(catre_control);
      if (cuv != null) {
         catre_control.getDatabase().removeObject(cuv.getDataUID());
      }
      if (cu != null) {
         catre_control.getDatabase().removeObject(cu.getDataUID());
      }

      new LogoutHandler().handle(cs, e);
      }
}


private class ForgotPasswordHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange exchange) {
      // should send email here if needed
      sendResponse(exchange, "unimplemented feature");
   }
}




// /********************************************************************************/
// /*										*/
// /*	Handle model setup requests						*/
// /*										*/
// /********************************************************************************/
// }
private class AddBridgeHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      Map<String,String> keys = new HashMap<>();
      String bridge = null;

      for (Map.Entry<String,List<String>> ent : ((Map<String, List<String>>)e.getAttribute("paramMap")).entrySet()) {
         if (ent.getValue() == null || ent.getValue().size() != 1) continue;
         String val = ent.getValue().get(0);
         if (ent.getKey().equalsIgnoreCase("BRIDGE")) bridge = val;
         else if (ent.getKey().startsWith("AUTH")) keys.put(ent.getKey(),val);
      }

      boolean fg = cs.getUser(catre_control).addAuthorization(bridge,keys);

      if (!fg) {
         sendResponse(e, jsonError(cs,"No bridge given"));
      }

      sendResponse(e, jsonResponse(cs,"STATUS","OK"));
   }
}  


private String handleKeyPair(HttpExchange e,CatreSession cs)
{
   String uid = CatreUtil.randomString(16);
   String pat = CatreUtil.randomString(24);

   return jsonResponse(cs,"STATUS","OK","UID",uid,"PAT",pat);
}


private class DiscoverHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      //TODO implement discover;

      sendResponse(e,jsonResponse(cs, "unimplemented"));
   }
}


private class AddVirtualDeviceHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      CatreUniverse cu = cs.getUniverse(catre_control);

      // JSONObject dev = getJson(s,"DEVICE"); //TODO -- convert this!
      String dev = getParameter(e, "DEVICE"); //.toJson;
      System.out.println("dev: "+dev);
      // Map<String,Object> map = dev.toMap();

      // CatreDevice cd = cu.createVirtualDevice(cu.getCatre().getDatabase(),map);

      // if (cd == null) {
      //    sendResponse(e, jsonError(cs,"Bad device definition"));
      // }
      // else {
      //    sendResponse(e, JsonResponse(cs,"STATUS","OK",
      //       "DEVICE",cd.toJson(),
      //       "DEVICEID",cd.getDeviceId()));
      // }

      sendResponse(e, "unimplemented");
   }
}


private class AddWebDeviceHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      // TODO : implement new web device

      sendResponse(e,jsonResponse(cs, "unimplemented"));
   }
}


private class RemoveDeviceHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      CatreUniverse cu = cs.getUniverse(catre_control);
      
      String devid = getParameter(e, "DEVICEID"); //.toJson;
      CatreDevice cd = cu.findDevice(devid);
      if (cd == null) {
         sendResponse(e, jsonError(cs,"Device not found"));
         return;
      }


      if (cd.getBridge() != null && cd.isEnabled()) {
         sendResponse(e, jsonError(cs,"Can't remove active device"));
         return;
      }


      cu.removeDevice(cd);
      sendResponse(e, jsonResponse(cs,"STATUS","OK"));

   }
}


private class EnableDeviceHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      CatreUniverse cu = cs.getUniverse(catre_control);

      String devid = getParameter(e, "DEVICEID");
      CatreDevice cd = cu.findDevice(devid);
      if (cd == null) {
         sendResponse(e, jsonError(cs,"Device not found"));
         return;
      }

      String flag = getParameter(e, "ENABLE");
      if (flag == null || flag == ""){
         sendResponse(e, jsonError(cs,"Enable/disable not given"));
         return;
      }

      char c0 = flag.charAt(0);
      boolean fg;
      if ("d0fn".indexOf(c0) >= 0) fg = false;
      else if ("e1ty".indexOf(c0) >= 0) fg = true;
      else {
         sendResponse(e, jsonError(cs,"Bad enable value"));
         return;
      }

      cd.setEnabled(fg);

      sendResponse(e, jsonResponse(cs,"STATUS","OK"));
   }
}


private class GetUniverseHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      Map<String,Object> unimap = cs.getUniverse(catre_control).toJson();
      //TODO - remove any private information from unimap

      JSONObject obj = new JSONObject(unimap);

      sendResponse(e,  jsonResponse(obj));
   }
}


private class ListRulesHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      Map<String,Object> unimap = cs.getUniverse(catre_control).toJson();
      //TODO - remove any private information from unimap

      JSONObject obj = new JSONObject(unimap);

      sendResponse(e,  jsonResponse(obj));

      CatreUniverse cu = cs.getUniverse(catre_control);
      CatreProgram cp = cu.getProgram();
      List<CatreRule> rules = cp.getRules();
      List<Map<String,Object>> ruleout = new ArrayList<>();
      for (CatreRule cr : rules) {
         ruleout.add(cr.toJson());
      }

      sendResponse(e, jsonResponse(cs,"RULES",ruleout));
   }
}


private class AddRuleHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      CatreUniverse cu = cs.getUniverse(catre_control);
      CatreProgram cp = cu.getProgram();

      String ruletext = getParameter(e, "RULE");
      JSONObject jobj = new JSONObject(ruletext);
      Map<String,Object> rulemap = jobj.toMap();

      CatreLog.logI("CATSERVE","Create rule: " + rulemap);

      CatreRule cr = cp.createRule(cu.getCatre().getDatabase(),rulemap);

      if (cr == null) {
         sendResponse(e, jsonError(cs,"Bad rule definition"));
         return;
      }

      cp.addRule(cr);

      sendResponse(e, jsonResponse(cs,"STATUS","OK","RULE",cr.toJson()));
   }
}


private class SetRulePriorityHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      CatreUniverse cu = cs.getUniverse(catre_control);
      CatreProgram cp = cu.getProgram();
      String rid = getParameter(e, "RULEID");
      CatreRule cr = cp.findRule(rid);
      if (cr == null) {
         sendResponse(e, jsonError(cs,"No such rule"));
         return;
      }

      String pstr = getParameter(e, "PRIORITY");
      if (pstr == null) {
         sendResponse(e, jsonError(cs,"No priority given"));
         return;
      }

      try {
         double p = Double.valueOf(pstr);
         if (p > 0) {
            cr.setPriority(p);
            sendResponse(e, jsonResponse(cs,"STATUS","OK"));
            return;
         }
      }
      catch (NumberFormatException err) { }

      sendResponse(e, jsonError(cs,"Bad priority value"));
   }
}



private class RemoveRuleHandler implements ICatreHandler {
   @Override
   public void handle(CatreSession cs, HttpExchange e) {
      CatreUniverse cu = cs.getUniverse(catre_control);
      CatreProgram cp = cu.getProgram();
      String rid = getParameter(e, "RULEID");
      CatreRule cr = cp.findRule(rid);
      if (cr == null) {
         jsonResponse(cs, jsonError(cs,"No such rule"));
         return;
      }
      cp.removeRule(cr);

      jsonResponse(cs, jsonResponse(cs,"STATUS","OK"));
   }
}


/********************************************************************************/
/*										*/
/*	Response methods							*/
/*										*/
/********************************************************************************/

// static void sendResponse(HttpExchange exchange, Response response) {
//    try{
//       String responseBody = response.getBody();
//       exchange.sendResponseHeaders(response.getStatus(), responseBody.getBytes().length);
//       InputStream is = response.getData();
//       OutputStream os = exchange.getResponseBody();
//       byte[] buffer = new byte[1024]; // Choose an appropriate buffer size
//       int bytesRead;
//       while ((bytesRead = is.read(buffer)) != -1) {
//          os.write(buffer, 0, bytesRead);
//       }
//       os.close();
//    } catch(IOException e){
//       System.err.println("Error sending response to server, message: " + e.getMessage());
//    }
// }

static void sendResponse(HttpExchange exchange, String response) {
   try{
      int rCode = 200;
      if(response.contains("Error")){
         rCode = 500;
      }
      exchange.sendResponseHeaders(rCode, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
   } catch(IOException e){
      System.err.println("(2) Error sending response to server, message: " + e.getMessage());
   }
}

static String jsonResponse(JSONObject jo)
{
   
   if (jo.optString("STATUS",null) == null) jo.put("STATUS","OK");
   
   return jo.toString(2);
}

static String jsonResponse(CatreSession cs, Object... val) {
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


static String jsonError(CatreSession cs,String msg){
   CatreLog.logD("CATSERVE","ERROR " + msg);
   return jsonResponse(cs,"STATUS","ERROR","MESSAGE",msg);
}

static String jsonError(CatreSession cs, Status status, String msg){
   CatreLog.logD("CATSERVE","ERROR " + status + " " + msg);
   return jsonResponse(cs,"STATUS","ERROR","MESSAGE",errorResponse(status, msg));
}

static String errorResponse(Status status,String msg)
{
   CatreLog.logD("CATSERVE","ERROR " + status + " " + msg);

   return status.getRequestStatus() + " " + status.toString() + " " + msg;
}

static String errorResponse(String msg)
{
   return errorResponse(Status.BAD_REQUEST,msg);
}



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/


public static void parseQueryParameters(HttpExchange exchange) {
   
   Map<String, List<String>> parameters = new HashMap<>();

   String x = exchange.getRequestURI().toString();

   String query = exchange.getRequestURI().getQuery();
   if (query != null) {
       String[] pairs = query.split("&");
       for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            String key = keyValue[0];
            String value = keyValue[1];

            // Check if the key already exists in the parameters map
            List<String> values = parameters.getOrDefault(key, new ArrayList<String>());
            values.add(value);

            parameters.put(key, values);
       }
   }

   exchange.setAttribute("paramMap", parameters);
}

 

public static Map<String, String> parsePostParameters(HttpExchange exchange) throws IOException {
    Map<String, String> parameters = new HashMap<>();
    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        String query = br.readLine();
        if(query == null) return parameters;
        String[] keyValuePairs = query.split("&");
        for (String keyValue : keyValuePairs) {
            String[] parts = keyValue.split("=");
            if (parts.length == 2) {
                String key = parts[0];
                String value = parts[1];
                parameters.put(key, value);
            }
        }
    }
    
    return parameters;
}

public static @Tainted String getParameter(HttpExchange e,String name)
{
   try{
      return ((Map<String, List<String>>)e.getAttribute("paramMap")).get(name).get(0);
   } catch(Exception err){
      return null;
   }
}


static void setParameter(HttpExchange exchange,String name,String val)
{
   // if (val == null) s.getParameters().remove(name);

   Map<String, List<String>> parameters = (Map<String, List<String>>) exchange.getAttribute("paramMap");
   if (val == null) {
      parameters.remove(name);
   } else {
      parameters.put(name, Collections.singletonList(val));
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

// @Override public void closeAll()
// { }

// @Override public void closed(ClientHandler ch)
// { }

// @Override public void exec(ClientHandler ch)
// { }


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

