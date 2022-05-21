package ax.nd.universalauth.xposed;

import static ax.nd.universalauth.xposed.common.XposedConstants.EXTRA_BYPASS_KEYGUARD;
import static ax.nd.universalauth.xposed.common.XposedConstants.EXTRA_UNLOCK_MODE;
import static ax.nd.universalauth.xposed.common.XposedConstants.MODE_UNLOCK_FADING;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import kotlin.Unit;

public class Module implements IXposedHookLoadPackage {
    private static final String TAG = "XposedUniversalAuth";
    private static final String STATUS_BAR_CLASS = "com.android.systemui.statusbar.phone.StatusBar";
    private static final String SYSTEM_UI_CLASS = "com.android.systemui.SystemUI";
    private static final String CORE_STARTABLE_CLASS = "com.android.systemui.CoreStartable";
    private static final String KEYGUARD_UPDATE_MONITOR_CLASS = "com.android.keyguard.KeyguardUpdateMonitor";
    private static final String STATUS_BAR_STATE_CONTROLLER_CLASS = "com.android.systemui.plugins.statusbar.StatusBarStateController";
    private static final String KEYGUARD_UPDATE_MONITOR_LAST_MODE = "ax.nd.universalauth.last-mode";

    // com.android.systemui.statusbar.StatusBarState.SHADE_LOCKED
    private static final int SHADE_LOCKED = 2;

    private Method isUserInLockdownMethod;

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
            Class<?> kumClazz = lpparam.classLoader.loadClass(KEYGUARD_UPDATE_MONITOR_CLASS);
            isUserInLockdownMethod = getIsUserInLockdownMethod(kumClazz);

            try {
                // Hook com.android.systemui.statusbar.phone.StatusBar.start
                Class<?> statusBarClass = lpparam.classLoader.loadClass(STATUS_BAR_CLASS);
                hookStatusBar(lpparam, statusBarClass);
            } catch(Throwable e) {
                // Failed to hook status bar class, we are on Android 13 DP Beta 1+
                // Try using another class
                try {
                    Class<?> statusBarClass = lpparam.classLoader.loadClass("com.android.systemui.statusbar.phone.CentralSurfaces");
                    hookStatusBar(lpparam, statusBarClass);
                } catch(Throwable e2) {
                    XposedBridge.log("Failed to hook status bar! Initial attempt failed with:");
                    XposedBridge.log(e);
                    XposedBridge.log("We tried an alternative class but that failed too:");
                    XposedBridge.log(e2);
                }
            }

