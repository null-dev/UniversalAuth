package ax.nd.faceunlock.service

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import ax.nd.faceunlock.FaceApplication
import ax.nd.faceunlock.LibManager
import ax.nd.faceunlock.util.Util
import ax.nd.faceunlock.vendor.FacePPImpl

/**
 * Keeps a copy of FacePPImpl preloaded in the background for speed
 */
class FacePPPreloader(private val context: Context) {
    private var queuedInstance: PreloadedInstance? = null

    init {
        if(shouldPreload())
            queuedInstance = preloadNextInstance(context)
    }

    @Synchronized
    fun obtain(): PreloadedInstance {
        // Do we have an instance? If so, hand it over.
        queuedInstance?.let {
            queuedInstance = null
            return it
        }
        // No available instance, make a new one
        Log.d(TAG, "FacePP is not preloaded! Loading a new one from scratch...")
        return preloadNextInstance(context)
    }

    @Synchronized
    fun release(instance: PreloadedInstance) {
        // Ok, old instance is dead now, it's safe to make a new one if we haven't already
        if(queuedInstance == null && shouldPreload()) {
            queuedInstance = preloadNextInstance(context)
        }
    }

    private fun shouldPreload(): Boolean {
        return FaceApplication.getApp().prefs.preloadFace.get()
                && LibManager.libsLoaded.get()
                && Util.isFaceUnlockEnrolled(context)
    }

    private fun preloadNextInstance(context: Context): PreloadedInstance {
        val handlerThread = HandlerThread(TAG, -2)
        handlerThread.start()
        val mFaceAuth = FacePPImpl(context)
        val instance = PreloadedInstance(mFaceAuth, handlerThread.looper)
        if (!Util.isFaceUnlockDisabledByDPM(context) && Util.isFaceUnlockEnrolled(context)) {
            instance.handler.post(mFaceAuth::init)
        }
        return instance
    }

    companion object {
        const val TAG = "FacePPPreloader"
    }
}

class PreloadedInstance(val impl: FacePPImpl, looper: Looper) {
    val handler: Handler = FaceHandler(this, looper)
    @JvmField
    var mChallenge: Long = 0
    @JvmField
    var mChallengeCount = 0
    var stopCallback: (() -> Unit)? = null

    // For safety, we don't actually try to reset the instance, instead we just make a new one
    // Theoretically this method is enough to reset it though
    fun reset() {
        handler.removeMessages(MSG_CHALLENGE_TIMEOUT)
    }

    private class FaceHandler(private val instance: PreloadedInstance, looper: Looper) : Handler(looper) {
        override fun handleMessage(message: Message) {
            if (message.what == MSG_CHALLENGE_TIMEOUT) {
                instance.mChallenge = 0
                instance.mChallengeCount = 0
                instance.stopCallback?.invoke()
//                stopCurrentWork()
            }
        }
    }
    companion object {
        const val MSG_CHALLENGE_TIMEOUT = 100
    }
}