package mig0.bosheculogger.service;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.os.Build.VERSION;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.Log;
import mig0.bosheculogger.utils.Hex2StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

public class BluetoothCommunThread extends Thread {
    private static String LOG_TAG;
    byte[] buffer;
    private DataCallbackListener mCallbackListener;
    private ReadStatus mCurrentReadStatus;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private BluetoothSocket mSocket;
    public boolean running;
    int totalBytes;
    String totalData;

    private boolean bluetooth_has_exception = false;

    public interface DataCallbackListener {
        void onConnectionError();

        void onConnectionLost();

        void onReadObject(String str);

        void onSocketClosedException();

        void onSocketNullException();
    }

    enum ReadStatus {
        StatusIdle,
        StatusNormalBegin,
        StatusPartData
    }

    static {
        LOG_TAG = "BluetoothCommunThread";
    }

    public BluetoothCommunThread(DataCallbackListener callbackListener, BluetoothSocket socket) {
        this.buffer = new byte[AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT];
        this.totalData = "";
        this.totalBytes = 0;
        this.running = false;
        this.mCurrentReadStatus = ReadStatus.StatusIdle;
        this.mCallbackListener = callbackListener;
        this.mSocket = socket;
        try {
            this.mOutputStream = socket.getOutputStream();
            this.mInputStream = socket.getInputStream();
            this.running = true;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (this.mCallbackListener != null) {
                this.mCallbackListener.onConnectionError();
            }
        }
    }

    public void run() {
        while (isSocketConnect()) {
            int partByteSize = -1;
            try {
                if (this.mInputStream != null) {
                    partByteSize = mInputStream.read(this.buffer);
                }
                if (partByteSize > 0) {
                    byte[] buf_dataTemp = new byte[partByteSize];
                    for (int i = 0; i < partByteSize; i++) {
                        buf_dataTemp[i] = this.buffer[i];
                    }
                    String partData = "";
                    if (buf_dataTemp != null) {
                        partData = Hex2StringUtils.bytesToHexString(buf_dataTemp);
                        Log.i(LOG_TAG, "thread read = " + partData);
                    }
                    if (partData.startsWith("2A")) {
                        Log.d(LOG_TAG, "get status code 2A reset!");
                        this.mCurrentReadStatus = ReadStatus.StatusNormalBegin;
                        this.totalBytes = partByteSize;
                        this.totalData = partData;
                    } else if ("21".equals(partData)) {
                        if (this.mCurrentReadStatus == ReadStatus.StatusIdle) {
                            Log.d(LOG_TAG, "send handshake information");
                            if (this.mCallbackListener != null) {
                                this.mCallbackListener.onReadObject(partData);
                            }
                            this.totalBytes = 0;
                            this.totalData = "";
                        }
                    } else if (partData.startsWith("3D")) {
                        if (this.mCurrentReadStatus == ReadStatus.StatusIdle) {
                            Log.e(LOG_TAG, "got wrong package 3D!");
                            if (this.mCallbackListener != null) {
                                this.mCallbackListener.onReadObject(partData);
                            }
                            this.totalBytes = 0;
                            this.totalData = "";
                        }
                    } else if (!"2F".equals(partData)) {
                        Log.d(LOG_TAG, "part data");
                        this.mCurrentReadStatus = ReadStatus.StatusPartData;
                        this.totalBytes += partByteSize;
                        this.totalData += partData;
                    } else if (this.mCurrentReadStatus == ReadStatus.StatusIdle) {
                        Log.e(LOG_TAG, "got timeout packet 2F!");
                        if (this.mCallbackListener != null) {
                            this.mCallbackListener.onReadObject(partData);
                        }
                        this.totalBytes = 0;
                        this.totalData = "";
                    }
                    if (this.totalData != null && this.totalData.startsWith("2A")) {
                        if (this.totalBytes == 63 || this.totalBytes == 6 || this.totalBytes == 8) {
                            Log.d(LOG_TAG, "send normal byte");
                            this.mCurrentReadStatus = ReadStatus.StatusIdle;
                            if (this.mCallbackListener != null) {
                                this.mCallbackListener.onReadObject(this.totalData);
                            }
                            this.totalBytes = 0;
                            this.totalData = "";
                        }
                    }
                } else {
                    return;
                }
            } catch (Exception ex) {
                bluetooth_has_exception = true;
                Log.e(LOG_TAG, "Read bluetooth has exception");
                if (this.mCallbackListener != null) {
                    this.mCallbackListener.onConnectionLost();
                }
                ex.printStackTrace();
                this.running = false;
                this.mCurrentReadStatus = ReadStatus.StatusIdle;
            }
        }
        Log.i(LOG_TAG, "Lost bluetooth connection");
        if (this.mCallbackListener != null) {
            this.mCallbackListener.onConnectionLost();
        }
        this.running = false;
        Log.i(LOG_TAG, "release IO resource!");
        if (this.mInputStream != null) {
            try {
                this.mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (this.mOutputStream != null) {
            try {
                this.mOutputStream.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        if (this.mSocket != null) {
            try {
                this.mSocket.close();
            } catch (IOException e22) {
                e22.printStackTrace();
            }
        }
    }

    public void writeObject(byte[] str) {
        try {
            this.mOutputStream.write(str);
            this.mOutputStream.flush();
        } catch (IOException e) {
            this.running = false;
            e.printStackTrace();
        }
    }

    public void streamFlush() {
        if (this.mOutputStream != null) {
            try {
                this.mOutputStream.flush();
                this.running = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    @SuppressLint({"NewApi"})
    private boolean isSocketConnect() {
        int sdkLevel = VERSION.SDK_INT;
        if (sdkLevel > 13) {
            return mSocket.isConnected();
        }
        if (sdkLevel <= 6) {
            return false;
        }
        try {
            Field closedField = mSocket.getClass().getDeclaredField("mClosed");
            closedField.setAccessible(true);
            return !closedField.getBoolean(mSocket);
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
    */
    @SuppressLint("NewApi")
    private boolean isSocketConnect() {
        if(bluetooth_has_exception) return false;
        else return mSocket.isConnected();
    }

    public void reset() {
        Log.d(LOG_TAG, "reset to StatusIdle!");
        this.mCurrentReadStatus = ReadStatus.StatusIdle;
        this.totalBytes = 0;
        this.totalData = "";
    }
}
