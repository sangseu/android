package mig0.bosheculogger.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.os.Message;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;


import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mig0.bosheculogger.R;
import mig0.bosheculogger.service.BluetoothCommunThread;
import mig0.bosheculogger.service.BluetoothConnectionService;
import mig0.bosheculogger.service.DeviceConnectionManager;
import mig0.bosheculogger.utils.BluetoothTools;
import mig0.bosheculogger.utils.Command;
import mig0.bosheculogger.utils.ConfigManager;
import mig0.bosheculogger.utils.ConfigManager.Strings;
import mig0.bosheculogger.utils.Hex2StringUtils;
import mig0.viewpager.PagerSlidingTabStrip;

public class MainActivity extends AppCompatActivity {

    private static String handshake_ecu_response    = "21";
    private static String request_serial_no         = "5204FF55";
    private static String request_software_info     = "5206FE56";
    private static String request_hardware_info     = "5206FD55";

    private static int max_handshake_retry = 3;
    private static int max_request_retry = 4;

    private boolean animation = false;

    private static final int CMD_HANDSHAKE_TIMEOUT = 1004;
    private static final int CMD_REQUEST_CONTROL_MESSAGE = 1002;
    private static final int CMD_REQUEST_RETURN_TIMEOUT = 1003;
    private static final int CMD_REQUEST_STATUS_MESSAGE = 1001;

    public static final int MESSAGE_CONNECT_ERROR = 3;
    public static final int MESSAGE_READ_OBJECT = 4;
    public static final int MESSAGE_CONNECT_LOST = 5;

    public static final int DIANOGE_FRAGMENT = 0;
    public static final int BASIC_FRAGMENT = 1;
    public static final int SENIOR_FRAGMENT = 2;


