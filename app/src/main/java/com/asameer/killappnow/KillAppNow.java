package com.asameer.killappnow;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import android.content.pm.ApplicationInfo;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class KillAppNow implements IXposedHookLoadPackage {

    private final List<String> mKillIgnoreList = new ArrayList<>(Arrays.asList(
            "com.android.systemui",
            "com.google.android.systemui"
    ));

    private Context mContext;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("android"))
            return;

        mContext = (Context) XposedHelpers.callMethod(
                XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null),
                        "currentActivityThread"), "getSystemContext");

        XposedHelpers.findAndHookMethod("com.android.server.policy.PhoneWindowManager",
                lpparam.classLoader, "interceptKeyBeforeQueueing", KeyEvent.class,
                int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        KeyEvent event = (KeyEvent) param.args[0];
                        int keyCode = event.getKeyCode();

                        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                            long downTime = event.getDownTime();
                            long eventTime = event.getEventTime();

                            if (eventTime - downTime > 300) {
                                killForegroundApp();
                            }
                        }
                    }
                });
    }
    private void killForegroundApp() {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        mainHandler.post(() -> {
            try {
                ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();

                String foregroundApp = null;
                for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
                    if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        foregroundApp = processInfo.processName;
                        break;
                    }
                }

                if (foregroundApp != null && !mKillIgnoreList.contains(foregroundApp) && !foregroundApp.equals(getDefaultLauncherPackageName())) {
                    XposedHelpers.callMethod(am, "forceStopPackage", foregroundApp);
                    String appLabel = getApplicationLabel(foregroundApp, mContext.getPackageManager());
                    Toast.makeText(mContext, "Killed: " + appLabel, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "Nothing to kill.", Toast.LENGTH_SHORT).show();
                }
            } catch (Throwable t) {
                Log.e("KillAppNow", "Error in killForegroundApp", t);
            }
        });
    }

    // Helper method to get the default launcher package name
    private String getDefaultLauncherPackageName() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        return null;
    }

    private String getApplicationLabel(String packageName, PackageManager pm) {
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return (String) pm.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            return packageName; // Fallback to package name if the app label is not found
        }
    }
}
