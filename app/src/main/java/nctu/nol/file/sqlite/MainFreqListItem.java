package nctu.nol.file.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by user on 2016/7/27.
 */
public class MainFreqListItem {
    // 表格名稱
    public static final String TABLE_NAME = "mainfreqlist_item";

    // 編號表格欄位名稱，固定不變
    public static final String KEY_ID = "_id";

    // 其它表格欄位名稱
    public static final String FREQS_COLUMN = "freqs";
    public static final String VALUES_COLUMN = "fvalues";
    public static final String THRESHOLD_COLUMN = "score_threshold";
    public static final String MATCH_TRAINING_FILE = "match_training_id";


    // 使用上面宣告的變數建立表格的SQL指令
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    FREQS_COLUMN + " TEXT NOT NULL, " +
                    VALUES_COLUMN + " TEXT NOT NULL, " +
                    THRESHOLD_COLUMN + " REAL NOT NULL, " +
                    MATCH_TRAINING_FILE + " INTEGER NOT NULL )" ;


    // 資料庫物件
    private SQLiteDatabase db;

    // 建構子，一般的應用都不需要修改
    public MainFreqListItem(Context context) {
        db = SQLiteHandler.getDatabase(context);
    }

    // 關閉資料庫，一般的應用都不需要修改
    public void close() {
        db.close();
    }

    // 新增參數指定的物件
    public long insert(final List<HashMap.Entry<Float, Float>> topk_freqs, double threshold, long matching_training_id) {
        StringBuilder FreqStr = new StringBuilder(), ValueStr = new StringBuilder();
        for(int i = 0; i < topk_freqs.size(); i++) {
            HashMap.Entry<Float, Float> entry = topk_freqs.get(i);
            float freq = entry.getKey();
            float val = entry.getValue();

            if( i == 0 ){
                FreqStr.append(freq);
                ValueStr.append(val);
            }else{
                FreqStr.append(",").append(freq);
                ValueStr.append(",").append(val);
            }
        }

        // 建立準備新增資料的ContentValues物件
        ContentValues cv = new ContentValues();

        // 加入ContentValues物件包裝的新增資料
        // 第一個參數是欄位名稱， 第二個參數是欄位的資料
        cv.put(FREQS_COLUMN, FreqStr.toString());
        cv.put(VALUES_COLUMN, ValueStr.toString());
        cv.put(THRESHOLD_COLUMN, threshold);
        cv.put(MATCH_TRAINING_FILE, matching_training_id);


        // 新增一筆資料並取得編號
        // 第一個參數是表格名稱
        // 第二個參數是沒有指定欄位值的預設值
        // 第三個參數是包裝新增資料的ContentValues物件
        long id = db.insert(TABLE_NAME, null, cv);

        return id;
    }


    // 刪除參數指定編號的資料
    public boolean delete(long id){
        // 設定條件為編號，格式為「欄位名稱=資料」
        String where = KEY_ID + "=" + id;
        // 刪除指定編號資料並回傳刪除是否成功
        return db.delete(TABLE_NAME, where , null) > 0;
    }

    public FreqModel GetFreqModel(long matching_training_id) {
        String where = MATCH_TRAINING_FILE + "=" + matching_training_id;
        Cursor cursor = db.query(
                TABLE_NAME, null, where, null, null, null, null, null);

        // 如果有查詢結果
        FreqModel item = null;
        if (cursor.moveToFirst()) {
            // 讀取包裝一筆資料的物件
            item = getRecord(cursor);
        }

        // 關閉Cursor物件
        cursor.close();
        return item;
    }


    // 把Cursor目前的資料包裝為物件
    public FreqModel getRecord(Cursor cursor) {
        // 準備回傳結果用的物件
        FreqModel result = new FreqModel();

        result.id = cursor.getLong(0);
        String FreqStr = cursor.getString(1);
        String ValueStr = cursor.getString(2);
        result.threshold = cursor.getDouble(3);
        result.match_training_id = cursor.getLong(4);

        String[] token = FreqStr.split(",");
        result.freqs = new double[token.length];
        for(int i = 0; i < token.length; i++)
            result.freqs[i] = Double.parseDouble(token[i]);

        token = ValueStr.split(",");
        result.vals = new double[token.length];
        for(int i = 0; i < token.length; i++)
            result.vals[i] = Double.parseDouble(token[i]);

        // 回傳結果
        return result;
    }

    // 取得資料數量
    public int getCount() {
        int result = 0;
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);

        if (cursor.moveToNext())
            result = cursor.getInt(0);

        return result;
    }


    public class FreqModel{
        public long id;
        public double[] freqs;
        public double[] vals;
        public double threshold;
        public long match_training_id;
    }
}
