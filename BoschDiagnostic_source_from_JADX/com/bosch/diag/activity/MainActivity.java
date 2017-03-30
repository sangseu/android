package com.bosch.diag.activity;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
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
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.media.TransportMediator;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.internal.widget.ActionBarView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
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
import com.bosch.diag.service.BluetoothCommunThread.DataCallbackListener;
import com.bosch.diag.service.BluetoothConnectionService;
import com.bosch.diag.service.DeviceConnectionManager;
import com.bosch.diag.utils.BluetoothTools;
import com.bosch.diag.utils.Command;
import com.bosch.diag.utils.ConfigManager;
import com.bosch.diag.utils.ConfigManager.Strings;
import com.bosch.diag.utils.Hex2StringUtils;
import com.eScooterDiagTool.C0102R;
import com.viewpager.PagerSlidingTabStrip;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends ActionBarActivity {
    private static final int CMD_HANDSHAKE_TIMEOUT = 1004;
    private static final int CMD_REQUEST_CONTROL_MESSAGE = 1002;
    private static final int CMD_REQUEST_RETURN_TIMEOUT = 1003;
    private static final int CMD_REQUEST_STATUS_MESSAGE = 1001;
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

    /* renamed from: com.bosch.diag.activity.MainActivity.1 */
    class C00831 extends Handler {
        C00831(Looper $anonymous0) {
            super($anonymous0);
        }

        public void handleMessage(Message msg) {
            if (msg.what == MainActivity.CMD_WRITE) {
                Bundle data = msg.getData();
                if (data != null) {
                    MainActivity.this.mDeviceConnectionManager.writeObject(data.getByteArray("cmd"));
                }
            }
            super.handleMessage(msg);
        }
    }

    /* renamed from: com.bosch.diag.activity.MainActivity.3 */
    class C00843 implements OnClickListener {
        C00843() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            MainActivity.this.finish();
        }
    }

    /* renamed from: com.bosch.diag.activity.MainActivity.4 */
    class C00854 implements OnKeyListener {
        C00854() {
        }

        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode == 4) {
                return true;
            }
            return false;
        }
    }

    /* renamed from: com.bosch.diag.activity.MainActivity.5 */
    class C00865 implements View.OnClickListener {
        private final /* synthetic */ Button val$button;

        C00865(Button button) {
            this.val$button = button;
        }

        public void onClick(View arg0) {
            if ("En".equals((String) this.val$button.getText())) {
                this.val$button.setText(MainActivity.this.getResources().getString(C0102R.string.switch_chinese));
                MainActivity.this.swithLanguage("en");
                return;
            }
            this.val$button.setText("En");
            MainActivity.this.swithLanguage("zh");
        }
    }

    /* renamed from: com.bosch.diag.activity.MainActivity.6 */
    class C00876 implements OnClickListener {
        C00876() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            MainActivity.this.finish();
        }
    }

    /* renamed from: com.bosch.diag.activity.MainActivity.7 */
    class C00887 implements OnClickListener {
        C00887() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            dialog.dismiss();
        }
    }

    private class ConnectionBroadcastReceiver extends BroadcastReceiver {
        private ConnectionBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothConnectionService.INTENT_CONNECTING.equals(action)) {
                ConfigManager cManager = ConfigManager.getInstance(MainActivity.this);
                Toast.makeText(MainActivity.this, cManager.getString(MainActivity.this.mCurrentLanguage, Strings.CONNECT_TO) + intent.getStringExtra("deviceName"), 0).show();
            } else if (BluetoothConnectionService.INTENT_CONNECT_SUCCESS.equals(action)) {
                MainActivity.this.start();
            } else if (BluetoothConnectionService.INTENT_CONNECT_ERROR.equals(action)) {
                Toast.makeText(MainActivity.this, ConfigManager.getInstance(MainActivity.this).getString(MainActivity.this.mCurrentLanguage, Strings.CONNECT_FAIL), 0).show();
            }
        }
    }

    static class MainHandler extends Handler {
        WeakReference<MainActivity> mActivity;

        public MainHandler(MainActivity activity) {
            this.mActivity = new WeakReference(activity);
        }

        public void handleMessage(Message msg) {
            MainActivity activity = (MainActivity) this.mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER /*3*/:
                    case FragmentManagerImpl.ANIM_STYLE_FADE_ENTER /*5*/:
                        Log.d(MainActivity.LOG_TAG, "MainHandler case MESSAGE_CONNECT_LOST");
                        activity.stopAnimation();
                        if (!activity.mPause) {
                            activity.showMessage(Strings.CONNNECT_ERROR);
                            Intent serviceIntent = new Intent(activity, BluetoothConnectionService.class);
                            serviceIntent.setAction(BluetoothConnectionService.ACTION_CONNECT);
                            activity.startService(serviceIntent);
                        }
                        clearMessageQueue();
                        activity.mConnectionLost = true;
                    case TransportMediator.FLAG_KEY_MEDIA_PLAY /*4*/:
                        Log.d(MainActivity.LOG_TAG, "MainHandler case MESSAGE_READ_OBJECT");
                        String messageRead = msg.obj;
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
                    case MainActivity.CMD_REQUEST_STATUS_MESSAGE /*1001*/:
                        Log.d(MainActivity.LOG_TAG, "MainHandler case CMD_REQUEST_STATUS_MESSAGE");
                        activity.requestStatusMessage();
                    case MainActivity.CMD_REQUEST_CONTROL_MESSAGE /*1002*/:
                        Log.d(MainActivity.LOG_TAG, "MainHandler case CMD_REQUEST_CONTROL_MESSAGE");
                        activity.requestControlMessage();
                    case MainActivity.CMD_REQUEST_RETURN_TIMEOUT /*1003*/:
                        Log.d(MainActivity.LOG_TAG, "MainHandler case CMD_REQUEST_RETURN_TIMEOUT");
                        activity.retry(Strings.TIMEOUT);
                    case MainActivity.CMD_HANDSHAKE_TIMEOUT /*1004*/:
                        Log.d(MainActivity.LOG_TAG, "MainHandler case CMD_HANDSHAKE_TIMEOUT");
                        activity.handshakeRetry();
                    default:
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

    /* renamed from: com.bosch.diag.activity.MainActivity.2 */
    class C01252 implements DataCallbackListener {
        C01252() {
        }

        public void onReadObject(String message) {
            Log.d(MainActivity.LOG_TAG, " Main read + " + message);
            MainActivity.this.mMainHandler.removeMessages(MainActivity.CMD_REQUEST_RETURN_TIMEOUT);
            Message msg = MainActivity.this.mMainHandler.obtainMessage();
            msg.what = 4;
            msg.obj = message;
            msg.sendToTarget();
        }

        public void onConnectionLost() {
            Log.i(MainActivity.LOG_TAG, "onConnectionLost");
            MainActivity.this.mMainHandler.sendEmptyMessage(5);
        }

        public void onConnectionError() {
            Log.i(MainActivity.LOG_TAG, "onConnectionError");
            MainActivity.this.mMainHandler.sendEmptyMessage(3);
        }

        public void onSocketNullException() {
            Log.i(MainActivity.LOG_TAG, "onSocketNullException");
            MainActivity.this.finish();
        }

        public void onSocketClosedException() {
            Log.i(MainActivity.LOG_TAG, "onSocketClosedException");
            MainActivity.this.mMainHandler.sendEmptyMessage(3);
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
                case ActionBarView.DISPLAY_DEFAULT /*0*/:
                    if (MainActivity.this.diagnoseFragment == null) {
                        MainActivity.this.diagnoseFragment = new DiagnoseFragment();
                        args = new Bundle();
                        args.putString("lan", MainActivity.this.mCurrentLanguage);
                        MainActivity.this.diagnoseFragment.setArguments(args);
                    }
                    return MainActivity.this.diagnoseFragment;
                case CursorAdapter.FLAG_AUTO_REQUERY /*1*/:
                    if (MainActivity.this.basicFragment == null) {
                        MainActivity.this.basicFragment = new BasicFragment();
                        args = new Bundle();
                        args.putString("lan", MainActivity.this.mCurrentLanguage);
                        MainActivity.this.basicFragment.setArguments(args);
                    }
                    return MainActivity.this.basicFragment;
                case CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER /*2*/:
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

    public MainActivity() {
        this.mRetry = 0;
        this.mHandshakeRetry = 0;
        this.mPause = false;
        this.mRequstIndex = 0;
        this.hasHandShaked = false;
        this.mConnectionLost = false;
    }

    static {
        LOG_TAG = "MainActivity";
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(LOG_TAG, "Main onCreate");
        setContentView((int) C0102R.layout.activity_main);
        getLanguageSettings();
        updateTitle(this.mCurrentLanguage);
        if (VERSION.SDK_INT <= 13) {
            getSupportActionBar().setIcon((int) C0102R.drawable.logo1);
        }
        this.mMainHandler = new MainHandler(this);
        this.mWorkThread = new HandlerThread("work");
        this.mWorkThread.start();
        this.mWorkHandler = new C00831(this.mWorkThread.getLooper());
        this.mDeviceConnectionManager = DeviceConnectionManager.getInstance(this);
        Log.d(LOG_TAG, "connection manager code = " + this.mDeviceConnectionManager.hashCode());
        this.dm = getResources().getDisplayMetrics();
        this.mViewPager = (ViewPager) findViewById(C0102R.id.pager);
        this.tabs = (PagerSlidingTabStrip) findViewById(C0102R.id.tabs);
        this.mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), this.mCurrentLanguage);
        this.mViewPager.setAdapter(this.mPagerAdapter);
        this.tabs.setViewPager(this.mViewPager);
        setTabsValue();
        this.mReceiver = new ConnectionBroadcastReceiver();
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

    protected void onStop() {
        Log.i(LOG_TAG, "in onStop");
        this.hasHandShaked = false;
        super.onStop();
    }

    protected void onResume() {
        Log.e(LOG_TAG, "in onResume");
        super.onResume();
        registerBroadcastReceiver();
        start();
    }

    private void start() {
        this.mPause = false;
        this.mConnectionLost = false;
        if (this.mDeviceConnectionManager.isSocketLost()) {
            this.mMainHandler.sendEmptyMessage(5);
            return;
        }
        this.mDeviceConnectionManager.startCommuThread(new C01252());
        this.mDeviceConnectionManager.resetThreadFlag();
        handShake();
    }

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

    public void onConfigurationChanged(Configuration newConfig) {
        Log.e(LOG_TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

    private void cleanMessage() {
        this.mMainHandler.removeMessages(CMD_REQUEST_CONTROL_MESSAGE);
        this.mMainHandler.removeMessages(CMD_REQUEST_STATUS_MESSAGE);
        this.mMainHandler.removeMessages(CMD_REQUEST_RETURN_TIMEOUT);
        this.mMainHandler.removeMessages(CMD_HANDSHAKE_TIMEOUT);
        this.mMainHandler.removeMessages(4);
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

    private void calCmdString() {
        Command cmd = ConfigManager.getInstance(this).getReadStatusCmd();
        String h = Integer.toHexString((Integer.parseInt(cmd.cmd, 16) + Integer.parseInt(cmd.length, 16)) + Integer.parseInt(cmd.id, 16));
        this.mCmdString = cmd.cmd + cmd.length + cmd.id + h.substring(h.length() - 2);
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

    private void handshakeRetry() {
        if (this.mHandshakeRetry >= 3) {
            showMessage(Strings.HANDSHAKE_ERROR);
            return;
        }
        Log.e(LOG_TAG, "handshake time out retry!");
        handShake();
        this.mHandshakeRetry++;
    }

    private void startAnimation() {
        if (this.mRunningAnimation != null && !this.mRunningAnimation.isRunning()) {
            this.mRunningAnimation.start();
        }
    }

    private void stopAnimation() {
        if (this.mRunningAnimation != null) {
            Log.i(LOG_TAG, "stopAnimation");
            this.mRunningAnimation.stop();
            this.mRunningAnimation.selectDrawable(0);
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

    private void getLanguageSettings() {
        this.mCurrentLanguage = PreferenceManager.getDefaultSharedPreferences(this).getString("language", Locale.getDefault().getLanguage());
    }

    private void setLanguageSettings(String lan) {
        if (!TextUtils.isEmpty(lan)) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("language", lan).commit();
            this.mCurrentLanguage = lan;
        }
    }

    private void updateTitle(String lan) {
        CharSequence title = ConfigManager.getInstance(this).getString(lan, Strings.TITLE_MAIN);
        setTitle(title);
        getSupportActionBar().setTitle(title);
    }

    private void showMessage(String messageId) {
        Toast.makeText(this, ConfigManager.getInstance(this).getString(this.mCurrentLanguage, messageId), 1).show();
    }

    private void showExitDialog(String title, String message) {
        Builder builder = new Builder(this);
        String ok = ConfigManager.getInstance(this).getString(this.mCurrentLanguage, Strings.BT_OK);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(ok, new C00843());
        builder.setOnKeyListener(new C00854());
        this.mDialogUnsupport = builder.create();
        this.mDialogUnsupport.setCanceledOnTouchOutside(false);
        this.mDialogUnsupport.show();
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

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(C0102R.menu.main, menu);
        Button button = (Button) ((LinearLayout) MenuItemCompat.getActionView(menu.findItem(C0102R.id.action_encn))).findViewById(C0102R.id.button_view);
        if ("zh".equals(this.mCurrentLanguage)) {
            button.setText("En");
        } else if ("en".equals(this.mCurrentLanguage)) {
            button.setText(getResources().getString(C0102R.string.switch_chinese));
        }
        button.setOnClickListener(new C00865(button));
        this.mRunningAnimation = (AnimationDrawable) ((ImageView) ((LinearLayout) MenuItemCompat.getActionView(menu.findItem(C0102R.id.action_progressbar))).findViewById(C0102R.id.imageView_red)).getBackground();
        return true;
    }

    private void swithLanguage(String lan) {
        if (!TextUtils.isEmpty(lan)) {
            updateTitle(lan);
            this.mPagerAdapter.updatePageTitle(lan);
            this.tabs.notifyDataSetChanged();
            if (this.basicFragment != null) {
                this.basicFragment.updateLanguage(lan);
            } else {
                Log.e(LOG_TAG, "basic fragment == null");
            }
            if (this.seniorFragment != null) {
                this.seniorFragment.updateLanguage(lan);
            }
            if (this.diagnoseFragment != null) {
                this.diagnoseFragment.updateLanguage(lan);
            }
            setLanguageSettings(lan);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case C0102R.id.action_settings:
                startActivity(new Intent(this, HelpActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onBackPressed() {
        Builder builder = new Builder(this);
        ConfigManager cManager = ConfigManager.getInstance(this);
        String exitTitle = cManager.getString(this.mCurrentLanguage, Strings.EXIT_TITLE);
        String exitMessage = cManager.getString(this.mCurrentLanguage, Strings.EXIT_MESSAGE);
        String exitOk = cManager.getString(this.mCurrentLanguage, Strings.EXIT_POSBTN);
        String exitCancel = cManager.getString(this.mCurrentLanguage, Strings.EXIT_NAVBTN);
        builder.setTitle(exitTitle);
        builder.setMessage(exitMessage);
        builder.setPositiveButton(exitOk, new C00876());
        builder.setNegativeButton(exitCancel, new C00887());
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    protected void onDestroy() {
        Log.e(LOG_TAG, "main onDestory");
        this.mDeviceConnectionManager.clear();
        BluetoothTools.cmdArray.clear();
        cleanMessage();
        this.mWorkHandler.removeMessages(CMD_WRITE);
        this.mWorkThread.quit();
        super.onDestroy();
    }
}
