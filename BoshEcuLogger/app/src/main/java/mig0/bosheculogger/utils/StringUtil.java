package mig0.bosheculogger.utils;

import mig0.bosheculogger.utils.ConfigManager.Strings;

public final class StringUtil {
    public static String[] getHelpItems(String lan) {
        if (ConfigManager.getInstance() == null) {
            return null;
        }
        return new String[]{new StringBuilder(String.valueOf(ConfigManager.getInstance().getString(lan, Strings.SOFTWARE_VERSION))).append(":").append(ConfigManager.getInstance().getSoftwareInfo().appVersion).toString(), new StringBuilder(String.valueOf(ConfigManager.getInstance().getString(lan, ConfigManager.Strings.CONFIG_VERSION))).append(":").append(ConfigManager.getInstance().getSoftwareInfo().configVersion).toString(), ConfigManager.getInstance().getString(lan, ConfigManager.Strings.BT_CONFIG), ConfigManager.getInstance().getString(lan, Strings.REPAIR_GUIDE)};
    }
}
