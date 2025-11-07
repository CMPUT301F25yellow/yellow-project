package com.example.yellow.users;

public class WaitingRoomLogic {

    public boolean shouldJoin(boolean exists) {
        return !exists;
    }

    public int adjustWaitlistCount(int currentCount, boolean leaving) {
        return leaving ? currentCount - 1 : currentCount + 1;
    }
}
