package mig0.bosheculogger.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.Toast;


import java.lang.ref.WeakReference;

import mig0.bosheculogger.service.BluetoothConnectionService;
import mig0.bosheculogger.service.DeviceConnectionManager;
import mig0.bosheculogger.utils.Command;
import mig0.bosheculogger.utils.ConfigManager;
import mig0.bosheculogger.utils.ConfigManager.Strings;

public class MainActivity extends AppCompatActivity {

    private static final int CMD_HANDSHAKE_TIMEOUT = 1004;
    private static final int CMD_REQUEST_CONTROL_MESSAGE = 1002;
    private static final int CMD_REQUEST_RETURN_TIMEOUT = 1003;
    private static final int CMD_REQUEST_STATUS_MESSAGE = 1001;

    public static final int ANIM_STYLE_CLOSE_ENTER = 3;
    public static final int MESSAGE_READ_OBJECT = 4;
    public static final int MESSAGE_CONNECT_LOST = 5;

    public static final int DIANOGE_FRAGMENT = 0;
    public static final int BASIC_FRAGMENT = 1;
    public static final int SENIOR_FRAGMENT = 2;

    private static final int CMD_WRITE = 10005;
    private static final int HANDSHAKE_TIMEOUT = 500;
    private static String LOG_TAG = null;
    private static final int REQUSET_TIMEOUT = 200;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onStop() {
        Log.i(LOG_TAG, "in onStop");
        /*
        this.hasHandShaked = false;
        */
        super.onStop();
    }

    @Override
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

    @Override
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

    static class MainHandler extends Handler {
        WeakReference<MainActivity> mActivity;

        public MainHandler(MainActivity activity) {
            this.mActivity = new WeakReference(activity);
        }

