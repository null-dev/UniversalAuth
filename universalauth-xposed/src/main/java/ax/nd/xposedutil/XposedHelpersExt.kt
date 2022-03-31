package ax.nd.xposedutil

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.Exception
import java.util.*

object XposedHelpersExt {
    /**
     * Log the stack trace and call arguments for any calls to this method
     */
    fun findAndLogCallsToMethod(className: String, classLoader: ClassLoader, methodName: String, vararg parameterTypes: Any) {
        XposedHelpers.findAndHookMethod(className, classLoader, methodName, *parameterTypes, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                Log.d(DEBUG_LOG_TAG, "[$methodName] called, full call path: $className.$methodName")
                Log.d(DEBUG_LOG_TAG, "[$methodName] called, args: ${param.args.contentToString()}")
                Log.d(DEBUG_LOG_TAG, "[$methodName] this: ${param.thisObject}")
                Log.d(DEBUG_LOG_TAG, "[$methodName] stack trace:", Exception())
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                Log.d(DEBUG_LOG_TAG, "[$methodName] result: ${param.result}")
            }
        })
    }

    /**
     * Run hook after class is constructed, doesn't matter how many constructors the class has.
     * It will always run after last constructor.
     */
    fun runAfterClassConstructed(clazz: Class<*>, hook: (param: XC_MethodHook.MethodHookParam) -> Unit) {
        val uuid = UUID.randomUUID().toString()

        fun incConstructorRan(target: Any) {
            val cur = XposedHelpers.getAdditionalInstanceField(target, uuid) as? Int ?: 0
            XposedHelpers.setAdditionalInstanceField(target, uuid, cur + 1)
        }
        fun decConstructorRan(target: Any): Boolean {
            val cur = XposedHelpers.getAdditionalInstanceField(target, uuid) as? Int
            if(cur == null || cur == 0) {
                Log.w(DEBUG_LOG_TAG, "decConstructorRan instance field is $cur!")
                return false // Never run hook
            }
            return if(cur == 1) {
                XposedHelpers.removeAdditionalInstanceField(target, uuid)
                true
            } else {
                val newValue = cur - 1
                XposedHelpers.setAdditionalInstanceField(target, uuid, newValue)
                false
            }
        }

        XposedBridge.hookAllConstructors(clazz, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                incConstructorRan(param.thisObject)
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                if(decConstructorRan(param.thisObject)) {
                    hook(param)
                }
            }
        })
    }
}