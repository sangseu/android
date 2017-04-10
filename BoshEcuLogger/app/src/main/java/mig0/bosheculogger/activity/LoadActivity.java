package mig0.bosheculogger.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.Locale;
import java.util.Set;

import mig0.bosheculogger.R;
import mig0.bosheculogger.service.DeviceConnectionManager;
import mig0.bosheculogger.utils.ConfigManager;

import static android.graphics.PixelFormat.RGBA_8888;

public class LoadActivity extends Activity {

    private static final int LOAD_DISPLAY_TIME = 2000;
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
        getWindow().setFormat(RGBA_8888); /*1*/
        setContentView(R.layout.activity_load);
        /* Get language for GUI */
        getLanguageSettings();
        /* Get config from *.xml file, show activity_load's image */
        ConfigManager.getInstance(this).loadConfig("diag_cfg_strings.xml");
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.load);
        /* chinese background */
        if ("zh".equals(mCurrentLanguage)) {
            relativeLayout.setBackgroundResource(R.drawable.smarthome_light_chinese);
        } else { /* eng background */
            relativeLayout.setBackgroundResource(R.drawable.smarthome_light_english);
        }

        final Handler handler = new Handler();
        /* get paired bluetooth device, run after LOAD_DISPLAY_TIME delay*/
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                preferences = LoadActivity.this.getSharedPreferences("bosch", MODE_PRIVATE); /*0*/
                String bluetoothMAC = preferences.getString("bluetoothMAC", null);
                if(adapter != null && adapter.isEnabled()) {
                    pairedDevices = adapter.getBondedDevices();
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
                    Log.d(LOG_TAG, "call SearchActivity");
                    mContext.startActivityForResult(new Intent(mContext, SearchActivity.class), 0);
                }
            }
        }, LOAD_DISPLAY_TIME);
    }

    /* Connect if had paired device before */
    private void connectDevice(BluetoothDevice device) {
        Log.d(LOG_TAG, "load connect device " + device);
        DeviceConnectionManager.getInstance(this).connectDevice(device, new DeviceConnectionManager.ConnectStatusListener() {
            public void onConnectSuccess() {
                Log.d(LOG_TAG, "load connect success");
                LoadActivity.this.gotoMain();
            }

            public void onConnectError() {
                Log.d(LOG_TAG, "load connect error");
                Intent searchIntent = new Intent(LoadActivity.this, SearchActivity.class);
                searchIntent.putExtra(SearchActivity.FLAG_CONNECTION_LOST, true);
                LoadActivity.this.mContext.startActivityForResult(searchIntent, 0);
            }
        });
    }

    private void getLanguageSettings() {
        mCurrentLanguage = PreferenceManager.getDefaultSharedPreferences(this).getString("language", Locale.getDefault().getLanguage());
    }

    public void onBackPressed() {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* requestCode for check which request is responding */
        if (resultCode == RESULT_OK) { /*-1*/
            Log.d(LOG_TAG, "load got result ok!");
            gotoMain();
        } else if (resultCode == RESULT_CANCELED) { /*0*/
            Log.d(LOG_TAG, "load got result canceled!");
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /* If pass onActivityResult, go to MainActivity*/
    private void gotoMain() {
        this.mContext.startActivity(new Intent(this.mContext, MainActivity.class));
        Log.d(LOG_TAG, "gotoMain");
        finish();
    }
}
