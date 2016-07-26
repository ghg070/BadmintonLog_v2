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
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import cc.nctu1210.api.koala6x.KoalaDevice;
import cc.nctu1210.api.koala6x.KoalaService;
import cc.nctu1210.api.koala6x.KoalaServiceManager;
import cc.nctu1210.api.koala6x.SensorEvent;
import cc.nctu1210.api.koala6x.SensorEventListener;
import nctu.nol.algo.StrokeClassifier;
import nctu.nol.algo.StrokeDetector;
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

    // Time Calibration
    private LinkedBlockingQueue<SensorData> acc_buffer = null;
    private LinkedBlockingQueue<SensorData> gyro_buffer = null;
    private static final int SEQ_MAX = 128;
    private static long TimeCalibration_Period = 2000; //ms
    private AtomicBoolean IsTimeCalibration = new AtomicBoolean(false);
    private Thread ThreadTimeCalibration = null;
    private double LastCaliTime = 0;

    // Data Store
    private LinkedBlockingDeque<SensorData> AccDataset_for_algo = null;
    private LinkedBlockingDeque<SensorData> AccDataset_GravityReduced_for_algo = null;
    private LinkedBlockingDeque<SensorData> GyroDataset_for_algo = null;
    public static final long Abandon_Time = 20000; // 決定多久以前的資料要捨去, 需大於Correct_Corrdinate_Time
    private AtomicBoolean IsFeatureExtracting = new AtomicBoolean(false);

    // Classifcation
    private StrokeClassifier StrokeTypeClassifier = null;

    // FileWrite for Logging
    private LogFileWriter AccDataWriter;
    private LogFileWriter GyroDataWriter;
    private LogFileWriter AccDataReducedWriter;
    private LogFileWriter Cal_AccDataWriter;
    private LinkedBlockingQueue<SensorData> AccDataset_for_file = null;
    private LinkedBlockingQueue<SensorData> AccDataset_GravityReduced_for_file = null;
    private LinkedBlockingQueue<SensorData> GyroDataset_for_file = null;
    private boolean mIsRecording = false;
    public AtomicBoolean isWrittingSensorDataLog = new AtomicBoolean(false);

    // Broadcast Related
    public final static String ACTION_BEACON_CONNECT_STATE = "BEACONHANDLER.ACTION_BEACON_CONNECT_STATE";
    public final static String ACTION_BEACON_DISCONNECT_STATE = "BEACONHANDLER.ACTION_BEACON_DISCONNECT_STATE";
    public final static String ACTION_BEACON_FIRST_DATA_RECEIVE = "BEACONHANDLER.ACTION_BEACON_FIRST_DATA_RECEIVE";
    private boolean first_data_flag = false;

    // Gravity Reducing Related
    private static final double GravityReducing_Alpah = 0.8;
    private static double[] gravity = new double[3];

    //Correction coordinates Related
    public float[] virtualX = {0,0,0};
    public float[] virtualY = {0,0,0};
    public float[] virtualZ = {0,0,0};
    public static final long Correct_Corrdinate_Time = 5000; // 決定三軸校正時要花多久時間，須大於時間校正的時間

    public BeaconHandler(Activity activity){
        this.mActivity = activity;
        StrokeTypeClassifier = new StrokeClassifier(activity);
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
    }

    /*********************************/
    /** BeaconHandler Scan Function **/
    /*********************************/
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

    /*****************************************/
    /**  BeaconHandler Connection Function  **/
    /*****************************************/
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
            //避免重複送推播 (Library問題)
            if(!SystemParameters.IsKoalaReady) {
                SystemParameters.IsKoalaReady = true;
                first_data_flag = false;
                Intent broadcast = new Intent(ACTION_BEACON_CONNECT_STATE);
                mActivity.sendBroadcast(broadcast);
            }
        } else {
            SystemParameters.IsKoalaReady = false;
            Intent broadcast = new Intent(ACTION_BEACON_DISCONNECT_STATE);
            mActivity.sendBroadcast(broadcast);
        }
    }

    /*******************************************/
    /** BeaconHandler Data Recording Function **/
    /*******************************************/
    private void initLogFile(int uType){
        //UnCeliAccDataWriter = new LogFileWriter("UnCeliAccData.csv", LogFileWriter.ACCELEROMETER_DATA_TYPE, uType);
        if(uType == LogFileWriter.TESTING_TYPE) {
            AccDataWriter = new LogFileWriter("AccData.csv", LogFileWriter.ACCELEROMETER_DATA_TYPE, uType);
            AccDataReducedWriter = new LogFileWriter("AccData_Reduced.csv", LogFileWriter.ACCELEROMETER_DATA_TYPE, uType);
            GyroDataWriter = new LogFileWriter("GyroData.csv", LogFileWriter.GYROSCOPE_DATA_TYPE, uType);
            Cal_AccDataWriter = new LogFileWriter("Cal_AccData.csv", LogFileWriter.ACCELEROMETER_DATA_TYPE, uType);
        }
        else if(uType == LogFileWriter.CALIBRATION_Y_TYPE)
            AccDataWriter = new LogFileWriter("Cal_Y.csv", LogFileWriter.ACCELEROMETER_DATA_TYPE, uType);
        else if(uType == LogFileWriter.CALIBRATION_Z_TYPE)
            AccDataWriter = new LogFileWriter("Cal_Z.csv", LogFileWriter.ACCELEROMETER_DATA_TYPE, uType);

    }

    private void initParameters(){
        acc_buffer = new LinkedBlockingQueue<SensorData>();
        gyro_buffer = new LinkedBlockingQueue<SensorData>();
        LastCaliTime = 0;

        AccDataset_for_file = new LinkedBlockingQueue<SensorData>();
        GyroDataset_for_file = new LinkedBlockingQueue<SensorData>();
        AccDataset_GravityReduced_for_file = new LinkedBlockingQueue<>();

        AccDataset_for_algo = new LinkedBlockingDeque<SensorData>();
        AccDataset_GravityReduced_for_algo = new LinkedBlockingDeque<SensorData>();
        GyroDataset_for_algo = new LinkedBlockingDeque<SensorData>();

        for(int i = 0; i < gravity.length; i++)
            gravity[i] = 0;
    }

    public void startRecording(int uType) {
        initParameters();
        initLogFile(uType);
        if(uType == LogFileWriter.TESTING_TYPE)
            StrokeTypeClassifier.initLogFile();

        mIsRecording = true;

        StartTimeCalibrationTask();
        startDeleteOldSensorData();
        startLogging(uType);
        if(uType == LogFileWriter.TESTING_TYPE)
            StartCheckClassifyRequest();
    }

    public void stopRecording(){
        mIsRecording = false;
        StopTimeCalibrationTask();
        StrokeTypeClassifier.closeLogFile();
    }

    // 將時間過久的感測器資料捨棄, 避免佔用記憶體空間
    private void startDeleteOldSensorData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(mIsRecording) {
                    if ( !IsFeatureExtracting.get() ) {
                        // Check AccDataset
                        if (AccDataset_for_algo.size() > 0 && SystemParameters.SensorEndTime - AccDataset_for_algo.peek().time > Abandon_Time)
                            AccDataset_for_algo.poll();

                        // Check AccDataset(Gravity Reduced)
                        if (AccDataset_GravityReduced_for_algo.size() > 0 && SystemParameters.SensorEndTime - AccDataset_GravityReduced_for_algo.peek().time > Abandon_Time)
                            AccDataset_GravityReduced_for_algo.poll();

                        // Check GyroDataset
                        if (GyroDataset_for_algo.size() > 0 && SystemParameters.SensorEndTime - GyroDataset_for_algo.peek().time > Abandon_Time)
                            GyroDataset_for_algo.poll();
                    }
                }
            }
        }).start();
    }


    // 開始寫Log File
    private void startLogging(final int uType){
        new Thread(new Runnable() {
            @Override
            public void run() {
                isWrittingSensorDataLog.set(true);
                while( mIsRecording ||  AccDataset_for_file.size() > 0 || GyroDataset_for_file.size() > 0 || AccDataset_GravityReduced_for_file.size() > 0 || IsTimeCalibration.get()){
                    if( AccDataset_for_file.size() > 0 ) {
                        final SensorData acc_data = AccDataset_for_file.poll();
                        try {
                            if(uType == LogFileWriter.TESTING_TYPE) {
                                AccDataWriter.writeInertialDataFile(acc_data.seq, acc_data.time, acc_data.values[0], acc_data.values[1], acc_data.values[2]);
                                final float CalibrationTemp[] = getCorrectionValue(acc_data.values);
                                Cal_AccDataWriter.writeInertialDataFile(acc_data.seq, acc_data.time, CalibrationTemp[0], CalibrationTemp[1], CalibrationTemp[2]);
                            }
                            else if(uType == LogFileWriter.CALIBRATION_Y_TYPE)
                                AccDataWriter.writeInertialDataFile(acc_data.seq, acc_data.time, acc_data.values[0], acc_data.values[1], acc_data.values[2]);
                            else if(uType == LogFileWriter.CALIBRATION_Z_TYPE)
                                AccDataWriter.writeInertialDataFile(acc_data.seq, acc_data.time, acc_data.values[0], acc_data.values[1], acc_data.values[2]);
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                    if( AccDataset_GravityReduced_for_file.size() > 0 ){
                        final SensorData acc_reduce_data = AccDataset_GravityReduced_for_file.poll();
                        try {
                            if( uType == LogFileWriter.TESTING_TYPE)
                                AccDataReducedWriter.writeInertialDataFile(acc_reduce_data.seq, acc_reduce_data.time, acc_reduce_data.values[0], acc_reduce_data.values[1], acc_reduce_data.values[2]);
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                    if( GyroDataset_for_file.size() > 0 ) {
                        final SensorData gyro_data = GyroDataset_for_file.poll();
                        try {
                            if( uType == LogFileWriter.TESTING_TYPE)
                                GyroDataWriter.writeInertialDataFile(gyro_data.seq, gyro_data.time, gyro_data.values[0], gyro_data.values[1], gyro_data.values[2]);
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

                isWrittingSensorDataLog.set(false);
            }
        }).start();
    }

    @Override
    // 收到Sensor Data後處理方式
    public void onSensorChange(final SensorEvent e) {
        // Broadcast when receive first data
        if( !first_data_flag ){
            Intent broadcast = new Intent(ACTION_BEACON_FIRST_DATA_RECEIVE);
            mActivity.sendBroadcast(broadcast);
            first_data_flag = true;
        }

        if( mIsRecording ) {
            final int eventType = e.type;
            final float values[] = new float[3];
            final int seq = (e.seq+SEQ_MAX)%SEQ_MAX;

            if (SystemParameters.isServiceRunning.get()) {
                // Set Time
                long curTime = System.currentTimeMillis();
                long passTime = curTime - SystemParameters.StartTime;


                switch (eventType) {
                    case SensorEvent.TYPE_ACCELEROMETER:
                        values[0] = e.values[0];
                        values[1] = e.values[1];
                        values[2] = e.values[2];

                        SensorData asd = new SensorData(seq, passTime, values);
                        acc_buffer.add(asd);
                        break;
                    case SensorEvent.TYPE_GYROSCOPE:
                        values[0] = e.values[0];
                        values[1] = e.values[1];
                        values[2] = e.values[2];
                        //Log.d(TAG, "time=" + System.currentTimeMillis() + "wX:" + values[0] + "wY:" + values[1] + "wZ:" + values[2] + "\n");

                        SensorData gsd = new SensorData(seq, passTime, values);
                        gyro_buffer.add(gsd);

                        break;
                }
            }
        }
    }

    @Override
    public void onRSSIChange(String addr, float rssi) {
        final int position = findKoalaDevice(addr);
        if (position != -1) {
            //Log.d(TAG, "mac Address:" + addr + " rssi:" + rssi);
        }
    }

    @Override
    public void onKoalaServiceStatusChanged(boolean b) {

    }


    /***************************************/
    /**  BeaconHandler Feature Extraction **/
    /***************************************/
    private void StartCheckClassifyRequest(){
        /*
        The Function Has Problem Now.........
        偵測到擊球後馬上Finish後, 會留在while迴圈出不去, 等待下次開始後, 因為AccDataset_for_algo被清空了, 所以會直接死去

        new Thread() {
            @Override
            public void run() {
                int pointer_idx = 0;
                while(mIsRecording) {
                    if (pointer_idx < SystemParameters.StrokeTimes.size() ) {
                        long StrokeTime = SystemParameters.StrokeTimes.get(pointer_idx);

                        // wait until get Acc get data
                        while (AccDataset_for_algo.size() == 0 || AccDataset_for_algo.peekLast().time < StrokeTime + StrokeClassifier.FeatureExtraction_Beta + TimeCalibration_Period); --> Problem Here!!!!!
                        while (GyroDataset_for_algo.size() == 0 || GyroDataset_for_algo.peekLast().time < StrokeTime + StrokeClassifier.FeatureExtraction_Beta + TimeCalibration_Period);

                        // Get data already, start to classify
                        ClassifyStrokeType(StrokeTime);

                        pointer_idx++;
                    }
                }
            }
        }.start();
        */
    }

    private synchronized void ClassifyStrokeType(final long StrokeTime){
        /** Feature Extraction* */
        IsFeatureExtracting.set(true);

        /** Get Left Part of AccData **/
        ArrayList<float[]> LeftPart_AccData = new ArrayList<float[]>();
        while (AccDataset_for_algo.size() > 0 && AccDataset_for_algo.peek().time < StrokeTime - StrokeClassifier.FeatureExtraction_Alpha)
            AccDataset_for_algo.poll();
        while (AccDataset_for_algo.size() > 0 && AccDataset_for_algo.peek().time < StrokeTime)
            LeftPart_AccData.add( AccDataset_for_algo.poll().values );

        /** Get Right Part of AccData **/
        ArrayList<float[]> RightPart_AccData = new ArrayList<float[]>();
        while (AccDataset_for_algo.size() > 0 && AccDataset_for_algo.peek().time < StrokeTime + StrokeClassifier.FeatureExtraction_Beta)
            RightPart_AccData.add( AccDataset_for_algo.poll().values );

        /** Get Left Part of AccData (Reduce Gravity) **/
        ArrayList<float[]> LeftPart_AccData_ReduceGravity = new ArrayList<float[]>();
        while (AccDataset_GravityReduced_for_algo.size() > 0 && AccDataset_GravityReduced_for_algo.peek().time < StrokeTime - StrokeClassifier.FeatureExtraction_Alpha)
            AccDataset_GravityReduced_for_algo.poll();
        while (AccDataset_GravityReduced_for_algo.size() > 0 && AccDataset_GravityReduced_for_algo.peek().time < StrokeTime)
            LeftPart_AccData_ReduceGravity.add( AccDataset_GravityReduced_for_algo.poll().values );

        /** Get Right Part of AccData (Reduce Gravity) **/
        ArrayList<float[]> RightPart_AccData_ReduceGravity = new ArrayList<float[]>();
        while (AccDataset_GravityReduced_for_algo.size() > 0 && AccDataset_GravityReduced_for_algo.peek().time < StrokeTime + StrokeClassifier.FeatureExtraction_Beta)
            RightPart_AccData_ReduceGravity.add( AccDataset_GravityReduced_for_algo.poll().values );

        /** Get Left Part of GyroData **/
        ArrayList<float[]> LeftPart_GyroData = new ArrayList<float[]>();
        while (GyroDataset_for_algo.size() > 0 && GyroDataset_for_algo.peek().time < StrokeTime - StrokeClassifier.FeatureExtraction_Alpha)
            GyroDataset_for_algo.poll();
        while (GyroDataset_for_algo.size() > 0 && GyroDataset_for_algo.peek().time < StrokeTime)
            LeftPart_GyroData.add( GyroDataset_for_algo.poll().values );

        /** Get Right Part of GyroData **/
        ArrayList<float[]> RightPart_GyroData = new ArrayList<float[]>();
        while (GyroDataset_for_algo.size() > 0 && GyroDataset_for_algo.peek().time < StrokeTime + StrokeClassifier.FeatureExtraction_Beta)
            RightPart_GyroData.add( GyroDataset_for_algo.poll().values );

        final ArrayList<Float> stroke_features =  StrokeTypeClassifier.FeatureExtraction(
                LeftPart_AccData,
                RightPart_AccData,
                LeftPart_AccData_ReduceGravity,
                RightPart_AccData_ReduceGravity,
                LeftPart_GyroData,
                RightPart_GyroData);
        IsFeatureExtracting.set(false);

        /** Classification **/
        StrokeTypeClassifier.Classify(StrokeTime, stroke_features);

    }



    /*****************************************/
    /**  BeaconHandler Calibration Function **/
    /*****************************************/
    // Training average Axis cross product X
    public void StartAxisCalibration(int uType) {
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

    // Correction coordinates
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

    private void StartTimeCalibrationTask(){
        ThreadTimeCalibration = new Thread(){
            @Override
            public void run() {
                while(mIsRecording) {
                    if(SystemParameters.isServiceRunning.get()) {
                        try {
                            sleep(TimeCalibration_Period);
                            inerital_time_calibration();
                        } catch (InterruptedException e) {}
                    }
                }
            }
        };
        ThreadTimeCalibration.start();
    }

    private void StopTimeCalibrationTask(){
        ThreadTimeCalibration.interrupt();
        ThreadTimeCalibration = null;
    }

    private void inerital_time_calibration(){
        new Thread(){
            @Override
            public void run(){
                IsTimeCalibration.set(true);

                // pop current inertial data
                SensorData[] acc_dataset = new SensorData[acc_buffer.size()];
                SensorData[] gyro_dataset = new SensorData[gyro_buffer.size()];
                for (int i = 0; i < acc_dataset.length; i++) {
                    SensorData sd = acc_buffer.poll();
                    acc_dataset[i] = new SensorData(sd.seq, sd.time, sd.values);
                }
                for (int i = 0; i < gyro_dataset.length; i++) {
                    SensorData sd = gyro_buffer.poll();
                    gyro_dataset[i] = new SensorData(sd.seq, sd.time, sd.values);
                }

                if(acc_dataset.length > 0) {
                    SystemParameters.SensorCount += acc_dataset.length;

                    // count all data length (contain loss packet)
                    int dataSize_acc = getSumDataSizeWithLossPacket(acc_dataset, AccDataset_for_algo.peekLast());
                    int dataSize_gyro = getSumDataSizeWithLossPacket(gyro_dataset, GyroDataset_for_algo.peekLast());
                    SystemParameters.SensorCount_ContainLoss += dataSize_acc;

                    // count current frequency in a period time
                    double currentTime = (double) (System.currentTimeMillis() - SystemParameters.StartTime);
                    double Period = (currentTime - LastCaliTime); //不直接使用Calibraiton_Time, Thread的執行時間會有誤差, 累積下來很可觀
                    double deltaT_acc = Period / dataSize_acc;
                    double deltaT_gyro = Period / dataSize_gyro;

                    // set time to dataset
                    SensorData[] new_acc_dataset = setCaliedTime(acc_dataset, dataSize_acc, deltaT_acc, LastCaliTime, AccDataset_for_algo.peekLast());
                    SensorData[] new_gyro_dataset = setCaliedTime(gyro_dataset, dataSize_gyro, deltaT_gyro, LastCaliTime, GyroDataset_for_algo.peekLast());
                    LastCaliTime += Period;
                    SystemParameters.SensorEndTime = (long) LastCaliTime;

                    // reduce gravity for accData
                    final SensorData[] reduced_acc_dataset = ReduceGravity(new_acc_dataset);

                    // Add data into deque
                    for(int i = 0; i < new_acc_dataset.length; i++){
                        AccDataset_for_algo.add(new_acc_dataset[i]);
                        AccDataset_for_file.add(new_acc_dataset[i]);
                    }
                    for(int i = 0; i < new_gyro_dataset.length; i++){
                        GyroDataset_for_algo.add(new_gyro_dataset[i]);
                        GyroDataset_for_file.add(new_gyro_dataset[i]);
                    }
                    for(int i = 0; i < reduced_acc_dataset.length; i++){
                        AccDataset_GravityReduced_for_algo.add(reduced_acc_dataset[i]);
                        AccDataset_GravityReduced_for_file.add(reduced_acc_dataset[i]);
                    }
                }
                IsTimeCalibration.set(false);
            }
        }.start();
    }

    private int getSumDataSizeWithLossPacket(final SensorData[] dataset, final SensorData prev_data){
        int dataSize = dataset.length;
        int prev_seq = (prev_data == null) ? -1 : prev_data.seq;
        for(int i = 0; i < dataset.length; i++){
            if(prev_seq == -1 ) { // 第一個校正Window的資料集, 沒有前一筆資料
                prev_seq = dataset[i].seq;
                continue;
            }
            int cur_seq = (dataset[i].seq < prev_seq) ? (dataset[i].seq+SEQ_MAX) : dataset[i].seq;

            dataSize += (cur_seq-prev_seq-1); // loss packet num
            prev_seq = dataset[i].seq;
        }
        return dataSize;
    }

    private final SensorData[] setCaliedTime(final SensorData[] dataset, final int DataSizeWithLoss ,double deltaT, double CurrentDataTime, final SensorData prev_sensor_data){
        SensorData[] new_dataset = new SensorData[DataSizeWithLoss];
        SensorData prev_data = (prev_sensor_data == null) ? null : new SensorData(prev_sensor_data.seq,prev_sensor_data.time,prev_sensor_data.values);
        int dataCount = 0;

        for(int i = 0; i < dataset.length; i++){
            if(prev_data == null){
                new_dataset[dataCount] = new SensorData(dataset[i].seq, (long)CurrentDataTime, dataset[i].values);
                prev_data = new_dataset[dataCount];
                CurrentDataTime += deltaT;
                dataCount++;

            }else{
                int prev_seq = prev_data.seq;
                int cur_seq = (dataset[i].seq < prev_seq) ? (dataset[i].seq+SEQ_MAX) : dataset[i].seq;
                for(int j = 1; j <= (cur_seq-prev_seq); j++){
                    float[] values = new float[3];
                    int X = prev_seq + j;
                    for(int k = 0; k < values.length; k++)
                        values[k] = ((X - prev_seq)/(float)(cur_seq - prev_seq)) * (dataset[i].values[k] - prev_data.values[k]) + prev_data.values[k];

                    if(X >= SEQ_MAX)
                        new_dataset[dataCount] = new SensorData(X-SEQ_MAX, (long)CurrentDataTime, values);
                    else
                        new_dataset[dataCount] = new SensorData(X, (long)CurrentDataTime, values);

                    prev_data = new_dataset[dataCount];
                    CurrentDataTime += deltaT;
                    dataCount++;
                }
            }
        }
        return new_dataset;
    }

    private SensorData[] ReduceGravity(final SensorData[] ori_dataset){
        SensorData[] result = new SensorData[ori_dataset.length];
        for(int i = 0; i < ori_dataset.length; i++) {
            gravity[0] = ori_dataset[i].values[0] * (1.0 - GravityReducing_Alpah) + GravityReducing_Alpah * gravity[0];
            gravity[1] = ori_dataset[i].values[1] * (1.0 - GravityReducing_Alpah) + GravityReducing_Alpah * gravity[1];
            gravity[2] = ori_dataset[i].values[2] * (1.0 - GravityReducing_Alpah) + GravityReducing_Alpah * gravity[2];

            // reduce gravity
            float[] new_values = new float[3];
            for(int j = 0; j < new_values.length; j++)
                new_values[j] = (float)(ori_dataset[i].values[j] - gravity[j]);

            result[i] = new SensorData(ori_dataset[i].seq,ori_dataset[i].time ,new_values);
        }
        return result;
    }

    public class SensorData{
        public int seq;
        public long time;
        public float values[];
        public SensorData(int seq, long time , float[] vals){
            this.seq = seq;
            this.time = time;
            this.values = new float[3];
            for(int i = 0; i < 3; i++)
                this.values[i] = vals[i];
        }
    }

}
