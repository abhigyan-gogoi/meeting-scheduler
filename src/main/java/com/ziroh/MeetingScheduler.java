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
    private final int intervals = 4;
    private final int bitArrayLength = 24 * intervals;
    private final BitSet timelineBitArray = new BitSet(bitArrayLength);
    private final ArrayList<Integer> positionArray = new ArrayList<>();
    private String startTimeString = "2022-11-25 00:00:00";
    private String endTimeString = "2022-11-26 00:00:00";
    private final CalendarDetails calendarDetails;

    public MeetingScheduler() {
        calendarDetails = new CalendarDetails();
    }

    public MeetingScheduler(String startTimeString, String endTimeString) {
        calendarDetails = new CalendarDetails();
        this.startTimeString = startTimeString;
        this.endTimeString = endTimeString;
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

    /**
     * Build a new authorized Google Calendar API client calendarService
     *
     * @return A Google Calendar API Service
     * @throws GeneralSecurityException TODO
     * @throws IOException If the credentials.json file cannot be found
     */
    public Calendar buildCalendarService() throws GeneralSecurityException, IOException {
        // Build a new authorized API client calendarService.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Calendar.Builder(HTTP_TRANSPORT, calendarDetails.getJsonFactory(), getCredentials(HTTP_TRANSPORT))
                .setApplicationName(calendarDetails.getAppName())
                .build();
    }

    /**
     * Lists out all the calendars specified in CalendarDetails
     *
     * @param calendarService Google Calendar API Service instance
     * @throws IOException TODO
     */
    public void showAllCalendarEventsList(Calendar calendarService) throws IOException {
        // List the events of a date from a calendar specified by calendarID
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try{
            //formatting the dateString to convert it into a Date
            DateTime startTime = new DateTime(sdf.parse(startTimeString));
            DateTime endTime = new DateTime(sdf.parse(endTimeString));
            showAllEvents(calendarService, startTime, endTime);
        }catch(ParseException e){
            e.printStackTrace();
        }
    }

    /**
     * Lists out all the event in the calendars specified in CalendarDetails
     *
     * @param calendarService Google Calendar API Service instance
     * @param startTime Google DateTime start time specification
     * @param endTime Google DateTime end time specification
     * @throws IOException TODO
     */
    private void showAllEvents(Calendar calendarService, DateTime startTime, DateTime endTime)
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

    /**
     * Prints out all the events details in a calendar
     *
     * @param calendar Google Calendar API calendar instance
     * @param events Google Calendar API events instance. Stores list of events in a specified calendar
     */
    private void showCalendarEventsList(com.google.api.services.calendar.model.Calendar calendar, Events events) {
        System.out.println("CALENDAR NAME: " + calendar.getSummary());
        List<Event> items = events.getItems();
        if (items.isEmpty()) {
            System.out.println("\tNO UPCOMING EVENTS FOUND: ");
        } else {
            System.out.println("\tUPCOMING EVENTS: ");
            for (Event event : items) {
                showEventDetails(event);
                setBitArrayIndices(event);
            }
        }
    }

    /**
     * Prints out the details of an Event and sets the event on the TimeLine Bit Array
     *
     * @param event Google Calendar API event instance. Stores the details of an Event
     */
    private void showEventDetails(Event event) {
        // Get start and end times from event
        DateTime start = event.getStart().getDateTime();
        DateTime end = event.getEnd().getDateTime();
        if (start == null) {
            start = event.getStart().getDate();
        }
        // Print out the event details
        System.out.println("\t\tEVENT DETAILS: " + event.getSummary());
        // Stores DateTime details as an array of strings
        ArrayList<String> startDateDetails = getDateDetails(start);
        ArrayList<String> endDateDetails = getDateDetails(end);
        // Print out the specific event details
        String startDate = startDateDetails.get(0);
        String startTime = startDateDetails.get(1);
        String timeZone = startDateDetails.get(2);
        String endDate = endDateDetails.get(0);
        String endTime = endDateDetails.get(1);
        System.out.println("\t\t\tSTART-DATE: " + startDate);
        System.out.println("\t\t\tSTART-TIME: " + startTime);
        System.out.println("\t\t\tEND-DATE  : " + endDate);
        System.out.println("\t\t\tEND-TIME  : " + endTime);
        System.out.println("\t\t\tTIME-ZONE : " + timeZone);
    }

    /**
     * Helper method to get DateTime Details in an Array List of Strings
     *
     * @param dateTime Google Calendar API DateTime object
     * @return Array List of Strings containing Date Details
     */
    private ArrayList<String> getDateDetails(DateTime dateTime) {
        Pattern pattern = Pattern.compile(
                "(\\d{4}-\\d{2}-\\d{2})|(\\d{2}:\\d{2}:\\d{2})|([+-]\\d{2}:\\d{2})");
        Matcher matcher = pattern.matcher(dateTime.toStringRfc3339());
        ArrayList<String> dateDetails = new ArrayList<>();
        while (matcher.find()) {
            dateDetails.add(matcher.group());
        }
        return dateDetails;
    }

    /**
     * Sets the bits for an event in the Timeline Bit Array
     * Also prints the Event interval details for reference
     *
     * @param event Google Calendar API event instance. Stores the details of an Event
     */
    private void setBitArrayIndices(Event event) {
        // Get start and end times
        DateTime start = event.getStart().getDateTime();
        DateTime end = event.getEnd().getDateTime();
        if (start == null) {
            start = event.getStart().getDate();
        }
        // Stores DateTime details as an array of strings
        ArrayList<String> startDateDetails = getDateDetails(start);
        ArrayList<String> endDateDetails = getDateDetails(end);
        // Store required details for easy reference
        String startDate = startDateDetails.get(0);
        String startTime = startDateDetails.get(1);
        String timeZone = startDateDetails.get(2);
        String endDate = endDateDetails.get(0);
        String endTime = endDateDetails.get(1);
        // Stores date time as a standard pattern
        String startDateTime = startDate + " " + startTime;
        String endDateTime = endDate + " " + endTime;
        LocalDateTime localStart = LocalDateTime.parse(startDateTime,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime localEnd = LocalDateTime.parse(endDateTime,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime trueStart = LocalDateTime.parse(startTimeString,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        // Calculate milliseconds details
        long startMillis = localStart
                .atZone(ZoneId.of(timeZone))
                .toInstant().toEpochMilli();
        long endMillis = localEnd
                .atZone(ZoneId.of(timeZone))
                .toInstant().toEpochMilli();
        long duration = endMillis - startMillis;
        long trueStartMillis = trueStart
                .atZone(ZoneId.of(timeZone))
                .toInstant().toEpochMilli();
        long relativeStartMillis = startMillis - trueStartMillis;
        // Convert to minutes and intervals
        int trueMinutes = (int) (((relativeStartMillis/1000)/60));
        int minutes = (int) (((duration/1000)/60));
        int hour = (int) Math.floor(minutes/60);
        int interval = minutes/(60/4);
        int intervalPosition = trueMinutes/(60/4);
        // Print event minute and interval details
        System.out.println("\t\tEVENT INTERVAL DETAILS : ");
        System.out.println("\t\t\tHOUR             : " + hour);
        System.out.println("\t\t\tMINUTES          : " + (minutes - (hour * 60)));
        System.out.println("\t\t\tTOTAL MINUTES    : " + minutes);
        System.out.println("\t\t\tINTERVAL POSITION: " + intervalPosition);
        System.out.println("\t\t\tSTEPS            : " + interval);
        System.out.println("---------------------------------------");
        // Set timeline bit array indices
        timelineBitArray.set(intervalPosition, intervalPosition + interval);
    }

    /**
     * Prints out the Timeline Bit Array
     * @param intervals Specifies per hour intervals
     */
    public void showTimelineBitArray(int intervals) {
        StringBuilder bitString = new StringBuilder();
        for (int i = 0; i < bitArrayLength; i++) {
            if (timelineBitArray.get(i)) {
                bitString.append(1);
            } else {
                bitString.append(0);
                if (checkSlot(i, intervals) && i <= bitArrayLength - intervals) {
                    positionArray.add(i);
                }
            }

        }
        System.out.println("TIMELINE ARRAY: ");
        System.out.println(bitString);
        System.out.println("\tTIMELINE ARRAY LENGTH: ");
        System.out.println("\t" + bitArrayLength);
    }

    /**
     * Checks timeline array for a meeting slot specified number of intervals
     *
     * @param index Timeline bit array index
     * @param intervals Meeting interval
     * @return True if a slot exists in the timeline
     */
    private boolean checkSlot(int index, int intervals) {
        BitSet bitSet = new BitSet(intervals);
        BitSet timelineBitSet = timelineBitArray.get(index, index + intervals);
        bitSet.or(timelineBitSet);
        return bitSet.isEmpty();
    }

    /**
     * Returns the position array with interval information about possible meeting slots
     * @return Array of position of possible meeting slots
     */
    public ArrayList<Integer> getNoConflictPositions() {
        return positionArray;
    }

    /**
     * Returns the position array with interval information about possible meeting slots
     */
    public void showNoConflictPositions(int interval) {
        System.out.println("POSSIBLE MEETING SLOTS: ");
        for (int i:positionArray) {
            System.out.println("SLOT " + (i) + ": {" + (i + 1) + " - " + (i + interval - 1) + "}");
        }
    }
}