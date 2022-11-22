/********************************************************************************/
/*                                                                              */
/*              CatbridgeSmartThings.java                                       */
/*                                                                              */
/*      description of class                                                    */
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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;

import edu.brown.cs.catre.catre.CatreBridge;
import edu.brown.cs.catre.catre.CatreBridgeAuthorization;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreSession;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreUser;
import edu.brown.cs.ivy.file.IvyFile;



class CatbridgeSmartThings extends CatbridgeBase implements CatreBridge, CatbridgeConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  access_token;
private String  api_address;
private JSONObject api_endpoint;

private Map<CatreUniverse,CatbridgeSmartThings> known_instances;



private static Map<String,String> known_capabilities;


static {
   known_capabilities = new HashMap<String,String>();
   known_capabilities.put("acceleration", "Acceleration Sensor");
   known_capabilities.put("actuator", "Actuator");
   known_capabilities.put("alarm", "Alarm");
   known_capabilities.put("battery", "Battery");
   known_capabilities.put("beacon", "Beacon");
   known_capabilities.put("button", "Button");
   known_capabilities.put("carbonMonoxideDetector", "Carbon Monoxide Detector");
   known_capabilities.put("colorControl", "Color Control");
   known_capabilities.put("configuration", "Configuration");
   known_capabilities.put("contact", "Contact Sensor");
   known_capabilities.put("doorControl", "Door Control");
   known_capabilities.put("energyMeter", "Energy Meter");
   known_capabilities.put("illuminanceMeasurement", "Illuminance Measurement");
   known_capabilities.put("lock", "Lock");
   known_capabilities.put("momentary", "Momentary");
   known_capabilities.put("motion", "Motion Sensor");
   known_capabilities.put("notification", "Notification");
   known_capabilities.put("polling", "Polling");
   known_capabilities.put("powerMeter", "Power Meter");
   known_capabilities.put("presence", "Presence Sensor");
   known_capabilities.put("refresh", "Refresh");
   known_capabilities.put("relativeHumidityMeasurement", "Relative Humidity Measurement");
   known_capabilities.put("relaySwitch", "Relay Switch");
   known_capabilities.put("sensor", "Sensor");
   known_capabilities.put("signalStrength", "Signal Strength");
   known_capabilities.put("sleepSensor", "Sleep Sensor");
   known_capabilities.put("smokeDetector", "Smoke Detector");
   known_capabilities.put("stepSensor", "Step Sensor");
   known_capabilities.put("switch", "Switch");
   known_capabilities.put("temperature", "Temperature Measurement");
   known_capabilities.put("thermostatCoolingSetpoint", "Thermostat Cooling Setpoint");
   known_capabilities.put("thermostatFanMode", "Thermostat Fan Mode");
   known_capabilities.put("thermostatHeatingSetpoint", "Thermostat Heating Setpoint");
   known_capabilities.put("thermostatMode", "Thermostat Mode");
   known_capabilities.put("thermostatOperatingState", "Thermostat Operating State");
   known_capabilities.put("thermostatSetpoint", "Thermostat Setpoint");
   known_capabilities.put("threeAxis", "ThreeAxis");
   known_capabilities.put("tone", "Tone");
   known_capabilities.put("touch", "Touch Sensor");
   known_capabilities.put("valve", "Valve");
   known_capabilities.put("waterSensor", "Water Sensor");
}




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private CatbridgeSmartThings(CatbridgeSmartThings base,CatreUniverse u)
{
   super(base,u);
   CatreUser cu = u.getUser();
   CatreBridgeAuthorization ba = cu.getAuthorization("SmartThings");
   access_token = ba.getValue("AUTH_TOKEN");
   api_address = ba.getValue("AUTH_API");
   known_instances = null;
}


CatbridgeSmartThings(CatreController cc)
{
   for_universe = null;
   access_token = null;
   api_address = null;
   known_instances = new HashMap<>();
   setupRoutes(cc);
}



/********************************************************************************/
/*                                                                              */
/*      Create instance for a particular universe                               */
/*                                                                              */
/********************************************************************************/

