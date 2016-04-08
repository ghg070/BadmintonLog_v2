package nctu.nol.algo;


import android.app.Activity;
import android.content.Intent;
import android.text.format.Time;
import android.util.Log;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import nctu.nol.file.LogFileWriter;
import nctu.nol.file.SystemParameters;

public class StrokeDetector {
    private final static String TAG = StrokeDetector.class.getSimpleName();
    private Activity mContext;

    /* Window Related */
    private ScoreComputing curSC;

    /* Rule Related */
    private static final double SCORETHRESHOLD = 0.55;
    private static final int WINDOWTHRESHOLD = 2;
    private static final int RESERVEDWINDOWNUM = 10; //Window數超過該變數後, 才開始進行判斷

    /* Thread Related */
    private Thread detector_t;

    /* Logging Related */
    private LogFileWriter StrokeWriter;

    /* Stroke Time Related */
    private Vector<Long> StrokeTimes = new Vector<Long>();

    /* Broadcast Related */
    public final static String ACTION_STROKE_DETECTED_STATE = "STROKEDETECTOR.ACTION_STROKE_DETECTED_STATE";

    /* Constructor, Class內的初始化(參數之類的), 在new這個Class的時候會自動觸發 */
    public StrokeDetector(Activity act, ScoreComputing sc){
        this.curSC = sc;
        this.mContext = act;
    }

    private void initParameter(){
        StrokeTimes.clear();
        StrokeWriter = new LogFileWriter("StrokeTime.csv", LogFileWriter.STROKE_TIME_TYPE, LogFileWriter.TESTING_TYPE);
    }

    /* 啟動Thread持續偵測Window分數是否連續達標 */
    public void StartStrokeDetector(){
        /*
        *   用Thread持續檢查Window Score的變化情形
        *   連續N個Window Score大於Threshold即稱為有擊球行為
        *   若偵測到擊球必須發送Broadcast出去(在MainActivity.java內另外實作接收端)
        * */
        initParameter();
        detector_t = new Thread() {
            public void run() {

                while(SystemParameters.isServiceRunning.get()){
                    final LinkedBlockingQueue<ScoreComputing.WindowScore> w_scores = curSC.getAllWindowScores();
                    try{
                        // 偵測是否符合規則, 若符合，改變curIdx並發出broadcast
                        long result = CheckStroke(w_scores);

                        Log.e(TAG, "Get Stroke!!!!");
                        StrokeTimes.add(result);
                        try {
                            StrokeWriter.writeStrokeTime( MillisecToString(result) );
                        } catch (IOException e) {
                            Log.e(TAG,e.getMessage());
                        }

                        // if no exception occur, jump curIdx to the window position where the score is lower than threshold
                        JumpWindow(w_scores);

                        Intent broadcast = new Intent(ACTION_STROKE_DETECTED_STATE);
                        mContext.sendBroadcast(broadcast);

                    } catch (NotMatchRuleException e) {}
                    catch (ReservedException e) {}
                }

                if( StrokeWriter != null)
                    StrokeWriter.closefile();
            }
        };
        detector_t.start();
    }

    // 檢查是否符合規則, 若不符或是access到保留區皆會丟出Exception
    public long CheckStroke(final LinkedBlockingQueue<ScoreComputing.WindowScore> AllWindows) throws NotMatchRuleException, ReservedException{
        if(AllWindows.size() < RESERVEDWINDOWNUM) // it will access reserved space
            throw new ReservedException();

        long stroke_t = 0;
        for (int i = 0; i < WINDOWTHRESHOLD; i++) {
            ScoreComputing.WindowScore ws = AllWindows.poll();
            if (ws.score < SCORETHRESHOLD)
                throw new NotMatchRuleException();

            if(i == 0)
                stroke_t = ws.w_time;
        }
        return stroke_t;
    }

    // 符合規則後, 繼續pop到score低於Threshold為止
    public void JumpWindow( final LinkedBlockingQueue<ScoreComputing.WindowScore> AllWindows ){
        while(AllWindows.size() > 0){
            ScoreComputing.WindowScore ws = AllWindows.poll();
            if (ws.score < SCORETHRESHOLD)
                break;
        }
    }

    private String MillisecToString(long timestamp){
        Time t=new Time();
        t.set(timestamp);
        int minute = t.minute;
        int second = t.second;
        int millisecond = (int)(timestamp%1000);

        // MM:SS.mmm
        return String.format("%02d:%02d.%03d", minute, second, millisecond);
    }

    /**********************/
    /**   Custom Exception    **/
    /**********************/
    public class ReservedException extends Exception {public ReservedException() { super("The index is in reserved position");} }
    public class NotMatchRuleException extends Exception {public NotMatchRuleException() { super("The rule is not matched");} }



}
