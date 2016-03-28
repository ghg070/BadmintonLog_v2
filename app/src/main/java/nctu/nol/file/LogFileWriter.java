package nctu.nol.file;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

/**************  
 * All the file writer, include:
 * 1. Gps Log File Writer
 * 2. Accelerometer Log File Writer
 * 3. Magnetic Log File Writer
 * 4. Extraction Algorithm Log File Writer
 * 5. Anomaly Log File Writer
 * 6. Readme Writer
 * *****************/

public class LogFileWriter {
	private final static String TAG = LogFileWriter.class.getName();
	
	private String fileName;
	private int fileType;
	private DataOutputStream outputStream;
	public File curFile;
	
	//Attribute
	//private static final String attributeAcc  = "Timestamp,Gx,Gy,Gz,Gv\n";
	private static final String attributeAudioDataBuffer  = "Timestamp,BufferSize,VoiceEnergy";
	
	//Type Number
	public static final int SOUNDWAVE_RAW_TYPE = 0;
	public static final int SOUNDWAVE_TIMESTAMP_TYPE = 1;
	public static final int SOUNDWAVE_DATA_TYPE = 2;
	public static final int README_TYPE = 10;
	public static final int OTHER_TYPE = 11;
	
	//Used Type Number
	public static final int TRAINING_TYPE = 0;
	public static final int TESTING_TYPE = 1;
	
	
	public LogFileWriter(String filename,int type, int utype){
		this.fileName=filename;
		this.fileType=type;
		initialize(type,utype);
		
	}
	
	public void writeSoundWaveRawFile(final short[] dataset) throws IOException{
		for (int i = 0; i < dataset.length; i++){
			outputStream.writeShort(dataset[i]);
		}		
	}
	
	
	public void writeAudioDataBufferFile(final long timestamp, final short[] dataset) throws IOException{
		StringBuilder outputString =  new StringBuilder(timestamp+","+dataset.length);
		for(int i = 0 ; i < dataset.length; i++){
			double data = dataset[i]/32768.0;
			outputString.append(String.format(",%.6f",data));
		}
		outputString.append("\n");
		outputStream.write(outputString.toString().getBytes());
	}
	
	//TEST FUNCTION
	public void writePeakIndexFile(final int index) throws IOException{
		StringBuilder outputString = new StringBuilder( String.format("%d", index) );
		outputString.append("\n");
		outputStream.write(outputString.toString().getBytes());
	}
	public void writeFreqPeakIndexFile(final int peak_num, final int window_num, final float[] sortedFreq, final float[] sortedPower) throws IOException{
		StringBuilder outputString = new StringBuilder( String.format("%d,%d",peak_num,window_num) );
		for(int i = 0 ; i < sortedFreq.length; i++){
			double f = sortedFreq[i];
			outputString.append(String.format(",%.3f",f));
		}
		for(int i = 0 ; i < sortedPower.length; i++){
			double p = sortedPower[i];
			outputString.append(String.format(",%.3f",p));
		}
		outputString.append("\n");
		outputStream.write(outputString.toString().getBytes());
	}
	public void writeMainFreqPower(final float f, final float p) throws IOException{
		StringBuilder outputString = new StringBuilder( String.format("%.3f,%.3f", f,p) );
		outputString.append("\n");
		outputStream.write(outputString.toString().getBytes());
	}
	
	
	public void writeReadMeFile() throws IOException{
		Log.d(TAG, "SystemParameters.totalSecond:"+SystemParameters.Duration);
		if(SystemParameters.Duration > 0){
	
		
			long AudioRate = (long)( SystemParameters.AudioCount/((double)(SystemParameters.SoundEndTime-SystemParameters.SoundStartTime)/1000.0) );
			
			String outputString = 
				"File Format Version: 1.0\r\n"
				+"File List:\r\n"
				+"\tReadMe.txt This file\r\n"
				+"\tSound.wav Audio Wav file\r\n"
				+"\tAudioDataBuffer.csv Audio Buffer file ("+SystemParameters.AudioCount+" records @ "+AudioRate+"Hz)\r\n"
				+"\tAudioDataBuffer.csv Record Format\r\n"
				+"\t\t"+ attributeAudioDataBuffer + "\r\n"
				+"Start Logging Date/Time: "+SystemParameters.StartDate	+"\r\n"	
				+"Duration: "+SystemParameters.Duration	+" sec\r\n"
				+"////////////// Audio Buffer Information //////////////"+ "\r\n"
				+"First Buffer Start Timestamp: "+SystemParameters.SoundStartTime + "\r\n"
				+"Final Buffer End Timestamp: "+SystemParameters.SoundEndTime + "\r\n";
				
		
			outputStream.write(outputString.getBytes());
		
		}
	}

