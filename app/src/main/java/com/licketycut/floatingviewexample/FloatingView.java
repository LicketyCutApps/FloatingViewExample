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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import static android.content.Context.WINDOW_SERVICE;

/** Abstract superclass for floating views. */
public abstract class FloatingView {
    private final String TAG ="FloatingView";

    private final FloatingView instance =this;

    private FloatingViewService floatingViewService;
    private boolean floatingViewBound = false;

    // Switch to turn on the foreground notification when floating view is bound.
    private boolean startForeground = false;

    private final WindowManager windowManager;
    private final WindowManager.LayoutParams floatingLayoutParams;

    // The parent view that we will be working with.
    private final View rootView;
    private boolean isAttached = false;

    /**
     * Create the floating view.
     *
     * @param context Context which we will attach to.
     */
    protected FloatingView(Context context, int layoutId) {

        // Inflate the floating view resource which has been sent by the subclass.
        rootView = LayoutInflater.from(context).inflate(layoutId, null);
        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);

        // LayoutParams has changed the OVERLAY flag starting with Oreo.
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        floatingLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                // This is important to keep our floating view from hijacking the focus.
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        // Default is to start in the center of the screen.
        floatingLayoutParams.gravity = Gravity.CENTER;

        // Start and bind to floating view service.
        bindFloatingViewService();
    }

    /** Begin methods used to modify and update floating view layout params. */
    protected void allowFloatingViewOffScreen(){
        getFloatingLayoutParams().flags |=WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        updateLayoutParams();
    }

    protected void setLayoutWidthMatchParent(){
        getFloatingLayoutParams().width =WindowManager.LayoutParams.MATCH_PARENT;
        updateLayoutParams();
    }

    protected void setLayoutHeightMatchParent(){
        getFloatingLayoutParams().height =WindowManager.LayoutParams.MATCH_PARENT;
        updateLayoutParams();
    }

    protected void setLayoutWidth(int width) {
        getFloatingLayoutParams().width = width;
        updateLayoutParams();
    }

    protected void setLayoutHeight(int height) {
        getFloatingLayoutParams().height = height;
        updateLayoutParams();
    }

    protected int getLayoutX(){
        return getFloatingLayoutParams().x;
    }

    protected void setLayoutX(int x){
        getFloatingLayoutParams().x = x;
        updateLayoutParams();
    }

    protected int getLayoutY(){
        return floatingLayoutParams.y;
    }

    protected void setLayoutY(int y){
        getFloatingLayoutParams().y = y;
        updateLayoutParams();
    }

    protected int getLayoutGravity() {
        return getFloatingLayoutParams().gravity;
    }

    protected void setLayoutGravity(int gravity) {
        floatingLayoutParams.gravity = gravity;
        updateLayoutParams();
    }

    private WindowManager.LayoutParams getFloatingLayoutParams() {
        return floatingLayoutParams;
    }

    private void updateLayoutParams(){
        if(getFloatingLayoutParams() !=null) {
            if (windowManager != null) {
                if (isAttached) {
                    windowManager.updateViewLayout(getRootView(), getFloatingLayoutParams());
                }
            }
        }
    }
    /* End methods used to modify and update floating view layout params. */

    /** Begin methods to make root view and attached status available to the subclass. */
    protected View getRootView() {
        return rootView;
    }

    protected boolean isAttached() {
        return isAttached;
    }
    /* End methods to make root view and attached status available to the subclass.  */

    /** 
     * Attach our floating view to the current views in the window manager 
     * and start the foreground notification if requested.
     */
    protected synchronized void attachToWindow(final Context context, boolean startForeground) {
        if (!isAttached) {
            // Double check that we have draw overlay permission.
            if (checkDrawOverlayPermission(context)) {
                // Attach the floating view to the current views in the window manager.
                windowManager.addView(rootView, floatingLayoutParams);
                isAttached = true;

                if(startForeground) {
                    
                    if (floatingViewService !=null) {
                        // If the floating view service is already bound,
                        // then start the foreground notification.
                        floatingViewService.startForeground(context);
                    } else { 
                        // Otherwise set our switch 
                        // to start foreground notification when the service starts.
                        this.startForeground = true;
                    }
                }
            } else { 
                // Somehow we arrived here without draw overlay permission, 
                // return to the main activity.
                Intent intent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Log.w(TAG, "Attempt to attach FloatingView without DrawOverlay permission");
            }
        }
    }

    /** 
     * Detach the floating view from window manager views 
     * and dismiss the foreground notification if requested.
     */
    protected synchronized void detachFromWindow(boolean dismissNotification) {
        if (isAttached) {
            windowManager.removeView(rootView);
            isAttached = false;

            floatingViewService.removeFloatingView(this);
        }

        if (dismissNotification) {
            floatingViewService.dismissForegroundNotification();
        }
    }

    /** Simple test for draw overlay permission. */
    private boolean checkDrawOverlayPermission(Context context) {
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // If the device is running Marshmallow or above 
            // then draw overlay permission is required.
            return Settings.canDrawOverlays(context);
        } else {
            return true;
        }
    }

    /**  Start and bind to the floating view service. */
    private void bindFloatingViewService(){
        if(!floatingViewBound) {
            Context context =getRootView().getContext();
            Intent intent = new Intent(context, FloatingViewService.class);
            floatingViewBound =context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    /** If we are bound to the floating view service then unbind. */
    protected void unbindFloatingViewService() {
        if(floatingViewBound){
            getRootView().getContext().unbindService(connection);
            floatingViewBound = false;
        }
    }

    /**
     * Define callbacks for service binding, passed to bindService().
     */
    private final ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Context context = getRootView().getContext();
            // We've bound to floating view service, get the floating view service instance.
            FloatingViewService.LocalBinder binder = (FloatingViewService.LocalBinder) service;
            floatingViewService = binder.getService();

            floatingViewService.addFloatingView(instance);

            // Create an intent using floating view service INTENT_CLOSE flag
            // and register it to our broadcast receiver.
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(FloatingViewService.INTENT_CLOSE);
            context.registerReceiver(floatingViewServiceReceiver, intentFilter);

            if (startForeground) {
                floatingViewService.startForeground(context);
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            getRootView().getContext().unregisterReceiver(floatingViewServiceReceiver);
            floatingViewService =null;
        }
    };

    protected void broadcastOnClick(String onClickAction){
        if(floatingViewBound){
            floatingViewService.broadcastOnClick(onClickAction);
        }
    }

    /**
     * Receive broadcasts from the floating view service.
     */
    private final BroadcastReceiver floatingViewServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action !=null) {
                // If the floating view service has broadcast intent to close,
                // then we are no longer bound.
                if (intent.getAction().equals(FloatingViewService.INTENT_CLOSE)) {
                    floatingViewBound = false;
                }
            }
        }
    };
}
