/********************************************************************************/
/*										*/
/*		CatbridgeGoogleCalendar.java					*/
/*										*/
/*	Device based on accessing a Google calendar				*/
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




package edu.brown.cs.catre.catbridge;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttachment;
import com.google.api.services.calendar.model.EventAttendee;

import edu.brown.cs.catre.catdev.CatdevDevice;
import edu.brown.cs.catre.catre.CatreBridgeAuthorization;
import edu.brown.cs.catre.catre.CatreCalendarEvent;
import edu.brown.cs.catre.catre.CatreTimeSlotEvent;
import edu.brown.cs.catre.catre.CatreController;
import edu.brown.cs.catre.catre.CatreDevice;
import edu.brown.cs.catre.catre.CatreLog;
import edu.brown.cs.catre.catre.CatreParameter;
import edu.brown.cs.catre.catre.CatreStore;
import edu.brown.cs.catre.catre.CatreUniverse;

/**
 *      Need to authorize additional test users at https://console.cloud.google.com/apis/credentials/consent?project=catre-372313
 *      before they can successfully use this package.
 **/
/**
 *      To allow access to a calendar, one needs to share the calendar with sherpa.catre@gmail.com.  We should
 *      create an app or web page that will do this automatically, asking permission from the user.  There should
 *      also be a password attached to this so that only this user can access the calendar.  Note that this has
 *      to work for shared calendars such as holidays.  
 *
 *      To allow access to a particular calendar, the user should first register that calendar with a passkey.
 *      If the calendar is already registered, the key needs to match -- however if the calendar is not yet
 *      shared, there should be an option of resetting the key (perhaps sending an email to sherpa.catre@gmail.com
 *      from the calendar owner's address).  Then when adding a calendar to an account, the key and calendar
 *      known key must match.  Some calendars (e.g. Holiday) will be set up with a null key.
 *
 **/

class CatbridgeGoogleCalendar extends CatbridgeBase
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

// Storage for common calendar bridge
private File credentials_file;
private File tokens_file;
private com.google.api.services.calendar.Calendar calendar_service;
private Map<String,CalendarData> all_calendars;

// Storage for universe-specific calendar bridge
private CatbridgeGoogleCalendar google_access;
private List<CalendarData> use_calendars;
private DateTime last_check;
private long check_calendars;
private CatreBridgeAuthorization calendar_auth;
private Set<CalEvent> all_events;


private static final String APPLICATION_NAME= "Catre IoT Server";
private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
private static final String TOKENS_DIRECTORY_PATH = "google-tokens";
private static final List<String> SCOPES =
   List.of(CalendarScopes.CALENDAR_READONLY,
         CalendarScopes.CALENDAR_EVENTS_READONLY);


public static final int OAUTH_PORT = 8888;

private static String HOLIDAYS;

