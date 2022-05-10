package software.blob.catablob.util

import android.widget.EditText

/**
 * Shortcut to get the raw text of an [EditText]
 */
val EditText.rawText get() = text?.toString() ?: ""