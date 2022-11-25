package com.ziroh;

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
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/* class to demonstarte use of Calendar events list API */
public class CalendarTrials {
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
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES =
            Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final List<String> CALENDAR_ID_LIST =
            new ArrayList<>(List.of(
                    "c_5fbc6e88b1213bcd40fb097220944c98e4ef69384de512da4ab48be4c291ff93@group.calendar.google.com",
                    "c_96524e5b976f03f83610e947c5526e4f3388bc897ba8707cf9c69fccd136ae0e@group.calendar.google.com",
                    "c_1be3acae48fb49ab76e8375b58a164cd16552dbe9cab175af2442a3f11ee7fc9@group.calendar.google.com")
            );
    private static int INTERVALS = 4;
    private static BitSet TIMELINE_BIT_ARRAY = new BitSet(24 * INTERVALS);

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
        InputStream in = CalendarTrials.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        //returns an authorized Credential object.
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    private static void showCalendarEventsList(com.google.api.services.calendar.model.Calendar calendar, Events events) {
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

    private static void getAllEvents(Calendar calendarService, DateTime startTime, DateTime endTime)
            throws IOException {
        for (String calendarId:CALENDAR_ID_LIST) {
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

    private static void getAllCalendarEventsList(Calendar calendarService) throws IOException {
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

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client calendarService.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Calendar calendarService =
                new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
        getAllCalendarEventsList(calendarService);
    }
}