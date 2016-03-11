package com.k12net.k12netframe;

import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private EditText editTextUrl;
    private Button buttonSave;

    DatabaseHelper databaseHelper;
    List<DatabaseHelper.Instance> instances;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        editTextUrl = (EditText) findViewById(R.id.editTextUrl);
        buttonSave = (Button) findViewById(R.id.buttonSave);

        databaseHelper = new DatabaseHelper(this, 3);

        instances = databaseHelper.GetInstances();

        editTextUrl.setText(instances.get(0).Url);
    }

    public void buttonSaveClicked(View view){
        String url = editTextUrl.getText().toString();

        if(!url.startsWith("http") || !url.contains(".")){
            Toast.makeText(getBaseContext(), "Invalid url", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseHelper.Instance instance = null;

        for (DatabaseHelper.Instance item : instances){
            if(item.Url.equalsIgnoreCase(url)) instance = item;
        }

        if(instance == null){
            databaseHelper.addInstance(url, true);
        }else {
            databaseHelper.setAsDefault(instance);
        }

        Intent intent = new Intent(this, LoginActivity.class);

        intent.putExtra("IsLoggedOut", true);
        startActivity(intent);
    }
}