static {
   try {
      HOLIDAYS = URLEncoder.encode("en.usa#holiday@group.v.calendar.google.com","UTF-8");
    }
   catch (UnsupportedEncodingException e) { }
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

CatbridgeGoogleCalendar(CatreController cc)
{
   calendar_service = null;
   
   File f1 = cc.findBaseDirectory();
   File f2 = new File(f1,"secret");
   File f3 = new File(f2,"gcal-creds.json");
   
   CatreLog.logD("CATBRIDGE","Credentials file: " + f3 + " " + f3.exists());
   credentials_file = f3;
   
   if (!f3.exists()) return;
   
   tokens_file = new File(f2,TOKENS_DIRECTORY_PATH);
   tokens_file.mkdirs();
   CatreLog.logD("CATBRIDGE","Tokens directory: " + tokens_file);
   
   try {
      setupService();
      updateCalendars();
    }
   catch (Exception e) {
      CatreLog.logE("CATBRIDGE","Problem setting up google calendar service",e);
    }
}



CatbridgeGoogleCalendar(CatbridgeGoogleCalendar base,CatreUniverse u,CatreBridgeAuthorization ba)
{
   super(base,u);
   google_access = base;
   calendar_service = null;
   credentials_file = null;
   tokens_file = null;
   last_check = null;
   all_events = new HashSet<>();
   check_calendars = 0;
   
   use_calendars = new ArrayList<>();
   calendar_auth = ba;
   setupAuthorizedCalendars();
}


protected CatbridgeBase createInstance(CatreUniverse u,CatreBridgeAuthorization ba)
{
   return new CatbridgeGoogleCalendar(this,u,ba);
}



private void setupAuthorizedCalendars()
{
   check_calendars = 0;
   
   List<CalendarData> use = new ArrayList<>();
   
   boolean haveholidays = false;
   for (int i = 0; i < calendar_auth.getAuthorizationCount(); ++i) {
      String cnm = calendar_auth.getValue(i,"AUTH_CALENDAR");
      String pwd = calendar_auth.getValue(i,"AUTH_PASSWORD");
      if (pwd == null) pwd = "*";
      CalendarData cd = google_access.findCalendar(cnm,pwd);
      if (cd == null) {
         check_calendars = System.currentTimeMillis() + 24*60*60*1000;
       }
      else {
         CatreStore cs = getUniverse().getCatre().getDatabase();
         if (cd.getId().equals(HOLIDAYS)) haveholidays = true;
         if (cs.validateCalendar(getUniverse().getUser(),cnm,pwd)) {
            use.add(cd);
          }
       }
    }
   if (!haveholidays) {
      CalendarData cd = google_access.findCalendar(HOLIDAYS,"*");
      if (cd != null) use.add(cd);
    }
   
   use_calendars = use;
}



/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override public String getName()			{ return "gcal"; }

@Override protected boolean useCedes()                  { return false; } 


@Override public List<CatreDevice> findDevices()
{
   List<CatreDevice> rslt = new ArrayList<>();

   rslt.add(new GoogleCalendarDevice(this));

   return rslt;
}


@Override public JSONObject getBridgeInfo()
{
   JSONObject rslt = super.getBridgeInfo();
   
   JSONArray calvals = new JSONArray();
   JSONArray calpwds = new JSONArray();
   for (int i = 0; i < calendar_auth.getAuthorizationCount(); ++i) {
      String cnm = calendar_auth.getValue(i,"AUTH_CALENDAR");
      String pwd = calendar_auth.getValue(i,"AUTH_PASSWORD");
      if (pwd == null) pwd = "*";
      calvals.put(cnm);
      calpwds.put(pwd);
    }
   
   JSONObject f1 = buildJson("KEY","AUTH_CALENDAR","LABEL","Calendar Name","VALUE",calvals,
         "HINT","Calendar name or id","TYPE","STRING");
   JSONObject f2 = buildJson("KEY","AUTH_PASSWORD","LABEL","Calendar Password","VALUE",calpwds,
         "HINT","Calendar password (defines password if first use)","TYPE","STRING");
   rslt.put("FIELDS",buildJsonArray(f1,f2));
   rslt.put("SINGLE",false);
   
   String desc = "To allow access to your calendar events, you should first share your " +
        "calendar with sherpa.catre@gmail.com.   This could take a day or two since " + 
        "it has to be acknowledged manually.  Then you should provide " +
         "the calendar id (e.g. some_person@gmail.com) and a unique pass key for that " +
         "calendar. " +
         "Within a day, you should be able to access the calendar events as conditions. " +
         "Some calendars, e.g. Holidays in United States, can be shared with no pass key.";
         
   rslt.put("HELP",desc);
   
   return rslt;
}




/********************************************************************************/
/*										*/
/*	Access google calendar							*/
/*										*/
/********************************************************************************/

private Credential getCredentials(NetHttpTransport transport) throws IOException
{
   // Load client secrets.
   FileReader in = new FileReader(credentials_file);
   GoogleClientSecrets clientsecrets = GoogleClientSecrets.load(JSON_FACTORY,in);

   // Build flow and trigger user authorization request.

   GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
	 transport, JSON_FACTORY, clientsecrets, SCOPES)
         .setDataStoreFactory(new FileDataStoreFactory(tokens_file))
         .setAccessType("offline")
         .addRefreshListener(new CredRefresher())
         .build();
   
   LocalServerReceiver receiver = new LocalServerReceiver.Builder()
         .setPort(OAUTH_PORT)
         .build();
   Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
   CatreLog.logD("CATBRIDGE","Created credential " + credential +
         credential.getAccessToken() + " " + credential.getMethod() + " " + 
         credential.getRefreshToken() + " " + credential.getTokenServerEncodedUrl());
   //returns an authorized Credential object.
   return credential;
}


private void setupService() throws IOException, GeneralSecurityException
{
   final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
   calendar_service = new com.google.api.services.calendar.Calendar.Builder(transport,
         JSON_FACTORY,
         getCredentials(transport))
      .setApplicationName(APPLICATION_NAME)
      .build();
   
   CatreLog.logD("CATBRIDGE","GOOGLE calendar service setup: " +
         calendar_service.getApplicationName() + " " + 
         calendar_service.getBaseUrl() + " " +
         calendar_service.getRootUrl() + " " +
         calendar_service.getServicePath() + " " + 
         calendar_service.getSuppressPatternChecks());
   
   CalendarList cl = calendar_service.calendarList().list().execute();
   CatreLog.logD("CATBRIDGE","FOUND Calendars " + cl.size() + " " + cl);
}


