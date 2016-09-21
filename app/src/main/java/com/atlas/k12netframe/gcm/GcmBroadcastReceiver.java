package com.atlas.k12netframe.gcm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
        // Explicitly specify that GcmIntentService will handle the intent.
        if( intent != null && "com.google.android.c2dm.intent.REGISTRATION".equals(intent.getAction())) {

            String registrationId = intent.getStringExtra("registration_id");
            String error = intent.getStringExtra("error");

            if( error == null && context != null) {
                //Utils.saveRegistrationID( context, registrationId );
            }

        }
        else if ( intent != null && "com.google.android.c2dm.intent.RECEIVE".equals(intent.getAction()) ) {

            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
            String messageType = gcm.getMessageType(intent);

            // Filter messages based on message type. It is likely that GCM will be extended in the future
            // with new message types, so just ignore message types you're not interested in, or that you
            // don't recognize.
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                // It's an error.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                // Deleted messages on the server.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

                ComponentName comp = new ComponentName(context.getPackageName(),
                        GcmIntentService.class.getName());
                // Start the service, keeping the device awake while it is launching.
                startWakefulService(context, (intent.setComponent(comp)));
                setResultCode(Activity.RESULT_OK);

            }

        }

   /*     GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

	        // The getMessageType() intent parameter must be the intent you received
	        // in your BroadcastReceiver.
	
	        String messageType = gcm.getMessageType(intent);  
	
	    if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
	            // Logic
	    } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
	            // Logic
	    } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
	            // Logic
	    }*/
    }

}
