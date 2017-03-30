package com.bosch.diag.utils;

public class Fault extends BaseInformation {
    public String bit;
    public String position;
    public String promptEn;
    public String promptZh;

    public String getPrompt() {
        if (this.lan.equals("zh")) {
            return this.promptZh;
        }
        return this.promptEn;
    }

    public String getValue() {
        return null;
    }

    public boolean getBooleanValue() {
        if (BluetoothTools.cmdArray == null || BluetoothTools.cmdArray.size() == 0) {
            return false;
        }
        int positionReverse = 7 - Integer.parseInt(this.bit);
        char value = Hex2StringUtils.hexString2BinaryString((String) BluetoothTools.cmdArray.get(Integer.parseInt(this.position))).toCharArray()[positionReverse];
        if (value == '1') {
            return true;
        }
        if (value == '0') {
            return false;
        }
        return false;
    }
}
