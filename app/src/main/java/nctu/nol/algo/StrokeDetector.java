package nctu.nol.algo;


import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import java.util.Vector;

import nctu.nol.file.SystemParameters;

public class StrokeDetector {
    private final static String TAG = StrokeDetector.class.getSimpleName();
    private Activity mContext;

    /* Window Related */
    ScoreComputing curSC;

    /* Rule Related */
    private static final double SCORETHRESHOLD = 0.55;
    private static final int WINDOWTHRESHOLD = 2;
    private static final int RESERVEDWINDOWNUM = 10; //Window數超過該變數後, 才開始進行判斷

    /* Thread Related */
    private Thread detector_t;

    /* Broadcast Related */
    public final static String ACTION_STROKE_DETECTED_STATE = "STROKEDETECTOR.ACTION_STROKE_DETECTED_STATE";

    /* Constructor, Class內的初始化(參數之類的), 在new這個Class的時候會自動觸發 */
    public StrokeDetector(Activity act, ScoreComputing sc){
        this.curSC = sc;
        this.mContext = act;
    }

    /* 啟動Thread持續偵測Window分數是否連續達標 */
    public void StartStrokeDetector(){
        /*
        *   用Thread持續檢查Window Score的變化情形
        *   連續N個Window Score大於Threshold即稱為有擊球行為
        *   若偵測到擊球必須發送Broadcast出去(在MainActivity.java內另外實作接收端)
        *   ps. 必須要有變數(curIdx)紀錄下一次迴圈從第幾個Window開始檢查
        * */
        detector_t = new Thread() {
            public void run() {
                int curIdx = 0;
                while(SystemParameters.isServiceRunning.get()){
                    final Vector<ScoreComputing.WindowScore> w_scores = curSC.getAllWindowScores();
                    final int curSize = w_scores.size(); //This is to avoid w_scores size change when for loop is running.

                    try{
                        // 偵測是否符合規則, 若符合，改變curIdx並發出broadcast
                        CheckStroke(w_scores, curIdx);
                        Log.e(TAG, "Get Stroke!!!!");
                        // if no exception occur, jump curIdx to the window position where the score is lower than threshold
                        curIdx = GetJumpIndex(w_scores, curIdx);

                        Intent broadcast = new Intent(ACTION_STROKE_DETECTED_STATE);
                        mContext.sendBroadcast(broadcast);

                    } catch (NotMatchRuleException e) {
                        curIdx++;
                    } catch (ReservedException e) {}

                    //Log.d(TAG, curIdx + "");
                }
            }
        };
        detector_t.start();
    }

    // 檢查是否符合規則, 若不符或是access到保留區皆會丟出Exception
    public void CheckStroke(final Vector<ScoreComputing.WindowScore> AllWindows, int idx) throws NotMatchRuleException, ReservedException{
        if(AllWindows.size() < idx+RESERVEDWINDOWNUM) // it will access reserved space
            throw new ReservedException();

        for (int i = idx; i < idx + WINDOWTHRESHOLD; i++) {
            ScoreComputing.WindowScore ws = AllWindows.get(i);
            if (ws.score < SCORETHRESHOLD)
                throw new NotMatchRuleException();
        }
    }

    // 符合規則後, 需要決定下次從哪個Idx開始
    public int GetJumpIndex( final Vector<ScoreComputing.WindowScore> AllWindows, int idx ){
        int result_idx = idx;
        for(; result_idx < AllWindows.size() ; result_idx++){
            ScoreComputing.WindowScore ws = AllWindows.get(result_idx);
            if (ws.score < SCORETHRESHOLD)
                break;
        }
        return result_idx;
    }


    /**********************/
    /**   Custom Exception    **/
    /**********************/
    public class ReservedException extends Exception {public ReservedException() { super("The index is in reserved position");} }
    public class NotMatchRuleException extends Exception {public NotMatchRuleException() { super("The rule is not matched");} }



}
