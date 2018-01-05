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
import java.util.HashMap;
import java.util.Set;

import static android.R.attr.key;
import static com.k12nt.k12netframe.R.string.status;

/**
 * Created by ali-bugdayci on 11.12.2017.
 */

public class DeviceHandler implements BeaconConsumer {

    private static final String TAG = "DeviceHandler";
    private static final double DISTANCE_TRESHOLD = 5; //meters
    private static long waitBeforeInform = 60000; //1 min in ms
    private static long waitBeforeAlarm = 720000; //2 hour in ms

    WebViewerActivity mainActivity;
    private BeaconManager deviceManager;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 11;
    private static final int REQUEST_ENABLE_BT = 12;

    class DeviceData {
        public long firstFound;
        public long lastFound;
        public boolean informedFound;
        public boolean informedMissing;
        public boolean informedAlarmed;

        DeviceData(long firstFound){
            this.firstFound = firstFound;
            this.lastFound = firstFound;
        }
    }

    HashMap<String,DeviceData> devicesFound;

    DeviceHandler(WebViewerActivity belonging){
        mainActivity = belonging;

        checkPermissions();
        handleBluetooth();
        initDevices();

        devicesFound = new HashMap<>();
    }

    void startBind(){
        if(!deviceManager.isBound(this)) {
            deviceManager.bind(this);
            devicesFound = new HashMap<>();
        }
    }


    void reset(){
        if(deviceManager != null)
            deviceManager.unbind(this);
    }



    private  void initDevices(){
        deviceManager = BeaconManager.getInstanceForApplication(mainActivity);
        deviceManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        //deviceManager.setAndroidLScanningDisabled(true);
        //deviceManager.setBackgroundMode(false);

        // set the duration of the scan to be 5 seconds
        deviceManager.setBackgroundScanPeriod(5000l);

        deviceManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> devices, Region region) {
                long now = System.currentTimeMillis();

                for (Beacon device : devices) {
                    if(device.getDistance() > DISTANCE_TRESHOLD)
                        continue;

                    String major = device.getId2().toString();
                    String minor = device.getId3().toString();
                    String key = major+ "-"+ minor;

                    /*
                    String log = "The devices I see is:" +device.getBeaconTypeCode()  + " : "+  device.getBluetoothName()+ " about "+device.getDistance()+" meters away.";
                    log += " Major minor: " + key ;
                    Log.e(TAG, log);
                    */

                    DeviceData data;

                    if(!devicesFound.containsKey(key)){
                        data = new DeviceData(now);
                    }else
                    {
                        data = devicesFound.get(key);
                        data.lastFound = now;
                    }

                    Log.e(TAG, key + " distance: " + device.getDistance() + " firstFound: " + data.firstFound + " lastFound: " + data.lastFound);
                    devicesFound.put(key,data);
                }

                Set<String> ids = devicesFound.keySet();

                for (String id : ids) {
                    DeviceData data = devicesFound.get(id);
                    if(data.informedFound)
                    {
                        if(data.informedMissing)
                            continue;

                        if(data.lastFound + waitBeforeInform < now){
                            data.informedMissing = true;
                            Log.e(TAG, id + " now: " + now + " firstFound: " + data.firstFound + " lastFound: " + data.lastFound);

                            mainActivity.devicestatusChanged(id,"out of bus");
                        }

                        if(data.informedAlarmed)
                            continue;


                        if(data.firstFound + waitBeforeAlarm < now){
                            data.informedAlarmed = true;

                            Log.e(TAG, id + " now: " + now + " firstFound: " + data.firstFound + " lastFound: " + data.lastFound);
                            mainActivity.devicestatusChanged(id, "too long in bus");
                        }
                    }
                    else
                    {
                        if(data.firstFound + waitBeforeInform < data.lastFound){
                            data.informedFound = true;
                            mainActivity.devicestatusChanged(id, "get in bus");
                        }
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