	public void closefile(){
		try {
			outputStream.flush();
			outputStream.close();
			
			} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void initialize(int type, int utype) {
		Log.d(TAG, "initialize begin");
		
		String dirPath = CheckDirectoryPath(utype);
		
		if(isExternalStorageWritable()){
			//統一folder的名字
			curFile = new File(dirPath, fileName);
			try {
				outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(curFile)));
				//initializeAttribute(type);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
			Log.d("LogFileWriter", "No Free Space to Create File");
	}
	
	/*private void initializeAttribute(int type) throws IOException{
		if(type == ACC_TYPE)
			outputStream.write(attributeAcc.getBytes());
		
	}*/
	
	private String CheckDirectoryPath(int utype){
		if(isExternalStorageWritable() && SystemParameters.filePath.equals("")){
			String path = Environment.getExternalStorageDirectory().getPath();
			Time t=new Time();
			t.setToNow();
			String year = String.valueOf(t.year);
			String month = String.valueOf(t.month+1);
			String day = String.valueOf(t.monthDay);
			String hour = String.valueOf(t.hour);
			String minute = String.valueOf(t.minute);
			String second = String.valueOf(t.second);
			
			month = "00".substring(0, 2 - month.length()) + month;
			day = "00".substring(0, 2 - day.length()) + day;
			hour = "00".substring(0, 2 - hour.length()) + hour;
			minute = "00".substring(0, 2 - minute.length()) + minute;
			second = "00".substring(0, 2 - second.length()) + second;
				  		    
			//YYYYMMDD-HHMMSS
			String date = year+month+day+"-"+hour+minute+second;
			File dir;
			
			switch(utype){
				case TRAINING_TYPE:
					SystemParameters.filePath = path+"/NOL/"+date+" - Training/";
					break;
				case TESTING_TYPE:
					SystemParameters.filePath = path+"/NOL/"+date+" - Testing/";
				break;	
				default:
					break;
			}
			
			dir = new File(SystemParameters.filePath);
			if (!dir.exists()){
				Log.d(TAG, SystemParameters.filePath);
				dir.mkdirs();
			}
			
	
			return SystemParameters.filePath;
		}
		return SystemParameters.filePath;
	}
	
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
	
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}
	
	
	
	/** Transform raw file to wav file **/
	public void rawToWave(final int SAMPLERATE) throws IOException {
		if(this.fileType == SOUNDWAVE_RAW_TYPE){
			byte[] rawData = new byte[(int) curFile.length()];
			DataInputStream input = null;
			try {
				input = new DataInputStream(new FileInputStream(curFile));
				input.read(rawData);
			} finally {
				if (input != null) {
					input.close();
				}
			}
	
			String filePath = curFile.getAbsolutePath();
			File waveFile = new File(filePath.substring(0, filePath.length()-4)+".wav");
			
			DataOutputStream output = null;
			try {
				output = new DataOutputStream(new FileOutputStream(waveFile));
				// WAVE header
				// see http://www.topherlee.com/software/pcm-tut-wavformat.html
				writeString(output, "RIFF"); // chunk id
				writeInt(output, 36 + rawData.length); // chunk size
				writeString(output, "WAVE"); // format
				writeString(output, "fmt "); // subchunk 1 id
				writeInt(output, 16); // subchunk 1 size
				writeShort(output, (short) 1); // audio format (1 = PCM)
				writeShort(output, (short) 1); // number of channels
				writeInt(output, SAMPLERATE); // sample rate
				writeInt(output, SAMPLERATE * 2); // byte rate
				writeShort(output, (short) 2); // block align
				writeShort(output, (short) 16); // bits per sample
				writeString(output, "data"); // subchunk 2 id
				writeInt(output, rawData.length); // subchunk 2 size
				// Audio data (conversion big endian -> little endian)
				short[] shorts = new short[rawData.length / 2];
				ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
				ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
				for (short s : shorts) {
					bytes.putShort(s);
				}
				output.write(bytes.array());
			} finally {
				if (output != null) {
					output.close();
				}
			}
			curFile.delete();	
		}else{
			Log.e(TAG,"This file cannot transfer to wav file.");
		}
	}
	private void writeInt(final DataOutputStream output, final int value) throws IOException {
		output.write(value >> 0);
		output.write(value >> 8);
		output.write(value >> 16);
		output.write(value >> 24);
	}
	private void writeShort(final DataOutputStream output, final short value) throws IOException {
		output.write(value >> 0);
		output.write(value >> 8);
	}
	private void writeString(final DataOutputStream output, final String value) throws IOException {
		for (int i = 0; i < value.length(); i++) {
			output.write(value.charAt(i));
		}
	}
	
}
