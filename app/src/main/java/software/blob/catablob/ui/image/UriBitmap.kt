package software.blob.catablob.ui.image

import android.graphics.Bitmap

/**
 * A [Bitmap] with its associated source URI
 */
class UriBitmap(val uri: String, val bitmap: Bitmap) {

    override fun toString() = uri

    override fun hashCode() = uri.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UriBitmap
        if (uri != other.uri) return false
        return true
    }
}