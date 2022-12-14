/********************************************************************************/
/*                                                                              */
/*              CatprogGoogleCalendar.java                                      */
/*                                                                              */
/*      Interface to GOOGLE calendar through their RESTful API                  */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2022 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2022, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.catre.catprog;



import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.Calendar.Events;
import com.google.api.services.calendar.model.*;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.util.DateTime;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.catre.catre.CatreCalendarEvent;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreWorld;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class CatprogGoogleCalendar implements CatprogConstants
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<String>				  cal_names;
private DateTime				  last_check;
private File					  cal_file;
private long					  cal_dlm;
private List<CalEvent>				  cal_events;

private static com.google.api.services.calendar.Calendar cal_service;

private static Map<CatreWorld,CatprogGoogleCalendar> the_calendars;

private static final String APPLICATION_NAME = "smartsign";
private static File DATA_STORE_DIR;
private static File DATA_STORE_CREDS;
private static HttpTransport HTTP_TRANSPORT;
private static FileDataStoreFactory DATA_STORE_FACTORY;
private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
private static final List<String> SCOPES = Arrays.asList(CalendarScopes.CALENDAR_READONLY);

static {
   the_calendars = new WeakHashMap<>();
   // TODO: load credentials from CatreUser
   try {
      DATA_STORE_DIR = IvyFile.expandFile("$(HOME)/.upod-calendar.json");
      DATA_STORE_CREDS = new File("/ws/volfred/smartsign/calendar-quickstart.json");
      HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
    }
   catch (Throwable t) {
      CatreLog.logE("CATMODEL","GOOGLECAL: Initialization problem with calendar api",t);
      t.printStackTrace();
      DATA_STORE_DIR = null;
      HTTP_TRANSPORT = null;
      DATA_STORE_FACTORY = null;
    }
}



/********************************************************************************/
/*										*/
/*	Static access methods							*/
/*										*/
/********************************************************************************/

static synchronized CatprogGoogleCalendar getCalendar(CatreWorld w)
{
   if (DATA_STORE_DIR == null) return null;
   if (cal_service == null) {
      try {
	 cal_service = getCalendarService();
       }
      catch (IOException e) {
	 CatreLog.logE("CATMODEL","GOOGLECAL: Authorization problem with calendar api: " + e);
	 DATA_STORE_DIR = null;
	 HTTP_TRANSPORT = null;
	 DATA_STORE_FACTORY = null;
	 return null;
       }
    }
   
   CatprogGoogleCalendar rslt = the_calendars.get(w);
   if (rslt == null) {
      rslt = new CatprogGoogleCalendar();
      the_calendars.put(w,rslt);
    }
   
   return rslt;
}

/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private CatprogGoogleCalendar()
{
   cal_file = IvyFile.expandFile("$(HOME)/.catredcal");
   cal_names = new ArrayList<>();
   cal_dlm = 0;
   last_check = null;
   cal_events = new ArrayList<>();
   
   loadCalendarData();
}



/********************************************************************************/
/*										*/
/*	Event checker								*/
/*										*/
/********************************************************************************/

Collection<CalendarEvent> getActiveEvents(long when)
{
   List<CalendarEvent> rslt = new ArrayList<CalendarEvent>();
   try {
      getAllEvents(when);
      rslt.addAll(cal_events);
    }
   catch (Exception e) {
      CatreLog.logE("CATMODEL","GOOGLECAL: problem getting events",e);
    }
   return rslt;
}



private void getAllEvents(long whent) throws Exception
{
   if (whent == 0) whent = System.currentTimeMillis();
   Calendar c1 = Calendar.getInstance();
   c1.setTimeInMillis(whent);
   Calendar c2 = CatreCalendarEvent.startOfDay(c1);
   DateTime dt1 = new DateTime(c2.getTimeInMillis());
   c1.setTimeInMillis(whent + 2*T_DAY);
   c2 = CatreCalendarEvent.startOfDay(c1);
   DateTime dt2 = new DateTime(c2.getTimeInMillis());
   
   if (dt1.equals(last_check)) return;
   cal_events.clear();
   
   for (String calname : cal_names) {
      Events.List list = cal_service.events().list(calname);
      list.setTimeMin(dt1);
      list.setTimeMax(dt2);
      list.setOrderBy("startTime");
      list.setSingleEvents(true);
      // list.setMaxResults(10);
      try {
	 List<Event> items = list.execute().getItems();
	 for (Event evt : items) {
	    CalEvent ce = new CalEvent(evt);
	    cal_events.add(ce);
	  }
       }
      catch (Exception e) {
         CatreLog.logE("CATMODEL","GOOGLECAL: problem getting events",e);
       }
    }
   
   last_check = dt1;
}



