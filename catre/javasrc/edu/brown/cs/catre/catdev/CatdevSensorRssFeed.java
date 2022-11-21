/********************************************************************************/
/*                                                                              */
/*              CatdevSensorRssFeed.java                                        */
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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.catre.catmodel.CatmodelCondition;
import edu.brown.cs.catre.catre.CatreCondition;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatrePropertySet;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;
import edu.brown.cs.catre.catre.CatreWorld;
import edu.brown.cs.ivy.xml.IvyXml;

public class CatdevSensorRssFeed extends CatdevSensorWeb
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private long		last_update;
private long		last_hash;
private RssCondition	trigger_condition;


private static DateFormat rss_date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ");




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected CatdevSensorRssFeed(CatreUniverse uu,String name,String url)
{
   super(uu,name,url,null,300000);
   
   initialize();
}


protected CatdevSensorRssFeed(CatreUniverse uu,CatreStore cs,Map<String,Object> map)
{
   super(uu);
   
   fromJson(cs,map);
   
   initialize();
}


private void initialize()
{
   last_update = System.currentTimeMillis();
   last_hash = 0;
   trigger_condition = new RssCondition();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public List<CatreCondition> getConditions()
{
   List<CatreCondition> rslt = new ArrayList<>();
   rslt.add(trigger_condition);
   return rslt;
}


@Override public boolean isDependentOn(CatreDevice d)
{
   return false;
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
   List<Element> trigs = new ArrayList<Element>();
   
   for (Element itm : IvyXml.children(xml,"item")) {
      String pdate = IvyXml.getTextElement(itm,"pubDate");
      long ndate = dateAfter(pdate);
      if (ndate <= 0) break;
      newdate = Math.max(newdate,ndate);
      trigs.add(itm);
    }
   
   if (trigs.isEmpty()) return;
   last_update = newdate;
   
   for (int i = trigs.size() - 1; i >= 0; --i) {
      Element itm = trigs.get(i);
      trigger_condition.trigger(itm);
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



/********************************************************************************/
/*                                                                              */
/*      Local condition                                                         */
/*                                                                              */
/********************************************************************************/


private class RssCondition extends CatmodelCondition {
   
   RssCondition() { 
      super(CatdevSensorRssFeed.this.getUniverse());
    }
   
   @Override public String getName() {
      return CatdevSensorRssFeed.this.getName() + "_TRIGGER";
    }
   
   @Override public void getSensors(Collection<CatreDevice> rslt) {
      rslt.add(CatdevSensorRssFeed.this);
    }
   
   @Override public String getDescription() {
      return "New Item for " + CatdevSensorRssFeed.this.getDescription();
    }
   
   @Override public void setTime(CatreWorld w)			{ }
   
   @Override public boolean isBaseCondition()                   { return false; }
   
   @Override public void addImpliedProperties(CatrePropertySet ups)     { }
   
   @Override public boolean isConsistentWith(CatreCondition c)  { return true; }
   
   @Override public boolean isTrigger() 	   { return true; }
   
   void trigger(Element itm) {
      CatreWorld w = for_universe.getCurrentWorld();
      CatrePropertySet props = for_universe.createPropertySet();
      props.put("ITEM",IvyXml.getTextElement(itm,"title"));
      props.put("DESC",IvyXml.getTextElement(itm,"description"));
      String lnk = IvyXml.getTextElement(itm,"link");
      if (lnk != null) props.put("LINK",lnk);
      fireTrigger(w,props);
    }
   
}	// end of inner class RssCondition


}       // end of class CatdevSensorRssFeed




/* end of CatdevSensorRssFeed.java */

