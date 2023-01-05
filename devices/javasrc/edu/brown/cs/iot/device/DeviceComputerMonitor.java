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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

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


private final String IDLE_COMMAND = "ioreg -c IOHIDSystem | fgrep HIDIdleTime";

private final String ZOOM_COMMAND = "ps -ax | fgrep zoom | fgrep CptHost";

private final long POLL_TIME = 30000;



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

   JSONObject obj = buildJson("LABEL","Monitor status on " + getHostName(),
         "TRANSTIONS",List.of(trans1,trans2),
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
    }
}


private void sendEmail(String subj,String body)
{
   if (subj == null || body == null) return;
   String sendto = getDeviceParameter("email");
   if (sendto == null) return;
   
}


private void sendText(String msg)
{
   if (msg == null) return;
   String num = getDeviceParameter("textNumber");
   String prov = getDeviceParameter("textProvider");
   if (num == null || prov == null) return;
   
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
   try (BufferedReader br = runCommand(IDLE_COMMAND)) {
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
   try (BufferedReader br = runCommand(ZOOM_COMMAND)) {
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
   
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,"Documents");
   File f3 = new File(f2,"Zoom");
   if (!f3.exists()) return false;
   
   File last = null;
   long lastdlm = 0;
   for (File f4 : f3.listFiles()) {
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

