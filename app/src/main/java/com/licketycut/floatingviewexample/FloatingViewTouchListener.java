/*
 * Copyright 2019 Adam Claflin [adam.r.claflin@gmail.com].
 *
 * Licensed under the Attribution-NonCommercial 4.0 International (CC BY-NC 4.0);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://creativecommons.org/licenses/by-nc/4.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.licketycut.floatingviewexample;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import android.os.Handler;

/**
 * Custom {@link View.OnTouchListener  } to process touch events and move a floating view,
 * recognize swipe gestures with {@link GestureDetector } and make callbacks as appropriate.
 */
public class FloatingViewTouchListener implements View.OnTouchListener {
    private static final String TAG="FloatingViewTouchListener";

    // Public flags to indicate which movements and gestures to handle.
    public static final int IGNORE_HORIZONTAL =0x01;
    public static final int IGNORE_VERTICAL =0x10;
    public static final int IGNORE_GESTURES =0x100;
    public static final int NO_SNAP_BACK =0x1000;

    // Handler and runnable to detect long presses.
    private final Handler handler = new Handler();
    private Runnable longPressHandler;

    private final GestureDetector gestureDetector;
    private final FloatingView floatingView;

    // Switches to indicate what we will process.
    private boolean processX =true;
    private boolean processY =true;
    private boolean processGestures =true;
    private boolean snapBack =true;

    /** Default constructor which handles horizontal and vertical movement and gestures.
     * @param floatingView FloatingView which we will be moving and making callbacks to.
     */
    public FloatingViewTouchListener(FloatingView floatingView){
        this.floatingView = floatingView;
        gestureDetector = new GestureDetector(floatingView.getRootView().getContext(),
                new GestureListener());
    }

    /**
     * Constructor for specifying process flags.
     * @param floatingView  FloatingView which we will be moving and making callbacks to.
     * @param flags         Process flags.
     */
    public FloatingViewTouchListener(FloatingView floatingView, int flags){
        this.floatingView = floatingView;
        gestureDetector = new GestureDetector(floatingView.getRootView().getContext(),
                new GestureListener());
        if((flags & IGNORE_HORIZONTAL) == IGNORE_HORIZONTAL){
            processX =false;
        }

        if((flags & IGNORE_VERTICAL) == IGNORE_VERTICAL){
            processY =false;
        }

        if((flags & IGNORE_GESTURES) == IGNORE_GESTURES){
            processGestures = false;
        }

        if((flags & NO_SNAP_BACK) == NO_SNAP_BACK){
            snapBack = false;
        }
    }

    /** Start handler for delayed callback to test for long press.*/
    private void startLongPressHandler(){
        // If the user has touched and held for longer than our long press threshold 
        // callback OnLongPress.
        longPressHandler = new Runnable() {
            @Override
            public void run() {
                onLongPress();
            }
        };
        handler.postDelayed(longPressHandler, LONG_PRESS_ACTION_THRESHOLD);
    }

    private void stopLongPressHandler(){
        handler.removeCallbacks(longPressHandler);
        longPressHandler = null;
    }

    // We only want to move the view if the user has actually dragged it a bit, not just touched it.
    private boolean hasMoved = false;
    // Initial x and y of the parent View.
    private float initX, initY;
    // Raw x and y values of users initial ACTION_DOWN touch event.
    private float initTouchX, initTouchY;
    // The threshold of pixel variance that we are looking for as an intention to drag.
    private static final int MOVEMENT_ACTION_THRESHOLD = 32;

