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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.licketycut.floatingviewexample.floatingviews.FloatingButtonView;
import com.licketycut.floatingviewexample.floatingviews.FloatingInfoView;
import com.licketycut.floatingviewexample.utils.SharedPreferencesUtil;

/**
 * Example activity which demonstrates {@link FloatingView}s
 * using the {@link FloatingViewService}.
 */
public class MainActivity extends AppCompatActivity {
    private final String TAG = "FloatingViewExample";

    // Flag to indicate permission activity result.
    private final int REQUEST_CODE_CHECK_DRAW_OVERLAY_PERM = 101;

    private FloatingViewService floatingViewService;
    private boolean floatingViewBound;

    // Which floating view we want to start if called back from permission request.
    private FloatingView currentFloatingView;

    private FloatingButtonView floatingButtonView;
    // Flag to indicate exit menu chosen for floating button view in our broadcast receiver.
    private final String FLOATING_VIEW_MENU_EXIT = "FLOATING_VIEW_MENU_EXIT";
    // Flag to indicate on click for floating button view in our broadcast receiver.
    private final String FLOATING_VIEW_BUTTON_ONCLICK = "FLOATING_VIEW_BUTTON_ONCLICK";
    private int floatingViewButtonNumClicks = 0;

    private FloatingInfoView floatingInfoView;
    private String floatingInfoViewTitleText;
    private String floatingInfoViewInfoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bindFloatingViewService();

