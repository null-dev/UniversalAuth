package ax.nd.faceunlock.stub

import android.content.Context
import ax.nd.faceunlock.stub.face.FaceManager

class ContextShim(private val context: Context) {
    fun <T> getSystemService(serviceClass: Class<T>): T {
        if(serviceClass == FaceManager::class.java) {
            return faceManager as T
        }
        return context.getSystemService(serviceClass)
    }

    companion object {
        private val faceManager = FaceManager(null)
    }
}