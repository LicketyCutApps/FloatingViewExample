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

package com.licketycut.floatingviewexample.floatingviews;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.licketycut.floatingviewexample.FloatingView;
import com.licketycut.floatingviewexample.FloatingViewTouchListener;
import com.licketycut.floatingviewexample.MainActivity;
import com.licketycut.floatingviewexample.R;

/** 
 * Extension of our {@link FloatingView} class which consists of 
 * an information rectangle with title and text.
 * Tap to return to MainActivity, swipe left or right to dismiss.
 * */
public class FloatingInfoView extends FloatingView {
    private final String TAG ="FloatingButtonView";

    /** Initialize our FloatingInfoView with title and text values. */
    public FloatingInfoView(Context context, String title, String text) {
        // Call FloatingView superclass first to initialize the root view.
        super(context, R.layout.floating_info);

        // Setup our FloatingView specific layout properties.
        setLayoutWidthMatchParent();
        setLayoutGravity(Gravity.TOP);
        allowFloatingViewOffScreen();

        TextView titleTextView =getRootView().findViewById(R.id.text_view_title);
        titleTextView.setText(title);

        TextView infoTextView =getRootView().findViewById(R.id.text_view_info);
        infoTextView.setText(text);

        setupViewListeners(getRootView());
    }

    /** Setup our touch, swipe and click listeners. */
    private void setupViewListeners(final View rootView) {
        rootView.setOnTouchListener(new FloatingViewTouchListener(this,
                // We only want horizontal touch events with gestures.
                FloatingViewTouchListener.IGNORE_VERTICAL ) {

            // On click the user has chosen to dismiss the view 
            // and return to the MainActivity.
            @Override
            public boolean onClick(){
                dismissAndReturnToMain();
                return true;
            }

            // On swipe gesture callbacks, begin animations to fling rootView off screen.
            // Add finish listener to cleanup.
            @Override
            public boolean onSwipeRight() {
                ObjectAnimator animation =ObjectAnimator.ofFloat(rootView, 
                                                    "translationX", rootView.getWidth());
                animation.setDuration(300);
                animation.addListener(animationListener);
                animation.start();

                return true;
            }

            @Override
            public boolean onSwipeLeft() {
                ObjectAnimator animation =ObjectAnimator.ofFloat(rootView, 
                                                    "translationX", -rootView.getWidth());
                animation.setDuration(300);
                animation.addListener(animationListener);
                animation.start();

                return true;
            }
        });
    }

    /** Handle Animation callbacks, we really only want animation end. */
    private final Animator.AnimatorListener animationListener =new Animator.AnimatorListener() {

        @Override public void onAnimationStart(Animator animation) {}
        @Override public void onAnimationRepeat(Animator animation) {}
        @Override public void onAnimationCancel(Animator animation) {}

        @Override public void onAnimationEnd(Animator animation) {
            // We should not destroy the view inside the animation listener UI thread
            // as it still holds a reference to the view.
            // So create a separate thread to do it.
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    detachFromWindow(true);
                }
            });

        }
    };

    private void dismissAndReturnToMain() {
        // Create an intent to start the main activity.
        Intent intent = new Intent(getRootView().getContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            // Send the intent.
            PendingIntent pendingIntent = PendingIntent.getActivity(getRootView().getContext(), 
                                                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            pendingIntent.send();
        } catch(PendingIntent.CanceledException e) {
            Log.w(TAG, "Pending intent to start Main Activity failed : "+e.getMessage());
        } finally {
            detachFromWindow(true);
        }
    }

}

