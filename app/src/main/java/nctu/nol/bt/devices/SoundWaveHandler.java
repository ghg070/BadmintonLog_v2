package nctu.nol.bt.devices;

import java.io.IOException;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;
import android.widget.Toast;
import nctu.nol.badmintonlogprogram.MainActivity;
import nctu.nol.bt.BTCommunicationService;
import nctu.nol.file.LogFileWriter;
import nctu.nol.file.SystemParameters;

public class SoundWaveHandler {

	private static final String TAG = "SoundWaveHandler";
    
    //Context
    private MainActivity mContext;
    
    //Audio related
	public static final int SAMPLE_RATE = 11025;
	private AudioManager am;
    private AudioRecord record;
    
    //Buffer related
    private short[] mAudioBuffer;
    private int mBufferSize;
	private boolean mIsRecording = false;
	private Vector<AudioDataBuffer> AudioDataBuffer = new Vector<AudioDataBuffer>();
	private Vector<AudioData> AudioDataset = new Vector<AudioData>();
	
	//RealTime Get Data
	private int CurPointer = 0;
	public final static int Active_BufferNumThreshold = 50;
	public final static int Remind_BufferNumThreshold = 20;
	
	
	//FileWrite for Logging
	private LogFileWriter SoundDataWriter;
	private LogFileWriter SoundRawWriter;
	private int usedType;
	public AtomicBoolean isWrittingAudioDataLog = new AtomicBoolean(false);
	
	//Broadcast Related
	public final static String ACTION_SOUND_NOT_PREPARE_STATE = "SOUNDWAVEHANDLER.ACTION_SOUND_NOT_PREPARE_STATE";
	public final static String ACTION_SOUND_PREPARING_STATE = "SOUNDWAVEHANDLER.ACTION_ACTION_SOUND_PREPARING_STATE";
	public final static String ACTION_SOUND_PREPARED_STATE = "SOUNDWAVEHANDLER.ACTION_ACTION_SOUND_PREPARED_STATE";
	private IntentFilter SCOActionFilter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED); 
	private boolean SCOActionRegistedFlag = false;

		
	public SoundWaveHandler(MainActivity context) {
		this.mContext = context; 
		mContext.registerReceiver(mHeadsetStateUpdateReceiver, makeBTHeadsetUpdateIntentFilter());
    }

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
		SoundDataWriter = new LogFileWriter("AudioDataBuffer.csv", LogFileWriter.SOUNDWAVE_DATA_TYPE, usedType);
		SoundRawWriter = new LogFileWriter("Sound.raw", LogFileWriter.SOUNDWAVE_RAW_TYPE, usedType);
	}

	
	private void initParameters(){
		// Compute the minimum required audio buffer size and allocate the buffer.
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mBufferSize = minBufferSize/2; 
        
        mAudioBuffer = new short[mBufferSize];
        Log.d(TAG,"Set mAudioBuffer Size = " + mBufferSize);
        
        record = new AudioRecord(AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        
		AudioDataBuffer.clear();
		AudioDataset.clear();
	}
	
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
					long curTime = System.currentTimeMillis();
					
					final int readSize = record.read(mAudioBuffer, 0, mBufferSize);
									
					if(SystemParameters.isServiceRunning.get()){
						//This buffer receive before Service Start, Throw it.
						if(curTime < SystemParameters.StartTime) continue;
						
						long passTime = curTime-SystemParameters.StartTime;
						if(SystemParameters.SoundStartTime == 0)
							SystemParameters.SoundStartTime = curTime; 
						
						
						if(readSize > 0){
							AudioDataBuffer adb = new AudioDataBuffer(passTime, mAudioBuffer);
							AudioDataBuffer.add(adb);
							
							float deltaT = 1/(float)SAMPLE_RATE;
							for(int i = 0 ; i < mAudioBuffer.length; i++){
								AudioData ad = new AudioData( passTime+(long)deltaT*i , (float)mAudioBuffer[i]/32768 );
								AudioDataset.add(ad);
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
				int CurrentWriteBufferIndex = 0;
				isWrittingAudioDataLog.set(true);
				while( mIsRecording || CurrentWriteBufferIndex < AudioDataBuffer.size()){
					int curSize = AudioDataBuffer.size();
					for(; CurrentWriteBufferIndex < curSize ;CurrentWriteBufferIndex++){
						final AudioDataBuffer adb = AudioDataBuffer.get(CurrentWriteBufferIndex);
						try {
							SoundDataWriter.writeAudioDataBufferFile(adb.timestamp, adb.buffer);
							SoundRawWriter.writeSoundWaveRawFile(adb.buffer);
						} catch (IOException e) {
							Log.e(TAG,e.getMessage());
						}
					}
				
				}
				if(SoundDataWriter != null)
					SoundDataWriter.closefile();
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
	
	public final Vector<AudioData> getSampleData(){
		return AudioDataset;
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
				BTCommunicationService.ACTION_DETECT_CONNECTION_STATE.equals(action)) {
				
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
					if(SCOActionRegistedFlag){
						mContext.unregisterReceiver(mSCOStateUpdateReceiver);
						SCOActionRegistedFlag = false;
					}
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
	    intentFilter.addAction(BTCommunicationService.ACTION_DETECT_CONNECTION_STATE);
	
	    return intentFilter;
	}
	
	
}