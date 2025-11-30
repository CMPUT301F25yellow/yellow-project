package com.example.yellow.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationManagerRecipientNamesTest {

    @Test
    public void buildRecipientNames_usesNamesWhenAvailableAndFallbackOtherwise_inOrder() {
        // Arrange: three userIds, but only two have profile names.
        List<String> userIds = Arrays.asList(
                "abc123456",
                "def789",
                "xyz"
        );

        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("abc123456", "Alice Nguyen");
        // "def789" will be missing -> should use fallback
        nameMap.put("xyz", "Zoe Li");

        // Act: we only care about the first 3 (all of them here)
        List<String> recipientNames =
                NotificationManager.buildRecipientNames(userIds, nameMap, 10);

        // Assert: we keep the order of userIds,
        // use real names when present, and fallback "User <prefix>" otherwise.
        assertEquals(3, recipientNames.size());
        assertEquals("Alice Nguyen", recipientNames.get(0));
        assertEquals("User def789", recipientNames.get(1)); // fallback for missing name
        assertEquals("Zoe Li", recipientNames.get(2));
    }
}
