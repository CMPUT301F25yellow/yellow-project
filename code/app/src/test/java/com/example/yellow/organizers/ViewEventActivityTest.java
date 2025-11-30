package com.example.yellow.organizers;

import android.content.Intent;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import com.example.yellow.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowToast;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class ViewEventActivityTest {

    @Test
    public void deepLink_showsEventIdToast_andInflatesTabs() {
        String id = "TEST_EVENT_ID_42";
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("yellow://event/" + id));
        i.setPackage(ApplicationProvider.getApplicationContext().getPackageName());

        ActivityController<ViewEventActivity> controller =
                Robolectric.buildActivity(ViewEventActivity.class, i).create().start().resume();

        ViewEventActivity act = controller.get();
        assertNotNull(act.findViewById(R.id.tabLayout));
        assertNotNull(act.findViewById(R.id.viewPager));

        String latestToast = ShadowToast.getTextOfLatestToast();
        assertEquals("eventId=" + id, latestToast);
    }
}
