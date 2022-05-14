package software.blob.catablob.util

import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.PluralsRes
import software.blob.catablob.R
import java.io.File
import java.lang.Exception

private const val TAG = "ContextUtil"

/**
 * Determine if the device screen is tablet size
 */
val Context.isTablet get() = resources.getBoolean(R.bool.tablet)

/**
 * Determine if the device is in portrait orientation
 */
val Context.isPortrait get() = resources.configuration.orientation == ORIENTATION_PORTRAIT

/**
 * Get the app's temporary files directory
 * @return Temporary files directory
 */
val Context.tempDirectory get() = getOrCreateDirectory("tmp")

/**
 * Returns the string necessary for grammatically correct pluralization of the given resource
 * ID for the given quantity.
 * Shortcut for [Resources.getQuantityString]
 * @param id Plural string resource ID
 * @param quantity Quantity to use for pluralization
 * @param formatArgs String formatting arguments
 * @return Pluralized string
 */
fun Context.getQuantityString(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any) =
    resources.getQuantityString(id, quantity, *formatArgs)

/**
 * Get a sub-directory in this app's directory
 * This method will create the directory if it doesn't already exist
 * @param name String
 * @return Sub-directory
 */
fun Context.getOrCreateDirectory(name: String): File {
    var dir = getExternalFilesDir(name)
    if (dir == null) dir = File(name)
    if (!dir.exists()) dir.mkdirs()
    return dir
}

/**
 * Decode a content URI to a bitmap
 * @param uri Image file URI
 * @return Bitmap or null if failed
 */
fun Context.decodeBitmap(uri: Uri): Bitmap? {
    try {
        contentResolver.openInputStream(uri).use { stream ->
            val bytes = stream?.readBytes() ?: return null
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to decode URI to bitmap: $uri", e)
    }
    return null
}

/**
 * Get the soft keyboard interface (input method manager)
 * @return Input method manager
 */
fun Context.getSoftKeyboard(): InputMethodManager? {
    return getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
}

/**
 * Open the soft keyboard for this view
 * @param flags Operating flags
 */
fun View.openSoftKeyboard(flags: Int = InputMethodManager.SHOW_IMPLICIT) {
    context.getSoftKeyboard()?.showSoftInput(this, flags)
}

/**
 * Close the soft keyboard
 * @param flags Operating flags
 */
fun View.closeSoftKeyboard(flags: Int = InputMethodManager.HIDE_NOT_ALWAYS) {
    context.getSoftKeyboard()?.hideSoftInputFromWindow(windowToken, flags)
}

/**
 * Toggle the keyboard open or closed
 * @param showFlags The flags to use when showing the keyboard
 * @param hideFlags The flags to use when hiding the keyboard
 */
fun View.toggleSoftKeyboard(
    showFlags: Int = InputMethodManager.SHOW_IMPLICIT,
    hideFlags: Int = InputMethodManager.HIDE_NOT_ALWAYS) {
    // XXX - Even though this method is deprecated, it seems to be the only method
    // that sort of works. This keyboard API is very unreliable.
    context.getSoftKeyboard()?.toggleSoftInputFromWindow(windowToken, showFlags, hideFlags)
}