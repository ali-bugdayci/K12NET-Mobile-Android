package com.k12net.k12netframe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;


public class DefaultActivity extends AppCompatActivity {

    private WebView webView;
    private Boolean isLoggedOut;

    GoogleCloudMessaging googleCloudMessaging;
    private BroadcastReceiver registrationBroadcastReceiver;
    private boolean isReceiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);

        webView = (WebView)findViewById(R.id.webViewDefault);

        this.Navigate();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(registrationBroadcastReceiver);
        isReceiverRegistered = false;
        super.onPause();
    }

    private void Navigate(){
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("Logout.aspx")) {
                    isLoggedOut = true;
                } else if (url.contains("Login.aspx")) {
                    ReLogin();
                    return false;
                }

                return super.shouldOverrideUrlLoading(view, url);
            }

        });

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        String url = getIntent().getStringExtra("Url");
        String[] cookies = getIntent().getStringArrayExtra("Cookies");

        for (String cookie : cookies){
            Integer domainIndex =  cookie.indexOf("domain=");
            if(domainIndex != -1){
                Integer endIndex = cookie.indexOf(";", domainIndex);
                String domain = cookie.substring(domainIndex + 7,endIndex);
                cookieManager.setCookie(domain, cookie);
            }else {
                cookieManager.setCookie(url, cookie);
            }
        }


        webView.loadUrl(url);

        registerNotification(url, cookies);
    }

    private void ReLogin(){
        Intent intent = new Intent(this, LoginActivity.class);

        intent.putExtra("IsLoggedOut", this.isLoggedOut);

        startActivity(intent);
    }

    private  void registerNotification(String url, String[] cookies){

        registrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences.getBoolean("sentTokenToServer", false);
                if (sentToken) {
                    String x="sent";
                } else {
                    String y="token_error_message";
                }
            }
        };

        // Registering BroadcastReceiver
        registerReceiver();


        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);

            intent.putExtra("Url", url);
            intent.putExtra("Cookies", cookies);

            startService(intent);
        }
    }

    private void registerReceiver(){
        if(!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(registrationBroadcastReceiver, new IntentFilter("registrationComplete"));
            isReceiverRegistered = true;
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 9000).show();
            } else {
                Log.i("K12NET Frame Message", "This device is not supported.");
                return false;
            }
            return false;
        }
        return true;
    }
}
