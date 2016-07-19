package nctu.nol.badmintonlogprogram;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private ListView lv_StrokeData;
    private DataItemAdapter Adapter;
    private List<DataListItem.DataItem> stroke_dataset = new ArrayList<>();

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

        //listStrokeData.setOnItemClickListener(ListClickListener);

    }

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
