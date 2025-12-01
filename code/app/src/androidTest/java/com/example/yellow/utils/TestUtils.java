package com.example.yellow.utils;

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;
//author: waylon
/**
 * Utility class for common test operations
 */
public class TestUtils {

    /**
     * Custom ViewAction to wait for a view
     */
    public static ViewAction waitFor(long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Wait for " + millis + " milliseconds.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }

    /**
     * Custom ViewAction to get text from TextView
     */
    public static String getText(final View view) {
        if (view instanceof android.widget.TextView) {
            return ((android.widget.TextView) view).getText().toString();
        }
        return "";
    }

    /**
     * RecyclerView matcher for testing lists
     */
    public static class RecyclerViewMatcher {
        private final int recyclerViewId;

        public RecyclerViewMatcher(int recyclerViewId) {
            this.recyclerViewId = recyclerViewId;
        }

        public Matcher<View> atPosition(final int position) {
            return atPositionOnView(position, -1);
        }

        public Matcher<View> atPositionOnView(final int position, final int targetViewId) {
            return new org.hamcrest.TypeSafeMatcher<View>() {
                @Override
                protected boolean matchesSafely(View view) {
                    androidx.recyclerview.widget.RecyclerView recyclerView =
                            view.getRootView().findViewById(recyclerViewId);
                    if (recyclerView == null || recyclerView.getId() != recyclerViewId) {
                        return false;
                    }

                    androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder =
                            recyclerView.findViewHolderForAdapterPosition(position);

                    if (viewHolder == null) {
                        return false;
                    }

                    View targetView = targetViewId == -1 ?
                            viewHolder.itemView :
                            viewHolder.itemView.findViewById(targetViewId);

                    return view == targetView;
                }

                @Override
                public void describeTo(org.hamcrest.Description description) {
                    description.appendText("with recycler view at position: " + position);
                }
            };
        }
    }
}