    // Last time the user began an ACTION_DOWN touch event.
    private long lastTouchDown;
    // If we receive ACTION_DOWN followed by ACTION_UP within the threshold,
    // we'll consider it a click event.
    private static final int CLICK_ACTION_THRESHOLD = 200;
    private static final int LONG_PRESS_ACTION_THRESHOLD = 800;


    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if(hasMoved){
            // If we have a new event and the view has been moved,
            // then stop the runnable we have scheduled to test for long press.
            stopLongPressHandler();
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // User has started a chain of touch events by touching down.
                lastTouchDown = System.currentTimeMillis();
                hasMoved = false;
                initX = floatingView.getLayoutX();
                initY = floatingView.getLayoutY();
                initTouchX = event.getRawX();
                initTouchY = event.getRawY();

                startLongPressHandler();

                if(processGestures){
                    gestureDetector.onTouchEvent(event);
                }
                return true;
            case MotionEvent.ACTION_UP:
                // User has stopped touching.
                if (hasMoved) {
                    // Process gestures as requested.
                    if(processGestures && !gestureDetector.onTouchEvent(event)) {
                        if(snapBack) {
                            // If the event hasn't been processed yet,
                            // return to the initial coordinates as requested.
                            if (processX) {
                                floatingView.setLayoutX((int) initX);
                            }
                            if (processY) {
                                floatingView.setLayoutY((int) initY);
                            }
                        }

                    }
                } else{
                    // If user has stopped touching before the threshold,
                    // stop the runnable we have scheduled to test for long press.
                    stopLongPressHandler();
                    if(System.currentTimeMillis() - lastTouchDown < CLICK_ACTION_THRESHOLD){
                        // If the user has touched and released within our click threshold,
                        // forward the click to the view.
                        v.performClick();
                        // Event has been processed so finish.
                        return onClick();
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // User is currently moving the view.
                if (Math.abs(initTouchX - event.getRawX()) > MOVEMENT_ACTION_THRESHOLD
                        || Math.abs(initTouchY - event.getRawY()) > MOVEMENT_ACTION_THRESHOLD) {
                    // We've been dragged far enough to consider it an intentional drag event.
                    hasMoved = true;

                    // Calculate the next x and y positions based on movement
                    // relative to the initial touch.
                    int nextX = (int) (initX + (event.getRawX() - initTouchX));
                    int nextY = (int) (initY + (event.getRawY() - initTouchY));

                    // Set and update the new x and y of our parent floating view layout.
                    if (processX) {
                        floatingView.setLayoutX(nextX);
                    }
                    if (processY) {
                        floatingView.setLayoutY(nextY);
                    }
                    if (processGestures) {
                        // Process gestures as requested and finish.
                        return gestureDetector.onTouchEvent(event);
                    }
                    // Event has been processed and consumed, so finish.
                    return true;
                }
                break;
        }
        // Return false if we haven't consumed the event so it propagates to other handlers.
        // Returning true indicates that we've handled it.
        return hasMoved;
    }

    /** Detect user fling events and make callbacks as necessary. */
    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        // The velocity and movement thresholds which we recognize as swipes.
        private static final int SWIPE_THRESHOLD = 8;
        private static final int SWIPE_VELOCITY_THRESHOLD = 16;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;

            // Calculate the difference in x,y coordinates between event 1 and event 2.
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();

            if (Math.abs(diffX) > Math.abs(diffY)) {
                // If the difference in x is greater than y, check for horizontal gestures.
                if (Math.abs(diffX) > SWIPE_THRESHOLD
                        && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    // If the movement and velocity have met the threshold criteria,
                    // then make OnSwipe callbacks.
                    if (diffX > 0) {
                        result = onSwipeRight();
                    } else {
                        result = onSwipeLeft();
                    }
                }
            } else { 
                // If the difference in y is greater than x, check for vertical gestures.
                if (Math.abs(diffY) > SWIPE_THRESHOLD
                        && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    // If the movement and velocity have met the threshold criteria,
                    // then make OnSwipe callbacks.
                    if (diffY > 0) {
                        result = onSwipeDown();
                    } else {
                        result = onSwipeUp();
                    }
                }
            }
            // Return false if we haven't consumed the event so it propagates to other handlers.
            // Returning true indicates that we've handled it.
            return result;
        }
    }

    /** Default touch event and gesture callbacks
     * which return false indicating we have not acted on the events. */
    protected boolean onClick() { return false; }

    protected boolean onLongPress() { return false; }

    protected boolean onSwipeRight() { return false; }

    protected boolean onSwipeLeft() { return false; }

    protected boolean onSwipeUp() { return false; }

    protected boolean onSwipeDown() { return false; }
}
