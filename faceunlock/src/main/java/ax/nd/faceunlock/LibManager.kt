package ax.nd.faceunlock

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class RequiredLib(
    val name: String,
    val armeabi_v7a: String? = null, // Honestly not sure if they need hard float, so maybe they are just armeabi?
    val arm64_v8a: String? = null
) {
    fun hashForCurrentAbi(): String? {
        val abis = Build.SUPPORTED_ABIS
        if("arm64-v8a" in abis) {
            return arm64_v8a
        }
        if("armeabi-v7a" in abis) {
            return armeabi_v7a
        }
        return null
    }
}

data class LibraryState(
    val library: RequiredLib,
    val valid: Boolean
)

object LibManager {
    const val LIB_DIR = "facelibs"
    const val HASH_TYPE = "SHA-512"
    private val TAG = LibManager::class.simpleName

    // ORDER MATTERS!!! The libraries will be loaded in this order by updateLibraryData()
    // Some of the libraries depend on each other so they must be loaded in the correct order
    val requiredLibraries = listOf(
        RequiredLib(
            "libmegface.so",
            armeabi_v7a = "a4f28ce4311746ce48427b85a0ee46e09374ef2174179b5cdba2be48e6b6314ced5197f91560da13b6afa6d02181d72d2dd8af054188845780f1e8e64c4e097d",
            arm64_v8a = "ee0f4e88d305e8bd56cbf712c0610e9b27597e10c9fb57ae6a76917ac69e3261dc5664a6ca69be5b22746aaa13a0bb44ba3e211e2867736123dc29acb26db6db"
        ),
        RequiredLib(
            "libFaceDetectCA.so",
            armeabi_v7a = "93a05eed6fd832f54839646ae7bd4c31d0a2cc369fc0d9d010009b143725e560b4d6e166a96ca26a134c7e6beeab3517297fd5f1887ac60c646ff582486855ca",
            arm64_v8a = "246aad9cb643f75ce4e70375ea6333f5ab27faaa98e86655063dac124e8afdef5a0d8bcf4a64627f9932d07bc852e46a47259c0b478fe626e948ea6292cc9e3c"
        ),
        RequiredLib(
            "libMegviiUnlock.so",
            armeabi_v7a = "a0a84309c90f2b18d7aa3978a1635c74e68b775f41a3d9ba907c3873de2c0907f8bb769ad8b11f35ea7840e59893be70b3d94e60b97e76cafc656344a7165bbc",
            arm64_v8a = "44c599a4873d8891ac5a3d4aada1a2486854cee88724b06b7c88b20ad7730103a6061ad8dc1c7ea61fd24e5a066760d757ec0a0009cef7365e9e7dedcfa9e3f4"
        ),
        RequiredLib(
            "libMegviiUnlock-jni-1.2.so",
            armeabi_v7a = "533a12fb9b21108f40e34c0f8e0daf11f1b2ad1fad87eb3980b7ea28d2b8713127bc6e361f229bafe7a8d7746cc7f0e43e2e48da51b9907bd70587ae3c31435b",
            arm64_v8a = "098a1f476fe100f198e7527f6cb6bcdba611b1165f15cfbbe6a8ce4f5cd05f75aa5f26e079aa18c623b1a31a2c5b6d0df8b963dc1fe4ee472c8c588bf1256f48"
        ),
    )

    val librariesData = MutableStateFlow<List<LibraryState>>(emptyList())
    val libsLoaded = AtomicBoolean(false)
    val libLoadError = MutableStateFlow<Throwable?>(null)

    fun init(context: Context) {
        // It's IO but we do it on main thread as it's pretty important
        updateLibraryData(context)
    }

    fun updateLibraryData(context: Context) {
        val newStatus = requiredLibraries.map {
            LibraryState(
                library = it,
                valid = getLibFile(context, it).exists()
            )
        }
        librariesData.value = newStatus
        // Load libs if they all valid
        if (newStatus.all { it.valid }) {
            try {
                for(lib in newStatus) {
                    System.load(getLibFile(context, lib.library).absolutePath)
                }
                libsLoaded.set(true)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load native libraries!", t)
                libLoadError.value = t
            }
        }
    }

    fun getLibFile(context: Context, lib: RequiredLib, temp: Boolean = false): File {
        val fileDir = context.filesDir
        val libsDir = File(fileDir, LIB_DIR)
        val fname = if(temp) {
            "${lib.name}.tmp"
        } else lib.name
        return File(libsDir, fname)
    }
}