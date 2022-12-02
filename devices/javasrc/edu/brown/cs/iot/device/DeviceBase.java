/********************************************************************************/
/*                                                                              */
/*              DeviceBase.java                                                 */
/*                                                                              */
/*      Base class handling device communications                               */
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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONArray;
import org.json.JSONObject;

public abstract class DeviceBase implements DeviceConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  user_id;
private String  personal_token;
private String  access_token;
protected String device_uid;
private Object ping_lock;


private static Random rand_gen = new Random();
private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected DeviceBase()
{ 
   ping_lock = new Object();
   
   try {
      setupAccess();
    }
   catch (IOException e) {
      System.err.println("DEVICE: Problem getting/setting up access codes");
      System.exit(1);
    }
   
   device_uid = null;
}


protected void start()
{
   // lock to ensure exclusivity
   File p0 = new File(System.getProperty("user.home"));
   File p1 = new File(p0,LOCK_DIR);   
   p1.mkdir();
   String dnm = getDeviceName();
   dnm = dnm.replace(" ","_");
   File p2 = new File(p1, dnm);
   FileLocker locker = new FileLocker(p2);
   if (!locker.tryLock()) {
      System.exit(0);
    }
   
   // compute unique id if needed
   getUniqueId();
   
   setupPing();
   
   authenticate();
}



/********************************************************************************/
/*                                                                              */
/*      Abstract methods for device                                             */
/*                                                                              */
/********************************************************************************/

protected String getUniqueId()
{
   if (device_uid != null) return device_uid;
   
   String dnm = getDeviceName();
   dnm = dnm.replace(" ","_");
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,".config");
   File f3 = new File(f2,CONFIG_DIR);
   File f4 = new File(f3,NAME_FILE + dnm);
   if (!f4.exists()) {
      device_uid = dnm + "-" + randomString(16);
      JSONObject obj = new JSONObject();
      obj.put(DEVICE_UID,device_uid);
      try (FileWriter fw = new FileWriter(f4)) {
         fw.write(obj.toString(2));
       }
      catch (IOException e) { }
    }
   else {
      try (FileReader fr = new FileReader(f4)) {
         String cnts = loadFile(fr);
         JSONObject obj = new JSONObject(cnts);
         device_uid = obj.getString(DEVICE_UID);
       }
      catch (IOException e) { }
    }
   
   return device_uid;
}


protected abstract String getDeviceName();
protected abstract JSONObject getDeviceJson();

protected void handleCommand(JSONObject cmd)            { }
protected void handlePoll()                             { }



/********************************************************************************/
/*                                                                              */
/*      Authentication                                                          */
/*                                                                              */
/********************************************************************************/

private void setupAccess() throws IOException
{
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,".config");
   File f3 = new File(f2,CONFIG_DIR);
   File f4 = new File(f3,CONFIG_FILE);
   if (!f4.exists()) {
      user_id = randomString(12);
      personal_token = randomString(16);
      JSONObject obj = new JSONObject();
      obj.put(CONFIG_UID,user_id);
      obj.put(CONFIG_PAT,personal_token);
      try (FileWriter fw = new FileWriter(f4)) {
         fw.write(obj.toString(2));
       };
    }
   else {
      try (FileReader fr = new FileReader(f4)) {
         String cnts = loadFile(fr);
         JSONObject obj = new JSONObject(cnts);
         user_id = obj.getString(CONFIG_UID);
         personal_token = obj.getString(CONFIG_PAT);
       };
    }
   
   access_token = null;
}



protected boolean authenticate()
{
   synchronized (ping_lock) {
      JSONObject rslt = sendToCedes("attach","uid",user_id);
      String seed = rslt.optString("seed",null);
      if (seed == null) return false;
      
      String p0 = secureHash(personal_token);
      String p1 = secureHash(p0 + user_id);
      String p2 = secureHash(p1 + seed);
      
      JSONObject rslt1 = sendToCedes("authorize","uid",user_id,
            "patencoded",p2);
      String tok = rslt1.optString("token",null);
      if (tok == null) return false;
      
      access_token = tok;
    }

   return true;
}



/********************************************************************************/
/*                                                                              */
/*      PING logic                                                              */
/*                                                                              */
/********************************************************************************/

private void setupPing()
{
   Timer timer = new Timer();
   PingTask task = new PingTask();
   timer.schedule(task,PING_TIME,PING_TIME);
}



private class PingTask extends TimerTask {
   
   private long last_time;
   
   PingTask() {
      last_time = 0;
    }
   
   @Override public void run() {  
      synchronized (ping_lock) {
         if (access_token == null) {
            if (last_time > 0 && System.currentTimeMillis() - last_time > ACCESS_TIME) {
               authenticate();
             }
          }
         else {
            JSONObject obj = sendToCedes("ping","uid",user_id);
            String sts = obj.optString("status","FAIL");
            switch (sts) {
               case "DEVICES" :
                  sendDeviceInfo();
                  break;
               case "COMMAND" :
                  JSONObject cmd = obj.getJSONObject("command");
                  handleCommand(cmd);
               case "OK" :
                  break;
               default :
                  access_token = null;
                  break;
             }
          }
         last_time = System.currentTimeMillis();
         handlePoll();
       }
    }
   
}