        /* handle message from handshake with ecu */
        public void handleMessage(Message msg) {
            MainActivity activity = (MainActivity) this.mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case MainActivity.ANIM_STYLE_CLOSE_ENTER:
                    case MainActivity.MESSAGE_CONNECT_LOST:
                        Log.d(MainActivity.LOG_TAG, "MainHandler case MESSAGE_CONNECT_LOST");
                        activity.stopAnimation();
                        if (!activity.mPause) {
                            activity.showMessage(ConfigManager.Strings.CONNNECT_ERROR);
                            Intent serviceIntent = new Intent(activity, BluetoothConnectionService.class);
                            serviceIntent.setAction(BluetoothConnectionService.ACTION_CONNECT);
                            activity.startService(serviceIntent);
                        }
                        clearMessageQueue();
                        activity.mConnectionLost = true;
                        return;
                    case MainActivity.MESSAGE_READ_OBJECT:
                        Log.d(MainActivity.LOG_TAG, "MainHandler case MESSAGE_READ_OBJECT");
                        String messageRead = msg.obj.toString();
                        if ("21".equals(messageRead)) {
                            Log.d(MainActivity.LOG_TAG, "1.handshake success!");
                            activity.hasHandShaked = true;
                            activity.mHandshakeRetry = 0;
                            activity.startAnimation();
                            removeMessages(MainActivity.CMD_HANDSHAKE_TIMEOUT);
                            sendEmptyMessage(MainActivity.CMD_REQUEST_CONTROL_MESSAGE);
                            return;
                        }
                        activity.handleMessageRead(messageRead);
                        return;
                    case MainActivity.CMD_REQUEST_STATUS_MESSAGE /*1001*/:
                        Log.d(MainActivity.LOG_TAG, "MainHandler case CMD_REQUEST_STATUS_MESSAGE");
                        activity.requestStatusMessage();
                        return;
                    case MainActivity.CMD_REQUEST_CONTROL_MESSAGE /*1002*/:
                        Log.d(MainActivity.LOG_TAG, "MainHandler case CMD_REQUEST_CONTROL_MESSAGE");
                        activity.requestControlMessage();
                        return;
                    case MainActivity.CMD_REQUEST_RETURN_TIMEOUT /*1003*/:
                        Log.d(MainActivity.LOG_TAG, "MainHandler case CMD_REQUEST_RETURN_TIMEOUT");
                        activity.retry(Strings.TIMEOUT);
                        return;
                    case MainActivity.CMD_HANDSHAKE_TIMEOUT /*1004*/:
                        Log.d(MainActivity.LOG_TAG, "MainHandler case CMD_HANDSHAKE_TIMEOUT");
                        activity.handshakeRetry();
                        return;
                    default:
                        return;
                }
            }
        }

        private void clearMessageQueue() {
            removeMessages(MainActivity.CMD_HANDSHAKE_TIMEOUT);
            removeMessages(MainActivity.CMD_REQUEST_CONTROL_MESSAGE);
            removeMessages(MainActivity.CMD_REQUEST_RETURN_TIMEOUT);
            removeMessages(MainActivity.CMD_REQUEST_STATUS_MESSAGE);
        }
    }

    public class MyPagerAdapter extends FragmentPagerAdapter {
        private String[] titles;

        public MyPagerAdapter(FragmentManager fm, String lan) {
            super(fm);
            updatePageTitle(lan);
        }

        public void updatePageTitle(String lan) {
            ConfigManager cManager = ConfigManager.getInstance(MainActivity.this);
            String diagnostic = cManager.getString(lan, Strings.DIAGNOSTIC);
            String basic = cManager.getString(lan, Strings.BASIC);
            String advanced = cManager.getString(lan, Strings.ADVANCED);
            this.titles = new String[]{diagnostic, basic, advanced};
        }

        public CharSequence getPageTitle(int position) {
            return this.titles[position];
        }

        public int getCount() {
            return this.titles.length;
        }

        public Fragment getItem(int position) {
            Log.e(MainActivity.LOG_TAG, "get fragment " + position);
            Bundle args;
            switch (position) {
                case DIANOGE_FRAGMENT /*0*/:
                    if (MainActivity.this.diagnoseFragment == null) {
                        MainActivity.this.diagnoseFragment = new DiagnoseFragment();
                        args = new Bundle();
                        args.putString("lan", MainActivity.this.mCurrentLanguage);
                        MainActivity.this.diagnoseFragment.setArguments(args);
                    }
                    return MainActivity.this.diagnoseFragment;
                case BASIC_FRAGMENT /*1*/:
                    if (MainActivity.this.basicFragment == null) {
                        MainActivity.this.basicFragment = new BasicFragment();
                        args = new Bundle();
                        args.putString("lan", MainActivity.this.mCurrentLanguage);
                        MainActivity.this.basicFragment.setArguments(args);
                    }
                    return MainActivity.this.basicFragment;
                case SENIOR_FRAGMENT /*2*/:
                    if (MainActivity.this.seniorFragment == null) {
                        MainActivity.this.seniorFragment = new SeniorFragment();
                        args = new Bundle();
                        args.putString("lan", MainActivity.this.mCurrentLanguage);
                        MainActivity.this.seniorFragment.setArguments(args);
                    }
                    return MainActivity.this.seniorFragment;
                default:
                    return null;
            }
        }

        public Object instantiateItem(ViewGroup container, int position) {
            Log.i(MainActivity.LOG_TAG, "adapter instantiateItem " + position);
            return super.instantiateItem(container, position);
        }

        public int getItemPosition(Object object) {
            return -2;
        }
    }

    private class ConnectionBroadcastReceiver extends BroadcastReceiver {
        private ConnectionBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothConnectionService.INTENT_CONNECTING.equals(action)) {
                ConfigManager cManager = ConfigManager.getInstance(MainActivity.this);
                Toast.makeText(MainActivity.this, cManager.getString(MainActivity.this.mCurrentLanguage, Strings.CONNECT_TO) + intent.getStringExtra("deviceName"), Toast.LENGTH_SHORT).show();
            } else if (BluetoothConnectionService.INTENT_CONNECT_SUCCESS.equals(action)) {
                MainActivity.this.start();
            } else if (BluetoothConnectionService.INTENT_CONNECT_ERROR.equals(action)) {
                Toast.makeText(MainActivity.this, ConfigManager.getInstance(MainActivity.this).getString(MainActivity.this.mCurrentLanguage, Strings.CONNECT_FAIL), Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void calCmdString()
    {
        Command localCommand = ConfigManager.getInstance(this).getReadStatusCmd();
        String str = Integer.toHexString(Integer.parseInt(localCommand.cmd, 16) + Integer.parseInt(localCommand.length, 16) + Integer.parseInt(localCommand.id, 16));
        str = str.substring(str.length() - 2);
        this.mCmdString = (localCommand.cmd + localCommand.length + localCommand.id + str);
    }


}
