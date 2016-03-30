package nctu.nol.badmintonlogprogram;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import nctu.nol.bt.devices.BeaconHandler;

public class KoalaScan extends Activity{

    // Scan Related
    private BeaconHandler bh = null;
    private Button btScan;
    private ListView listkoala;
    private ArrayAdapter adapter;
    // Pass Result Related
    public static final String macAddress = "KoalaScan.MacAddress";
    //ProgressDialog
    private ProgressDialog dialog;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.koalascan);

        bh = new BeaconHandler(KoalaScan.this);

        initialViewandEvent();
    }
    private void initialViewandEvent(){
        //ListView
        listkoala = (ListView) findViewById(R.id.list_device);
        listkoala.setOnItemClickListener(ListClickListener);

        //Button
        btScan = (Button) findViewById(R.id.bt_scan);
        btScan.setOnClickListener(KoalaScanListener);
    }
    private Button.OnClickListener KoalaScanListener = new Button.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            bh.scanLeDevice();
            dialog = ProgressDialog.show(KoalaScan.this, "讀取中", "請等待", true);
            new Thread(new Runnable(){
                @Override
                public void run() {
                    try{
                        // DO something
                        Thread.sleep(3000);
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                    finally{
                        dialog.dismiss();
                    }
                }
            }).start();
        }
    };
    private ListView.OnItemClickListener ListClickListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            String sel =arg0.getItemAtPosition(arg2).toString();
            Log.i("LAG",sel);
            /*Intent intent = new Intent();
            Bundle b = new Bundle();
            b.putString(macAddress, "Koala Result");
            intent.putExtras(b);
            this.setResult(RESULT_OK, intent);
            finish();*/
        }
    };


    /*
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        Bundle b = new Bundle();
        b.putString(macAddress, "Koala Result");
        intent.putExtras(b);
        this.setResult(RESULT_OK, intent);

        finish();
    }*/

}
