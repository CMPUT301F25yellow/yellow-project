package com.example.yellow.organizers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.example.yellow.users.WaitingRoomLogic;

public class WaitingRoomLogicTest {
    //test for waiting room

    private WaitingRoomLogic logic;

    @Before
    public void setup() {
        logic = new WaitingRoomLogic();
    }

    @Test
    public void shouldJoin_WhenUserNotInWaitingList_ReturnsTrue() {
        Assert.assertTrue(logic.shouldJoin(false));
    }

    @Test
    public void shouldJoin_WhenUserAlreadyExists_ReturnsFalse() {
        Assert.assertFalse(logic.shouldJoin(true));
    }

    @Test
    public void adjustWaitlistCount_Join_Increments() {
        int updated = logic.adjustWaitlistCount(5, false);
        Assert.assertEquals(6, updated);
    }

    @Test
    public void adjustWaitlistCount_Leave_Decrements() {
        int updated = logic.adjustWaitlistCount(5, true);
        Assert.assertEquals(4, updated);
    }
}
