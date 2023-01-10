/********************************************************************************/
/*                                                                              */
/*              DeviceComputerMonitor.java                                      */
/*                                                                              */
/*      Machine usage monitor sensor device                                     */
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



package edu.brown.cs.iot.device;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public class DeviceComputerMonitor extends DeviceBase
{


/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String ... args)
{
   DeviceComputerMonitor mon = new DeviceComputerMonitor(args);
   mon.start();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

enum ZoomOption { ON_ZOOM, PERSONAL_ZOOM, NOT_ON_ZOOM };
enum WorkOption { WORKING, IDLE, AWAY };

private long    last_idle;
private ZoomOption last_zoom = null;
private long    last_check;

private String  alert_command;
private String  idle_command;
private String  zoom_command;
private File    zoom_docs;

private final String IDLE_COMMAND = "ioreg -c IOHIDSystem | fgrep HIDIdleTime";

private final String ZOOM_COMMAND = "ps -ax | fgrep zoom | fgrep CptHost";

private final String MAC_ALERT_COMMAND = 
   "/usr/bin/osascript -e 'display notification \"$MSG\" with title \"From Sherpa\" sound name \"Basso\" '";

   
private final long POLL_TIME = 30000;

private static Map<String,String> carrier_table;

static {
   carrier_table = new HashMap<>();
   carrier_table.put("att","txt.att.net");
   carrier_table.put("boost","sms.myboostmobile.com");
   carrier_table.put("consumer","mailmymobile.net");
   carrier_table.put("consumercellular","mailmymobile.net");
   carrier_table.put("cricket","sms.cricketwireless.net");
   carrier_table.put("fi","msg.fi.google.com");
   carrier_table.put("replublic","text.republicwireless.com");
   carrier_table.put("sprint", "messaging.sprintpcs.com");
   carrier_table.put("straighttalk","vtext.com");
   carrier_table.put("tmobile","tmomail.net");
   carrier_table.put("ting","message.ting.com");
   carrier_table.put("tracfone","mmst5.tracfone.com");
   carrier_table.put("uscellular","email.uscc.net");
   carrier_table.put("verizon","vtext.com");
   carrier_table.put("virgin","vmobl.com");
   carrier_table.put("virginmobile","vmobl.com"); 
   carrier_table.put("bellmobility","txt.bellmobility.com");
   carrier_table.put("bell mobility","txt.bellmobility.com");
   carrier_table.put("rogers","pcs.rogers.com");
   carrier_table.put("fido","fido.ca");
   carrier_table.put("telus","msg.telus.com");
   carrier_table.put("koodo","msg.koodomobile.com");
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private DeviceComputerMonitor(String [] args)
{
   last_idle = -1;
   last_zoom = null;
   last_check = 0;
   
   String os = System.getProperty("os.name").toLowerCase();
   
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,"Documents");
   zoom_docs = new File(f2,"Zoom");
   
   if (os.contains("mac")) {
      alert_command = MAC_ALERT_COMMAND;
      idle_command = IDLE_COMMAND;
      zoom_command = ZOOM_COMMAND;
    }
   else if (os.contains("win")) {
      alert_command = null;
      idle_command = null;
      zoom_command = null;
      
    }
   else {                               // linux
      alert_command = null;
      idle_command = IDLE_COMMAND;
      zoom_command = ZOOM_COMMAND;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override protected String getDeviceName()
{
   return "COMPUTER_MONITOR_" + getHostName();
}



@Override protected JSONObject getDeviceJson()
{
   JSONObject param0 = buildJson("NAME","Presence",
         "TYPE","ENUM",
         "ISSENSOR",true,
         "ISTARGET",false,
         "VALUES",List.of(WorkOption.values()));
   
   JSONObject param1 = buildJson("NAME","ZoomStatus",
         "TYPE","ENUM",
         "ISSENSOR",true,
         "ISTARGET",false,
         "VALUES",List.of(ZoomOption.values()));
   
   JSONObject tparam2 = buildJson("NAME","Subject","TYPE","STIRNG");
   JSONObject tparam3 = buildJson("NAME","Body","TYPE","STRING");     
   JSONObject tparam4 = buildJson("NAME","Message","TYPE","STRING");
   
   JSONObject trans1 = buildJson("NAME","SendEmail",
         "DEFAULTS",List.of(tparam2,tparam3));
   JSONObject trans2 = buildJson("NAME","SendText","DEFAULTS",List.of(tparam4));
   JSONObject trans3 = buildJson("NAME","SendAlert","DEFAULTS",List.of(tparam4));

   JSONObject obj = buildJson("LABEL","Monitor status on " + getHostName(),
         "TRANSTIONS",List.of(trans1,trans2, trans3),
         "PARAMETERS",List.of(param0,param1));
   
   return obj;
}



/********************************************************************************/
/*                                                                              */
/*      Start method                                                            */
/*                                                                              */
/********************************************************************************/

@Override protected void start()
{
   super.start();
}



/********************************************************************************/
/*                                                                              */
/*      Command processing                                                      */
/*                                                                              */
/********************************************************************************/

@Override protected void processDeviceCommand(String name,JSONObject values)
{
   switch (name) {
      case "SendEmail" :
         sendEmail(values.optString("Subject"),values.optString("Body"));
         break;
      case "SendText" :
         sendText(values.optString("Message"));
         break;
      case "SendAlert" :
         sendAlert(values.optString("Message"));
    }
}


private boolean sendEmail(String subj,String body)
{
   if (subj == null && body == null) return false;
   String sendto = getDeviceParameter("email");
   if (sendto == null) return false;
   
   try {
      if (subj != null) subj = URLEncoder.encode(subj,"UTF-8");
    }
   catch (UnsupportedEncodingException e) { }
   try {
      if (body != null) body = URLEncoder.encode(body,"UTF-8");
    }
   catch (UnsupportedEncodingException e) { }
   
   String full = "mailto:" + sendto;
   String pfx = "?";
   try {
      if (subj != null) {
         full += pfx + "subject=" + subj;
         pfx = "&";
       }
      if (body != null) {
         full +=  pfx + "body=" + body;
         pfx = "&";
       }
      URI u = new URI(full);
      Desktop.getDesktop().mail(u);  
    }
   catch (Throwable e) {
      return false;
    }
   
   return true;
}


private boolean sendText(String msg)
{
   if (msg == null) return false;
   String num = getDeviceParameter("textNumber");
   String prov = getDeviceParameter("textProvider");
   if (num == null || prov == null) return false;
   
   if (num.length() > 10 && num.startsWith("1")) num = num.substring(1);
   
   prov = prov.toLowerCase();
   prov = prov.replace(" ","");
   prov = prov.replace("&","");
   prov = prov.replace(".","");
   prov = prov.replace("-","");
   
   String sfx = carrier_table.get(prov);
   if (sfx == null) return false;
   
   try {
      msg = URLEncoder.encode(msg,"UTF-8");
    }
   catch (UnsupportedEncodingException e) { }
   
   String full = "mailto:" + num + "@" + sfx + "?body=" +  msg;
   
   try {
      URI u = new URI(full);
      Desktop.getDesktop().mail(u);
    }
   catch (Throwable t) {
      return false;
    }
   
   return true;
}


private boolean sendAlert(String msg)
{
   if (msg == null || alert_command == null) return false;
   
   msg = msg.replace("\"","#");
   String cmd = alert_command.replace("$MSG",msg);
   
   try {
      runCommand(cmd);
    }
   catch (IOException e) {
      return false;
    }
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Check for changes                                                       */
/*                                                                              */
/********************************************************************************/

@Override protected void handlePoll()
{
   long now = System.currentTimeMillis();
   if (now - last_check < POLL_TIME) return;
   last_check = now;
   checkStatus();
}


private void checkStatus()
{
   long idle = getIdleTime();
   boolean zoom = usingZoom();
   boolean personal = inPersonalZoom(zoom);
   
   WorkOption presence = null;
   if (idle > 0) {
      if (idle < 300) {
         if (last_idle >= 300 || last_idle < 0) {
            presence = WorkOption.WORKING;
          }
       }
      else if (idle < 3600) {
         if (last_idle >= 3600 || last_idle < 300) {
            presence = WorkOption.IDLE;
          }
       }
      else if (last_idle < 3600) {
         presence = WorkOption.AWAY;
       }
      last_idle = idle;
    }
   
   ZoomOption zoomval = null;
   if (!zoom) zoomval = ZoomOption.NOT_ON_ZOOM;
   else if (!personal) zoomval = ZoomOption.ON_ZOOM;
   else zoomval = ZoomOption.PERSONAL_ZOOM;
   if (zoomval == last_zoom) zoomval = null;
   else last_zoom = zoomval;
   
   if (presence != null) {
      sendParameterEvent("Presence",presence);
    }
   
   if (zoomval != null) {
      sendParameterEvent("ZoomStatus",zoomval);
    }
}


private long getIdleTime()
{
   if (idle_command == null) return -1;
   
   try (BufferedReader br = runCommand(idle_command)) {
      for ( ; ; ) {
         String ln = br.readLine();
         if (ln == null) break;
         if (ln.contains("HIDIdleTime")) {
            int idx = ln.indexOf("=");
            if (idx < 0) continue;
            String nums = ln.substring(idx+1).trim();
            long lv = Long.parseLong(nums);
            lv /= 1000000000;
            return lv;
          }
       }
    }
   catch (IOException e) { }

   return -1;
}



private boolean usingZoom()
{
   if (zoom_command == null) return false;
   
   try (BufferedReader br = runCommand(zoom_command)) {
      for ( ; ; ) {
         String ln = br.readLine();
         if (ln == null) break;
         if (ln.contains("sh -c")) continue;
         if (ln.contains("zoom") && ln.contains("CptHost")) {
            return true;
          }
       }
      return false;
    }
   catch (IOException e) { }
   
   return false;
}


private boolean inPersonalZoom(boolean zoom)
{
   if (!zoom) return false;
   
   if (!zoom_docs.exists()) return false;
   
   File last = null;
   long lastdlm = 0;
   for (File f4 : zoom_docs.listFiles()) {
      if (f4.lastModified() > lastdlm) {
         last = f4;
         lastdlm = f4.lastModified();
       }
    }
   if (last == null) return false;
   
   if (last.getName().contains("Personal Meeting Room")) return true;
   
   return false;
}

}       // end of class DeviceComputerMonitor



/* end of DeviceComputerMonitor.java */

