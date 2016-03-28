package nctu.nol.bt.devices;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cc.nctu1210.api.koala6x.KoalaDevice;
import cc.nctu1210.api.koala6x.KoalaService;
import cc.nctu1210.api.koala6x.KoalaServiceManager;
import cc.nctu1210.api.koala6x.SensorEvent;


public class BeaconHandler{
    private final static String TAG = BeaconHandler.class.getSimpleName();

    // Activity Related
    private Activity mActivity;

    // Beacon Related
    private KoalaServiceManager mServiceManager;
    /******** for SDK version > 21 **********/
    private BluetoothLeScanner mBLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    /******** for SDK version > 21 **********/
    public static ArrayList<KoalaDevice> mDevices = new ArrayList<KoalaDevice>();  // Manage the devices
    public static ArrayList<AtomicBoolean> mFlags = new ArrayList<AtomicBoolean>();

    private static final long SCAN_PERIOD = 2000;


    public BeaconHandler(Activity activity){
        this.mActivity = activity;
    }
/*
    public void initBLEService(){
        mServiceManager = new KoalaServiceManager(mActivity);
        mServiceManager.registerSensorEventListener(this, SensorEvent.TYPE_ACCELEROMETER, KoalaService.MOTION_WRITE_RATE_20, KoalaService.MOTION_ACCEL_SCALE_16G, KoalaService.MOTION_GYRO_SCALE_500);
        //mServiceManager.registerSensorEventListener(this, SensorEvent.TYPE_ACCELEROMETER);
        mServiceManager.registerSensorEventListener(this, SensorEvent.TYPE_GYROSCOPE);

    }
*/




}
