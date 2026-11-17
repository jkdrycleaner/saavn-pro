package com.saavn.pro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;

@CapacitorPlugin(name = "AppLockPlugin")
public class AppLockPlugin extends Plugin {

    @PluginMethod
    public void getInstalledApps(PluginCall call) {
        try {
            PackageManager pm = getContext().getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            JSArray appList = new JSArray();

            for (ApplicationInfo packageInfo : packages) {
                Intent launchIntent = pm.getLaunchIntentForPackage(packageInfo.packageName);
                if (launchIntent != null && !packageInfo.packageName.equals(getContext().getPackageName())) {
                    String appName = pm.getApplicationLabel(packageInfo).toString();
                    JSObject appObj = new JSObject();
                    appObj.put("name", appName);
                    appObj.put("package", packageInfo.packageName);
                    appList.put(appObj);
                }
            }

            JSObject ret = new JSObject();
            ret.put("apps", appList);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Failed to list installed apps: " + e.getMessage());
        }
    }

    @PluginMethod
    public void saveMultiAppSettings(PluginCall call) {
        try {
            JSArray lockedArr = call.getArray("lockedPackages");
            JSArray hiddenArr = call.getArray("hiddenPackages");

            Set<String> lockedSet = new HashSet<>();
            if (lockedArr != null) {
                for (int i = 0; i < lockedArr.length(); i++) {
                    lockedSet.add(lockedArr.getString(i));
                }
            }

            Set<String> hiddenSet = new HashSet<>();
            if (hiddenArr != null) {
                for (int i = 0; i < hiddenArr.length(); i++) {
                    hiddenSet.add(hiddenArr.getString(i));
                }
            }

            SharedPreferences prefs = getContext().getSharedPreferences(AppProtectorService.PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                .putStringSet(AppProtectorService.KEY_LOCKED_SET, lockedSet)
                .putStringSet(AppProtectorService.KEY_HIDDEN_SET, hiddenSet)
                .apply();

            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("lockedCount", lockedSet.size());
            ret.put("hiddenCount", hiddenSet.size());
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Error saving multi app settings: " + e.getMessage());
        }
    }

    @PluginMethod
    public void getMultiAppSettings(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(AppProtectorService.PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> lockedSet = prefs.getStringSet(AppProtectorService.KEY_LOCKED_SET, new HashSet<>());
        Set<String> hiddenSet = prefs.getStringSet(AppProtectorService.KEY_HIDDEN_SET, new HashSet<>());

        JSArray lockedArr = new JSArray();
        for (String s : lockedSet) lockedArr.put(s);

        JSArray hiddenArr = new JSArray();
        for (String s : hiddenSet) hiddenArr.put(s);

        JSObject ret = new JSObject();
        ret.put("lockedPackages", lockedArr);
        ret.put("hiddenPackages", hiddenArr);
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
