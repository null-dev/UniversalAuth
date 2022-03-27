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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ax.nd.universalauth.xposed.common.XposedConstants;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Module implements IXposedHookLoadPackage {
    private static final String TAG = "XposedUniversalAuth";
    private static final String STATUS_BAR_CLASS = "com.android.systemui.statusbar.phone.StatusBar";
    private static final String SYSTEM_UI_CLASS = "com.android.systemui.SystemUI";
    private static final String KEYGUARD_UPDATE_MONITOR_CLASS = "com.android.keyguard.KeyguardUpdateMonitor";
    private static final String STATUS_BAR_STATE_CONTROLLER_CLASS = "com.android.systemui.plugins.statusbar.StatusBarStateController";
    private static final String KEYGUARD_UPDATE_MONITOR_LAST_MODE = "ax.nd.universalauth.last-mode";

    // com.android.systemui.statusbar.StatusBarState.SHADE_LOCKED
    private static final int SHADE_LOCKED = 2;

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
                    STATUS_BAR_CLASS,
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
                            hookStatusBar(lpparam.classLoader, param);
//                            XposedBridge.log("FaceUnlock hook installed!");
                        }
                    }
            );

            // Hook com.android.keyguard.KeyguardUpdateMonitor.updateFaceListeningState
            try {
                addHookEarlyUnlock(lpparam);
            } catch (Throwable t) {
                XposedBridge.log("Failed to hook early unlock, early unlock hook will not work:");
                XposedBridge.log(t);
            }
        }
    }

    private void addHookEarlyUnlock(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Class<?> kumClazz = lpparam.classLoader.loadClass(KEYGUARD_UPDATE_MONITOR_CLASS);
        Field mStatusBarStateControllerField = asAccessible(kumClazz.getDeclaredField("mStatusBarStateController"));
        Field mKeyguardIsVisibleField = asAccessible(kumClazz.getDeclaredField("mKeyguardIsVisible"));
        Field mDeviceInteractiveField = asAccessible(kumClazz.getDeclaredField("mDeviceInteractive"));
        Field mGoingToSleepField = asAccessible(kumClazz.getDeclaredField("mGoingToSleep"));
        Field mContextField = asAccessible(kumClazz.getDeclaredField("mContext"));

        Class<?> sbscClazz = lpparam.classLoader.loadClass(STATUS_BAR_STATE_CONTROLLER_CLASS);
        Method getStateMethod = asAccessible(sbscClazz.getDeclaredMethod("getState"));

        XposedHelpers.findAndHookMethod(
                kumClazz,
                "updateFaceListeningState",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object kum = param.thisObject;
                        Object sbsc = mStatusBarStateControllerField.get(kum);
                        boolean mKeyguardIsVisible = mKeyguardIsVisibleField.getBoolean(kum);
                        boolean mDeviceInteractive = mDeviceInteractiveField.getBoolean(kum);
                        boolean mGoingToSleep = mGoingToSleepField.getBoolean(kum);
                        int sbscState = (int) getStateMethod.invoke(sbsc);

                        // From: com.android.keyguard.KeyguardUpdateMonitor.shouldListenForFace
                        final boolean statusBarShadeLocked = sbscState == SHADE_LOCKED;
                        final boolean awakeKeyguard = mKeyguardIsVisible && mDeviceInteractive && !mGoingToSleep
                                && !statusBarShadeLocked;

                        Object prevAwakeKeyguard = XposedHelpers.setAdditionalInstanceField(kum, KEYGUARD_UPDATE_MONITOR_LAST_MODE, awakeKeyguard);

                        if (!Objects.equals(prevAwakeKeyguard, awakeKeyguard)) {
                            Context mContext = (Context) mContextField.get(kum);
                            hookEarlyUnlock(mContext, awakeKeyguard);
                        }
                    }
                }
        );
    }

    private void hookEarlyUnlock(Context context, boolean newAwakeKeyguard) throws Throwable {
        context.sendBroadcast(new Intent(XposedConstants.ACTION_EARLY_UNLOCK)
                .putExtra(XposedConstants.EXTRA_EARLY_UNLOCK_MODE, newAwakeKeyguard));
    }

    private <T extends AccessibleObject> T asAccessible(T a) {
        a.setAccessible(true);
        return a;
    }

    private String dumpArray(Object[] obj) {
        return dumpStream(Arrays.stream(obj));
    }

    private String dumpStream(Stream<Object> stream) {
        return stream.map(Object::toString).collect(Collectors.joining(",\n"));
    }

    private void hookStatusBar(ClassLoader classLoader, XC_MethodHook.MethodHookParam param) throws Throwable {
        Object statusBar = param.thisObject;
        Class<?> systemUiClass = classLoader.loadClass(SYSTEM_UI_CLASS);
        Class<?> statusBarClass = classLoader.loadClass(STATUS_BAR_CLASS);
        Context context = (Context) asAccessible(systemUiClass.getDeclaredField("mContext")).get(statusBar);

        UnlockMethod method = hookStatusBarBiometricUnlock(statusBar, statusBarClass);

        BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Unlocking device...");
                try {
                    method.unlock(intent);
                } catch (Throwable throwable) {
                    Log.e(TAG, "Failed to unlock device!", throwable);
                }
            }
        };

        // Register broadcastReceiver for IPC
        // TODO Handle multiple users
        IntentFilter intentFilter = new IntentFilter(ACTION_UNLOCK_DEVICE);
        context.registerReceiver(unlockReceiver, intentFilter, PERMISSION_UNLOCK_DEVICE, null);
    }

    public interface UnlockMethod {
        void unlock(Intent t) throws Throwable;
    }

    private UnlockMethod hookStatusBarBiometricUnlock(Object statusBar, Class<?> statusBarClass) throws Throwable {
        Object biometricUnlockController = asAccessible(statusBarClass.getDeclaredField("mBiometricUnlockController")).get(statusBar);
        Method startWakeAndUnlock = asAccessible(biometricUnlockController
                .getClass()
                .getDeclaredMethod("startWakeAndUnlock", int.class));

        return intent -> {
            int unlockMode = intent.getIntExtra(EXTRA_UNLOCK_MODE, MODE_UNLOCK_FADING);
            startWakeAndUnlock.invoke(biometricUnlockController, unlockMode);
        };
    }
}
