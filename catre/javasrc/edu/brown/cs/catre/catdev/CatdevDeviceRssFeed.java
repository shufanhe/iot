/********************************************************************************/
/*										*/
/*		CatdevDeviceRssFeed.java					*/
/*										*/
/*	Device to trigger on new rss feed entries				*/
/*										*/
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
private CatreParameter	title_parameter;
private CatreParameter	description_parameter;
private CatreParameter	link_parameter;

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

   for (int i = trigs.size() - 1; i >= 0; --i) {
      Element itm = trigs.get(i);
      setParameterValue(title_parameter,IvyXml.getTextElement(itm,"title"));
      setParameterValue(description_parameter,IvyXml.getTextElement(itm,"description"));
      setParameterValue(link_parameter,
	    IvyXml.getTextElement(itm,"link"));
      fireChanged(title_parameter);
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
/*										*/
/*	I/O methods								*/
/*										*/
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


}	// end of class CatdevDeviceRssFeed




/* end of CatdevDeviceRssFeed.java */

