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

import java.lang.reflect.Array;
import java.util.ArrayList;

import cc.nctu1210.api.koala6x.KoalaDevice;
import nctu.nol.bt.devices.BeaconHandler;


public class KoalaScan extends Activity{

    // Scan Related
    private BeaconHandler bh = null;
    private Button btScan;
    private ListView listkoala;
    private ArrayAdapter<String> Adapter;
    private ArrayList<String> koala= new ArrayList<String>();
    // Pass Result Related
    public static final String macAddress = "KoalaScan.MacAddress";
    public static final String deviceName = "KoalaScan.DeviceName";
    //ProgressDialog
    private ProgressDialog dialog;

    @Override
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
        Adapter = new ArrayAdapter<String>(KoalaScan.this, android.R.layout.simple_list_item_1, koala);
        listkoala.setAdapter(Adapter);

        //Button
        btScan = (Button) findViewById(R.id.bt_scan);
        btScan.setOnClickListener(KoalaScanListener);
    }
    private Button.OnClickListener KoalaScanListener;

    {
        KoalaScanListener = new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                koala.clear();
                bh.scanLeDevice();
                dialog = ProgressDialog.show(KoalaScan.this, "請稍後", "藍芽設備搜尋中", true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(bh.SCAN_PERIOD);
                            for (int i = 0, size = bh.getScanedDevices().size(); i < size; i++) {
                                KoalaDevice d = bh.getScanedDevices().get(i);
                                koala.add(d.getDevice().getName() + " " + d.getDevice().getAddress());
                            }
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Adapter.notifyDataSetChanged();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            dialog.dismiss();
                        }
                    }
                }).start();

            }
        };
    }

    private ListView.OnItemClickListener ListClickListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            String sel =arg0.getItemAtPosition(arg2).toString();
            Intent intent = new Intent();
            Bundle b = new Bundle();
            b.putString(deviceName, sel.split(" ")[0]);
            b.putString(macAddress, sel.split(" ")[1]);
            intent.putExtras(b);
            KoalaScan.this.setResult(RESULT_OK, intent);
            finish();
        }
    };

}
