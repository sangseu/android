package mig0.bosheculogger.utils;

import android.util.Log;
import mig0.bosheculogger.expression.ExpressionParser;
import mig0.bosheculogger.expression.ParserException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class Information extends BaseInformation implements InformationInterface {
    private static final String LOG_TAG = "Information.java";
    public String decimalLength;
    public String expression;
    public String length;
    public String signed;
    public String start;
    public String unit;

    public Information() {
        signed = "N";
    }

    public String toString() {
        return "information name = " + this.nameZh;
    }

    public boolean isSigned() {
        if ("Y".equals(this.signed)) {
            return true;
        }
        return false;
    }

    public String getValue() {
        if (BluetoothTools.cmdArray == null || BluetoothTools.cmdArray.size() == 0) {
            return this.unit;
        }
        int startInt = Integer.parseInt(this.start);
        int lengthInt = Integer.parseInt(this.length);
        int dLength = Integer.parseInt(this.decimalLength);
        StringBuffer sb = new StringBuffer();
        int i;
        StringBuffer stringBuffer;
        int i2;
        if (ConfigManager.getInstance().isBigEndian()) {
            for (i = (startInt + lengthInt) - 1; i >= startInt; i--) {
                stringBuffer = sb;
                i2 = 0;
                stringBuffer.insert(i2, (String) BluetoothTools.cmdArray.get(i));
            }
        } else {
            for (i = startInt; i < startInt + lengthInt; i++) {
                stringBuffer = sb;
                i2 = 0;
                stringBuffer.insert(i2, (String) BluetoothTools.cmdArray.get(i));
            }
        }
        String hexString = sb.toString();
        long intValue = 0;
        try {
            if (isSigned()) {
                intValue = new BigInteger(Hex2StringUtils.hexStringToByte(hexString)).longValue();
                Log.i(LOG_TAG, "isSigned intValue: " + intValue);
            } else {
                intValue = Long.parseLong(hexString, 16);
            }
        } catch (NumberFormatException e1) {
            e1.printStackTrace();
        }
        String value = "";
        try {
            BigDecimal b = new BigDecimal(new ExpressionParser().calculate(this.expression.replace("x", new StringBuilder(String.valueOf(intValue)).toString())));
            if (dLength > 0) {
                return new StringBuilder(String.valueOf(b.setScale(dLength, 4).doubleValue())).append(this.unit).toString();
            } else if (dLength != 0) {
                return value;
            } else {
                return new StringBuilder(String.valueOf(b.setScale(dLength, 4).intValue())).append(this.unit).toString();
            }
        } catch (ParserException e) {
            e.printStackTrace();
            return value;
        }
    }
}
