package ax.nd.xposedutil

import java.lang.reflect.AccessibleObject

fun <T : AccessibleObject> T.asAccessible(): T {
    isAccessible = true
    return this
}