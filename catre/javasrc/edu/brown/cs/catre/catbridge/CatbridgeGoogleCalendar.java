/********************************************************************************/
/*                                                                              */
/*              CatbridgeGoogleCalendar.java                                    */
/*                                                                              */
/*      Device based on accessing a Google calendar                             */
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



package edu.brown.cs.catre.catbridge;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimerTask;

import com.google.api.client.auth.oauth2.Credential;
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

class CatbridgeGoogleCalendar extends CatbridgeBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

// Storage for common calendar bridge
private File credentials_file;
private com.google.api.services.calendar.Calendar calendar_service;

// Storage for universe-specific calendar bridge
private CatbridgeGoogleCalendar google_access;
private Map<String,String> calendar_ids;
private DateTime last_check;
private Set<CalEvent> all_events; 

private static final String APPLICATION_NAME= "Catre Google Calendar Bridge";
private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
private static final String TOKENS_DIRECTORY_PATH = "tokens";
private static final List<String> SCOPES =
   Collections.singletonList(CalendarScopes.CALENDAR_READONLY);



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CatbridgeGoogleCalendar(CatreController cc)
{
   File f1 = cc.findBaseDirectory();
   File f2 = new File(f1,"secret");
   File f3 = new File(f2,"catre-sherpa-creds.json");
   credentials_file = f3;
   
   try {
      setupService();
    }
   catch (Exception e) {
      CatreLog.logE("CATBRIDGE","Problem setting up google calendar service",e);
    }
}



CatbridgeGoogleCalendar(CatbridgeGoogleCalendar base,CatreUniverse u,CatreBridgeAuthorization ba)
{
   super(base,u);
   google_access = base;
   calendar_ids = new HashMap<>();
   last_check = null;
   all_events = new HashSet<>();
   
   String cals = ba.getValue("AUTH_CALENDARS");
   StringTokenizer tok = new StringTokenizer(cals," ,;");
   while (tok.hasMoreTokens()) {
      String cspec = tok.nextToken();
      int idx = cspec.indexOf("=");
      if (idx > 0) {
         String cnm = cspec.substring(0,idx).trim();
         String ccd = cspec.substring(idx+1).trim();
         calendar_ids.put(cnm,ccd);
       }
      else {
         calendar_ids.put(cspec,cspec);
       }
    }
}


protected CatbridgeBase createInstance(CatreUniverse u,CatreBridgeAuthorization ba)
{
   return new CatbridgeGoogleCalendar(this,u,ba);
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public String getName()                       { return "gcal"; }


@Override public List<CatreDevice> findDevices()
{
   List<CatreDevice> rslt = new ArrayList<>();
    
   rslt.add(new GoogleCalendarDevice(this));
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Access google calendar                                                  */
/*                                                                              */
/********************************************************************************/

private Credential getCredentials(NetHttpTransport transport) throws IOException
{
   // Load client secrets.
   FileReader in = new FileReader(credentials_file);
   GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,in);
   
   // Build flow and trigger user authorization request.
   GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
         transport, JSON_FACTORY, clientSecrets, SCOPES)
      .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
      .setAccessType("offline")
      .build();
   LocalServerReceiver receiver = new LocalServerReceiver.Builder()
//    .setHost("sherpa.cs.brown.edu") 
//    .setCallbackPath("/oauth")
//    .setPort(3332)
      .build();
   Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
   //returns an authorized Credential object.
   return credential;
   
}


private void setupService() throws IOException, GeneralSecurityException 
{
   final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
   calendar_service = new com.google.api.services.calendar.Calendar.Builder(transport, JSON_FACTORY, getCredentials(transport))
      .setApplicationName(APPLICATION_NAME)
      .build();
}




private Set<CalEvent> loadEvents(DateTime dt1,DateTime dt2,Collection<String> cals)
{
   Set<CalEvent> rslt = new HashSet<>();
   
   for (String calname : cals) {
      try {
         List<Event> events = calendar_service.events().list(calname)
            .setTimeMin(dt1)
            .setTimeMax(dt2)
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute()
            .getItems();
         for (Event evt : events) {
            CalEvent ce = new CalEvent(evt);
            rslt.add(ce);
          }
       }
      catch (IOException e) {
         CatreLog.logE("CATBRIDGE","Problem accessing calendar",e);
       }
    }
   
   return rslt;
} 



/********************************************************************************/
/*                                                                              */
/*      Update events for universe                                              */
/*                                                                              */
/********************************************************************************/

private boolean updateActiveEvents(long whent)
{
   if (whent == 0) whent = System.currentTimeMillis();
   Calendar c1 = Calendar.getInstance();
   c1.setTimeInMillis(whent);
   Calendar c2 = CatreTimeSlotEvent.startOfDay(c1);
   DateTime dt1 = new DateTime(c2.getTimeInMillis());
   c1.setTimeInMillis(whent + 2*T_DAY);
   c2 = CatreTimeSlotEvent.startOfDay(c1);
   DateTime dt2 = new DateTime(c2.getTimeInMillis());
   
   if (dt1.equals(last_check)) return false;                  // up to date
   
   Set<CalEvent> evts = google_access.loadEvents(dt1,dt2,calendar_ids.values());
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
/*                                                                              */
/*      Calendar Device                                                         */
/*                                                                              */
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
   
   @Override public boolean isCalendarDevice()          { return true; }
   
   private CatbridgeGoogleCalendar getCalBridge() {
      return (CatbridgeGoogleCalendar) getBridge();
    }
   
   void setTime() {
      long delay = T_HOUR; 		// check at least each hour to allow new events
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
      Collection<?> col = (Collection<?>) getValueInWorld(cp,null);
      if (!cur.equals(col)) {
         setValueInWorld(cp,cur,null);
         fireChanged(getCurrentWorld(),cp);
       }
    }
   
}       // end of inner class GoogleCalendarDevice



/********************************************************************************/
/*                                                                              */
/*      Timer to update the calendar                                            */
/*                                                                              */
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



        
}       // end of class CatbridgeGoogleCalendar




/* end of CatbridgeGoogleCalendar.java */

