package mig0.bosheculogger.activity;

import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final int CMD_HANDSHAKE_TIMEOUT = 1004;
    private static final int CMD_REQUEST_CONTROL_MESSAGE = 1002;
    private static final int CMD_REQUEST_RETURN_TIMEOUT = 1003;
    private static final int CMD_REQUEST_STATUS_MESSAGE = 1001;
    private static final int CMD_WRITE = 10005;
    private static final int HANDSHAKE_TIMEOUT = 500;
    private static String LOG_TAG = null;
    private static final int REQUSET_TIMEOUT = 200;
    /*
    private BasicFragment basicFragment;
    private DiagnoseFragment diagnoseFragment;
    private DisplayMetrics dm;
    private boolean hasHandShaked;
    private String mCmdString;
    public boolean mConnectionLost;
    private String mCurrentLanguage;
    private DeviceConnectionManager mDeviceConnectionManager;
    private AlertDialog mDialogUnsupport;
    private int mHandshakeRetry;
    private MainHandler mMainHandler;
    private MyPagerAdapter mPagerAdapter;
    private boolean mPause;
    private ConnectionBroadcastReceiver mReceiver;
    private int mRequstIndex;
    private int mRetry;
    private AnimationDrawable mRunningAnimation;
    private ViewPager mViewPager;
    private Handler mWorkHandler;
    private HandlerThread mWorkThread;
    private SeniorFragment seniorFragment;
    private PagerSlidingTabStrip tabs;
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    protected void onStop() {
        Log.i(LOG_TAG, "in onStop");
        /*
        this.hasHandShaked = false;
        */
        super.onStop();
    }

    protected void onResume() {
        Log.e(LOG_TAG, "in onResume");
        super.onResume();
        /*
        registerBroadcastReceiver();
        */
        start();
    }

    private void start() {
        /*
        this.mPause = false;
        this.mConnectionLost = false;
        if (this.mDeviceConnectionManager.isSocketLost()) {
            this.mMainHandler.sendEmptyMessage(5);
            return;
        }
        this.mDeviceConnectionManager.startCommuThread(new C01252());
        this.mDeviceConnectionManager.resetThreadFlag();
        handShake();
        */
    }

    protected void onPause() {
        Log.e(LOG_TAG, "in onPause");
        /*
        this.mPause = true;
        cleanMessage();
        unregisterBroadcastReceiver();
        Intent intent = new Intent(this, BluetoothConnectionService.class);
        intent.setAction(BluetoothConnectionService.ACTION_CANCEL);
        sendBroadcast(intent);
        if (this.mDialogUnsupport != null && this.mDialogUnsupport.isShowing()) {
            this.mDialogUnsupport.dismiss();
            this.mDialogUnsupport = null;
        }
        */
        super.onPause();
    }

}
