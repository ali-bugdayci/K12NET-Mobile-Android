package com.atlas.k12netframe.gcm;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.atlas.k12netframe.LoginActivity;
import com.atlas.k12netframe.R;
import com.atlas.k12netframe.WebViewerActivity;
import com.atlas.k12netframe.utils.definition.AsistoStaticDefinition;

import java.util.HashMap;
import java.util.List;

public class GcmIntentService extends IntentService {

    public static final int NOTIFICATION_ID = 1;
    private static final String TAG = null;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super(GCMRegisterAsyncTask.SENDER_ID);
    }

    static HashMap<String, Integer> messageTitleMap = null;

    HashMap<String, Integer> getMessageTitleMap() {
        if (messageTitleMap == null) {
            messageTitleMap = new HashMap<>();
            messageTitleMap.put(AsistoStaticDefinition.NotificationPartHomework, R.string.receive_homework);
            messageTitleMap.put(AsistoStaticDefinition.NotificationPartAnnouncement, R.string.receive_announcement);
            messageTitleMap.put(AsistoStaticDefinition.NotificationPartMessage, R.string.receive_message);
            messageTitleMap.put(AsistoStaticDefinition.NotificationPartAssessment, R.string.receive_assessment);
            messageTitleMap.put(AsistoStaticDefinition.NotificationPartAchievementGuide, R.string.receive_achievement_guide);
            messageTitleMap.put(AsistoStaticDefinition.NotificationPartSection, R.string.receive_section);
            messageTitleMap.put(AsistoStaticDefinition.NotificationPartCalendar, R.string.receive_calendar);
            messageTitleMap.put(AsistoStaticDefinition.NotificationPartActivity, R.string.receive_activity);
            messageTitleMap.put(AsistoStaticDefinition.NotificationPartBehaviorAssesment, R.string.receive_behavior_assessment);
            messageTitleMap.put(AsistoStaticDefinition.NotificationPartNone, R.string.receive_error);
        }
        return messageTitleMap;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Asisto", "Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Asisto", "Deleted messages on server: " +
                        extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
    /*            for (int i=0; i<5; i++) {
                    Log.i(TAG, "Working... " + (i+1)
                            + "/5 @ " + SystemClock.elapsedRealtime());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                }
                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());*/
                // Post notification of received message.

                String webPart = extras.getString("part", AsistoStaticDefinition.NotificationPartNone);
                String message = extras.getString("message", "");
                String message_en = extras.getString("message_en", message);
                String message_tr = extras.getString("message_tr", message);
                sendNotification(message_en, message_tr, isActivityRunning(WebViewerActivity.class), webPart);

                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String title, String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setSound(soundUri)
                        .setContentText(msg);

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg_en, String msg_tr, boolean isRunning, String webPart) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        long[] vibrate = { 0, 100, 200, 300 };

        PendingIntent contentIntent = null;

        if (isRunning) {
            Intent asisto_activity = new Intent(this, WebViewerActivity.class);
            asisto_activity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            contentIntent = PendingIntent.getActivity(this, 0,
                    asisto_activity, 0);

            Intent intent = new Intent("MainActivity");

            //put whatever data you want to send, if any
            intent.putExtra("message", webPart);

            //send broadcast
            getBaseContext().sendBroadcast(intent);
        } else {
            contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, LoginActivity.class), 0);
        }

        String msg = "undefined language";
        if(this.getResources().getString(R.string.localString).equals(getBaseContext().getString(R.string.localString))) {
            msg = msg_tr;
        }
        else {
            msg =msg_en;
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(this.getString(getMessageTitleMap().get(webPart)))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setSound(soundUri)
                        .setVibrate(vibrate)
                        .setContentText(msg);

        //.addAction(R.drawable.ninja, "View", pIntent)
        //.addAction(0, "Remind", pIntent)

        // If you want to hide the notification after it was selected, do the code below
        // myNotification.flags |= Notification.FLAG_AUTO_CANCEL;


        mBuilder.setAutoCancel(true);
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    public void cancelNotification(int notificationId) {

        if (Context.NOTIFICATION_SERVICE != null) {
            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(ns);
            nMgr.cancel(notificationId);
        }
    }

    protected Boolean isActivityRunning(Class activityClass) {
        ActivityManager activityManager = (ActivityManager) getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

        for (ActivityManager.RunningTaskInfo task : tasks) {
            if (activityClass.getCanonicalName().equalsIgnoreCase(task.topActivity.getClassName()))
                return true;
        }

        return false;
    }

}
