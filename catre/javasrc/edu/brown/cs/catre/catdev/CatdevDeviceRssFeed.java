/********************************************************************************/
/*                                                                              */
/*              CatdevDeviceRssFeed.java                                        */
/*                                                                              */
/*      Device to trigger on new rss feed entries                               */
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreWorld;
import edu.brown.cs.ivy.xml.IvyXml;

public abstract class CatdevDeviceRssFeed extends CatdevDeviceWeb
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private long		last_update;
private long		last_hash;
private CatreParameter  title_parameter;
private CatreParameter  description_parameter;
private CatreParameter  link_parameter;

private static DateFormat rss_date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ");

private static long UPDATE_RATE = 5 * T_MINUTE;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatdevDeviceRssFeed(CatreUniverse uu,String name)
{
   super(uu,UPDATE_RATE);
   
   setName(name);
   
   initialize();
}


protected CatdevDeviceRssFeed(CatreUniverse uu,CatreStore cs,Map<String,Object> map)
{
   super(uu,UPDATE_RATE);
   
   fromJson(cs,map);
   
   initialize();
}


private void initialize()
{
   last_update = System.currentTimeMillis();
   last_hash = 0;
   
   CatreParameter bp0 = getUniverse().createStringParameter("last_title");
   CatreParameter bp1 = getUniverse().createStringParameter("last_description");
   title_parameter = addParameter(bp0);
   description_parameter = addParameter(bp1);
}



/********************************************************************************/
/*										*/
/*	RSS Processing								*/
/*										*/
/********************************************************************************/

@Override protected void handleContents(String cnts)
{
   int hash = cnts.hashCode();
   if (hash == last_hash) return;
   last_hash = hash;
   
   Element xml = IvyXml.convertStringToXml(cnts);
   if (xml == null) return;
   String lbd = IvyXml.getTextElement(xml,"lastBuildDate");
   if (dateAfter(lbd) <= 0) return;
   
   long newdate = 0;
   List<Element> trigs = new ArrayList<>();
   
   for (Element itm : IvyXml.children(xml,"item")) {
      String pdate = IvyXml.getTextElement(itm,"pubDate");
      long ndate = dateAfter(pdate);
      if (ndate <= 0) break;
      newdate = Math.max(newdate,ndate);
      trigs.add(itm);
    }
   
   if (trigs.isEmpty()) return;
   last_update = newdate;
   
   CatreWorld cw = getUniverse().getCurrentWorld();
   for (int i = trigs.size() - 1; i >= 0; --i) {
      Element itm = trigs.get(i);
      setValueInWorld(title_parameter,IvyXml.getTextElement(itm,"title"),cw);
      setValueInWorld(description_parameter,IvyXml.getTextElement(itm,"description"),cw); 
      setValueInWorld(link_parameter,
            IvyXml.getTextElement(itm,"link"),cw);
      fireChanged(cw,title_parameter);
    }
}



private long dateAfter(String lbd)
{
   if (lbd == null) return 0;
   
   try {
      Date d = rss_date.parse(lbd);
      long t = d.getTime();
      if (t > last_update) return t;
    }
   catch (ParseException e) {
      CatreLog.logE("CATDEV","Bad Date in RSS feed: " + lbd);
    }
   
   return 0;
}




/********************************************************************************/
/*                                                                              */
/*      I/O methods                                                             */
/*                                                                              */
/********************************************************************************/


@Override public Map<String,Object> toJson()
{
   Map<String,Object> rslt = super.toJson();
   return rslt;
}


@Override public void fromJson(CatreStore cs,Map<String,Object> map)
{
   super.fromJson(cs,map);
}


}       // end of class CatdevDeviceRssFeed




/* end of CatdevDeviceRssFeed.java */