@Override protected 
CatbridgeSmartThings createInstance(CatreUniverse u)
{
   CatbridgeSmartThings cb = known_instances.get(u);
   
   CatreUser cu = u.getUser();
   CatreBridgeAuthorization ba = cu.getAuthorization("SmartThings");
   if (ba == null) {
      if (cb != null) known_instances.remove(u);
      return null;
    }
   
   if (cb == null) cb = new CatbridgeSmartThings(this,u);
   
   return cb;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getName()               { return "SmartThings"; }



/********************************************************************************/
/*                                                                              */
/*      Find devices via the bridge                                             */
/*                                                                              */
/********************************************************************************/

@Override
public List<CatreDevice> findDevices()
{
   List<CatreDevice> rslt = new ArrayList<>();
   
   loadDevices();
   
   return rslt;
}



private void loadDevices()
{
   String q = api_address + "?access_token=" + access_token;
   JSONArray endpoints = sendArrayRequest("GET",q);
   if (endpoints == null || endpoints.length() == 0) {
      CatreLog.logT("CATBRIDGE","No endpoint found");
      System.exit(1);
    }
   api_endpoint = endpoints.getJSONObject(0);
   
   List<CatbridgeSmartThingsDevice> adddevices = new ArrayList<CatbridgeSmartThingsDevice>();
   
   for (String key : known_capabilities.keySet()) {
      String q1 = "https://graph.api.smartthings.com" + api_endpoint.getString("url") + "/" + key;
      CatreLog.logD("CATBRIDGE","SEND: " + q1);

      JSONArray devs = sendArrayRequest("GET",q1);
      for (int i = 0; i < devs.length(); ++i) {
	 JSONObject devobj = devs.getJSONObject(i);
	 CatreLog.logD("CATBRIDGE","DEVICE: " + devobj);
	 String id = devobj.getString("id");
	 JSONObject value = devobj.getJSONObject("value");
	 String lbl = devobj.getString("label");
	 CatbridgeSmartThingsDevice bd = (CatbridgeSmartThingsDevice) device_map.get(id);
	 if (bd == null) {
	    bd = CatbridgeSmartThingsDevice.createDevice(this,lbl,id,key);
	    adddevices.add(bd);
	    device_map.put(id,bd);
	  }
// 	 bd.addSTCapability(uc);
// 	 bd.handleValue(stc,value);
       }
    }
// 
// for (SmartThingsDevice std : adddevices) {
//    addDevice(std);
//  }
// 
// for (UpodDevice ud : getDevices()) {
//    if (ud instanceof SmartThingsDevice) {
// 	 SmartThingsDevice std = (SmartThingsDevice) ud;
// 	 if (device_map.get(std.getSTId()) == null) {
// 	    ud.enable(false);
// 	  }
//     }
//  }
}


/********************************************************************************/
/*                                                                              */
/*      Communication methods                                                   */
/*                                                                              */
/********************************************************************************/


JSONArray sendArrayRequest(String method,String rqst)
{
   String json = sendRequest(method,rqst);
   if (json == null) return null;
   return new JSONArray(json);
}


Object sendObjectRequest(String method,String rqst)
{
   String json = sendRequest(method,rqst);
   if (json == null) return null;
   return new JSONObject(json);
}



synchronized void sendCommand(String type,CatreDevice std,Object rslt)
{
// String urlstr = "https://graph.api.smartthings.com" + api_endpoint.getString("url") + "/" + type;
// urlstr += "/" + std.getSTId();
// 
   String urlstr = null;
   String cnts = rslt.toString();
   try {
      URL u = new URL(urlstr);
      HttpURLConnection conn = (HttpURLConnection) u.openConnection();
      conn.setRequestMethod("PUT");
      conn.setDoOutput(true);
      conn.setDoInput(true);
      conn.setRequestProperty("Accept","application/json");
      conn.setRequestProperty("Content-Type","application/json");
      conn.setRequestProperty("Content-Length",Integer.toString(cnts.length()));
      conn.setRequestProperty("User-Agent","smarthab");
      conn.setRequestProperty("Authorization","Bearer " + access_token);
      OutputStreamWriter ots = new OutputStreamWriter(conn.getOutputStream());
      ots.write(cnts);
      ots.close();
      InputStreamReader ins = new InputStreamReader(conn.getInputStream());
      String json = IvyFile.loadFile(ins);
      CatreLog.logD("CATBRIDGE","COMMAND RESULT = " + json);
      ins.close();
    }
   catch (IOException e) {
      CatreLog.logE("CATBRIDGE","Problem sending command: " + e,e);
    }
   
   CatreLog.logD("CATBRIDGE","TRY: curl -H 'Authorization: Bearer " + access_token + "' " +
         urlstr + " -X PUT -d '" + cnts + "'");
   
   CatreLog.logD("CATBRIDGE","SEND: " + urlstr);
   
}



synchronized String sendRequest(String method,String rqst)
{
   try {
      String urlstr = rqst;
      URL u = new URL(urlstr);
      HttpURLConnection conn = (HttpURLConnection) u.openConnection();
      conn.setRequestMethod(method);
      conn.setDoOutput(false);
      conn.setDoInput(true);
      conn.setRequestProperty("Accept","application/json");
      conn.setRequestProperty("User-Agent","smarthab");
      conn.setRequestProperty("Authorization","Bearer " + access_token);
      InputStreamReader ins = new InputStreamReader(conn.getInputStream());
      String json = IvyFile.loadFile(ins);
      ins.close();
      // BasisLogger.logD("RECEIVED:\n" + json);
      return json;
    }
   catch (IOException e) {
      CatreLog.logE("CATBRIDGE","I/O problem with server for " + method + " " + rqst,e);
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Handle listening to smartthings events                                  */
/*                                                                              */
/********************************************************************************/

private void setupRoutes(CatreController cc)
{
   cc.addRoute("POST","/smartthings",this::handleSmartThings);
}


private Response handleSmartThings(IHTTPSession s,CatreSession cs) 
{
   CatreLog.logI("CATBRIDGE","Recieved from smartthings: " +
         s.getUri());
   for (String key : s.getParameters().keySet()) {
       StringBuffer buf = new StringBuffer();
       for (String sv : s.getParameters().get(key)) {
          buf.append(sv);
          buf.append(",");
        }
       CatreLog.logI("CATBRIDGE","\t" + key + " = " + buf.toString());
    }
   
   return cs.jsonResponse("STATUS","OK");
}


}       // end of class CatbridgeSmartThings




/* end of CatbridgeSmartThings.java */

