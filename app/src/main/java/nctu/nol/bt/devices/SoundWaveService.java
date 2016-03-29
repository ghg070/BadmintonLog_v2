package nctu.nol.bt.devices;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class SoundWaveService extends Service {
	private final static String TAG = SoundWaveService.class.getName();
    public final static String ACTION_DETECT_CONNECTION_STATE = "SoundWaveService.ACTION_DETECT_CONNECTION_STATE";
	
	
	private final IBinder mBinder = new LocalBinder();
	
	private BluetoothAdapter mBluetoothAdapter = null;
    private final List<BluetoothDevice> mBoundedDevices = new ArrayList<BluetoothDevice>();	


    //Connected Device
    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothA2dp mBluetoothA2DP;
		
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	public class LocalBinder extends Binder {
        public SoundWaveService getService() {
            return SoundWaveService.this;
        }
    }	

	public boolean initialize() {
		final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        SetProfileProxy();
        return true;
	}
	
	public final List<BluetoothDevice> getBoundedBTDevices() {
		mBoundedDevices.clear();
		mBoundedDevices.addAll(mBluetoothAdapter.getBondedDevices());
		return mBoundedDevices;
	}
	
	public void ScanBTDevice() {
		stopScanBTDevice();
		mBluetoothAdapter.startDiscovery(); 
	}
	
	public void stopScanBTDevice() {
	    if (mBluetoothAdapter.isDiscovering())
	    	mBluetoothAdapter.cancelDiscovery();
	}
	
	public final List<BluetoothDevice> getBondedBTDevice(int DeviceType){
		List<BluetoothDevice> result = new ArrayList<BluetoothDevice>();
		
		if(mBluetoothAdapter != null){
			Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
			for(BluetoothDevice d : devices){
				BluetoothClass bluetoothClass = d.getBluetoothClass();
	            if (bluetoothClass != null) {
	              	int deviceClass = bluetoothClass.getDeviceClass();
	               	if( deviceClass == DeviceType) 
	               		result.add(d);
	            }
			}
		}
		
		return result;
	}
	
	
	public void ConnectScoBTHeadset(final BluetoothDevice d){
		DisconnectAllScoBTHeadset();
		try {			
			Method connect = BluetoothHeadset.class.getDeclaredMethod("connect", BluetoothDevice.class);
			connect.setAccessible(true);
            connect.invoke(mBluetoothHeadset, d);
            
            connect = BluetoothA2dp.class.getDeclaredMethod("connect", BluetoothDevice.class);
			connect.setAccessible(true);
            connect.invoke(mBluetoothA2DP, d);
            
        }catch (Exception e){
        	Log.e(TAG,"ConnectScoBTHeadset: "+e.getMessage());
        }
	}
	
	public void DisconnectAllScoBTHeadset(){
		try {
			if(mBluetoothHeadset != null){
				for(BluetoothDevice d:mBluetoothHeadset.getConnectedDevices()){
					Method disconnect = BluetoothHeadset.class.getDeclaredMethod("disconnect", BluetoothDevice.class);
					disconnect.setAccessible(true);
					disconnect.invoke(mBluetoothHeadset, d);
				}
			}
			
			if(mBluetoothA2DP != null){
				for(BluetoothDevice d:mBluetoothA2DP.getConnectedDevices()){
					Method disconnect = BluetoothA2dp.class.getDeclaredMethod("disconnect", BluetoothDevice.class);
					disconnect.setAccessible(true);
					disconnect.invoke(mBluetoothA2DP, d);
				}
			}
	    }catch (Exception e){
	    	Log.e(TAG,"DisconnectScoBTHeadset: "+e.getMessage());
	    } 
	}
	
	
	public void close() {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        DisconnectAllScoBTHeadset();
        UnsetProfileProxy();
	}
	

	public void SetProfileProxy(){
		if(mBluetoothAdapter != null){
			mBluetoothAdapter.getProfileProxy(this, ProfileListener, BluetoothProfile.HEADSET );
			mBluetoothAdapter.getProfileProxy(this , ProfileListener, BluetoothProfile.A2DP);
		}
		else
			Log.e(TAG,"BluetoothAdapter not initialized");
	}
	public void UnsetProfileProxy(){
		if(mBluetoothAdapter != null){
			mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
			mBluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, mBluetoothA2DP);
			mBluetoothHeadset = null;
			mBluetoothA2DP = null;
		}
		else
			Log.e(TAG,"BluetoothAdapter not initialized");
	}
	public final BluetoothProfile.ServiceListener ProfileListener = new  BluetoothProfile.ServiceListener() {
	    @Override

	    public void onServiceConnected(int i, final BluetoothProfile bluetoothProfile) {
	    	Log.d(TAG,"onServiceConnected");
	    	    		
	        if (i == BluetoothProfile.HEADSET){
	            mBluetoothHeadset = (BluetoothHeadset) bluetoothProfile;
	           
	            for(BluetoothDevice d:mBluetoothHeadset.getConnectedDevices()){
	            	
	            	Intent intent = new Intent(ACTION_DETECT_CONNECTION_STATE);    
	                intent.putExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED); 
	                intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_CONNECTED);
	                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, d); 
	                sendBroadcast(intent);
	            }
	            
	        }else if(i == BluetoothProfile.A2DP){
	        	mBluetoothA2DP = (BluetoothA2dp) bluetoothProfile;
	        }
	        
	        //DisconnectAllScoBTHeadset();
	    }
	    @Override
	    public void onServiceDisconnected(int i) {
	        Log.d(TAG,"onServiceDisconnected");
	    }
	};
	
	
}
