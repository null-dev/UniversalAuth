package ax.nd.universalauth.xposed.common

object XposedConstants {
    const val ACTION_UNLOCK_DEVICE = "ax.nd.universalauth.unlock-device"
    const val EXTRA_UNLOCK_MODE = "ax.nd.universalauth.unlock-device.unlock-mode"

    const val PERMISSION_UNLOCK_DEVICE = "ax.nd.universalauth.permission.UNLOCK_DEVICE"

    /**
     * From com.android.systemui.statusbar.phone.BiometricUnlockController
     */
    const val MODE_NONE = 0
    const val MODE_WAKE_AND_UNLOCK = 1 // No animation
    const val MODE_WAKE_AND_UNLOCK_PULSING = 2 // No animation
    const val MODE_UNLOCK_COLLAPSING = 5
    const val MODE_UNLOCK_FADING = 7 // Animated
    const val MODE_DISMISS_BOUNCER = 8
}