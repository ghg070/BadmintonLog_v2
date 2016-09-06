package nctu.nol.algo;


import android.app.Activity;
import android.content.Intent;
import android.text.format.Time;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import nctu.nol.bt.devices.BeaconHandler;
import nctu.nol.bt.devices.SoundWaveHandler;
import nctu.nol.file.LogFileWriter;
import nctu.nol.file.SystemParameters;

public class StrokeDetector {
    private final static String TAG = StrokeDetector.class.getSimpleName();
    private Activity mContext;

    /* Window Related */
    private ScoreComputing curSC;
    private long prev_window_time;
    private float prev_window_audiomax = Float.NEGATIVE_INFINITY;

    /* Rule Related */
    private static double SCORETHRESHOLD = 0.55; // default, You can change by other method
    public static double RATIOTHRESHOLD = 0.325; // fixed, SumMainFreqPower / SquareSumAllFreqPower
    private static final int WINDOWTHRESHOLD = 2;
    private static final double alpha = 0.15; // used to count score threshold
    private static final int RESERVEDWINDOWNUM = 10; //Window數超過該變數後, 才開始進行判斷

    /* Thread Related */
    private Thread detector_t;

    /* Broadcast Related */
    public final static String ACTION_STROKE_DETECTED_STATE = "STROKEDETECTOR.ACTION_STROKE_DETECTED_STATE";
    public final static String EXTRA_STROKETIME = "STROKEDETECTOR.STROKETIME";

    /* Constructor, Class內的初始化(參數之類的), 在new這個Class的時候會自動觸發 */
    public StrokeDetector(Activity act, ScoreComputing sc){
        this.curSC = sc;
        this.mContext = act;
    }

    /* 根據訓練資料, 計算StrokeDetector的Score Threshold */
    public static double ComputeScoreThreshold(final List<HashMap.Entry<Float, Float>> FreqBands, final float[] audio_samples, final int SamplingRate, final int w_size){
        // Initial Parameter
        List<Integer> FreqIdxs = new ArrayList<Integer>();
        float FreqMax = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < FreqBands.size(); i++) {
            float freq = FreqBands.get(i).getKey();
            float power = FreqBands.get(i).getValue();
            FreqIdxs.add((int) (freq * w_size / SamplingRate));
            if (FreqMax < power)
                FreqMax = power;
        }

        // Counting Window Score of whole training audio data
        List<Float> AllScores = new ArrayList<Float>();
        float sum = 0;
        for(int i = 0; i <= (audio_samples.length-w_size); i+=w_size){

            float w_dataset[] = new float[w_size];

            // Copy data in specific window
            for (int j = i; (j-i) < w_size; j++)
                w_dataset[j-i] = audio_samples[j];

            // Get Spectrum
            FrequencyBandModel fbm  = new FrequencyBandModel();
            CountSpectrum cs = new CountSpectrum(FrequencyBandModel.FFT_LENGTH);
            final Vector<FrequencyBandModel.FreqBand> spec = fbm.getSpectrum(cs, 0, w_dataset, SoundWaveHandler.SAMPLE_RATE);

            // Count score
            float score = ScoreComputing.CountWindowScore(spec, FreqIdxs, FreqMax);
            AllScores.add(score);
            sum += score;
        }
        // Mean
        float score_mean = sum/AllScores.size();

        //Standard Deviation
        float score_std = 0;
        for(int i = 0; i < AllScores.size(); i++)
            score_std += Math.pow(AllScores.get(i)-score_mean, 2);
        score_std = (float)Math.sqrt(score_std/AllScores.size());


        SCORETHRESHOLD = score_mean + alpha * score_std;

        Log.d(TAG, "Score Threshold = "+SCORETHRESHOLD);

        return SCORETHRESHOLD;
    }

    /* 啟動Thread持續偵測Window分數是否連續達標 */
    public void StartStrokeDetector(final BeaconHandler bh){
        /*
        *   用Thread持續檢查Window Score的變化情形
        *   連續N個Window Score大於Threshold即稱為有擊球行為
        *   若偵測到擊球必須發送Broadcast出去(在MainActivity.java內另外實作接收端)
        * */
        detector_t = new Thread() {
            public void run() {
                // Wait Flag to true
                while(!SystemParameters.isServiceRunning.get());

                while(SystemParameters.isServiceRunning.get()){
                    final LinkedBlockingQueue<ScoreComputing.WindowScore> w_scores = curSC.getAllWindowScores();
                    try{
                        // 偵測是否符合規則, 若符合，改變curIdx並發出broadcast
                        long result = CheckStroke(w_scores);

                        Log.e(TAG, "Get Stroke!!!!");
                        if(result != 0) SystemParameters.StrokeTimes.add(result);

                        // if no exception occur, jump curIdx to the window position where the score is lower than threshold
                        JumpWindow(w_scores);

                        Intent broadcast = new Intent(ACTION_STROKE_DETECTED_STATE);
                        broadcast.putExtra(EXTRA_STROKETIME, result);
                        mContext.sendBroadcast(broadcast);

                    } catch (NotMatchRuleException e) {}
                    catch (ReservedException e) {}
                }
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
            if (ws.score < SCORETHRESHOLD || ws.ratio < RATIOTHRESHOLD) {
                prev_window_time = ws.w_time;
                prev_window_audiomax = ws.audio_max;
                throw new NotMatchRuleException();
            }

            if(i == 0 && prev_window_audiomax < ws.audio_max)
                stroke_t = ws.w_time;
            else if(i == 0 && prev_window_audiomax >= ws.audio_max)
                stroke_t = prev_window_time;
        }
        return stroke_t;
    }

    // 符合規則後, 繼續pop到score低於Threshold且超過8個Window為止為止
    public void JumpWindow( final LinkedBlockingQueue<ScoreComputing.WindowScore> AllWindows ){
        int jump_w = 8;
        while(AllWindows.size() > 0){
            ScoreComputing.WindowScore ws = AllWindows.poll();
            prev_window_time = ws.w_time;
            prev_window_audiomax = ws.audio_max;
            jump_w--;
            if (ws.score < SCORETHRESHOLD && jump_w < 1)
                break;
        }
    }

    /**********************/
    /**   Custom Exception    **/
    /**********************/
    public class ReservedException extends Exception {public ReservedException() { super("The index is in reserved position");} }
    public class NotMatchRuleException extends Exception {public NotMatchRuleException() { super("The rule is not matched");} }



}