/********************************************************************************/
/*										*/
/*	Event finder								*/
/*										*/
/********************************************************************************/


boolean findEvent(long when,String desc,Map<String,String> fields)
{
   try {
      getAllEvents(when);
    }
   catch (Exception e) {
      CatreLog.logE("CATMODEL","Problem getting calendar events: " + e,e);
    }
   
   EventMatcher em = new EventMatcher(desc);	// might want to cache these
   for (CalEvent ce : cal_events) {
      // CatmodelLogger.logD("CALENDAR MATCH " + ce + " " + ce.isCurrent(when) + " " + em.match(ce));
      if (!ce.isCurrent(when)) continue;
      if (em.match(ce)) {
	 ce.getFields(fields);
	 return true;
       }
    }
   
   return false;
}





/********************************************************************************/
/*										*/
/*	Load data from calendar file						*/
/*										*/
/********************************************************************************/

private void loadCalendarData()
{
   if (!cal_file.exists()) {
      cal_dlm = 0;
      cal_names.clear();
      cal_names.add("primary");
    }
   else if (cal_file.lastModified() > cal_dlm) {
      cal_dlm = cal_file.lastModified();
      cal_names.clear();
      try (BufferedReader br = new BufferedReader(new FileReader(cal_file))) {
         for ( ; ; ) {
            String ln = br.readLine();
            if (ln == null) break;
            ln = ln.trim();
            if (ln.startsWith("#") || ln.length() == 0) continue;
            cal_names.add(ln);
          }
         if (cal_names.size() == 0) cal_names.add("primary");
       }
      catch (IOException e) {
         CatreLog.logE("CATPROG","Problem reading google calendar data",e);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Authorization code							*/
/*										*/
/********************************************************************************/

private static com.google.api.services.calendar.Calendar getCalendarService() throws IOException
{
   Credential cred = authorize();
   com.google.api.services.calendar.Calendar.Builder bldr =
      new com.google.api.services.calendar.Calendar.Builder(HTTP_TRANSPORT,JSON_FACTORY,cred);
   bldr.setApplicationName(APPLICATION_NAME);
   
   com.google.api.services.calendar.Calendar cal = bldr.build();
   
   return cal;
}



private static Credential authorize() throws IOException
{
   FileReader ins = new FileReader(DATA_STORE_CREDS);
   GoogleClientSecrets clisec = GoogleClientSecrets.load(JSON_FACTORY,ins);
   
   GoogleAuthorizationCodeFlow.Builder builder = new GoogleAuthorizationCodeFlow.Builder(
	 HTTP_TRANSPORT,JSON_FACTORY,clisec,SCOPES);
   builder.setDataStoreFactory(DATA_STORE_FACTORY);
   builder.setAccessType("offline");
   GoogleAuthorizationCodeFlow flow = builder.build();
   
   Credential cred = new AuthorizationCodeInstalledApp(flow,new LocalServerReceiver()).authorize("user");
   System.err.println("GOOGLECAL: Credentials saved to " + DATA_STORE_DIR.getPath());
   
   return cred;
}



/********************************************************************************/
/*										*/
/*	Represent an event							*/
/*										*/
/********************************************************************************/

private class CalEvent implements CalendarEvent {
   
   private long start_time;
   private long end_time;
   private Map<String,String> property_set;
   
   CalEvent(Event evt) {
      if (evt == null) return;
      if (evt.getStart() == null || evt.getStart().getDateTime() == null) start_time = 0;
      else start_time = evt.getStart().getDateTime().getValue();
      if (evt.getEnd() == null) end_time = 0;
      else if (evt.getEnd().getDateTime() == null) end_time = 0;
      else end_time = evt.getEnd().getDateTime().getValue();
      property_set = new HashMap<>();
      property_set.put("ID",evt.getICalUID());
      setProperty("STATUS",evt.getStatus(),true);
      setProperty("TRANS",evt.getTransparency(),true);
      setProperty("VISIBILITY",evt.getVisibility(),true);
      setProperty("CONTENT",evt.getDescription(),false);
      setProperty("WHERE",evt.getLocation(),false);
      
      setProperty("CALENDAR",evt.getOrganizer().getDisplayName(),false);
      
      StringBuffer buf = new StringBuffer();
      if (evt.getAttendees() != null) {
         for (EventAttendee attd : evt.getAttendees()) {
            if (buf.length() > 0) buf.append("\t");
            buf.append(attd.getDisplayName());
          }
         if (buf.length() > 0) property_set.put("WHO",buf.toString());
       }
      
      buf = new StringBuffer();
      if (evt.getHtmlLink() != null) buf.append(evt.getHtmlLink());
      if (evt.getAttachments() != null) {
         for (EventAttachment attc : evt.getAttachments()) {
            if (buf.length() > 0) buf.append("\t");
            buf.append(attc.getFileUrl());
          }
       }
      if (buf.length() > 0) property_set.put("LINKS",buf.toString());
      
      Calendar c0 = Calendar.getInstance();
      c0.setTimeInMillis(start_time);
      Calendar c1 = Calendar.getInstance();
      c1.setTimeInMillis(end_time);
      Calendar c2 = CatreCalendarEvent.startOfDay(c0);
      Calendar c3 = CatreCalendarEvent.startOfDay(c1);
      if (c0.equals(c2) && c1.equals(c3)) {
         property_set.put("ALLDAY","true");
       }
    }
   
   @Override public long getStartTime() 	{ return start_time; }
   @Override public long getEndTime()		{ return end_time; }
   @Override public Map<String,String> getProperties() {
      return new HashMap<String,String>(property_set);
    }
   
   String getProperty(String key)	{ return property_set.get(key.toUpperCase()); }
   
   boolean isCurrent(long now) {
      if (now == 0) now = System.currentTimeMillis();
      return now >= start_time && now < end_time;
    }
   
   void getFields(Map<String,String> fields) {
      String v = property_set.get("WHERE");
      if (v != null && v.length() > 0) fields.put("WHERE",v);
      DateFormat df = new SimpleDateFormat("h:mmaa");
      if (start_time > 0) {
         fields.put("START",df.format(start_time));
       }
      if (end_time > 0) {
         // if end time is on another day, we should set things differently
         fields.put("END",df.format(end_time));
       }
      
      String w = property_set.get("TITLE");
      if (w != null) fields.put("CONTENT",w);
      
      if (property_set.get("ALLDAY") != null) fields.put("ALLDAY","TRUE");
    }
   
   private void setProperty(String id,String v,boolean upper) {
      if (v == null) return;
      if (upper) v = v.toUpperCase();
      property_set.put(id,v);
    }
   
   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("EVENT " + new Date(start_time) + " " + new Date(end_time) + "\n");
      for (Map.Entry<String,String> ent : property_set.entrySet()) {
         buf.append("\t" + ent.getKey() + "=\t'" + ent.getValue() + "'\n");
       }
      return buf.toString();
    }
   
}	// end of inner class CalEvent




/********************************************************************************/
/*										*/
/*	Event matcher class							*/
/*										*/
/********************************************************************************/

private class EventMatcher {
   
   private List<MatchItem> match_items;
   
   EventMatcher(String m) {
      match_items = new ArrayList<MatchItem>();
      StringTokenizer tok = new StringTokenizer(m,",");
      while (tok.hasMoreTokens()) {
         String s = tok.nextToken();
         MatchItem mi = new MatchItem(s);
         match_items.add(mi);
       }
    }
   
   boolean match(CalEvent cev) {
      for (MatchItem mi : match_items) {
         if (!mi.match(cev)) return false;
       }
      return true;
    }
   
}	// end of inner class EventMatcher



private class MatchItem {
   
   private String match_key;
   private Pattern match_pattern;
   private boolean invert_match;
   
   MatchItem(String s) {
      int i1 = s.indexOf("=");
      int i2 = s.indexOf("!");
      if (i1 < 0 && i2 < 0) {			// KEY -- just must be non-null
         match_key = s.trim();
         match_pattern = null;
         invert_match = true;
       }
      else if (i2 == 0 && i1 < 0) {		// !KEY -- just must be null
         match_key = s.substring(1).trim();
         match_pattern = null;
         invert_match = false;
       }
      else {
         int i3;
         int i4 = -1;
         if (i1 > 0 && i2 > 0 && i1 == i2+1) {
            i3 = i1;
            i4 = i2;
            invert_match = true;
          }
         else if (i1 < 0 || (i2 > 0 && i2 < i1)) {
            i3 = i2;
            invert_match = true;
          }
         else i3 = i1;
         if (i4 < 0) i4 = i3;
         match_key = s.substring(0,i4).trim();
         String pat = s.substring(i3+1).trim();
         match_pattern = Pattern.compile(pat,Pattern.CASE_INSENSITIVE);
       }
    }
   
   boolean match(CalEvent cev) {
      if (match_key == null) return true;
      
      String v = cev.getProperty(match_key);
      boolean rslt = false;
      if (v == null || v.length() == 0) rslt = match_pattern == null;
      else if (match_pattern == null) rslt = false;
      else {
         Matcher m = match_pattern.matcher(v);
         rslt = m.find();
       }
      
      if (invert_match) rslt = !rslt;
      
      return rslt;
    }
   
}	// end of inner class MatchItem



}       // end of class CatprogGoogleCalendar




/* end of CatprogGoogleCalendar.java */

