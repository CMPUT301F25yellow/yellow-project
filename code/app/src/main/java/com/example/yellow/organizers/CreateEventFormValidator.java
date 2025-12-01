package com.example.yellow.organizers;

import java.util.Calendar;

/**
 * Validation logic for the CreateEventActivity form
 * @author Kien Tran - kht
 */
public final class CreateEventFormValidator {

    private CreateEventFormValidator() {}

    public static class Result {
        public boolean isValid;
        public String nameError;
        public String maxParticipantsError;
        public String maxEntrantsError;
        public String toastMessage;
    }

    public static Result validate(
            String rawName,
            String rawMaxParticipants,
            String rawMaxEntrants,
            Calendar startCal,
            Calendar endCal
    ) {
        Result r = new Result();
        String name = safeTrim(rawName);
        String maxParticipantsStr = safeTrim(rawMaxParticipants);
        String maxEntrantsStr = safeTrim(rawMaxEntrants);

        // 1. Name required
        if (name.isEmpty()) {
            r.nameError = "Required";
            return r;
        }

        // 2. maxParticipants required
        if (maxParticipantsStr.isEmpty()) {
            r.maxParticipantsError = "Required";
            return r;
        }

        // 3. maxParticipants must be integer > 0
        int maxParticipants;
        try {
            maxParticipants = Integer.parseInt(maxParticipantsStr);
            if (maxParticipants <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            r.maxParticipantsError = "Invalid number";
            return r;
        }

        // 4. End date cannot be before start date
        if (endCal.before(startCal)) {
            r.toastMessage = "End date cannot be before start date";
            return r;
        }

        // 5. maxEntrants optional; if present, must be integer >= 0
        int maxEntrants = 0; // 0 = no limit
        if (!maxEntrantsStr.isEmpty()) {
            try {
                maxEntrants = Integer.parseInt(maxEntrantsStr);
                if (maxEntrants < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                r.maxEntrantsError = "Invalid number";
                return r;
            }
        }

        // 6. If both set, require maxEntrants ≥ maxParticipants (unless 0 = no limit)
        if (maxEntrants != 0 && maxEntrants < maxParticipants) {
            r.maxEntrantsError = "Must be ≥ max participants";
            return r;
        }

        // All good
        r.isValid = true;
        return r;
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
