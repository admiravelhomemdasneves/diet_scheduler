package com.dietscheduler.backend.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

@Service
public class GoogleTokenVerifierService {

    private final String rawClientIds;
    private GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierService(@Value("${dietscheduler.google.client-ids}") String rawClientIds) {
        this.rawClientIds = rawClientIds;
    }

    @PostConstruct
    void init() {
        List<String> audiences = Arrays.stream(rawClientIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        if (audiences.isEmpty()) {
            // No client IDs configured yet -- verification will fail closed until DIETSCHEDULER_GOOGLE_CLIENT_IDS is set.
            audiences = List.of("__unconfigured__");
        }
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(audiences)
                .build();
    }

    /** @return the verified payload, or empty if the token is invalid/expired/wrong audience */
    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new InvalidGoogleTokenException("Google ID token failed verification");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidGoogleTokenException("Could not verify Google ID token: " + e.getMessage());
        }
    }

    public static class InvalidGoogleTokenException extends RuntimeException {
        public InvalidGoogleTokenException(String message) {
            super(message);
        }
    }
}
