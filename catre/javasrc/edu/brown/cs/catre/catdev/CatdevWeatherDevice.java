/********************************************************************************/
/*                                                                              */
/*              CatdevWeatherDevice.java                                        */
/*                                                                              */
/*      Weather (temp and condition) sensor using web access                    */
/*                                                                              */
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.ivy.file.IvyFile;

class CatdevWeatherDevice extends CatdevDeviceWeb
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  city_name;
private String  unit_type;

private static String  api_key = null;

private static final String weather_url =
   "https://api.opeopenwenweathermap.org/data/2.5/weather?$(WHERE)&APPID=$(APPID)&units=$(UNITS)";

private static long UPDATE_RATE = 10 * T_MINUTE;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public CatdevWeatherDevice(CatreUniverse uu,CatreStore cs,Map<String,Object> map)
{
   super(uu,UPDATE_RATE);
   
   city_name = null;
   unit_type = "imperial";
   
   setupKeys();
   
   fromJson(cs,map);
   
   initialize();
}



private void initialize()
{
   String did = "Weather_" + city_name + "_" + unit_type;
   setDeviceId(did);
   
   if (getName() == null) {
      setName("Weather-" + city_name);
      setLabel("Weather for " + city_name);
    }
   
   CatreParameter pp = for_universe.createRealParameter("Temperature",-100,160);
   pp.setIsSensor(true);
   addParameter(pp);
   CatreParameter pp1 = for_universe.createStringParameter("WeatherCondition");
   pp1.setIsSensor(true);
   addParameter(pp1);
   // add other parameters as well
}


@Override public boolean validateDevice()
{
   if (city_name == null || unit_type == null) return false;
   if (api_key == null || api_key.startsWith("00000000")) return false;
   
   return super.validateDevice();
}



private void setupKeys()
{
   if (api_key != null) return;
   
   api_key = "00000000000";
   try {
      CatreController cc = getCatre();
      File f1 = cc.findBaseDirectory();
      File f2 = new File(f1,"secret");
      File f3 = new File(f2,"openweather");
      String jsonkey = IvyFile.loadFile(f3);
      JSONObject json = new JSONObject(jsonkey);
      api_key = json.getString("apikey");
    }
   catch (IOException e) {
       CatreLog.logE("Problem getting weather key",e);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected String getUrl()
{
   String orig = weather_url;
   
   Map<String,String> zmap = new HashMap<>();
   
   String where = null;
   if (Character.isDigit(city_name.charAt(0))) {
      where = "zip=" + city_name;
    }
   else {
      where = "q=" + city_name; 
    }
   
   zmap.put("WHERE",where);
   zmap.put("APPID",api_key);
   zmap.put("UNITS",unit_type);
   
   String url = IvyFile.expandText(orig,zmap);
   
   return url;
}


@Override protected void handleContents(String cnts)
{
   JSONObject json = new JSONObject(cnts);
   JSONObject current = json.getJSONObject("main");
   String temp = current.getString("temp");
   JSONObject weather = json.getJSONArray("weather").getJSONObject(0);
   String cond = weather.getString("main");
   
   CatreParameter p0 = findParameter("Temperature");
   setParameterValue(p0,temp);
   CatreParameter p1 = findParameter("WeatherCondition");
   setParameterValue(p1,cond);
}


/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   
   rslt.put("VTYPE","Weather");
   
   rslt.put("CITY",city_name);
   rslt.put("UNITS",unit_type);
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
   
   city_name = getSavedString(map,"CITY",city_name);
   unit_type = getSavedString(map,"UNITS",unit_type);
}



}       // end of class CatdevWeatherDevice




/* end of CatdevWeatherDevice.java */

