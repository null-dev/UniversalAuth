package ax.nd.faceunlock.util;

import android.content.Context;
import android.util.Log;

public class Settings {
    private static final String PROPERTY_FACEUNLOCK_AVAILABLE = "property_faceunlock_available";
    private static final String TAG = Settings.class.getSimpleName();

    /*public static boolean isByPassLockScreenEnabled(Context context) {
        // TODO Allow customization on whether or not to skip lock screen
        return true;
//        int defaultValue = context.getResources().getBoolean(
//                com.android.internal.R.bool.config_faceAuthDismissesKeyguard) ? 1 : 0;
//        boolean isEnabled = android.provider.Settings.Secure.getInt(
//                context.getContentResolver(),
//                "face_unlock_dismisses_keyguard", defaultValue) == 1;
//        Log.d(TAG, "isByPassLockScreenEnabled: " + isEnabled);
//        return isEnabled;
    }

    public static void setByPassLockScreenEnabled(Context context, boolean enabled) {
        android.provider.Settings.Secure.putInt(context.getContentResolver(), "face_unlock_dismisses_keyguard", enabled ? 1 : 0);
        Log.d(TAG, "setByPassLockScreenEnabled: " + enabled);
    }*/

    public static void setFaceUnlockAvailable(Context context, int i) {
        SharedUtil sharedPrefUtil = new SharedUtil(context);
        sharedPrefUtil.saveIntValue(PROPERTY_FACEUNLOCK_AVAILABLE, i);
        Log.d(TAG, "setFaceUnlockAvailable: " + i);
    }

    public static boolean isFaceUnlockAvailable(Context context) {
        SharedUtil sharedPrefUtil = new SharedUtil(context);
        return sharedPrefUtil.getIntValueByKey(PROPERTY_FACEUNLOCK_AVAILABLE) == 1;
    }
}
