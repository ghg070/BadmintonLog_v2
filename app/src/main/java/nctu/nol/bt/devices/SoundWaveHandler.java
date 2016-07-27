package nctu.nol.bt.devices;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import nctu.nol.badmintonlogprogram.MainActivity;
import nctu.nol.file.LogFileWriter;
import nctu.nol.file.SystemParameters;

public class SoundWaveHandler {

	private static final String TAG = "SoundWaveHandler";
    
    //Context
    private MainActivity mContext;

	//Service Relate
	private SoundWaveService mSoundWaveService =null;

    //Audio related
	public static final int SAMPLE_RATE = 11025;
	private AudioManager am;
    private AudioRecord record;
    
    //Buffer related
    private short[] mAudioBuffer;
    private int mBufferSize;
	private boolean mIsRecording = false;
	private LinkedBlockingQueue<AudioDataBuffer> AudioDataBuffer_for_file = null;
	private LinkedBlockingQueue<AudioData> AudioDataset_for_algo = null;
	
	//FileWrite for Logging
	//private LogFileWriter SoundDataWriter;
	private LogFileWriter SoundRawWriter;
	private int usedType;
	public AtomicBoolean isWrittingAudioDataLog = new AtomicBoolean(false);
	
	//Broadcast Related
	public final static String ACTION_SOUND_SERVICE_CONNECT_STATE = "SOUNDWAVEHANDLER.ACTION_SOUND_SERVICE_CONNECT_STATE";
	public final static String ACTION_SOUND_NOT_PREPARE_STATE = "SOUNDWAVEHANDLER.ACTION_SOUND_NOT_PREPARE_STATE";
	public final static String ACTION_SOUND_PREPARING_STATE = "SOUNDWAVEHANDLER.ACTION_ACTION_SOUND_PREPARING_STATE";
	public final static String ACTION_SOUND_PREPARED_STATE = "SOUNDWAVEHANDLER.ACTION_ACTION_SOUND_PREPARED_STATE";
	private IntentFilter SCOActionFilter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED); 
	private boolean SCOActionRegistedFlag = false;

		
	public SoundWaveHandler(MainActivity context) {
		this.mContext = context;

		//啟動BT service
		Intent SoundWaveServiceIntent = new Intent(mContext, SoundWaveService.class);
		mContext.bindService(SoundWaveServiceIntent, mServiceConnection, mContext.BIND_AUTO_CREATE);
    }


	/****************************/
	/**  SoundWave Service Related **/
	/***************************/
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName,
									   IBinder service) {
			mSoundWaveService = ((SoundWaveService.LocalBinder) service)
					.getService();
			Log.i(TAG, "Initializing Bluetooth.....");
			if (!mSoundWaveService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				mContext.finish();
			}
			Log.i(TAG, "Success!");

			Intent intent = new Intent(ACTION_SOUND_SERVICE_CONNECT_STATE);
			mContext.sendBroadcast(intent);

			mContext.registerReceiver(mHeadsetStateUpdateReceiver, makeBTHeadsetUpdateIntentFilter());
		}
		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mSoundWaveService = null;
		}
	};
	public final SoundWaveService getService(){
		return mSoundWaveService;
	}

	/**********************************/
	/**  SoundWaveHandler Initial Function **/
	/**********************************/
	public void initAudioManager() {	
	    am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE); 	
	    try{
	    	am.startBluetoothSco(); //if bt headset is not connected, it throws null exception
	    }catch(Exception e){
	    	Log.e(TAG,e.getMessage());
	    }
	}
	
	private void initLogFile(int uType){
		usedType = uType;
		//SoundDataWriter = new LogFileWriter("AudioDataBuffer.csv", LogFileWriter.SOUNDWAVE_DATA_TYPE, usedType);
		SoundRawWriter = new LogFileWriter("Sound.raw", LogFileWriter.SOUNDWAVE_RAW_TYPE, usedType);
	}

	
	private void initParameters(){
		// Compute the minimum required audio buffer size and allocate the buffer.
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mBufferSize = minBufferSize/2; 
        
        mAudioBuffer = new short[mBufferSize];
        Log.d(TAG, "Set mAudioBuffer Size = " + mBufferSize);
        
        record = new AudioRecord(AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        
		AudioDataBuffer_for_file = new LinkedBlockingQueue<AudioDataBuffer>();
		AudioDataset_for_algo =  new LinkedBlockingQueue<AudioData>();
	}

	/******************************************/
	/**  SoundWaveHandler  Data Recording Function **/
	/******************************************/
	public void startRecording(int uType){
		if(SystemParameters.IsBtHeadsetReady){
			
	        //Initial
	        initParameters();
	        initLogFile(uType);
	                
			mIsRecording = true;
			record.startRecording();
			StartWriteLogThread();
			startBufferedRead();
			

		}
    }
	
	public void stopRecording(){
		if(mIsRecording){
			mIsRecording = false;
			record.stop();
    	}
	}
	
	private void startBufferedRead() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (mIsRecording) {
					if(SystemParameters.isServiceRunning.get()){

						long curTime = System.currentTimeMillis();
						final int readSize = record.read(mAudioBuffer, 0, mBufferSize);
						long passTime = curTime-SystemParameters.StartTime;

						if(SystemParameters.SoundStartTime == 0)
							SystemParameters.SoundStartTime = curTime;
						long offset = SystemParameters.SoundStartTime-SystemParameters.StartTime;
						
						if(readSize > 0){
							AudioDataBuffer adb = new AudioDataBuffer(passTime, mAudioBuffer);
							AudioDataBuffer_for_file.add(adb);
							
							float deltaT = (1/(float)SAMPLE_RATE)*1000;
							for(int i = 0 ; i < mAudioBuffer.length; i++){
								AudioData ad = new AudioData( (long)(offset + deltaT*(SystemParameters.AudioCount+i)) , (float)mAudioBuffer[i]/32768 );
								AudioDataset_for_algo.add(ad);
							}
							
							SystemParameters.AudioCount += readSize;
							SystemParameters.SoundBufferCount++;
							SystemParameters.SoundEndTime = System.currentTimeMillis();
						}
					}
				}
			}
		}).start();
	}
	
	
	private void StartWriteLogThread(){	
		//Data Write
		new Thread(){
			@Override
			public void run() {
				isWrittingAudioDataLog.set(true);
				while( mIsRecording ||  AudioDataBuffer_for_file.size() > 0){
					if( AudioDataBuffer_for_file.size() > 0 ) {
						final AudioDataBuffer adb = AudioDataBuffer_for_file.poll();
						try {
							//SoundDataWriter.writeAudioDataBufferFile(adb.timestamp, adb.buffer);
							SoundRawWriter.writeSoundWaveRawFile(adb.buffer);
						} catch (IOException e) {
							Log.e(TAG, e.getMessage());
						}
					}
				}
				//if(SoundDataWriter != null)
					//SoundDataWriter.closefile();
				if(SoundRawWriter != null)
					SoundRawWriter.closefile();
				GenerateWavFile();

				isWrittingAudioDataLog.set(false);
			}
		}.start();
	}
	
	private void GenerateWavFile(){			
		try {
			SoundRawWriter.rawToWave(SAMPLE_RATE);
		} catch (final IOException e) {
			mContext.runOnUiThread(new Runnable() {     
				public void run(){    
					Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
				}
			});
		}
	}
	
	public final LinkedBlockingQueue<AudioData> getSampleData(){
		return AudioDataset_for_algo;
	}
	
	
	
	public void deleteObject(){
		if(record != null){
			record.release();
		}
		try{	
	    	am.stopBluetoothSco(); //if bt headset is not connected, it throws null exception
	    	try {
				this.finalize();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				Log.e(TAG,e.getMessage());
			}
	    }catch(Exception e){
	    	Log.e(TAG,e.getMessage());
	    }
		
		//Broadcast Receiver 
		if(SCOActionRegistedFlag){
			mContext.unregisterReceiver(mSCOStateUpdateReceiver);
			SCOActionRegistedFlag = false;
		}

		if (mServiceConnection != null) {
			mSoundWaveService.close();
			mContext.unbindService(mServiceConnection);
		}

		mContext.unregisterReceiver(mHeadsetStateUpdateReceiver);
	}

	public class AudioDataBuffer{
		public long timestamp;
		public short[] buffer;
		public int length;
		
		AudioDataBuffer(final long time, final short[] b ){
			this.timestamp = time;
			this.length = b.length;
			this.buffer = new short[this.length];
			for(int i = 0; i < this.length ; i++)
				this.buffer[i] = b[i];
		}
	}
	
	public class AudioData{
		public float data;
		public long time;
		AudioData(final long t, final float d){
			this.data = d;
			this.time = t;
		}
	}
	
	
	/**********************/
    /** Broadcast Event	 **/
	/**********************/
	private final BroadcastReceiver mHeadsetStateUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
	
			if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action) ||
					SoundWaveService.ACTION_DETECT_CONNECTION_STATE.equals(action)) {
				
				int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,0);
				
				if(state == BluetoothProfile.STATE_CONNECTED){
					Log.d(TAG,"Sound device preparing");
					
					final BluetoothDevice d =  intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);	

					Intent broadcast = new Intent(ACTION_SOUND_PREPARING_STATE);    
					broadcast.putExtra(BluetoothDevice.EXTRA_DEVICE, d); 
	                mContext.sendBroadcast(broadcast);
	                
					new Thread(){
						public void run(){
							try{	
								sleep(500);
								
								//Broadcast Receiver 
								if(!SCOActionRegistedFlag){
									mContext.registerReceiver(mSCOStateUpdateReceiver, SCOActionFilter);
									SCOActionRegistedFlag = true;
								}
								
								initAudioManager();
								
							}catch(InterruptedException e) {
								Log.e(TAG,e.getMessage());
							}
						}
					}.start();
					
				}else if(state == BluetoothProfile.STATE_DISCONNECTED){
					Log.d(TAG,"Sound device disconnected");
				}
			}
		}
    };
    private final BroadcastReceiver mSCOStateUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// Active when user call startBluetoothSco or stopBluetoothSco (SoundWaveHandler.java)
			if(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(action)){
            	int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                
                Log.d(TAG, "Audio SCO state: " + state);
                Log.d(TAG,intent.getAction());
                
                if ( AudioManager.SCO_AUDIO_STATE_CONNECTED == state ){
                	SystemParameters.IsBtHeadsetReady = true;
                	Intent broadcast = new Intent(ACTION_SOUND_PREPARED_STATE);    
	                mContext.sendBroadcast(broadcast);
                }else if( AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state ){
					if(SCOActionRegistedFlag && SystemParameters.IsBtHeadsetReady ){
						mContext.unregisterReceiver(mSCOStateUpdateReceiver);
						SCOActionRegistedFlag = false;
					}
                	SystemParameters.IsBtHeadsetReady = false;
                	Intent broadcast = new Intent(ACTION_SOUND_NOT_PREPARE_STATE);    
	                mContext.sendBroadcast(broadcast);
                }

            }
		}
    };
    
    private static IntentFilter makeBTHeadsetUpdateIntentFilter() {
	    final IntentFilter intentFilter = new IntentFilter();
	
	    intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
	    intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
	    intentFilter.addAction(SoundWaveService.ACTION_DETECT_CONNECTION_STATE);
	
	    return intentFilter;
	}
	
	
}