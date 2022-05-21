package ax.nd.universalauth.xposed

import android.app.ActivityManager
import ax.nd.xposedutil.asAccessible

object Util {
    private val activityManagerGetCurrentUser =
        ActivityManager::class.java.getDeclaredMethod("getCurrentUser").asAccessible()

    fun getCurrentUser(): Int {
        return activityManagerGetCurrentUser.invoke(null) as Int
    }
}