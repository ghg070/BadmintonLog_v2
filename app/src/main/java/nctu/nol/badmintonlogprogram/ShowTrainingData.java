package nctu.nol.badmintonlogprogram;

import android.app.Activity;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import nctu.nol.algo.FrequencyBandModel;
import nctu.nol.algo.PeakDetector;
import nctu.nol.badmintonlogprogram.chart.AudioWaveChart;
import nctu.nol.bt.devices.SoundWaveHandler;
import nctu.nol.file.WavReader;

/**
 * Created by Smile on 2016/7/21.
 */
public class ShowTrainingData extends Activity {
    private final static String TAG = ShowTrainingData.class.getSimpleName();

    private LinearLayout chart_layout;

    // Extra data
    private String DataPath;
    private long offset;

    // Audio Data
    private double[] audio_time = null;
    private double[] audio_value = null;

    // Peak Data
    private double[] peak_time = null;
    private double[] peak_value = null;
    private int[] peak_idx = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_training);

        initialViewandEvent();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            DataPath = extras.getString(DataListPage.EXTRA_PATH);
            offset = extras.getLong(DataListPage.EXTRA_OFFSET);
            Prepare();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    private void initialViewandEvent(){
        chart_layout = (LinearLayout)findViewById(R.id.chart_whole_audio_wave);
    }

    private void Prepare(){
        final ProgressDialog dialog = ProgressDialog.show(ShowTrainingData.this, "請稍後", "讀取音訊資料中", true);
        new Thread() {
            @Override
            public void run() {

                final AudioWaveChart awc = new AudioWaveChart(ShowTrainingData.this);
                HandleAudioData(awc);
                HandlePeakData(awc);
                HandleFreqTrainingBlock(awc);

                runOnUiThread(new Runnable() {
                    public void run() {
                        awc.MakeChart(chart_layout);
                        dialog.dismiss();
                    }
                });
            }
        }.start();
    }


    private void HandleAudioData(final AudioWaveChart awc){
        SetAudioSamplesByPath(DataPath, offset);
        awc.AddChartDataset(audio_time, audio_value, Color.argb(255, 51, 102, 0));
    }

    private void HandlePeakData(final AudioWaveChart awc){
        // Peak Data
        float [] time_f = new float[audio_time.length];
        float [] val_f = new float[audio_value.length];
        for(int i = 0 ; i < audio_time.length; i++){
            // double to float
            time_f[i] = (float)audio_time[i];
            val_f[i] = (float)audio_value[i];
        }
        PeakDetector pd = new PeakDetector(700, 350);
        List<Integer> peaks = pd.findPeakIndex(time_f, val_f, (float)0.35);
        peak_time = new double[peaks.size()];
        peak_value = new double[peaks.size()];
        peak_idx = new int[peaks.size()];
        for(int i = 0; i < peaks.size(); i++){
            int idx = peaks.get(i);
            peak_idx[i] = idx;
            peak_time[i] = audio_time[idx];
            peak_value[i] = audio_value[idx];
        }
        awc.AddChartDataset(peak_time, peak_value, Color.RED);
    }

    private void HandleFreqTrainingBlock(final AudioWaveChart awc){
/*
        double[] block_time = new double[2];
        double[] block_value = new double[2];

        for(int i = 0; i < peak_idx.length; i++){
            int idx = peak_idx[i];
            for(int j = 0; j < FrequencyBandModel.WINDOW_NUM; j++){
                block_time[0] = audio_time[idx];
                block_value[0] = 0.8;
                block_time[1] = audio_time[idx+512];
                block_value[1] = 0.8;
                idx += FrequencyBandModel.FFT_LENGTH;
                awc.AddChartDataset(block_time, block_value, Color.argb(120, 255, 0, 0));
            }
        }
*/
    }

    private void SetAudioSamplesByPath(final String path, final long offset) {
        // Read Wav File, store data
        try {
            WavReader wr = new WavReader(new FileInputStream(path + "Sound.wav"));
            short[] samples = wr.getShortSamples();

            audio_time = new double[samples.length];
            audio_value = new double[samples.length];

            float deltaT = (1 / (float) SoundWaveHandler.SAMPLE_RATE) * 1000;

            for (int i = 0; i < samples.length; i++) {
                audio_time[i] = offset + deltaT * i;
                audio_value[i] = (float)samples[i] / 32768;
            }

        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
