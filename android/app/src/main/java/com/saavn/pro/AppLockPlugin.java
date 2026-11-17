package com.saavn.pro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "AppLockPlugin")
public class AppLockPlugin extends Plugin {

    @PluginMethod
    public void setLockState(PluginCall call) {
        Boolean locked = call.getBoolean("locked", true);
        String targetPkg = call.getString("targetPackage", "com.whatsapp");

        SharedPreferences prefs = getContext().getSharedPreferences(AppProtectorService.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(AppProtectorService.KEY_LOCKED, locked)
            .putString(AppProtectorService.KEY_TARGET_PKG, targetPkg)
            .apply();

        JSObject ret = new JSObject();
        ret.put("success", true);
        ret.put("locked", locked);
        ret.put("targetPackage", targetPkg);
        call.resolve(ret);
    }

    @PluginMethod
    public void getLockState(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(AppProtectorService.PREFS_NAME, Context.MODE_PRIVATE);
        boolean locked = prefs.getBoolean(AppProtectorService.KEY_LOCKED, true);
        String targetPkg = prefs.getString(AppProtectorService.KEY_TARGET_PKG, "com.whatsapp");

        JSObject ret = new JSObject();
        ret.put("locked", locked);
        ret.put("targetPackage", targetPkg);
        call.resolve(ret);
    }

    @PluginMethod
    public void openAccessibilitySettings(PluginCall call) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        call.resolve();
    }
}
