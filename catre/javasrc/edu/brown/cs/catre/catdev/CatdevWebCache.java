/********************************************************************************/
/*                                                                              */
/*              CatdevWebCache.java                                             */
/*                                                                              */
/*      Cache and access web pages                                              */
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



package edu.brown.cs.catre.catdev;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class CatdevWebCache implements CatdevConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,CacheItem> cache_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatdevWebCache()
{
   cache_map = new ConcurrentHashMap<>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getContents(String url,long delay)
{
   long now = System.currentTimeMillis();
   
   CacheItem ci = cache_map.get(url);
   if (ci != null) {
      if (ci.getEndTime() > now) return ci.getText();
    }
   
   String cnts = null;
   try {
      URL u = new URL(url);
      HttpURLConnection hc = (HttpURLConnection) u.openConnection();
      hc.setReadTimeout(60000);
      hc.setConnectTimeout(60000);
      hc.setUseCaches(false);
      hc.setInstanceFollowRedirects(true);
      if (ci != null) hc.setIfModifiedSince(ci.getLastModified());
      hc.connect();
      if (ci != null && hc.getResponseCode() == 304) {
	 return ci.getText();
       }
      else if (ci != null) cache_map.remove(url);
      
      Reader ins = new InputStreamReader(hc.getInputStream());
      StringBuffer prslt = new StringBuffer();
      char [] buf = new char[8192];
      for ( ; ; ) {
	 int ln = ins.read(buf);
	 if (ln <= 0) break;
	 prslt.append(buf,0,ln);
       }
      ins.close();
      cnts = prslt.toString();
    }
   catch (IOException e) { }
   
   if (cnts != null &&delay > 5000) {	
      ci = new CacheItem(now,now+delay-5000,cnts);
      cache_map.put(url,ci);
    }
   
   return cnts;
}



/********************************************************************************/
/*										*/
/*	Cache for web pages							*/
/*										*/
/********************************************************************************/

private static class CacheItem {
   
   private long time_out;
   private String result_text;
   private long last_modified;
   
   CacheItem(long mod,long timeout,String text) {
      last_modified = mod;
      time_out = Math.max(timeout,0);
      result_text = text;
    }
   
   long getEndTime()			{ return time_out; }
   long getLastModified()		{ return last_modified; }
   String getText()			{ return result_text; }
   
}	// end of inner class CacheItem




}       // end of class CatdevWebCache




/* end of CatdevWebCache.java */