protected void sendDeviceInfo()
{
   JSONObject dev = getDeviceJson();
   dev.put("UID",getUniqueId());
   dev.put("NAME",getDeviceName());
   dev.put("BRIDGE","generic");

   JSONArray jarr = new JSONArray();
   jarr.put(dev);
   
   sendToCedes("devices","devices",jarr);
}



/********************************************************************************/
/*                                                                              */
/*      Event logic                                                             */
/*                                                                              */
/********************************************************************************/

protected void sendParameterEvent(String param,Object val)
{ 
   JSONObject evt = buildJson("DEVICE",getUniqueId(),"TYPE","PARAMETER",
         "PARAMETER",param,
         "VALUE",val);
   sendToCedes("event","event",evt);
}


      

/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

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




static public String secureHash(String s)
{
   try {
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      byte [] dvl = md.digest(s.getBytes());
      String rslt = Base64.getEncoder().encodeToString(dvl);
      return rslt;
    }
   catch (Exception e) {
      throw new Error("Problem with sha-512 encoding of " + s);
    }
}



public static String loadFile(Reader fr) throws IOException
{
   StringBuffer buf = new StringBuffer();
   
   char [] cbuf = new char[16384];
   for ( ; ; ) {
      int ln = fr.read(cbuf);
      if (ln <= 0) break;
      buf.append(cbuf,0,ln);
    }
   
   return buf.toString();
}


public static String loadFile(InputStream ins) throws IOException
{
   InputStreamReader isr = new InputStreamReader(ins);
   return loadFile(isr);
}




protected JSONObject sendToCedes(String nm,Object ... args)
{
   JSONObject obj = buildJson(args);
   
   return sendToCedes(nm,obj);
}


protected JSONObject sendToCedes(String nm,JSONArray arr)
{
   return sendToCedes(nm,arr.toString(2));
}


protected JSONObject sendToCedes(String nm,JSONObject obj)
{
   return sendToCedes(nm,obj.toString(2));
}


protected JSONObject sendToCedes(String nm,String cnts)
{
   try {
      String url = BASE_URL + nm;
      URL u = new URL(url);
      HttpURLConnection hc = (HttpURLConnection) u.openConnection();
      hc.setUseCaches(false);
      hc.addRequestProperty("content-type","application/json");
      hc.addRequestProperty("accept","application/json");
      hc.setRequestMethod("POST");
      if (access_token != null) {
         hc.addRequestProperty("Authorization","Bearer " + access_token);
       }
      hc.setDoOutput(true);
      hc.setDoInput(true);
      
      hc.connect();
      
      OutputStream ots = hc.getOutputStream();
      ots.write(cnts.getBytes());
      
      InputStream ins = hc.getInputStream();
      String rslts = loadFile(ins);
      return new JSONObject(rslts);
    }
   catch (IOException e) {
      // report error?
    }

   return null;
}



protected String getHostName()
{
   String h = System.getenv("HOST");

   if (h == null) {  
      try {
         InetAddress lh = InetAddress.getLocalHost();
         h = lh.getCanonicalHostName();
       }
      catch (IOException e ) { }
    }
   
   if (h == null) h = "localhost";
   
   if (h.endsWith(".local")) {
      int idx = h.lastIndexOf(".");
      h = h.substring(0,idx);
    }
      
   return h;
}


protected JSONObject buildJson(Object ... args)
{
   JSONObject rslt = new JSONObject();
   for (int i = 0; i < args.length-1; i += 2) {
      String key = args[i].toString();
      Object val = args[i+1];
      rslt.put(key,val);
    }
   
   return rslt;
}


protected BufferedReader runCommand(String cmd) throws IOException
{
   String [] args = new String [] { "sh","-c",cmd };
   
   Process proc = Runtime.getRuntime().exec(args,null);
   
   proc.getOutputStream().close();
   
   InputStream ins = proc.getInputStream();
   InputStreamReader isr = new InputStreamReader(ins);
   BufferedReader br = new BufferedReader(isr);
   return br;
}


/********************************************************************************/
/*                                                                              */
/*      File lock to ensure only one copy running                               */
/*                                                                              */
/********************************************************************************/


private static class FileLocker
{

   private FileOutputStream lock_file;
   private FileLock file_lock;

   FileLocker(File f) {
      lock_file = null;
      file_lock = null;
      
      if (f.isDirectory()) f = new File(f,".lock");
      else if (!f.getName().endsWith(".lock")) f = new File(f.getPath() + ".lock");
      
      f.setWritable(true,false);
      
      try {
         lock_file = new FileOutputStream(f);
         f.setWritable(true,false);
       }
      catch (IOException e) { }
    }
   
   boolean tryLock() {
      if (lock_file == null) return false;
      if (file_lock != null) return true;          // assumes only one lock per process
      
      try {
         file_lock = lock_file.getChannel().tryLock();
         if (file_lock != null) return true;
       }
      catch (IOException e) { }
      
      return false;
    }

}       // end of class FileLocker







}       // end of class DeviceBase




/* end of DeviceBase.java */

