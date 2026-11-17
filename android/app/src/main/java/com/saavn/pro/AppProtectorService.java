package com.saavn.pro;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import java.util.HashSet;
import java.util.Set;

public class AppProtectorService extends AccessibilityService {

    public static final String PREFS_NAME = "SaavnProSecurity";
    public static final String KEY_LOCKED_SET = "locked_packages_set";
    public static final String KEY_HIDDEN_SET = "hidden_packages_set";
    public static final String KEY_LOCKED = "is_target_locked";
    public static final String KEY_HIDDEN = "is_target_hidden";
    public static final String KEY_TARGET_PKG = "target_package_name";

    private long lastBlockTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> lockedSet = prefs.getStringSet(KEY_LOCKED_SET, new HashSet<>());
        Set<String> hiddenSet = prefs.getStringSet(KEY_HIDDEN_SET, new HashSet<>());

        // Backward compatibility
        boolean isLegacyLocked = prefs.getBoolean(KEY_LOCKED, false);
        boolean isLegacyHidden = prefs.getBoolean(KEY_HIDDEN, false);
        String legacyPkg = prefs.getString(KEY_TARGET_PKG, "");
        if ((isLegacyLocked || isLegacyHidden) && !legacyPkg.isEmpty()) {
            lockedSet = new HashSet<>(lockedSet);
            lockedSet.add(legacyPkg);
        }

        if (lockedSet.isEmpty() && hiddenSet.isEmpty()) return;

        String currentPkg = event.getPackageName().toString();

        // 1. Direct Package Match against all selected apps
        boolean isBlocked = false;
        for (String pkg : lockedSet) {
            if (currentPkg.equalsIgnoreCase(pkg) || currentPkg.startsWith(pkg + ".")) {
                isBlocked = true;
                break;
            }
        }
        if (!isBlocked) {
            for (String pkg : hiddenSet) {
                if (currentPkg.equalsIgnoreCase(pkg) || currentPkg.startsWith(pkg + ".")) {
                    isBlocked = true;
                    break;
                }
            }
        }

        // 2. Settings inspection match
        if (!isBlocked && (currentPkg.equalsIgnoreCase("com.android.settings") || currentPkg.contains("settings") || currentPkg.contains("packageinstaller"))) {
            CharSequence text = event.getText() != null ? event.getText().toString() : "";
            CharSequence contentDesc = event.getContentDescription() != null ? event.getContentDescription().toString() : "";
            String combined = (text + " " + contentDesc).toLowerCase();

            for (String pkg : lockedSet) {
                if (combined.contains(pkg.toLowerCase()) || combined.contains("whatsapp")) {
                    isBlocked = true;
                    break;
                }
            }
            if (!isBlocked) {
                for (String pkg : hiddenSet) {
                    if (combined.contains(pkg.toLowerCase()) || combined.contains("whatsapp")) {
                        isBlocked = true;
                        break;
                    }
                }
            }
        }

        if (isBlocked) {
            long now = System.currentTimeMillis();
            if (now - lastBlockTime > 300) {
                lastBlockTime = now;
                // Force close current screen and send back home
                performGlobalAction(GLOBAL_ACTION_HOME);

                // Open Saavn Pro
                Intent lockIntent = new Intent(this, MainActivity.class);
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                lockIntent.putExtra("BLOCK_REASON", "TARGET_LOCKED");
                startActivity(lockIntent);
            }
        }
    }

    @Override
    public void onInterrupt() {
    }
}
