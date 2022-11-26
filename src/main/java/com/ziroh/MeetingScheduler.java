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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MeetingScheduler {
    private int intervals = 4;
    private BitSet timelineBitArray = new BitSet(24 * intervals);
    private String startTimeString = "2022-11-25 00:00:00";
    private String endTimeString = "2022-11-26 00:00:00";
    private CalendarDetails calendarDetails;

    public MeetingScheduler() {
        calendarDetails = new CalendarDetails();
    }

    public MeetingScheduler(String startTimeString, String endTimeString) {
        calendarDetails = new CalendarDetails();
        this.startTimeString = startTimeString;
        this.endTimeString = endTimeString;
    }

    public Calendar buildCalendarService() throws GeneralSecurityException, IOException {
        // Build a new authorized API client calendarService.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Calendar.Builder(HTTP_TRANSPORT, calendarDetails.getJsonFactory(), getCredentials(HTTP_TRANSPORT))
                .setApplicationName(calendarDetails.getAppName())
                .build();
    }

    public void getAllCalendarEventsList(Calendar calendarService) throws IOException {
        // List the events of a date from a calendar specified by calendarID
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try{
            //formatting the dateString to convert it into a Date
            DateTime startTime = new DateTime(sdf.parse(startTimeString));
            DateTime endTime = new DateTime(sdf.parse(endTimeString));
            getAllEvents(calendarService, startTime, endTime);
        }catch(ParseException e){
            e.printStackTrace();
        }
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

    private void showCalendarEventsList(com.google.api.services.calendar.model.Calendar calendar, Events events) {
        System.out.println("CALENDAR NAME: " + calendar.getSummary());
        List<Event> items = events.getItems();
        if (items.isEmpty()) {
            System.out.println("\tNO UPCOMING EVENTS FOUND: ");
        } else {
            System.out.println("\tUPCOMING EVENTS: ");
            for (Event event : items) {

                DateTime start = event.getStart().getDateTime();
                DateTime end = event.getEnd().getDateTime();
                if (start == null) {
                    start = event.getStart().getDate();
                }
                System.out.println("\t\tEVENT: " + event.getSummary());
                showEventDetails(start, end);
            }
        }
    }

    private void showEventDetails(DateTime start, DateTime end) {
        Pattern pattern = Pattern.compile(
                "(\\d{4}-\\d{2}-\\d{2})|(\\d{2}:\\d{2}:\\d{2})|([+-]\\d{2}:\\d{2})");
        Matcher matcher = pattern.matcher(start.toStringRfc3339());
        ArrayList<String> startDateDetails = new ArrayList<>();
        while (matcher.find()) {
            startDateDetails.add(matcher.group());
        }
        ArrayList<String> endDateDetails = new ArrayList<>();
        matcher = pattern.matcher(end.toStringRfc3339());
        while (matcher.find()) {
            endDateDetails.add(matcher.group());
        }
        String date = startDateDetails.get(0);
        String startTime = startDateDetails.get(1);
        String timeZone = startDateDetails.get(2);
        String endTime = endDateDetails.get(1);
        System.out.println("\t\t\tDATE: " + date);
        System.out.println("\t\t\tSTART-TIME: " + startTime);
        System.out.println("\t\t\tEND-TIME: " + endTime);
        System.out.println("\t\t\tTIME-ZONE: " + timeZone);

        getBitArrayIndex(date, startTime, date, endTime, timeZone);
    }

    private void getBitArrayIndex(String startDate, String startTime, String endDate, String endTime, String timeZone) {
        String startDateTime = startDate + " " + startTime;
        String endDateTime = endDate + " " + endTime;

        LocalDateTime localStart = LocalDateTime.parse(startDateTime,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime localEnd = LocalDateTime.parse(endDateTime,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime trueStart = LocalDateTime.parse(startTimeString,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        long startMillis = localStart
                .atZone(ZoneId.of(timeZone))
                .toInstant().toEpochMilli();

        long endMillis = localEnd
                .atZone(ZoneId.of(timeZone))
                .toInstant().toEpochMilli();

        long duration = endMillis - startMillis;

        System.out.println("START MILLIS  : " + startMillis);
        System.out.println("END   MILLIS  : " + endMillis);
        System.out.println("EVENT DURATION: " + duration);

        long trueStartMillis = trueStart
                .atZone(ZoneId.of(timeZone))
                .toInstant().toEpochMilli();

        long relativeStartMillis = startMillis - trueStartMillis;
        int trueMinutes = (int) (((relativeStartMillis/1000)/60));

        int minutes = (int) (((duration/1000)/60));
        int hour = (int) Math.floor(minutes/60);

        System.out.println("\tHOUR   : " + hour);
        System.out.println("\tMINUTES: " + minutes);

        int steps = minutes/(60/4);
        int interval = trueMinutes/(60/4);
        System.out.println("\t\tINTERVAL: " + interval);
        System.out.println("\t\tSTEPS   : " + steps);
        setTimelineBitArray(interval, steps);
    }

    private void setTimelineBitArray(int interval, int steps) {
        timelineBitArray.set(interval, interval+steps);
        StringBuilder bitString = new StringBuilder();
        for (int i = 0; i < timelineBitArray.length(); i++) {
            bitString.append(timelineBitArray.get(i) == true ? 1:0);
        }
        System.out.println(bitString);
        System.out.println(timelineBitArray.length());
    }
}