private void updateCalendars()
{
   if (calendar_service == null) return;
   
   Map<String,CalendarData> calmap = new HashMap<>();
   
   try {
      CalendarList cl = calendar_service.calendarList().list().execute();
      for (CalendarListEntry ent : cl.getItems()) {
         CalendarData cd = new CalendarData(ent);
         calmap.put(cd.getId(),cd);  
         if (cd.getName() != null) calmap.put(cd.getName(),cd);
         if (cd.getAltName() != null) calmap.put(cd.getAltName(),cd);
       }
    }
   catch (IOException e) {
      CatreLog.logE("CATBRIDGE","Problem accessing calendar list",e);
    }
   
   all_calendars = calmap;
}



private Set<CalEvent> loadEvents(DateTime dt1,DateTime dt2,Collection<CalendarData> cals)
{
   Set<CalEvent> rslt = new HashSet<>();
   
   if (calendar_service == null) return rslt;
   
   CatreLog.logD("CATBRIDGE","CALENDAR DATES: " + dt1.toStringRfc3339() + " " + dt2.toStringRfc3339());
   
   for (CalendarData calent : cals) {
      String calname = calent.getId();
      try {
         // add eventType parameter here (need to update library) to avoid working location events
         // or allow use of WorkingLocation, OutOfOffice and FocusTime information
         List<Event> events = calendar_service.events().list(calname)
            .setTimeMin(dt1)
            .setTimeMax(dt2)
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute()
            .getItems();
         CatreLog.logD("CATBRIDGE: Successfully found GOOGLE events " + events.size());
         for (Event evt : events) {
            CalEvent ce = new CalEvent(evt);
            rslt.add(ce);
          }
         break;
       }
      catch (IOException e) {
         CatreLog.logE("CATBRIDGE","Problem accessing calendar " + calname,e);
       }
    }

   return rslt;
}


private synchronized CalendarData findCalendar(String id,String pwd)
{
   if (all_calendars == null) return null;
   CalendarData cd = all_calendars.get(id);
   if (pwd == null) return cd;
   
   if (cd == null) {
      updateCalendars();
      cd = all_calendars.get(id);
    }
   
   if (cd == null) return null;
   
   return all_calendars.get(id);
}



private class CredRefresher implements CredentialRefreshListener {

   @Override public void onTokenErrorResponse(Credential cred,TokenErrorResponse resp) {
      CatreLog.logE("CATBRIDGE","Token error response " + resp + " " +
            cred.getTokenServerEncodedUrl() + " " + cred.getMethod() + "\n\tTOKENS: " + 
            cred.getAccessToken() + " AND " + cred.getRefreshToken());
    }
   
   @Override public void onTokenResponse(Credential cred,TokenResponse resp) {
      CatreLog.logD("CATBRIDGE","Token response " + cred.getAccessToken() + " " + " " + resp);
    }
   
}       // end of inner class CredRefresher




/********************************************************************************/
/*										*/
/*	Update events for universe						*/
/*										*/
/********************************************************************************/

private boolean updateActiveEvents(long whent)
{
   if (whent == 0) whent = System.currentTimeMillis();
   if (check_calendars > 0 && whent > check_calendars) {
      setupAuthorizedCalendars();
    }
   
   Calendar c1 = Calendar.getInstance();
   c1.setTimeInMillis(whent);
   Calendar c2 = CatreTimeSlotEvent.startOfDay(c1);
   DateTime dt1 = new DateTime(c2.getTimeInMillis());
   c1.setTimeInMillis(whent + 2*T_DAY);
   c2 = CatreTimeSlotEvent.startOfDay(c1);
   DateTime dt2 = new DateTime(c2.getTimeInMillis());

   if (dt1.equals(last_check)) return false;		      // up to date

   Set<CalEvent> evts = google_access.loadEvents(dt1,dt2,use_calendars); 
   last_check = dt1;
   if (evts.equals(all_events)) return false;

   all_events = evts;
   return true;
}



/********************************************************************************/
/*										*/
/*	Represent an event							*/
/*										*/
/********************************************************************************/

private class CalEvent implements CatreCalendarEvent {

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
      Calendar c2 = CatreTimeSlotEvent.startOfDay(c0);
      Calendar c3 = CatreTimeSlotEvent.startOfDay(c1);
      if (c0.equals(c2) && c1.equals(c3)) {
         property_set.put("ALLDAY","true");
       }
    }

   @Override public long getStartTime() 	{ return start_time; }
   @Override public long getEndTime()		{ return end_time; }
   @Override public Map<String,String> getProperties() {
      return new HashMap<>(property_set);
    }

   boolean isCurrent(long now) {
      if (now == 0) now = System.currentTimeMillis();
      return now >= start_time && now < end_time;
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

   @Override public int hashCode() {
      Long l0 = start_time;
      Long l1 = end_time;
      int hc = l0.hashCode() + l1.hashCode() + property_set.hashCode();
      return hc;
    }

   @Override public boolean equals(Object o) {
      if (o instanceof CalEvent) {
	 CalEvent ce = (CalEvent) o;
	 if (ce.start_time != start_time) return false;
	 if (ce.end_time != end_time) return false;
	 if (!ce.property_set.equals(property_set)) return false;
	 return true;
       }
      return false;
    }

}	// end of inner class CalEvent




