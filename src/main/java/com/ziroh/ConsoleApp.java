package com.ziroh;

import com.google.api.services.calendar.Calendar;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

public class ConsoleApp {
    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client calendarService.
        int interval = 4;
        MeetingScheduler meetingScheduler = new MeetingScheduler();
        Calendar calendarService = meetingScheduler.buildCalendarService();
        meetingScheduler.showAllCalendarEventsList(calendarService);
        meetingScheduler.showTimelineBitArray(interval);
        ArrayList<Integer> meetingPositionArray = meetingScheduler.getNoConflictPositions();
        meetingScheduler.showNoConflictPositions(interval);
    }
}
