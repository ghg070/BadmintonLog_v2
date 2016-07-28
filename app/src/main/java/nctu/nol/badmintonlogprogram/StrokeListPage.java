package nctu.nol.badmintonlogprogram;

import android.app.Activity;
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

import java.util.ArrayList;
import java.util.List;

import nctu.nol.algo.StrokeClassifier;
import nctu.nol.file.sqlite.StrokeListItem;

/**
 * Created by Smile on 2016/7/28.
 */
public class StrokeListPage extends Activity {
    private final static String TAG = StrokeListPage.class.getSimpleName();

    private ListView lv_StrokeData;
    private StrokeItemAdapter Adapter;
    private List< StrokeListItem.StrokeItem> strokelist_dataset = new ArrayList<>();

    private long DataID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.strokelist);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
            DataID = extras.getLong(DataListPage.EXTRA_ID);
        strokelist_dataset = SQLiteGetStrokeById(DataID);
        initialViewandEvent();
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

            //Intent i = new Intent(DataListPage.this, ShowTrainingData.class);
            //startActivity(i);
        }
    };
    /**********************
     *      SQLite Related
     ***********************/
    public List< StrokeListItem.StrokeItem> SQLiteGetStrokeById(long id){
        StrokeListItem slistDB = new StrokeListItem(StrokeListPage.this);
        List< StrokeListItem.StrokeItem> result = slistDB.GetStrokesInOneTestingFile(id);
        slistDB.close();

        return result;
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
