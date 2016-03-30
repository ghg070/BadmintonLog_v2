package nctu.nol.badmintonlogprogram;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import nctu.nol.bt.devices.BeaconHandler;

public class KoalaScan extends Activity {

    // Scan Related
    private BeaconHandler bh = null;
    private Button btScan;
    private ListView listkoala;

    // Pass Result Related
    public static final String macAddress = "KoalaScan.MacAddress";


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.koalascan);

        bh = new BeaconHandler(KoalaScan.this);

        initialViewandEvent();
    }
    private void initialViewandEvent(){
        //ListView
        listkoala = (ListView) findViewById(R.id.list_device);

        //Button
        btScan = (Button) findViewById(R.id.bt_scan);
        btScan.setOnClickListener(KoalaScanListener);
    }
    private Button.OnClickListener KoalaScanListener = new Button.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            bh.scanLeDevice();
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
