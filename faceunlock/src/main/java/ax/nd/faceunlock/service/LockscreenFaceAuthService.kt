package ax.nd.faceunlock.service

import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Display
import android.view.Gravity
import android.view.ViewPropertyAnimator
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.core.content.getSystemService
import ax.nd.faceunlock.FaceApplication
import ax.nd.faceunlock.LibManager
import ax.nd.faceunlock.pref.Prefs
import ax.nd.faceunlock.util.Util
import ax.nd.faceunlock.util.dpToPx
import ax.nd.universalauth.xposed.common.XposedConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LockscreenFaceAuthService : AccessibilityService(), FaceAuthServiceCallbacks {
    private var windowManager: WindowManager? = null
    private var textView: TextView? = null
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP
    }

    private var active: Boolean = false
    private var lockStateReceiver: BroadcastReceiver? = null
    private var unlockReceiver: BroadcastReceiver? = null
    private var controller: FaceAuthServiceController? = null
    private var keyguardManager: KeyguardManager? = null
    private var displayManager: DisplayManager? = null
    private var handler: Handler? = null
    private var startTime: Long = 0
    private var textViewAnimator: ViewPropertyAnimator? = null

    private lateinit var serviceJob: Job
    private lateinit var serviceScope: CoroutineScope
    private lateinit var prefs: Prefs

    private var showStatusText = true
    private var booted = false

    override fun onCreate() {
        super.onCreate()

        // Coroutine env
        serviceJob = Job()
        serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

        prefs = FaceApplication.getApp().prefs

        controller = FaceAuthServiceController(this, prefs, this)

        windowManager = getSystemService()
        keyguardManager = getSystemService()
        displayManager = getSystemService()
        handler = Handler(Looper.getMainLooper())

        textView = TextView(this)
        textView?.setTextColor(getColor(android.R.color.white))
        textView?.setShadowLayer(10f, 0f, 0f, Color.BLACK)
        textView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        textView?.setPadding(0, 16.dpToPx.toInt(), 0, 0)
        textView?.textAlignment = TextView.TEXT_ALIGNMENT_CENTER

        booted = !prefs.requirePinOnBoot.get()
        if(booted) {
            reconfigureUnlockHook()
        }
        setupUnlockReceiver()

        serviceScope.launch {
            prefs.showStatusText.asFlow().collect { showStatusText ->
                this@LockscreenFaceAuthService.showStatusText = showStatusText
            }
        }
    }

    private fun setupUnlockReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
        }
        unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                if(!booted) {
                    booted = true
                    reconfigureUnlockHook()
                }
                prefs.failedUnlockAttempts.set(0)
            }
        }
        registerReceiver(unlockReceiver, intentFilter)
    }

    private fun unregisterUnlockReceiver() {
        unlockReceiver?.let {
            unregisterReceiver(it)
            unlockReceiver = null
        }
    }

    private fun reconfigureUnlockHook() {
        serviceScope.launch {
            prefs.earlyUnlockHook.asFlow().collect { earlyUnlock ->
                unregisterLockStateReceiver()
                if(earlyUnlock) {
                    registerEarlyUnlockReceiver()
                } else {
                    registerNormalReceiver()
                }
            }
        }
    }

    private fun registerNormalReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        lockStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                doubleCheckLockscreenState()
            }
        }
        registerReceiver(lockStateReceiver, intentFilter)
    }

    private fun registerEarlyUnlockReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(XposedConstants.ACTION_EARLY_UNLOCK)
        }
        lockStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent) {
                val mode = p1.getBooleanExtra(XposedConstants.EXTRA_EARLY_UNLOCK_MODE, false)
                if(mode) {
                    show()
                } else {
                    hide()
                }
            }
        }
        registerReceiver(lockStateReceiver, intentFilter)
    }

    private fun unregisterLockStateReceiver() {
        lockStateReceiver?.let {
            unregisterReceiver(it)
            lockStateReceiver = null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        if(!LibManager.libsLoaded.get()) {
            // Libs not loaded, abort
            Log.w(TAG, "Libs not loaded, disabling self...")
            disableSelf() // This doesn't work in onCreate
            return
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterLockStateReceiver()
        unregisterUnlockReceiver()
        hide()
        serviceJob.cancel()
    }

    // Broadcast receiver isn't 100% accurate
    private fun doubleCheckLockscreenState() {
        val mainDisplayOn = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.state == Display.STATE_ON

        if(mainDisplayOn && keyguardManager?.isKeyguardLocked == true) {
            show()
        } else {
            hide()
        }
    }

    private fun show() {
        if(!active) {
            if(Util.isFaceUnlockEnrolled(this)) {
                active = true
                // If animation is currently playing, we need to fire it's end listener
                if(showStatusText) {
                    textViewAnimator?.cancel()
                    textView?.text = "Looking for face..."
                    windowManager?.addView(textView, params)
                }
                startTime = System.currentTimeMillis()
                controller?.start()
            }
        }
    }

    private fun hide(delay: Int = 0) {
        if(active) {
            active = false
            if(showStatusText) {
                if (delay > 0) {
                    // Delayed hide is animated
                    textViewAnimator = textView?.animate()
                        ?.alpha(0f)
                        ?.setListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(p0: Animator?) {}
                            override fun onAnimationEnd(p0: Animator?) = onTextViewAnimationEnd()
                            override fun onAnimationCancel(p0: Animator?) {}
                            override fun onAnimationRepeat(p0: Animator?) {}
                        })
                        ?.setStartDelay(delay.toLong())
                        ?.setDuration(300)
                    textViewAnimator?.start()
                } else {
                    removeTextViewFromWindowManager()
                }
            }
            controller?.stop()
        } else {
            if(showStatusText) {
                if (delay == 0) {
                    // If hide animation is running, skip it
                    textViewAnimator?.cancel()
                }
            }
        }
    }

    // Should reset textview to the state it was in before animation started playing
    private fun onTextViewAnimationEnd() {
        removeTextViewFromWindowManager()
        textViewAnimator?.cancel()
        textViewAnimator = null
        textView?.alpha = 1f
    }

    private fun removeTextViewFromWindowManager() {
        windowManager?.removeView(textView)
    }

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
        // No need for these events
    }

    override fun onInterrupt() {
    }

    override fun onAuthed() {
        Log.d("MeasureFaceUnlock", "Total time: " + (System.currentTimeMillis() - startTime))
        // Tell Xposed module to unlock device
        val unlockAnimation = when(FaceApplication.getApp().prefs.unlockAnimation.get()) {
            "mode_wake_and_unlock" -> XposedConstants.MODE_WAKE_AND_UNLOCK
            "mode_wake_and_unlock_pulsing" -> XposedConstants.MODE_WAKE_AND_UNLOCK_PULSING
//            "mode_unlock_fading"
            else -> XposedConstants.MODE_UNLOCK_FADING
        }
        sendBroadcast(Intent(XposedConstants.ACTION_UNLOCK_DEVICE).apply {
            putExtra(XposedConstants.EXTRA_UNLOCK_MODE, unlockAnimation)
            putExtra(XposedConstants.EXTRA_BYPASS_KEYGUARD, prefs.bypassKeyguard.get())
        })
        handler?.post {
            textView?.text = "Welcome!"
            hide(delay = 1000)
        }
    }

    override fun onError(errId: Int, message: String) {
        handler?.post {
            textView?.text = message
            hide(delay = 2000)
        }
    }

    companion object {
        private val TAG = LockscreenFaceAuthService::class.simpleName
    }
}