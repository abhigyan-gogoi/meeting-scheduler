package com.ziroh.common;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.CalendarScopes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CalendarDetails {
    /**
     * Application name.
     */
    private final String appName = "Google Calendar API Java Quickstart";

    /**
     * Global instance of the JSON factory.
     */
    private final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    /**
     * Directory to store authorization tokens for this application.
     */
    private final String TokensDirPath = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private final List<String> scopes =
            Collections.singletonList(CalendarScopes.CALENDAR_READONLY);

    private final String credFilePath = "/credentials.json";

    private final List<String> calendarIdList =
            new ArrayList<>(List.of(
                    "c_5fbc6e88b1213bcd40fb097220944c98e4ef69384de512da4ab48be4c291ff93@group.calendar.google.com",
                    "c_96524e5b976f03f83610e947c5526e4f3388bc897ba8707cf9c69fccd136ae0e@group.calendar.google.com",
                    "c_1be3acae48fb49ab76e8375b58a164cd16552dbe9cab175af2442a3f11ee7fc9@group.calendar.google.com")
            );

    public String getAppName() {
        return appName;
    }

    public JsonFactory getJsonFactory() {
        return jsonFactory;
    }

    public String getTokensDirPath() {
        return TokensDirPath;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public String getCredFilePath() {
        return credFilePath;
    }

    public List<String> getCalendarIdList() {
        return calendarIdList;
    }
}
