package ax.nd.universalauth.xposed;

import static ax.nd.universalauth.xposed.common.XposedConstants.ACTION_UNLOCK_DEVICE;
import static ax.nd.universalauth.xposed.common.XposedConstants.EXTRA_UNLOCK_MODE;
import static ax.nd.universalauth.xposed.common.XposedConstants.MODE_UNLOCK_FADING;
import static ax.nd.universalauth.xposed.common.XposedConstants.PERMISSION_UNLOCK_DEVICE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Module implements IXposedHookLoadPackage {
    private static final String TAG = "XposedUniversalAuth";

    /**
     * This method is called when an app is loaded. It's called very early, even before
     * {@link Application#onCreate} is called.
     * Modules can set up their app-specific hooks here.
     *
     * @param lpparam Information about the app.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Hook SystemUi
        if (Objects.equals(lpparam.packageName, "com.android.systemui")) {
            // Hook com.android.systemui.statusbar.phone.StatusBar.start
            XposedHelpers.findAndHookMethod(
                    "com.android.systemui.statusbar.phone.StatusBar",
                    lpparam.classLoader,
                    "start",
                    new XC_MethodHook() {
                        /**
                         * Called after the invocation of the method.
                         *
                         * <p>You can use {@link MethodHookParam#setResult} and {@link MethodHookParam#setThrowable}
                         * to modify the return value of the original method.
                         *
                         * <p>Note that implementations shouldn't call {@code super(param)}, it's not necessary.
                         *
                         * @param param Information about the method call.
                         * @throws Throwable Everything the callback throws is caught and logged.
                         */
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                            XposedBridge.log("FaceUnlock hook pre-install!");
                            hookStatusBar(param);
//                            XposedBridge.log("FaceUnlock hook installed!");
                        }
                    }
            );
        }
    }


    private <T extends AccessibleObject> T asAccessible(T a) {
        a.setAccessible(true);
        return a;
    }

    private void hookStatusBar(XC_MethodHook.MethodHookParam param) throws Throwable {
        Object statusBar = param.thisObject;
        Class<?> statusBarClass = statusBar.getClass();
        Context context = (Context) asAccessible(statusBarClass.getField("mContext")).get(statusBar);
        Object biometricUnlockController = asAccessible(statusBarClass.getDeclaredField("mBiometricUnlockController")).get(statusBar);
        Method startWakeAndUnlock = asAccessible(biometricUnlockController
                .getClass()
                .getDeclaredMethod("startWakeAndUnlock", int.class));

        BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Unlocking device...");
                try {
                    int unlockMode = intent.getIntExtra(EXTRA_UNLOCK_MODE, MODE_UNLOCK_FADING);
                    startWakeAndUnlock.invoke(biometricUnlockController, unlockMode);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    Log.e(TAG, "Failed to unlock device!", e);
                }
            }
        };

        // Register broadcastReceiver for IPC
        // TODO Handle multiple users
        IntentFilter intentFilter = new IntentFilter(ACTION_UNLOCK_DEVICE);
        context.registerReceiver(unlockReceiver, intentFilter, PERMISSION_UNLOCK_DEVICE, null);
    }
}
