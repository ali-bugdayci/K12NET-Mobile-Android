package com.k12nt.k12netframe.async_tasks;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.webkit.CookieManager;
import android.widget.Toast;

import com.k12nt.k12netframe.LoginActivity;
import com.k12nt.k12netframe.R;
import com.k12nt.k12netframe.WebViewerActivity;
import com.k12nt.k12netframe.fcm.MyFirebaseInstanceIDService;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

public class LoginAsyncTask extends AsistoAsyncTask {

    Activity currentActivity;
    Context ctx;
    Dialog progress_dialog;
    String username;
    String password;

    Boolean success;
    String line = null;

    private Boolean isLogin = false;

    public LoginAsyncTask(Context ctx, String username, String password, Activity currentActivity) {
        this.currentActivity = currentActivity;
        this.ctx = ctx;
        this.username = username;
        this.password = password;
    }

    @Override
    public void onPreExecute() {
        progress_dialog = new Dialog(ctx, R.style.K12NET_ModalLayout);
        progress_dialog.setContentView(R.layout.loading_view_layout);
        progress_dialog.show();
    }

    @Override
    protected Void doInBackground(Void... params) {

        isLogin = login();

        return null;
    }

    public static boolean login() {
        HttpResponse response = null;
        try {
            JSONObject json = new JSONObject();
            json.put("userName", K12NetUserReferences.getUsername());
            json.put("password", K12NetUserReferences.getPassword());
            json.put("createPersistentCookie", "false");

            String connString = K12NetUserReferences.getConnectionAddress() + "/Authentication_JSON_AppService.axd/Login";
            HttpPost httpost = new HttpPost(connString);
            StringEntity entity = new StringEntity(json.toString(), HTTP.UTF_8);
            httpost.setEntity(entity);
            httpost.setHeader("Content-type", "application/json;charset=UTF-8");

            String line = K12NetHttpClient.execute(httpost);

            JSONObject responseJSON = new JSONObject(line);

            if(responseJSON.optBoolean("d", false)) {

                HttpGet userGet = new HttpGet(K12NetUserReferences.getConnectionAddress() + "/SPSL.Web/ClientBin/Yuce-K12NET-SPSL-Web-AuthenticationService.svc/json/GetUser");
                line = K12NetHttpClient.execute(userGet);

                JSONObject jObject = (new JSONObject(line)).getJSONObject("GetUserResult").getJSONArray("RootResults").getJSONObject(0);

                LoginActivity.providerId = jObject.optString("ProviderUserKey", "");

                String cookieString = CookieManager.getInstance().getCookie(K12NetUserReferences.getConnectionAddress());

                return true;

            }
            else {
                LoginActivity.providerId = "";
                return false;
            }

        } catch (Exception ex) {
            /*if(response != null) {
                try {
                    response.getEntity().consumeContent();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
            ex = null;
            //}
        }

        return false;

    }


    @Override
    protected void onAsyncComplete() {
        if(progress_dialog.isShowing()) {
            try {
                progress_dialog.dismiss();
            }
            catch (Exception ex) {

            }
        }

        if (isLogin == false) {
            Toast.makeText(ctx, R.string.login_failed, Toast.LENGTH_SHORT).show();
        } else {

            MyFirebaseInstanceIDService firebaseInstanceIDService = new MyFirebaseInstanceIDService();
            firebaseInstanceIDService.onTokenRefresh();

            WebViewerActivity.startUrl = K12NetUserReferences.getConnectionAddress();
            Intent intent = new Intent(ctx, WebViewerActivity.class);
            ctx.startActivity(intent);
        }
    }
}
