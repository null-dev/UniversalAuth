package ax.nd.faceunlock.util;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.Log;

import ax.nd.faceunlock.AppConstants;

import java.lang.reflect.InvocationTargetException;

public class Util {
    public static final boolean DEBUG = true;
    private static final String TAG = Util.class.getSimpleName();

    public static void setFaceUnlockAvailable(Context context) {
        Settings.setFaceUnlockAvailable(context, isFaceUnlockEnrolled(context) ? 1 : 0);
    }

    public static boolean isFaceUnlockEnrolled(Context context) {
        SharedUtil sharedUtil = new SharedUtil(context);
        return sharedUtil.getIntValueByKey(AppConstants.SHARED_KEY_FACE_ID) > 0 && sharedUtil.getByteArrayValueByKey(AppConstants.SHARED_KEY_ENROLL_TOKEN) != null;
    }

    public static boolean isFaceUnlockDisabledByDPM(Context context) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            if (devicePolicyManager.getPasswordQuality(null) > 32768) {
                return true;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "isFaceUnlockDisabledByDPM error:", e);
        }
        return (devicePolicyManager.getKeyguardDisabledFeatures(null) & 128) != 0;
    }

    public static int getUserId(Context context) {
        try {
            return (Integer) Context.class.getDeclaredMethod("getUserId", new Class[0]).invoke(context, new Object[0]);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static boolean isNightModeEnabled(Context context) {
        return (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void jumpToAppInfo(Activity activity, int requestCode) {
        Intent intent = new Intent()
                .setAction("android.settings.APPLICATION_DETAILS_SETTINGS")
                .setData(Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivityForResult(intent, requestCode);
    }

    public static boolean isByPassLockScreenAvailable(Context context) {
        // TODO Allow customization on whether or not to skip lock screen
        return true;
//        return !context.getResources().getBoolean(
//                com.android.internal.R.bool.config_faceAuthOnlyOnSecurityView);
    }
}
