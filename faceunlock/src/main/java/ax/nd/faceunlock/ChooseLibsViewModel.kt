package ax.nd.faceunlock

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.apache.commons.codec.binary.Hex
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Exception
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

sealed interface CheckResult {
    val library: RequiredLib

    data class BadHash(override val library: RequiredLib, val tmpFile: File) : CheckResult
    data class FileError(override val library: RequiredLib, val error: Exception) : CheckResult
}

class ChooseLibsViewModel : ViewModel() {
    val checkingStatus = MutableStateFlow(false)
    val checkResult = MutableStateFlow<CheckResult?>(null)

    fun addLib(context: Context, library: RequiredLib, uri: Uri) {
        if (!checkingStatus.value && checkResult.value == null) {
            checkingStatus.value = true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    addLibInternal(context, library, uri)
                } finally {
                    checkingStatus.value = false
                }
            }
        }
    }

    fun saveBadHashLib(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = checkResult.value
            if (!checkingStatus.value && result is CheckResult.BadHash) {
                if(!result.tmpFile.renameTo(LibManager.getLibFile(context, result.library))) {
                    val exception = IOException("Failed to save library file!")
                    checkResult.value = CheckResult.FileError(result.library, exception)
                    Log.e(TAG, "Failed to save library file!", exception)
                    return@launch
                }
                // Valid, update library data
                LibManager.updateLibraryData(context)
            }
            clearCheckResult()
        }
    }

    fun clearCheckResult() {
        checkResult.value = null
    }

    private fun addLibInternal(context: Context, library: RequiredLib, uri: Uri) {
        try {
            context.contentResolver.openInputStream(uri) ?: run {
                val exception = NullPointerException("ContentProvider crashed!")
                checkResult.value = CheckResult.FileError(library, exception)
                Log.e(TAG, "Failed to open input stream!", exception)
                return
            }
        } catch (e: FileNotFoundException) {
            checkResult.value = CheckResult.FileError(library, e)
            Log.e(TAG, "Content provider reported file not found!", e)
            return
        }.use { inputStream ->
            val targetFile = LibManager.getLibFile(context, library, temp = true)
            targetFile.parentFile?.mkdirs()

            val digest = try {
                MessageDigest.getInstance(LibManager.HASH_TYPE)
            } catch (e: NoSuchAlgorithmException) {
                checkResult.value = CheckResult.FileError(library, e)
                Log.e(TAG, "Missing hash type: ${LibManager.HASH_TYPE}", e)
                return
            }
            val wrappedInputStream = DigestInputStream(inputStream, digest)
            try {
                targetFile.outputStream().use { outputStream ->
                    wrappedInputStream.copyTo(outputStream)
                }
            } catch(e: IOException) {
                checkResult.value = CheckResult.FileError(library, e)
                Log.e(TAG, "Failed to write library to: $targetFile!", e)
                return
            }

            val hash = digest.digest()
            val hex = Hex.encodeHexString(hash, true)

            val targetHash = library.hashForCurrentAbi() ?: run {
                val exception = UnsupportedOperationException("This app cannot run on your device: unsupported ABI!")
                checkResult.value = CheckResult.FileError(library, exception)
                Log.e(TAG, "App cannot run on device, ABI not supported!", exception)
                return
            }
            if(hex == targetHash) {
                if(!targetFile.renameTo(LibManager.getLibFile(context, library))) {
                    val exception = IOException("Failed to save library file!")
                    checkResult.value = CheckResult.FileError(library, exception)
                    Log.e(TAG, "Failed to save library file!", exception)
                    return
                }
                // Valid, update library data
                LibManager.updateLibraryData(context)
            } else {
                checkResult.value = CheckResult.BadHash(library, tmpFile = targetFile)
            }
        }
    }

    companion object {
        private val TAG = ChooseLibsViewModel::class.simpleName
    }
}