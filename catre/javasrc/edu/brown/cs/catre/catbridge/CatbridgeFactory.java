/********************************************************************************/
/*										*/
/*		CatbridgeFactory.java						*/
/*										*/
/*	Factory for accessing bridges						*/
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




package edu.brown.cs.catre.catbridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreOauth;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.ivy.file.IvyFile;

public class CatbridgeFactory implements CatbridgeConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<CatbridgeBase> all_bridges;
private Map<String,CatbridgeBase> actual_bridges;
private CatreController catre_control;
private Set<String> active_keys;

private static String bridge_key = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public CatbridgeFactory(CatreController cc)
{
   catre_control = cc;
   all_bridges = new ArrayList<>();
   actual_bridges = new HashMap<>();
   active_keys = new HashSet<>();

   all_bridges.add(new CatbridgeGeneric(cc));
   all_bridges.add(new CatbridgeIQsign(cc));
   all_bridges.add(new CatbridgeGoogleCalendar(cc));
   all_bridges.add(new CatbridgeSamsung(cc));

   ServerThread sthrd = new ServerThread();
   sthrd.start();
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public Collection<CatreBridge> getAllBridges(CatreUniverse cu)
{
   List<CatreBridge> rslt = new ArrayList<>();
   for (CatbridgeBase base : all_bridges) {
      CatreBridge bridge = base.createBridge(cu);
      if (bridge != null) rslt.add(bridge);
    }
   return rslt;
}


public CatreBridge createBridge(String name,CatreUniverse cu)
{
   CatreLog.logD("CATBRIDGE","CREATE BRIDGE " + name + " " +
         cu.getUser().getUserName());

   for (CatbridgeBase base : all_bridges) {
      if (base.getName().equals(name)) {
	 CatbridgeBase cb = base.createBridge(cu);
	 if (cb == null) continue;
	 CatbridgeBase oldcb = actual_bridges.put(cb.getBridgeId(),cb);
         if (oldcb == null) {
            cb.registerBridge();
          }
	 return cb;
       }
    }
   return null;
}



public void setupForUser(CatreUser cu)
{
   CatreLog.logD("CATBRIDGE","SETUP " + cu.getUserName());

   CatreUniverse univ = cu.getUniverse();
   if (univ == null) return;

   CatreLog.logD("CATBRIDGE","SETUP FOR USER " + univ.getName());

   for (CatbridgeBase base : all_bridges) {
      CatreBridge cb = createBridge(base.getName(),univ);
      if (cb != null) {
	 CatreLog.logD("CATBRIDGE","Setup bridge " + cb.getName() + " FOR " + cu.getUserName() + " " +
	       univ.getName());
       }
    }
}



/********************************************************************************/
/*										*/
/*	Handle talking to server						*/
/*										*/
/********************************************************************************/

static JSONObject sendCedesMessage(String cmd,Map<String,Object> data,CatbridgeBase bridge)
{
   if (data == null) data = new HashMap<>();
   if (!cmd.contains("/")) cmd = "catre/" + cmd;
   JSONObject obj = new JSONObject(data);
   
   if (bridge != null) {
      data.put("bridgeid",bridge.getBridgeId());
      data.put("bridge",bridge.getName().toLowerCase());
    }

   try {
      String url = "https://" + CEDES_HOST + ":" + CEDES_PORT + "/" + cmd;
      CatreLog.logD("CATBRIDGE","Send to CEDES: " + url + " " + obj.toString(2));
      URL u = new URI(url).toURL();
      HttpURLConnection hc = (HttpURLConnection) u.openConnection();
      hc.setUseCaches(false);
      hc.setRequestMethod("POST");
      hc.addRequestProperty("content-type","application/json");
      hc.addRequestProperty("accept","application/json");
      String key = CatbridgeFactory.getBridgeKey();
      if (key != null) {
	 hc.addRequestProperty("Authorization","Bearer " + key);
	 data.put("bearer_token",key);
       }
      hc.setDoOutput(true);
      hc.setDoInput(true);

      hc.connect();

      OutputStream ots = hc.getOutputStream();
      ots.write(obj.toString(2).getBytes());

      InputStream ins = hc.getInputStream();
      String rslts = IvyFile.loadFile(ins);
      JSONObject jrslt = new JSONObject(rslts);
         CatreLog.logD("CATBRIDGE","CEDES RETURNED " + jrslt.toString(2));
      return jrslt;
    }
   catch (ConnectException e) {
      CatreLog.logD("CATBRIDGE","CEDES NO RESULT: Waiting for CEDES to allow connections");
    }
   catch (IOException | URISyntaxException e) {
      CatreLog.logE("CATBRIDGE","Problem sending command to CEDES",e);
      JSONObject rslt = new JSONObject();
      rslt.put("status","ERROR");
      rslt.put("reason","Bad connection to CEDES");
      return rslt;
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Server Thread								*/
/*										*/
/********************************************************************************/

static String getBridgeKey()			{ return bridge_key; }


private class ServerThread extends Thread {

   private ServerSocket server_socket;

   ServerThread() {
      super("CatbridgeServerThread");
      try {
         server_socket = new ServerSocket(BRIDGE_PORT);
       }
      catch (IOException e) {
         CatreLog.logE("CATBRIDGE","Can't create server socket on " + BRIDGE_PORT,e);
         System.exit(1);
       }
      CatreLog.logD("CATBRIDGE","Server running on " + BRIDGE_PORT);
    }

   @Override public void run() {
      for ( ; ; ) {
         JSONObject resp = sendCedesMessage("catre/setup",null,null);
         if (resp != null) break;
         try {
            Thread.sleep(1000); 
          }
         catch (InterruptedException e) { }
       }
   
      for ( ; ; ) {
         try {
            Socket client = server_socket.accept();
            createClient(client);
          }
         catch (IOException e) {
            CatreLog.logE("CATBRIDGE","Error on server accept",e);
            server_socket = null;
            break;
          }
       }
      System.exit(0);
    }

}	// end of inner class ServerThread



/********************************************************************************/
/*										*/
/*	Client management							*/
/*										*/
/********************************************************************************/

private void createClient(Socket s)
{
   ClientThread cthread = new ClientThread(s);
   cthread.start();
}


private class ClientThread extends Thread {

   private Socket client_socket;

   ClientThread(Socket s) {
      super("Catbridge_Listener_" + s.getRemoteSocketAddress());
      client_socket = s;
      CatreLog.logD("CATBRIDGE","CLIENT Started " + s.getRemoteSocketAddress());
    }

   @Override public void run() {
      JSONObject result = new JSONObject();
      try {
         String args = IvyFile.loadFile(client_socket.getInputStream());
         JSONObject argobj = new JSONObject(args);
         CatreLog.logD("CATBRIDGE","BRIDGE COMMAND: " + argobj.toString(2));
         
         result.put("status","OK");
         String cmd = argobj.getString("command");
         CatbridgeBase bridge = null;
         String bid = argobj.optString("bid",null);
         if (bid != null) {
            bridge = actual_bridges.get(bid);
          }
         CatreOauth oauth = null;
         if (cmd.startsWith("OAUTH_")) {
            oauth = catre_control.getDatabase().getOauth();
          }
         
         switch (cmd) {
            case "INITIALIZE" :
               String key = argobj.getString("auth");
               if (active_keys.add(key)) {
                  bridge_key = key;
                  for (CatbridgeBase cb : actual_bridges.values()) {
                     cb.registerBridge();
                   }
                }
               break;
            case "DEVICES" :
               if (bridge != null) {
                  JSONArray devs = argobj.getJSONArray("devices");
                  bridge.handleDevicesFound(devs);
                }
               break;
            case "EVENT" :
               if (bridge == null) break;
               bridge.handleEvent(argobj.getJSONObject("event"));
               break;
            case "OAUTH_GETTOKEN" :
               result = oauth.getToken(argobj);
               break;
            case "OAUTH_SAVETOKEN" :
               result = oauth.saveToken(argobj);
               break;
            case "OAUTH_REVOKETOKEN" :
               result = oauth.revokeToken(argobj);
               break;
            case "OAUTH_SAVECODE" :
               result = oauth.saveCode(argobj);
               break;
            case "OAUTH_GETCODE" :
               result = oauth.getCode(argobj);
               break;
            case "OAUTH_REVOKE" :
               result = oauth.revokeCode(argobj);
               break;
            case "OAUTH_GETREFRESH" :
               result = oauth.getRefreshToken(argobj);
               break;
            case "OAUTH_VERIFYSCOPE" :
               result = oauth.verifyScope(argobj);
               break;
            case "OAUTH_LOGIN" :
               result = oauth.handleLogin(argobj);
               break;
          }
       }
      catch (IOException e) {
         CatreLog.logE("CATBRIDGE","Problem processing input",e);
         result.put("status","ERROR");
         result.put("message",e.toString());
       }
      catch (Throwable e) {
         CatreLog.logE("CATBRIDGE","Problem processing input",e);
         result.put("status","ERROR");
         result.put("message",e.toString());
       }
   
      try {
         CatreLog.logD("CATBRIDGE","SEND reply to CEDES: " + result.toString(2));
         OutputStreamWriter otw = new OutputStreamWriter(client_socket.getOutputStream());
         otw.write(result.toString(2));
         otw.close();
       }
      catch (IOException e) {
        
       }
    }

}	// end of inner class ClientThread



}	// end of class CatbridgeFactory




/* end of CatbridgeFactory.java */

