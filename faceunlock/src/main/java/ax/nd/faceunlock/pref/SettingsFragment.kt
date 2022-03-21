package ax.nd.faceunlock.pref

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import ax.nd.faceunlock.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}