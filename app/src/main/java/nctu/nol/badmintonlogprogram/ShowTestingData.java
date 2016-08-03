package nctu.nol.badmintonlogprogram;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Vector;

import nctu.nol.algo.CountSpectrum;
import nctu.nol.algo.FrequencyBandModel;
import nctu.nol.badmintonlogprogram.chart.AudioWaveChart;
import nctu.nol.badmintonlogprogram.chart.SpectrumChart;
import nctu.nol.bt.devices.SoundWaveHandler;
import nctu.nol.file.sqlite.DataListItem;
import nctu.nol.file.sqlite.MainFreqListItem;

/**
 * Created by Smile on 2016/7/28.
 */
public class ShowTestingData extends Activity {
    private final static String TAG = ShowTrainingData.class.getSimpleName();

    private RelativeLayout chart_audio;
    private RelativeLayout chart_fft;
    private AudioWaveChart awc;
    private SpectrumChart sc;

    private final static int ROWCOUNT = FrequencyBandModel.PEAKFREQ_NUM;
    private TextView[] tv_Freqs = new TextView[ROWCOUNT];
    private TextView[] tv_Weight = new TextView[ROWCOUNT];
    private TextView tv_WeightTotal;
    private TextView tv_Threshold;

    // Extra data
    private long StrokeTime;
    private long DataID;
    private int BlockStartIndex;

    // Audio Data
    private double[] audio_time = {};
    private double[] audio_value = {};
    private float[] audio_value_f = {};


