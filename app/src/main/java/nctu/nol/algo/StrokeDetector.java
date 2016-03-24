package nctu.nol.algo;


import java.util.Vector;

import nctu.nol.file.SystemParameters;

public class StrokeDetector {

    /* Window Related */
    ScoreComputing curSC;

    /* Rule Related */
    private static final double SCORETHRESHOLD = 0.6;
    private static final int WINDOWTHRESHOLD = 2;

    /* Thread Related */
    private Thread detector_t;

    /* Broadcast Related */
    public final static String ACTION_STROKE_DETECTED_STATE = "STROKEDETECTOR.ACTION_STROKE_DETECTED_STATE";

    /* Constructor, Class內的初始化(參數之類的), 在new這個Class的時候會自動觸發 */
    public StrokeDetector(ScoreComputing sc){
        this.curSC = sc;
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
                    for(;curIdx < curSize -WINDOWTHRESHOLD+1; curIdx++) {
                        /*
                        *   偵測是否符合規則, 若符合，改變curIdx並發出broadcast
                        * */
                    }
                }
            }
        };
        detector_t.start();
    }

}
