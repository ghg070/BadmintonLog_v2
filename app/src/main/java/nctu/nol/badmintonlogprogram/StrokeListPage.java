package nctu.nol.badmintonlogprogram;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import nctu.nol.algo.FrequencyBandModel;
import nctu.nol.algo.StrokeClassifier;
import nctu.nol.bt.devices.SoundWaveHandler;
import nctu.nol.file.WavReader;
import nctu.nol.file.sqlite.StrokeListItem;

/**
 * Created by Smile on 2016/7/28.
 */
public class StrokeListPage extends Activity {
    private final static String TAG = StrokeListPage.class.getSimpleName();

    public final static String EXTRA_ID = "StrokeListPage.EXTRA_ID";
    public final static String EXTRA_STROKETIME = "StrokeListPage.EXTRA_STROKETIME";
    public final static String EXTRA_AUDIODATA_TIME = "StrokeListPage.EXTRA_AUDIODATA_TIME";
    public final static String EXTRA_AUDIODATA_VALUE = "StrokeListPage.EXTRA_AUDIODATA_VALUE";
    public final static String EXTRA_AUDIODATA_BLOCKSTARTIDX = "StrokeListPage.EXTRA_AUDIODATA_BLOCKSTARTIDX";

    private long DataID = -1;
    private String DataPath;
    private long offset;

    // Audio Data
    private double[] whole_audio_time = {};
    private double[] whole_audio_value = {};
    private final static long SHOWRANGE = 1000; // show 1 sec data

