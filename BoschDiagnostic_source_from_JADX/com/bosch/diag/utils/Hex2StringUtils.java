package com.bosch.diag.utils;

import android.support.v4.view.MotionEventCompat;
import java.text.DecimalFormat;

public class Hex2StringUtils {
    public static String toHexString(String s) {
        String str = "";
        for (int i = 0; i < s.length(); i++) {
            str = new StringBuilder(String.valueOf(str)).append(Integer.toHexString(s.charAt(i))).toString();
        }
        return str;
    }

    public static String toStringHex(String s) {
        byte[] baKeyword = new byte[(s.length() / 2)];
        for (int i = 0; i < baKeyword.length; i++) {
            try {
                baKeyword[i] = (byte) (Integer.parseInt(s.substring(i * 2, (i * 2) + 2), 16) & MotionEventCompat.ACTION_MASK);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            return new String(baKeyword, "utf-8");
        } catch (Exception e1) {
            e1.printStackTrace();
            return s;
        }
    }

    public static String toHexOctal(String s) {
        String str = "";
        return Long.toString(Long.parseLong(s, 16));
    }

    public static String hexString2BinaryString(String hexString) {
        if (hexString == null || hexString.length() % 2 != 0) {
            return null;
        }
        String bString = "";
        for (int i = 0; i < hexString.length(); i++) {
            String tmp = "0000" + Integer.toBinaryString(Integer.parseInt(hexString.substring(i, i + 1), 16));
            bString = new StringBuilder(String.valueOf(bString)).append(tmp.substring(tmp.length() - 4)).toString();
        }
        return bString;
    }

    public static byte[] str2Bcd(String asc) {
        int len = asc.length();
        if (len % 2 != 0) {
            asc = "0" + asc;
            len = asc.length();
        }
        byte[] abt = new byte[len];
        if (len >= 2) {
            len /= 2;
        }
        byte[] bbt = new byte[len];
        abt = asc.getBytes();
        int p = 0;
        while (p < asc.length() / 2) {
            int j;
            int k;
            if (abt[p * 2] >= (byte) 48 && abt[p * 2] <= (byte) 57) {
                j = abt[p * 2] - 48;
            } else if (abt[p * 2] < (byte) 97 || abt[p * 2] > (byte) 122) {
                j = (abt[p * 2] - 65) + 10;
            } else {
                j = (abt[p * 2] - 97) + 10;
            }
            if (abt[(p * 2) + 1] >= (byte) 48 && abt[(p * 2) + 1] <= (byte) 57) {
                k = abt[(p * 2) + 1] - 48;
            } else if (abt[(p * 2) + 1] < (byte) 97 || abt[(p * 2) + 1] > (byte) 122) {
                k = (abt[(p * 2) + 1] - 65) + 10;
            } else {
                k = (abt[(p * 2) + 1] - 97) + 10;
            }
            bbt[p] = (byte) ((j << 4) + k);
            p++;
        }
        return bbt;
    }

    public static String bytes2HexString(byte[] b) {
        String ret = "";
        for (byte b2 : b) {
            String hex = Integer.toHexString(b2 & MotionEventCompat.ACTION_MASK);
            if (hex.length() == 1) {
                hex = new StringBuilder(String.valueOf('0')).append(hex).toString();
            }
            ret = new StringBuilder(String.valueOf(ret)).append(hex.toUpperCase()).toString();
        }
        return ret;
    }

    public static String bcd2Str(byte[] bytes) {
        StringBuffer temp = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            temp.append((byte) ((bytes[i] & 240) >>> 4));
            temp.append((byte) (bytes[i] & 15));
        }
        return temp.toString().substring(0, 1).equalsIgnoreCase("0") ? temp.toString().substring(1) : temp.toString();
    }

    public static final String bytesToHexString(byte[] bArray) {
        Exception e;
        StringBuffer sb = null;
        try {
            StringBuffer sb2 = new StringBuffer(bArray.length);
            int i = 0;
            while (i < bArray.length) {
                try {
                    String sTemp = Integer.toHexString(bArray[i] & MotionEventCompat.ACTION_MASK);
                    if (sTemp.length() < 2) {
                        sb2.append(0);
                    }
                    sb2.append(sTemp.toUpperCase());
                    i++;
                } catch (Exception e2) {
                    e = e2;
                    sb = sb2;
                }
            }
            sb = sb2;
        } catch (Exception e3) {
            e = e3;
            e.printStackTrace();
            return sb.toString();
        }
        return sb.toString();
    }

    public static byte[] hexStringToByte(String hex) {
        int len = hex.length() / 2;
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) ((toByte(achar[pos]) << 4) | toByte(achar[pos + 1]));
        }
        return result;
    }

    private static byte toByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static double getDouble(double sourceData, String sf) {
        return Double.parseDouble(new DecimalFormat(sf).format(sourceData));
    }

    public static float getFloatRound(double sourceData, int a) {
        return ((float) ((int) Math.round(((double) a) * sourceData))) / ((float) a);
    }
}
