package com.lms.courseservice.configuration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleCalendarConfig {

    @Value("${google.service-account.credentials-file}")
    private String credentialsFile;

    @Value("${google.service-account.delegated-email}")
    private String delegatedEmail;

    @Value("${google.application-name:LMS-Application}")
    private String applicationName;

    public Calendar buildCalendarClient() throws GeneralSecurityException, IOException {
        try (InputStream credStream = loadCredentialsStream()) {
            ServiceAccountCredentials sa = (ServiceAccountCredentials)
                    ServiceAccountCredentials.fromStream(credStream)
                            .createScoped(Collections.singletonList(CalendarScopes.CALENDAR_EVENTS));
            GoogleCredentials delegated = sa.createDelegated(delegatedEmail);

            return new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(delegated))
                    .setApplicationName(applicationName)
                    .build();
        }
    }

    private InputStream loadCredentialsStream() throws IOException {
        if (credentialsFile.startsWith("classpath:")) {
            String path = credentialsFile.substring("classpath:".length());
            InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
            if (stream == null)
                throw new IOException("Không tìm thấy service account credentials: " + credentialsFile);
            return stream;
        }
        return new FileInputStream(credentialsFile);
    }
}
