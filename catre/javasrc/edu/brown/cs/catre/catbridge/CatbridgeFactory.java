/********************************************************************************/
/*                                                                              */
/*              CatbridgeFactory.java                                           */
/*                                                                              */
/*      Factory for accessing bridges                                           */
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



package edu.brown.cs.catre.catbridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.ivy.file.IvyFile;

public class CatbridgeFactory implements CatbridgeConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<CatbridgeBase> all_bridges;
private Map<String,CatbridgeBase> actual_bridges;

private static String bridge_key = null;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public CatbridgeFactory(CatreController cc)
{
   all_bridges = new ArrayList<>();
   actual_bridges = new HashMap<>();
   
   all_bridges.add(new CatbridgeGeneric(cc));
   all_bridges.add(new CatbridgeSmartThings(cc));
   
   ServerThread sthrd = new ServerThread();
   sthrd.start();
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
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
   for (CatbridgeBase base : all_bridges) {
      if (base.getName().equals(name)) {
         CatbridgeBase cb = base.createBridge(cu);
         if (cb == null) continue;
         actual_bridges.put(cb.getBridgeId(),cb);
         cb.registerBridge();
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
/*                                                                              */
/*      Handle talking to server                                                */
/*                                                                              */
/********************************************************************************/

static JSONObject sendCedesMessage(String cmd,Map<String,Object> data,CatbridgeBase bridge)
{
   if (data == null) data = new HashMap<>();
   if (!cmd.contains("/")) cmd = "catre/" + cmd;
   
   if (bridge != null) data.put("bridgeid",bridge.getBridgeId());
   
   try {
      String url = "https://" + CEDES_HOST + ":" + CEDES_PORT + "/" + cmd;
      CatreLog.logD("CATBRIDGE","Send to CEDES: " + url);
      URL u = new URL(url);
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
      
      JSONObject obj = new JSONObject(data);
      OutputStream ots = hc.getOutputStream();
      ots.write(obj.toString(2).getBytes());
      
      InputStream ins = hc.getInputStream();
      String rslts = IvyFile.loadFile(ins);
      return new JSONObject(rslts);
    }
   catch (IOException e) {
      CatreLog.logE("CATBRIDGE","Problem sending command to CEDES",e);
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Server Thread                                                           */
/*                                                                              */
/********************************************************************************/

static String getBridgeKey()                    { return bridge_key; }


private class ServerThread extends Thread {
   
   private ServerSocket server_socket;
   
   ServerThread() {
      super("CatbridgeServerThread");
      try {
         server_socket = new ServerSocket(BRIDGE_PORT);
       }
      catch (IOException e) {
         CatreLog.logE("CATBRIDGE","Can't create server socket on " + BRIDGE_PORT);
         System.exit(1);
       }
      CatreLog.logD("CATBRIDGE","Server running on " + BRIDGE_PORT);
    }
   
   @Override public void run() {
      sendCedesMessage("catre/setup",null,null);
      
      for ( ; ; ) {
         try {
            Socket client = server_socket.accept();
            createClient(client);
          }
         catch (IOException e) {
            System.err.println("signmaker: Error os server accept");
            server_socket = null;
            break;
          }
       }
      System.exit(0);
    }
   
}       // end of inner class ServerThread



/********************************************************************************/
/*                                                                              */
/*      Client management                                                       */
/*                                                                              */
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
      CatreLog.logD("CATBRIDGE","CLIENT " + s.getRemoteSocketAddress());
    }
   
   @Override public void run() {
      JSONObject result = new JSONObject();
      try {
         String args = IvyFile.loadFile(client_socket.getInputStream());
         CatreLog.logD("CATBRIDGE","CLIENT INPUT: " + args);
         JSONObject argobj = new JSONObject(args);
         result.put("status","OK");
         String cmd = argobj.getString("command");
         CatbridgeBase bridge = null;
         String bid = argobj.optString("bid",null);
         if (bid != null) {
            bridge = actual_bridges.get(bid);
          }
         switch (cmd) {
            case "INITIALIZE" :
               bridge_key = argobj.getString("auth");
               for (CatbridgeBase cb : actual_bridges.values()) {
                  cb.registerBridge();
                }
               break;
            case "DEVICES" :
               JSONArray devs = argobj.getJSONArray("devices");
               bridge.handleDevicesFound(devs);
               break;
            case "EVENT" :
               bridge.handleEvent(argobj.getJSONObject("event"));
               break;
          }
       }
      catch (IOException e) {
         result.put("status","ERROR");
         result.put("message",e.toString());
       }
      catch (Throwable e) {
         CatreLog.logE("CATBRIDGE","Problem processing input",e);
         result.put("status","ERROR");
         result.put("message",e.toString());
       }
      
      try {
         OutputStreamWriter otw = new OutputStreamWriter(client_socket.getOutputStream());
         otw.write(result.toString(2));
         otw.close();
       }
      catch (IOException e) {
         
       }
    }

}       // end of inner class ClientThread



}       // end of class CatbridgeFactory




/* end of CatbridgeFactory.java */

