package ax.nd.universalauth.xposed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import ax.nd.universalauth.xposed.common.XposedConstants.ACTION_UNLOCK_DEVICE
import ax.nd.universalauth.xposed.common.XposedConstants.PERMISSION_UNLOCK_DEVICE
import de.robv.android.xposed.XposedHelpers

object UnlockReceiver {
    private val TAG = UnlockReceiver::class.simpleName
    private val ATTACH_KEY = "${UnlockReceiver::class.qualifiedName}.attach-key"

    fun setup(context: Context, attachTo: Any, exec: (intent: Intent) -> Unit): BroadcastReceiver {
        val unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "Unlocking device...")
                try {
                    exec(intent)
                } catch (throwable: Throwable) {
                    Log.e(TAG, "Failed to unlock device!", throwable)
                }
            }
        }

        // Register broadcastReceiver for IPC
        // TODO Handle multiple users
        val intentFilter = IntentFilter(ACTION_UNLOCK_DEVICE)
        context.registerReceiver(unlockReceiver, intentFilter, PERMISSION_UNLOCK_DEVICE, null)

        XposedHelpers.setAdditionalInstanceField(attachTo, ATTACH_KEY, unlockReceiver)

        return unlockReceiver
    }

    fun teardown(context: Context, attachTo: Any) {
        val receiver = XposedHelpers.removeAdditionalInstanceField(attachTo, ATTACH_KEY) as BroadcastReceiver?
        if (receiver != null) {
            try {
                context.unregisterReceiver(receiver)
            } catch(e: IllegalArgumentException) {
                // Receiver never registered?
                Log.w(TAG, "Failed to unregister unlock receiver", e)
            }
        }
    }
}