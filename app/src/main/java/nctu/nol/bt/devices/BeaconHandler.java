package nctu.nol.bt.devices;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cc.nctu1210.api.koala6x.KoalaDevice;
import cc.nctu1210.api.koala6x.KoalaService;
import cc.nctu1210.api.koala6x.KoalaServiceManager;
import cc.nctu1210.api.koala6x.SensorEvent;
import cc.nctu1210.api.koala6x.SensorEventListener;
import nctu.nol.file.SystemParameters;


public class BeaconHandler implements SensorEventListener {
    private final static String TAG = BeaconHandler.class.getSimpleName();

    // Activity Related
    private Activity mActivity;

    // Beacon Related
    private boolean startScan = false;
    private KoalaServiceManager mServiceManager;
    private BluetoothAdapter mBluetoothAdapter;
    /******** for SDK version > 21 **********/
    private BluetoothLeScanner mBLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    public static ArrayList<KoalaDevice> mDevices = new ArrayList<KoalaDevice>();  // Manage the devices
    public static ArrayList<AtomicBoolean> mFlags = new ArrayList<AtomicBoolean>();
    public static final long SCAN_PERIOD = 2000;

    // Broadcast Related
    public final static String ACTION_BEACON_CONNECT_STATE = "BEACONHANDLER.ACTION_BEACON_CONNECT_STATE";
    public final static String ACTION_BEACON_DISCONNECT_STATE = "BEACONHANDLER.ACTION_BEACON_DISCONNECT_STATE";

    public BeaconHandler(Activity activity){
        this.mActivity = activity;
        initBLEService();
    }

    public void initBLEService(){
        final BluetoothManager mBluetoothManager = (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(mActivity, "BLE not supported", Toast.LENGTH_SHORT).show();
            mActivity.finish();
            return;
        }
        mServiceManager = new KoalaServiceManager(mActivity);
        mServiceManager.registerSensorEventListener(BeaconHandler.this, SensorEvent.TYPE_ACCELEROMETER, KoalaService.MOTION_WRITE_RATE_5, KoalaService.MOTION_ACCEL_SCALE_16G, KoalaService.MOTION_GYRO_SCALE_500);
        mServiceManager.registerSensorEventListener(BeaconHandler.this, SensorEvent.TYPE_GYROSCOPE);

        if (Build.VERSION.SDK_INT >= 21) {
            mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<ScanFilter>();
        }
    }

    public void deleteObject(){
        mServiceManager.close();
    }

    private int findKoalaDevice(String macAddr) {
        if (mDevices.size() == 0)
            return -1;
        for (int i=0; i<mDevices.size(); i++) {
            KoalaDevice tmpDevice = mDevices.get(i);
            if (macAddr.matches(tmpDevice.getDevice().getAddress()))
                return i;
        }
        return -1;
    }

