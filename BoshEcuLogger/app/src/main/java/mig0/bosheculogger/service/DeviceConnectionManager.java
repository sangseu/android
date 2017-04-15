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
import java.lang.reflect.Method;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.content.Context.MODE_PRIVATE;

public class DeviceConnectionManager {
    private static DeviceConnectionManager sInstance;
    private String LOG_TAG = "devicecnnManager";

    private BluetoothCommunThread mBluetoothCommunThread;
    private createSocket mcreateSocket;

    private ConnectStatusListener mConnectStatusListener;
    private BluetoothSocket mConnectionSocket;
    private Context mContext;
    private BluetoothDevice mCurrentDevice;

    private boolean runThread = true;

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
            //new createSocket(device).start();
            mcreateSocket = new createSocket(device);
            mcreateSocket.start();
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
                mBluetoothCommunThread = new BluetoothCommunThread(dataCallbackListener, this.mConnectionSocket);
                mBluetoothCommunThread.start();
                Log.e(this.LOG_TAG, "start thread !");
            } else if (dataCallbackListener != null) {
                dataCallbackListener.onSocketClosedException();
            }
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
        } else if (!(this.mCurrentDevice.getBondState() == BOND_BONDED && isSocketConnect())) {
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
        Editor editor = this.mContext.getSharedPreferences("bosch", MODE_PRIVATE).edit();
        if (clear) {
            editor.putString("bluetoothNAME", "");
            editor.putString("bluetoothMAC", "");
            return;
        }
        editor.putString("bluetoothNAME", mCurrentDevice.getName());
        editor.putString("bluetoothMAC", mCurrentDevice.getAddress());
        editor.apply();
    }

    public void writeObject(byte[] str) {
        if (mBluetoothCommunThread != null && mBluetoothCommunThread.running) {
            mBluetoothCommunThread.writeObject(str);
        }
    }

    public void clear() {
        runThread = false;
        if (mConnectionSocket != null) {
            try {
                mConnectionSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCurrentDevice = null;
            mBluetoothCommunThread = null;
            mcreateSocket.cancel();
            mcreateSocket = null;

            sInstance = null;
            Log.d(LOG_TAG, "CLEAR");
        }
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

        /*
        //fail
        if(bluetooth_has_exception) return false;
        else return mConnectionSocket.isConnected();
        */
    }

    public boolean isSocketLost() {
        return this.mConnectionSocket == null || !isSocketConnect();
    }

    public void resetThreadFlag() {
        if (this.mBluetoothCommunThread != null) {
            this.mBluetoothCommunThread.reset();
        }
    }

    private class createSocket extends Thread {

        private BluetoothDevice mDevice;
        private BluetoothSocket mSocket;

        createSocket(BluetoothDevice device) {
            if(device == null) {
                return;
            }
            mDevice = device;
            BluetoothSocket tmp = null;
            try{
                Method m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                tmp = (BluetoothSocket)m.invoke(device, 1);
            }catch (SecurityException e) {
                Log.e(LOG_TAG, "create() failed", e);
            } catch (NoSuchMethodException e) {
                Log.e(LOG_TAG, "create() failed", e);
            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "create() failed", e);
            } catch (IllegalAccessException e) {
                Log.e(LOG_TAG, "create() failed", e);
            } catch (InvocationTargetException e) {
                Log.e(LOG_TAG, "create() failed", e);
            }
            mSocket = tmp;
        }

        @Override
        public void run() {
            super.run();
            Log.d(LOG_TAG, "runThread = " + runThread);
            //if(!isSocketLost()) {
                try {
                    mSocket.connect();
                    try {
                        if ((mConnectionSocket != null) && isSocketConnect()) {
                            Log.i(LOG_TAG, "close preview device " + DeviceConnectionManager.this.mCurrentDevice);
                            mBluetoothCommunThread.streamFlush();
                            mConnectionSocket.close();
                        }
                        mConnectionSocket = mSocket;
                        mCurrentDevice = mDevice;
                        saveBluetooth(false);
                        if (mConnectStatusListener != null) {
                            Log.d(LOG_TAG, "create socket successful");
                            mConnectStatusListener.onConnectSuccess();
                        }
                    } catch (IOException e) {
                        Log.i(LOG_TAG, "Connect unsuccess.", e);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (mConnectStatusListener != null) {
                        mConnectStatusListener.onConnectError();
                        Log.d(LOG_TAG, "Unable to connect");
                        return;
                    }
                /*
                try {
                    mSocket.close();
                    Log.d(LOG_TAG, "Socket closed !");
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                */
                }
            //}
        }

        void cancel() {
            try {
                mSocket.close();
                Log.d(LOG_TAG, "Socket closed !");
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }

    }
}
