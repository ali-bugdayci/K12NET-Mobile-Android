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

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ali-bugdayci on 11.12.2017.
 */

public class DeviceHandler implements BeaconConsumer {

    private static final String TAG = "DeviceHandler";

    public static final String GET_IN_BUS = "DeviceFound";
    public static final String OUT_OF_BUS = "DeviceMissing";
    public static final String TOO_LONG_IN_BUS = "DeviceTooLongInBus";

    private static double DISTANCE_TRESHOLD = 15; //meters
    private static long waitBeforeInformFound = 5000; //5 secs in ms
    private static long waitBeforeInformOut = 30000; //30 secs in ms
    private static long waitBeforeAlarm = 7200000; //2 hour in ms

    private static long refreshDevicesSince = 10000; //10 secs in ms


    private static Float speed;
    private static Float distance;

    enum Beep  {
        None,
        Short,
        Long
    };

    private static Beep beep =  Beep.None;


    WebViewerActivity mainActivity;
    private BeaconManager deviceManager;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 11;
    private static final int REQUEST_ENABLE_BT = 12;

    HashSet<String> deviceIDs = new HashSet();

    public void setDeviceIDs(String[] deviceIDArray) {
        deviceIDs = new HashSet();
        deviceIDs.addAll(Arrays.asList(deviceIDArray));
    }

    public void updateState(String name, String value) {
        switch (name){
            case "Speed":
                try {
                    speed = Float.valueOf(value);
                }catch (NumberFormatException e){}

                break;

            case "Distance":
                try {
                    distance = Float.valueOf(value);
                }catch (NumberFormatException e){}

                break;

            case "RefreshDeviceInfos":
                try {
                    refreshDevicesSince = Long.valueOf(value);
                }catch (NumberFormatException e){}

                refreshDeviceInfos();
                break;

            case "LongAlert":
                beep = Beep.Long;
                break;
            
            case "ShortBeep":
                beep = Beep.Short;
                break;

            case "DistanceThreshold":
                try {
                    DISTANCE_TRESHOLD = Long.valueOf(value);
                }catch (NumberFormatException e){}

                break;

            case "WaitBeforeInformFound":
                try {
                    waitBeforeInformFound = Long.valueOf(value);
                }catch (NumberFormatException e){}

                break;

            case "WaitBeforeInformOut":
                try {
                    waitBeforeInformOut = Long.valueOf(value);
                }catch (NumberFormatException e){}
                break;

            case "WaitBeforeAlarm":
                try {
                    waitBeforeAlarm = Long.valueOf(value);
                }catch (NumberFormatException e){}
                break;
        }
    }

    public void getDevicesOnBus() {

        Set<String> ids = devicesFound.keySet();

        ArrayList<String> devicesStillinBus = new ArrayList<>(ids.size());
        for (String id : ids) {
            DeviceData data = devicesFound.get(id);


            if (data.informedFound && !data.informedMissing)
                devicesStillinBus.add(id);
        }

        mainActivity.informDevicesOnBus(devicesStillinBus);
    }

    private void refreshDeviceInfos() {
        Set<String> ids = devicesFound.keySet();

        long now = System.currentTimeMillis();

        for (String id : ids) {
            DeviceData data = devicesFound.get(id);


            if(data.lastFound + refreshDevicesSince < now)
                mainActivity.devicestatusChanged(id, OUT_OF_BUS);
            else
                mainActivity.devicestatusChanged(id, GET_IN_BUS);
        }
    }

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

                    if(!deviceIDs.contains(key))
                        continue;
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
                        if(data.informedMissing){
                            checkInBusAgain(now, id, data);
                            continue;
                        }

                        checkOutOfBus(now, id, data);

                        if(data.informedAlarmed)
                            continue;

                        checkTooLongInBus(now, id, data);
                    }
                    else
                    {
                        checkInBus(id, data);
                    }
                }

                if (devices.size() == 0)
                    Log.e(TAG, "No devices found.");
            }
        });
    }

    private void checkTooLongInBus(long now, String id, DeviceData data) {
        if(data.firstFound + waitBeforeAlarm < now){
            data.informedAlarmed = true;

            Log.e(TAG, id + " now: " + now + " firstFound: " + data.firstFound + " lastFound: " + data.lastFound);
            mainActivity.devicestatusChanged(id, TOO_LONG_IN_BUS);
        }
    }

    private void checkOutOfBus(long now, String id, DeviceData data) {
        if(data.lastFound + waitBeforeInformOut < now){
            data.informedMissing = true;
            Log.e(TAG, id + " now: " + now + " firstFound: " + data.firstFound + " lastFound: " + data.lastFound);

            mainActivity.devicestatusChanged(id, OUT_OF_BUS);
        }
    }

    private void checkInBus(String id, DeviceData data) {
        if(data.firstFound + waitBeforeInformFound < data.lastFound){
            data.informedFound = true;
            mainActivity.devicestatusChanged(id, GET_IN_BUS);
        }
    }

    private void checkInBusAgain(long now, String id, DeviceData data) {
        if(now < data.lastFound + waitBeforeInformFound){
            data.informedMissing = false;
            mainActivity.devicestatusChanged(id, GET_IN_BUS);
        }
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
