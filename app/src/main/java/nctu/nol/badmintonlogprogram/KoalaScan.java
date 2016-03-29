package nctu.nol.badmintonlogprogram;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import nctu.nol.bt.devices.BeaconHandler;

public class KoalaScan extends Activity {

    private Button btScan;
    private ListView listkoala;

    private BeaconHandler bh = null;


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

}
