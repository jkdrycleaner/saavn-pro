package com.saavn.pro;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class AppProtectorService extends AccessibilityService {

    public static final String PREFS_NAME = "SaavnProSecurity";
    public static final String KEY_LOCKED = "is_target_locked";
    public static final String KEY_HIDDEN = "is_target_hidden";
    public static final String KEY_TARGET_PKG = "target_package_name";

    private long lastBlockTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isLocked = prefs.getBoolean(KEY_LOCKED, false);
        boolean isHidden = prefs.getBoolean(KEY_HIDDEN, false);
        String targetPkg = prefs.getString(KEY_TARGET_PKG, "com.whatsapp");

        if (!isLocked && !isHidden) return;

        String currentPkg = event.getPackageName().toString();

        // 1. Direct Target Match (exact or sub-package)
        boolean isTarget = currentPkg.equalsIgnoreCase(targetPkg) || currentPkg.startsWith(targetPkg + ".");

        // 2. Settings search / app details inspection match
        boolean isSettingsInterference = false;
        if (currentPkg.equalsIgnoreCase("com.android.settings") || currentPkg.contains("settings") || currentPkg.contains("packageinstaller")) {
            CharSequence text = event.getText() != null ? event.getText().toString() : "";
            CharSequence contentDesc = event.getContentDescription() != null ? event.getContentDescription().toString() : "";
            String combined = (text + " " + contentDesc).toLowerCase();
            if (combined.contains("whatsapp") || combined.contains(targetPkg.toLowerCase())) {
                isSettingsInterference = true;
            }
        }

        if (isTarget || isSettingsInterference) {
            long now = System.currentTimeMillis();
            if (now - lastBlockTime > 400) { // debounce duplicate events
                lastBlockTime = now;
                performGlobalAction(GLOBAL_ACTION_HOME);

                Intent lockIntent = new Intent(this, MainActivity.class);
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                lockIntent.putExtra("BLOCK_REASON", "TARGET_LOCKED");
                startActivity(lockIntent);
            }
        }
    }

    @Override
    public void onInterrupt() {
    }
}