    // FFT Data
    private double[] fft_freq = {};
    private double[] fft_value = {};
    private double [] fft_mainfreq = new double[FrequencyBandModel.PEAKFREQ_NUM],
                fft_mainfreq_score = new double[FrequencyBandModel.PEAKFREQ_NUM];
    private double Threshold = 0;
    private Button bt_prev, bt_next;
    private int CurBlockIdx = 0;
    private int BlockNum = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_testing);

        initialViewandEvent();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            DataID = extras.getLong(StrokeListPage.EXTRA_ID);
            StrokeTime = extras.getLong(StrokeListPage.EXTRA_STROKETIME);
            audio_time = extras.getDoubleArray(StrokeListPage.EXTRA_AUDIODATA_TIME);
            audio_value = extras.getDoubleArray(StrokeListPage.EXTRA_AUDIODATA_VALUE);
            BlockStartIndex = extras.getInt(StrokeListPage.EXTRA_AUDIODATA_BLOCKSTARTIDX);
            Prepare();
        }

        registerReceiver(mChartClickEventReceiver, makeChartClickEventIntentFilter());
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mChartClickEventReceiver);
    }

    private void initialViewandEvent(){
        chart_audio = (RelativeLayout)findViewById(R.id.chart_onestroke_audio_wave);
        chart_fft = (RelativeLayout) findViewById(R.id.chart_onestroke_fft_wave);

        awc = new AudioWaveChart(ShowTestingData.this, chart_audio);
        sc = new SpectrumChart(ShowTestingData.this, chart_fft);

        bt_prev = (Button) findViewById(R.id.bt_onestroke_block_prev);
        bt_next = (Button) findViewById(R.id.bt_onestroke_block_next);
        bt_prev.setOnClickListener(prevListener);
        bt_next.setOnClickListener(nextListener);

        tv_Freqs[0] = (TextView) findViewById(R.id.tv_table_onestroke_freq1);
        tv_Freqs[1] = (TextView) findViewById(R.id.tv_table_onestroke_freq2);
        tv_Freqs[2] = (TextView) findViewById(R.id.tv_table_onestroke_freq3);
        tv_Freqs[3] = (TextView) findViewById(R.id.tv_table_onestroke_freq4);
        tv_Freqs[4] = (TextView) findViewById(R.id.tv_table_onestroke_freq5);

        tv_Weight[0] = (TextView) findViewById(R.id.tv_table_onestroke_weight1);
        tv_Weight[1] = (TextView) findViewById(R.id.tv_table_onestroke_weight2);
        tv_Weight[2] = (TextView) findViewById(R.id.tv_table_onestroke_weight3);
        tv_Weight[3] = (TextView) findViewById(R.id.tv_table_onestroke_weight4);
        tv_Weight[4] = (TextView) findViewById(R.id.tv_table_onestroke_weight5);

        tv_WeightTotal = (TextView) findViewById(R.id.tv_table_onestroke_weight_total);
        tv_Threshold = (TextView) findViewById(R.id.tv_table_onestroke_threshold);
    }

    private void Prepare(){
        final ProgressDialog dialog = ProgressDialog.show(ShowTestingData.this, "請稍後", "讀取音訊資料中", true);
        new Thread() {
            @Override
            public void run() {
                HandleAudioData(awc);
                HandleStrokeData(awc);
                HandleFreqTestingBlock(awc);

                runOnUiThread(new Runnable() {
                    public void run() {
                        awc.MakeChart();
                        ChangeFocusBlock(CurBlockIdx, true, DataID);
                        dialog.dismiss();
                    }
                });
            }
        }.start();
    }
    /**************
     *  Event Handler
     * **************/
    private Button.OnClickListener prevListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (CurBlockIdx != 0) {
                CurBlockIdx--;
                ChangeFocusBlock(CurBlockIdx, true, DataID);
            }
        }
    };

    private Button.OnClickListener nextListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (CurBlockIdx != BlockNum - 1) {
                CurBlockIdx++;
                ChangeFocusBlock(CurBlockIdx, true, DataID);
            }
        }
    };

    /********************
     *  Chart Data Handling Function
     * *********************/

    private void HandleAudioData(final AudioWaveChart awc){
        audio_value_f = new float[audio_time.length];
        for(int i = 0; i < audio_value.length; i++)
            audio_value_f[i] = (float) audio_value[i];

        awc.AddChartDataset(audio_time, audio_value, Color.argb(255, 51, 102, 0));
    }

    private void HandleStrokeData(final AudioWaveChart awc){
        double [] stroke_time = new double[1];
        double [] stroke_val = new double[1];

        // 指定到該時間點的最高點 (去掉小數後, 會有很多資料點擁有相同的時間點)
        int s_idx = -1;
        double max = Double.NEGATIVE_INFINITY;
        for(int i = 0; i < audio_value.length; i++){
            long time = (long) audio_time[i];
            if(time == StrokeTime && max < Math.abs(audio_value[i])){
                max = Math.abs(audio_value[i]);
                s_idx = i;
            }
        }

        stroke_time[0] = audio_time[s_idx];
        stroke_val[0] = audio_value[s_idx];
        awc.AddChartDataset(stroke_time, stroke_val, Color.RED);
    }

    private void HandleFreqTestingBlock(final AudioWaveChart awc){
        int idx = BlockStartIndex;
        while(idx+FrequencyBandModel.FFT_LENGTH < audio_time.length){
            double[] block_time = new double[2];
            double[] block_value = new double[2];

            block_time[0] = audio_time[idx];
            block_value[0] = 1;
            block_time[1] = audio_time[idx+FrequencyBandModel.FFT_LENGTH];
            block_value[1] = 1;
            idx += FrequencyBandModel.FFT_LENGTH;

            awc.AddChartDataset(block_time, block_value, Color.argb(60, 255, 0, 0));
            BlockNum++;

            // Initial Block Setting
            if(block_time[0] <= StrokeTime && block_time[1] > StrokeTime)
                CurBlockIdx = BlockNum-1;
        }
    }

    private void HandlerFFTData(final SpectrumChart sc, int start_position, long testing_id){
        //Use fft
        FrequencyBandModel fbm  = new FrequencyBandModel();
        CountSpectrum cs = new CountSpectrum(FrequencyBandModel.FFT_LENGTH);
        Vector<FrequencyBandModel.FreqBand> spec = fbm.getSpectrum(cs, start_position, audio_value_f, SoundWaveHandler.SAMPLE_RATE);

        fft_freq = new double[spec.size()];
        fft_value = new double[spec.size()];
        for(int i = 0; i < spec.size(); i++){
            fft_freq[i] = spec.get(i).Freq;
            fft_value[i] = spec.get(i).Power;
        }
        sc.AddChartDataset(fft_freq, fft_value, Color.BLUE);

        // FFT Main Freqs
        DataListItem dlistDB = new DataListItem(ShowTestingData.this);
        DataListItem.DataItem fileinfo = dlistDB.get(testing_id);
        dlistDB.close();

        long match_traing_id = fileinfo.match_id;
        MainFreqListItem mflistDB = new MainFreqListItem(ShowTestingData.this);
        MainFreqListItem.FreqModel model = mflistDB.GetFreqModel(match_traing_id);
        mflistDB.close();

        Threshold = model.threshold;
        double maxValue = Double.NEGATIVE_INFINITY;
        for(int i = 0 ; i < model.vals.length; i++){
            if(model.vals[i] > maxValue)
                maxValue = model.vals[i];
        }

        double[] mFreq = new double[FrequencyBandModel.PEAKFREQ_NUM];
        double[] mValue = new double[FrequencyBandModel.PEAKFREQ_NUM];
        for(int i = 0; i < FrequencyBandModel.PEAKFREQ_NUM; i++){
            int f_idx = (int)Math.round(model.freqs[i] / ((double) SoundWaveHandler.SAMPLE_RATE / FrequencyBandModel.FFT_LENGTH));
            mFreq[i] = fft_freq[f_idx];
            mValue[i] = fft_value[f_idx];

            fft_mainfreq[i] = fft_freq[f_idx];
            fft_mainfreq_score[i] = fft_value[f_idx]/maxValue;
        }
        bubbleSort(mFreq, mValue);
        sc.AddChartDataset(mFreq, mValue, Color.RED);
    }

    private double HandleFrequencyTable(){
        double sum = 0;
        for(int i = 0; i < ROWCOUNT; i++){
            if(fft_mainfreq.length > i) {
                tv_Freqs[ROWCOUNT - i - 1].setText(String.format("%.3f",fft_mainfreq[i]));
                tv_Weight[ROWCOUNT - i - 1].setText(String.format("%.3f",fft_mainfreq_score[i]));
                sum += fft_mainfreq_score[i];
            }
        }
        tv_WeightTotal.setText(String.format("%.3f", sum));
        tv_Threshold.setText(String.format("%.3f", Threshold));

        return sum;
    }


    private void ChangeFocusBlock(final int CurBlockIdx, final boolean MoveToCenter, final long testing_id){
        new Thread(){
            @Override
            public void run() {
                final int data_idx = CurBlockIdx*FrequencyBandModel.FFT_LENGTH + BlockStartIndex;

                sc.ClearAllDataset();
                HandlerFFTData(sc, data_idx, testing_id);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(MoveToCenter)
                            awc.MovePointToCenter(audio_time[data_idx], 0.5, 0.5);

                        double weight_sum = HandleFrequencyTable();
                        awc.ChangeSeriesColor(CurBlockIdx + 2, (weight_sum > Threshold) ? Color.argb(60, 0, 255, 0) : Color.argb(60, 40, 40, 40)); // 0: Audio Wave, 1: Peak Point, 2~end: Block
                        sc.MakeChart();
                    }
                });
            }
        }.start();

    }


    /********************/
    /**    Help Function     **/
    /********************/
    private void bubbleSort(final double[] compared_arr, final double[] other_arr) {
        boolean swapped = true;
        int j = 0;
        double tmp;
        while (swapped) {
            swapped = false;
            j++;
            for (int i = 0; i < compared_arr.length - j; i++) {
                if (compared_arr[i] > compared_arr[i + 1]) {
                    tmp = compared_arr[i];
                    compared_arr[i] = compared_arr[i + 1];
                    compared_arr[i + 1] = tmp;

                    tmp = other_arr[i];
                    other_arr[i] = other_arr[i + 1];
                    other_arr[i + 1] = tmp;

                    swapped = true;
                }
            }
        }
    }

    /**********************/
    /**    Broadcast Event	 **/
    /**********************/
    private final BroadcastReceiver mChartClickEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if( AudioWaveChart.ACTION_CLICK_EVENT.equals(action) ) {
                double clicked_time = intent.getDoubleExtra(AudioWaveChart.EXTRA_CLICK_POSITION_TIME, 0);

                // check clicked_time is in fft block
                for(int i = 0; i < BlockNum; i++) {
                    int idx = BlockStartIndex + i*FrequencyBandModel.FFT_LENGTH;

                    if(idx+FrequencyBandModel.FFT_LENGTH < audio_time.length
                            && audio_time[idx] <= clicked_time
                            && audio_time[idx+FrequencyBandModel.FFT_LENGTH] > clicked_time){

                            int block_idx = i;
                            CurBlockIdx = block_idx;
                            ChangeFocusBlock(CurBlockIdx, false, DataID);
                            break;
                    }
                }
            }
        }
    };
    private static IntentFilter makeChartClickEventIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AudioWaveChart.ACTION_CLICK_EVENT);
        return intentFilter;
    }
}
