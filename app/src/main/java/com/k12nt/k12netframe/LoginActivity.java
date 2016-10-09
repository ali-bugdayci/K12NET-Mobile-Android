package com.k12nt.k12netframe;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.k12nt.k12netframe.async_tasks.LoginAsyncTask;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;

public class LoginActivity extends Activity {

    public static String providerId;
    final Context context = this;
   // final int anim_len = 75 * 50;
     final int anim_wait_len = 1200;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        K12NetUserReferences.initUserReferences(getApplicationContext());
        K12NetHttpClient.resetBrowser(getApplicationContext());

        //String lang = K12NetUserReferences.getLanguageCode();
        //Locale locale = new Locale(lang);
        //Locale.setDefault(locale);
        //Configuration config = new Configuration();
        //config.locale = locale;
        //getBaseContext().getResources().updateConfiguration(config,getBaseContext().getResources().getDisplayMetrics());

        setContentView(R.layout.k12net_login_layout);

        final EditText username = (EditText) findViewById(R.id.txt_login_username);
        final EditText password = (EditText) findViewById(R.id.txt_login_password);
        final CheckBox chkRememberMe = (CheckBox) findViewById(R.id.chk_remember_me);

        chkRememberMe.setChecked(K12NetUserReferences.getRememberMe());

        username.setText(K12NetUserReferences.getUsername());
        if (chkRememberMe.isChecked()) {
            password.setText(K12NetUserReferences.getPassword());
        }

        ImageView img_logo = (ImageView) findViewById(R.id.img_login_icon);

        Button login_button = (Button) findViewById(R.id.btn_login_submit);

        login_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                K12NetUserReferences.setUsername(username.getText().toString());
                K12NetUserReferences.setPassword(password.getText().toString());
                K12NetUserReferences.setRememberMe(chkRememberMe.isChecked());

            /*    Mint.addExtraData("enteredUserName", username.getText().toString());
                Mint.addExtraData("enteredPassword", password.getText().toString());
                Mint.addExtraData("enteredUrl", K12NetUserReferences.getConnectionAddress());
                Mint.addExtraData("enteredFS", K12NetUserReferences.getFileServerAddress());*/

                K12NetHttpClient.resetBrowser(getApplicationContext());
                LoginAsyncTask loginTasAsyncTask = new LoginAsyncTask(context, username.getText().toString(), password.getText().toString(), LoginActivity.this);
                loginTasAsyncTask.execute();

                //Log.d("time", "login basildi: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS").format(System.currentTimeMillis())) ;

            }
        });

        Button settings_button = (Button) findViewById(R.id.btn_settings);
        settings_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                K12NetSettingsDialogView dialogView = new K12NetSettingsDialogView(arg0.getContext());
                dialogView.createContextView(null);
                dialogView.show();
            }
        });

        if (chkRememberMe.isChecked()) {
            login_button.performClick();
        }
    }
}
