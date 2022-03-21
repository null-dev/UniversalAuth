package ax.nd.faceunlock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ax.nd.faceunlock.databinding.ActivityMain2Binding
import ax.nd.faceunlock.service.LockscreenFaceAuthService
import ax.nd.faceunlock.service.RemoveFaceController
import ax.nd.faceunlock.service.RemoveFaceControllerCallbacks
import ax.nd.faceunlock.util.SharedUtil
import ax.nd.universalauth.xposed.common.XposedConstants
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), RemoveFaceControllerCallbacks {
    private lateinit var binding: ActivityMain2Binding
    private var removeFaceController: RemoveFaceController? = null
    private val chooseLibsViewModel: ChooseLibsViewModel by viewModels()
    private var pickLibsLauncher: Map<String, ActivityResultLauncher<String>>? = null
    private var requestUnlockPermsLauncher: ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.setupBtn.setOnClickListener {
            startActivity(Intent(this, SetupFaceIntroActivity::class.java))
        }
        binding.authBtn.setOnClickListener {
            startActivity(Intent(this, FaceAuthActivity::class.java))
        }

        pickLibsLauncher = LibManager.requiredLibraries.associate {
            it.name to registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if(uri != null)
                    chooseLibsViewModel.addLib(this, it, uri)
            }
        }

        requestUnlockPermsLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

        if(LibManager.librariesData.value.all { it.valid }) {
            // All libs valid, perform permission checks
            checkAndAskForPermissions()
        } else {
            // First setup libs
            ChooseLibsDialog(this, chooseLibsViewModel).open()
        }

        lifecycleScope.launch {
            // Update lib load error
            var curErrorDialog: MaterialDialog? = null
            LibManager.libLoadError.flowWithLifecycle(lifecycle).collect { error ->
                if(error != null) {
                    curErrorDialog?.cancel()
                    curErrorDialog = MaterialDialog(this@MainActivity).show {
                        title(text = "Fatal error")
                        message(text = "Failed to load libraries, application will now crash! If you wish to switch the app libraries, please clear the app data.")
                        positiveButton(text = "Ok :(") {
                            throw error
                        }
                        cancelable(false)
                        cancelOnTouchOutside(false)
                    }
                }
            }
        }
    }

    // Called once all libs are valid
    fun checkAndAskForPermissions() {
        // Request permission to unlock device if not yet granted
        if(ContextCompat.checkSelfPermission(this, XposedConstants.PERMISSION_UNLOCK_DEVICE) != PackageManager.PERMISSION_GRANTED) {
            requestUnlockPermsLauncher?.launch(XposedConstants.PERMISSION_UNLOCK_DEVICE)
        }

        // Request accessibility prefs
        if(!isAccessServiceEnabled(this, LockscreenFaceAuthService::class.java)) {
            MaterialDialog(this).show {
                title(text = "Accessibility service not enabled")
                message(text = "Face unlock needs accessibility access to interact with your lock screen. Please enable the 'Face unlock' accessibility service on the next page.")
                positiveButton(text = "Ok") {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        updateButtons()
    }

    override fun onStop() {
        super.onStop()
        releaseRemoveFaceController()
    }

    private fun isAccessServiceEnabled(context: Context, accessibilityServiceClass: Class<*>): Boolean {
        val prefString = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val serviceName = accessibilityServiceClass.canonicalName
        return prefString?.split(":")?.any {
            val slashIndex = it.indexOf('/')
            val possibleServiceName = it.substring(slashIndex + 1)
            possibleServiceName == serviceName || (it.substring(0, slashIndex) + possibleServiceName) == serviceName
        } == true
    }

    private fun removeFace(faceId: Int) {
        releaseRemoveFaceController()
        removeFaceController = RemoveFaceController(this, faceId, this).apply {
            start()
        }
    }

    private fun releaseRemoveFaceController() {
        removeFaceController?.stop()
        removeFaceController = null
    }

    private fun updateButtons() {
        val faceId = SharedUtil(this).getIntValueByKey(AppConstants.SHARED_KEY_FACE_ID)
        if(faceId > -1) {
            // Face enrolled
            binding.setupBtn.isEnabled = false
            binding.authBtn.isEnabled = true
            binding.removeBtn.isEnabled = true
            binding.removeBtn.setOnClickListener {
                removeFace(faceId)
            }
        } else {
            // No face enrolled
            binding.setupBtn.isEnabled = true
            binding.authBtn.isEnabled = false
            binding.removeBtn.isEnabled = false
        }
    }

    override fun onRemove() {
        runOnUiThread {
            Toast.makeText(this, "Face successfully removed!", Toast.LENGTH_SHORT).show()
            updateButtons()
            releaseRemoveFaceController()
        }
    }

    override fun onError(errId: Int, message: String) {
        runOnUiThread {
            Toast.makeText(this, "Failed to remove face: $message", Toast.LENGTH_LONG).show()
            releaseRemoveFaceController()
        }
    }

    fun browseForFiles(lib: RequiredLib) {
        pickLibsLauncher?.get(lib.name)?.launch("*/*")
    }
}