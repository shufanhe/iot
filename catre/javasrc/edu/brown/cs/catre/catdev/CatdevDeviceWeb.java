/********************************************************************************/
/*                                                                              */
/*              CatdevDeviceWeb.java                                            */
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

public abstract class CatdevDeviceWeb extends CatdevDevice 
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private TimerTask timer_task;

private long	poll_rate;
private long	cache_rate;

private static CatdevWebCache web_cache = new CatdevWebCache();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatdevDeviceWeb(CatreUniverse uu,long rate) 
{
   super(uu);
   poll_rate = rate;
   cache_rate = rate;
   timer_task = null;
}



@Override public boolean validateDevice()
{
   if (poll_rate <= 0 || cache_rate <= 0) return false;
   
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


protected void setPollRate(long rate)
{
   poll_rate = rate;
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
   rslt.put("POLLRATE",poll_rate); 
   rslt.put("CACHERATE",cache_rate);
   
   return rslt;
}



@Override public void fromJson(CatreStore cs,Map<String,Object> map) 
{
   super.fromJson(cs,map);
   
   setPolling(0);
   
   poll_rate = getSavedLong(map,"POLLRATE",600000);
   cache_rate = getSavedLong(map,"CACHERATE",cache_rate);
}




/********************************************************************************/
/*										*/
/*	Polling methods 							*/
/*										*/
/********************************************************************************/

@Override protected void localStartDevice()
{
   setPolling(poll_rate);
}

@Override protected void localStopDevice()
{
   setPolling(0);
}


public synchronized void setPolling(long time)
{
   if (poll_rate == time && timer_task != null) return;
   
   if (timer_task != null) timer_task.cancel();
   timer_task = null;
   
   poll_rate = time;
   if (time == 0) return;
   
   timer_task = new Updater();
   getCatre().schedule(timer_task,0,poll_rate);
}


private class Updater extends TimerTask {
   
   @Override public void run() {
      updateCurrentState();
    }

}	// end of inner class Updater



}       // end of class CatdevDeviceWeb




/* end of CatdevDeviceWeb.java */

