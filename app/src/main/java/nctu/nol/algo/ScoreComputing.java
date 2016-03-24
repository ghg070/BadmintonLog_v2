package nctu.nol.algo;


import java.util.Vector;

import nctu.nol.bt.devices.SoundWaveHandler;
import nctu.nol.file.SystemParameters;

public class ScoreComputing {

    /* Audio Data Related */
    private SoundWaveHandler curSW; //用來獲得SoundWaveHandler的物件, 目的是為了取得取樣時的音訊資料
    private Vector<WindowScore> AllWindowScores = new Vector<WindowScore>(); //用來儲存所有的Window時間、分數資訊

    /* Thread Related */
    private Thread computing_t;

    /* Constructor, Class內的初始化(參數之類的), 在new這個Class的時候會自動觸發 */
    public ScoreComputing(SoundWaveHandler sw){
        this.curSW = sw;
    }

    /* 啟動Thread持續計算Window分數 */
    public void StartComputingScore(){
        /*
        *   用Thread去觀察curSW內音訊資料數量, 每512點就必須計算一個WindowScore
        *   計算出來的結果請存到AllWindowScores內
        *   ps. 必須要有變數去儲存目前算到哪裡
        *
        * */
        computing_t = new Thread() {
            public void run() {
                while(SystemParameters.isServiceRunning.get()){
                    // Do Something....
                }
            }
        };
        computing_t.start();
    }


    /* 其他Class要使用AllWindowScores時, 會用到的函式 */
    public final Vector<WindowScore> getAllWindowScores(){
        return AllWindowScores;
    }

    /* 用來儲存每個Window的時間以及分數 */
    public class WindowScore{
        public long w_time; //用來儲存Window內第一筆資料的時間戳記
        public double score; //用來儲存Window經計算後的分數

        /* Constructor, Class內的初始化(參數之類的), 在new這個Class的時候會自動觸發 */
        public WindowScore(long timestamp, double score){
            this.w_time = timestamp;
            this.score = score;
        }
    }


}
