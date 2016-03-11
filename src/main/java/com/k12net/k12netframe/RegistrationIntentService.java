package com.k12net.k12netframe;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.JsonReader;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken("803175672697", GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i(TAG, "GCM Registration Token: " + token);

            // TODO: Implement this method to send any registration to your app's servers.
            sendRegistrationToServer(token, intent);

            // Subscribe to topic channels
            subscribeTopics(token);

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            sharedPreferences.edit().putBoolean("sentTokenToServer", true).apply();
            // [END register_for_gcm]
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean("sentTokenToServer", false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent("registrationComplete");
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     * <p/>
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token, Intent intent) {

        String instance = intent.getStringExtra("Url");
        String[] cookies = intent.getStringArrayExtra("Cookies");
        // Add custom implementation, as needed.

        RegisterTask registerTask = new RegisterTask();

        registerTask.execute(instance, cookies[0], token);
    }

    private class RegisterTask extends AsyncTask<String, Void, Boolean> {
        private static final String K12NETAndroidID = "9c260947-ba8f-e511-bf62-3c15c2ddcd05";
        private String instance;
        private String cookie;
        private String token;

        @Override
        protected Boolean doInBackground(String... params) {
            instance = params[0];
            cookie = params[1];
            token = params[2];

            return tryRegister();
        }

        private Boolean tryRegister() {
            try {
                URL url = new URL(instance + "/SPSL.Web/SPSL.Web/ClientBin/Yuce-K12NET-SPSL-Web-AuthenticationService.svc/json/GetUser");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("Cookie", this.cookie);

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }

                bufferedReader.close();

                JSONObject serviceResult = new JSONObject(stringBuilder.toString());
                JSONObject result = serviceResult.getJSONObject("GetUserResult");
                JSONObject user = result.getJSONArray("RootResults").getJSONObject(0);
                String ID = user.getString("ProviderUserKey");

                //url = new URL(instance + "/SPSL.Web/ClientBin/Yuce-K12NET-SPServicesLibrary-SPDomainService.svc/json/GetPersonalInfo_ElectronicIdsExpanded?propertyPath=ElectronicId&$where=it.PersonalInfoID==Guid(%22" + ID +"%22)");
                url = new URL(instance + "/SPSL.Web/ClientBin/Yuce-K12NET-SPServicesLibrary-SPDomainService.svc/json/GetPersonalInfo_ElectronicIdsExpanded?propertyPath=ElectronicId&$where=it.PersonalInfoID==Guid(%22" + ID + "%22)%2526%2526it.ElectronicId.TypeId==Guid(%22" + K12NETAndroidID + "%22) ");
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("Cookie", this.cookie);

                bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                stringBuilder = new StringBuilder();

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }

                bufferedReader.close();

                serviceResult = new JSONObject(stringBuilder.toString());
                result = serviceResult.getJSONObject("GetPersonalInfo_ElectronicIdsExpandedResult");
                JSONArray electronicIds = result.getJSONArray("IncludedResults");

                JSONObject electronicId = null;

                for (int index = 0; index < electronicIds.length(); index++) {
                    JSONObject item = electronicIds.getJSONObject(index);

                    if (token.equals(item.getString("Value"))) electronicId = item;
                }

                if (electronicId != null) return true;

                String changesSet = "{\"changeSet\":[{\"Entity\":{\"__type\":\"PersonalInfo_ElectronicId:#Yuce.K12NET.SPServicesLibrary\",\"ElectronicIdID\":\"00000000-0000-0000-0000-000000000000\",\"PersonalInfoID\":\"" + ID + "\"},\"HasMemberChanges\":false,\"Operation\":2,\"Id\":0,\"Associations\":[{\"Key\":\"ElectronicId\",\"Value\":[1]}]},{\"Entity\":{\"__type\":\"ElectronicId:#Yuce.K12NET.SPServicesLibrary\",\"ID\":\"00000000-0000-0000-0000-000000000000\",\"TypeID\":\"" + K12NETAndroidID + "\",\"Value\":\"" + token + "\"},\"HasMemberChanges\":false,\"Operation\":2,\"Id\":1}]}";

                byte[] postData = changesSet.getBytes(Charset.forName("UTF-8"));
                int postDataLength = postData.length;

                url = new URL(instance + "/SPSL.Web/ClientBin/Yuce-K12NET-SPServicesLibrary-SPDomainService.svc/json/SubmitChanges");
                connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
                connection.setRequestProperty("Cookie", this.cookie);
                connection.setUseCaches(false);

                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

                outputStream.write(postData);
                outputStream.flush();
                outputStream.close();

                InputStream inputStream = connection.getInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                stringBuilder = new StringBuilder();

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }

                bufferedReader.close();

                String response = stringBuilder.toString();
            } catch (Exception e) {
                return false;
            }

            return true;
        }
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]

}
