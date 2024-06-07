/********************************************************************************/
/*                                                                              */
/*              CalendarQuickstart.java                                         */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2023 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2023, Brown University, Providence, RI.                            *
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
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/* class to demonstrate use of Calendar events list API */
public class CalendarQuickstart {
/**
 * Application name.
 */
private static final String APPLICATION_NAME = "Google Calendar API Java Quickstart";
/**
 * Global instance of the JSON factory.
 */
private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
/**
 * Directory to store authorization tokens for this application.
 */
private static final String TOKENS_DIRECTORY_PATH = "/pro/iot/secret/quicktokens";

/**
 * Global instance of the scopes required by this quickstart.
 * If modifying these scopes, delete your previously saved tokens/ folder.
 */
private static final List<String> SCOPES =
   List.of(CalendarScopes.CALENDAR,
         CalendarScopes.CALENDAR_EVENTS_READONLY);
// Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
private static final String CREDENTIALS_FILE_PATH = "/pro/iot/secret/gcal-creds.json";

/**
 * Creates an authorized Credential object.
 *
 * @param HTTP_TRANSPORT The network HTTP Transport.
 * @return An authorized Credential object.
 * @throws IOException If the credentials.json file cannot be found.
 */
private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
   throws IOException {
   // Load client secrets.
   FileReader in = new FileReader(CREDENTIALS_FILE_PATH);
   GoogleClientSecrets clientSecrets =
      GoogleClientSecrets.load(JSON_FACTORY, in);
   
   // Build flow and trigger user authorization request.
   File f1 = new java.io.File(TOKENS_DIRECTORY_PATH);
   GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
         HTTP_TRANSPORT, JSON_FACTORY, 
         clientSecrets, SCOPES)
         .setDataStoreFactory(new FileDataStoreFactory(f1))
         .setAccessType("offline")
         .build();
   LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
   Credential credential = new AuthorizationCodeInstalledApp(flow, receiver)
         .authorize("user");
   //returns an authorized Credential object.
   System.err.println("Created credential " + credential +
         credential.getAccessToken() + " " + credential.getMethod() + " " + 
         credential.getRefreshToken() + " " + credential.getTokenServerEncodedUrl());
   
   return credential;
}

public static void main(String... args) throws IOException, GeneralSecurityException {
   // Build a new authorized API client service.
   final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
   Calendar service =
      new Calendar.Builder(HTTP_TRANSPORT, 
            JSON_FACTORY,
            getCredentials(HTTP_TRANSPORT))
      .setApplicationName(APPLICATION_NAME)
      .build();
   
   System.err.println("GOOGLE calendar service setup: " +
         service.getApplicationName() + " " + 
         service.getBaseUrl() + " " +
         service.getRootUrl() + " " +
         service.getServicePath() + " " + 
         service.getSuppressPatternChecks());
   
   CalendarList cl = service.calendarList().list().execute();
   System.err.println("FOUND CALENDARS: " + cl.size() + " " + cl);
   
   // List the next 10 events from the primary calendar.
   DateTime now = new DateTime(System.currentTimeMillis());
   Events events = service.events().list("steven_reiss@brown.edu")
         .setMaxResults(10)
         .setTimeMin(now)
         .setOrderBy("startTime")
         .setSingleEvents(true)
         .execute();
   List<Event> items = events.getItems();
   if (items.isEmpty()) {
      System.out.println("No upcoming events found.");
    } else {
       System.out.println("Upcoming events");
       for (Event event : items) {
          DateTime start = event.getStart().getDateTime();
          if (start == null) {
             start = event.getStart().getDate();
           }
          System.out.printf("%s (%s)\n", event.getSummary(), start);
        }
     }
}


}       // end of class CalendarQuickstart




/* end of CalendarQuickstart.java */

