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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import cc.nctu1210.api.koala6x.KoalaDevice;
import cc.nctu1210.api.koala6x.KoalaService;
import cc.nctu1210.api.koala6x.KoalaServiceManager;
import cc.nctu1210.api.koala6x.SensorEvent;
import cc.nctu1210.api.koala6x.SensorEventListener;
import nctu.nol.file.LogFileWriter;
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
    public static final long SCAN_PERIOD = 3000;

    // Data Store
    private LinkedBlockingQueue<SensorData> AccDataset_for_file = null;
    private LinkedBlockingQueue<SensorData> GyroDataset_for_file = null;;
    private LinkedBlockingQueue<SensorData> AccDataset_for_algo = null;
    private LinkedBlockingQueue<SensorData> GyroDataset_for_algo = null;;

    // FileWrite for Logging
    private LogFileWriter AccDataWriter;
    private LogFileWriter GyroDataWriter;
    private LogFileWriter Cal_virtualY;
    private LogFileWriter Cal_virtualZ;
    private LogFileWriter Cal_AccDataWriter;
    private boolean mIsRecording = false;
    public AtomicBoolean isWrittingSensorDataLog = new AtomicBoolean(false);

    // Broadcast Related
    public final static String ACTION_BEACON_CONNECT_STATE = "BEACONHANDLER.ACTION_BEACON_CONNECT_STATE";
    public final static String ACTION_BEACON_DISCONNECT_STATE = "BEACONHANDLER.ACTION_BEACON_DISCONNECT_STATE";

    //Correction coordinates
    public float[] virtualX = null;
    public float[] virtualY = null;
    public float[] virtualZ = null;
    public static final long Cal_Time = 5000;

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
        mServiceManager.registerSensorEventListener(BeaconHandler.this, SensorEvent.TYPE_ACCELEROMETER, KoalaService.MOTION_WRITE_RATE_10, KoalaService.MOTION_ACCEL_SCALE_16G, KoalaService.MOTION_GYRO_SCALE_500);
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
        mServiceManager.customFinalize();
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
    }

    @Override
    public void onConnectionStatusChange(boolean status) {
        Log.e(TAG, "Connect State: " + status);
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

    /***************************************/
    /**  BeaconHandler  Data Recording Function **/
    /***************************************/
    private void initLogFile(int uType){
        if(uType == LogFileWriter.TESTING_TYPE) {
            AccDataWriter = new LogFileWriter("AccData.csv", LogFileWriter.ACCELEROMETER_DATA_TYPE, uType);
            GyroDataWriter = new LogFileWriter("GyroData.csv", LogFileWriter.GYROSCOPE_DATA_TYPE, uType);
            Cal_AccDataWriter = new LogFileWriter("Cal_AccData.csv", LogFileWriter.ACCELEROMETER_CALIBRATION_TYPE, uType);
        }
        else if(uType == LogFileWriter.CALIBRATION_Y_TYPE)
            Cal_virtualY = new LogFileWriter("Cal_Y.csv", LogFileWriter.ACCELEROMETER_CALIBRATION_TYPE, uType);
        else if(uType == LogFileWriter.CALIBRATION_Z_TYPE)
            Cal_virtualZ = new LogFileWriter("Cal_Z.csv", LogFileWriter.ACCELEROMETER_CALIBRATION_TYPE, uType);

    }

    private void initParameters(){
        AccDataset_for_file = new LinkedBlockingQueue<SensorData>();
        GyroDataset_for_file = new LinkedBlockingQueue<SensorData>();
        AccDataset_for_algo = new LinkedBlockingQueue<SensorData>();
        GyroDataset_for_algo = new LinkedBlockingQueue<SensorData>();
    }

    public void startRecording(int uType){
        initParameters();
        initLogFile(uType);
        mIsRecording = true;
        startLogging(uType);
    }

    public void stopRecording(){
        mIsRecording = false;
    }

    private void startLogging(final int uType){
        new Thread(new Runnable() {
            @Override
            public void run() {
                isWrittingSensorDataLog.set(true);
                while( mIsRecording ||  AccDataset_for_file.size() > 0 || GyroDataset_for_file.size() > 0){
                    if( AccDataset_for_file.size() > 0 ) {
                        final SensorData acc_data = AccDataset_for_file.poll();
                        try {
                            if(uType == LogFileWriter.TESTING_TYPE) {
                                AccDataWriter.writeInertialDataFile(acc_data.time, acc_data.values[0], acc_data.values[1], acc_data.values[2]);
                                float[] CalibrationTemp = new float[3];
                                CalibrationTemp = getCorrectionValue(acc_data.values);
                                Cal_AccDataWriter.writeInertialDataFile(acc_data.time, CalibrationTemp[0], CalibrationTemp[1], CalibrationTemp[2]);
                            }
                            else if(uType == LogFileWriter.CALIBRATION_Y_TYPE) {
                                Cal_virtualY.writeInertialDataFile(acc_data.time, acc_data.values[0], acc_data.values[1], acc_data.values[2]);
                            }
                            else if(uType == LogFileWriter.CALIBRATION_Z_TYPE){
                                Cal_virtualZ.writeInertialDataFile(acc_data.time, acc_data.values[0], acc_data.values[1], acc_data.values[2]);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                    if( GyroDataset_for_file.size() > 0 && uType == LogFileWriter.TESTING_TYPE ) {
                        final SensorData gyro_data = GyroDataset_for_file.poll();
                        try {
                            GyroDataWriter.writeInertialDataFile(gyro_data.time, gyro_data.values[0], gyro_data.values[1], gyro_data.values[2]);
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                }
                if(AccDataWriter != null)
                    AccDataWriter.closefile();
                if(GyroDataWriter != null)
                    GyroDataWriter.closefile();
                if(Cal_AccDataWriter != null)
                    Cal_AccDataWriter.closefile();
                if(Cal_virtualY != null)
                    Cal_virtualY.closefile();
                if(Cal_virtualZ != null)
                    Cal_virtualZ.closefile();

                isWrittingSensorDataLog.set(false);
            }
        }).start();
    }

    @Override
    public void onSensorChange(final SensorEvent e) {
        if( mIsRecording ) {
            final int eventType = e.type;
            final float values[] = new float[3];

            // Set Time
            long curTime = System.currentTimeMillis();
            long passTime = curTime - SystemParameters.StartTime;

            switch (eventType) {
                case SensorEvent.TYPE_ACCELEROMETER:
                    SystemParameters.SensorCount++;
                    values[0] = e.values[0];
                    values[1] = e.values[1];
                    values[2] = e.values[2];
                    //Log.d(TAG, "time=" + System.currentTimeMillis() + "gX:" + values[0] + "gY:" + values[1] + "gZ:" + values[2] + "\n");
                    if (SystemParameters.isServiceRunning.get()) {
                        SensorData sd = new SensorData(passTime, values);
                        AccDataset_for_file.add(sd);
                        AccDataset_for_algo.add(sd);
                    }
                    break;
                case SensorEvent.TYPE_GYROSCOPE:
                    values[0] = e.values[0];
                    values[1] = e.values[1];
                    values[2] = e.values[2];
                    //Log.d(TAG, "time=" + System.currentTimeMillis() + "wX:" + values[0] + "wY:" + values[1] + "wZ:" + values[2] + "\n");
                    if (SystemParameters.isServiceRunning.get()) {
                        SensorData sd = new SensorData(passTime, values);
                        GyroDataset_for_file.add(sd);
                    }
                    break;
            }
        }
    }

    @Override
    public void onRSSIChange(String addr, float rssi) {
        final int position = findKoalaDevice(addr);
        if (position != -1) {
            Log.d(TAG, "mac Address:" + addr + " rssi:" + rssi);
        }
    }


    //training average Axis cross product X
    public void startCalibration(int uType) {
        // Initial
        float[] average = new float[3];
        for(int i = 0; i < 3; i++)
            average[i] = 0;

        // Pop Acc Data
        int length = AccDataset_for_algo.size();
        while (AccDataset_for_algo.size() > 0) {
            final SensorData acc_data = AccDataset_for_algo.poll();
            for(int i = 0; i < 3; i++)
                average[i] += acc_data.values[i];
        }

        if (length > 0) {
            //Average
            for (int i = 0; i < 3; i++)
                average[i] /= length;

            if (uType == LogFileWriter.CALIBRATION_Y_TYPE) {
                virtualY = new float[3];
                for(int i = 0; i < 3; i++)
                    virtualY[i] = average[i];
                Log.e(TAG, "virtualY" + virtualY[0] + virtualY[1] + virtualY[2]);
            } else if (uType == LogFileWriter.CALIBRATION_Z_TYPE) {
                virtualZ = new float[3];
                for(int i = 0; i < 3; i++)
                    virtualZ[i] = average[i]*(-1);
                Log.e(TAG, "virtualZ" + virtualZ[0] + virtualZ[1] + virtualZ[2]);

                virtualX = new float[3];
                for (int i = 0; i < 3; i++)
                    virtualX[i] = virtualY[(i + 1) % 3] * virtualZ[(i + 2) % 3] - virtualY[(i + 2) % 3] * virtualZ[(i + 1) % 3];
                Log.e(TAG, "virtualX" + virtualX[0] + virtualX[1] + virtualX[2]);
            }
        }
    }

    //Correction coordinates
    public float[] getCorrectionValue(float[] data) {
        float CorrectionValue[] = new float[3];
        for(int i = 0; i < 3; i++) {
            CorrectionValue[0] += data[i] * virtualX[i];
            CorrectionValue[1] += data[i] * virtualY[i];
            CorrectionValue[2] += data[i] * virtualZ[i];
        }
        CorrectionValue[0] /= Math.sqrt(Math.pow(virtualX[0],2)+Math.pow(virtualX[1],2)+Math.pow(virtualX[2],2));
        CorrectionValue[1] /= Math.sqrt(Math.pow(virtualY[0],2)+Math.pow(virtualY[1],2)+Math.pow(virtualY[2],2));
        CorrectionValue[2] /= Math.sqrt(Math.pow(virtualZ[0],2)+Math.pow(virtualZ[1],2)+Math.pow(virtualZ[2],2));
        return CorrectionValue;
    }


    public class SensorData{
        long time;
        float values[];
        public SensorData(long time, float[] vals){
            this.time = time;
            this.values = new float[3];
            for(int i = 0; i < 3; i++)
                this.values[i] = vals[i];
        }
    }

}
