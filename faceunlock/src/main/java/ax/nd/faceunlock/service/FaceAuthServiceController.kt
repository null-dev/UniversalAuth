package ax.nd.faceunlock.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants
import ax.nd.faceunlock.stub.face.FaceManager
import com.android.internal.util.custom.faceunlock.IFaceService
import com.android.internal.util.custom.faceunlock.IFaceServiceReceiver

interface FaceAuthServiceCallbacks {
    fun onAuthed()
    fun onError(errId: Int, message: String)
}

class FaceAuthServiceController(private val context: Context, private val cb: FaceAuthServiceCallbacks) {
    private var serviceBound = false
    private val authCallback = object : IFaceServiceReceiver.Stub() {
        @Throws(RemoteException::class)
        override fun onEnrollResult(faceId: Int, userId: Int, remaining: Int) {
            Log.wtf(TAG, "onEnrollResult called in FaceAuthServiceController???")
            // Probably don't need to handle this
        }

        @Throws(RemoteException::class)
        override fun onAuthenticated(faceId: Int, userId: Int, token: ByteArray?) {
            if(userId == -1) {
                // onAuthenticated can still be called if the request times out
                onError(BiometricFaceConstants.FACE_ERROR_TIMEOUT, 0)
            } else {
                Log.d(TAG, "Authentication OK: $faceId, $userId")
                stopInternal(cancel = false)
                cb.onAuthed()
            }
        }

        @Throws(RemoteException::class)
        override fun onError(error: Int, vendorCode: Int) {
            val clientErrId = FaceManager.calcClientMsgId(error, vendorCode)
            val message = FaceManager.getErrorString(context, error, vendorCode)
            Log.d(TAG, "FaceAuthActivity callback onError: $clientErrId, $error, $vendorCode, $message")
            stopInternal(cancel = false)
            cb.onError(clientErrId, message)
        }

        @Throws(RemoteException::class)
        override fun onRemoved(faceIds: IntArray, userId: Int) {
            Log.wtf(TAG, "onRemoved called in FaceAuthServiceController???")
            // Not used, safe to skip
        }

        @Throws(RemoteException::class)
        override fun onEnumerate(faceIds: IntArray, userId: Int) {
            Log.wtf(TAG, "onEnumerate called in FaceAuthServiceController???")
            // Not used, safe to skip
        }
    }
    private var authBinder: IFaceService? = null
    private val faceAuthConn: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            authBinder = iBinder as IFaceService
            try {
                (authBinder ?: return).setCallback(authCallback)
            } catch (e: RemoteException) {
                Log.e(TAG, "Callback setup error!", e)
            }
            try {
                (authBinder ?: return).authenticate(0)
            } catch (e: RemoteException) {
                Log.e(TAG, "Auth error!", e)
                authCallback.onError(BiometricFaceConstants.FACE_ERROR_UNKNOWN, 0)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            authBinder = null
        }
    }

    fun start() {
        if(!serviceBound) {
            serviceBound = true
            context.bindService(
                Intent(context, FaceAuthService::class.java),
                faceAuthConn,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    fun stop() {
        stopInternal(cancel = true)
    }

    private fun stopInternal(cancel: Boolean) {
        if(serviceBound) {
            serviceBound = false
            if (cancel) {
                try {
                    authBinder?.cancel()
                } catch (e: RemoteException) {
                    Log.e(TAG, "Failed to cancel face auth!", e)
                }
            }
            context.unbindService(faceAuthConn)
        }
    }

    companion object {
        private const val TAG = "FaceAuthServiceController"
    }
}