/********************************************************************************/
/*										*/
/*	Calendar Device 							*/
/*										*/
/********************************************************************************/

@Override public CatreDevice createDevice(CatreStore cs,Map<String,Object> map)
{
   return new GoogleCalendarDevice(this,cs,map);
}



private static class GoogleCalendarDevice extends CatdevDevice {

   GoogleCalendarDevice(CatbridgeGoogleCalendar bridge) {
      super(bridge.getUniverse(),bridge);
   
      setName("GoogleCalendar_" + getUniverse().getUser().getUserName());
      setLabel("Google Calendar for " + getUniverse().getUser().getUserName());
      StringBuffer buf = new StringBuffer();
      buf.append("GCAL_");
      buf.append(getUniverse().getDataUID());
      setDeviceId(buf.toString());
   
      CatreParameter cp = getUniverse().createEventsParameter("EVENTS");
      addParameter(cp);
    }


   GoogleCalendarDevice(CatbridgeBase bridge,CatreStore cs,Map<String,Object> map) {
      super(bridge.getUniverse(),bridge);
      fromJson(cs,map);
   
      CatreParameter cp = getUniverse().createEventsParameter("EVENTS");
      addParameter(cp);
    }

   @Override public boolean isCalendarDevice()		{ return true; }

   private CatbridgeGoogleCalendar getCalBridge() {
      return (CatbridgeGoogleCalendar) getBridge();
    }

   @Override protected void localStartDevice() {
      setTime();
    }
   
   void setTime() {
      long delay = T_HOUR;		// check at least each hour to allow new events
      long now = System.currentTimeMillis();
      
      CatbridgeGoogleCalendar cal = getCalBridge();
      cal.updateActiveEvents(now);
   
      Set<CalEvent> cur = new HashSet<>();
   
      for (CalEvent ce : cal.all_events) {
         if (ce.isCurrent(now)) cur.add(ce);
         long tim = ce.getStartTime();
         if (tim <= now) tim = ce.getEndTime();
         if (tim <= now) continue;
         delay = Math.min(delay,tim-now);
       }
      delay = Math.max(delay,10*T_SECOND);
      CatreLog.logD("CATBRIDGE","Schedule Calendar check for " + getUniverse().getName() +
            " " + delay + " at " + (new Date(now+delay).toString()));
   
      getUniverse().getCatre().schedule(new CheckTimer(this),delay);
   
      CatreParameter cp = findParameter("EVENTS");
      Object val = getParameterValue(cp);
      Collection<?> col = new ArrayList<>();
      if (val != null && val instanceof Collection<?>) {
         col = (Collection<?>) val;
       }
      else val = null;
      
      if (val == null || !cur.equals(col)) {
         setParameterValue(cp,cur);
         fireChanged(cp);
       }
    }

}	// end of inner class GoogleCalendarDevice



/********************************************************************************/
/*										*/
/*	Timer to update the calendar						*/
/*										*/
/********************************************************************************/

private static class CheckTimer extends TimerTask {

   private GoogleCalendarDevice for_device;

   CheckTimer(GoogleCalendarDevice d) {
      for_device = d;
    }

   @Override public void run() {
      CatreLog.logI("CATBRIDGE","Checking google Calendar for " +
            for_device.getUniverse().getName() +
            " at " + (new Date().toString()));
      for_device.setTime();
    }

}	// end of inner class CheckTimer




/********************************************************************************/
/*                                                                              */
/*      Information about a calendar                                            */
/*                                                                              */
/********************************************************************************/

private static class CalendarData {
   
   private String summary_name;
   private String alt_summary;
   private String calendar_id;
   private String time_zone;
   
   CalendarData(CalendarListEntry ent) {
      summary_name = ent.getSummary();
      alt_summary = ent.getSummaryOverride();
      calendar_id = ent.getId();
      time_zone = ent.getTimeZone();
    }
   
   private String getId()                               { return calendar_id; }
   
   private String getName()                             { return summary_name; }
   private String getAltName()                          { return alt_summary; }
   
   @SuppressWarnings("unused")
   private String getTimeZone()                         { return time_zone; }
}


}	// end of class CatbridgeGoogleCalendar




/* end of CatbridgeGoogleCalendar.java */

