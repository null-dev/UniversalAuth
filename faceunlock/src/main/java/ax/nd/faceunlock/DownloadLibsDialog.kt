package ax.nd.faceunlock

import android.app.Dialog
import android.app.ProgressDialog
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch

class DownloadLibsDialog(private val activity: MainActivity, private val viewModel: ChooseLibsViewModel) {
    fun open() {
        var dialog: Dialog? = null

        activity.lifecycleScope.launch {
            viewModel.downloadStatus
                .combine(LibManager.librariesData) { a, b -> a to b }
                .transformWhile { pair ->
                    emit(pair)
                    pair.second.any { !it.valid } // Terminate flow once all libs are valid
                }
                .flowWithLifecycle(activity.lifecycle)
                .collect { (status, libs) ->
                    dialog?.cancel()
                    dialog = null

                    when {
                        libs.all { it.valid } -> {
                            // All libs valid, continue to check perms
                            activity.checkAndAskForPermissions()
                        }
                        status == null -> {
                            // Ask download
                            dialog = MaterialDialog(activity).show {
                                title(text = "Download required")
                                message(text = "The app needs to download some library files (<35 MB) necessary for face recognition to work. Download them now?" +
                                        "\n\nAlternatively, you can import the file manually.")
                                positiveButton(android.R.string.ok) {
                                    viewModel.downloadLibs(activity, null)
                                }
                                negativeButton(text = "Manual import") {
                                    viewModel.setAskImport()
                                }
                                cancelOnTouchOutside(false)
                                cancelable(false)
                                noAutoDismiss()
                            }
                        }
                        status is DownloadStatus.AskImport -> {
                            // Ask user to import libraries manually
                            dialog = MaterialDialog(activity).show {
                                title(text = "Manual import")
                                message(text = "Please find the APK of version 01.03.0312 of the 'Moto Face Unlock' app. It is about 33 MB. Press OK when you are ready to import the APK.")
                                positiveButton(android.R.string.ok) {
                                    activity.browseForFiles()
                                }
                                negativeButton(android.R.string.cancel) {
                                    viewModel.clearDownloadResult()
                                }
                                cancelOnTouchOutside(false)
                                cancelable(false)
                                noAutoDismiss()
                            }
                        }
                        status is DownloadStatus.Downloading -> {
                            // Downloading
                            dialog = ProgressDialog.show(
                                activity,
                                "Processing",
                                if(status.importing) "Importing APK..." else "Downloading files...",
                                true,
                                false
                            )
                        }
                        status is DownloadStatus.DownloadError -> {
                            // Download failed
                            dialog = MaterialDialog(activity).show {
                                title(text = "Error")
                                if(status.importing) {
                                    message(text = "Could not import the APK, are you sure it's the correct APK?")
                                    positiveButton(text = "Ok") {
                                        viewModel.setAskImport()
                                    }
                                } else {
                                    message(text = "An error occurred while downloading the files: ${status.error ?: "Unknown error"}")
                                    positiveButton(text = "Retry") {
                                        viewModel.downloadLibs(activity, null)
                                    }
                                    negativeButton(text = "Manual import") {
                                        viewModel.setAskImport()
                                    }
                                }
                                cancelOnTouchOutside(false)
                                cancelable(false)
                                noAutoDismiss()
                            }
                        }
                    }
            }
        }
    }
}