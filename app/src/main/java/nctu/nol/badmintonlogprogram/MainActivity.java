package nctu.nol.badmintonlogprogram;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import nctu.nol.algo.FrequencyBandModel;
import nctu.nol.algo.PeakDetector;
import nctu.nol.algo.ScoreComputing;
import nctu.nol.algo.StrokeClassifier;
import nctu.nol.algo.StrokeDetector;
import nctu.nol.bt.devices.BeaconHandler;
import nctu.nol.bt.devices.SoundWaveHandler;
import nctu.nol.bt.devices.SoundWaveHandler.AudioData;
import nctu.nol.file.SystemParameters;
import nctu.nol.file.LogFileWriter;
import nctu.nol.file.sqlite.DataListItem;

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
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

	private final static String TAG = "MainActivity";
	public final static int KOALA_SCAN_PAGE_RESULT = 11;
	
	/* BT related */
	private BluetoothAdapter mBluetoothAdapter = null;	
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_OPEN_BTSETTING = 2;
	
    /* Log Related */ 
  	private static LogFileWriter ReadmeWriter;
  	private Button btTraining;
  	private Button btTesting;
	private Boolean isTraining = false;
	private Boolean isTesting = false;

	/* View History Related */
	private Button btDataPage;
    
	/* Sound Wave Related */
	private SoundWaveHandler sw = null;
	private Button btMicConnect;

	/* Beacon Connect Related */
	private BeaconHandler bh = null;
	private Button btKoalaConnect;
	private ProgressDialog WaitConnectDialog = null;

    /* Algorithm Related */
    private FrequencyBandModel fbm = null;
	private ScoreComputing SC = null;
    
	/* Stroke */
	private TextView tv_strokeCount;
	private TextView tv_strokeType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		//View & Event Initial
        initialViewandEvent();
        
        //Bluetooth Initial
        initialBTManager();

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
		unregisterReceiver(mStrokeCountUpdateReceiver);
		unregisterReceiver(mStrokeTypeResultReceiver);

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
		//TextView
		tv_strokeCount = (TextView) findViewById(R.id.tv_stroke_count);
		tv_strokeType = (TextView) findViewById(R.id.tv_stroke_type);

		//Button
		btMicConnect = (Button) findViewById(R.id.bt_micconnect);
		btKoalaConnect = (Button) findViewById(R.id.bt_koalaconnect);
		btTraining = (Button) findViewById(R.id.bt_trainingstart);
		btTesting = (Button) findViewById(R.id.bt_testingstart);
		btDataPage = (Button) findViewById(R.id.bt_viewstrokedata);

		btMicConnect.setOnClickListener(MicConnectListener);
		btKoalaConnect.setOnClickListener(KoalaConnectListener);
		btTraining.setOnClickListener(TrainingStartClickListener);
		btTesting.setOnClickListener(TestingStartClickListener);
		btDataPage.setOnClickListener(DataPageListener);

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
		registerReceiver(mKoalaStateUpdateReceiver,makeKoalaStateUpdateIntentFilter());

		//Initial StrokeDetector
		registerReceiver(mStrokeCountUpdateReceiver,makeStrokeCountUpdateIntentFilter());

		// Initial StrokeClassifier
		registerReceiver(mStrokeTypeResultReceiver, makeStrokeTypeResultIntentFilter());
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
				if( SystemParameters.isServiceRunning.get() && isTesting)
					btTesting.performClick();
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

            	if( SystemParameters.isServiceRunning.get() && isTraining )//if Logging is running
            		btTraining.performClick();
				else if( SystemParameters.isServiceRunning.get() && isTesting)
					btTesting.performClick();

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
	private final BroadcastReceiver mStrokeCountUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if( StrokeDetector.ACTION_STROKE_DETECTED_STATE.equals(action) ){
				SystemParameters.StrokeCount++;
				tv_strokeCount.setText(SystemParameters.StrokeCount + "");

				long stroke_time = intent.getLongExtra(StrokeDetector.EXTRA_STROKETIME,0);
			}
		}
	};
	private static IntentFilter makeStrokeCountUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(StrokeDetector.ACTION_STROKE_DETECTED_STATE);

		return intentFilter;
	}
	private final BroadcastReceiver mStrokeTypeResultReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if( StrokeClassifier.ACTION_OUTPUT_RESULT_STATE.equals(action) ){
				String stroke_type = intent.getStringExtra(StrokeClassifier.EXTRA_TYPE);
				tv_strokeType.setText(stroke_type);
			}
		}
	};
	private static IntentFilter makeStrokeTypeResultIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(StrokeClassifier.ACTION_OUTPUT_RESULT_STATE);
		return intentFilter;
	}

	/*********************
	 *    Page Change Related
	 *********************/
	private Button.OnClickListener DataPageListener = new Button.OnClickListener() {
		public void onClick(View v) {
			Intent i = new Intent(MainActivity.this, DataListPage.class);
			startActivity(i);
		}
	};


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

    /********************/
    /** Logging Event **/
	/********************/
    private Button.OnClickListener TrainingStartClickListener = new Button.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if(SystemParameters.IsBtHeadsetReady && !isTraining)
				ActiveLogging(LogFileWriter.TRAINING_TYPE);
			else if(isTraining)
				StopLogging();
			else
				Toast.makeText(MainActivity.this,"You have to connect bt headset.",Toast.LENGTH_SHORT).show();
		}
	};

	private Button.OnClickListener TestingStartClickListener = new Button.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if(fbm != null && fbm.CheckModelHasTrained()){
				if(SystemParameters.IsBtHeadsetReady/* && SystemParameters.IsKoalaReady*/ && !isTesting)
					ActiveLogging(LogFileWriter.TESTING_TYPE);
				else if(isTesting)
					StopLogging();
				else
					Toast.makeText(MainActivity.this,"You have to connect bt headset and koala.",Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(getBaseContext(), "You must train your racket first.", Toast.LENGTH_SHORT).show();
			}
		}
	};

	private void ActiveLogging(final int LogType){
		//SystemParameters Initial
		SystemParameters.initializeSystemParameters();

		//UI Button Control
		if(LogType == LogFileWriter.TESTING_TYPE) {
			btTesting.setText(R.string.Testing_State);
			btTraining.setEnabled(false);
			isTesting = true;
		}
		else{
			btTraining.setText(R.string.Training_State);
			btTesting.setEnabled(false);
			isTraining = true;
		}

		//Initial Log File
		ReadmeWriter = new LogFileWriter("Readme.txt", LogFileWriter.README_TYPE, LogType);

		new Thread(){
			@Override
			public void run() {
				try {
					// Trigger Sensor to Ready (wait isServiceRunning become true)
					sw.startRecording(LogType);
					if( isTesting ) bh.startRecording(LogType);

					sleep(2000); //等待2sec後開始
					SetMeasureStartTime(); //設定開始時間
					SystemParameters.isServiceRunning.set(true);

					// if isTest == true, Testing Start
					if(isTesting) StartTestingAlgo();

					runOnUiThread(new Runnable() {
						public void run(){
						Toast.makeText(getBaseContext(), "Log Service is Start", Toast.LENGTH_SHORT).show();
						//init UI
						tv_strokeCount.setText("0");
						tv_strokeType.setText("None");
						}
					});
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Log.e(TAG,e.getMessage());
				}
			}
		}.start();
	}

	private void StopLogging(){
		Toast.makeText(getBaseContext(), "Log Service is Stop", Toast.LENGTH_SHORT).show();
		SystemParameters.isServiceRunning.set(false);
		SystemParameters.Duration = (System.currentTimeMillis() - SystemParameters.StartTime)/1000.0;
		sw.stopRecording();
		if( isTesting ) bh.stopRecording();

		final ProgressDialog dialog = ProgressDialog.show(MainActivity.this,
				"寫檔中", "處理檔案中，請稍後",true);

		new Thread(){
			public void run(){
				//Wait log file write done
				if( sw != null ) while(sw.isWrittingAudioDataLog.get());
				if( SC != null ) while(SC.isWrittingWindowScore.get());
				if( bh != null ) while(bh.isWrittingSensorDataLog.get());

				if(isTraining) StartTrainingAlgo(sw);

				// Local Database Handler
				long id = SQLiteInsertNewLoggingRecord(SystemParameters.StartDate, "ghg070", SystemParameters.StrokeCount, SystemParameters.filePath, isTesting, SystemParameters.SoundStartTime-SystemParameters.StartTime);

				//Show UI
				runOnUiThread(new Runnable() {
					public void run() {
						showLogInformationDialog();

						//UI Button Control
						if (isTesting) {
							btTesting.setText(R.string.Not_Testing_State);
							btTraining.setEnabled(true);
							isTesting = false;
						} else if (isTraining) {
							btTraining.setText(R.string.Not_Training_State);
							btTesting.setEnabled(true);
							isTraining = false;
						}
						dialog.dismiss();
					}
				});
			}
		}.start();
	}
	
	private void SetMeasureStartTime(){
		//Set time
		long currentTime = System.currentTimeMillis();
	    Time t=new Time();
		t.set(currentTime);
	    String year = String.valueOf(t.year);
	    String month = String.valueOf(t.month+1);
	    String day = String.valueOf(t.monthDay);
	    int hour =t.hour;
	    int minute = t.minute;
	    int second = t.second;
	    int millisecond = (int)(currentTime%1000);
	    
	    //YYYYMMDD-HHMMSS
	    String date = year+"-"+month+"-"+day+" "+String.format("%02d:%02d:%02d.%03d",hour, minute, second, millisecond);
		SystemParameters.StartDate = date;
		SystemParameters.StartTime = currentTime;
	}

    private void showLogInformationDialog(){//and also write readme.txt	
		try {
			if(ReadmeWriter != null){
				ReadmeWriter.writeReadMeFile();
				ReadmeWriter.closefile();
			}	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
		alertDialogBuilder.setTitle("Log Information")
						.setMessage("Duration: " + SystemParameters.Duration + "sec\n"
								+ 	"SoundFile: "+SystemParameters.AudioCount+" records\n"
								+	"InertialFile: "+SystemParameters.SensorCount+" records\n"
						).setPositiveButton("OK",new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,int id) {}
		}).show();
	}

	/************************
	 *  Local Database Related
	 ***********************/
	private long SQLiteInsertNewLoggingRecord(String date, String subject, int stroke_num, String path, boolean is_testing, long offset){
		DataListItem dlistDB = new DataListItem(MainActivity.this);
		long id = dlistDB.insert(date, subject, stroke_num, path, is_testing, offset);
		dlistDB.close();
		return id;
	}


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
			Message = "請將拍子平放，熊耳朝上";
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
					SetMeasureStartTime();
					SystemParameters.isServiceRunning.set(true);
					Thread.sleep(bh.Correct_Corrdinate_Time);
					bh.stopRecording();
					SystemParameters.isServiceRunning.set(false);
					while(bh.isWrittingSensorDataLog.get()); //wait logging
					bh.StartAxisCalibration(LogType);
				} catch (Exception e) {
					Log.e(TAG,e.getMessage());
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

    /***********************/
    /** Algorithm Related **/
    /***********************/
    private void StartTrainingAlgo(final SoundWaveHandler sw){
    	//split time array and data array
		final LinkedBlockingQueue<AudioData> ads = sw.getSampleData();
		float times[] = new float[ads.size()],
				vals[] = new float[ads.size()];

		Iterator<AudioData> it = ads.iterator();
		int count = 0;
		while(it.hasNext()){
			AudioData ad = it.next();
			times[count] = (float)ad.time;
			vals[count] = ad.data;
			count++;
		}
		
		//Find all peak
		PeakDetector pd = new PeakDetector(700, 350);
		List<Integer> peaks = pd.findPeakIndex(times, vals, (float)0.35);
		SystemParameters.StrokeCount = peaks.size();


		// Find top K freq band
		fbm = new FrequencyBandModel();
		Vector<FrequencyBandModel.MainFreqInOneWindow> AllMainFreqBands = fbm.FindSpectrumMainFreqs(peaks, vals, SoundWaveHandler.SAMPLE_RATE);
		fbm.setTopKFreqBandTable(AllMainFreqBands, peaks.size());
		List<HashMap.Entry<Float, Float>> TopKMainFreqs = fbm.getTopKMainFreqBandTable();

		//Count Stroke Detector's Threshold
		StrokeDetector.ComputeScoreThreshold(TopKMainFreqs,vals,SoundWaveHandler.SAMPLE_RATE, FrequencyBandModel.FFT_LENGTH);


		// Test File for All Spectrum Main Freq Bands
		LogFileWriter AllSpectrumMainFreqsTestWriter = new LogFileWriter("AllSpectrumMainFreqs.csv", LogFileWriter.OTHER_TYPE, LogFileWriter.TRAINING_TYPE);
		for(int i = 0; i < AllMainFreqBands.size(); i++){
			FrequencyBandModel.MainFreqInOneWindow mf = AllMainFreqBands.get(i);

			float [] sortedFreq = new float[mf.freqbands.length];
			float [] sortedPower = new float[mf.freqbands.length];
			for(int j = 0; j < mf.freqbands.length; j++){
				sortedFreq[j] = mf.freqbands[j].Freq;
				sortedPower[j] = mf.freqbands[j].Power;
			}
			try {
				AllSpectrumMainFreqsTestWriter.writeFreqPeakIndexFile(mf.peak_num, mf.window_num, sortedFreq, sortedPower);
			} catch (IOException e) {
				Log.e(TAG,e.getMessage());
			}
		}
		AllSpectrumMainFreqsTestWriter.closefile();

		//Test File for Top K Freq Band Table
		LogFileWriter TopKMainFreqTableWriter = new LogFileWriter("TopKMainFreqTable.csv", LogFileWriter.OTHER_TYPE, LogFileWriter.TRAINING_TYPE);
		for(int i = 0; i < TopKMainFreqs.size(); i++){
			HashMap.Entry<Float, Float> entry = TopKMainFreqs.get(i);
			float freq = entry.getKey();
			float val = entry.getValue();
			try {
				TopKMainFreqTableWriter.writeMainFreqPower(freq, val);
			} catch (IOException e) {
				Log.e(TAG,e.getMessage());
			}
		}
		TopKMainFreqTableWriter.closefile();
    }
    
    private void StartTestingAlgo(){
		/* 用來計算Window分數的模組 */
		SC = new ScoreComputing(sw);
		SC.StartComputingScore(fbm.getTopKMainFreqBandTable(), SoundWaveHandler.SAMPLE_RATE, FrequencyBandModel.FFT_LENGTH);
		SC.StartLogging();

		/* 用來偵測擊球的模組 */
		StrokeDetector SD = new StrokeDetector(MainActivity.this, SC);
		SD.StartStrokeDetector(bh);
    }

}


