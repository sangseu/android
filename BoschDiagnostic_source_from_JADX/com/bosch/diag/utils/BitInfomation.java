package com.bosch.diag.utils;

public class BitInfomation extends Fault {
    public String en0;
    public String en1;
    public String zh0;
    public String zh1;

    public String toString() {
        return "BitInfomation name = " + this.nameZh;
    }

    public String getValue() {
        if (BluetoothTools.cmdArray == null || BluetoothTools.cmdArray.size() == 0) {
            return "";
        }
        String result = "";
        int positionReverse = 7 - Integer.parseInt(this.bit);
        char value = Hex2StringUtils.hexString2BinaryString((String) BluetoothTools.cmdArray.get(Integer.parseInt(this.position))).toCharArray()[positionReverse];
        if (value == '1') {
            if ("zh".equals(this.lan)) {
                return this.zh1;
            }
            return this.en1;
        } else if (value != '0') {
            return result;
        } else {
            if ("zh".equals(this.lan)) {
                return this.zh0;
            }
            return this.en0;
        }
    }
}
