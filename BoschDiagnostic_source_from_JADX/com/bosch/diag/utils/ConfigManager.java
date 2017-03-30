package com.bosch.diag.utils;

import android.content.Context;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.internal.widget.ActionBarView;
import android.util.Log;
import android.util.Xml;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ConfigManager {
    private static ConfigManager sInstance;
    private String LOG_TAG;
    private ArrayList<InformationInterface> mAdvancedInformations;
    private ArrayList<InformationInterface> mBasicInformations;
    private Context mContext;
    private ArrayList<FaultGroup> mFaultList;
    private boolean mIsBigEndian;
    private Command mReadStatusCmd;
    private int mRequestInterVal;
    private SoftwareInfo mSoftwareInfo;
    private HashMap<String, StringBean> mStrings;

    public static class Strings {
        public static final String ADVANCED = "advanced";
        public static final String ALREADY_CONNECT = "already_connect";
        public static final String APP_NAME = "app_name";
        public static final String AVAILABLE_DEVICES = "available_devices";
        public static final String BASIC = "basic";
        public static final String BT_CONFIG = "bt_config";
        public static final String BT_FOUND_DEVICE = "bt_found_device";
        public static final String BT_OK = "bt_ok";
        public static final String BT_PAIRED_DEVICE = "bt_paired_device";
        public static final String CANCEL_PAIRED = "cancel_paired";
        public static final String CONFIG_VERSION = "config_version";
        public static final String CONNECT_FAIL = "connect_fail";
        public static final String CONNECT_TO = "connect_to";
        public static final String CONNNECT_ERROR = "connect_error";
        public static final String CONTENT_UNAVAILABLE_CONTROLLER = "content_unavailable_controller";
        public static final String DATA_VALIDATE_ERROR = "data_validate_error";
        public static final String DEVICE_CONNCET_ERROR = "device_connect_error";
        public static final String DIAGNOSTIC = "diagnostic";
        public static final String EXIT_MESSAGE = "exit_message";
        public static final String EXIT_NAVBTN = "exit_navbtn";
        public static final String EXIT_POSBTN = "exit_posbtn";
        public static final String EXIT_TITLE = "exit_title";
        public static final String FAQ_NOT_FOUND = "faq_not_found";
        public static final String GET_SOFTWARE_ERROR = "get_software_error";
        public static final String HANDSHAKE_ERROR = "handshake_fail";
        public static final String HELP = "help";
        public static final String NO_DEVICE = "no_device";
        public static final String NO_FAULT = "no_fault";
        public static final String PAIRED_DEVICE = "paired_device";
        public static final String PAIRED_DEVICES = "paired_devices";
        public static final String PDF_NO_APP = "pdf_no_app";
        public static final String REPAIR_GUIDE = "repair_guide";
        public static final String SOFTWARE_VERSION = "software_version";
        public static final String START_SEARCH = "start_search";
        public static final String STOP_SEARCH = "stop_search";
        public static final String TIMEOUT = "timeout";
        public static final String TITLE_MAIN = "title_main";
        public static final String TITLE_PROMPT = "title_prompt";
        public static final String UNPAIR_FAILED_MESSAGE = "unpair_failed_message";
        public static final String UNPAIR_FAILED_TITLE = "unpair_failed_title";
        public static final String UNPAIR_MESSAGE = "unpair_message";
        public static final String UNPAIR_NAVBTN = "unpair_navbtn";
        public static final String UNPAIR_POSBTN = "unpair_posbtn";
        public static final String UNPAIR_TITLE = "unpair_title";
        public static final String WAIT = "wait";
        public static final String WAIT_CONTENT = "wait_content";
        public static final String WRONG_PACKAGE = "wrong_package";
    }

    private ConfigManager(Context context) {
        this.LOG_TAG = "ConfigManager";
        this.mSoftwareInfo = new SoftwareInfo();
        this.mStrings = new HashMap();
        this.mFaultList = new ArrayList();
        this.mBasicInformations = new ArrayList();
        this.mAdvancedInformations = new ArrayList();
        this.mIsBigEndian = false;
        this.mRequestInterVal = 250;
        this.mContext = context;
    }

    public static ConfigManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ConfigManager(context);
        }
        return sInstance;
    }

    public static ConfigManager getInstance() {
        return sInstance;
    }

    public boolean loadConfig(String configName) {
        Log.i(this.LOG_TAG, "try to load " + configName);
        long start = System.currentTimeMillis();
        try {
            InputStream is = this.mContext.getAssets().open(configName);
            if (is == null) {
                return false;
            }
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");
            StringBean stringBean = null;
            FaultGroup fGroup = null;
            boolean basic = false;
            for (int event = parser.getEventType(); event != 1; event = parser.next()) {
                switch (event) {
                    case ActionBarView.DISPLAY_DEFAULT /*0*/:
                        this.mFaultList = new ArrayList();
                        break;
                    case CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER /*2*/:
                        String tagName = parser.getName();
                        if (!"command".equals(tagName)) {
                            if (!"Diagnostic".equals(tagName)) {
                                if (!"FaultGroup".equals(tagName)) {
                                    if (!"Fault".equals(tagName)) {
                                        if (!"BasicInformation".equals(tagName)) {
                                            if (!"BaseInformation".equals(tagName)) {
                                                if (!"Information".equals(tagName)) {
                                                    if (!"BitInfomation".equals(tagName)) {
                                                        if (!"AdvancedInformation".equals(tagName)) {
                                                            if (!"string".equals(parser.getName())) {
                                                                if (!"software".equals(parser.getName())) {
                                                                    if (!"softwareVersion".equals(parser.getName())) {
                                                                        if (!"configVersion".equals(parser.getName())) {
                                                                            if (!"appVersion".equals(parser.getName())) {
                                                                                if (!"endian".equals(tagName)) {
                                                                                    if ("interval".equals(tagName)) {
                                                                                        String interval = parser.getAttributeValue(null, "value");
                                                                                        Log.i(this.LOG_TAG, "loadConfig function interval:" + interval);
                                                                                        if (interval != null) {
                                                                                            this.mRequestInterVal = Integer.parseInt(interval);
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }
                                                                                String endianValue = parser.getAttributeValue(null, "value");
                                                                                if (!"1".equals(endianValue)) {
                                                                                    if ("0".equals(endianValue)) {
                                                                                        this.mIsBigEndian = false;
                                                                                        break;
                                                                                    }
                                                                                }
                                                                                this.mIsBigEndian = true;
                                                                                break;
                                                                            }
                                                                            this.mSoftwareInfo.appVersion = parser.nextText();
                                                                            break;
                                                                        }
                                                                        this.mSoftwareInfo.configVersion = parser.nextText();
                                                                        break;
                                                                    }
                                                                    this.mSoftwareInfo.softwareVersion = parser.nextText();
                                                                    break;
                                                                }
                                                                this.mSoftwareInfo = new SoftwareInfo();
                                                                break;
                                                            }
                                                            stringBean = new StringBean();
                                                            stringBean.id = parser.getAttributeValue(null, "id");
                                                            stringBean.en = parser.getAttributeValue(null, "en");
                                                            stringBean.zh = parser.getAttributeValue(null, "zh");
                                                            break;
                                                        }
                                                        this.mAdvancedInformations = new ArrayList();
                                                        break;
                                                    }
                                                    BitInfomation info = new BitInfomation();
                                                    info.nameEn = parser.getAttributeValue(null, "en");
                                                    info.nameZh = parser.getAttributeValue(null, "zh");
                                                    info.position = parser.getAttributeValue(null, "position");
                                                    info.bit = parser.getAttributeValue(null, "bit");
                                                    info.zh0 = parser.getAttributeValue(null, "zh0");
                                                    info.zh1 = parser.getAttributeValue(null, "zh1");
                                                    info.en0 = parser.getAttributeValue(null, "en0");
                                                    info.en1 = parser.getAttributeValue(null, "en1");
                                                    if (!basic) {
                                                        this.mAdvancedInformations.add(info);
                                                        break;
                                                    }
                                                    this.mBasicInformations.add(info);
                                                    break;
                                                }
                                                Information info2 = new Information();
                                                info2.nameEn = parser.getAttributeValue(null, "en");
                                                info2.nameZh = parser.getAttributeValue(null, "zh");
                                                info2.length = parser.getAttributeValue(null, "length");
                                                info2.start = parser.getAttributeValue(null, "start");
                                                info2.expression = parser.getAttributeValue(null, "expression");
                                                info2.decimalLength = parser.getAttributeValue(null, "decimalLength");
                                                info2.unit = parser.getAttributeValue(null, "unit");
                                                info2.signed = parser.getAttributeValue(null, "signed");
                                                if (!basic) {
                                                    this.mAdvancedInformations.add(info2);
                                                    break;
                                                }
                                                this.mBasicInformations.add(info2);
                                                break;
                                            }
                                            BaseInformation info3 = new BaseInformation();
                                            info3.nameEn = parser.getAttributeValue(null, "en");
                                            info3.nameZh = parser.getAttributeValue(null, "zh");
                                            if (!basic) {
                                                this.mAdvancedInformations.add(info3);
                                                break;
                                            }
                                            this.mBasicInformations.add(info3);
                                            break;
                                        }
                                        this.mBasicInformations = new ArrayList();
                                        basic = true;
                                        break;
                                    }
                                    Fault f = new Fault();
                                    f.nameEn = parser.getAttributeValue(null, "en");
                                    f.nameZh = parser.getAttributeValue(null, "zh");
                                    f.position = parser.getAttributeValue(null, "position");
                                    f.bit = parser.getAttributeValue(null, "bit");
                                    f.promptEn = parser.getAttributeValue(null, "prompten");
                                    f.promptZh = parser.getAttributeValue(null, "promptzh");
                                    fGroup.addFault(f);
                                    break;
                                }
                                fGroup = new FaultGroup();
                                String en = parser.getAttributeValue(null, "en");
                                String zh = parser.getAttributeValue(null, "zh");
                                fGroup.en = en;
                                fGroup.zh = zh;
                                this.mFaultList.add(fGroup);
                                break;
                            }
                        }
                        Command command = new Command();
                        command.name = parser.getAttributeValue(null, "name");
                        command.cmd = parser.getAttributeValue(null, "cmd");
                        command.length = parser.getAttributeValue(null, "length");
                        command.id = parser.getAttributeValue(null, "id");
                        command.checksum = parser.getAttributeValue(null, "checksum");
                        this.mReadStatusCmd = command;
                        break;
                        break;
                    case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER /*3*/:
                        if (!"string".equals(parser.getName())) {
                            if (!"BasicInformation".equals(parser.getName())) {
                                "AdvancedInformation".equals(parser.getName());
                                break;
                            }
                            basic = false;
                            break;
                        }
                        String id = stringBean.id;
                        this.mStrings.put(id, stringBean);
                        break;
                }
            }
            long cost = System.currentTimeMillis() - start;
            Log.d(this.LOG_TAG, "load config cost " + cost + "ms");
            return true;
        } catch (IOException e) {
            Log.d(this.LOG_TAG, "IOException");
            e.printStackTrace();
            return false;
        } catch (XmlPullParserException e2) {
            Log.d(this.LOG_TAG, "XmlPullParserException");
            e2.printStackTrace();
            return false;
        }
    }

    public String getString(String lan, String id) {
        StringBean sb = (StringBean) this.mStrings.get(id);
        if (sb == null) {
            return "";
        }
        if ("zh".equals(lan)) {
            return sb.zh;
        }
        return sb.en;
    }

    public SoftwareInfo getSoftwareInfo() {
        return this.mSoftwareInfo;
    }

    public Command getReadStatusCmd() {
        return this.mReadStatusCmd;
    }

    public ArrayList<FaultGroup> getFaultList() {
        return this.mFaultList;
    }

    public ArrayList<InformationInterface> getBasicInformationList() {
        return this.mBasicInformations;
    }

    public ArrayList<InformationInterface> getAdvancedInformationList() {
        return this.mAdvancedInformations;
    }

    public boolean isBigEndian() {
        return this.mIsBigEndian;
    }

    public int requestInterval() {
        return this.mRequestInterVal;
    }
}
