package ax.nd.universalauth.xposed

import android.app.ActivityManager
import android.content.Context
import ax.nd.universalauth.xposed.common.XposedConstants.EXTRA_BYPASS_KEYGUARD
import ax.nd.xposedutil.XposedHelpersExt
import ax.nd.xposedutil.asAccessible

object TrustHook {
    fun hookKum(kumClazz: Class<*>) {
        val contextField = kumClazz.getDeclaredField("mContext").asAccessible()
        val onFaceAuthenticated = kumClazz.getDeclaredMethod(
            "onFaceAuthenticated",
            Int::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!
        ).asAccessible()
        val activityManagerGetCurrentUser =
            ActivityManager::class.java.getDeclaredMethod("getCurrentUser").asAccessible()
        XposedHelpersExt.runAfterClassConstructed(kumClazz) { param ->
            val context = contextField.get(param.thisObject) as Context
            UnlockReceiver.setup(context, param.thisObject) { intent ->
                if (!intent.getBooleanExtra(EXTRA_BYPASS_KEYGUARD, true)) {
                    val currentUser = activityManagerGetCurrentUser.invoke(null)
                    onFaceAuthenticated.invoke(param.thisObject, currentUser, true)
                }
            }
        }

        /* This method doesn't actually seem to exist:
        XposedHelpers.findAndHookMethod(kumClazz, "destroy", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val context = contextField.get(param.thisObject) as Context
                UnlockReceiver.teardown(context, param.thisObject)
            }
        })*/
    }
}