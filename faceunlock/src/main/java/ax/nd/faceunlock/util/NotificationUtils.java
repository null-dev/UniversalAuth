package ax.nd.faceunlock.util;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import ax.nd.faceunlock.R;

public class NotificationUtils {
    private static final String CHANNEL_ID = "ax.nd.faceunlock";
    private static final String CHANNEL_NAME = "Face Unlock";
    private static final String FACE_NOTIFICATION_COUNT = "face_notification_count";
    private static final String FACE_UNLOCKINTENT_COUNT = "face_unlockintent_count";
    private static final String TAG = NotificationUtils.class.getSimpleName();
    private static SharedUtil mSharedUtil;

    public static void checkAndShowNotification(Context context) {
        mSharedUtil = new SharedUtil(context);
        if (Settings.isFaceUnlockAvailable(context)) {
            if (Util.DEBUG) {
                Log.d(TAG, "face unlock has been set");
            }
            cancelNotification(context);
            setNotiCount();
        } else if (Util.isFaceUnlockDisabledByDPM(context)) {
            if (Util.DEBUG) {
                Log.d(TAG, "face unlock disabled by DPM");
            }
            cancelNotification(context);
        } else if (!isFirstNotification()) {
            if (Util.DEBUG) {
                Log.d(TAG, "not the first time to show the notification");
            }
        } else if (isUnlockCountReached()) {
            if (Util.DEBUG) {
                Log.d(TAG, "unlock intent count reached");
            }
            createNotification(context);
        }
    }

    private static boolean isUnlockCountReached() {
        int count = mSharedUtil.getIntValueByKey(FACE_UNLOCKINTENT_COUNT, 0);
        if (Util.DEBUG) {
            Log.d(TAG, "unlockIntentCount = " + count);
        }
        if (count >= 3) {
            return true;
        }
        count += 1;
        mSharedUtil.saveIntValue(FACE_UNLOCKINTENT_COUNT, count);
        return count >= 3;
    }

    @SuppressLint("WrongConstant")
    private static void createNotification(Context context) {
        setNotiCount();
        cancelNotification(context);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW));
        }
        Intent intent = new Intent();
        intent.setAction("android.settings.FACE_SETTINGS");
        intent.addFlags(268435456);
        notificationManager.notify(1, new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_face_auth)
                .setContentTitle(context.getString(R.string.facelock_setup_notification_title))
                .setContentText(context.getString(R.string.facelock_setup_notification_text))
                .setAutoCancel(true)
                .setChannelId(CHANNEL_ID)
                .setContentIntent(PendingIntent.getActivity(context, 0, intent, 268435456)).build());
    }

    private static void setNotiCount() {
        if (Util.DEBUG) {
            Log.d(TAG, "set noti count to 1");
        }
        mSharedUtil.saveIntValue(FACE_NOTIFICATION_COUNT, 1);
    }

    public static void cancelNotification(Context context) {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
    }

    private static boolean isFirstNotification() {
        return mSharedUtil.getIntValueByKey(FACE_NOTIFICATION_COUNT) <= 0;
    }
}