            // Hook com.android.keyguard.KeyguardUpdateMonitor.updateFaceListeningState
            try {
                addHookEarlyUnlock(kumClazz, lpparam);
            } catch (Throwable t) {
                XposedBridge.log("Failed to hook early unlock, early unlock hook will not work:");
                XposedBridge.log(t);
            }
            try {
                TrustHook.INSTANCE.hookKum(kumClazz);
            } catch (Throwable t) {
                XposedBridge.log("Failed to hook trust hook, trust hook will not work:");
                XposedBridge.log(t);
            }
        }
    }

    private void hookStatusBar(XC_LoadPackage.LoadPackageParam lpparam, Class<?> statusBarClass) {
        XposedHelpers.findAndHookMethod(
                statusBarClass,
                "start",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                            XposedBridge.log("FaceUnlock hook pre-install!");
                        hookStatusBar(statusBarClass, lpparam.classLoader, param);
//                            XposedBridge.log("FaceUnlock hook installed!");
                    }
                }
        );
    }

    private Method getIsUserInLockdownMethod(Class<?> kumClazz) {
        try {
            return asAccessible(kumClazz.getDeclaredMethod("isUserInLockdown", int.class));
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private boolean isUserInLockdown(Object kum) throws InvocationTargetException, IllegalAccessException {
        return isUserInLockdownMethod != null
                && (boolean) isUserInLockdownMethod.invoke(kum, Util.INSTANCE.getCurrentUser());
    }

    private void addHookEarlyUnlock(Class<?> kumClazz, XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Field mStatusBarStateControllerField = asAccessible(kumClazz.getDeclaredField("mStatusBarStateController"));
        Field mKeyguardIsVisibleField = asAccessible(kumClazz.getDeclaredField("mKeyguardIsVisible"));
        Field mDeviceInteractiveField = asAccessible(kumClazz.getDeclaredField("mDeviceInteractive"));
        Field mGoingToSleepField = asAccessible(kumClazz.getDeclaredField("mGoingToSleep"));
        Field mContextField = asAccessible(kumClazz.getDeclaredField("mContext"));

        Class<?> sbscClazz = lpparam.classLoader.loadClass(STATUS_BAR_STATE_CONTROLLER_CLASS);
        Method getStateMethod = asAccessible(sbscClazz.getDeclaredMethod("getState"));

        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object kum = param.thisObject;
                if(isUserInLockdown(kum)) {
                    return;
                }

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
        };

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            // Android 12L+
            XposedHelpers.findAndHookMethod(
                    kumClazz,
                    "updateFaceListeningState",
                    int.class,
                    hook
            );
        } else {
            XposedHelpers.findAndHookMethod(
                    kumClazz,
                    "updateFaceListeningState",
                    hook
            );
        }
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

    private void hookStatusBar(Class<?> statusBarClass, ClassLoader classLoader, XC_MethodHook.MethodHookParam param) throws Throwable {
        Object statusBar = param.thisObject;
        Class<?> systemUiClass;
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            // >= Android 13
            systemUiClass = classLoader.loadClass(CORE_STARTABLE_CLASS);
        } else if(Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2) {
            // == Android 12L
            try {
                // Try loading SystemUI class for Android 12L
                systemUiClass = classLoader.loadClass(SYSTEM_UI_CLASS);
            } catch (ClassNotFoundException ex) {
                // We are on Android 13 DP (same API level as 12L: https://issuetracker.google.com/issues/36973990#comment2)
                // Load Android 13 class
                systemUiClass = classLoader.loadClass(CORE_STARTABLE_CLASS);
            }
        } else {
            // <= Android 12
            systemUiClass = classLoader.loadClass(SYSTEM_UI_CLASS);
        }
        Context context = (Context) asAccessible(systemUiClass.getDeclaredField("mContext")).get(statusBar);
        Object kum = asAccessible(statusBarClass.getDeclaredField("mKeyguardUpdateMonitor")).get(statusBar);

        UnlockMethod method = hookStatusBarBiometricUnlock(statusBar, statusBarClass);

        UnlockReceiver.INSTANCE.setup(context, statusBar, intent -> {
            try {
                if(!isUserInLockdown(kum)) {
                    method.unlock(intent);
                }
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
            return Unit.INSTANCE;
        });
    }

    public interface UnlockMethod {
        void unlock(Intent t) throws Throwable;
    }

    private Object getBiometricUnlockControllerFromStatusBar(Object statusBar, Class<?> statusBarClass) throws NoSuchFieldException, IllegalAccessException {
        return asAccessible(statusBarClass.getDeclaredField("mBiometricUnlockController")).get(statusBar);
    }

    private UnlockMethod hookStatusBarBiometricUnlock(Object statusBar, Class<?> statusBarClass) throws Throwable {
        Object biometricUnlockController = getBiometricUnlockControllerFromStatusBar(statusBar, statusBarClass);
        Method startWakeAndUnlock = asAccessible(biometricUnlockController
                .getClass()
                .getDeclaredMethod("startWakeAndUnlock", int.class));

        return intent -> {
            if (intent.getBooleanExtra(EXTRA_BYPASS_KEYGUARD, true)) {
                int unlockMode = intent.getIntExtra(EXTRA_UNLOCK_MODE, MODE_UNLOCK_FADING);
                startWakeAndUnlock.invoke(biometricUnlockController, unlockMode);
            }
        };
    }
}
