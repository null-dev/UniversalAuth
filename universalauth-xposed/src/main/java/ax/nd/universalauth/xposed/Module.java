package ax.nd.universalauth.xposed;

import static ax.nd.universalauth.xposed.common.XposedConstants.ACTION_UNLOCK_DEVICE;
import static ax.nd.universalauth.xposed.common.XposedConstants.EXTRA_BYPASS_KEYGUARD;
import static ax.nd.universalauth.xposed.common.XposedConstants.PERMISSION_UNLOCK_DEVICE;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseLongArray;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Android 14+ SystemUI: face unlock state lives in DeviceEntryFaceAuthInteractor, which never
 * sees our software sensor. So we report the auth through KeyguardUpdateMonitor.onFaceAuthenticated
 * (drives BiometricUnlockController and TrustManager like a real face HAL would) and answer the
 * "is the user unlocked with biometric" queries ourselves until the screen turns off or the
 * keyguard goes away.
 */
public class Module implements IXposedHookLoadPackage {
    private static final String TAG = "UniversalAuth: ";
    private static final String LOG_TAG = "UniversalAuth";
    private static final String KUM_CLASS = "com.android.keyguard.KeyguardUpdateMonitor";
    private static final String BYPASS_CONTROLLER_CLASS =
            "com.android.systemui.statusbar.phone.KeyguardBypassController";
    private static final String KEYGUARD_STATE_CONTROLLER_CLASS =
            "com.android.systemui.statusbar.policy.KeyguardStateControllerImpl";

    // Upper bound for swipe-to-open after a face match; normally cleared much earlier on sleep
    private static final long FACE_UNLOCK_VALID_MS = 60_000;

    // Context.RECEIVER_EXPORTED / RECEIVER_NOT_EXPORTED, inlined: compileSdk predates them
    private static final int RECEIVER_EXPORTED = 0x2;
    private static final int RECEIVER_NOT_EXPORTED = 0x4;

    // user -> elapsedRealtime deadline. Touched only on the SystemUI main thread.
    private final SparseLongArray faceUnlockedUntil = new SparseLongArray();
    // Created lazily: the module is instantiated before the process has a main Looper
    private Handler mainHandler;
    private final Runnable expire = () -> clearFaceUnlock("expired");
    private boolean forceBypass;
    private Object kum;
    // KeyguardStateController caches canDismissLockScreen; it must be recomputed after we revoke
    private Object keyguardStateController;
    private Method keyguardStateUpdate;

