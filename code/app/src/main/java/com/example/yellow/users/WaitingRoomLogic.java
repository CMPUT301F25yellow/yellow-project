package com.example.yellow.users;

/**
 * Logic for the waiting room.
 * @author Waylong Wang - waylon1
 */
public class WaitingRoomLogic {
    /**
     * Returns true if the user should join the waiting room.
     * @param exists: true if the user is already in the waiting room
     * @return true if the user should join the waiting room
     */
    public boolean shouldJoin(boolean exists) {
        return !exists;
    }

    /**
     * Adjusts the waitlist count based on whether the user is leaving or joining.
     * @param currentCount: the current waitlist count
     * @param leaving: true if the user is leaving, false if they are joining
     * @return the adjusted waitlist count
     */
    public int adjustWaitlistCount(int currentCount, boolean leaving) {
        return leaving ? currentCount - 1 : currentCount + 1;
    }
}