    // Stroke
    private List< StrokeListItem.StrokeItem> strokelist_dataset = new ArrayList<>();
    private ListView lv_StrokeData;
    private StrokeItemAdapter Adapter;
    private List<StrokeInfo> StrokeTable = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.strokelist);

        Bundle extras = getIntent().getExtras();
        initialViewandEvent();
        if (extras != null) {
            DataID = extras.getLong(DataListPage.EXTRA_ID);
            DataPath = extras.getString(DataListPage.EXTRA_PATH);
            offset = extras.getLong(DataListPage.EXTRA_OFFSET);
            Prepare();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    private void initialViewandEvent(){
        lv_StrokeData = (ListView) findViewById(R.id.list_each_stroke_info);
        Adapter = new StrokeItemAdapter(StrokeListPage.this, strokelist_dataset);
        lv_StrokeData.setAdapter(Adapter);
        lv_StrokeData.setOnItemClickListener(ListClickListener);
    }

    private ListView.OnItemClickListener ListClickListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            StrokeListItem.StrokeItem slist_item = strokelist_dataset.get(arg2);
            StrokeInfo info = StrokeTable.get(arg2);

            int len = info.right_idx-info.left_idx+1;
            double[] audio_time = new double[len];
            double[] audio_val = new double[len];
            for(int i = 0; i < len; i++){
                audio_time[i] = whole_audio_time[info.left_idx+i];
                audio_val[i] = whole_audio_value[info.left_idx+i];
            }

            Intent i = new Intent(StrokeListPage.this, ShowTestingData.class);
            i.putExtra(EXTRA_ID, DataID);
            i.putExtra(EXTRA_STROKETIME, slist_item.stroke_time);
            i.putExtra(EXTRA_AUDIODATA_TIME, audio_time);
            i.putExtra(EXTRA_AUDIODATA_VALUE, audio_val);
            i.putExtra(EXTRA_AUDIODATA_BLOCKSTARTIDX, info.block_start_idx);

            startActivity(i);
        }
    };

    private void Prepare(){
        final ProgressDialog dialog = ProgressDialog.show(StrokeListPage.this, "請稍後", "讀取音訊資料中", true);
        new Thread() {
            @Override
            public void run(){
                strokelist_dataset.clear();
                strokelist_dataset.addAll(SQLiteGetStrokeById(DataID));
                SetAudioSamplesByPath(DataPath, offset);
                BuildStrokeTable(strokelist_dataset, whole_audio_time);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Adapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                });
            }
        }.start();

    }

    /***************
     *    File Related
     * ***************/
    private void SetAudioSamplesByPath(final String path, final long offset) {
        // Read Wav File, store data
        try {
            WavReader wr = new WavReader(new FileInputStream(path + "Sound.wav"));
            short[] samples = wr.getShortSamples();

            whole_audio_time = new double[samples.length];
            whole_audio_value = new double[samples.length];
            float deltaT = (1 / (float) SoundWaveHandler.SAMPLE_RATE) * 1000;
            for (int i = 0; i < samples.length; i++) {
                whole_audio_time[i] = offset + deltaT * i;
                whole_audio_value[i] = (double)samples[i] / 32768;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
    }



    /**********************
     *      SQLite Related
     ***********************/
    public List< StrokeListItem.StrokeItem> SQLiteGetStrokeById(long id){
        StrokeListItem slistDB = new StrokeListItem(StrokeListPage.this);
        List< StrokeListItem.StrokeItem> result = slistDB.GetStrokesInOneTestingFile(id);
        slistDB.close();
        return result;
    }

    /******************
     *      Stroke Table
     * ******************/
    private class StrokeInfo{
        public int left_idx;
        public int right_idx;
        public int block_start_idx;
        public StrokeInfo(){
            left_idx = -1;
            right_idx = -1;
            block_start_idx = -1;
        }
    }

    private void BuildStrokeTable(final List<StrokeListItem.StrokeItem> dataset, final double[] audio_time){
        int curStrokeCount = 0;
        int left = -1, right;
        for(int i = 0; i < audio_time.length; i++){
            if(curStrokeCount >= dataset.size())
                break;

            long curStrokeTime = dataset.get(curStrokeCount).stroke_time;
            if(left == -1 && audio_time[i] >= curStrokeTime-SHOWRANGE/2) {
                left = i;
                right = left;
                while(right < audio_time.length &&  audio_time[right] < curStrokeTime+SHOWRANGE/2)
                    right++;

                StrokeInfo info = new StrokeInfo();
                info.left_idx = left;
                info.right_idx = right;

                int difference = FrequencyBandModel.FFT_LENGTH - left%FrequencyBandModel.FFT_LENGTH;
                info.block_start_idx = difference;

                StrokeTable.add(info);

                left = -1;
                curStrokeCount++;
            }
        }
    }

    /************************
     *     Custom ListView Related
     ***********************/
    public class StrokeItemAdapter extends BaseAdapter {
        private LayoutInflater myInflater;
        private List<StrokeListItem.StrokeItem> items;
        private Context context;

        public StrokeItemAdapter(Context context,List<StrokeListItem.StrokeItem> dataset){
            myInflater = LayoutInflater.from(context);
            this.items = dataset;
            this.context = context;
        }

        /*private view holder class*/
        private class ViewHolder {
            TextView StrokeNum;
            TextView StrokeTime;
            TextView StrokeType;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;

            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.strokelist_item, null); // 動態載入xml
                holder = new ViewHolder();
                holder.StrokeNum = (TextView) convertView.findViewById(R.id.tv_strokelist_num);
                holder.StrokeTime = (TextView) convertView.findViewById(R.id.tv_strokelist_time);
                holder.StrokeType = (TextView) convertView.findViewById(R.id.tv_strokelist_type);

                convertView.setTag(holder);
            }
            else {
                // 取得進入recycler的view(移到畫面外的row), 達到重複利用
                holder = (ViewHolder) convertView.getTag();
            }

            StrokeListItem.StrokeItem rowItem = (StrokeListItem.StrokeItem) getItem(position);

            holder.StrokeNum.setText(String.valueOf(position + 1));
            holder.StrokeTime.setText(StrokeClassifier.MillisecToString(rowItem.stroke_time));
            holder.StrokeType.setText(rowItem.stroke_type);

            return convertView;
        }

        @Override
        public int getCount() {
            return items.size();
        }
        @Override
        public Object getItem(int id) {
            return items.get(id);
        }
        @Override
        public long getItemId(int position) {
            return items.indexOf(getItem(position));
        }

    };
}
