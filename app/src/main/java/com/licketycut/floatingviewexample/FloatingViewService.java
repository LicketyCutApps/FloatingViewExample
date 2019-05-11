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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;

/** Service used to manage {@link FloatingView}s. */
public class FloatingViewService extends Service {
    private final static String TAG="FloatingViewService";
    // Used to broadcast an intention when the service is ended.
    // Public so it can be accessed by receivers to identify the broadcast.
    public final static String INTENT_CLOSE =TAG+".close";

    // Binder given to clients.
    private final IBinder binder = new LocalBinder();

    // We will keep a list of the floating views which we are attached to.
    private final ArrayList<FloatingView> floatingViews =new ArrayList<>();

    private boolean notificationShowing =false;

    protected void addFloatingView(FloatingView floatingView){
        floatingViews.add(floatingView);
    }

    protected void removeFloatingView(FloatingView floatingView){
        floatingViews.remove(floatingView);
        // If this is the last FloatingView attached
        if(floatingViews.size() ==0){
            dismissForegroundNotification();
        }
    }

    /** Detach any attached floating views. */
    protected void detachAllFloatingViews(){
        for(FloatingView floatingView: floatingViews){
            if(floatingView !=null){
                if(floatingView.isAttached()){
                    // This method removes the floating view from our array list
                    // after detaching it from the display.
                    // When the last one is detached, the foreground notification is removed.
                    floatingView.detachFromWindow(false);
                }
            }
        }
    }

    /** Send a broadcast to notify that a floating view root view has been clicked. */
    protected void broadcastOnClick(String clickAction){
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(clickAction));
    }

    /** Send a broadcast to notify that a floating view root view has been swiped. */
    protected void broadcastOnSwipe(String swipeAction){
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(swipeAction));
    }

    /**
     * Class used for the client binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to be a messenger,
     * instead we will use a local broadcast manager.
     */
    class LocalBinder extends Binder {
        FloatingViewService getService() {
            // Return this instance of the floating view service so clients can call public methods.
            return FloatingViewService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /** Handle os call to start the floating view service. */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // If memory becomes low and the floating view service is stopped,
        // tell os to recreate the service when memory is available again.
        return START_STICKY;
    }

    /** Stop the floating view service and cleanup. */
    public void stop() {
        detachAllFloatingViews();
        stopSelf();
        onDestroy();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        detachAllFloatingViews();

        // Send a broadcast to notify that floating view service is destroyed.
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(
                INTENT_CLOSE));
    }

    public boolean isNotificationShowing(){
        return notificationShowing;
    }

    /** Start our foreground notification. */
    public void startForeground(Context context) {
        startForeground(context.getResources().getInteger(R.integer.ongoing_notification_id), getForegroundNotification(context));
        notificationShowing = true;
    }

    /** Stop our foreground notification. */
    public void dismissForegroundNotification() {
        stopForeground(true);
        notificationShowing = false;
    }

    /**
     * Build a foreground notification which allows the user to return to the main activity
     * and close the current floating view.
     */
    private Notification getForegroundNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, getString(R.string.MAIN_CHANNEL_ID));
        // If the device is using Oreo or above then we need to create a notification channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(getString(R.string.MAIN_CHANNEL_ID),
                    "Floating View Service", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(channel);
            builder.setChannelId(getString(R.string.MAIN_CHANNEL_ID));
        }
        // Populate our notification.
        builder.setSmallIcon(R.mipmap.notify_icon);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.icon));
        builder.setTicker(getString(R.string.app_name));
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.notification_content));
        // Create an intent which restarts the main activity including a flag to indicate
        // that it came from the notification.
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        notificationIntent.putExtra(getString(R.string.INTENT_START_FROM_NOTIFY), true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        return builder.build();
    }
}