    private Method onFaceAuthenticated;
    private Method isUserInLockdown;
    private Method isUnlockingWithBiometricAllowed;
    private Method getCurrentUser;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }
        ClassLoader cl = lpparam.classLoader;
        Class<?> kumClass = cl.loadClass(KUM_CLASS);

        getCurrentUser = accessible(ActivityManager.class.getDeclaredMethod("getCurrentUser"));
        onFaceAuthenticated = accessible(
                kumClass.getDeclaredMethod("onFaceAuthenticated", int.class, boolean.class));
        isUserInLockdown = optionalMethod(kumClass, "isUserInLockdown", int.class);
        isUnlockingWithBiometricAllowed =
                optionalMethod(kumClass, "isUnlockingWithBiometricAllowed", boolean.class);

        XposedBridge.hookAllConstructors(kumClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                onKumCreated(param.thisObject);
            }
        });

        try {
            Class<?> kscClass = cl.loadClass(KEYGUARD_STATE_CONTROLLER_CLASS);
            keyguardStateUpdate = optionalMethod(kscClass, "update", boolean.class);
            XposedBridge.hookAllConstructors(kscClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    keyguardStateController = param.thisObject;
                }
            });
        } catch (Throwable t) {
            log("KeyguardStateController not found", t);
        }

        // SCREEN_OFF arrives after keyguard already re-evaluated dismissibility, so revoke synchronously
        XC_MethodHook revokeOnSleep = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                clearFaceUnlock("going to sleep");
            }
        };
        hookOptional(kumClass, "dispatchStartedGoingToSleep", revokeOnSleep, int.class);
        hookOptional(kumClass, "handleStartedGoingToSleep", revokeOnSleep, int.class);

        // R8 may inline some of these into their callers, so hook every level we can find
        XC_MethodHook unlockedForUser = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!Boolean.TRUE.equals(param.getResult()) && isFaceUnlocked((int) param.args[0])) {
                    param.setResult(true);
                }
            }
        };
        hookOptional(kumClass, "getUserCanSkipBouncer", unlockedForUser, int.class);
        hookOptional(kumClass, "getUserUnlockedWithBiometric", unlockedForUser, int.class);
        hookOptional(kumClass, "isCurrentUserUnlockedWithFace", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!Boolean.TRUE.equals(param.getResult()) && isFaceUnlocked(currentUser())) {
                    param.setResult(true);
                }
            }
        });

        try {
            Class<?> bypassClass = cl.loadClass(BYPASS_CONTROLLER_CLASS);
            hookOptional(bypassClass, "getBypassEnabled", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (forceBypass) {
                        param.setResult(true);
                    }
                }
            });
        } catch (Throwable t) {
            log("bypass controller not found, \"bypass keyguard\" will behave like swipe-to-open", t);
        }
    }

    private void onKumCreated(Object kum) throws Throwable {
        if (this.kum != null) {
            return;
        }
        this.kum = kum;
        mainHandler = new Handler(Looper.getMainLooper());
        Context context = (Context) findField(kum.getClass(), "mContext").get(kum);

        BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                try {
                    unlock(intent.getBooleanExtra(EXTRA_BYPASS_KEYGUARD, true));
                } catch (Throwable t) {
                    log("unlock failed", t);
                }
            }
        };
        // Exported: the sender is the face unlock app; guarded by the signature permission
        context.registerReceiver(unlockReceiver, new IntentFilter(ACTION_UNLOCK_DEVICE),
                PERMISSION_UNLOCK_DEVICE, null, receiverFlags(RECEIVER_EXPORTED));

        BroadcastReceiver resetReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                clearFaceUnlock(intent.getAction());
            }
        };
        IntentFilter resetFilter = new IntentFilter();
        resetFilter.addAction(Intent.ACTION_SCREEN_OFF);
        resetFilter.addAction(Intent.ACTION_USER_PRESENT);
        resetFilter.addAction("android.intent.action.USER_SWITCHED"); // @hide in the SDK
        context.registerReceiver(resetReceiver, resetFilter, null, null,
                receiverFlags(RECEIVER_NOT_EXPORTED));

        XposedBridge.log(TAG + "hooked KeyguardUpdateMonitor");
    }

    private void unlock(boolean bypassKeyguard) throws Throwable {
        int user = currentUser();
        boolean lockedDown = isLockedDown(user);
        boolean allowed = biometricAllowed();
        Log.i(LOG_TAG, "unlock request: user=" + user + " bypass=" + bypassKeyguard
                + " lockdown=" + lockedDown + " biometricAllowed=" + allowed);
        if (lockedDown || !allowed) {
            // Lockdown, fresh boot or strong-auth timeout: the bouncer must win
            return;
        }
        if (keyguardStateController == null || keyguardStateUpdate == null) {
            // Fail closed: without a way to revoke, a granted unlock would outlive the screen
            Log.w(LOG_TAG, "cannot refresh KeyguardStateController, refusing to unlock");
            return;
        }
        faceUnlockedUntil.put(user, SystemClock.elapsedRealtime() + FACE_UNLOCK_VALID_MS);
        mainHandler.removeCallbacks(expire);
        mainHandler.postDelayed(expire, FACE_UNLOCK_VALID_MS);
        forceBypass = bypassKeyguard;
        try {
            onFaceAuthenticated.invoke(kum, user, false);
        } finally {
            forceBypass = false;
        }
        Log.i(LOG_TAG, "face auth reported, canSkipBouncer=" + isFaceUnlocked(user));
    }

    private boolean isFaceUnlocked(int user) {
        try {
            return faceUnlockedUntil.get(user, 0) > SystemClock.elapsedRealtime()
                    && !isLockedDown(user) && biometricAllowed();
        } catch (Throwable t) {
            log("state check failed", t);
            return false;
        }
    }

    private void clearFaceUnlock(String reason) {
        if (mainHandler == null) {
            return;
        }
        mainHandler.removeCallbacks(expire);
        if (faceUnlockedUntil.size() == 0) {
            return;
        }
        faceUnlockedUntil.clear();
        Log.i(LOG_TAG, "face unlock revoked: " + reason);
        try {
            keyguardStateUpdate.invoke(keyguardStateController, true);
        } catch (Throwable t) {
            log("KeyguardStateController refresh failed", t);
        }
        try {
            // onFaceAuthenticated also marked the user in TrustManagerService (isDeviceLocked, keystore)
            Object trustManager = findField(kum.getClass(), "mTrustManager").get(kum);
            // BiometricSourceType is @hide in the SDK
            @SuppressWarnings({"unchecked", "rawtypes"})
            Class<Enum> sourceType = (Class<Enum>) Class.forName(
                    "android.hardware.biometrics.BiometricSourceType");
            trustManager.getClass()
                    .getMethod("clearAllBiometricRecognized", sourceType, int.class)
                    .invoke(trustManager, Enum.valueOf(sourceType, "FACE"), -1);
        } catch (Throwable t) {
            log("TrustManager biometric reset failed", t);
        }
    }

    private boolean isLockedDown(int user) throws Throwable {
        return isUserInLockdown != null && (boolean) isUserInLockdown.invoke(kum, user);
    }

    private boolean biometricAllowed() throws Throwable {
        return isUnlockingWithBiometricAllowed == null
                || (boolean) isUnlockingWithBiometricAllowed.invoke(kum, false);
    }

    private int currentUser() {
        try {
            return (int) getCurrentUser.invoke(null);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int receiverFlags(int flag) {
        return Build.VERSION.SDK_INT >= 33 ? flag : 0;
    }

    private static void hookOptional(Class<?> clazz, String name, XC_MethodHook hook,
                                     Class<?>... params) {
        Method m = optionalMethod(clazz, name, params);
        if (m == null) {
            XposedBridge.log(TAG + clazz.getSimpleName() + "." + name + " not found, skipped");
            return;
        }
        XposedBridge.hookMethod(m, hook);
    }

    private static Method optionalMethod(Class<?> clazz, String name, Class<?>... params) {
        try {
            return accessible(clazz.getDeclaredMethod(name, params));
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static <T extends java.lang.reflect.AccessibleObject> T accessible(T a) {
        a.setAccessible(true);
        return a;
    }

    private static void log(String msg, Throwable t) {
        XposedBridge.log(TAG + msg);
        XposedBridge.log(t);
    }
}
