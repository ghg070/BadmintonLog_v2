package nctu.nol.file.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2016/7/27.
 */
public class StrokeListItem {
    // 表格名稱
    public static final String TABLE_NAME = "strokelist_item";

    // 編號表格欄位名稱，固定不變
    public static final String KEY_ID = "_id";

    // 其它表格欄位名稱
    public static final String STROKETIME_COLUMN = "stroke_time";
    public static final String STROKETYPE_COLUMN = "stroke_type";
    public static final String MATCH_TESTING_FILE = "match_testing_id";


    // 使用上面宣告的變數建立表格的SQL指令
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    STROKETIME_COLUMN + " INTEGER NOT NULL, " +
                    STROKETYPE_COLUMN + " TEXT NOT NULL, " +
                    MATCH_TESTING_FILE + " INTEGER NOT NULL )" ;


    // 資料庫物件
    private SQLiteDatabase db;

    // 建構子，一般的應用都不需要修改
    public StrokeListItem(Context context) {
        db = SQLiteHandler.getDatabase(context);
    }

    // 關閉資料庫，一般的應用都不需要修改
    public void close() {
        db.close();
    }

    // 新增參數指定的物件
    public long insert(long StrokeTime, String StrokeType, long matching_testing_id) {
        // 建立準備新增資料的ContentValues物件
        ContentValues cv = new ContentValues();

        // 加入ContentValues物件包裝的新增資料
        // 第一個參數是欄位名稱， 第二個參數是欄位的資料
        cv.put(STROKETIME_COLUMN, StrokeTime);
        cv.put(STROKETYPE_COLUMN, StrokeType);
        cv.put(MATCH_TESTING_FILE, matching_testing_id);


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

    public List<StrokeItem> GetStrokesInOneTestingFile(long match_testing_id) {
        List<StrokeItem> result = new ArrayList<>();
        String where = MATCH_TESTING_FILE + "=" + match_testing_id;
        Cursor cursor = db.query(
                TABLE_NAME, null, where, null, null, null, null, null);

        while (cursor.moveToNext())
            result.add(getRecord(cursor));

        cursor.close();
        return result;
    }


    // 把Cursor目前的資料包裝為物件
    public StrokeItem getRecord(Cursor cursor) {
        // 準備回傳結果用的物件
        StrokeItem result = new StrokeItem();

        result.id = cursor.getLong(0);
        result.stroke_time = cursor.getLong(1);
        result.stroke_type = cursor.getString(2);
        result.match_testing_id = cursor.getLong(3);

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


    public class StrokeItem{
        public long id;
        public long stroke_time;
        public String stroke_type;
        public long match_testing_id;
    }
}
