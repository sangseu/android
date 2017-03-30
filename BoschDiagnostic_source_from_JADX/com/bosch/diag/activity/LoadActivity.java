package com.bosch.diag.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RelativeLayout;
import com.bosch.diag.service.DeviceConnectionManager;
import com.bosch.diag.service.DeviceConnectionManager.ConnectStatusListener;
import com.bosch.diag.utils.ConfigManager;
import com.eScooterDiagTool.C0102R;
import java.util.Locale;
import java.util.Set;

public class LoadActivity extends Activity {
    private static final int LOAD_DISPLAY_TIME = 3000;
    protected String LOG_TAG;
    private Activity mContext;
    protected String mCurrentLanguage;
    private Set<BluetoothDevice> pairedDevices;
    public SharedPreferences preferences;

    /* renamed from: com.bosch.diag.activity.LoadActivity.1 */
    class C00821 implements Runnable {
        C00821() {
        }

        public void run() {
            LoadActivity.this.preferences = LoadActivity.this.getSharedPreferences("bosch", 0);
            String bluetoothMAC = LoadActivity.this.preferences.getString("bluetoothMAC", null);
            LoadActivity.this.pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            if (bluetoothMAC == null || LoadActivity.this.pairedDevices.size() <= 0) {
                LoadActivity.this.mContext.startActivityForResult(new Intent(LoadActivity.this.mContext, SearchActivity.class), 0);
                return;
            }
            boolean paired = false;
            for (BluetoothDevice bluetoothDevice : LoadActivity.this.pairedDevices) {
                if (bluetoothDevice.getAddress().equals(bluetoothMAC)) {
                    LoadActivity.this.connectDevice(bluetoothDevice);
                    paired = true;
                    break;
                }
            }
            if (!paired) {
                LoadActivity.this.mContext.startActivityForResult(new Intent(LoadActivity.this.mContext, SearchActivity.class), 0);
            }
        }
    }

    /* renamed from: com.bosch.diag.activity.LoadActivity.2 */
    class C01242 implements ConnectStatusListener {
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

    public LoadActivity() {
        this.LOG_TAG = getClass().getSimpleName();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = this;
        getWindow().setFormat(1);
        setContentView(C0102R.layout.load);
        getLanguageSettings();
        ConfigManager.getInstance(this).loadConfig("diag_cfg_strings.xml");
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(C0102R.id.load);
        if ("zh".equals(this.mCurrentLanguage)) {
            relativeLayout.setBackgroundResource(C0102R.drawable.smarthome_light_chinese);
        } else {
            relativeLayout.setBackgroundResource(C0102R.drawable.smarthome_light_english);
        }
        new Handler().postDelayed(new C00821(), 3000);
    }

    private void getLanguageSettings() {
        this.mCurrentLanguage = PreferenceManager.getDefaultSharedPreferences(this).getString("language", Locale.getDefault().getLanguage());
    }

    private void connectDevice(BluetoothDevice device) {
        Log.d(this.LOG_TAG, "load connect device " + device);
        DeviceConnectionManager.getInstance(this).connectDevice(device, new C01242());
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

    private void gotoMain() {
        this.mContext.startActivity(new Intent(this.mContext, MainActivity.class));
        finish();
    }
}
