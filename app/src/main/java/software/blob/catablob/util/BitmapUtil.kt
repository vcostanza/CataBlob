package software.blob.catablob.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Compress a bitmap to a file
 * @param file File to save bitmap to
 * @param format Compression format (JPEG by default)
 * @param quality Compression quality (100 by default)
 */
fun Bitmap.compressToFile(
    file: File,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 100) {
    file.outputStream().use { stream -> compress(format, quality, stream) }
}

/**
 * Rotate a bitmap based on the image's EXIF data
 * @param byteArray Image byte array
 * @return Rotated bitmap
 */
fun Bitmap.exifRotate(byteArray: ByteArray): Bitmap {
    var exif: ExifInterface?
    ByteArrayInputStream(byteArray).use { stream -> exif = ExifInterface(stream) }
    val rotate = when (exif?.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )) {
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        else -> 0
    }
    if (rotate == 0) return this
    val matrix = Matrix()
    matrix.postRotate(rotate.toFloat())
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true);
}