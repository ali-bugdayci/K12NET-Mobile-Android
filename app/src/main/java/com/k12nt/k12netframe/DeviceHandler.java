package com.k12nt.k12netframe;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.altbeacon.beacon.*;
import java.util.Collection;
import java.util.HashSet;

/**
 * Created by ali-bugdayci on 11.12.2017.
 */

public class DeviceHandler implements BeaconConsumer {

    private static final String TAG = "DeviceHandler";
    WebViewerActivity mainActivity;
    private BeaconManager deviceManager;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 11;
    private static final int REQUEST_ENABLE_BT = 12;

    HashSet<String> devicesFound;

    DeviceHandler(WebViewerActivity belonging){
        mainActivity = belonging;

        checkPermissions();
        handleBluetooth();
        initDevices();

        devicesFound = new HashSet<>();
    }

    void startBind(){
        deviceManager.bind(this);
    }


    void reset(){
        if(deviceManager != null)
            deviceManager.unbind(this);
    }



    private  void initDevices(){
        deviceManager = BeaconManager.getInstanceForApplication(mainActivity);
        deviceManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        //deviceManager.setAndroidLScanningDisabled(true);
        deviceManager.setBackgroundMode(false);

        deviceManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> devices, Region region) {

                for (Beacon device : devices) {
                    String log = "The devices I see is:" +device.getBeaconTypeCode()  + " : "+  device.getBluetoothName()+ " about "+device.getDistance()+" meters away.";
                    String major = device.getId2().toString();
                    String minor = device.getId3().toString();
                    String key = major+ "-"+ minor;

                    log += " Major minor: " + key ;

                    Log.e(TAG, log);

                    if(!devicesFound.contains(key)){
                        devicesFound.add(key);
                        mainActivity.deviceFound(key);
                    }
                }

                if (devices.size() == 0)
                    Log.e(TAG, "No devices found.");
            }
        });
    }

    @Override
    public Context getApplicationContext() {
        return mainActivity.getApplicationContext();
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        mainActivity.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return mainActivity.bindService(intent,serviceConnection,i);
    }


    @Override
    public void onBeaconServiceConnect() {
        try {
            //Region region = new Region("alldevices", Identifier.parse("B9407F30-F5F8-466E-AFF9-25556B57FE6D") , null, null);
            Region region = new Region("alldevices", null , null, null);
            deviceManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception: ", e);
        }
    }


    private void checkPermissions() {

        /*
        try {
            PackageInfo info = getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            Log.d(TAG, "SDK "+Build.VERSION.SDK_INT+" App Permissions:");
            if (info.requestedPermissions != null) {
                for (String p : info.requestedPermissions) {
                    int grantResult = this.checkPermission(p, android.os.Process.myPid(), android.os.Process.myUid());
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, p+" PERMISSION_GRANTED");
                    }
                    else {
                        Log.d(TAG, p+" PERMISSION_DENIED: "+grantResult);
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Cannot get permissions due to error", e);
        }
        */

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;


        // Android M Permission check
        if (mainActivity.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            DialogInterface.OnDismissListener listener = new DialogInterface.OnDismissListener() {
                @TargetApi(Build.VERSION_CODES.M)
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mainActivity.requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            };

            showAlert(R.string.location_access_title,
                    R.string.location_access_message,
                    listener);

        }

    }


    private void showAlert(int title, int message, DialogInterface.OnDismissListener listener){
        final AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, null);


        if(listener == null)
            listener = new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                }
            };

        builder.setOnDismissListener(listener);
        builder.show();
    }



    private void handleBluetooth(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Log.e(TAG, "No bluetooth device");
            return;
        }
        else if (!mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Ask for enabling") ;

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mainActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


}
