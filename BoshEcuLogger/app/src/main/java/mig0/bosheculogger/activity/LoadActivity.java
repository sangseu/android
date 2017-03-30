package mig0.bosheculogger.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;

import java.util.Locale;
import java.util.Set;

import mig0.bosheculogger.R;
import mig0.bosheculogger.service.DeviceConnectionManager;
import mig0.bosheculogger.utils.ConfigManager;

import static android.graphics.PixelFormat.RGBA_8888;

public class LoadActivity extends Activity {

    private static final int LOAD_DISPLAY_TIME = 3000;
    protected String LOG_TAG = "loadactivity";
    private Activity mContext;
    protected String mCurrentLanguage;
    public SharedPreferences preferences;
    private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    private Set<BluetoothDevice> pairedDevices;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = this;
        getWindow().setFormat(RGBA_8888);
        setContentView(R.layout.activity_load);
        // Get language for GUI
        getLanguageSettings();
        // Get config from *.xml file, show activity_load's image
        ConfigManager.getInstance(this).loadConfig("diag_cfg_strings.xml");
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.load);
        if ("zh".equals(this.mCurrentLanguage)) {
            relativeLayout.setBackgroundResource(R.drawable.smarthome_light_chinese);
        } else {
            relativeLayout.setBackgroundResource(R.drawable.smarthome_light_english);
        }

        final Handler handler = new Handler();
        /* get paired bluetooth device*/
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                preferences = LoadActivity.this.getSharedPreferences("bosch", MODE_PRIVATE);
                String bluetoothMAC = preferences.getString("bluetoothMAC", null);
                if(adapter != null && adapter.isEnabled()) {
                    pairedDevices = adapter.getDefaultAdapter().getBondedDevices();
                }
                if (bluetoothMAC == null || pairedDevices.size() <= 0) {
                    Intent intent = new Intent(mContext, SearchActivity.class);
                    startActivityForResult(intent, 0);
                    return;
                }
                boolean paired = false;
                for (BluetoothDevice bluetoothDevice : pairedDevices) {
                    if (bluetoothDevice.getAddress().equals(bluetoothMAC)) {
                        connectDevice(bluetoothDevice);
                        paired = true;
                        break;
                    }
                }
                if (!paired) {
                    LoadActivity.this.mContext.startActivityForResult(new Intent(LoadActivity.this.mContext, SearchActivity.class), 0);
                }
            }
        }, LOAD_DISPLAY_TIME);
    }

    private void getLanguageSettings() {
        this.mCurrentLanguage = PreferenceManager.getDefaultSharedPreferences(this).getString("language", Locale.getDefault().getLanguage());
    }

    public void onBackPressed() {
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == -1) {
            Log.d(this.LOG_TAG, "load got result ok!");
            gotoMain();
        } else if (resultCode == 0) {
            Log.d(this.LOG_TAG, "load got result canceled!");
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*If pass onActivityResult, go to MainActivity*/
    private void gotoMain() {
        //this.mContext.startActivity(new Intent(this.mContext, MainActivity.class));
        Log.d(this.LOG_TAG, "gotoMain");
        finish();
    }

    /*Connect if had paired device before*/
    private void connectDevice(BluetoothDevice device) {
        Log.d(this.LOG_TAG, "load connect device " + device);
        DeviceConnectionManager.getInstance(this).connectDevice(device, new C01242());
    }

    /*Class call SearchActivity and Listen connect status*/
    //!!!!!!!!!!!!!!!!!! need check DeviceConnectionManager.ConnectStatusListener
    class C01242 implements DeviceConnectionManager.ConnectStatusListener {
        C01242() {
        }

        public void onConnectSuccess() {
            Log.d(LoadActivity.this.LOG_TAG, "load connect success");
            LoadActivity.this.gotoMain();
        }

        public void onConnectError() {
            Log.d(LoadActivity.this.LOG_TAG, "load connect error");
            Intent searchIntent = new Intent(LoadActivity.this, SearchActivity.class);
            searchIntent.putExtra(SearchActivity.FLAG_CONNECTION_LOST, true);
            LoadActivity.this.mContext.startActivityForResult(searchIntent, 0);
        }
    }
}
