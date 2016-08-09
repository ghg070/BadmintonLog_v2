package nctu.nol.badmintonlogprogram;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import nctu.nol.account.NetworkCheckService;
import nctu.nol.algo.StrokeClassifier;
import nctu.nol.algo.StrokeDetector;
import nctu.nol.bt.devices.BeaconHandler;
import nctu.nol.bt.devices.SoundWaveHandler;
import nctu.nol.file.LogFileWriter;
import nctu.nol.file.SystemParameters;


public class MainActivity extends FragmentActivity {

	private final static String TAG = "MainActivity";
	public final static int KOALA_SCAN_PAGE_RESULT = 11;
	
	/* BT related */
	private BluetoothAdapter mBluetoothAdapter = null;	
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_OPEN_BTSETTING = 2;
    
	/* Sound Wave Related */
	private SoundWaveHandler sw = null;
	private Button btMicConnect;

	/* Beacon Connect Related */
	private BeaconHandler bh = null;
	private Button btKoalaConnect;
	private ProgressDialog WaitConnectDialog = null;

	/* Fragment */
	private LoggingFragment curFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        //Bluetooth Initial
        initialBTManager();

		//View & Event Initial
		initialViewandEvent();
    }
    
    @Override
    protected void onResume(){
    	super.onResume();
    }
       
    @Override
    protected void onPause(){
    	super.onPause();
    }
    
    @Override
	protected void onDestroy(){
		super.onDestroy();

		if (sw != null){
			sw.deleteObject();
			sw = null;
		}

		if(bh != null){
			bh.deleteObject();
			bh = null;
		}

		unregisterReceiver(mSoundWaveHandlerStateUpdateReceiver);
		unregisterReceiver(mKoalaStateUpdateReceiver);

        Intent intent = new Intent(MainActivity.this,NetworkCheckService.class);
        stopService(intent);

		System.exit(0);
		return;
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT){
            if(resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
            	Log.e(TAG,"Bluetooth is not enabled.");
                finish();
                return;
            }else if(resultCode == Activity.RESULT_OK){}
        }else if(requestCode == REQUEST_OPEN_BTSETTING){}

		else if(requestCode == KOALA_SCAN_PAGE_RESULT){
			if(resultCode == Activity.RESULT_OK && bh != null){
				final String clickedMacAddress = data.getExtras().getString(KoalaScan.macAddress);
				final String clickedDeviceName = data.getExtras().getString(KoalaScan.deviceName);
				//CurKoalaDevice = clickedDeviceName + "-" +clickedMacAddress;
				bh.ConnectToKoala(clickedMacAddress);

				WaitConnectDialog = ProgressDialog.show(this, "連線中", "請稍後...", true);
				WaitConnectDialog.show();

				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					public void run() {
						if (WaitConnectDialog.isShowing()) {
							WaitConnectDialog.dismiss();
							Toast.makeText(MainActivity.this, "Connect fail, please retry.", Toast.LENGTH_SHORT).show();
						}
					}
				}, 15000);  // 15 seconds
			}
		}
        super.onActivityResult(requestCode, resultCode, data);
    }
    
	private void initialViewandEvent(){
		//Button
		btMicConnect = (Button) findViewById(R.id.bt_micconnect);
		btKoalaConnect = (Button) findViewById(R.id.bt_koalaconnect);

		btMicConnect.setOnClickListener(MicConnectListener);
		btKoalaConnect.setOnClickListener(KoalaConnectListener);

		changeFragment(LoggingFragment.newInstance(MainActivity.this, sw, bh, LoggingFragment.TRAINING_TYPE));
	}
	private void initialBTManager() {
		Log.d(TAG, "Check if BT is enable");
		//Check BT Enabled
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	    }
		
		Log.d(TAG, "Bind BT Service");

		//Initial SoundWave Handler
		sw = new SoundWaveHandler(MainActivity.this);
		registerReceiver(mSoundWaveHandlerStateUpdateReceiver, makeSoundWaveHandlerStateUpdateIntentFilter());

		//Initial Beacon Handler
		bh = new BeaconHandler(MainActivity.this);
		registerReceiver(mKoalaStateUpdateReceiver, makeKoalaStateUpdateIntentFilter());

	}

    
    /**********************/
    /**    Broadcast Event	 **/
	/**********************/
	private final BroadcastReceiver mKoalaStateUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if( BeaconHandler.ACTION_BEACON_CONNECT_STATE.equals(action) ){
				btKoalaConnect.setBackground(getResources().getDrawable(R.drawable.koala_connect));
			}else if( BeaconHandler.ACTION_BEACON_DISCONNECT_STATE.equals(action) ){
				btKoalaConnect.setBackground(getResources().getDrawable(R.drawable.koala_disconnect));
				curFragment.InterruptLogging(LoggingFragment.DEVICE_KOALA);
			}else if( BeaconHandler.ACTION_BEACON_FIRST_DATA_RECEIVE.equals(action) ){
				WaitConnectDialog.dismiss();

				// Active Calibration
				SystemParameters.initializeSystemParameters();
				for(int i = 0; i < 2; i++)
					ActiveCalibration(i);
			}
		}
	};
	private static IntentFilter makeKoalaStateUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(BeaconHandler.ACTION_BEACON_CONNECT_STATE);
		intentFilter.addAction(BeaconHandler.ACTION_BEACON_DISCONNECT_STATE);
		intentFilter.addAction(BeaconHandler.ACTION_BEACON_FIRST_DATA_RECEIVE);

		return intentFilter;
	}

	private final BroadcastReceiver mSoundWaveHandlerStateUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if( SoundWaveHandler.ACTION_SOUND_SERVICE_CONNECT_STATE.equals(action) ) {}
			else if( SoundWaveHandler.ACTION_SOUND_NOT_PREPARE_STATE.equals(action) ){
				btMicConnect.setBackground(getResources().getDrawable(R.drawable.headset_disconnect));
				btMicConnect.setEnabled(true);
				btKoalaConnect.setEnabled(true);
				curFragment.InterruptLogging(LoggingFragment.DEVICE_HEADSET);

			}else if( SoundWaveHandler.ACTION_SOUND_PREPARING_STATE.equals(action) ){
				btMicConnect.setEnabled(false);
				btKoalaConnect.setEnabled(false);

			}else if( SoundWaveHandler.ACTION_SOUND_PREPARED_STATE.equals(action) ){
				btMicConnect.setBackground(getResources().getDrawable(R.drawable.headset_connect));
				btMicConnect.setEnabled(true);
				btKoalaConnect.setEnabled(true);
			}
		}
	};
	private static IntentFilter makeSoundWaveHandlerStateUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(SoundWaveHandler.ACTION_SOUND_SERVICE_CONNECT_STATE);
		intentFilter.addAction(SoundWaveHandler.ACTION_SOUND_NOT_PREPARE_STATE);
		intentFilter.addAction(SoundWaveHandler.ACTION_SOUND_PREPARING_STATE);
		intentFilter.addAction(SoundWaveHandler.ACTION_SOUND_PREPARED_STATE);
		    
		return intentFilter;
	}


	/********************/
	/** Connecting Event **/
	/********************/
	private Button.OnClickListener MicConnectListener = new Button.OnClickListener() {
		public void onClick(View v) {
		if( !SystemParameters.IsBtHeadsetReady ) {
			Intent BTSettingIntent = new Intent(Intent.ACTION_MAIN, null);
			BTSettingIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			ComponentName cn = new ComponentName("com.android.settings",
					"com.android.settings.bluetooth.BluetoothSettings");
			BTSettingIntent.setComponent(cn);
			startActivityForResult(BTSettingIntent, REQUEST_OPEN_BTSETTING);
		}
		else
			sw.getService().DisconnectAllScoBTHeadset();
		}
	};

	private Button.OnClickListener KoalaConnectListener = new Button.OnClickListener() {
		public void onClick(View v) {
			if(SystemParameters.IsKoalaReady)
				bh.DisconnectToKoala();
			else{
				Intent i = new Intent(MainActivity.this, KoalaScan.class);
				startActivityForResult(i, KOALA_SCAN_PAGE_RESULT);
			}
		}
	};

	/************************
	 *  Axis Calibration Related
	 ***********************/
	/* Calibration UI */
	public void ActiveCalibration(int type) {
		String Title,Message;
		final int TypeTemp ;
		if(type == 0) {
			Title = "Calibration Z";
			Message = "請將拍子垂直朝下";
			TypeTemp = LogFileWriter.CALIBRATION_Z_TYPE;
		}
		else {
			Title = "Calibration Y";
			Message = "請將拍子水平放置";
			TypeTemp = LogFileWriter.CALIBRATION_Y_TYPE;
		}

		AlertDialog.Builder CalZDialog = new AlertDialog.Builder(MainActivity.this);
		CalZDialog.setTitle(Title)
				.setMessage(Message)
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						showAxisCalibrationProcessDialog(TypeTemp);
					}
				}).show();
	}

	private void showAxisCalibrationProcessDialog(final int LogType) {
		final ProgressDialog Cal_dialog = ProgressDialog.show(MainActivity.this, "校正中", "計算校正軸，請稍後",true);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					bh.startRecording(LogType);
					//Service Start
					SystemParameters.SetMeasureStartTime();
					SystemParameters.isServiceRunning.set(true);
					Thread.sleep(bh.Correct_Corrdinate_Time);
					bh.stopRecording();
					SystemParameters.isServiceRunning.set(false);
					while(bh.isWrittingSensorDataLog.get()); //wait logging
					bh.StartAxisCalibration(LogType);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
				} finally {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Cal_dialog.dismiss();
						}
					});
				}
			}
		}).start();
	}



	private void changeFragment(Fragment f) {
		curFragment = (LoggingFragment)f;

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.replace(R.id.fragment_container, f);
		transaction.commitAllowingStateLoss();
	}


	private final int group1Id = 1;
	private final int VoicePrintId = Menu.FIRST;
	private final int PlayId = Menu.FIRST +1;
	private final int DataListId = Menu.FIRST +2;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(group1Id, VoicePrintId, VoicePrintId, "Voiceprint");
		menu.add(group1Id, PlayId, PlayId, "Play");
		menu.add(group1Id, DataListId, DataListId, "Data List");

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(!SystemParameters.isServiceRunning.get()) {
			switch (item.getItemId()) {
				case VoicePrintId:
					// write your code here
					changeFragment(LoggingFragment.newInstance(MainActivity.this, sw, bh, LoggingFragment.TRAINING_TYPE));
					return true;

				case PlayId:
					// write your code here
					changeFragment(LoggingFragment.newInstance(MainActivity.this, sw, bh, LoggingFragment.TESTING_TYPE));
					return true;

				case DataListId:
					// write your code here
					Intent i = new Intent(MainActivity.this, DataListPage.class);
					startActivity(i);
					return true;
				default:
					return super.onOptionsItemSelected(item);
			}
		}else {
			Toast.makeText(MainActivity.this, "Please stop logging before selecting sub-page", Toast.LENGTH_SHORT).show();
			return false;
		}
	}

}




