package ax.nd.faceunlock.pref

import android.content.Context
import androidx.preference.PreferenceManager
import com.fredporciuncula.flow.preferences.FlowSharedPreferences

class Prefs(context: Context) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val flowPrefs = FlowSharedPreferences(sharedPreferences)

    val preloadFace = flowPrefs.getBoolean(PrefKeys.PRELOAD_FACE, true)
    val unlockAnimation = flowPrefs.getString(PrefKeys.UNLOCK_ANIMATION, "mode_unlock_fading")
}