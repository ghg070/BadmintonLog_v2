package nctu.nol.badmintonlogprogram;

import android.app.Activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import nctu.nol.algo.CountSpectrum;
import nctu.nol.algo.FrequencyBandModel;
import nctu.nol.algo.PeakDetector;
import nctu.nol.algo.TrainingWindowFinder;
import nctu.nol.badmintonlogprogram.chart.AudioWaveChart;
import nctu.nol.badmintonlogprogram.chart.SpectrumChart;
import nctu.nol.bt.devices.BeaconHandler;
import nctu.nol.bt.devices.SoundWaveHandler;
import nctu.nol.file.SystemParameters;
import nctu.nol.file.WavReader;
import nctu.nol.file.sqlite.MainFreqListItem;

/**
 * Created by Smile on 2016/7/21.
 */
public class ShowTrainingData extends Activity {
    private final static String TAG = ShowTrainingData.class.getSimpleName();

    private RelativeLayout chart_audio;
    private RelativeLayout chart_fft;
    private AudioWaveChart awc;
    private SpectrumChart sc;

    private final static int ROWCOUNT = FrequencyBandModel.PEAKFREQ_NUM;
    private TextView[] tv_Freqs = new TextView[ROWCOUNT];
    private TextView[] tv_Energy = new TextView[ROWCOUNT];

    // Extra data
    private String DataPath;
    private long offset;
    private long DataID;

    // Audio Data
    private double[] audio_time = {};
    private double[] audio_value = {};
    private float[] audio_time_f = {};
    private float[] audio_value_f = {};

    // Training Position
    private int[] training_pos_idx = {};

