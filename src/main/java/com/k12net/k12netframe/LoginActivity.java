package com.k12net.k12netframe;

import android.app.Activity;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private Switch switchRememberMe;
    private Button buttonForgetMe;
    private Button loginButton;
    private ProgressBar progressBarLogin;

    DatabaseHelper databaseHelper;
    List<DatabaseHelper.UserProfile> userProfiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        editTextUsername = (EditText) findViewById(R.id.editTextUsername);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);
        switchRememberMe = (Switch) findViewById(R.id.switchRememberMe);
        buttonForgetMe = (Button) findViewById(R.id.buttonForgetMe);
        loginButton = (Button) findViewById(R.id.loginButton);
        progressBarLogin = (ProgressBar) findViewById(R.id.progressBarLogin);


        if (!this.isConnected()) {
            Toast.makeText(this, "You are not connected.", Toast.LENGTH_LONG).show();

            return;
        }

        rememberUser();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void rememberUser() {
        databaseHelper = new DatabaseHelper(this, 3);

        userProfiles = databaseHelper.GetUserProfiles();

        if (userProfiles.size() == 0) {
            switchRememberMe.setVisibility(View.VISIBLE);
            buttonForgetMe.setVisibility(View.INVISIBLE);

            return;
        }

        DatabaseHelper.UserProfile userProfile = userProfiles.get(0);

        editTextUsername.setText(userProfile.Username);
        editTextUsername.setEnabled(false);
        editTextPassword.setText(userProfile.Password);
        editTextPassword.setEnabled(false);
        switchRememberMe.setChecked(true);
        switchRememberMe.setVisibility(View.INVISIBLE);
        buttonForgetMe.setVisibility(View.VISIBLE);

        boolean isLoggedOut = getIntent().getBooleanExtra("IsLoggedOut", false);

        if (!isLoggedOut) {
            loginButtonClicked(null);
        }
    }

    public void persistUser() {
        DatabaseHelper.UserProfile userProfile = null;

        for (DatabaseHelper.UserProfile profile : userProfiles) {
            if (profile.Username.equalsIgnoreCase(editTextUsername.getText().toString()))
                userProfile = profile;
        }

        if (userProfile == null) {
            databaseHelper.addUserProfile(editTextUsername.getText().toString(), editTextPassword.getText().toString());
        } else {
            userProfile.Username = editTextUsername.getText().toString();
            userProfile.Password = editTextPassword.getText().toString();
            userProfile.LastLoginTime = (new Date()).getTime();

            databaseHelper.update(userProfile);
        }
    }

    public void buttonForgetMeClicked(View view) {
        DatabaseHelper.UserProfile userProfile = null;

        for (DatabaseHelper.UserProfile profile : userProfiles) {
            if (profile.Username.equalsIgnoreCase(editTextUsername.getText().toString()))
                userProfile = profile;
        }

        if (userProfile != null) databaseHelper.delete(userProfile);

        rememberUser();

        editTextUsername.setText("");
        editTextUsername.setEnabled(true);
        editTextPassword.setText("");
        editTextPassword.setEnabled(true);
    }

    public void loginButtonClicked(View view) {
        loginButton.setEnabled(false);
        this.progressBarLogin.setVisibility(View.VISIBLE);

        LoginTask task = new LoginTask();

        task.Username = this.editTextUsername.getText().toString();
        task.Password = this.editTextPassword.getText().toString();

        List<DatabaseHelper.Instance> instances = databaseHelper.GetInstances();

        task.execute(instances);
    }

    private boolean isConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    private class LoginTask extends AsyncTask<List<DatabaseHelper.Instance>, Void, Boolean> {
        private DatabaseHelper.Instance Instance;
        private List<String> Messages = new ArrayList<String>();
        private List<String> Cookies;

        public String Username;
        public String Password;

        @Override
        protected Boolean doInBackground(List<DatabaseHelper.Instance>... params) {
            for (DatabaseHelper.Instance instance : params[0]) {
                this.Instance = instance;

                if (this.tryAuthenticate()) {
                    return true;
                }
            }

            return false;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Intent intent = new Intent(getBaseContext(), DefaultActivity.class);

                intent.putExtra("Url", this.Instance.Url);
                intent.putExtra("Cookies", this.Cookies.toArray(new String[this.Cookies.size()]));

                startActivity(intent);

                if (switchRememberMe.isChecked()) persistUser();

                if (!this.Instance.IsSelected) databaseHelper.setAsDefault(this.Instance);
            } else {
                Toast.makeText(getBaseContext(), "Invalid credentials.", Toast.LENGTH_LONG).show();
            }

            loginButton.setEnabled(true);
            progressBarLogin.setVisibility(View.INVISIBLE);
        }

        private boolean tryAuthenticate() {
            String urlParameters = "{\"userName\":\"" + this.Username + "\",\"password\":\"" + this.Password + "\",\"createPersistentCookie\":false}";

            byte[] postData = urlParameters.getBytes(Charset.forName("UTF-8"));
            int postDataLength = postData.length;

            try {
                URL url = new URL(this.Instance.Url + "/Authentication_JSON_AppService.axd/Login");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                connection.setUseCaches(false);

                try {
                    DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

                    outputStream.write(postData);
                    outputStream.flush();
                    outputStream.close();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    StringBuffer stringBuffer = new StringBuffer();

                    while ((line = reader.readLine()) != null) {
                        stringBuffer.append(line);
                        stringBuffer.append('\r');
                    }
                    reader.close();

                    String response = stringBuffer.toString();

                    if (response.contains("{\"d\":true}")) {
                        this.Cookies = connection.getHeaderFields().get("Set-Cookie");
                        return true;
                    } else {
                        this.Messages.add("Invalid credentials.");
                        return false;
                    }

                } catch (Exception e1) {
                    this.Messages.add(e1.toString());
                    return false;
                }

            } catch (Exception e) {
                this.Messages.add(e.toString());
                return false;
            }
        }
    }
}
