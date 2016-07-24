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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import nctu.nol.file.sqlite.DataListItem;

/**
 * Created by user on 2016/7/19.
 */
public class DataListPage extends Activity {
    private final static String TAG = DataListPage.class.getSimpleName();

    private ListView lv_StrokeData;
    private DataItemAdapter Adapter;
    private List<DataListItem.DataItem> stroke_dataset = new ArrayList<>();

    public final static String EXTRA_PATH = "DataListPage.EXTRA_PATH";
    public final static String EXTRA_OFFSET = "DataListPage.EXTRA_OFFSET";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datalist);

        stroke_dataset = SQLiteGetAllLoggingRecord();
        initialViewandEvent();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    private void initialViewandEvent(){
        lv_StrokeData = (ListView) findViewById(R.id.list_stroke_data);
        Adapter = new DataItemAdapter(DataListPage.this,stroke_dataset);
        lv_StrokeData.setAdapter(Adapter);
        lv_StrokeData.setOnItemClickListener(ListClickListener);
    }

    private ListView.OnItemClickListener ListClickListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            DataListItem.DataItem d_item = stroke_dataset.get(arg2);
            if(d_item.is_testing == 0){
                Intent i = new Intent(DataListPage.this, ShowTrainingData.class);
                i.putExtra(EXTRA_PATH, d_item.path);
                i.putExtra(EXTRA_OFFSET, d_item.offset);
                startActivity(i);
            }
        }
    };


   /*********************
     *    Local Database Related
     ***********************/
    private final List<DataListItem.DataItem> SQLiteGetAllLoggingRecord(){
        DataListItem dlistDB = new DataListItem(DataListPage.this);
        List<DataListItem.DataItem> list = dlistDB.getAll();
        dlistDB.close();
        return list;
    }


   /************************
     *     Custom ListView Related
     ***********************/
    public class DataItemAdapter extends BaseAdapter {
        private LayoutInflater myInflater;
        private List<DataListItem.DataItem> items;
        private Context context;

        public DataItemAdapter(Context context,List<DataListItem.DataItem> dataset){
            myInflater = LayoutInflater.from(context);
            this.items = dataset;
            this.context = context;
        }

        /*private view holder class*/
        private class ViewHolder {
            TextView DateTime;
            TextView DataType;
            TextView StrokeNum;
            ImageView imageView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;

            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.datalist_item, null); // 動態載入xml
                holder = new ViewHolder();
                holder.DateTime = (TextView) convertView.findViewById(R.id.tv_datetime);
                holder.DataType = (TextView) convertView.findViewById(R.id.tv_datatype);
                holder.imageView = (ImageView) convertView.findViewById(R.id.image_uploaded);
                holder.StrokeNum = (TextView) convertView.findViewById(R.id.tv_list_stroke_count);
                convertView.setTag(holder);
            }
            else {
                // 取得進入recycler的view(移到畫面外的row), 達到重複利用
                holder = (ViewHolder) convertView.getTag();
            }

            DataListItem.DataItem rowItem = (DataListItem.DataItem) getItem(position);

            holder.DateTime.setText(rowItem.date.substring(0,rowItem.date.length()-4)); // subtract milli sec
            holder.DataType.setText(getResources().getString((rowItem.is_testing == 1) ? R.string.TestingData : R.string.TrainingData ));
            holder.imageView.setImageResource((rowItem.isupdated == 1) ? R.drawable.uploaded : R.drawable.upload);
            holder.StrokeNum.setText("Stroke: " + String.valueOf(rowItem.stroke_num));

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
