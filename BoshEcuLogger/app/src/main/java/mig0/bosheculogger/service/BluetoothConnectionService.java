package mig0.bosheculogger.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.BaseExpandableListAdapter;

import mig0.bosheculogger.activity.BaseActivity;
import mig0.bosheculogger.activity.MainActivity;
import mig0.bosheculogger.service.DeviceConnectionManager.ConnectStatusListener;
import java.lang.ref.WeakReference;
import java.util.Set;

public class BluetoothConnectionService extends Service {
    /* !!! user's define*/
    public static final String ACTION_CANCEL = "mig0.bosheculogger.ACTION_CANCEL";
    public static final String ACTION_CONNECT = "mig0.bosheculogger.ACTION_CONNECT";
    private static final int CMD_CONNECT = 1;
    public static final String INTENT_CONNECTING = "mig0.bosheculogger.CONNECTING";
    public static final String INTENT_CONNECT_ERROR = "mig0.bosheculogger.CONNECT_ERROR";
    public static final String INTENT_CONNECT_SUCCESS = "mig0.bosheculogger.CONNECT_SUCCESS";
    private static final String TAG = "BLTconnectionSerivce";
    /* !!! user's define*/
    boolean mCanceled;
    private Handler mHandler;

    //private int tryReconnect = 10; /* tryReconnect = 10 at first run */

    private class ServiceHandler extends Handler {
        WeakReference<BluetoothConnectionService> mService;

        ServiceHandler(BluetoothConnectionService service) {
            this.mService = new WeakReference(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == BluetoothConnectionService.CMD_CONNECT) {
                BluetoothConnectionService service = mService.get();
                Log.d(TAG, "service canceled " + service.mCanceled);
                if (service == null || service.mCanceled) {
                    Log.e(TAG, "service instanse null or canceled");
                } else {
                    service.tryToConnectHistoryDevice();
                }
            }
            super.handleMessage(msg);
        }
    }

    public BluetoothConnectionService() {
        this.mCanceled = false;
    }

    private void tryToConnectHistoryDevice() {
        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        String bluetoothMAC = getSharedPreferences("bosch", MODE_PRIVATE).getString("bluetoothMAC", null);
        BluetoothDevice deviceToConnect = null;
        for (BluetoothDevice bluetoothDevice : pairedDevices) {
            if (bluetoothDevice.getAddress().equals(bluetoothMAC)) {
                deviceToConnect = bluetoothDevice;
                break;
            }
        }
        if (deviceToConnect != null) {
            connectDevice(deviceToConnect);
        }
    }

    private void connectDevice(BluetoothDevice device) {
        Log.d(TAG, "try to connect device " + device);
        broadcastConnecting(device);
        DeviceConnectionManager.getInstance(this).connectDevice(device, new ConnectStatusListener() {
            public void onConnectSuccess() {
                Log.d(TAG, "onConnectSuccess!");
                if (!mCanceled) {
                    broadcastConnectSuccess();
                    //-----
                    mHandler.removeMessages(CMD_CONNECT);
                    stopSelf();
                    mCanceled = true;
                    //-----
                    Log.d(TAG, "broadcastConnectSuccess()");
                }
            }

            public void onConnectError() {
                Log.d(TAG, "onConnectError!");
                if (!mCanceled) {
                    broadcastConnectError();
                    Log.d(TAG, "broadcastConnectError()");
                    mHandler.sendEmptyMessageDelayed(CMD_CONNECT, 1000);
                }
            }
        });
    }

    private void broadcastConnecting(BluetoothDevice device) {
        String deviceName = null;
        if (device != null) {
            deviceName = device.getName();
        }
        Intent intent = new Intent(INTENT_CONNECTING);
        intent.putExtra("deviceName", deviceName);
        sendBroadcast(intent);
    }

    /* send mess to MainActivity */
    private void broadcastConnectError() {
        sendBroadcast(new Intent(INTENT_CONNECT_ERROR));
    }

    private void broadcastConnectSuccess() {
        sendBroadcast(new Intent(INTENT_CONNECT_SUCCESS));
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Service onCreate");
        this.mHandler = new ServiceHandler(this);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service onDestroy");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.d(TAG, "service got action : " + action);
        if (ACTION_CONNECT.equals(action)) {
            mHandler.sendEmptyMessage(CMD_CONNECT);
            mCanceled = false;
        } else if (ACTION_CANCEL.equals(action)) {
            mHandler.removeMessages(CMD_CONNECT);
            stopSelf();
            mCanceled = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
