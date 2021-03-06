package com.bosch.diag.activity;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import com.bosch.diag.service.BluetoothConnectionService;
import com.bosch.diag.service.DeviceConnectionManager;
import com.bosch.diag.service.DeviceConnectionManager.ConnectStatusListener;
import com.bosch.diag.utils.BluetoothTools;
import com.bosch.diag.utils.ConfigManager;
import com.bosch.diag.utils.ConfigManager.Strings;
import com.eScooterDiagTool.C0102R;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class SearchActivity extends BaseActivity {
    private static final int CMD_ALL_DEVICE_FOUND = 1003;
    private static final int CMD_CANCEL_PAIRED_DEVICE = 1004;
    private static final int CMD_DEVICE_FOUND = 1001;
    private static final int CMD_NO_DEVICE_FOUND = 1002;
    private static final int CMD_SHOW_CONNECT_ERROR = 1005;
    public static final String FLAG_CONNECTION_LOST = "connectionLost";
    public static final String FLAG_NEED_BACK = "needBack";
    private String alreadyConnect;
    private String connectError;
    private String connectFail;
    private String connectTo;
    private String exitCancel;
    private String exitMessage;
    private String exitOk;
    private String exitTitle;
    private String foundDevice;
    ArrayList<DeviceGroup> groups;
    private MyAdapter mAdapter;
    private AnimationDrawable mAnimationDrawable;
    private BluetoothDiagBroadcast mBluetoothReceiver;
    private Context mContext;
    private boolean mEnterForConnectionLost;
    private IntentFilter mFilter;
    private DeviceGroup mFoundDevice;
    private ExpandableListView mListView;
    private boolean mNeedBack;
    private TextView mNoDataView;
    private DeviceGroup mPairedDevice;
    private Button mSearchButton;
    private SearchHandler mSearchHandler;
    private AlertDialog mWaitDialog;
    private String noDevice;
    private String pairedDevice;
    private String startSearch;
    private String stopSearch;
    private String title;
    private String unpairCancel;
    private String unpairFailedMessage;
    private String unpairFailedTitle;
    private String unpairMessage;
    private String unpairOk;
    private String unpairTitle;
    private String waitContent;
    private String waitTitle;

    /* renamed from: com.bosch.diag.activity.SearchActivity.1 */
    class C00891 implements OnChildClickListener {
        C00891() {
        }

        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            Log.d(SearchActivity.this.LOG_TAG, "child click group" + groupPosition + "child" + childPosition);
            if (SearchActivity.this.mEnterForConnectionLost) {
                SearchActivity.this.stopConnectionService();
            }
            SearchActivity.this.connectDecvie(((DeviceGroup) SearchActivity.this.groups.get(groupPosition)).getBluetoothDevice(childPosition));
            return true;
        }
    }

    /* renamed from: com.bosch.diag.activity.SearchActivity.2 */
    class C00902 implements OnClickListener {
        C00902() {
        }

        public void onClick(View arg0) {
            if (SearchActivity.this.mSearchButton.getText().equals(SearchActivity.this.startSearch)) {
                SearchActivity.this.mFoundDevice.deviceList.clear();
                BluetoothTools.mDevices.clear();
                SearchActivity.this.updateList();
                SearchActivity.this.startDiscovery();
                if (!SearchActivity.this.mAnimationDrawable.isRunning()) {
                    SearchActivity.this.mAnimationDrawable.start();
                }
                SearchActivity.this.mSearchButton.setText(SearchActivity.this.stopSearch);
            } else if (SearchActivity.this.mSearchButton.getText().equals(SearchActivity.this.stopSearch)) {
                SearchActivity.this.stopfindDevice();
                SearchActivity.this.mSearchButton.setText(SearchActivity.this.startSearch);
            }
        }
    }

    /* renamed from: com.bosch.diag.activity.SearchActivity.3 */
    class C00913 implements DialogInterface.OnClickListener {
        C00913() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            SearchActivity.this.finish();
        }
    }

    /* renamed from: com.bosch.diag.activity.SearchActivity.4 */
    class C00924 implements DialogInterface.OnClickListener {
        C00924() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            dialog.dismiss();
        }
    }

    /* renamed from: com.bosch.diag.activity.SearchActivity.6 */
    class C00936 implements Runnable {
        C00936() {
        }

        public void run() {
            SearchActivity.this.mAnimationDrawable.start();
        }
    }

    private class BluetoothDiagBroadcast extends BroadcastReceiver {
        private BluetoothDiagBroadcast() {
        }

        public void onReceive(Context ctx, Intent intent) {
            if (intent != null) {
                String actionString = intent.getAction();
                Log.d(SearchActivity.this.LOG_TAG, "current connected action: " + actionString);
                Message message;
                if ("android.bluetooth.device.action.FOUND".equals(actionString)) {
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    if (bluetoothDevice.getBondState() != 12) {
                        Log.i(SearchActivity.this.LOG_TAG, "Bluetooth Signal Strength:    " + bluetoothDevice.getName() + ":  " + intent.getExtras().getShort("android.bluetooth.device.extra.RSSI") + "dBM");
                    }
                    if (!(bluetoothDevice == null || TextUtils.isEmpty(bluetoothDevice.getName()) || BluetoothTools.mDevices.contains(bluetoothDevice))) {
                        BluetoothTools.mDevices.add(bluetoothDevice);
                        Log.i(SearchActivity.this.LOG_TAG, "devcie size" + BluetoothTools.mDevices.size());
                    }
                    if (BluetoothTools.mDevices.size() > 0) {
                        message = new Message();
                        message.what = SearchActivity.CMD_DEVICE_FOUND;
                        SearchActivity.this.mSearchHandler.sendMessage(message);
                    }
                } else if ("android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(actionString)) {
                    SearchActivity.this.mAnimationDrawable.stop();
                    SearchActivity.this.mSearchButton.setText(SearchActivity.this.startSearch);
                    if (BluetoothTools.mDevices.isEmpty()) {
                        message = new Message();
                        message.what = SearchActivity.CMD_NO_DEVICE_FOUND;
                        SearchActivity.this.mSearchHandler.sendMessage(message);
                    } else if (BluetoothTools.mDevices.size() > 0) {
                        message = new Message();
                        message.what = SearchActivity.CMD_ALL_DEVICE_FOUND;
                        SearchActivity.this.mSearchHandler.sendMessage(message);
                    }
                } else if (!"android.bluetooth.device.action.BOND_STATE_CHANGED".equals(actionString) && !"android.bluetooth.adapter.action.STATE_CHANGED".equals(actionString)) {
                    if (BluetoothConnectionService.INTENT_CONNECTING.equals(actionString)) {
                        Toast.makeText(SearchActivity.this, new StringBuilder(String.valueOf(SearchActivity.this.connectTo)).append(intent.getStringExtra("deviceName")).toString(), 0).show();
                    } else if (BluetoothConnectionService.INTENT_CONNECT_SUCCESS.equals(actionString)) {
                        SearchActivity.this.handleConnectSuccess();
                    } else if (BluetoothConnectionService.INTENT_CONNECT_ERROR.equals(actionString)) {
                        Toast.makeText(SearchActivity.this, SearchActivity.this.connectFail, 0).show();
                    }
                }
            }
        }
    }

    private class DeviceGroup {
        ArrayList<BluetoothDevice> deviceList;
        String name;

        private DeviceGroup() {
            this.deviceList = new ArrayList();
        }

        public void addDevcie(BluetoothDevice device) {
            this.deviceList.add(device);
        }

        public BluetoothDevice getBluetoothDevice(int position) {
            return (BluetoothDevice) this.deviceList.get(position);
        }

        public int getSize() {
            return this.deviceList.size();
        }
    }

    private class MyAdapter extends BaseExpandableListAdapter {
        private RadioButton mCurrentButton;
        private ArrayList<DeviceGroup> mGroup;
        private AlertDialog mUnpairDialog;

        /* renamed from: com.bosch.diag.activity.SearchActivity.MyAdapter.1 */
        class C00971 implements OnClickListener {
            private final /* synthetic */ RadioButton val$button;
            private final /* synthetic */ int val$childPosition;
            private final /* synthetic */ int val$groupPosition;

            /* renamed from: com.bosch.diag.activity.SearchActivity.MyAdapter.1.1 */
            class C00941 implements OnClickListener {
                private final /* synthetic */ RadioButton val$button;
                private final /* synthetic */ int val$childPosition;
                private final /* synthetic */ int val$groupPosition;

                C00941(int i, int i2, RadioButton radioButton) {
                    this.val$groupPosition = i;
                    this.val$childPosition = i2;
                    this.val$button = radioButton;
                }

                public void onClick(View view) {
                    BluetoothDevice device = ((DeviceGroup) MyAdapter.this.mGroup.get(this.val$groupPosition)).getBluetoothDevice(this.val$childPosition);
                    ((DeviceGroup) SearchActivity.this.groups.get(this.val$groupPosition)).deviceList.remove(this.val$childPosition);
                    SearchActivity.this.updateList();
                    MyAdapter.this.unPairedDev(device);
                    this.val$button.setChecked(false);
                    MyAdapter.this.mCurrentButton = null;
                }
            }

            /* renamed from: com.bosch.diag.activity.SearchActivity.MyAdapter.1.2 */
            class C00952 implements OnClickListener {
                private final /* synthetic */ RadioButton val$button;

                C00952(RadioButton radioButton) {
                    this.val$button = radioButton;
                }

                public void onClick(View view) {
                    this.val$button.setChecked(false);
                    MyAdapter.this.mCurrentButton = null;
                    MyAdapter.this.mUnpairDialog.dismiss();
                }
            }

            /* renamed from: com.bosch.diag.activity.SearchActivity.MyAdapter.1.3 */
            class C00963 implements OnKeyListener {
                private final /* synthetic */ RadioButton val$button;

                C00963(RadioButton radioButton) {
                    this.val$button = radioButton;
                }

                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (event.getAction() == 1 && keyCode == 4) {
                        this.val$button.setChecked(false);
                        MyAdapter.this.mCurrentButton = null;
                    }
                    return false;
                }
            }

            C00971(RadioButton radioButton, int i, int i2) {
                this.val$button = radioButton;
                this.val$groupPosition = i;
                this.val$childPosition = i2;
            }

            public void onClick(View arg0) {
                MyAdapter.this.mCurrentButton = this.val$button;
                View layout = ((LayoutInflater) SearchActivity.this.mContext.getSystemService("layout_inflater")).inflate(C0102R.layout.unpaired_dialog, null);
                TextView titleView = (TextView) layout.findViewById(C0102R.id.unpair_diag_title);
                if (titleView != null) {
                    titleView.setText(SearchActivity.this.unpairTitle);
                }
                TextView msgView = (TextView) layout.findViewById(C0102R.id.unpair_diag_message);
                if (msgView != null) {
                    msgView.setText(SearchActivity.this.unpairMessage);
                }
                Button cancelBtn = (Button) layout.findViewById(C0102R.id.cancel_btn);
                Button okBtn = (Button) layout.findViewById(C0102R.id.ok_btn);
                if (!(cancelBtn == null || okBtn == null)) {
                    cancelBtn.setText(SearchActivity.this.unpairCancel);
                    okBtn.setText(SearchActivity.this.unpairOk);
                }
                okBtn.setOnClickListener(new C00941(this.val$groupPosition, this.val$childPosition, this.val$button));
                cancelBtn.setOnClickListener(new C00952(this.val$button));
                Builder builder = new Builder(SearchActivity.this);
                builder.setOnKeyListener(new C00963(this.val$button));
                builder.setView(layout);
                MyAdapter.this.mUnpairDialog = builder.create();
                MyAdapter.this.mUnpairDialog.setCanceledOnTouchOutside(false);
                MyAdapter.this.mUnpairDialog.show();
            }
        }

        /* renamed from: com.bosch.diag.activity.SearchActivity.MyAdapter.2 */
        class C00982 implements OnKeyListener {
            C00982() {
            }

            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                return (event.getAction() != 1 || keyCode == 4) ? false : false;
            }
        }

        /* renamed from: com.bosch.diag.activity.SearchActivity.MyAdapter.3 */
        class C00993 implements OnClickListener {
            private final /* synthetic */ AlertDialog val$unpairFailedDialog;

            C00993(AlertDialog alertDialog) {
                this.val$unpairFailedDialog = alertDialog;
            }

            public void onClick(View view) {
                Intent i = new Intent("android.settings.BLUETOOTH_SETTINGS");
                if (i != null) {
                    SearchActivity.this.mContext.startActivity(i);
                }
                this.val$unpairFailedDialog.dismiss();
            }
        }

        /* renamed from: com.bosch.diag.activity.SearchActivity.MyAdapter.4 */
        class C01004 implements OnClickListener {
            private final /* synthetic */ AlertDialog val$unpairFailedDialog;

            C01004(AlertDialog alertDialog) {
                this.val$unpairFailedDialog = alertDialog;
            }

            public void onClick(View view) {
                this.val$unpairFailedDialog.dismiss();
            }
        }

        class ViewHolder {
            RadioButton radio;
            ImageView splitLine;
            TextView text;

            ViewHolder() {
            }
        }

        public MyAdapter(Context context, ArrayList<DeviceGroup> group) {
            this.mGroup = group;
        }

        public void dismissAlertDialog() {
            if (this.mUnpairDialog != null && this.mUnpairDialog.isShowing()) {
                Log.i(SearchActivity.this.LOG_TAG, "dismiss dialog from onstop");
                this.mUnpairDialog.dismiss();
                if (this.mCurrentButton != null && this.mCurrentButton.isChecked()) {
                    this.mCurrentButton.setChecked(false);
                    this.mCurrentButton = null;
                }
            }
        }

        public View generateGroupView(int position, ViewGroup parent) {
            View view = LayoutInflater.from(SearchActivity.this).inflate(C0102R.layout.group_list_item_tag, parent, false);
            ((TextView) view.findViewById(C0102R.id.group_list_item_text)).setText(((DeviceGroup) this.mGroup.get(position)).name);
            return view;
        }

        public View generateChildView(int groupPosition, int childPosition, ViewGroup parent) {
            LinearLayout ll = (LinearLayout) LayoutInflater.from(SearchActivity.this).inflate(C0102R.layout.group_list_item, parent, false);
            RadioButton button = (RadioButton) ll.findViewById(C0102R.id.device_cancel_paired);
            button.setFocusable(false);
            button.setOnClickListener(new C00971(button, groupPosition, childPosition));
            return ll;
        }

        public Object getChild(int group_postion, int child_position) {
            return ((DeviceGroup) this.mGroup.get(group_postion)).getBluetoothDevice(child_position);
        }

        public long getChildId(int groupPosition, int childPosition) {
            return (long) childPosition;
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;
            Log.d(SearchActivity.this.LOG_TAG, "get child view");
            if (convertView != null) {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            } else {
                view = generateChildView(groupPosition, childPosition, parent);
                holder = new ViewHolder();
                holder.text = (TextView) view.findViewById(C0102R.id.group_list_item_text);
                holder.radio = (RadioButton) view.findViewById(C0102R.id.device_cancel_paired);
                holder.splitLine = (ImageView) view.findViewById(C0102R.id.device_unpair_line);
                view.setTag(holder);
            }
            BluetoothDevice device = ((DeviceGroup) this.mGroup.get(groupPosition)).getBluetoothDevice(childPosition);
            String name = device.getName();
            holder.text.setText(name);
            if (SearchActivity.this.pairedDevice.equals(((DeviceGroup) SearchActivity.this.groups.get(groupPosition)).name) && TextUtils.equals(device.getAddress(), getCurrentConnectDevice())) {
                holder.text.setText(new StringBuilder(String.valueOf(name)).append("(").append(SearchActivity.this.alreadyConnect).append(")").toString());
            }
            if (SearchActivity.this.pairedDevice.equals(((DeviceGroup) SearchActivity.this.groups.get(groupPosition)).name)) {
                holder.radio.setVisibility(0);
                holder.splitLine.setVisibility(0);
            } else {
                holder.radio.setVisibility(4);
                holder.splitLine.setVisibility(4);
            }
            return view;
        }

        private String getCurrentConnectDevice() {
            return SearchActivity.this.getSharedPreferences("bosch", 0).getString("bluetoothMAC", null);
        }

        public int getChildrenCount(int group_position) {
            Log.d(SearchActivity.this.LOG_TAG, "child count" + ((DeviceGroup) this.mGroup.get(group_position)).getSize());
            return ((DeviceGroup) this.mGroup.get(group_position)).getSize();
        }

        public Object getGroup(int postion) {
            return this.mGroup.get(postion);
        }

        public int getGroupCount() {
            Log.d(SearchActivity.this.LOG_TAG, "group count = " + this.mGroup.size());
            return this.mGroup.size();
        }

        public long getGroupId(int groupPosition) {
            return (long) groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            ((ExpandableListView) parent).expandGroup(groupPosition);
            Log.d(SearchActivity.this.LOG_TAG, "get group view");
            return generateGroupView(groupPosition, parent);
        }

        public boolean hasStableIds() {
            return false;
        }

        public boolean isChildSelectable(int arg0, int arg1) {
            return true;
        }

        private void unPairedDev(BluetoothDevice bluetoothDevice) {
            if (bluetoothDevice != null) {
                try {
                    Boolean returnValue = (Boolean) bluetoothDevice.getClass().getMethod("removeBond", new Class[0]).invoke(bluetoothDevice, new Object[0]);
                    Log.i(SearchActivity.this.LOG_TAG, "removeBond return value: " + returnValue);
                    if (!returnValue.booleanValue() || this.mUnpairDialog == null) {
                        this.mUnpairDialog.dismiss();
                        View layout = ((LayoutInflater) SearchActivity.this.mContext.getSystemService("layout_inflater")).inflate(C0102R.layout.unpaired_failed_dialog, null);
                        TextView titleView = (TextView) layout.findViewById(C0102R.id.unpair_failed_title);
                        if (titleView != null) {
                            titleView.setText(SearchActivity.this.unpairFailedTitle);
                        }
                        TextView msgView = (TextView) layout.findViewById(C0102R.id.unpair_failed_message);
                        if (msgView != null) {
                            msgView.setText(SearchActivity.this.unpairFailedMessage);
                        }
                        Button cancelBtn = (Button) layout.findViewById(C0102R.id.cancel_btn);
                        Button okBtn = (Button) layout.findViewById(C0102R.id.ok_btn);
                        if (!(cancelBtn == null || okBtn == null)) {
                            cancelBtn.setText(SearchActivity.this.unpairCancel);
                            okBtn.setText(SearchActivity.this.unpairOk);
                        }
                        Builder builder = new Builder(SearchActivity.this);
                        builder.setOnKeyListener(new C00982());
                        builder.setView(layout);
                        AlertDialog unpairFailedDialog = builder.create();
                        unpairFailedDialog.setCanceledOnTouchOutside(false);
                        unpairFailedDialog.show();
                        okBtn.setOnClickListener(new C00993(unpairFailedDialog));
                        cancelBtn.setOnClickListener(new C01004(unpairFailedDialog));
                        return;
                    }
                    this.mUnpairDialog.dismiss();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e2) {
                    e2.printStackTrace();
                } catch (IllegalArgumentException e3) {
                    e3.printStackTrace();
                } catch (InvocationTargetException e4) {
                    e4.printStackTrace();
                }
            }
        }
    }

    static class SearchHandler extends Handler {
        WeakReference<SearchActivity> mActivity;

        public SearchHandler(SearchActivity activity) {
            this.mActivity = new WeakReference(activity);
        }

        public void handleMessage(Message msg) {
            SearchActivity activity = (SearchActivity) this.mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case SearchActivity.CMD_DEVICE_FOUND /*1001*/:
                        activity.showDevices();
                        break;
                    case SearchActivity.CMD_NO_DEVICE_FOUND /*1002*/:
                        activity.showDevicesError();
                        activity.stopfindDevice();
                        break;
                    case SearchActivity.CMD_SHOW_CONNECT_ERROR /*1005*/:
                        activity.showConnectError();
                        break;
                }
                super.handleMessage(msg);
            }
        }
    }

    /* renamed from: com.bosch.diag.activity.SearchActivity.5 */
    class C01265 implements ConnectStatusListener {
        C01265() {
        }

        public void onConnectSuccess() {
            SearchActivity.this.hideWaitDialog();
            SearchActivity.this.handleConnectSuccess();
        }

        public void onConnectError() {
            SearchActivity.this.hideWaitDialog();
            SearchActivity.this.mSearchHandler.sendEmptyMessage(SearchActivity.CMD_SHOW_CONNECT_ERROR);
        }
    }

    public SearchActivity() {
        this.mFoundDevice = new DeviceGroup();
        this.mPairedDevice = new DeviceGroup();
        this.unpairTitle = "";
        this.unpairFailedTitle = "";
        this.unpairFailedMessage = "";
        this.unpairMessage = "";
        this.unpairOk = "";
        this.unpairCancel = "";
        this.mEnterForConnectionLost = false;
        this.mNeedBack = false;
        this.groups = new ArrayList();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) C0102R.layout.bluethooth_config);
        setActionBarUp();
        getConnectionLost();
        this.mContext = this;
        initStringRes();
        setTitle(this.title);
        getSupportActionBar().setTitle(this.title);
        if (VERSION.SDK_INT <= 13) {
            getSupportActionBar().setIcon((int) C0102R.drawable.logo1);
        }
        this.mSearchHandler = new SearchHandler(this);
        if (BluetoothTools.mDevices != null) {
            BluetoothTools.mDevices.clear();
        }
        if (!isEnable()) {
            enableBlueTooth();
        }
        this.mListView = (ExpandableListView) findViewById(C0102R.id.list);
        this.mListView.setGroupIndicator(null);
        this.mNoDataView = (TextView) findViewById(C0102R.id.no_data_view);
        this.mPairedDevice.name = this.pairedDevice;
        this.mFoundDevice.name = this.foundDevice;
        this.mAdapter = new MyAdapter(this.mContext, this.groups);
        this.mListView.setAdapter(this.mAdapter);
        updateDataPaired();
        updateList();
        this.mListView.setOnChildClickListener(new C00891());
        this.mSearchButton = (Button) findViewById(C0102R.id.SearchActivity_Search_button);
        this.mSearchButton.setText(this.startSearch);
        this.mSearchButton.setOnClickListener(new C00902());
    }

    private void initStringRes() {
        ConfigManager cManager = ConfigManager.getInstance(this.mContext);
        this.title = cManager.getString(this.mCurrentLanguage, Strings.BT_CONFIG);
        this.pairedDevice = cManager.getString(this.mCurrentLanguage, Strings.BT_PAIRED_DEVICE);
        this.foundDevice = cManager.getString(this.mCurrentLanguage, Strings.BT_FOUND_DEVICE);
        this.startSearch = cManager.getString(this.mCurrentLanguage, Strings.START_SEARCH);
        this.stopSearch = cManager.getString(this.mCurrentLanguage, Strings.STOP_SEARCH);
        this.connectError = cManager.getString(this.mCurrentLanguage, Strings.DEVICE_CONNCET_ERROR);
        this.unpairTitle = cManager.getString(this.mCurrentLanguage, Strings.UNPAIR_TITLE);
        this.unpairMessage = cManager.getString(this.mCurrentLanguage, Strings.UNPAIR_MESSAGE);
        this.unpairFailedTitle = cManager.getString(this.mCurrentLanguage, Strings.UNPAIR_FAILED_TITLE);
        this.unpairFailedMessage = cManager.getString(this.mCurrentLanguage, Strings.UNPAIR_FAILED_MESSAGE);
        this.unpairOk = cManager.getString(this.mCurrentLanguage, Strings.UNPAIR_POSBTN);
        this.unpairCancel = cManager.getString(this.mCurrentLanguage, Strings.UNPAIR_NAVBTN);
        this.noDevice = cManager.getString(this.mCurrentLanguage, Strings.NO_DEVICE);
        this.waitTitle = cManager.getString(this.mCurrentLanguage, Strings.WAIT);
        this.waitContent = cManager.getString(this.mCurrentLanguage, Strings.WAIT_CONTENT);
        this.exitTitle = cManager.getString(this.mCurrentLanguage, Strings.EXIT_TITLE);
        this.exitMessage = cManager.getString(this.mCurrentLanguage, Strings.EXIT_MESSAGE);
        this.exitOk = cManager.getString(this.mCurrentLanguage, Strings.EXIT_POSBTN);
        this.exitCancel = cManager.getString(this.mCurrentLanguage, Strings.EXIT_NAVBTN);
        this.alreadyConnect = cManager.getString(this.mCurrentLanguage, Strings.ALREADY_CONNECT);
        this.connectTo = cManager.getString(this.mCurrentLanguage, Strings.CONNECT_TO);
        this.connectFail = cManager.getString(this.mCurrentLanguage, Strings.CONNECT_FAIL);
    }

    protected void onStart() {
        super.onStart();
        startDiscovery();
        if (this.mSearchButton != null) {
            this.mSearchButton.setText(this.stopSearch);
        }
        if (this.mAnimationDrawable != null && !this.mAnimationDrawable.isRunning()) {
            this.mAnimationDrawable.start();
        }
    }

    protected void onResume() {
        super.onResume();
        registBroadcast();
    }

    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(this.mBluetoothReceiver);
            stopConnectionService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopConnectionService() {
        Intent intent = new Intent(this, BluetoothConnectionService.class);
        intent.setAction(BluetoothConnectionService.ACTION_CANCEL);
        startService(intent);
    }

    protected void onStop() {
        super.onStop();
        this.groups.clear();
        if (this.mAdapter != null) {
            this.mAdapter.notifyDataSetChanged();
            this.mAdapter.dismissAlertDialog();
        }
        if (this.mAnimationDrawable != null && this.mAnimationDrawable.isRunning()) {
            stopfindDevice();
        }
    }

    public Intent getSupportParentActivityIntent() {
        Log.d(this.LOG_TAG, "getSupportParentActivityIntent");
        finish();
        return super.getSupportParentActivityIntent();
    }

    public boolean onSupportNavigateUp() {
        Log.d(this.LOG_TAG, "onSupportNavigateUp");
        return super.onSupportNavigateUp();
    }

    public void onBackPressed() {
        if (this.mNeedBack) {
            finish();
            return;
        }
        Builder builder = new Builder(this);
        builder.setTitle(this.exitTitle);
        builder.setMessage(this.exitMessage);
        builder.setPositiveButton(this.exitOk, new C00913());
        builder.setNegativeButton(this.exitCancel, new C00924());
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void connectDecvie(BluetoothDevice device) {
        showWaitDialog();
        DeviceConnectionManager.getInstance(this).connectDevice(device, new C01265());
    }

    private void handleConnectSuccess() {
        Log.d(this.LOG_TAG, "set result ok!");
        setResult(-1);
        finish();
    }

    private void showWaitDialog() {
        String diagTitle = this.waitTitle;
        String diagContent = this.waitContent;
        View waitDialog = LayoutInflater.from(this).inflate(C0102R.layout.searchdevice_wait_dialog, null);
        TextView msgTV = (TextView) waitDialog.findViewById(C0102R.id.wait_message);
        ((TextView) waitDialog.findViewById(C0102R.id.wait_title)).setText(diagTitle);
        msgTV.setText(diagContent);
        Builder builder = new Builder(this);
        builder.setView(waitDialog);
        this.mWaitDialog = builder.create();
        this.mWaitDialog.setCanceledOnTouchOutside(false);
        this.mWaitDialog.show();
    }

    private void hideWaitDialog() {
        if (this.mWaitDialog != null && this.mWaitDialog.isShowing()) {
            this.mWaitDialog.dismiss();
        }
    }

    private void setActionBarUp() {
        Intent intent = getIntent();
        if (intent != null) {
            this.mNeedBack = intent.getBooleanExtra(FLAG_NEED_BACK, false);
        }
        ActionBar actionBar = getSupportActionBar();
        if (this.mNeedBack) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        } else {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    private void getConnectionLost() {
        Intent intent = getIntent();
        if (intent != null) {
            this.mEnterForConnectionLost = intent.getBooleanExtra(FLAG_CONNECTION_LOST, false);
            if (this.mEnterForConnectionLost) {
                Log.i("zxl", "serach activity enter for connection lost!");
                Intent serviceIntent = new Intent(this, BluetoothConnectionService.class);
                serviceIntent.setAction(BluetoothConnectionService.ACTION_CONNECT);
                startService(serviceIntent);
            }
        }
    }

    private void registBroadcast() {
        this.mBluetoothReceiver = new BluetoothDiagBroadcast();
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.bluetooth.device.action.FOUND");
        this.mFilter.addAction("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
        this.mFilter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        this.mFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        this.mFilter.addAction(BluetoothConnectionService.INTENT_CONNECTING);
        this.mFilter.addAction(BluetoothConnectionService.INTENT_CONNECT_SUCCESS);
        this.mFilter.addAction(BluetoothConnectionService.INTENT_CONNECT_ERROR);
        registerReceiver(this.mBluetoothReceiver, this.mFilter);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(C0102R.menu.settings, menu);
        ImageView button = (ImageView) ((LinearLayout) MenuItemCompat.getActionView(menu.findItem(C0102R.id.action_progressbar_refresh))).findViewById(C0102R.id.button_settings);
        this.mAnimationDrawable = (AnimationDrawable) button.getBackground();
        button.post(new C00936());
        this.mSearchButton.setText(this.stopSearch);
        return true;
    }

    private void updateDataPaired() {
        this.mPairedDevice.deviceList.clear();
        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                this.mPairedDevice.addDevcie(device);
            }
        }
    }

    private void updateListPaired() {
        if (this.groups.contains(this.mPairedDevice) && this.mPairedDevice.deviceList.size() == 0) {
            this.mPairedDevice.deviceList.clear();
            this.groups.remove(this.mPairedDevice);
        }
        if (!this.groups.contains(this.mPairedDevice) && this.mPairedDevice.deviceList.size() > 0) {
            this.groups.add(this.mPairedDevice);
        }
        this.mAdapter.notifyDataSetChanged();
    }

    private void updateListFound() {
        if (!(this.mPairedDevice == null || this.mPairedDevice.deviceList == null || this.mPairedDevice.deviceList.size() <= 0 || this.mFoundDevice == null || this.mFoundDevice.deviceList == null || this.mFoundDevice.deviceList.size() <= 0)) {
            Iterator it = this.mPairedDevice.deviceList.iterator();
            while (it.hasNext()) {
                BluetoothDevice device = (BluetoothDevice) it.next();
                String deviceName = device.getAddress();
                if (this.mFoundDevice.deviceList.remove(device)) {
                    Log.i(this.LOG_TAG, "remove " + deviceName + " from list");
                }
            }
        }
        if (this.groups.contains(this.mFoundDevice) && this.mFoundDevice.deviceList.size() == 0) {
            this.mFoundDevice.deviceList.clear();
            this.groups.remove(this.mFoundDevice);
        }
        if (!this.groups.contains(this.mFoundDevice) && this.mFoundDevice.deviceList.size() > 0) {
            this.groups.add(this.mFoundDevice);
        }
        this.mAdapter.notifyDataSetChanged();
    }

    private void updateList() {
        updateListPaired();
        updateListFound();
    }

    public void showDevices() {
        Log.d(this.LOG_TAG, "got device size = " + BluetoothTools.mDevices.size());
        this.mFoundDevice.deviceList.clear();
        for (BluetoothDevice device : BluetoothTools.mDevices) {
            this.mFoundDevice.addDevcie(device);
        }
        updateDataPaired();
        updateList();
        setupNoDataView();
    }

    public void showDevicesError() {
        this.mFoundDevice.deviceList.clear();
        updateDataPaired();
        updateList();
        setupNoDataView();
    }

    public boolean isEnable() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    public void enableBlueTooth() {
        BluetoothAdapter.getDefaultAdapter().enable();
    }

    public void startDiscovery() {
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
    }

    protected void stopfindDevice() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        boolean discovering = adapter.isDiscovering();
        Log.i(this.LOG_TAG, "BluetoothAdapter isDiscovering=" + discovering);
        if (discovering) {
            adapter.cancelDiscovery();
        }
        if (this.mAnimationDrawable.isRunning()) {
            this.mAnimationDrawable.stop();
            this.mSearchButton.setText(this.startSearch);
        }
    }

    private void setupNoDataView() {
        this.mNoDataView.setText(this.noDevice);
        if (this.mAdapter.isEmpty()) {
            this.mNoDataView.setVisibility(0);
        } else {
            this.mNoDataView.setVisibility(8);
        }
    }

    private void showConnectError() {
        Toast.makeText(this.mContext, this.connectError, 1).show();
    }

    protected void cancelPaired(BluetoothDevice bluetoothDevice) {
        Message message = new Message();
        message.what = CMD_CANCEL_PAIRED_DEVICE;
        message.obj = bluetoothDevice;
        this.mSearchHandler.sendMessage(message);
    }
}
