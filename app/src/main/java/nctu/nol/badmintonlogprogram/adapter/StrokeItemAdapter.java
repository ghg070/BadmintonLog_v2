package nctu.nol.badmintonlogprogram.adapter;

/**
 * Created by Smile on 2016/8/8.
 */

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import nctu.nol.algo.StrokeClassifier;
import nctu.nol.badmintonlogprogram.R;
import nctu.nol.file.SystemParameters;
import nctu.nol.file.sqlite.StrokeItem;
import nctu.nol.file.sqlite.StrokeListItem;

/************************
 *     Custom ListView Related
 ***********************/
public class StrokeItemAdapter extends BaseAdapter {
    private LayoutInflater myInflater;
    private List<StrokeItem> items;
    private Context context;

    public StrokeItemAdapter(Context context,List<StrokeItem> dataset){
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

        StrokeItem rowItem = (StrokeItem) getItem(position);

        holder.StrokeNum.setText(String.valueOf(position + 1));
        holder.StrokeTime.setText(SystemParameters.MillisecToString(rowItem.stroke_time));
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