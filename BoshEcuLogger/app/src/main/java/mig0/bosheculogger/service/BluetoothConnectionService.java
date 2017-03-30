package mig0.bosheculogger.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import mig0.bosheculogger.service.DeviceConnectionManager.ConnectStatusListener;
import java.lang.ref.WeakReference;
import java.util.Set;

public class BluetoothConnectionService extends Service {
    public static final String ACTION_CANCEL = "com.bosch.diag.ACTION_CANCEL";
    public static final String ACTION_CONNECT = "com.bosch.diag.ACTION_CONNECT";
    private static final int CMD_CONNECT = 1;
    public static final String INTENT_CONNECTING = "com.bosch.diag.CONNECTING";
    public static final String INTENT_CONNECT_ERROR = "com.bosch.diag.CONNECT_ERROR";
    public static final String INTENT_CONNECT_SUCCESS = "com.bosch.diag.CONNECT_SUCCESS";
    private static final String TAG = "BluetoothConnectionService";
    boolean mCanceled;
    private Handler mHandler;

    static class ServiceHandler extends Handler {
        WeakReference<BluetoothConnectionService> mService;

        public ServiceHandler(BluetoothConnectionService service) {
            this.mService = new WeakReference(service);
        }

        public void handleMessage(Message msg) {
            if (msg.what == BluetoothConnectionService.CMD_CONNECT) {
                BluetoothConnectionService service = (BluetoothConnectionService) this.mService.get();
                Log.d(BluetoothConnectionService.TAG, "service canceled " + service.mCanceled);
                if (service == null || service.mCanceled) {
                    Log.e(BluetoothConnectionService.TAG, "service instanse null or canceled");
                } else {
                    service.tryToConnectHistoryDevice();
                }
            }
            super.handleMessage(msg);
        }
    }

    /* renamed from: com.bosch.diag.service.BluetoothConnectionService.1 */
    class C01271 implements ConnectStatusListener {
        C01271() {
        }

        public void onConnectSuccess() {
            Log.d(BluetoothConnectionService.TAG, "onConnectSuccess!");
            if (!BluetoothConnectionService.this.mCanceled) {
                BluetoothConnectionService.this.breadcastConnectSuccess();
            }
        }

        public void onConnectError() {
            Log.d(BluetoothConnectionService.TAG, "onConnectError!");
            if (!BluetoothConnectionService.this.mCanceled) {
                BluetoothConnectionService.this.boradcastConnectError();
                BluetoothConnectionService.this.mHandler.sendEmptyMessageDelayed(BluetoothConnectionService.CMD_CONNECT, 1000);
            }
        }
    }

    public BluetoothConnectionService() {
        this.mCanceled = false;
    }

    private void tryToConnectHistoryDevice() {
        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        String bluetoothMAC = getSharedPreferences("bosch", 0).getString("bluetoothMAC", null);
        BluetoothDevice deviceToConnect = null;
        for (BluetoothDevice bluetoothDevice : pairedDevices) {
            if (bluetoothDevice.getAddress().equals(bluetoothMAC)) {
                deviceToConnect = bluetoothDevice;
                break;
            }
        }
        if (deviceToConnect != null) {
            connectDecvie(deviceToConnect);
        }
    }

    private void connectDecvie(BluetoothDevice device) {
        Log.d(TAG, "try to connect device " + device);
        broadcastConnecting(device);
        DeviceConnectionManager.getInstance(this).connectDevice(device, new C01271());
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

    private void boradcastConnectError() {
        sendBroadcast(new Intent(INTENT_CONNECT_ERROR));
    }

    private void breadcastConnectSuccess() {
        sendBroadcast(new Intent(INTENT_CONNECT_SUCCESS));
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        Log.i(TAG, "Service onCreate");
        this.mHandler = new ServiceHandler(this);
        super.onCreate();
    }

    public void onDestroy() {
        Log.i(TAG, "Service onDestroy");
        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.d(TAG, "service got action : " + action);
        if (ACTION_CONNECT.equals(action)) {
            this.mHandler.sendEmptyMessage(CMD_CONNECT);
            this.mCanceled = false;
        } else if (ACTION_CANCEL.equals(action)) {
            this.mHandler.removeMessages(CMD_CONNECT);
            stopSelf();
            this.mCanceled = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
