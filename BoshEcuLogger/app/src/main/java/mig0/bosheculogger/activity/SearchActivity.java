package mig0.bosheculogger.activity;

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
import android.content.SharedPreferences;
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

import mig0.bosheculogger.R;
import mig0.bosheculogger.service.BluetoothConnectionService;
import mig0.bosheculogger.service.DeviceConnectionManager;
import mig0.bosheculogger.service.DeviceConnectionManager.ConnectStatusListener;
import mig0.bosheculogger.utils.BluetoothTools;
import mig0.bosheculogger.utils.ConfigManager;
import mig0.bosheculogger.utils.ConfigManager.Strings;
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
    ArrayList<DeviceGroup> groups = new ArrayList();
    private MyAdapter mAdapter;
    private AnimationDrawable mAnimationDrawable;
    private BluetoothDiagBroadcast mBluetoothReceiver;
    private Context mContext;
    private boolean mEnterForConnectionLost = false;
    private IntentFilter mFilter;
    private DeviceGroup mFoundDevice = new DeviceGroup();
    private ExpandableListView mListView;
    private boolean mNeedBack = false;
    private TextView mNoDataView;
    private DeviceGroup mPairedDevice = new DeviceGroup();
    private Button mSearchButton;
    private SearchHandler mSearchHandler;
    private AlertDialog mWaitDialog;
    private String noDevice;
    private String pairedDevice;
    private String startSearch;
    private String stopSearch;
    private String title;
    private String unpairCancel = "";
    private String unpairFailedMessage = "";
    private String unpairFailedTitle = "";
    private String unpairMessage = "";
    private String unpairOk = "";
    private String unpairTitle = "";
    private String waitContent;
    private String waitTitle;
    protected String LOG_TAG = "search_activity";

    private class BluetoothDiagBroadcast extends BroadcastReceiver {
        private BluetoothDiagBroadcast() {
        }
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (intent != null) {
                String actionString = intent.getAction();
                Log.d(SearchActivity.this.LOG_TAG, "current connected action: " + actionString);
                Message message;
                if (BluetoothDevice.ACTION_FOUND.equals(actionString)) {
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (bluetoothDevice.getBondState() != 12) {
                        Log.i(SearchActivity.this.LOG_TAG, "Bluetooth Signal Strength:    " + bluetoothDevice.getName() + ":  " + intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI) + "dBM");
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
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(actionString)) {
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
                } else if (!BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(actionString) && !BluetoothAdapter.ACTION_STATE_CHANGED.equals(actionString)) {
                    if (BluetoothConnectionService.INTENT_CONNECTING.equals(actionString)) {
                        Toast.makeText(SearchActivity.this, new StringBuilder(String.valueOf(SearchActivity.this.connectTo)).append(intent.getStringExtra(BluetoothDevice.EXTRA_NAME)).toString(), Toast.LENGTH_SHORT).show();
                    } else if (BluetoothConnectionService.INTENT_CONNECT_SUCCESS.equals(actionString)) {
                        SearchActivity.this.handleConnectSuccess();
                    } else if (BluetoothConnectionService.INTENT_CONNECT_ERROR.equals(actionString)) {
                        Toast.makeText(SearchActivity.this, SearchActivity.this.connectFail, Toast.LENGTH_SHORT).show();
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
            View view = LayoutInflater.from(SearchActivity.this).inflate(R.layout.group_list_item_tag, parent, false);
            ((TextView) view.findViewById(R.id.group_list_item_text)).setText(((DeviceGroup) this.mGroup.get(position)).name);
            return view;
        }

        public View generateChildView(final int groupPosition, final int childPosition, ViewGroup parent) {
            LinearLayout ll = (LinearLayout) LayoutInflater.from(SearchActivity.this).inflate(R.layout.group_list_item, parent, false);
            final RadioButton button = (RadioButton) ll.findViewById(R.id.device_cancel_paired);
            button.setFocusable(false);
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    MyAdapter.this.mCurrentButton = button;
                    View layout = ((LayoutInflater) SearchActivity.this.mContext.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.unpaired_dialog, null);
                    TextView titleView = (TextView) layout.findViewById(R.id.unpair_diag_title);
                    if (titleView != null) {
                        titleView.setText(SearchActivity.this.unpairTitle);
                    }
                    TextView msgView = (TextView) layout.findViewById(R.id.unpair_diag_message);
                    if (msgView != null) {
                        msgView.setText(SearchActivity.this.unpairMessage);
                    }
                    Button cancelBtn = (Button) layout.findViewById(R.id.cancel_btn);
                    Button okBtn = (Button) layout.findViewById(R.id.ok_btn);
                    if (!(cancelBtn == null || okBtn == null)) {
                        cancelBtn.setText(SearchActivity.this.unpairCancel);
                        okBtn.setText(SearchActivity.this.unpairOk);
                    }
                    final int i = groupPosition;
                    final int i2 = childPosition;
                    final RadioButton radioButton = button;
                    okBtn.setOnClickListener(new OnClickListener() {
                        public void onClick(View view) {
                            BluetoothDevice device = ((DeviceGroup) MyAdapter.this.mGroup.get(i)).getBluetoothDevice(i2);
                            ((DeviceGroup) SearchActivity.this.groups.get(i)).deviceList.remove(i2);
                            SearchActivity.this.updateList();
                            MyAdapter.this.unPairedDev(device);
                            radioButton.setChecked(false);
                            MyAdapter.this.mCurrentButton = null;
                        }
                    });
                    final RadioButton radioButton2 = button;
                    cancelBtn.setOnClickListener(new OnClickListener() {
                        public void onClick(View view) {
                            radioButton2.setChecked(false);
                            MyAdapter.this.mCurrentButton = null;
                            MyAdapter.this.mUnpairDialog.dismiss();
                        }
                    });
                    Builder builder = new Builder(SearchActivity.this);
                    //radioButton2 = button; //!!!!!!!!!!!!! need check
                    builder.setOnKeyListener(new OnKeyListener() {
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (event.getAction() == 1 && keyCode == 4) {
                                radioButton2.setChecked(false);
                                MyAdapter.this.mCurrentButton = null;
                            }
                            return false;
                        }
                    });
                    builder.setView(layout);
                    MyAdapter.this.mUnpairDialog = builder.create();
                    MyAdapter.this.mUnpairDialog.setCanceledOnTouchOutside(false);
                    MyAdapter.this.mUnpairDialog.show();
                }
            });
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
                holder.text = (TextView) view.findViewById(R.id.group_list_item_text);
                holder.radio = (RadioButton) view.findViewById(R.id.device_cancel_paired);
                holder.splitLine = (ImageView) view.findViewById(R.id.device_unpair_line);
                view.setTag(holder);
            }
            BluetoothDevice device = ((DeviceGroup) this.mGroup.get(groupPosition)).getBluetoothDevice(childPosition);
            String name = device.getName();
            holder.text.setText(name);
            if (SearchActivity.this.pairedDevice.equals(((DeviceGroup) SearchActivity.this.groups.get(groupPosition)).name) && TextUtils.equals(device.getAddress(), getCurrentConnectDevice())) {
                holder.text.setText(new StringBuilder(String.valueOf(name)).append("(").append(SearchActivity.this.alreadyConnect).append(")").toString());
            }
            if (SearchActivity.this.pairedDevice.equals(((DeviceGroup) SearchActivity.this.groups.get(groupPosition)).name)) {
                holder.radio.setVisibility(View.VISIBLE);
                holder.splitLine.setVisibility(View.VISIBLE);
            } else {
                holder.radio.setVisibility(View.INVISIBLE);
                holder.splitLine.setVisibility(View.INVISIBLE);
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
            Log.d(LOG_TAG, "get group view");
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
                    Log.i(LOG_TAG, "removeBond return value: " + returnValue);
                    if (!returnValue.booleanValue() || this.mUnpairDialog == null) {
                        this.mUnpairDialog.dismiss();
                        View layout = ((LayoutInflater) SearchActivity.this.mContext.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.unpaired_failed_dialog, null);
                        TextView titleView = (TextView) layout.findViewById(R.id.unpair_failed_title);
                        if (titleView != null) {
                            titleView.setText(SearchActivity.this.unpairFailedTitle);
                        }
                        TextView msgView = (TextView) layout.findViewById(R.id.unpair_failed_message);
                        if (msgView != null) {
                            msgView.setText(SearchActivity.this.unpairFailedMessage);
                        }
                        Button cancelBtn = (Button) layout.findViewById(R.id.cancel_btn);
                        Button okBtn = (Button) layout.findViewById(R.id.ok_btn);
                        if (!(cancelBtn == null || okBtn == null)) {
                            cancelBtn.setText(SearchActivity.this.unpairCancel);
                            okBtn.setText(SearchActivity.this.unpairOk);
                        }
                        Builder builder = new Builder(SearchActivity.this);
                        builder.setOnKeyListener(new OnKeyListener() {
                            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                return (event.getAction() != 1 || keyCode == 4) ? false : false;
                            }
                        });
                        builder.setView(layout);
                        final AlertDialog unpairFailedDialog = builder.create();
                        unpairFailedDialog.setCanceledOnTouchOutside(false);
                        unpairFailedDialog.show();
                        okBtn.setOnClickListener(new OnClickListener() {
                            public void onClick(View view) {
                                Intent i = new Intent("android.settings.BLUETOOTH_SETTINGS");
                                if (i != null) {
                                    SearchActivity.this.mContext.startActivity(i);
                                }
                                unpairFailedDialog.dismiss();
                            }
                        });
                        cancelBtn.setOnClickListener(new OnClickListener() {
                            public void onClick(View view) {
                                unpairFailedDialog.dismiss();
                            }
                        });
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
        @Override
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluethooth_config);

        Log.d(this.LOG_TAG, "onCreate");

        setActionBarUp();
        getConnectionLost();
        Log.d(this.LOG_TAG, "onCreat_getConnectionLost");
        this.mContext = this;
        initStringRes();
        setTitle(this.title);
        getSupportActionBar().setTitle(this.title);
        Log.d(this.LOG_TAG, "onCreat_setTitle");
        if (VERSION.SDK_INT <= 13) {
            getSupportActionBar().setIcon((int) R.drawable.logo1);
        }
        this.mSearchHandler = new SearchHandler(this);
        Log.d(LOG_TAG, "new SearchHandler");
        if (BluetoothTools.mDevices != null) {
            BluetoothTools.mDevices.clear();
        }
        Log.d(LOG_TAG, "BluetoothTools.mDevices.clear()");
        /*
        if (!isEnable()) {
            enableBlueTooth();
        }
        */
        Log.d(LOG_TAG, "pass if (!isEnable())");
        Log.d(this.LOG_TAG, "onCreat_SearchHandler");
        this.mListView = (ExpandableListView) findViewById(R.id.list);
        this.mListView.setGroupIndicator(null);
        this.mNoDataView = (TextView) findViewById(R.id.no_data_view);
        this.mPairedDevice.name = this.pairedDevice;
        this.mFoundDevice.name = this.foundDevice;
        Log.d(this.LOG_TAG, "onCreat_mAdapter");
        this.mAdapter = new MyAdapter(this.mContext, this.groups);
        Log.d(this.LOG_TAG, "onCreat_after mAdapter");
        this.mListView.setAdapter(this.mAdapter);
        //updateDataPaired();
        Log.d(this.LOG_TAG, "onCreat_pass mAdapter");
        //updateList();
        Log.d(this.LOG_TAG, "onCreat_pass updateList");
        this.mListView.setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Log.d(SearchActivity.this.LOG_TAG, "child click group" + groupPosition + "child" + childPosition);
                if (SearchActivity.this.mEnterForConnectionLost) {
                    SearchActivity.this.stopConnectionService();
                }
                SearchActivity.this.connectDecvie(((DeviceGroup) SearchActivity.this.groups.get(groupPosition)).getBluetoothDevice(childPosition));
                return true;
            }
        });
        Log.d(this.LOG_TAG, "onCreat_setOnChildClickListener");

        this.mSearchButton = (Button) findViewById(R.id.SearchActivity_Search_button);
        this.mSearchButton.setText(this.startSearch);
        this.mSearchButton.setOnClickListener(new OnClickListener() {
            @Override
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
        });
        Log.d(this.LOG_TAG, "onCreat_setOnClickListener");
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

    @Override
    protected void onStart() {
        super.onStart();
        //startDiscovery();
        Log.d(this.LOG_TAG, "onCreat_pass onStart");
        if (this.mSearchButton != null) {
            this.mSearchButton.setText(this.stopSearch);
        }
        if (this.mAnimationDrawable != null && !this.mAnimationDrawable.isRunning()) {
            this.mAnimationDrawable.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registBroadcast();
    }

    @Override
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

    @Override
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
        View waitDialog = LayoutInflater.from(this).inflate(R.layout.searchdevice_wait_dialog, null);
        TextView msgTV = (TextView) waitDialog.findViewById(R.id.wait_message);
        ((TextView) waitDialog.findViewById(R.id.wait_title)).setText(diagTitle);
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
        Log.d(this.LOG_TAG, "setActionBarUp");
        if (intent != null) {
            this.mNeedBack = intent.getBooleanExtra(FLAG_NEED_BACK, false);
            Log.d(this.LOG_TAG, "setActionBarUp_intent != null");
        }
        ActionBar actionBar = getSupportActionBar();
        Log.d(this.LOG_TAG, "setActionBarUp_getSupportActionBar");
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
        this.mFilter.addAction(BluetoothDevice.ACTION_FOUND);
        this.mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.mFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.mFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        this.mFilter.addAction(BluetoothConnectionService.INTENT_CONNECTING);
        this.mFilter.addAction(BluetoothConnectionService.INTENT_CONNECT_SUCCESS);
        this.mFilter.addAction(BluetoothConnectionService.INTENT_CONNECT_ERROR);
        registerReceiver(this.mBluetoothReceiver, this.mFilter);
    }


    /* renamed from: com.bosch.diag.activity.SearchActivity.6 */
    class C00936 implements Runnable {
        C00936() {
        }

        public void run() {
            SearchActivity.this.mAnimationDrawable.start();
        }
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        ImageView button = (ImageView) ((LinearLayout) MenuItemCompat.getActionView(menu.findItem(R.id.action_progressbar_refresh))).findViewById(R.id.button_settings);
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
            this.mNoDataView.setVisibility(View.VISIBLE);
        } else {
            this.mNoDataView.setVisibility(View.GONE);
        }
    }

    private void showConnectError() {
        Toast.makeText(this.mContext, this.connectError, Toast.LENGTH_LONG).show();
    }

    protected void cancelPaired(BluetoothDevice bluetoothDevice) {
        Message message = new Message();
        message.what = CMD_CANCEL_PAIRED_DEVICE;
        message.obj = bluetoothDevice;
        this.mSearchHandler.sendMessage(message);
    }
}
