package software.blob.catablob.util

/**
 * Used when an unsupported image format is encountered
 */
class UnsupportedImageException(message: String = "", cause: Throwable? = null)
    : Exception(message, cause)