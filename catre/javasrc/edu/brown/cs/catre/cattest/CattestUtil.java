/********************************************************************************/
/*                                                                              */
/*              CattestUtil.java                                                */
/*                                                                              */
/*      Utility methods for testing                                             */
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



package edu.brown.cs.catre.cattest;

import edu.brown.cs.catre.catmain.CatmainMain;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.ivy.file.IvyFile;

import org.json.JSONObject;import java.util.Map;


import java.util.HashMap;
import java.nio.charset.Charset;
import java.net.URLEncoder;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.InputStream;

class CattestUtil implements CattestConstants
{



/********************************************************************************/
/*                                                                              */
/*      Private storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final Charset UTF8 = Charset.forName("UTF-8");



/********************************************************************************/
/*                                                                              */
/*      Send a message and get JSON response                                    */
/*                                                                              */
/********************************************************************************/

static JSONObject sendGet(String file,Object ... val)
{
   Map<String,Object> map = new HashMap<>();
   if (val.length > 1) {
      for (int i = 0; i+1 < val.length; i += 2) {
         String key = val[i].toString();
         Object v = val[i+1];
         map.put(key,v);
       }
    }
   
   return sendGet(file,map);
}


static JSONObject sendJson(String method,String file,Object ... val)
{
   Map<String,Object> map = new HashMap<>();
   if (val.length > 1) {
      for (int i = 0; i+1 < val.length; i += 2) {
         String key = val[i].toString();
         Object v = val[i+1];
         map.put(key,v);
       }
    }
   
   return sendJson(method,file,new JSONObject(map));
}


static JSONObject sendGet(String file,Map<String,Object> map) 
{
   StringBuffer buf = new StringBuffer();
   buf.append(TEST_HOST);
   buf.append(file);
   
   String sep = "?";
   for (Map.Entry<String,Object> ent : map.entrySet()) {
      buf.append(sep);
      sep = "&";
      buf.append(ent.getKey());
      buf.append("=");
      buf.append(URLEncoder.encode(ent.getValue().toString(),UTF8));
    }
   
   return send("GET",buf.toString(),null);
}


static JSONObject sendJson(String method,String file,JSONObject jo)
{
   String url = TEST_HOST + file;
   String body = jo.toString(2);
   
   return send(method,url,body);
}



private static JSONObject send(String method,String url,String body)
{
   try {
      URL u = new URL(url);
      HttpURLConnection uc = (HttpURLConnection) u.openConnection();
      uc.addRequestProperty("content-type","application/json");
      uc.addRequestProperty("accept","application/json");
      uc.setDoInput(true);
      if (body != null) {
         uc.setDoOutput(true);
         OutputStream ots = uc.getOutputStream();
         ots.write(body.getBytes());
       }
      InputStream ins = uc.getInputStream();
      String rslts = IvyFile.loadFile(ins);
      return new JSONObject(rslts);
    }
   catch (Exception e) {
       throw new Error("Problem sending message " + method + " " + url + " " + body,e);  
    }
}






/********************************************************************************/
/*                                                                              */
/*      Methods to start web server in CATRE                                    */
/*                                                                              */
/********************************************************************************/

static void startCatre()
{
   CatreRunner cr = new CatreRunner();
   
   cr.start();
   
   for (int i = 0; i < 100; ++i) {
      try {
         JSONObject rslt = sendGet("/ping");
         if (rslt != null) break;
       }
      catch (Throwable t) { 
         CatreLog.logD("CATTEST","Wait for web server");
       }
      try {
         Thread.sleep(1000);
       }
      catch (InterruptedException e) { };
    }
}



private static class CatreRunner extends Thread {
   
   CatreRunner() {
      super("CatreRunnerThread");
    }
   
   @Override public void run() {
      CatmainMain.main(new String[] { });
    }
   
}       // end of inner class CatreRunner

}       // end of class CattestUtil




/* end of CattestUtil.java */

