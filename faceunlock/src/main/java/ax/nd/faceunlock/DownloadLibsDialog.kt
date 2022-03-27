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
                                message(text = "The app needs to download some library files (<35 MB) necessary for face recognition to work. Download them now?")
                                positiveButton(android.R.string.ok) {
                                    viewModel.downloadLibs(activity)
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
                                "Downloading files...",
                                true,
                                false
                            )
                        }
                        status is DownloadStatus.DownloadError -> {
                            // Download failed
                            dialog = MaterialDialog(activity).show {
                                title(text = "Error")
                                message(text = "An error occurred while downloading the files: ${status.error}")
                                positiveButton(text = "Retry") {
                                    viewModel.downloadLibs(activity)
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