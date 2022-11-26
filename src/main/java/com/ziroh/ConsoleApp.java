package com.ziroh;

import com.google.api.services.calendar.Calendar;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class ConsoleApp {
    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client calendarService.
        MeetingScheduler meetingScheduler = new MeetingScheduler();
        Calendar calendarService = meetingScheduler.buildCalendarService();
        meetingScheduler.getAllCalendarEventsList(calendarService);
        meetingScheduler.showTimelineBitArray(4);
        meetingScheduler.getNoConflictPositions();
    }
}
