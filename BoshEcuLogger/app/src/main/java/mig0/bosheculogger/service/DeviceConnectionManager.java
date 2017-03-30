package mig0.bosheculogger.service;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.Build.VERSION;
import android.util.Log;
import mig0.bosheculogger.service.BluetoothCommunThread.DataCallbackListener;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class DeviceConnectionManager {
    private static DeviceConnectionManager sInstance;
    private String LOG_TAG = getClass().getSimpleName();
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

        public void run() {
            /*
            try
            {
                Log.d(DeviceConnectionManager.this.LOG_TAG, "try to create new device socket");
                BluetoothSocket localBluetoothSocket = (BluetoothSocket)paramBluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] { Integer.TYPE }).invoke(paramBluetoothDevice, new Object[] { Integer.valueOf(1) });
                localBluetoothSocket.connect();
                try
                {
                    if ((DeviceConnectionManager.this.mConnectionSocket != null) && (DeviceConnectionManager.this.isSocketConnect()))
                    {
                        Log.i(DeviceConnectionManager.this.LOG_TAG, "close preview device " + DeviceConnectionManager.this.mCurrentDevice);
                        DeviceConnectionManager.this.mBluetoothCommunThread.streamFlush();
                        DeviceConnectionManager.this.mConnectionSocket.close();
                    }
                    DeviceConnectionManager.this.mConnectionSocket = localBluetoothSocket;
                    DeviceConnectionManager.this.mCurrentDevice = paramBluetoothDevice;
                    DeviceConnectionManager.this.saveBluetooth(false);
                    if (DeviceConnectionManager.this.mConnectStatusListener != null)
                    {
                        Log.d(DeviceConnectionManager.this.LOG_TAG, "create socket successful");
                        DeviceConnectionManager.this.mConnectStatusListener.onConnectSuccess();
                        return;
                    }
                }
                catch (IOException localIOException2)
                {
                    for (;;)
                    {
                        localIOException2.printStackTrace();
                    }
                }
                return;
            }
            catch (IOException localIOException1)
            {
                localIOException1.printStackTrace();
                if (DeviceConnectionManager.this.mConnectStatusListener != null)
                {
                    DeviceConnectionManager.this.mConnectStatusListener.onConnectError();
                    return;
                }
            }
            catch (NoSuchMethodException localNoSuchMethodException)
            {
                localNoSuchMethodException.printStackTrace();
                return;
            }
            catch (IllegalAccessException localIllegalAccessException)
            {
                localIllegalAccessException.printStackTrace();
                return;
            }
            catch (IllegalArgumentException localIllegalArgumentException)
            {
                localIllegalArgumentException.printStackTrace();
                return;
            }
            catch (InvocationTargetException localInvocationTargetException)
            {
                localInvocationTargetException.printStackTrace();
            }
            */
        }
    }

    public interface ConnectStatusListener {
        void onConnectError();

        void onConnectSuccess();
    }

    private DeviceConnectionManager(Context context) {
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
