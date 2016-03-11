package nctu.nol.file;

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
	
	//for Log File
	public static String filePath = "";
	
	
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
		
		//for log file
		filePath = "";
	}

}