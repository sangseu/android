package com.bosch.diag.service;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.Build.VERSION;
import android.util.Log;
import com.bosch.diag.service.BluetoothCommunThread.DataCallbackListener;
import java.io.IOException;
import java.lang.reflect.Field;

public class DeviceConnectionManager {
    private static DeviceConnectionManager sInstance;
    private String LOG_TAG;
    private BluetoothCommunThread mBluetoothCommunThread;
    private ConnectStatusListener mConnectStatusListener;
    private BluetoothSocket mConnectionSocket;
    private Context mContext;
    private BluetoothDevice mCurrentDevice;

    /* renamed from: com.bosch.diag.service.DeviceConnectionManager.1 */
    class C01011 extends Thread {
        private final /* synthetic */ BluetoothDevice val$device;

        C01011(BluetoothDevice bluetoothDevice) {
            this.val$device = bluetoothDevice;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            /*
            r9 = this;
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = r4.LOG_TAG;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r5 = "try to create new device socket";
            android.util.Log.d(r4, r5);	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = r9.val$device;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = r4.getClass();	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r5 = "createRfcommSocket";
            r6 = 1;
            r6 = new java.lang.Class[r6];	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r7 = 0;
            r8 = java.lang.Integer.TYPE;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r6[r7] = r8;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r2 = r4.getMethod(r5, r6);	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = r9.val$device;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r5 = 1;
            r5 = new java.lang.Object[r5];	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r6 = 0;
            r7 = 1;
            r7 = java.lang.Integer.valueOf(r7);	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r5[r6] = r7;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r3 = r2.invoke(r4, r5);	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r3 = (android.bluetooth.BluetoothSocket) r3;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r3.connect();	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = r4.mConnectionSocket;	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            if (r4 == 0) goto L_0x0075;
        L_0x003d:
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = r4.isSocketConnect();	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            if (r4 == 0) goto L_0x0075;
        L_0x0045:
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = r4.LOG_TAG;	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r5 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r6 = "close preview device ";
            r5.<init>(r6);	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r6 = com.bosch.diag.service.DeviceConnectionManager.this;	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r6 = r6.mCurrentDevice;	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r5 = r5.append(r6);	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r5 = r5.toString();	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            android.util.Log.i(r4, r5);	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = r4.mBluetoothCommunThread;	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4.streamFlush();	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = r4.mConnectionSocket;	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4.close();	 Catch:{ IOException -> 0x00a4, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
        L_0x0075:
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4.mConnectionSocket = r3;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r5 = r9.val$device;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4.mCurrentDevice = r5;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r5 = 0;
            r4.saveBluetooth(r5);	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = r4.mConnectStatusListener;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            if (r4 == 0) goto L_0x00a3;
        L_0x008f:
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = r4.LOG_TAG;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r5 = "create socket successful";
            android.util.Log.d(r4, r5);	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4 = r4.mConnectStatusListener;	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            r4.onConnectSuccess();	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
        L_0x00a3:
            return;
        L_0x00a4:
            r0 = move-exception;
            r0.printStackTrace();	 Catch:{ IOException -> 0x00a9, NoSuchMethodException -> 0x00bf, IllegalAccessException -> 0x00c4, IllegalArgumentException -> 0x00c9, InvocationTargetException -> 0x00ce }
            goto L_0x0075;
        L_0x00a9:
            r0 = move-exception;
            r0.printStackTrace();
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;
            r4 = r4.mConnectStatusListener;
            if (r4 == 0) goto L_0x00a3;
        L_0x00b5:
            r4 = com.bosch.diag.service.DeviceConnectionManager.this;
            r4 = r4.mConnectStatusListener;
            r4.onConnectError();
            goto L_0x00a3;
        L_0x00bf:
            r1 = move-exception;
            r1.printStackTrace();
            goto L_0x00a3;
        L_0x00c4:
            r1 = move-exception;
            r1.printStackTrace();
            goto L_0x00a3;
        L_0x00c9:
            r1 = move-exception;
            r1.printStackTrace();
            goto L_0x00a3;
        L_0x00ce:
            r1 = move-exception;
            r1.printStackTrace();
            goto L_0x00a3;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.bosch.diag.service.DeviceConnectionManager.1.run():void");
        }
    }

    public interface ConnectStatusListener {
        void onConnectError();

        void onConnectSuccess();
    }

    private DeviceConnectionManager(Context context) {
        this.LOG_TAG = getClass().getSimpleName();
        this.mContext = context;
    }

    public static synchronized DeviceConnectionManager getInstance(Context context) {
        DeviceConnectionManager deviceConnectionManager;
        synchronized (DeviceConnectionManager.class) {
            if (sInstance == null) {
                sInstance = new DeviceConnectionManager(context);
            }
            deviceConnectionManager = sInstance;
        }
        return deviceConnectionManager;
    }

    private void createSocket(BluetoothDevice device) {
        if (device != null) {
            new C01011(device).start();
        }
    }

    public void startCommuThread(DataCallbackListener dataCallbackListener) {
        boolean needStart = false;
        if (this.mBluetoothCommunThread == null) {
            needStart = true;
        } else if (!this.mBluetoothCommunThread.running) {
            needStart = true;
        }
        if (needStart) {
            if (this.mConnectionSocket == null) {
                Log.e(this.LOG_TAG, "mConnectionSocket = null");
                if (dataCallbackListener != null) {
                    dataCallbackListener.onSocketNullException();
                }
            } else if (isSocketConnect()) {
                this.mBluetoothCommunThread = new BluetoothCommunThread(dataCallbackListener, this.mConnectionSocket);
                this.mBluetoothCommunThread.start();
            } else if (dataCallbackListener != null) {
                dataCallbackListener.onSocketClosedException();
            }
            Log.e(this.LOG_TAG, "start thread !");
        }
    }

    public void connectDevice(BluetoothDevice device, ConnectStatusListener connectStatusListener) {
        this.mConnectStatusListener = connectStatusListener;
        connectDevice(device);
    }

    public void connectDevice(BluetoothDevice device) {
        Log.d(this.LOG_TAG, "current device = " + this.mCurrentDevice);
        Log.d(this.LOG_TAG, "set device = " + device);
        if (this.mConnectionSocket != null) {
            Log.d(this.LOG_TAG, "current socket connection = " + isSocketConnect());
        }
        boolean newDevice = false;
        if (this.mCurrentDevice == null) {
            newDevice = true;
        } else if (!this.mCurrentDevice.getAddress().equals(device.getAddress())) {
            newDevice = true;
        } else if (!(this.mCurrentDevice.getBondState() == 12 && isSocketConnect())) {
            newDevice = true;
        }
        if (newDevice) {
            createSocket(device);
        } else if (this.mConnectStatusListener != null) {
            Log.d(this.LOG_TAG, "alreay connect !");
            this.mConnectStatusListener.onConnectSuccess();
        }
    }

    private void saveBluetooth(boolean clear) {
        Editor editor = this.mContext.getSharedPreferences("bosch", 0).edit();
        if (clear) {
            editor.putString("bluetoothNAME", "");
            editor.putString("bluetoothMAC", "");
            return;
        }
        editor.putString("bluetoothNAME", this.mCurrentDevice.getName());
        editor.putString("bluetoothMAC", this.mCurrentDevice.getAddress());
        editor.commit();
    }

    public void writeObject(byte[] str) {
        if (this.mBluetoothCommunThread != null && this.mBluetoothCommunThread.running) {
            this.mBluetoothCommunThread.writeObject(str);
        }
    }

    public void clear() {
        if (this.mConnectionSocket != null) {
            try {
                this.mConnectionSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.mCurrentDevice = null;
        this.mBluetoothCommunThread = null;
        sInstance = null;
    }

    @SuppressLint({"NewApi"})
    private boolean isSocketConnect() {
        int sdkLevel = VERSION.SDK_INT;
        if (sdkLevel > 13) {
            return this.mConnectionSocket.isConnected();
        }
        if (sdkLevel <= 6) {
            return false;
        }
        try {
            boolean connect;
            Field closedField = this.mConnectionSocket.getClass().getDeclaredField("mClosed");
            closedField.setAccessible(true);
            if (closedField.getBoolean(this.mConnectionSocket)) {
                connect = false;
            } else {
                connect = true;
            }
            Log.e(this.LOG_TAG, "low level bt conncet : " + connect);
            return connect;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
            return false;
        } catch (IllegalArgumentException e3) {
            e3.printStackTrace();
            return false;
        }
    }

    public boolean isSocketLost() {
        return this.mConnectionSocket == null || !isSocketConnect();
    }

    public void resetThreadFlag() {
        if (this.mBluetoothCommunThread != null) {
            this.mBluetoothCommunThread.reset();
        }
    }
}
