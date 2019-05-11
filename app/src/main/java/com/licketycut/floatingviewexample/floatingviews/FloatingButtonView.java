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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import android.view.SoundEffectConstants;
import android.widget.Button;

import com.licketycut.floatingviewexample.FloatingView;
import com.licketycut.floatingviewexample.FloatingViewTouchListener;
import com.licketycut.floatingviewexample.MainActivity;
import com.licketycut.floatingviewexample.R;



/** Extension of our {@link FloatingView} class which consists of a button and popup menu. */
// We understand the implications, our extended touch listener issues performClick() as necessary.
@SuppressLint("ClickableViewAccessibility")
public class FloatingButtonView extends FloatingView {
    private final String TAG ="FloatingButtonView";

    /**
     * Initialize the floating button view.
     * @param context           Context which to attach.
     * @param buttonText        Button text.
     * @param onClickAction     Action string to broadcast on click.
    */
    public FloatingButtonView(final Context context, String buttonText,
                              final String onClickAction, final String onExitAction) {
        // Call floating view superclass first to initialize the root view.
        super(context, R.layout.floating_button);

        final Button button = getRootView().findViewById(R.id.button_floating_view);
        button.setText(buttonText);

        // Listener which allows the button to be moved around the screen 
        // and has callbacks for user touch events.
        button.setOnTouchListener(new FloatingViewTouchListener(this,
                // We don't want to handle gestures, just movement and clicks.
                FloatingViewTouchListener.IGNORE_GESTURES){

            // If our button has been clicked but not dragged then broadcast our on click message.
            @Override
            public boolean onClick(){
                    // Since we are handling clicks with a custom callback,
                    // we have to issue the click sound manually.
                    AudioManager audioManager =
                            (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager != null) {
                        // Click only sounds if user has touch sounds enabled.
                        audioManager.playSoundEffect(SoundEffectConstants.CLICK);
                    }
                broadcastOnClick(onClickAction);
                return true;
            }

            // If our button has been long clicked but not dragged then open the popup menu.
            @Override
            public boolean onLongPress(){
                new FloatingButtonViewMenu(getRootView().getContext(), button,
                        R.menu.menu_floating_button,
                        new FloatingButtonViewMenu.OnFloatingButtonMenuCallback() {

                    @Override
                    public void onShowMainItemClick() {
                        // Create an intent to start the the main activity.
                        Intent intent = new Intent(getRootView().getContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        // Indicate to the main activity
                        // that we want the notification to be dismissed.
                        try {
                            // Send the intent.
                            PendingIntent pendingIntent
                                    = PendingIntent.getActivity(getRootView().getContext(),
                                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            pendingIntent.send();
                            detachFromWindow(true);
                        } catch(PendingIntent.CanceledException e) {
                            Log.w(TAG, "Pending intent to start main activity failed : "
                                    +e.getMessage());
                        }
                    }

                    /** User has chosen to exit the app from the floating button view. */
                    @Override
                    public void onCloseItemClick() {
                        // Detach ourselves and dismiss the notification.
                        detachFromWindow(true);
                        broadcastOnClick(onExitAction);
                        // Unbind from the floating view service.
                        unbindFloatingViewService();
                    }
                }).show();
                return true;
            }

        });
    }
}

