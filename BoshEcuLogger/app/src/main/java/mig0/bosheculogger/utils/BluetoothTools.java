package mig0.bosheculogger.utils;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothTools {
    public static String LOG_TAG = "BluetoothTools";
    public static final int MESSAGE_CONNECT_ERROR = 3;
    public static final int MESSAGE_CONNECT_LOST = 5;
    public static final int MESSAGE_READ_OBJECT = 4;
    public static final UUID PRIVATE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    /* cmdArray contain 61 bytes data received */
    public static List<String> cmdArray = new ArrayList<String>();
    public static List<String> hardwareArray = new ArrayList<String>();
    public static List<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();
    public static List<String> serialArray = new ArrayList<String>();
    public static List<String> softwareArray = new ArrayList<String>();

    public static String getSerialInfo() {
        if (serialArray == null || serialArray.size() == 0) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (String s : serialArray) {
            sb.append(s);
        }
        String cmd = Hex2StringUtils.toHexOctal(sb.toString());
        Log.d(LOG_TAG, "serial no = " + cmd);
        return cmd;
    }

    public static String getHardwareInfo() {
        if (hardwareArray == null || hardwareArray.size() == 0) {
            return null;
        }
        String end = (String) hardwareArray.get(hardwareArray.size() - 1);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < hardwareArray.size() - 1; i++) {
            sb.append((String) hardwareArray.get(i));
        }
        String prefix = sb.toString();
        return new StringBuilder(String.valueOf(prefix)).append(" V").append(Hex2StringUtils.toHexOctal(end)).toString();
    }

    public static String getSoftwareInfo() {
        if (softwareArray == null || softwareArray.size() < 6) {
            return null;
        }
        String software1 = (String) softwareArray.get(1);
        String software2 = (String) softwareArray.get(2);
        String software3 = (String) softwareArray.get(MESSAGE_CONNECT_ERROR);
        String software4 = (String) softwareArray.get(MESSAGE_READ_OBJECT);
        String software5 = (String) softwareArray.get(MESSAGE_CONNECT_LOST);
        String software0Temp = Hex2StringUtils.toHexOctal((String) softwareArray.get(0));
        String software1Temp = Hex2StringUtils.toHexOctal(software1);
        String software2Temp = Hex2StringUtils.toHexOctal(software2);
        String software3Temp = Hex2StringUtils.toHexOctal(software3);
        String software4Temp = Hex2StringUtils.toStringHex(software4);
        String result = "v" + software0Temp + "." + software1Temp + "." + software2Temp + "." + software3Temp + software4Temp + Hex2StringUtils.toHexOctal(software5);
        Log.d(LOG_TAG, "got software info = " + result);
        return result;
    }
}
