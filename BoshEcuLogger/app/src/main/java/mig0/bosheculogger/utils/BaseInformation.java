package mig0.bosheculogger.utils;

/**
 * Created by mig-7 on 3/28/2017.
 */

public class BaseInformation implements InformationInterface {
    protected String lan;
    public String nameEn;
    public String nameZh;

    public String getName() {
        if ("zh".equals(this.lan)) {
            return this.nameZh;
        }
        return this.nameEn;
    }

    public String getValue() {
        if ("Software Info".equals(this.nameEn)) {
            return BluetoothTools.getSoftwareInfo();
        }
        if ("Hardware Info".equals(this.nameEn)) {
            return BluetoothTools.getHardwareInfo();
        }
        if ("Serial No".equals(this.nameEn)) {
            return BluetoothTools.getSerialInfo();
        }
        return null;
    }

    public void setLanguage(String lan) {
        this.lan = lan;
    }

    public boolean getBooleanValue() {
        return false;
    }
}
