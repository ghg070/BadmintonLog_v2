package nctu.nol.badmintonlogprogram.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import nctu.nol.badmintonlogprogram.R;
import nctu.nol.file.sqlite.DataListItem;

/**
 * Created by Smile on 2016/8/8.
 */
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
        holder.DataType.setText(context.getResources().getString((rowItem.is_testing == 1) ? R.string.TestingData : R.string.TrainingData ));
        holder.imageView.setImageResource((rowItem.isupdated == 1) ? R.drawable.uploaded : R.drawable.upload);
        holder.StrokeNum.setText((rowItem.is_testing == 1) ? "Stroke: " + String.valueOf(rowItem.stroke_num) : "");

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
