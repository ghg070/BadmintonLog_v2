package nctu.nol.badmintonlogprogram;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import nctu.nol.algo.FrequencyBandModel;
import nctu.nol.algo.PeakDetector;
import nctu.nol.bt.BTCommunicationService;
import nctu.nol.bt.devices.SoundWaveHandler;
import nctu.nol.bt.devices.SoundWaveHandler.AudioData;
import nctu.nol.file.SystemParameters;
import nctu.nol.file.LogFileWriter;
import nctu.nol.badmintonlogprogram.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

	private final static String TAG = "MainActivity";
	
	/* BT related */
	private BTCommunicationService mBluetoothService =null;
	private BluetoothAdapter mBluetoothAdapter = null;	
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_OPEN_BTSETTING = 2;
    
	/* Button State Related */
	private static final int BUTTON_ALLCLOSE = -1; 
	private static final int BUTTON_INITIALSTATE = 0; 
	private static final int BUTTON_SCANINGSTATE = 1; 
	private static final int BUTTON_READYSTATE = 2; 
	private static final int BUTTON_TRAININGSTATE = 3; 
	private static final int BUTTON_TESTINGSTATE = 4; 
	private static int curState;
	
    /* Log Related */ 
  	private static LogFileWriter ReadmeWriter;
  	private Button btScan;
  	private Button btTraining;
  	private Button btTesting;
    
	/* Sound Wave Related */
	private SoundWaveHandler sw;
	public TextView tv_HeadsetConnected;
     
    /* Bonded Device Related */
	private Spinner spBondedDeviceSpinner;
	private CustomArrayAdapter BondedDeviceNameList;
	private List<BluetoothDevice> BondedDevices = new ArrayList<BluetoothDevice>();
	private BluetoothDevice CurHeadsetDevice;
	
    /* Sampling Rate Control */
    /*private Spinner spSamplingRateSelection;
    private ArrayAdapter<Integer> SamplingRateList;  
    private Integer[] SamplingRate = { 100, 150, 200, 250 };*/
 
   
    /* Timer Related */
  	private Handler timerHandler = new Handler();
    private TextView tv_durationTime;
    
    /* Algorithm Related */
    private FrequencyBandModel fbm = new FrequencyBandModel();
    

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
    	updatedBondedDeviceSpinner();
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
		
		if (mServiceConnection != null){
			mBluetoothService.close();
            unbindService(mServiceConnection);
            unregisterReceiver(mSoundWaveHandlerStateUpdateReceiver);
		}
		
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
            }else if(resultCode == Activity.RESULT_OK){
            	//Get Bonded Devices
                updatedBondedDeviceSpinner();
            }
        }else if(requestCode == REQUEST_OPEN_BTSETTING){
        	//Get Bonded Devices
            updatedBondedDeviceSpinner();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
	private void initialViewandEvent(){
		//TextView
		tv_durationTime = (TextView) findViewById(R.id.tv_duration);	
		tv_HeadsetConnected = (TextView) findViewById(R.id.tv_headset);

		//Button
		btScan = (Button) findViewById(R.id.bt_scan);
		btTraining = (Button) findViewById(R.id.bt_trainingstart);
		btTesting = (Button) findViewById(R.id.bt_testingstart);
		btScan.setOnClickListener(scanListener);
		btTraining.setOnClickListener(TrainingStartClickListener);
		btTesting.setOnClickListener(TestingStartClickListener);
		
		
		//Spinner
		spBondedDeviceSpinner = (Spinner) findViewById(R.id.sp_bondeddevice);
		BondedDeviceNameList = new CustomArrayAdapter(MainActivity.this, android.R.layout.simple_spinner_item, BondedDevices);                                     
		spBondedDeviceSpinner.setAdapter(BondedDeviceNameList);
		spBondedDeviceSpinner.setOnItemSelectedListener(BondedDeviceSelectedListener);
		
		/*spSamplingRateSelection = (Spinner)findViewById(R.id.sp_samplingrate); 
		SamplingRateList = new ArrayAdapter<Integer>(MainActivity.this, android.R.layout.simple_spinner_item, SamplingRate);                                     
		spSamplingRateSelection.setAdapter(SamplingRateList);
		spSamplingRateSelection.setOnItemSelectedListener(SamplingRateSelectedListener);*/
		
		curState = BUTTON_INITIALSTATE;
		
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
		//啟動BT service
        Intent bluetoothSerialPortServiceIntent = new Intent(MainActivity.this, BTCommunicationService.class);
        bindService(bluetoothSerialPortServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

	}

	
	/***************************/
    /** BT Service Connection **/
	/***************************/
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
        	mBluetoothService = ((BTCommunicationService.LocalBinder) service)
                    .getService();
            Log.i(TAG, "Initializing Bluetooth.....");
            if (!mBluetoothService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            Log.i(TAG, "Success!");
            
            //Get Bonded Devices
            updatedBondedDeviceSpinner();
            
            //SoundWaveHandler Initial
            sw = new SoundWaveHandler(MainActivity.this);
            registerReceiver(mSoundWaveHandlerStateUpdateReceiver, makeSoundWaveHandlerStateUpdateIntentFilter());
            
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        	mBluetoothService = null;
        }
    };
    
    public void updatedBondedDeviceSpinner() {
    	if(mBluetoothService != null){
	    	BondedDevices = mBluetoothService.getBondedBTDevice(Device.AUDIO_VIDEO_WEARABLE_HEADSET); 
	    	BondedDeviceNameList.clear(); 
	        if (BondedDevices != null){
	        	//Sort Boned Device
	        	Collections.sort(BondedDevices, new Comparator<BluetoothDevice>() {
	                @Override
	                public int compare(BluetoothDevice lhs, BluetoothDevice rhs) {
	                    return lhs.getName().compareTo(rhs.getName());   
	                }
	            });
	        	
	            for (BluetoothDevice d : BondedDevices) {
	            	BondedDeviceNameList.insert(d, BondedDeviceNameList.getCount());
	            }
	           
	        }
	        BondedDeviceNameList.notifyDataSetChanged();
    	}

    }
    
    /**********************/
    /** Broadcast Event	 **/
	/**********************/
	private final BroadcastReceiver mSoundWaveHandlerStateUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if( SoundWaveHandler.ACTION_SOUND_NOT_PREPARE_STATE.equals(action) ){
            	tv_HeadsetConnected.setText("disconnected");               	
            	if(curState == BUTTON_TRAININGSTATE)//if Logging is running
            		btTraining.performClick();     
            	else
            		ControlButtons(BUTTON_INITIALSTATE);
			}else if( SoundWaveHandler.ACTION_SOUND_PREPARING_STATE.equals(action) ){			
				CurHeadsetDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				ControlButtons(BUTTON_ALLCLOSE);
			}else if( SoundWaveHandler.ACTION_SOUND_PREPARED_STATE.equals(action) ){
				//UI Control
            	tv_HeadsetConnected.setText(CurHeadsetDevice.getName()+"-"+CurHeadsetDevice.getAddress());
            	ControlButtons(BUTTON_READYSTATE);
			}
		}
	};
	private static IntentFilter makeSoundWaveHandlerStateUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		
		intentFilter.addAction(SoundWaveHandler.ACTION_SOUND_NOT_PREPARE_STATE);
		intentFilter.addAction(SoundWaveHandler.ACTION_SOUND_PREPARING_STATE);
		intentFilter.addAction(SoundWaveHandler.ACTION_SOUND_PREPARED_STATE);
		    
		return intentFilter;
	}
    
    
    /********************/
    /** Logging Event **/
	/********************/
    private Button.OnClickListener scanListener = new Button.OnClickListener() {
		public void onClick(View v) {
			
			if(curState == BUTTON_INITIALSTATE){
				ControlButtons(BUTTON_SCANINGSTATE);
				mBluetoothService.ConnectScoBTHeadset(CurHeadsetDevice);
			}
			else{
				mBluetoothService.DisconnectAllScoBTHeadset();
				ControlButtons(BUTTON_INITIALSTATE);
			}
		}
    };

    private Button.OnClickListener TrainingStartClickListener = new Button.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if(curState == BUTTON_READYSTATE){				
				ControlButtons(BUTTON_TRAININGSTATE);	
				
				//SystemParameters Initial
		        SystemParameters.initializeSystemParameters();				
				
				//Initial Log File
				ReadmeWriter = new LogFileWriter("Readme.txt", LogFileWriter.README_TYPE, LogFileWriter.TRAINING_TYPE);
				
				new Thread(){
		            public void run() {
		            	try {
		            		// AudioRecord Ready
		    				sw.startRecording(LogFileWriter.TRAINING_TYPE);	
		    				
		    				//等待1sec後開始
							sleep(1000);
							
							//設定開始時間
					        SetMeasureStartTime();
					      
					        //設定定時要執行的方法
							timerHandler.removeCallbacks(updateTimer);
							timerHandler.postDelayed(updateTimer, 1000);//設定Delay的時間
							
							//Service Start
							SystemParameters.isServiceRunning.set(true);
							
							runOnUiThread(new Runnable() {     
								public void run(){     
									Toast.makeText(getBaseContext(), "Log Service is Start", Toast.LENGTH_SHORT).show();
						        }     
						    }); 
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							Log.e(TAG,e.getMessage());
						}
		            	
		            }
				}.start();
			}
			else{
				Toast.makeText(getBaseContext(), "Log Service is Stop", Toast.LENGTH_SHORT).show();
				SystemParameters.isServiceRunning.set(false);
				SystemParameters.Duration = (System.currentTimeMillis() - SystemParameters.StartTime)/1000.0;
				sw.stopRecording();
				timerHandler.removeCallbacks(updateTimer);
				
				final ProgressDialog dialog = ProgressDialog.show(MainActivity.this,
	                    "寫檔中", "處理檔案中，請稍後",true);
				
				new Thread(){
					public void run(){
						//Wait log file write done
						while(sw.isWrittingAudioDataLog.get());
			
						//Training
						StartTrainingAlgo(sw);
						
						//Show UI
						runOnUiThread(new Runnable() {     
							public void run(){
								showLogInformationDialog();
								
								//Control Button
								if(SystemParameters.IsBtHeadsetReady)
									ControlButtons(BUTTON_READYSTATE);
								else
									ControlButtons(BUTTON_INITIALSTATE);
						    
								dialog.dismiss();
							}
						});
					}
				}.start();
			}
		}
	};
	
	private Button.OnClickListener TestingStartClickListener = new Button.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if(fbm.CheckModelHasTrained()){
				if(curState == BUTTON_READYSTATE){				
					ControlButtons(BUTTON_TESTINGSTATE);	
					
					//SystemParameters Initial
			        SystemParameters.initializeSystemParameters();				
					
					//Initial Log File
					ReadmeWriter = new LogFileWriter("Readme.txt", LogFileWriter.README_TYPE, LogFileWriter.TESTING_TYPE);

					new Thread(){
			            public void run() {
			            	try {
			            		// AudioRecord Ready
			    				sw.startRecording(LogFileWriter.TESTING_TYPE);	
			    				
			    				//等待1sec後開始
								sleep(1000);
								
								//設定開始時間
						        SetMeasureStartTime();
						      
						        //設定定時要執行的方法
								timerHandler.removeCallbacks(updateTimer);
								timerHandler.postDelayed(updateTimer, 1000);//設定Delay的時間
								
								//Service Start
								SystemParameters.isServiceRunning.set(true);
								
								runOnUiThread(new Runnable() {     
									public void run(){     
										Toast.makeText(getBaseContext(), "Log Service is Start", Toast.LENGTH_SHORT).show();
							        }     
							    }); 
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								Log.e(TAG,e.getMessage());
							}
			            	
			            }
					}.start();
				}
				else{
					Toast.makeText(getBaseContext(), "Log Service is Stop", Toast.LENGTH_SHORT).show();
					SystemParameters.isServiceRunning.set(false);
					SystemParameters.Duration = (System.currentTimeMillis() - SystemParameters.StartTime)/1000.0;
					sw.stopRecording();
					timerHandler.removeCallbacks(updateTimer);
					
					final ProgressDialog dialog = ProgressDialog.show(MainActivity.this,
		                    "寫檔中", "處理檔案中，請稍後",true);
					
					new Thread(){
						public void run(){
							//Wait log file write done
							while(sw.isWrittingAudioDataLog.get());
				
							//Show UI
							runOnUiThread(new Runnable() {     
								public void run(){
									showLogInformationDialog();
									
									//Control Button
									if(SystemParameters.IsBtHeadsetReady)
										ControlButtons(BUTTON_READYSTATE);
									else
										ControlButtons(BUTTON_INITIALSTATE);
							    
									dialog.dismiss();
								}
							});
						}
					}.start();
				}
			}else{
				Toast.makeText(getBaseContext(), "You must train your racket first.", Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	
	private void SetMeasureStartTime(){
		//Set time
	    Time t=new Time();
	    t.setToNow();
	    String year = String.valueOf(t.year);
	    String month = String.valueOf(t.month+1);
	    String day = String.valueOf(t.monthDay);
	    int hour =t.hour;
	    int minute = t.minute;
	    int second = t.second;
	    int millisecond = (int)(System.currentTimeMillis()%1000);
	    
	    //YYYYMMDD-HHMMSS
	    String date = year+"-"+month+"-"+day+" "+String.format("%02d:%02d:%02d.%03d",hour, minute, second, millisecond);
		SystemParameters.StartDate = date;
		SystemParameters.StartTime = System.currentTimeMillis();
	}
    private Runnable updateTimer = new Runnable() {
		@Override
		public void run() {
			//compute the passed time in millisecond
			Long spentTime = System.currentTimeMillis() - SystemParameters.StartTime;
			//compute the passed minutes
			Long minutes = (spentTime/1000)/60;
			//compute the passed seconds
			Long seconds = (spentTime/1000) % 60;
			//compute the passed hours
			Long hour = minutes / 60 ;
						
			tv_durationTime.setText(String.format("%02d:%02d",minutes, seconds));
			
			timerHandler.postDelayed(this, 1000);

		}
    };
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
						).setPositiveButton("OK",new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,int id) {
								// if this button is clicked, close							
								//MainActivity.this.finish();
							}
						}).show();
	}
    
    /***********************/
    /** Algorithm Related **/
    /***********************/
    private void StartTrainingAlgo(final SoundWaveHandler sw){
    	//split time array and data array
    	Queue<AudioData> ads = new LinkedBlockingQueue<AudioData>(sw.getSampleData());
		float times[] = new float[ads.size()],
			  vals[] = new float[ads.size()];
		
		int count = 0;
		while(ads.size() != 0){
			AudioData ad = ads.poll();
			times[count] = (float)ad.time;
			vals[count] = ad.data;
			count++;
		}
		
		//Find all peak
		PeakDetector pd = new PeakDetector(700, 350);
		List<Integer> peaks = pd.findPeakIndex(times, vals, (float)0.5);
		// Test File for Peak Detection
		/*
			LogFileWriter PeakTestWriter = new LogFileWriter("PeakIndex.txt", LogFileWriter.OTHER_TYPE, LogFileWriter.TRAINING_TYPE);
			for(int i = 0 ; i < peaks.size(); i++){
				int index = peaks.get(i);
				Log.d(TAG,"peak index = "+index+", time = "+times[index]+", val = "+vals[index]);

				try {
					PeakTestWriter.writePeakIndexFile(index);
				} catch (IOException e) {
					Log.e(TAG,e.getMessage());
				}
			}
			PeakTestWriter.closefile();
		*/

		// Find top K freq band
		Vector<FrequencyBandModel.MainFreqInOneWindow> AllMainFreqBands = fbm.FindSpectrumMainFreqs(peaks, vals, 512, SoundWaveHandler.SAMPLE_RATE);
		fbm.setTopKFreqBandTable(AllMainFreqBands, peaks.size());
		List<HashMap.Entry<Float, Float>> TopKMainFreqs = fbm.getTopKMainFreqBandTable();


		// Test File for All Spectrum Main Freq Bands
		LogFileWriter AllSpectrumMainFreqsTestWriter = new LogFileWriter("AllSpectrumMainFreqs.txt", LogFileWriter.OTHER_TYPE, LogFileWriter.TRAINING_TYPE);
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
		LogFileWriter TopKMainFreqTableWriter = new LogFileWriter("TopKMainFreqTable.txt", LogFileWriter.OTHER_TYPE, LogFileWriter.TRAINING_TYPE);
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
    	
    }
    
    
    /**********************/
    /** Control Elements **/
    /**********************/
    private void ControlButtons(int state){
		curState = state;
		//Log.d(TAG,"State: "+curState);
    	switch(state){
    		case BUTTON_INITIALSTATE:
    			btScan.setText("Scan");
    			btTraining.setText("Start Training");
    			btTesting.setText("Start Testing");
    			btScan.setEnabled(true);
    			btTraining.setEnabled(false);
    			btTesting.setEnabled(false);
    			spBondedDeviceSpinner.setEnabled(true);
    			break;
    		case BUTTON_SCANINGSTATE:
                btScan.setText("Reset");
                btTraining.setText("Start Training");
                btTesting.setText("Start Testing");
    			btScan.setEnabled(true);
    			btTraining.setEnabled(false);
    			btTesting.setEnabled(false);
    			spBondedDeviceSpinner.setEnabled(false);
    			break;
    		case BUTTON_READYSTATE:
    			btScan.setText("Reset");
    			btTraining.setText("Start Training");
    			btTesting.setText("Start Testing");
    			btScan.setEnabled(true);
    			btTraining.setEnabled(true);
    			btTesting.setEnabled(true);
    			spBondedDeviceSpinner.setEnabled(false);
    			break;
    		case BUTTON_TRAININGSTATE:
    			btScan.setText("Reset");
    			btTraining.setText("Finish");
    			btTesting.setText("Start Testing");
    			btScan.setEnabled(false);
    			btTraining.setEnabled(true);
    			btTesting.setEnabled(false);
    			spBondedDeviceSpinner.setEnabled(false);
    			break;
    		case BUTTON_TESTINGSTATE:
    			btScan.setText("Reset");
    			btTraining.setText("Start Training");
    			btTesting.setText("Finish");
    			btScan.setEnabled(false);
    			btTraining.setEnabled(false);
    			btTesting.setEnabled(true);
    			spBondedDeviceSpinner.setEnabled(false);
    			break;
    		case BUTTON_ALLCLOSE:
    			btScan.setEnabled(false);
    			btTraining.setEnabled(false);
    			btTesting.setEnabled(false);
    			spBondedDeviceSpinner.setEnabled(false);
    			break;
    		default:
    			break;
    	}
    }
    
    
    /***********************************************/
    /** Spinner Function for Select Bonded Device **/
    /***********************************************/
    public class CustomArrayAdapter extends ArrayAdapter<BluetoothDevice> {
    	final List<BluetoothDevice> devices;
    	final Context context;
    	
    	public CustomArrayAdapter(final Context context, final int resource, final List<BluetoothDevice> objects) {
			super(context, resource, objects);
			this.devices = objects;
			this.context = context;
		}

    	@Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            }
            ((TextView) convertView).setText(devices.get(position).getName()+"-"+devices.get(position).getAddress());
            return convertView;

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(android.R.layout.simple_spinner_item, parent, false);
            
            ((TextView) convertView).setText(devices.get(position).getName()+"-"+devices.get(position).getAddress());
            return convertView;
        }
    }
    
    AdapterView.OnItemSelectedListener BondedDeviceSelectedListener = new AdapterView.OnItemSelectedListener(){
        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1,int position, long arg3) {
        	
        	if(!BondedDevices.isEmpty()){
        		CurHeadsetDevice = BondedDevices.get(position);
        	}
        }

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO Auto-generated method stub
			
		}
        
    };
    
    
    /***************************************************/
    /** Main Item For Bluetooth Bonded Device Setting **/
    /***************************************************/
    private static final int group_id = 0;
    private static final int item_id_btsetting = 0;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(group_id, item_id_btsetting, 0, "BT Setting");
        return super.onCreateOptionsMenu(menu); 
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case item_id_btsetting:
        		Intent BTSettingIntent = new Intent(Intent.ACTION_MAIN, null);
        		BTSettingIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                ComponentName cn = new ComponentName("com.android.settings", 
                        "com.android.settings.bluetooth.BluetoothSettings");
                BTSettingIntent.setComponent(cn);
                startActivityForResult(BTSettingIntent, REQUEST_OPEN_BTSETTING);
        		return true;
        	default:
        		return super.onOptionsItemSelected(item);
        }
    }
    
    

    
};