    private static final int CMD_WRITE = 10005;
    private static final int HANDSHAKE_TIMEOUT = 500;
    private static String LOG_TAG = "MainActivity";
    private static final int REQUSET_TIMEOUT = 200;
    private BasicFragment basicFragment;
    private DiagnoseFragment diagnoseFragment;
    private DisplayMetrics dm;
    private boolean hasHandShaked = false;
    private String mCmdString; /* read from *.xml is C0 3D 06 xx */
    public boolean mConnectionLost = false;
    private String mCurrentLanguage;
    private DeviceConnectionManager mDeviceConnectionManager;
    private AlertDialog mDialogUnsupport;
    private int mHandshakeRetry = 0;
    private MainHandler mMainHandler;
    private MyPagerAdapter mPagerAdapter;
    private boolean mPause = false;
    private ConnectionBroadcastReceiver mReceiver;
    private int mRequstIndex = 0;
    private int mRetry = 0;
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
        setContentView(R.layout.activity_main);
        getLanguageSettings();
        updateTitle(mCurrentLanguage);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setIcon(R.drawable.logo_trans);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
        }
        mMainHandler = new MainHandler(this);
        /*start thread that has a lopper*/
        mWorkThread = new HandlerThread("work");
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper()) {
            /* couple Override and super */
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MainActivity.CMD_WRITE) {
                    Bundle data = msg.getData();
                    if (data != null) {
                        mDeviceConnectionManager.writeObject(data.getByteArray("cmd"));
                    }
                }
                super.handleMessage(msg);
            }
        };
        mDeviceConnectionManager = DeviceConnectionManager.getInstance(this);
        Log.d(LOG_TAG, "connection manager code = " + mDeviceConnectionManager.hashCode());
        dm = getResources().getDisplayMetrics();
        mViewPager = (ViewPager) findViewById(R.id.pager);
        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), mCurrentLanguage);
        mViewPager.setAdapter(mPagerAdapter);
        tabs.setViewPager(mViewPager);
        setTabsValue();
        mReceiver = new ConnectionBroadcastReceiver();
    }

    @Override
    protected void onStop() {
        Log.i(LOG_TAG, "in onStop");
        hasHandShaked = false;
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
        mPause = true;
        cleanMessage();
        unregisterBroadcastReceiver();
        Intent intent = new Intent(this, BluetoothConnectionService.class);
        intent.setAction(BluetoothConnectionService.ACTION_CANCEL);
        sendBroadcast(intent);
        if (mDialogUnsupport != null && mDialogUnsupport.isShowing()) {
            mDialogUnsupport.dismiss();
            mDialogUnsupport = null;
        }
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.e(LOG_TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        Builder builder = new Builder(this);
        ConfigManager cManager = ConfigManager.getInstance(this);
        String exitTitle = cManager.getString(mCurrentLanguage, Strings.EXIT_TITLE);
        String exitMessage = cManager.getString(mCurrentLanguage, Strings.EXIT_MESSAGE);
        String exitOk = cManager.getString(mCurrentLanguage, Strings.EXIT_POSBTN);
        String exitCancel = cManager.getString(mCurrentLanguage, Strings.EXIT_NAVBTN);
        builder.setTitle(exitTitle);
        builder.setMessage(exitMessage);
        builder.setPositiveButton(exitOk, new OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                MainActivity.this.finish();
            }
        });
        builder.setNegativeButton(exitCancel, new OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Log.e(LOG_TAG, "onOptionsItemSelected: " + item.getTitle());
                startActivity(new Intent(this, HelpActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.e(LOG_TAG, "main onDestory");
        mDeviceConnectionManager.clear();
        BluetoothTools.cmdArray.clear();
        cleanMessage();
        mWorkHandler.removeMessages(CMD_WRITE);
        mWorkThread.quit();
        super.onDestroy();
    }

    private void start() {
        mPause = false;
        mConnectionLost = false;
        if (mDeviceConnectionManager.isSocketLost()) {
            mMainHandler.sendEmptyMessage(MESSAGE_CONNECT_LOST);/*5*/
            return;
        }
        mDeviceConnectionManager.startCommuThread(new BluetoothCommunThread.DataCallbackListener() {
            public void onReadObject(String message) {
                Log.d(MainActivity.LOG_TAG, " Main read + " + message);
                mMainHandler.removeMessages(MainActivity.CMD_REQUEST_RETURN_TIMEOUT);
                Message msg = mMainHandler.obtainMessage();
                msg.what = MESSAGE_READ_OBJECT; /*4*/
                msg.obj = message;
                msg.sendToTarget();
            }

            public void onConnectionLost() {
                Log.i(LOG_TAG, "onConnectionLost");
                mMainHandler.sendEmptyMessage(MESSAGE_CONNECT_LOST); /*5*/
            }

            public void onConnectionError() {
                Log.i(MainActivity.LOG_TAG, "onConnectionError");
                mMainHandler.sendEmptyMessage(MESSAGE_CONNECT_ERROR); /*3*/
            }

            public void onSocketNullException() {
                Log.i(MainActivity.LOG_TAG, "onSocketNullException");
                finish();
            }

            public void onSocketClosedException() {
                Log.i(MainActivity.LOG_TAG, "onSocketClosedException");
                mMainHandler.sendEmptyMessage(MESSAGE_CONNECT_ERROR); /*3*/
            }
        });
        mDeviceConnectionManager.resetThreadFlag();
        handShake();
    }


    static class MainHandler extends Handler {
        WeakReference<MainActivity> mActivity;

        public MainHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        /* handle message when handshake with ecu */
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case MainActivity.MESSAGE_CONNECT_ERROR:/*3*/
                    case MainActivity.MESSAGE_CONNECT_LOST:/*5*/
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
                    case MainActivity.MESSAGE_READ_OBJECT:/*4*/
                        Log.d(MainActivity.LOG_TAG, "MainHandler case MESSAGE_READ_OBJECT");
                        String messageRead = msg.obj.toString();
                        if (handshake_ecu_response.equals(messageRead)) {
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
        if (mRunningAnimation != null) {
            Log.i(LOG_TAG, "stopAnimation");
            mRunningAnimation.stop();
            mRunningAnimation.selectDrawable(0);/*stop animation, go to child 0*/
            BaseActivity.isAnimation = 0;
        }
    }

    private void showMessage(String messageId) {
        /*get messahe in current language and show Toast*/
        Toast.makeText(this, ConfigManager.getInstance(this).getString(mCurrentLanguage, messageId), Toast.LENGTH_LONG).show();
    }

    private void startAnimation() {
        if (mRunningAnimation != null && !mRunningAnimation.isRunning()) {
            mRunningAnimation.start();
        }
        BaseActivity.isAnimation = 0;
    }

    private void handleMessageRead(String messageRead) {
        if (mPause) {
            Log.e(LOG_TAG, "activity pause return");
            return;
        }
        startAnimation();
        List<String> cmdArray = new ArrayList<String>();
        /* read 2 char in HEX format -> 1 byte data */
        for (int i = 0; i < messageRead.length(); i += 2) {
            cmdArray.add(messageRead.substring(i, i + 2));
        }
        String statusCode = (String) cmdArray.get(0);
        Log.i(LOG_TAG, "statusCode:" + statusCode);
        if ("3D".equals(statusCode) && hasHandShaked) {
            retry(Strings.WRONG_PACKAGE);
        } else if ("2F".equals(statusCode)) {
            retry(Strings.TIMEOUT);
        } else if ("2A".equals(statusCode)) {
            mRetry = 0;
            int intCheckSum = 0;
            cmdArray.remove(0);
            String checkSumTemp = (String) cmdArray.get(cmdArray.size() - 1);
            Log.i(LOG_TAG, "from server checkSum:" + checkSumTemp);
            cmdArray.remove(cmdArray.size() - 1);
            for (String str : cmdArray) {
                intCheckSum += Integer.valueOf(Hex2StringUtils.toHexOctal(str));
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
            /* when handshake ok, ecu send 2 ControlMessage to app
            * app need parse ControlMessage to do some thing then communicate
            * with ecu using handleStatusMessage */
            /* got status message */
            if (cmdArray.size() == 61) {
                handleStatusMessage(cmdArray);
            }
            /* got control message */
            else if (cmdArray.size() == 4 || cmdArray.size() == 6) {
                handleControlMessage(cmdArray);
            }
        }
    }

    private void handleStatusMessage(List<String> cmdArray) {
        /* send message to parser */
        BluetoothTools.cmdArray = cmdArray;
        /* update all value
        * it link to get_x_InformationList() in ConfigManager.java to get data*/
        updateFragments();
        /* *
        * default interval/delay is 150
        * frequency send request to ecu
        * */
        mMainHandler.sendEmptyMessageDelayed(CMD_REQUEST_STATUS_MESSAGE, (long) ConfigManager.getInstance().requestInterval());
    }

    private void updateFragments() {
        if (basicFragment != null) {
            basicFragment.updateView();
        } else {
            Log.e(LOG_TAG, "basic fragment == null");
        }
        if (seniorFragment != null) {
            seniorFragment.updateView();
        } else {
            Log.e(LOG_TAG, "advance fragment == null");
        }
        if (diagnoseFragment != null) {
            diagnoseFragment.updateView();
        } else {
            Log.e(LOG_TAG, "diagnose fragment == null");
        }
    }

    /* handle ControlMessage
    * if cmdArray reiceive match cfg_v255.6.1 version
    * then load data structure from cfg_v255.6.1.xml file
    * After get 2 ControlMessage, goto handle StatusMessage*/
    private void handleControlMessage(List<String> cmdArray) {
        Log.i(LOG_TAG, "got control message " + cmdArray);
        if (mRequstIndex == 0) {
            BluetoothTools.serialArray = cmdArray;
        } else if (mRequstIndex == 1) {
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
                showExitDialog(ConfigManager.getInstance(this).getString(this.mCurrentLanguage, Strings.TITLE_PROMPT), ConfigManager.getInstance(this).getString(mCurrentLanguage, Strings.CONTENT_UNAVAILABLE_CONTROLLER));
                return;
            }
        } else if (mRequstIndex == 2) {
            BluetoothTools.hardwareArray = cmdArray;
        }
        mRequstIndex++;
        if (mRequstIndex < 3) {
            mMainHandler.sendEmptyMessage(CMD_REQUEST_CONTROL_MESSAGE);
        } else {
            mMainHandler.sendEmptyMessage(CMD_REQUEST_STATUS_MESSAGE);
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
        Builder builder = new Builder(this);
        String ok = ConfigManager.getInstance(this).getString(mCurrentLanguage, Strings.BT_OK);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                MainActivity.this.finish();
            }
        });
        builder.setOnKeyListener(new OnKeyListener() {
            @Override
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
        if (mRetry >= max_request_retry) {
            stopAnimation();
        }
        Log.e(LOG_TAG, "retry reason = " + reasonId);
        if (mRequstIndex < 3) {
            requestControlMessage();
        } else {
            requestStatusMessage();
        }
        mRetry++;
    }

    private void requestControlMessage() {
        if (this.mConnectionLost) {
            Log.e(LOG_TAG, "connection lost requestControlMessage return!");
            return;
        }
        byte[] data;
        if (this.mRequstIndex == 0) {
            data = Hex2StringUtils.hexStringToByte(request_serial_no);
            Log.d(LOG_TAG, "request serial No");
            sendCmdToECU(data);
        } else if (this.mRequstIndex == 1) {
            data = Hex2StringUtils.hexStringToByte(request_software_info);
            Log.d(LOG_TAG, "request software info");
            sendCmdToECU(data);
        } else if (this.mRequstIndex == 2) {
            data = Hex2StringUtils.hexStringToByte(request_hardware_info);
            Log.d(LOG_TAG, "request hardware info");
            sendCmdToECU(data);
        }
        mMainHandler.sendEmptyMessageDelayed(CMD_REQUEST_RETURN_TIMEOUT, 200);
    }

    private void requestStatusMessage() {
        if (this.mConnectionLost) {
            Log.e(LOG_TAG, "connection lost requestStatusMessage return!");
            return;
        }
        byte[] data = Hex2StringUtils.hexStringToByte(this.mCmdString);
        Log.d(LOG_TAG, "3.requset status message");
        sendCmdToECU(data);
        mMainHandler.sendEmptyMessageDelayed(CMD_REQUEST_RETURN_TIMEOUT, 200);
    }

    private void cleanMessage() {
        mMainHandler.removeMessages(CMD_REQUEST_CONTROL_MESSAGE);
        mMainHandler.removeMessages(CMD_REQUEST_STATUS_MESSAGE);
        mMainHandler.removeMessages(CMD_REQUEST_RETURN_TIMEOUT);
        mMainHandler.removeMessages(CMD_HANDSHAKE_TIMEOUT);
        mMainHandler.removeMessages(MESSAGE_READ_OBJECT);
        mRequstIndex = 0;
        mRetry = 0;
        mHandshakeRetry = 0;
        hasHandShaked = false;
    }

    private void sendCmdToECU(byte[] cmd) {
        Message msg = mWorkHandler.obtainMessage(CMD_WRITE);
        Bundle data = new Bundle();
        data.putByteArray("cmd", cmd);
        msg.setData(data);
        mWorkHandler.sendMessage(msg);
    }

    private void handShake() {
        if (this.mConnectionLost) {
            Log.e(LOG_TAG, "connection lost handShake return!");
            return;
        }
        byte[] data_handshake = Hex2StringUtils.hexStringToByte("3F");
        Log.d(LOG_TAG, "1.send handshake");
        sendCmdToECU(data_handshake);
        mMainHandler.sendEmptyMessageDelayed(CMD_HANDSHAKE_TIMEOUT, 500);
    }

    private void handshakeRetry() {
        if (mHandshakeRetry >= max_handshake_retry) {
            showMessage(Strings.HANDSHAKE_ERROR);
            return;
        }
        Log.e(LOG_TAG, "handshake time out retry!");
        handShake();
        mHandshakeRetry++;
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
            titles = new String[]{diagnostic, basic, advanced};
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        /* // Returns total number of pages */
        @Override
        public int getCount() {
            return titles.length;
        }

        /* Returns the fragment to display for current page */
        @Override
        public Fragment getItem(int position) {
            Log.e(LOG_TAG, "on fragment: " + position);
            Bundle args;
            switch (position) {
                /* parse information using data in cmdArray and form in *.xml file  */
                case DIANOGE_FRAGMENT /*0*/:
                    if (diagnoseFragment == null) {
                        diagnoseFragment = new DiagnoseFragment();
                        args = new Bundle();
                        args.putString("lan", mCurrentLanguage);
                        diagnoseFragment.setArguments(args);
                    }
                    return diagnoseFragment;
                case BASIC_FRAGMENT /*1*/:
                    if (basicFragment == null) {
                        basicFragment = new BasicFragment();
                        args = new Bundle();
                        args.putString("lan", MainActivity.this.mCurrentLanguage);
                        basicFragment.setArguments(args);
                    }
                    return basicFragment;
                case SENIOR_FRAGMENT /*2*/:
                    if (seniorFragment == null) {
                        seniorFragment = new SeniorFragment();
                        args = new Bundle();
                        args.putString("lan", MainActivity.this.mCurrentLanguage);
                        seniorFragment.setArguments(args);
                    }
                    return seniorFragment;
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

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothConnectionService.INTENT_CONNECTING.equals(action)) {
                ConfigManager cManager = ConfigManager.getInstance(MainActivity.this);
                String connect_to_device = intent.getStringExtra("deviceName");
                if((connect_to_device != null) && !animation) {
                    Toast.makeText(MainActivity.this, cManager.getString(mCurrentLanguage, Strings.CONNECT_TO) + connect_to_device, Toast.LENGTH_SHORT).show();
                }
            } else if (BluetoothConnectionService.INTENT_CONNECT_SUCCESS.equals(action)) {
                MainActivity.this.start();
            } else if (BluetoothConnectionService.INTENT_CONNECT_ERROR.equals(action)) {
                if (!animation) {
                    Toast.makeText(MainActivity.this, ConfigManager.getInstance(MainActivity.this).getString(mCurrentLanguage, Strings.CONNECT_FAIL), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    private void calCmdString() {
        Command localCommand = ConfigManager.getInstance(this).getReadStatusCmd(); /*from *.xml file: cmd:C0 length:3D id:06*/
        Log.i(LOG_TAG, "on_calCmd localCommand + " + localCommand.cmd);
        String str = Integer.toHexString((Integer.parseInt(localCommand.cmd, 16) + Integer.parseInt(localCommand.length, 16)) + Integer.parseInt(localCommand.id, 16));
        str = str.substring(str.length() - 2);/* C03D06 length = 6, 6-2 = 4. -> 06*/
        mCmdString = (localCommand.cmd + localCommand.length + localCommand.id + str);
        Log.i(LOG_TAG, "on_calCmd mCmdString + " + mCmdString);
    }

    private void getLanguageSettings() {
        mCurrentLanguage = PreferenceManager.getDefaultSharedPreferences(this).getString("language", Locale.getDefault().getLanguage());
    }

    private void setLanguageSettings(String lan) {
        if (!TextUtils.isEmpty(lan)) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("language", lan).apply();
            mCurrentLanguage = lan;
        }
    }


    private void updateTitle(String lan) {
        CharSequence title = ConfigManager.getInstance(this).getString(lan, Strings.TITLE_MAIN);
        setTitle(title);
        getSupportActionBar().setTitle(title);
    }

    private void setTabsValue() {
        tabs.setShouldExpand(true);
        tabs.setUnderlineHeight((int) TypedValue.applyDimension(1, 1.0f, this.dm));
        tabs.setIndicatorHeight((int) TypedValue.applyDimension(1, 4.0f, this.dm));
        tabs.setTextSize((int) TypedValue.applyDimension(2, 18.0f, this.dm));
        tabs.setIndicatorColor(Color.parseColor("#029bec"));
        tabs.setSelectedTextColor(Color.parseColor("#39c0ff"));
        tabs.setTextColor(Color.parseColor("#004986"));
        tabs.setTabBackground(0);
        tabs.setBackgroundColor(Color.parseColor("#d4e4f3"));
        tabs.setDividerColor(Color.parseColor("#004986"));
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothConnectionService.INTENT_CONNECTING);
        filter.addAction(BluetoothConnectionService.INTENT_CONNECT_SUCCESS);
        filter.addAction(BluetoothConnectionService.INTENT_CONNECT_ERROR);
        registerReceiver(mReceiver, filter);
    }

    private void unregisterBroadcastReceiver() {
        unregisterReceiver(mReceiver);
    }

    /* draw menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        /* language button */
        final Button button = (Button) ((LinearLayout) MenuItemCompat.getActionView(menu.findItem(R.id.action_encn))).findViewById(R.id.button_view);
        if ("zh".equals(this.mCurrentLanguage)) {
            button.setText(getResources().getString(R.string.switch_english));
        } else if ("en".equals(this.mCurrentLanguage)) {
            button.setText(getResources().getString(R.string.switch_chinese));
        }
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                if ("En".equals((String) button.getText())) {
                    button.setText(MainActivity.this.getResources().getString(R.string.switch_chinese));
                    switchLanguage("en");
                    return;
                }

                /* set language to chinese, toggle button */
                button.setText(getResources().getString(R.string.switch_english));
                switchLanguage("zh");
            }
        });
        /* Animation running */
        mRunningAnimation = (AnimationDrawable) ((ImageView) ((LinearLayout) MenuItemCompat.getActionView(menu.findItem(R.id.action_progressbar))).findViewById(R.id.imageView_red)).getBackground();
        return true;
    }

    private void switchLanguage(String lan) {
        if (!TextUtils.isEmpty(lan)) {
            updateTitle(lan);
            mPagerAdapter.updatePageTitle(lan);
            tabs.notifyDataSetChanged();
            if (basicFragment != null) {
                basicFragment.updateLanguage(lan);
            } else {
                Log.e(LOG_TAG, " switchLanguage: basic fragment == null");
            }
            if (seniorFragment != null) {
                seniorFragment.updateLanguage(lan);
            } else {
                Log.e(LOG_TAG, "switchLanguage: advance fragment == null");
            }
            if (diagnoseFragment != null) {
                diagnoseFragment.updateLanguage(lan);
            } else {
                Log.e(LOG_TAG, "switchLanguage: diagnose fragment == null");
            }
            setLanguageSettings(lan);
        }
    }
}
