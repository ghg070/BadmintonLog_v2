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
    private static final long SCAN_PERIOD = 2000;

    // Broadcast Related
    public final static String ACTION_BEACON_SERVICE_INITIAL_STATE = "BEACONHANDLER.ACTION_SOUND_SERVICE_CONNECT_STATE";

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
        }
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

    public void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {
                if (Build.VERSION.SDK_INT < 21) {
                    mBluetoothAdapter.startLeScan(mLeScanCallback);

                    try {
                        Thread.sleep(SCAN_PERIOD);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                } else {
                    mBLEScanner.startScan(mScanCallback);

                    try {
                        Thread.sleep(SCAN_PERIOD);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    mBLEScanner.stopScan(mScanCallback);
                }

                /*
                runOnUiThread(new Runnable() {
		            @Override
		            public void run() {
		                // This code will always run on the UI thread, therefore is safe to modify UI elements.
		            	setupListView();
		            }
		        });
		        */
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
                            Log.i(TAG, "Find device:"+p.getDevice().getAddress());
                            /*runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                    setupListView();
                                }
                            });*/
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
            Log.i(TAG, "callbackType"+String.valueOf(callbackType));
            Log.i(TAG, "result"+result.toString());
            final ScanResult scanResult = result;
            final BluetoothDevice device = scanResult.getDevice();

            new Thread() {
                @Override
                public void run() {
                    if (device != null) {
                        KoalaDevice p = new KoalaDevice(device, scanResult.getRssi(), scanResult.getScanRecord().getBytes());
                        int position = findKoalaDevice(device.getAddress());
                        if (position == -1) {
                            AtomicBoolean flag = new AtomicBoolean(false);
                            mDevices.add(p);
                            mFlags.add(flag);
                            Log.i(TAG, "Find device:"+p.getDevice().getAddress());
                            /*runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                    setupListView();
                                }
                            });*/
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

    @Override
    public void onSensorChange(final SensorEvent e) {
        final int eventType = e.type;
        final double values [] = new double[3];
        switch (eventType) {
            case SensorEvent.TYPE_ACCELEROMETER:
                final int acc_position = findKoalaDevice(e.device.getAddress());
                if (acc_position != -1) {
                    final KoalaDevice d = mDevices.get(acc_position);
                    mActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                d.addRecvItem();
                                values[0] = e.values[0];
                                values[1] = e.values[1];
                                values[2] = e.values[2];
                                Log.d(TAG, "time=" + System.currentTimeMillis() + "gX:" + values[0] + "gY:" + values[1] + "gZ:" + values[2] + "\n");
                                //updateSamplingRate(acc_position, d.getCurrentSamplingRate());
                                //displayAccData(acc_position, values);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });
                }
                break;
            case SensorEvent.TYPE_GYROSCOPE:
                final int gyro_position = findKoalaDevice(e.device.getAddress());
                if (gyro_position != -1) {
                    final KoalaDevice d = mDevices.get(gyro_position);
                    mActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                //d.addRecvItem();
                                values[0] = e.values[0];
                                values[1] = e.values[1];
                                values[2] = e.values[2];
                                Log.d(TAG, "time=" + System.currentTimeMillis() + "aX:" + values[0] + "aY:" + values[1] + "aZ:" + values[2] + "\n");
                                //updateSamplingRate(gyro_position, d.getCurrentSamplingRate());
                                //displayAccData(gyro_position, values);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });
                }
                break;
        }
    }

    @Override
    public void onConnectionStatusChange(boolean status) {

    }

    @Override
    public void onRSSIChange(String addr, float rssi) {
        final int position = findKoalaDevice(addr);
        if (position != -1) {
            Log.d(TAG, "mac Address:" + addr + " rssi:" + rssi);
        }
    }




}
