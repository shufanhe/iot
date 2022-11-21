/********************************************************************************/
/*                                                                              */
/*              CatdevSensorWeb.java                                            */
/*                                                                              */
/*      Generic web-based sensor                                                *//*                                                                              */
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

import java.util.Map;
import java.util.TimerTask;

import org.jsoup.Jsoup;

import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreWorld;

public abstract class CatdevSensorWeb extends CatdevDevice
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	access_url;
private String	data_selector;
private String  device_name;

private TimerTask timer_task;

private long	start_rate;
private long	poll_rate;
private long	cache_rate;

private static CatdevWebCache web_cache = new CatdevWebCache();

private static long CACHE_RATE = 1*T_MINUTE;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatdevSensorWeb(CatreUniverse uu,String name,String url,String sel,long time)
{
   super(uu);
   start_rate = time;
   poll_rate = 0;
   cache_rate = CACHE_RATE;
   access_url = url;
   data_selector = sel;
   timer_task = null;
   device_name = name;
}


protected CatdevSensorWeb(CatreUniverse uu) 
{
   super(uu);
   start_rate = 0;
   poll_rate = 0;
   cache_rate = CACHE_RATE;
   access_url = null;
   data_selector = null;
   timer_task = null;
   device_name = null;
}





/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

protected void setCacheRate(long rate)
{
   cache_rate = rate;
}



protected void setAccess(String url,String pat)
{
   access_url = url;
   data_selector = pat;
}



/********************************************************************************/
/*										*/
/*	Checking methods							*/
/*										*/
/********************************************************************************/

@Override protected void checkCurrentState()		{ }


@Override protected void updateCurrentState()
{
   String cnts = null;
   String exp = expandUrl(access_url);
   
   cnts = web_cache.getContents(exp,cache_rate);
   
   handleContents(cnts);
}


protected String expandUrl(String orig) 		{ return orig; }

@Override public String getName()                       { return device_name; }
@Override public String getDescription() {
   return "Web sensor for " + access_url;
}


protected void handleContents(String cnts)
{
   if (cnts == null) return;
   
   CatreParameter param = null;
   for (CatreParameter up : getParameters()) {
      if (up.isSensor()) {
	 param = up;
	 break;
       }
    }
   
   String rslt = decodeWebResponse(cnts);
   
   if (rslt != null) {
      CatreWorld cw = getCurrentWorld();
      setValueInWorld(param,rslt,cw);
    }
}


protected String decodeWebResponse(String cnts)
{
   try {
      org.jsoup.nodes.Element doc = Jsoup.parse(cnts);
      org.jsoup.select.Elements elts = doc.select(data_selector);
      String rslt = null;
      if (elts.size() > 0) {
	 rslt = elts.get(0).text();
       }
      
      return rslt;
    }
   catch (Throwable t) {
      CatreLog.logE("CATDEV","Problem parsing web data: " + cnts,t);
    }
   
   return null;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   long rate = poll_rate;
   if (poll_rate == 0) rate = start_rate;
   rslt.put("POLLRATE",rate);
   if (cache_rate != rate && cache_rate != 0) rslt.put("CACHERATE",cache_rate);
   rslt.put("ACCESSURL",access_url);
   rslt.put("SELECTOR",data_selector);
   
   return rslt;
}



@Override public void fromJson(CatreStore cs,Map<String,Object> map) 
{
   super.fromJson(cs,map);
   
   device_name = getSavedString(map,"NAME",null);
   
   start_rate = getSavedLong(map,"POLLRATE",0);
   cache_rate = getSavedLong(map,"CACHERATE",CACHE_RATE);
   poll_rate = 0;
   access_url = getSavedString(map,"ACCESSURL",null);
   data_selector = getSavedString(map,"SELECTOR",null);
}




/********************************************************************************/
/*										*/
/*	Polling methods 							*/
/*										*/
/********************************************************************************/

@Override protected void localStartDevice()
{
   if (start_rate != 0) {
      long r = start_rate;
      start_rate = 0;
      setPolling(r);
    }
}


public void setPolling(long time)
{
   if (poll_rate == time) return;
   
   if (timer_task != null) timer_task.cancel();
   timer_task = null;
   
   poll_rate = time;
   if (time == 0) return;
   
   TimerTask tt = new Updater();
   getCatre().schedule(tt,0,poll_rate);
}


private class Updater extends TimerTask {
   
   @Override public void run() {
      updateCurrentState();
    }

}	// end of inner class Updater



}       // end of class CatdevSensorWeb




/* end of CatdevSensorWeb.java */

