package nctu.nol.badmintonlogprogram;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import nctu.nol.account.API;
import nctu.nol.account.LoginPage;
import nctu.nol.account.NetworkCheck;
import nctu.nol.account.ResponseListener;
import nctu.nol.file.sqlite.DataListItem;

/**
 * Created by user on 2016/7/19.
 */
public class DataListPage extends Activity {
    private final static String TAG = DataListPage.class.getSimpleName();

    private ListView lv_StrokeData;
    private DataItemAdapter Adapter;
    private List<DataListItem.DataItem> stroke_dataset = new ArrayList<>();

    public final static String EXTRA_ID = "DataListPage.EXTRA_ID";
    public final static String EXTRA_PATH = "DataListPage.EXTRA_PATH";
    public final static String EXTRA_OFFSET = "DataListPage.EXTRA_OFFSET";


    //file upload
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    private ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datalist);

        sharedPreferences = this.getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        editor = sharedPreferences.edit();

        stroke_dataset = SQLiteGetAllLoggingRecord();
        initialViewandEvent();
        upload();
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

            File dir = new File(d_item.path);
            if(dir.isDirectory()) {
                if (d_item.is_testing == 0) {
                    Intent i = new Intent(DataListPage.this, ShowTrainingData.class);
                    i.putExtra(EXTRA_ID, d_item.id);
                    i.putExtra(EXTRA_PATH, d_item.path);
                    i.putExtra(EXTRA_OFFSET, d_item.offset);
                    startActivity(i);
                } else {
                    Intent i = new Intent(DataListPage.this, StrokeListPage.class);
                    i.putExtra(EXTRA_ID, d_item.id);
                    i.putExtra(EXTRA_PATH, d_item.path);
                    i.putExtra(EXTRA_OFFSET, d_item.offset);
                    startActivity(i);
                }
            }else{
                new AlertDialog.Builder(DataListPage.this)
                        .setTitle("檔案遺失")
                        .setMessage("無法進行資料分析")
                        .setNegativeButton("確認",null)
                        .show();
            }
        }
    };
    /************************
     *     file upload
     ***********************/
    private void upload(){
        dialog = ProgressDialog.show(DataListPage.this, "請稍後", "檔案上傳中", true);
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000);
                    for(int i=0;i<stroke_dataset.size();i++)
                    {
                        if(stroke_dataset.get(i).isupdated==0)
                        {
                            try {
                                folderfileupload(stroke_dataset.get(i).path.toString());
                                DataListItem dlistDB = new DataListItem(DataListPage.this);
                                dlistDB.update(stroke_dataset.get(i).id, true);
                                dlistDB.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            stroke_dataset = SQLiteGetAllLoggingRecord();
                            Adapter = new DataItemAdapter(DataListPage.this, stroke_dataset);
                            lv_StrokeData.setAdapter(Adapter);
                        }
                    });
                }  catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    dialog.dismiss();
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }
    private void token_check() {
        API.check_token_valid(sharedPreferences.getString("token", null), new ResponseListener() {
            public void onResponse(JSONObject response) {

            }

            public void onErrorResponse(VolleyError error) {
                API.login(sharedPreferences.getString("account", null), sharedPreferences.getString("passwd", null), new ResponseListener() {
                    public void onResponse(JSONObject response) {
                        Log.d("Tag", "token expire,getting new token");
                        String token = null;
                        try {
                            token = response.getJSONObject("data").getString("token");
                            editor.putString("token", token);
                            editor.commit();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(DataListPage.this, "no token found while uploading, please login again", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(DataListPage.this, LoginPage.class);
                        startActivity(i);
                    }
                });
            }
        });
    }
    private void fileupload(final String filepath,final String filename) throws IOException {
        if (NetworkCheck.isNetworkConnected(DataListPage.this)) {
            token_check();
            final String token = sharedPreferences.getString("token", null);
            API.upload_file(filepath, token, filename, sharedPreferences.getString("account", null), new ResponseListener() {
                public void onResponse(JSONObject response) {
                    Toast.makeText(DataListPage.this, "upload success@@", Toast.LENGTH_SHORT).show();
                }

                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(DataListPage.this, "something went wrong while uploading", Toast.LENGTH_SHORT).show();
                    if (error.networkResponse.data != null) {
                        try {
                            String body = new String(error.networkResponse.data, "UTF-8");
                            Log.d("Tag", body);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }

                    Log.d("Tag",String.valueOf(error.networkResponse.statusCode));

                    editor.putString("account",sharedPreferences.getString("account",null));
                    editor.commit();

                }





        });

        } else {
            Toast.makeText(DataListPage.this, "no network aviable now, will upload later", Toast.LENGTH_SHORT).show();
            editor.putString("account", sharedPreferences.getString("account", null));
            editor.commit();
        }
    }

    private void folderfileupload(final String dirpath) throws IOException {
        File file_dir = new File(dirpath);
        File[] files = file_dir.listFiles();
        for (int i = 0; i < files.length; ++i) {
            String filename = files[i].getName();
            fileupload(dirpath,filename);
        }
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
}
