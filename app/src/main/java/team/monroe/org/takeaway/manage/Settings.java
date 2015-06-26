package team.monroe.org.takeaway.manage;

import org.monroe.team.android.box.services.SettingManager;

public final class Settings {
    private Settings() {}
    public static SettingManager.SettingItem<Boolean> MODE_OFFLINE = new SettingManager.Flag("offline_mode", false);
}