    // FFT Data
    private double[] fft_freq = {};
    private double[] fft_value = {};
    private Button bt_prev, bt_next;
    private int CurBlockIdx = -1;
    private int TotalBlockCount = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_training);

        initialViewandEvent();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            DataID = extras.getLong(DataListPage.EXTRA_ID);
            DataPath = extras.getString(DataListPage.EXTRA_PATH);
            offset = extras.getLong(DataListPage.EXTRA_OFFSET);
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
        chart_audio = (RelativeLayout)findViewById(R.id.chart_whole_audio_wave);
        chart_fft = (RelativeLayout) findViewById(R.id.chart_fft_wave);

        awc = new AudioWaveChart(ShowTrainingData.this, chart_audio);
        sc = new SpectrumChart(ShowTrainingData.this, chart_fft);

        bt_prev = (Button) findViewById(R.id.bt_block_prev);
        bt_next = (Button) findViewById(R.id.bt_block_next);
        bt_prev.setOnClickListener(prevListener);
        bt_next.setOnClickListener(nextListener);

        tv_Freqs[0] = (TextView) findViewById(R.id.tv_table_freq1);
        tv_Freqs[1] = (TextView) findViewById(R.id.tv_table_freq2);
        tv_Freqs[2] = (TextView) findViewById(R.id.tv_table_freq3);
        tv_Freqs[3] = (TextView) findViewById(R.id.tv_table_freq4);
        tv_Freqs[4] = (TextView) findViewById(R.id.tv_table_freq5);

        tv_Energy[0] = (TextView) findViewById(R.id.tv_table_power1);
        tv_Energy[1] = (TextView) findViewById(R.id.tv_table_power2);
        tv_Energy[2] = (TextView) findViewById(R.id.tv_table_power3);
        tv_Energy[3] = (TextView) findViewById(R.id.tv_table_power4);
        tv_Energy[4] = (TextView) findViewById(R.id.tv_table_power5);
    }

    private void Prepare(){
        final ProgressDialog dialog = ProgressDialog.show(ShowTrainingData.this, "請稍後", "讀取音訊資料中", true);
        new Thread() {
            @Override
            public void run() {
                HandleAudioData(awc);
                HandleTrainingPosition(awc);
                HandleFreqTrainingBlock(awc);

                runOnUiThread(new Runnable() {
                    public void run() {
                        awc.MakeChart();

                        if(CurBlockIdx == -1)
                            CurBlockIdx = 0;
                        ChangeFocusBlock(CurBlockIdx, true);

                        HandleFrequencyTable(DataID);
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
            if(training_pos_idx.length > 0) {
                if (CurBlockIdx > 0) {
                    CurBlockIdx--;
                    ChangeFocusBlock(CurBlockIdx, true);
                }
            }
        }
    };

    private Button.OnClickListener nextListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(training_pos_idx.length > 0) {
                if (CurBlockIdx < TotalBlockCount) {
                    CurBlockIdx++;
                    ChangeFocusBlock(CurBlockIdx, true);
                }
            }
        }
    };

    /********************
     *  Chart Data Handling Function
     * *********************/
    private void HandleAudioData(final AudioWaveChart awc){
        SetAudioSamplesByPath(DataPath, offset);
        awc.AddChartDataset(audio_time, audio_value, Color.argb(255, 51, 102, 0));
    }

    private void HandleTrainingPosition(final AudioWaveChart awc){
        //Find Training Window
        TrainingWindowFinder twFinder = new TrainingWindowFinder(FrequencyBandModel.FFT_LENGTH);
        List<Integer> wPos = twFinder.findWindowIndex(audio_time_f, audio_value_f);

        double []training_pos_time = new double[wPos.size()];
        double []training_pos_value = new double[wPos.size()];
        training_pos_idx = new int[wPos.size()];
        for(int i = 0; i < wPos.size(); i++){
            int idx = wPos.get(i);
            training_pos_idx[i] = idx;
            training_pos_time[i] = audio_time[idx];
            training_pos_value[i] = audio_value[idx];
        }
        awc.AddChartDataset(training_pos_time, training_pos_value, Color.TRANSPARENT);
    }

    private void HandleFreqTrainingBlock(final AudioWaveChart awc){
        int TrainingPosCount = 0, curPos = 0;
        int redCount = 0;

        while(curPos + FrequencyBandModel.FFT_LENGTH < audio_time.length){
            int CurTrainingPos = training_pos_idx[TrainingPosCount];
            if(curPos == CurTrainingPos){
                redCount+=5;
                if(TrainingPosCount < training_pos_idx.length-1)
                    TrainingPosCount++;
            }

            double[] block_time = new double[2];
            double[] block_value = new double[2];
            block_time[0] = audio_time[curPos];
            block_value[0] = 1;
            block_time[1] = audio_time[curPos+FrequencyBandModel.FFT_LENGTH];
            block_value[1] = 1;

            if(redCount > 0){
                if(CurBlockIdx == -1)
                    CurBlockIdx = TotalBlockCount;

                awc.AddChartDataset(block_time, block_value, Color.argb(60, 255, 0, 0));
                redCount--;
            }else
                awc.AddChartDataset(block_time, block_value, Color.argb(60, 40, 40, 40));

            curPos += FrequencyBandModel.FFT_LENGTH;
            TotalBlockCount++;
        }

    }

    private void HandlerFFTData(final SpectrumChart sc, int start_position){
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
        List<Integer> mainfreqs = fbm.FindSpectrumPeakIndex(spec, FrequencyBandModel.PEAKFREQ_NUM);
        double [] fft_mainfreq = new double[FrequencyBandModel.PEAKFREQ_NUM],
                fft_mainvalue = new double[FrequencyBandModel.PEAKFREQ_NUM];
        for(int i = 0; i < mainfreqs.size(); i++){
            int idx = mainfreqs.get(i);
            fft_mainfreq[i] = spec.get(idx).Freq;
            fft_mainvalue[i] = spec.get(idx).Power;
        }
        bubbleSort(fft_mainfreq, fft_mainvalue);
        sc.AddChartDataset(fft_mainfreq, fft_mainvalue, Color.RED);
    }

    private void HandleFrequencyTable(long id){
        MainFreqListItem mflistDB = new MainFreqListItem(ShowTrainingData.this);
        MainFreqListItem.FreqModel result = mflistDB.GetFreqModel(id);
        mflistDB.close();

        if(result != null){
            for(int i = 0; i < ROWCOUNT; i++){
                if(result.freqs.length > i) {
                    tv_Freqs[ROWCOUNT - i - 1].setText(String.valueOf(Math.round(result.freqs[i])));
                    tv_Energy[ROWCOUNT - i - 1].setText(String.format("%.2f",result.vals[i]));
                }
            }
        }
    }

    private void SetAudioSamplesByPath(final String path, final long offset) {
        // Read Wav File, store data
        try {
            WavReader wr = new WavReader(new FileInputStream(path + "Sound.wav"));
            short[] samples = wr.getShortSamples();

            audio_time = new double[samples.length];
            audio_value = new double[samples.length];
            audio_time_f = new float[samples.length];
            audio_value_f = new float[samples.length];

            float deltaT = (1 / (float) SoundWaveHandler.SAMPLE_RATE) * 1000;

            for (int i = 0; i < samples.length; i++) {
                audio_time[i] = offset + deltaT * i;
                audio_value[i] = (double)samples[i] / 32768;

                audio_time_f[i] = offset + deltaT * i;
                audio_value_f[i] = (float)samples[i] / 32768;
            }

        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void ChangeFocusBlock(final int CurBlockIdx, final boolean MoveToCenter){
        new Thread(){
            @Override
            public void run() {
                final int data_idx = CurBlockIdx*FrequencyBandModel.FFT_LENGTH;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(MoveToCenter)
                            awc.MovePointToCenter(audio_time[data_idx], 0.5, 0.5);
                        awc.ChangeSeriesColor(CurBlockIdx + 2, Color.argb(60, 0, 255, 0)); // 0: Audio Wave, 1: Peak Point, 2~end: Block
                        sc.ClearAllDataset();
                        HandlerFFTData(sc, data_idx);
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
                for(int i = 0; i < TotalBlockCount; i++) {
                    int idx = i*FrequencyBandModel.FFT_LENGTH;
                    if( audio_time[idx] <= clicked_time && audio_time[idx+FrequencyBandModel.FFT_LENGTH] > clicked_time){
                        CurBlockIdx = i;
                        ChangeFocusBlock(CurBlockIdx, false);
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
