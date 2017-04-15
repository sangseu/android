package mig0.bosheculogger.utils;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by mig-7 on 3/28/2017.
 */

public class ConfigManager {
    private static ConfigManager sInstance;
    private String LOG_TAG = "ConfigManager";

    private Context mContext;

    private ArrayList<InformationInterface> mAdvancedInformations;
    private ArrayList<InformationInterface> mBasicInformations;
    private ArrayList<FaultGroup> mFaultList;
    private boolean mIsBigEndian;
    private Command mReadStatusCmd;
    private int mRequestInterVal;
    private SoftwareInfo mSoftwareInfo;
    private HashMap<String, StringBean> mStrings = new HashMap<String, StringBean>();;

    private ConfigManager(Context paramContext)
    {
        this.mContext = paramContext;
    }

    public static ConfigManager getInstance()
    {
        return sInstance;
    }

    public static ConfigManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ConfigManager(context);
        }
        return sInstance;
    }

    public ArrayList<InformationInterface> getAdvancedInformationList()
    {
        return this.mAdvancedInformations;
    }

    public ArrayList<InformationInterface> getBasicInformationList()
    {
        return this.mBasicInformations;
    }

    public ArrayList<FaultGroup> getFaultList()
    {
        return this.mFaultList;
    }

    public Command getReadStatusCmd()
    {
        return this.mReadStatusCmd;
    }

    public SoftwareInfo getSoftwareInfo()
    {
        return this.mSoftwareInfo;
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


    boolean isBigEndian()
    {
        return this.mIsBigEndian;
    }

    /* parse config in *.xml to get data structure */
    public boolean loadConfig(String configName)
    {
        Log.i(this.LOG_TAG, "try to load " + configName);
        long start = System.currentTimeMillis();
        try {
            InputStream is = mContext.getAssets().open(configName);
            if (is == null) {
                return false;
            }
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "utf-8");


            StringBean stringBean = null;
            FaultGroup fGroup = null;
            boolean parsingBasicInformation = true;

            mAdvancedInformations = new ArrayList<InformationInterface>();
            mBasicInformations = new ArrayList<InformationInterface>();
            mFaultList = new ArrayList<FaultGroup>();
            mIsBigEndian = false;
            mRequestInterVal = 250;
            mSoftwareInfo = new SoftwareInfo();

            for (int event = parser.getEventType();
                 event != XmlPullParser.END_DOCUMENT;/*1*/
                 event = parser.next())
            {
                switch (event) {
                    case XmlPullParser.START_DOCUMENT /*0*/:
                        break;
                    case XmlPullParser.START_TAG /*2*/: {
                        String tagName = parser.getName();

                        /* diag_cfg_string */
                        if ("string".equals(tagName)) {
                            stringBean = new StringBean();
                            stringBean.id = parser.getAttributeValue(null, "id");
                            stringBean.en = parser.getAttributeValue(null, "en");
                            stringBean.zh = parser.getAttributeValue(null, "zh");
                            break;
                        }

                        /* diag_cfg_v255.6.1 */
                        if("software".equals(tagName)){
                            Log.d(LOG_TAG, "PARSE software");
                            break;
                        }
                            if("softwareVersion".equals(tagName)){
                                mSoftwareInfo.softwareVersion = parser.nextText();
                                Log.d(LOG_TAG, "PARSE softwareVersion");
                                break;
                            }
                            if("configVersion".equals(tagName)){
                                mSoftwareInfo.configVersion = parser.nextText();
                                Log.d(LOG_TAG, "PARSE configVersion");
                                break;
                            }
                            if("appVersion".equals(tagName)){
                                mSoftwareInfo.appVersion = parser.nextText();
                                Log.d(LOG_TAG, "PARSE appVersion");
                                break;
                            }

                        if ("endian".equals(tagName)) {
                            String endianValue = parser.getAttributeValue(null, "value");
                            if ("1".equals(endianValue)) {
                                mIsBigEndian = false;
                            }
                            else mIsBigEndian = false;
                            Log.d(LOG_TAG, "PARSE endian");
                            break;
                        }

                        if ("interval".equals(tagName)) {
                            String interval = parser.getAttributeValue(null, "value");
                            if (interval != null) {
                                mRequestInterVal = Integer.parseInt(interval);
                                Log.d(LOG_TAG, "PARSE interval");
                                break;
                            }
                        }

                        if ("command".equals(tagName)) {
                            Command command = new Command();
                            command.name = parser.getAttributeValue(null, "name");
                            command.cmd = parser.getAttributeValue(null, "cmd");
                            command.length = parser.getAttributeValue(null, "length");
                            command.id = parser.getAttributeValue(null, "id");
                            command.checksum = parser.getAttributeValue(null, "checksum");
                            mReadStatusCmd = command;
                            Log.d(LOG_TAG, "PARSE command");
                            break;
                        }

                        if ("Diagnostic".equals(tagName)) {
                            Log.d(LOG_TAG, "PARSE Diagnostic");
                            break;
                        }
                            /* New FaultGroup() every START_TAG FaultGroup */
                            if ("FaultGroup".equals(tagName)) {
                                fGroup = new FaultGroup();
                                String en = parser.getAttributeValue(null, "en");
                                String zh = parser.getAttributeValue(null, "zh");
                                fGroup.en = en;
                                fGroup.zh = zh;
                                Log.d(LOG_TAG, "PARSE FaultGroup");
                                break;
                            }
                            /* Parse Fault, add to FaultGroup */
                            if ("Fault".equals(tagName)) {
                                Fault f = new Fault();
                                f.nameEn = parser.getAttributeValue(null, "en");
                                f.nameZh = parser.getAttributeValue(null, "zh");
                                f.position = parser.getAttributeValue(null, "position");
                                f.bit = parser.getAttributeValue(null, "bit");
                                f.promptEn = parser.getAttributeValue(null, "prompten");
                                f.promptZh = parser.getAttributeValue(null, "promptzh");
                                fGroup.addFault(f);
                                Log.d(LOG_TAG, "PARSE Fault");
                                break;
                            }

                        if ("BasicInformation".equals(tagName)) {
                            parsingBasicInformation = true;
                            Log.d(LOG_TAG, "PARSE BasicInformation");
                            break;
                        }

                        if ("AdvancedInformation".equals(tagName)) {
                            parsingBasicInformation = false;
                            Log.d(LOG_TAG, "PARSE AdvancedInformation");
                            break;
                        }

                            /* Information and BitInformation are part of BasicInformation and AdvancedInformation
                            * so need check parsingBasicInformation to know we parsing Basic or Advance */
                            if ("Information".equals(tagName)) {
                                Information info2 = new Information();
                                info2.nameEn = parser.getAttributeValue(null, "en");
                                info2.nameZh = parser.getAttributeValue(null, "zh");
                                info2.length = parser.getAttributeValue(null, "length");
                                info2.start = parser.getAttributeValue(null, "start");
                                info2.expression = parser.getAttributeValue(null, "expression");
                                info2.decimalLength = parser.getAttributeValue(null, "decimalLength");
                                info2.unit = parser.getAttributeValue(null, "unit");
                                info2.signed = parser.getAttributeValue(null, "signed");
                                if(parsingBasicInformation) {
                                    mBasicInformations.add(info2);
                                    Log.d(LOG_TAG, "PARSE BasicInformation_Information");
                                    break;
                                }
                                else {
                                    mAdvancedInformations.add(info2);
                                    Log.d(LOG_TAG, "PARSE AdvancedInformations_Information");
                                    break;
                                }
                            }
                            if ("BitInfomation".equals(tagName)) {
                                BitInfomation info = new BitInfomation();
                                info.nameEn = parser.getAttributeValue(null, "en");
                                info.nameZh = parser.getAttributeValue(null, "zh");
                                info.position = parser.getAttributeValue(null, "position");
                                info.bit = parser.getAttributeValue(null, "bit");
                                info.zh0 = parser.getAttributeValue(null, "zh0");
                                info.zh1 = parser.getAttributeValue(null, "zh1");
                                info.en0 = parser.getAttributeValue(null, "en0");
                                info.en1 = parser.getAttributeValue(null, "en1");
                                if(parsingBasicInformation) {
                                    mBasicInformations.add(info);
                                    Log.d(LOG_TAG, "PARSE BasicInformation_BitInformation");
                                    break;
                                }
                                else {
                                    mAdvancedInformations.add(info);
                                    Log.d(LOG_TAG, "PARSE AdvancedInformations_BitInformation");
                                    break;
                                }
                            }
                    }
                    case XmlPullParser.END_TAG /*3*/: {
                        String tagName = parser.getName();

                        if("string".equals(tagName)) {
                            mStrings.put(stringBean.id, stringBean);
                            Log.d(LOG_TAG, "END_TAG string");
                            break;
                        }

                        if ("software".equals(tagName)) {
                            Log.d(LOG_TAG, "END_TAG software");
                            break;
                        }

                            /* end of FaultGroup, add FaultGroup with child (Fault) to FaultList */
                        if ("FaultGroup".equals(tagName)) {
                            mFaultList.add(fGroup);
                            Log.d(LOG_TAG, "END_TAG FaultGroup");
                            break;
                        }

                        if ("Diagnostic".equals(tagName)) {
                            Log.d(LOG_TAG, "END_TAG Diagnostic");
                            break;
                        }

                        if ("BasicInformation".equals(tagName)) {
                            parsingBasicInformation = false;
                            Log.d(LOG_TAG, "END_TAG BasicInformation");
                            break;
                        }

                        if ("AdvancedInformation".equals(tagName)) {
                            parsingBasicInformation = true;
                            Log.d(LOG_TAG, "END_TAG AdvancedInformation");
                            break;
                        }
                    }
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

    public int requestInterval()
    {
        return this.mRequestInterVal;
    }

    /*message ID using to search in *.xml dictionary*/
    public static class Strings
    {
        public static final String ADVANCED = "advanced";
        public static final String BASIC = "basic";
        public static final String DIAGNOSTIC = "diagnostic";

        public static final String ALREADY_CONNECT = "already_connect";
        public static final String APP_NAME = "app_name";
        public static final String AVAILABLE_DEVICES = "available_devices";
        public static final String BT_CONFIG = "bt_config";
        public static final String BT_FOUND_DEVICE = "bt_found_device";
        public static final String BT_OK = "bt_ok";
        public static final String BT_PAIRED_DEVICE = "bt_paired_device";
        public static final String CANCEL_PAIRED = "cancel_paired";
        public static final String CONFIG_VERSION = "config_version";
        public static final String CONNECT_FAIL = "connect_fail";
        public static final String CONNECT_TO = "connect_to";
        public static final String CONNNECT_ERROR = "connect_error";
        public static final String RE_CONNECT_OK = "reconnect_ok";
        public static final String CONTENT_UNAVAILABLE_CONTROLLER = "content_unavailable_controller";
        public static final String DATA_VALIDATE_ERROR = "data_validate_error";
        public static final String DEVICE_CONNCET_ERROR = "device_connect_error";
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

        public static final String HELP_TITLE = "help_title";
        public static final String HELP_MESS = "help_mess";
    }
}
