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

import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;

public abstract class CatdevSensorWeb extends CatdevDevice 
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

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

protected CatdevSensorWeb(CatreUniverse uu,String name,long time)
{
   super(uu);
   start_rate = time;
   poll_rate = 0;
   cache_rate = CACHE_RATE;
   timer_task = null;
   setName(name);
}


protected CatdevSensorWeb(CatreUniverse uu) 
{
   super(uu);
   start_rate = 0;
   poll_rate = 0;
   cache_rate = CACHE_RATE;
   timer_task = null;
}



@Override public boolean validateDevice()
{
   if (start_rate <= 0 || cache_rate <= 0) return false;
   
   return super.validateDevice();
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



/********************************************************************************/
/*										*/
/*	Checking methods							*/
/*										*/
/********************************************************************************/

@Override protected void checkCurrentState()		{ }

@Override protected  void updateCurrentState()
{
   String cnts = null;
   cnts = web_cache.getContents(getUrl(),cache_rate);
   handleContents(cnts);
}


abstract protected String getUrl();


abstract protected void handleContents(String cnts);



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
   
   return rslt;
}



@Override public void fromJson(CatreStore cs,Map<String,Object> map) 
{
   super.fromJson(cs,map);
   
   start_rate = getSavedLong(map,"POLLRATE",600000);
   cache_rate = getSavedLong(map,"CACHERATE",CACHE_RATE);
   poll_rate = 0;
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

