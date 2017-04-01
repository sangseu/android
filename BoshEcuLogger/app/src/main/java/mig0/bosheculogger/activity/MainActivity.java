package mig0.bosheculogger.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
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
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Toast;


import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mig0.bosheculogger.R;
import mig0.bosheculogger.service.BluetoothConnectionService;
import mig0.bosheculogger.service.DeviceConnectionManager;
import mig0.bosheculogger.utils.BluetoothTools;
import mig0.bosheculogger.utils.Command;
import mig0.bosheculogger.utils.ConfigManager;
import mig0.bosheculogger.utils.ConfigManager.Strings;
import mig0.bosheculogger.utils.Hex2StringUtils;
import mig0.viewpager.PagerSlidingTabStrip;

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

        Log.e(LOG_TAG, "Main onCreate");
        setContentView((int) R.layout.activity_main);
        getLanguageSettings();
        updateTitle(this.mCurrentLanguage);
        if (Build.VERSION.SDK_INT <= 13) {
            getSupportActionBar().setIcon((int) R.drawable.logo1);
        }
        this.mMainHandler = new MainHandler(this);
        this.mWorkThread = new HandlerThread("work");
        this.mWorkThread.start();
        this.mWorkHandler = new Handler(this.mWorkThread.getLooper()) {
            public void handleMessage(Message msg) {
                if (msg.what == MainActivity.CMD_WRITE) {
                    Bundle data = msg.getData();
                    if (data != null) {
                        MainActivity.this.mDeviceConnectionManager.writeObject(data.getByteArray("cmd"));
                    }
                }
                super.handleMessage(msg);
            }
        };
        this.mDeviceConnectionManager = DeviceConnectionManager.getInstance(this);
        Log.d(LOG_TAG, "connection manager code = " + this.mDeviceConnectionManager.hashCode());
        this.dm = getResources().getDisplayMetrics();
        this.mViewPager = (ViewPager) findViewById(R.id.pager);
        this.tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        this.mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), this.mCurrentLanguage);
        this.mViewPager.setAdapter(this.mPagerAdapter);
        this.tabs.setViewPager(this.mViewPager);
        setTabsValue();
        this.mReceiver = new ConnectionBroadcastReceiver();
    }

    @Override
    protected void onStop() {
        Log.i(LOG_TAG, "in onStop");
        this.hasHandShaked = false;
        super.onStop();
    }

    @Override
    protected void onResume() {
        Log.e(LOG_TAG, "in onResume");
        super.onResume();
        registerBroadcastReceiver();
        start();
    }

    @Override
    protected void onPause() {
        Log.e(LOG_TAG, "in onPause");
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
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.e(LOG_TAG, "main onDestory");
        this.mDeviceConnectionManager.clear();
        BluetoothTools.cmdArray.clear();
        cleanMessage();
        this.mWorkHandler.removeMessages(CMD_WRITE);
        this.mWorkThread.quit();
        super.onDestroy();
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

    private void stopAnimation() {
        if (this.mRunningAnimation != null) {
            Log.i(LOG_TAG, "stopAnimation");
            this.mRunningAnimation.stop();
            this.mRunningAnimation.selectDrawable(0);
        }
    }

    private void showMessage(String messageId) {
        Toast.makeText(this, ConfigManager.getInstance(this).getString(this.mCurrentLanguage, messageId), Toast.LENGTH_LONG).show();
    }

    private void startAnimation() {
        if (this.mRunningAnimation != null && !this.mRunningAnimation.isRunning()) {
            this.mRunningAnimation.start();
        }
    }

    private void handleMessageRead(String messageRead) {
        if (this.mPause) {
            Log.e(LOG_TAG, "activity pause return");
            return;
        }
        startAnimation();
        List<String> cmdArray = new ArrayList();
        for (int i = 0; i < messageRead.length(); i += 2) {
            cmdArray.add(messageRead.substring(i, i + 2));
        }
        String statusCode = (String) cmdArray.get(0);
        Log.i(LOG_TAG, "statusCode:" + statusCode);
        if ("3D".equals(statusCode) && this.hasHandShaked) {
            retry(Strings.WRONG_PACKAGE);
        } else if ("2F".equals(statusCode)) {
            retry(Strings.TIMEOUT);
        } else if ("2A".equals(statusCode)) {
            this.mRetry = 0;
            int intCheckSum = 0;
            cmdArray.remove(0);
            String checkSumTemp = (String) cmdArray.get(cmdArray.size() - 1);
            Log.i(LOG_TAG, "from server checkSum:" + checkSumTemp);
            cmdArray.remove(cmdArray.size() - 1);
            for (String str : cmdArray) {
                intCheckSum += Integer.valueOf(Hex2StringUtils.toHexOctal(str)).intValue();
            }
            String oracl2Hex = Integer.toHexString(intCheckSum);
            Log.i(LOG_TAG, "original calced checkSum:" + oracl2Hex);
            int beginIndex = oracl2Hex.length() - 2;
            if (beginIndex < 0) {
                beginIndex = 0;
            }
            String checkSum = oracl2Hex.substring(beginIndex);
            Log.i(LOG_TAG, "calced checkSum:" + checkSumTemp);
            if (!checkSum.equalsIgnoreCase(checkSumTemp)) {
                retry(Strings.DATA_VALIDATE_ERROR);
            }
            if (cmdArray.size() == 61) {
                handleStatusMessage(cmdArray);
            } else if (cmdArray.size() == 4 || cmdArray.size() == 6) {
                handleControlMessage(cmdArray);
            }
        }
    }

    private void handleStatusMessage(List<String> cmdArray) {
        BluetoothTools.cmdArray = cmdArray;
        updateFragments();
        this.mMainHandler.sendEmptyMessageDelayed(CMD_REQUEST_STATUS_MESSAGE, (long) ConfigManager.getInstance().requestInterval());
    }

    private void updateFragments() {
        if (this.basicFragment != null) {
            this.basicFragment.updateView();
        } else {
            Log.e(LOG_TAG, "basic fragment == null");
        }
        if (this.seniorFragment != null) {
            this.seniorFragment.updateView();
        }
        if (this.diagnoseFragment != null) {
            this.diagnoseFragment.updateView();
        }
    }

    private void handleControlMessage(List<String> cmdArray) {
        Log.i(LOG_TAG, "got control message + " + cmdArray);
        if (this.mRequstIndex == 0) {
            BluetoothTools.serialArray = cmdArray;
        } else if (this.mRequstIndex == 1) {
            BluetoothTools.softwareArray = cmdArray;
            List<String> softwareArray = BluetoothTools.softwareArray;
            if (softwareArray == null || softwareArray.size() != 6) {
                retry(Strings.GET_SOFTWARE_ERROR);
                return;
            }
            String softwareee = (String) softwareArray.get(1);
            String softwaredd = (String) softwareArray.get(2);
            String softwareff = Hex2StringUtils.toHexOctal((String) softwareArray.get(0));
            softwareee = Hex2StringUtils.toHexOctal(softwareee);
            String fullName = "v" + softwareff + "." + softwareee + "." + Hex2StringUtils.toHexOctal(softwaredd);
            String secondName = "v" + softwareff + "." + softwareee + ".*";
            String thirdName = "v+\\d+\\.+" + softwareee + "+\\.+\\d";
            String matchFile = matchConfig(fullName);
            if (matchFile == null) {
                matchFile = matchConfig(secondName);
            }
            if (matchFile == null) {
                matchFile = matchConfig(thirdName);
            }
            if (matchFile != null) {
                ConfigManager.getInstance(this).loadConfig(matchFile);
                calCmdString();
            } else {
                stopAnimation();
                showExitDialog(ConfigManager.getInstance(this).getString(this.mCurrentLanguage, Strings.TITLE_PROMPT), ConfigManager.getInstance(this).getString(this.mCurrentLanguage, Strings.CONTENT_UNAVAILABLE_CONTROLLER));
                return;
            }
        } else if (this.mRequstIndex == 2) {
            BluetoothTools.hardwareArray = cmdArray;
        }
        this.mRequstIndex++;
        if (this.mRequstIndex < 3) {
            this.mMainHandler.sendEmptyMessage(CMD_REQUEST_CONTROL_MESSAGE);
        } else {
            this.mMainHandler.sendEmptyMessage(CMD_REQUEST_STATUS_MESSAGE);
        }
    }

    private String matchConfig(String version) {
        String fullName = "diag_cfg_" + version + ".xml";
        boolean matched = false;
        String matchedFile = null;
        try {
            String[] fileList = getAssets().list("");
            for (String file : fileList) {
                if (file.matches(fullName)) {
                    matched = true;
                    matchedFile = file;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(LOG_TAG, "match config " + fullName + " result " + matched + " matchedFile: " + matchedFile);
        return matchedFile;
    }

    private void showExitDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String ok = ConfigManager.getInstance(this).getString(this.mCurrentLanguage, Strings.BT_OK);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(ok, new OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                MainActivity.this.finish();
            }
        });
        builder.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == 4) {
                    return true;
                }
                return false;
            }
        });
        mDialogUnsupport = builder.create();
        mDialogUnsupport.setCanceledOnTouchOutside(false);
        mDialogUnsupport.show();
    }

    private synchronized void retry(String reasonId) {
        if (this.mRetry >= 4) {
            stopAnimation();
        }
        Log.e(LOG_TAG, "retry reason = " + reasonId);
        if (this.mRequstIndex < 3) {
            requestControlMessage();
        } else {
            requestStatusMessage();
        }
        this.mRetry++;
    }

    private void requestControlMessage() {
        if (this.mConnectionLost) {
            Log.e(LOG_TAG, "connection lost requestControlMessage return!");
            return;
        }
        byte[] data;
        if (this.mRequstIndex == 0) {
            data = Hex2StringUtils.hexStringToByte("5204FF55");
            Log.d(LOG_TAG, "request serial No");
            sendCmdToECU(data);
        } else if (this.mRequstIndex == 1) {
            data = Hex2StringUtils.hexStringToByte("5206FE56");
            Log.d(LOG_TAG, "request software info");
            sendCmdToECU(data);
        } else if (this.mRequstIndex == 2) {
            data = Hex2StringUtils.hexStringToByte("5206FD55");
            Log.d(LOG_TAG, "request hardware info");
            sendCmdToECU(data);
        }
        this.mMainHandler.sendEmptyMessageDelayed(CMD_REQUEST_RETURN_TIMEOUT, 200);
    }

    private void requestStatusMessage() {
        if (this.mConnectionLost) {
            Log.e(LOG_TAG, "connection lost requestStatusMessage return!");
            return;
        }
        byte[] data = Hex2StringUtils.hexStringToByte(this.mCmdString);
        Log.d(LOG_TAG, "3.requset status message");
        sendCmdToECU(data);
        this.mMainHandler.sendEmptyMessageDelayed(CMD_REQUEST_RETURN_TIMEOUT, 200);
    }

    private void cleanMessage() {
        this.mMainHandler.removeMessages(CMD_REQUEST_CONTROL_MESSAGE);
        this.mMainHandler.removeMessages(CMD_REQUEST_STATUS_MESSAGE);
        this.mMainHandler.removeMessages(CMD_REQUEST_RETURN_TIMEOUT);
        this.mMainHandler.removeMessages(CMD_HANDSHAKE_TIMEOUT);
        this.mMainHandler.removeMessages(MESSAGE_READ_OBJECT);
        this.mRequstIndex = 0;
        this.mRetry = 0;
        this.mHandshakeRetry = 0;
        this.hasHandShaked = false;
    }

    private void sendCmdToECU(byte[] cmd) {
        Message msg = this.mWorkHandler.obtainMessage(CMD_WRITE);
        Bundle data = new Bundle();
        data.putByteArray("cmd", cmd);
        msg.setData(data);
        this.mWorkHandler.sendMessage(msg);
    }

    private void handShake() {
        if (this.mConnectionLost) {
            Log.e(LOG_TAG, "connection lost handShake return!");
            return;
        }
        byte[] data_handshake = Hex2StringUtils.hexStringToByte("3F");
        Log.d(LOG_TAG, "1.send handshake");
        sendCmdToECU(data_handshake);
        this.mMainHandler.sendEmptyMessageDelayed(CMD_HANDSHAKE_TIMEOUT, 500);
    }

    private void handshakeRetry() {
        if (this.mHandshakeRetry >= 3) {
            showMessage(Strings.HANDSHAKE_ERROR);
            return;
        }
        Log.e(LOG_TAG, "handshake time out retry!");
        handShake();
        this.mHandshakeRetry++;
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

        @Override
        public CharSequence getPageTitle(int position) {
            return this.titles[position];
        }

        /* // Returns total number of pages */
        @Override
        public int getCount() {
            return this.titles.length;
        }

        /* Returns the fragment to display for that page */
        @Override
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

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Log.i(MainActivity.LOG_TAG, "adapter instantiateItem " + position);
            return super.instantiateItem(container, position);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE; /* -2 */
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


    private void calCmdString() {
        Command localCommand = ConfigManager.getInstance(this).getReadStatusCmd();
        String str = Integer.toHexString(Integer.parseInt(localCommand.cmd, 16) + Integer.parseInt(localCommand.length, 16) + Integer.parseInt(localCommand.id, 16));
        str = str.substring(str.length() - 2);
        this.mCmdString = (localCommand.cmd + localCommand.length + localCommand.id + str);
    }

    private void getLanguageSettings() {
        this.mCurrentLanguage = PreferenceManager.getDefaultSharedPreferences(this).getString("language", Locale.getDefault().getLanguage());
    }

    private void updateTitle(String lan) {
        CharSequence title = ConfigManager.getInstance(this).getString(lan, Strings.TITLE_MAIN);
        setTitle(title);
        getSupportActionBar().setTitle(title);
    }

    private void setTabsValue() {
        this.tabs.setShouldExpand(true);
        this.tabs.setUnderlineHeight((int) TypedValue.applyDimension(1, 1.0f, this.dm));
        this.tabs.setIndicatorHeight((int) TypedValue.applyDimension(1, 4.0f, this.dm));
        this.tabs.setTextSize((int) TypedValue.applyDimension(2, 18.0f, this.dm));
        this.tabs.setIndicatorColor(Color.parseColor("#029bec"));
        this.tabs.setSelectedTextColor(Color.parseColor("#39c0ff"));
        this.tabs.setTextColor(Color.parseColor("#004986"));
        this.tabs.setTabBackground(0);
        this.tabs.setBackgroundColor(Color.parseColor("#d4e4f3"));
        this.tabs.setDividerColor(Color.parseColor("#004986"));
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothConnectionService.INTENT_CONNECTING);
        filter.addAction(BluetoothConnectionService.INTENT_CONNECT_SUCCESS);
        filter.addAction(BluetoothConnectionService.INTENT_CONNECT_ERROR);
        registerReceiver(this.mReceiver, filter);
    }

    private void unregisterBroadcastReceiver() {
        unregisterReceiver(this.mReceiver);
    }

}