    /******************************/
    /**  BeaconHandler  Scan  Function **/
    /******************************/
    public void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {
                if (Build.VERSION.SDK_INT < 21) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mBluetoothAdapter.startLeScan(mLeScanCallback);

                    try {
                        Thread.sleep(SCAN_PERIOD);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                } else {
                    mBLEScanner.stopScan(mScanCallback);
                    mBLEScanner.startScan(mScanCallback);
                    try {
                        Thread.sleep(SCAN_PERIOD);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    mBLEScanner.stopScan(mScanCallback);
                }
            }
        }.start();
    }

    /**
     * The event callback to handle the found of near le devices
     * For SDK version < 21.
     *
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {

            new Thread() {
                @Override
                public void run() {
                    if (device != null) {
                        KoalaDevice p = new KoalaDevice(device, rssi, scanRecord);
                        int position = findKoalaDevice(device.getAddress());
                        if (position == -1) {
                            AtomicBoolean flag = new AtomicBoolean(false);
                            mDevices.add(p);
                            mFlags.add(flag);
                            Log.i(TAG, "Find device:" + p.getDevice().getAddress());

                            //Intent broadcast = new Intent(ACTION_BEACON_FOUND_STATE);
                            //broadcast.putExtra(KOALA_NAME, p.getDevice().getName());
                            //broadcast.putExtra(KOALA_ADDRESS, p.getDevice().getAddress());
                            //mActivity.sendBroadcast(broadcast);
                        }
                    }
                }
            }.start();
        }
    };
   /**
     * The event callback to handle the found of near le devices
     * For SDK version >= 21.
     *
     */
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //Log.i(TAG, "callbackType"+String.valueOf(callbackType));
            //Log.i(TAG, "result"+result.toString());
            final ScanResult scanResult = result;
            final BluetoothDevice device = scanResult.getDevice();

            new Thread() {
                @Override
                public void run() {
                    if (device != null) {
                        final KoalaDevice p = new KoalaDevice(device, scanResult.getRssi(), scanResult.getScanRecord().getBytes());
                        int position = findKoalaDevice(device.getAddress());
                        if (position == -1) {
                            AtomicBoolean flag = new AtomicBoolean(false);
                            mDevices.add(p);
                            mFlags.add(flag);
                            Log.i(TAG, "Find device:"+p.getDevice().getAddress());
                        }
                    }
                }
            }.start();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i(TAG, "ScanResult - Results"+sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan Failed, Error Code: " + errorCode);
        }
    };

    public final ArrayList<KoalaDevice> getScanedDevices(){
        return mDevices;
    }

    /************************************/
    /**  BeaconHandler  Connection  Function  **/
    /************************************/
    public void ConnectToKoala(final String macAddress){
        mServiceManager.connect(macAddress);
    }

    public void DisconnectToKoala(){
        mServiceManager.disconnect();
       // mServiceManager.close();
    }

    /**************************************/
    /**  BeaconHandler  Data Recording Function **/
    /**************************************/
    @Override
    public void onSensorChange(final SensorEvent e) {
        final int eventType = e.type;
        final double values [] = new double[3];
        switch (eventType) {
            case SensorEvent.TYPE_ACCELEROMETER:
                final int acc_position = findKoalaDevice(e.device.getAddress());
                if (acc_position != -1) {
                    final KoalaDevice d = mDevices.get(acc_position);
                    d.addRecvItem();
                    SystemParameters.SensorCount++;
                    values[0] = e.values[0];
                    values[1] = e.values[1];
                    values[2] = e.values[2];
                    Log.d(TAG, "time=" + System.currentTimeMillis() + "gX:" + values[0] + "gY:" + values[1] + "gZ:" + values[2] + "\n");
                    //updateSamplingRate(acc_position, d.getCurrentSamplingRate());
                }
                break;
            case SensorEvent.TYPE_GYROSCOPE:
                final int gyro_position = findKoalaDevice(e.device.getAddress());
                if (gyro_position != -1) {
                    final KoalaDevice d = mDevices.get(gyro_position);
                    values[0] = e.values[0];
                    values[1] = e.values[1];
                    values[2] = e.values[2];
                    Log.d(TAG, "time=" + System.currentTimeMillis() + "wX:" + values[0] + "wY:" + values[1] + "wZ:" + values[2] + "\n");
                }
                break;
        }
    }

    @Override
    public void onConnectionStatusChange(boolean status) {
         Log.e(TAG,"Connect State: "+status);
        if( status ) {
            SystemParameters.IsKoalaReady = true;
            Intent broadcast = new Intent(ACTION_BEACON_CONNECT_STATE);
            mActivity.sendBroadcast(broadcast);
        } else {
            SystemParameters.IsKoalaReady = false;
            Intent broadcast = new Intent(ACTION_BEACON_DISCONNECT_STATE);
            mActivity.sendBroadcast(broadcast);
        }
    }

    @Override
    public void onRSSIChange(String addr, float rssi) {
        final int position = findKoalaDevice(addr);
        if (position != -1) {
            Log.d(TAG, "mac Address:" + addr + " rssi:" + rssi);
        }
    }

}
