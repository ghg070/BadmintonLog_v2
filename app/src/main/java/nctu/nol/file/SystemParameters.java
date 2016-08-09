package nctu.nol.file;

import android.text.format.Time;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;


/**************  
 * Store all GLobal Variable
 ******************/

public class SystemParameters {
	
	public static String StartDate =" ";							//start measuring date
	public static long StartTime = 0;								//start measuring time
	public static double Duration = 0;								//measuring duration
	public static AtomicBoolean isServiceRunning = new AtomicBoolean(false);
	
	//for audio
	public static long AudioCount  = 0;								//compute the amount of Audio data
	public static boolean IsBtHeadsetReady = false;
	public static long SoundStartTime = 0;
	public static long SoundEndTime = 0;
	public static int SoundBufferCount = 0;

	//for sensor
	public static boolean IsKoalaReady = false;
	public static int SensorCount = 0;
	public static int SensorCount_ContainLoss = 0;
	public static long SensorEndTime = 0;
	
	//for Log File
	public static String filePath = "";

	//for Stroke
	public static int StrokeCount = 0;
	public static Vector<Long> StrokeTimes = new Vector<Long>();
	public static String ModelName = "";

	//for Local DB
	public static long TrainingId = -1;
	public static long TestingId = -1;
	
	 /** Initial Function **/
	public static void initializeSystemParameters(){
		
		StartDate =" ";
		StartTime = 0;
		Duration = 0;
		isServiceRunning.set(false);
		
		//for audio
		AudioCount = 0;
		SoundStartTime = 0;
		SoundEndTime = 0;
		SoundBufferCount = 0;

		//for sensor
		SensorCount = 0;
		SensorCount_ContainLoss = 0;
		SensorEndTime = 0;
		
		//for log file
		filePath = "";

		//for Stroke
		StrokeCount = 0;
		StrokeTimes.clear();
	}

	public static void SetMeasureStartTime(){
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
		StartDate = date;
		StartTime = currentTime;
	}

	public static String MillisecToString(long timestamp){

		//compute the passed minutes
		Long minutes = (timestamp/1000)/60;
		//compute the passed seconds
		Long seconds = (timestamp/1000) % 60;
		//compute the passed hours
		Long millisecond = timestamp%1000 ;


		// MM:SS.mmm
		return String.format("%02d:%02d.%03d", minutes, seconds, millisecond);
	}

}