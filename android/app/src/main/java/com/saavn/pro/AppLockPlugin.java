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
    public void setSecurityConfig(PluginCall call) {
        Boolean locked = call.getBoolean("locked", false);
        Boolean hidden = call.getBoolean("hidden", false);
        String targetPkg = call.getString("targetPackage", "com.whatsapp");

        SharedPreferences prefs = getContext().getSharedPreferences(AppProtectorService.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(AppProtectorService.KEY_LOCKED, locked)
            .putBoolean(AppProtectorService.KEY_HIDDEN, hidden)
            .putString(AppProtectorService.KEY_TARGET_PKG, targetPkg)
            .apply();

        JSObject ret = new JSObject();
        ret.put("success", true);
        ret.put("locked", locked);
        ret.put("hidden", hidden);
        ret.put("targetPackage", targetPkg);
        call.resolve(ret);
    }

    @PluginMethod
    public void getSecurityConfig(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(AppProtectorService.PREFS_NAME, Context.MODE_PRIVATE);
        boolean locked = prefs.getBoolean(AppProtectorService.KEY_LOCKED, false);
        boolean hidden = prefs.getBoolean(AppProtectorService.KEY_HIDDEN, false);
        String targetPkg = prefs.getString(AppProtectorService.KEY_TARGET_PKG, "com.whatsapp");

        JSObject ret = new JSObject();
        ret.put("locked", locked);
        ret.put("hidden", hidden);
        ret.put("targetPackage", targetPkg);
        call.resolve(ret);
    }

    @PluginMethod
    public void launchTargetApp(PluginCall call) {
        String targetPkg = call.getString("targetPackage", "com.whatsapp");
        try {
            Intent launchIntent = getContext().getPackageManager().getLaunchIntentForPackage(targetPkg);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(launchIntent);
                call.resolve();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        call.reject("Could not launch target package: " + targetPkg);
    }

    @PluginMethod
    public void openAccessibilitySettings(PluginCall call) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        call.resolve();
    }
}
