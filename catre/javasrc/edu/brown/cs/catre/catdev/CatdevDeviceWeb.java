/********************************************************************************/
/*										*/
/*		CatdevDeviceWeb.java						*/
/*										*/
/*	Generic web-based sensor						*//*										  */
/********************************************************************************/
/*	Copyright 2023 Brown University -- Steven P. Reiss			*/
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/




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



}	// end of class CatdevDeviceWeb




/* end of CatdevDeviceWeb.java */

