package com.ziroh;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.ziroh.common.CalendarDetails;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.List;

/* class to demonstrate use of Calendar events list API */
public class MeetingScheduler {
    private int INTERVALS = 4;
    private BitSet TIMELINE_BIT_ARRAY = new BitSet(24 * INTERVALS);
    private CalendarDetails calendarDetails;

    public MeetingScheduler() {
        calendarDetails = new CalendarDetails();
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = MeetingScheduler.class.getResourceAsStream(calendarDetails.getCredFilePath());
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + calendarDetails.getCredFilePath());
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(calendarDetails.getJsonFactory(), new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, calendarDetails.getJsonFactory(), clientSecrets, calendarDetails.getScopes())
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(calendarDetails.getTokensDirPath())))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        //returns an authorized Credential object.
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    private void showCalendarEventsList(com.google.api.services.calendar.model.Calendar calendar, Events events) {
        System.out.println("CALENDAR NAME: " + calendar.getSummary());
        List<Event> items = events.getItems();
        if (items.isEmpty()) {
            System.out.println("\tNO UPCOMING EVENTS FOUND: ");
        } else {
            System.out.println("\tUPCOMING EVENTS");
            for (Event event : items) {

                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    start = event.getStart().getDate();
                }
                System.out.printf("\t%s (%s)\n", event.getSummary(), start);
            }
        }
    }

    private void getAllEvents(Calendar calendarService, DateTime startTime, DateTime endTime)
            throws IOException {
        for (String calendarId: calendarDetails.getCalendarIdList()) {
            com.google.api.services.calendar.model.Calendar calendar =
                    calendarService.calendars().get(calendarId).execute();
            Events events = calendarService.events()
                    .list(calendarId)
                    .setTimeMin(startTime)
                    .setTimeMax(endTime)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            showCalendarEventsList(calendar, events);
        }
    }

    public void getAllCalendarEventsList(Calendar calendarService) throws IOException {
        // List the events of a date from a calendar specified by calendarID
        SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
        String startTimeString = "25-11-2022 00:00:00";
        String endTimeString = "26-11-2022 00:00:00";
        try{
            //formatting the dateString to convert it into a Date
            DateTime startTime = new DateTime(sdf.parse(startTimeString));
            DateTime endTime = new DateTime(sdf.parse(endTimeString));
            System.out.println("TIME: " + System.currentTimeMillis());
            getAllEvents(calendarService, startTime, endTime);
        }catch(ParseException e){
            e.printStackTrace();
        }
    }

    public Calendar buildCalendarService() throws GeneralSecurityException, IOException {
        // Build a new authorized API client calendarService.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Calendar.Builder(HTTP_TRANSPORT, calendarDetails.getJsonFactory(), getCredentials(HTTP_TRANSPORT))
                .setApplicationName(calendarDetails.getAppName())
                .build();
    }
}