        setupViews(getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE);
    }

    /**
     * Setup views based on current orientation.
     */
    public void setupViews(boolean landscape) {
        if (landscape) {
            setContentView(R.layout.activity_main_landscape);
        } else {
            setContentView(R.layout.activity_main_portrait);
        }

        // Setup the action bar which includes the options menu.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize info title and text strings as necessary and setup text changed listener.
        final EditText floatingInfoTitle = findViewById(R.id.edit_text_floating_info_title);
        if (floatingInfoViewTitleText == null) {
            floatingInfoViewTitleText = floatingInfoTitle.getText().toString();
        }
        floatingInfoTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence,
                                          int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                floatingInfoViewTitleText = charSequence.toString();
            }
        });

        final EditText floatingInfoText = findViewById(R.id.edit_text_floating_info_text);
        if (floatingInfoViewInfoText == null) {
            floatingInfoViewInfoText = floatingInfoText.getText().toString();
        }
        floatingInfoText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence,
                                          int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                floatingInfoViewInfoText = charSequence.toString();
            }
        });

        // Setup up our button click listeners to start floating views.
        Button startFloatingButtonA = findViewById(R.id.button_start_floating_button);
        startFloatingButtonA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String buttonText = getResources().getString(R.string.button_floating_view);
                floatingButtonView = new FloatingButtonView(getApplicationContext(), buttonText,
                        FLOATING_VIEW_BUTTON_ONCLICK, FLOATING_VIEW_MENU_EXIT);
                startFloatingView(floatingButtonView);
            }
        });

        Button startFloatingButtonB = findViewById(R.id.button_start_floating_info);
        startFloatingButtonB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String title = floatingInfoTitle.getText().toString();
                String text = floatingInfoText.getText().toString();
                floatingInfoView = new FloatingInfoView(getApplicationContext(), title, text);
                startFloatingView(floatingInfoView);
            }
        });

        updateTextViews();
    }

    /**
     * Update text views to reflect number of floating button view clicks
     * and current info title and text strings.
     */
    public void updateTextViews() {
        TextView floatingViewButtonClicks = findViewById(R.id.text_num_button_clicks);
        String numClicks = getResources().getQuantityString(R.plurals.text_num_times,
                floatingViewButtonNumClicks, floatingViewButtonNumClicks);
        floatingViewButtonClicks.setText(numClicks);

        EditText floatingInfoTitle = findViewById(R.id.edit_text_floating_info_title);
        floatingInfoTitle.setText(floatingInfoViewTitleText);

        EditText floatingInfoText = findViewById(R.id.edit_text_floating_info_text);
        floatingInfoText.setText(floatingInfoViewInfoText);
    }

    /**
     * Inflate the action bar menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Handle action bar menu selections.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() ==R.id.action_exit) {
            // Unbind floating view service and end the activity.
            unbindFloatingViewService();
            finish();
            return true;
        }

        // We didn't process the event so send it along to super.
        return super.onOptionsItemSelected(item);
    }

    /**
     * Check for permissions and then attach the floating view.
     */
    private void startFloatingView(FloatingView floatingView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // If user has Marshmallow or above,
            // set our global variable since we need to wait for callback from permissions check.
            this.currentFloatingView = floatingView;
            //Check for draw overlay permission then if we have it then start the floating view.
            onCheckDrawOverlayPermissionResult();
        } else {
            attachFloatingView(floatingView);
        }
    }

    /**
     * Attach the floating view and start the foreground service.
     */
    private void attachFloatingView(FloatingView floatingView) {
        if (floatingView != null) {
            floatingView.attachToWindow(getApplicationContext(), true);
            moveTaskToBack(false);
        }
    }

    /**
     * Start and bind to the floating view service.
     */
    private void bindFloatingViewService() {
        if (!floatingViewBound) {
            Intent intent = new Intent(this, FloatingViewService.class);
            floatingViewBound = bindService(intent, floatingViewServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindFloatingViewService() {
        if (floatingViewBound) {
            unbindService(floatingViewServiceConnection);
            floatingViewBound = false;
        }
    }

    /**
     * Define callbacks for service binding.
     */
    private final ServiceConnection floatingViewServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to the floating view service,
            // cast the binder and get the floating view service instance.
            FloatingViewService.LocalBinder binder = (FloatingViewService.LocalBinder) service;
            floatingViewService = binder.getService();

            // Create an intent using floating view service INTENT_CLOSE flag
            // and register it with our receiver.
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(FLOATING_VIEW_BUTTON_ONCLICK);
            intentFilter.addAction(FLOATING_VIEW_MENU_EXIT);
            intentFilter.addAction(FloatingViewService.INTENT_CLOSE);
            LocalBroadcastManager.getInstance(floatingViewService)
                    .registerReceiver(floatingViewServiceReceiver, intentFilter);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            unregisterReceiver(floatingViewServiceReceiver);
            floatingViewService = null;
        }
    };

    /**
     * Receive broadcasts from the floating view service.
     */
    private final BroadcastReceiver floatingViewServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (intent.getAction()) {
                    case FLOATING_VIEW_BUTTON_ONCLICK:
                        floatingViewButtonNumClicks += 1;
                        Toast.makeText(context, "Main Activity: Floating Button clicked",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case FLOATING_VIEW_MENU_EXIT:
                        finish();
                        break;
                    case FloatingViewService.INTENT_CLOSE:
                        // If floating view service has broadcast intent to close,
                        // then we are no longer bound.
                        floatingViewBound = false;
                        break;
                }
            }
        }
    };

    /**
     * Called back with results of our permission request.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if(requestCode ==REQUEST_CODE_CHECK_DRAW_OVERLAY_PERM) {
            // We've been given permission so start the floating view.
            if (resultCode == RESULT_OK) {
                if (currentFloatingView != null) {
                    attachFloatingView(currentFloatingView);
                }
            } else if (resultCode != RESULT_CANCELED) {
                // We've been denied permission, inform the user and try again.
                // Display a dialog to the user for permission request.
                showPermissionDialog(
                        getString(R.string.dialog_content_need_draw_overlay_permission));
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Set shared preference flag to show that the floating view is no longer running
        // and this activity is back in front.
        SharedPreferencesUtil.getInstance().setIsAppShowing(this, true);

        String INTENT_START_FROM_NOTIFY = getString(R.string.INTENT_START_FROM_NOTIFY);
        if (intent != null && intent.hasExtra(INTENT_START_FROM_NOTIFY)) {
            if (intent.getBooleanExtra(INTENT_START_FROM_NOTIFY, false)) {
                // If we've been sent a notification to resume
                // from the floating view service foreground notification,
                // then detach our floating views and dismiss the foreground notification.
                if (floatingViewBound) {
                    if (floatingViewService != null) {
                        floatingViewService.detachAllFloatingViews();
                    }
                }

                // We need to duplicate the calling intent and remove the INTENT_START_FROM_NOTIFY
                // otherwise it may be consumed here again.
                Intent newIntent = (Intent) intent.clone();
                newIntent.removeExtra(INTENT_START_FROM_NOTIFY);
                // Set the modified intent so that this code is not reached again
                // until it it called by the floating view service foreground notification.
                setIntent(newIntent);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindFloatingViewService();
    }

    /**
     * Update our views on configuration change. This keeps us bound to our service
     * while providing horizontal and landscape friendly layouts.
     **/
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupViews(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    /**
     * Detach the floating views when the main activity is in the foreground.
     */
    @Override
    public void onResume() {
        super.onResume();
        // If we are returning to the foreground,
        // then detach the floating views and dismiss the foreground notification.
        SharedPreferencesUtil.getInstance().setIsAppShowing(this, true);
        if (floatingViewBound) {
            if (floatingViewService != null) {
                floatingViewService.detachAllFloatingViews();
            }
        }

        updateTextViews();
    }

    /**
     * If the user is running Marshmallow or above we need to ask for permission to draw overlays.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void onCheckDrawOverlayPermissionResult() {
        // If we've been granted draw overlay permission then start the floating view.
        if (Settings.canDrawOverlays(this)) {
            attachFloatingView(currentFloatingView);
        } else {
            // Display a dialog to the user for permission request.
            showPermissionDialog(
                    getString(R.string.dialog_content_need_draw_overlay_permission));
        }
    }

    /**
     * Show info dialog so that the user understands we are sending them to permission settings.
     *
     * @param message   Message to display to user.
     */
    private void showPermissionDialog(String message) {
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setTitle(R.string.dialog_title_accept_permission);
        ab.setMessage(message);
        ab.setPositiveButton(getString(R.string.button_settings),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestDrawOverlayPermission();
                        }
                    });

        // If the user chooses not to go to permission settings then exit the app.
        ab.setNegativeButton(R.string.button_exit_app, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.this.finish();
            }
        });
        ab.show();
    }

    /**
     * Generate and start an intent which leads the user to permission settings.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void requestDrawOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));

        try {
            startActivityForResult(intent, REQUEST_CODE_CHECK_DRAW_OVERLAY_PERM);
            // Starting with API 26, the settings activity result is not always returned,
            // so we will start a background thread to listen for the change.
            handler.postDelayed(checkSettingOn, 1000);
        } catch (ActivityNotFoundException e) {
            // If we can't send the user to the permission settings,
            // then alert them and try to send them to our app settings.
            e.printStackTrace();
            AlertDialog.Builder ab1 = new AlertDialog.Builder(getApplicationContext());
            ab1.setTitle(R.string.error);
            ab1.setMessage(R.string.error_open_manage_overlay_permission_failed);
            ab1.setPositiveButton(R.string.button_open_applications_page,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                    startActivityForResult(intent, REQUEST_CODE_CHECK_DRAW_OVERLAY_PERM);
                }
            });
            // If user declines to proceed to app settings then exit the app.
            ab1.setNegativeButton(R.string.button_close, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.this.finish();
                }
            });
            ab1.show();
        }
    }

    final Handler handler = new Handler();

    // Starting with API 26, the settings activity result is not always returned,
    // so this background thread listens for the change and passes it along.
    @TargetApi(Build.VERSION_CODES.M)
    final
    Runnable checkSettingOn = new Runnable() {
        @Override
        public void run() {
            if (Settings.canDrawOverlays(MainActivity.this)) {
                // We have the permission, so forward the result.
                onActivityResult(REQUEST_CODE_CHECK_DRAW_OVERLAY_PERM, RESULT_OK, getIntent());
                return;
            }
            handler.postDelayed(this, 200);
        }
    };
}
