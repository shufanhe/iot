/********************************************************************************/
/*                            */
/*    CatserveServer.java                 */
/*                            */
/* description of class                   */
/*                            */
/********************************************************************************/
/* Copyright 2023 Brown University -- Steven P. Reiss       */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.           *
 *                             *
 *         All Rights Reserved                *
 *                             *
 *  Permission to use, copy, modify, and distribute this software and its   *
 *  documentation for any purpose other than its incorporation into a       *
 *  commercial product is hereby granted without fee, provided that the     *
 *  above copyright notice appear in all copies and that both that       *
 *  copyright notice and this permission notice appear in supporting     *
 *  documentation, and that the name of Brown University not be used in     *
 *  advertising or publicity pertaining to distribution of the software     *
 *  without specific, written prior permission.              *
 *                             *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS     *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND       *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY    *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY     *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,      *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS       *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE     *
 *  OF THIS SOFTWARE.                         *
 *                             *
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
 import edu.brown.cs.ivy.exec.IvyExecQuery;
 
 import org.nanohttpd.protocols.http.response.Status;
 import org.json.JSONObject;
 import java.util.function.BiFunction;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import com.sun.net.httpserver.HttpsServer;
 import com.sun.net.httpserver.HttpExchange;
 import com.sun.net.httpserver.HttpHandler;
 import com.sun.net.httpserver.HttpsConfigurator;
 import com.sun.net.httpserver.HttpsParameters;
 
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
 import java.lang.reflect.Method;
 import java.net.InetSocketAddress;
 import java.net.URLDecoder;
 import java.net.http.HttpResponse.ResponseInfo;
 import java.nio.charset.StandardCharsets;
 import java.security.KeyStore;
 
 
 public class CatserveServer implements CatserveConstants
 {
 
 
 /********************************************************************************/
 /*                            */
 /* Private Storage                     */
 /*                            */
 /********************************************************************************/
 
 private CatreController catre_control;
 private CatserveSessionManager session_manager;
 private CatserveAuth auth_manager;
 private HttpsServer server;
 
 private ArrayList<Route> interceptors;
 int preroute_index;
 
 
 /********************************************************************************/
 /*                            *
 /* Constructors                        */
 /*                            */
 /********************************************************************************/
 
 public CatserveServer(CatreController cc)
 {
    // Create an HTTPS server (secure)
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
 
    interceptors = new ArrayList<>();
    preroute_index = 0;
 
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
 
    // TODO: get the ruleid and set as param based on url
    addRoute("POST","/rule/:ruleid/edit",this::handleEditRule);
    addRoute("POST","/rule/:ruleid/remove",this::handleRemoveRule);
    addRoute("POST","/rule/:ruleid/priority",this::handleSetRulePriority);
 
    server.createContext("/", new CatreHandler());
 
    cc.register(new SessionTable());
 }
 
 
 private class CatreHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange e) throws IOException {
       for(Route interceptor : interceptors){
          String resp = interceptor.handle(e);
          if(resp != null) {
             sendResponse(e, resp);
             return;
          }
       }
 
       sendResponse(e, "ERROR - not an endpoint");
    }
 }
 
 
 private String handlePing(HttpExchange e)
 {
    return "{ 'pong' : true }";
 }
 
 
 
 /********************************************************************************/
 /*                            */
 /* HTTPS methods                       */
 /*                            */
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
 /*                            */
 /* Run methods                      */
 /*                            */
 /********************************************************************************/
 
 public void start() throws IOException
 {
    this.server.start();
 
    CatreLog.logI("CATSERVE","CATRE SERVER STARTED ON " + HTTPS_PORT);
 }
 
 
 /********************************************************************************/
 /*                            */
 /* Session management                     */
 /*                            */
 /********************************************************************************/
 
 private String handleLogging(HttpExchange e){
    CatreLog.logI("CATSERVE",String.format("REST %s %s %s %s",
       e.getRequestMethod(),
       e.getRequestURI().toString(),
       ((Map<String, List<String>>)e.getAttribute("paramMap")).toString(),
       e.getRemoteAddress().getAddress().getHostAddress()));
    
    return null;
 }
 
 
 private String handleParameters(HttpExchange e) {
    parseQueryParameters(e);
    Map<String, String> filemap = new HashMap<>();
    if (!e.getRequestMethod().equals("GET")) {
       try{
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
       }
       catch (IOException e_IO) {
          return "Server Internal Error: Umonm" + e_IO.getMessage();
       }
       // catch (ResponseException e_R) {
       //    return e_R.getStatus() + " " + e_R.getMessage();
       // }
    }
    
    return null;
 }
 
 
 // /********************************************************************************/
 // /*                            */
 // /* Authorization functions                   */
 // /*                            */
 // /********************************************************************************/
 
 private String handlePrelogin(HttpExchange e, CatreSession cs)
 {
    String salt = CatreUtil.randomString(32);
    cs.setValue("SALT",salt);
    System.out.print("HERE: " + salt);
    return jsonResponse(cs,"SALT",salt);
 }
 
 
 private String handleAuthorize(HttpExchange e,CatreSession cs)
 {
    CatreLog.logD("CATSERVE","AUTHORIZE " + getParameter(e,SESSION_PARAMETER)); 
    if (cs.getUser(catre_control) == null ||
     cs.getUniverse(catre_control) == null) {
       return errorResponse(Status.FORBIDDEN,"Unauthorized");
     }
 
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
 /*                            */
 /* Handle model setup requests                  */
 /*                            */
 /********************************************************************************/
 
 private String handleAddBridge( HttpExchange e,CatreSession cs)
 {
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
       return jsonError(cs,"No bridge given");
     }
 
    return jsonResponse(cs,"STATUS","OK");
 }
 
 
 private String handleKeyPair(HttpExchange e,CatreSession cs)
 {
    String uid = CatreUtil.randomString(16);
    String pat = CatreUtil.randomString(24);
 
    return jsonResponse(cs,"STATUS","OK","UID",uid,"PAT",pat);
 }
 
 
 private String handleDiscover(HttpExchange e,CatreSession cs) {
    //TODO implement discover;
 
    return jsonResponse(cs, "unimplemented");
 }
 
 
 private String handleAddVirtualDevice(HttpExchange e,CatreSession cs)
 {
 
 
     CatreUniverse cu = cs.getUniverse(catre_control);
 
       // JSONObject dev = getJson(s,"DEVICE"); //TODO -- convert this!
       String dev = getParameter(e, "DEVICE"); //.toJson;
       // Map<String,Object> map = dev.toMap();
 
       // CatreDevice cd = cu.createVirtualDevice(cu.getCatre().getDatabase(),map);
 
       // if (cd == null) {
       //    return jsonError(cs,"Bad device definition");
       // }
       // else {
       //    return JsonResponse(cs,"STATUS","OK",
       //       "DEVICE",cd.toJson(),
       //       "DEVICEID",cd.getDeviceId());
       // }
 
       return jsonResponse(cs,"unimplemented");
 }
 
 
 
 private String handleAddWebDevice(HttpExchange e,CatreSession cs) {
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
 
 
 private String handleEnableDevice(HttpExchange e,CatreSession cs) {
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
 
 
 private String handleGetUniverse(HttpExchange e,CatreSession cs) {
    Map<String,Object> unimap = cs.getUniverse(catre_control).toJson();
    //TODO - remove any private information from unimap
 
    JSONObject obj = new JSONObject(unimap);
 
    return jsonResponse(obj);
 }
 
 
 private String handleListRules(HttpExchange e,CatreSession cs) {
    CatreUniverse cu = cs.getUniverse(catre_control);
    CatreProgram cp = cu.getProgram();
    List<CatreRule> rules = cp.getRules();
    List<Map<String,Object>> ruleout = new ArrayList<>();
    for (CatreRule cr : rules) {
       ruleout.add(cr.toJson());
    }
 
    return jsonResponse(cs,"RULES",ruleout);
 }
 
 
 private String handleAddRule(HttpExchange e,CatreSession cs) {
    CatreUniverse cu = cs.getUniverse(catre_control);
    CatreProgram cp = cu.getProgram();
 
    String ruletext = getParameter(e, "RULE");
    JSONObject jobj = new JSONObject(ruletext);
    Map<String,Object> rulemap = jobj.toMap();
 
    CatreLog.logI("CATSERVE","Create rule: " + rulemap);
 
    CatreRule cr = cp.createRule(cu.getCatre().getDatabase(),rulemap);
 
    if (cr == null) {
       return jsonError(cs,"Bad rule definition");
    }
 
    cp.addRule(cr);
 
    return jsonResponse(cs,"STATUS","OK","RULE",cr.toJson());
    
 }
 
 private String handleEditRule(HttpExchange e,CatreSession cs) {
    return handleAddRule(e,cs);
 }
 
 private String handleSetRulePriority(HttpExchange e,CatreSession cs) {
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
 
 
 
 private String handleRemoveRule(HttpExchange e,CatreSession cs) {
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
 /*                            */
 /* Response methods                    */
 /*                            */
 /********************************************************************************/
 
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
 /*                            */
 /* Routing methods                     */
 /*                            */
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
    interceptors.add(preroute_index++,new Route(method,url,h));
 }
 
 
 public void addRoute(String method,
       BiFunction<HttpExchange,CatreSession,String> h)
 {
    addHTTPInterceptor(new Route(method,null,h));
 }
 
 public void addHTTPInterceptor(Route r){
    interceptors.add(r);
 }
 
 
 /********************************************************************************/
 /*                            */
 /* Handle Routing implementation                */
 /*                            */
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
             setParameter(exchange,s,m.group(idx++));
          }
       }
       else if (check_url != null && !exchange.getRequestURI().toString().startsWith(check_url)) return null;       


       if (route_handle != null) {
          return route_handle.handle(exchange);
       } else if (route_function != null) {
          CatreSession cs = session_manager.findSession(exchange);
          return route_function.apply(exchange,cs);
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
    private int getHttpMethodOrdinal(String method){
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
 /*                            */
 /* Helper methods                      */
 /*                            */
 /********************************************************************************/
 
 
 public static void parseQueryParameters(HttpExchange exchange) {
    
    Map<String, List<String>> parameters = new HashMap<>();
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
       Map<String, List<String>> map = (Map<String, List<String>>)e.getAttribute("paramMap");
       return (map).get(name).get(0);
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
 /*                            */
 /* Threading methods                   */
 /*                            */
 /********************************************************************************/
 
 // @Override public void closeAll()
 // { }
 
 // @Override public void closed(ClientHandler ch)
 // { }
 
 // @Override public void exec(ClientHandler ch)
 // { }
 
 
 /********************************************************************************/
 /*                            */
 /* Table for storing sessions                */
 /*                            */
 /********************************************************************************/
 
 private static class SessionTable implements CatreTable {
 
    @Override public String getTableName()    { return "CatreSessions"; }
 
    @Override public String getTablePrefix()     { return SESSION_PREFIX; }
 
    @Override public boolean useFor(CatreSavable cs) {
       return cs instanceof CatreSession;
     }
 
    @Override public CatserveSessionImpl create(CatreStore store,Map<String,Object> data) {
       return new CatserveSessionImpl(store,data);
     }
 }  // end of inner class SessionTable
 
 
 
 }  // end of class CatserveServer
 
 
 
 /* end of CatserveServer.java */
 
 
 
 