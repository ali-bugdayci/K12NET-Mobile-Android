package com.atlas.k12net.gcm;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.atlas.k12net.LoginActivity;
import com.atlas.k12net.R;
import com.atlas.k12net.async_tasks.AsistoAsyncTask;
import com.atlas.k12net.utils.definition.AsistoStaticDefinition;
import com.atlas.k12net.utils.userSelection.AsistoUserReferences;
import com.atlas.k12net.utils.webConnection.AsistoHttpClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class GCMRegisterAsyncTask extends AsistoAsyncTask {
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    static String SENDER_ID = "1046541990163";
    private static final String TAG = "GCMRegister";

    Context context;
    String msg = "";

    public GCMRegisterAsyncTask(Context context) {
        this.context = context;
    }

    boolean isGooglePlayServicesAvailable = false;

    private static boolean checkPlayServices(Context context) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Toast.makeText(context, context.getResources().getString(R.string.error_gcm_installplayservices), Toast.LENGTH_SHORT).show();
                //GooglePlayServicesUtil.getErrorDialog(resultCode, context,
                 //       PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.error_gcm_devicenotsupported), Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        return true;
    }

    private static String getRegistrationId(Context context) {
        String registrationId = AsistoUserReferences.getAsistoRegisterId();
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = AsistoUserReferences.getAsistoVersionNo();
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        isGooglePlayServicesAvailable = false;

        if(checkPlayServices(context)) {
            isGooglePlayServicesAvailable = true;
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
       // if (checkPlayServices(context) == false) return null;

        try {
            boolean needsNewRegistrationID = false;

            if(isGooglePlayServicesAvailable) {

                if (AsistoUserReferences.getAsistoRegisterId().isEmpty()) {
                    needsNewRegistrationID = true;
                } else {
                    int registeredVersion = AsistoUserReferences.getAsistoVersionNo();
                    int currentVersion = getAppVersion(context);
                    if (registeredVersion != currentVersion) {
                        needsNewRegistrationID = true;
                    }
                }
            }

            if (needsNewRegistrationID) {
                GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

                String regId = gcm.register(SENDER_ID);
                msg = "Device registered, registration ID=" + regId;

                // For this demo: we don't need to send it because the device
                // will send upstream messages to a server that echo back the
                // message using the 'from' address in the message.

                // Persist the regID - no need to register again.

                int appVersion = getAppVersion(context);
                AsistoUserReferences.setAsistoRegisterId(regId);
                AsistoUserReferences.setAsistoVersionNo(appVersion);
            }

            //---------------------------
            String registrationId = AsistoUserReferences.getAsistoRegisterId();
            if(registrationId.isEmpty()) {
                registrationId = "-";
            }

            String requestQuery = AsistoUserReferences.getConnectionAddress() + "/SPSL.Web/ClientBin/Yuce-K12NET-SPServicesLibrary-SPDomainService.svc/json/SubmitChanges";

            String electronicIdJson = "{\"changeSet\": [{\"HasMemberChanges\": 0, \"Id\": 0, \"Operation\": 2, \"Entity\": {\"__type\": \"ElectronicId:#Yuce.K12NET.SPServicesLibrary\", \"ID\": \"00000000-0000-0000-0000-000000000000\", \"TypeID\": \""+AsistoStaticDefinition.ASISTO_ANDROID_APPLICATION_ID+"\", \"Value\": \""+registrationId+"\"}, \"Associations\": [{\"Key\": \"PersonalInfo_ElectronicIds\", \"Value\": [1]}]}, {\"HasMemberChanges\": 0, \"Id\": 1, \"Operation\": 2, \"Entity\": {\"__type\": \"PersonalInfo_ElectronicId:#Yuce.K12NET.SPServicesLibrary\", \"PersonalInfoID\": \""+ LoginActivity.providerId+"\", \"ElectronicIdID\": \"00000000-0000-0000-0000-000000000000\"}, \"Associations\": [{\"Key\": \"ElectronicId\", \"Value\": [0]}]}]}";

            String line = AsistoHttpClient.execute(requestQuery, electronicIdJson);

            Log.d("REGISTER", line);


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onAsyncComplete() {
        if (!msg.isEmpty()) {
            Log.d(TAG, msg);
            Toast.makeText(context, R.string.gcm_register_ok, Toast.LENGTH_SHORT).show();
        }
    }
}
