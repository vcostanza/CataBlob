package software.blob.catablob.ui.image

import android.app.Activity
import android.content.Context
import android.graphics.*
import okhttp3.HttpUrl
import software.blob.catablob.util.compressToFile
import software.blob.catablob.util.sha256
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.log2

/**
 * Generates thumbnails from image files or [Bitmap] objects
 * @param context Application context
 * @param thumbSize Thumbnail dimensions in pixels
 * @param useCache True to enable thumbnail caching
 */
class ThumbnailGenerator(
    private val context: Context,
    private val thumbSize: Int = 128,
    private val useCache: Boolean = true)
    : ImageUriReader(context) {

    private val sizeLog2 = log2(thumbSize.toFloat())
    private val executor = Executors.newCachedThreadPool()

    /**
     * Shutdown the thumbnail generator
     */
    override fun shutdown() {
        super.shutdown()
        executor.shutdown()
    }

    /**
     * Request a thumbnail to be generated for the given image URL
     * @param url Image URL
     */
    override fun request(url: HttpUrl) {
        // Check if the file already exists in the download folder
        // or its corresponding cache file already exists
        val imageFile = getDownloadedImage(url)
        val cacheFile = getCacheFile(imageFile)
        if (cacheFile != null && cacheFile.exists()) {
            // Perform thumbnail generation on the already downloaded image
            readBitmap(url.toString(), imageFile)
        } else {
            // Download the image for processing
            super.request(url)
        }
    }

    /**
     * Execute a thumbnail request on the thread pool
     * @param imageUri Original image URI
     * @param imageFile File where the image is saved
     */
    override fun readBitmap(imageUri: String, imageFile: File) {
        executor.submit { createThumbnail(imageUri, imageFile) }
    }

    /**
     * Create a thumbnail bitmap for the given image URI
     * @param imageUri Original image URI
     * @param imageFile File where the image is saved
     */
    private fun createThumbnail(imageUri: String, imageFile: File) {

        // Check if cached thumbnail exists
        val cacheFile = getCacheFile(imageFile)
        if (useCache && cacheFile != null && cacheFile.exists()) {
            // Load thumbnail and return
            val thumb = BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (thumb != null) {
                onNext(UriBitmap(imageUri, thumb))
                return
            }
        }

        // Decode the bounds first
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imageFile.absolutePath, opts)

        // Determine sample size
        val maxDim = opts.outWidth.coerceAtLeast(opts.outHeight)
        val sampleSize = 1 shl (log2(maxDim.toFloat()) - sizeLog2).toInt()

        // Decode the scaled down bitmap
        opts.inJustDecodeBounds = false
        opts.inSampleSize = sampleSize.coerceAtLeast(1)
        var bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, opts) ?: return
        val width = bitmap.width
        val height = bitmap.height

        // Render to a canvas that matches the exact supplied image size
        if (width > thumbSize || height > thumbSize) {
            val ar = width.toFloat() / height.toFloat()
            val scaled = Bitmap.createBitmap(thumbSize, thumbSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(scaled)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val srcRect = Rect(0, 0, width, height)
            val dstRect = RectF(0f, 0f, thumbSize.toFloat(), thumbSize.toFloat())
            if (ar < 1f) {
                dstRect.left = (thumbSize - (thumbSize * ar)) / 2
                dstRect.right = dstRect.left + thumbSize * ar
            } else {
                dstRect.top = (thumbSize - (thumbSize / ar)) / 2
                dstRect.bottom = dstRect.top + thumbSize / ar
            }
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
            bitmap = scaled
        }

        // Save thumbnail to cache if applicable
        if (useCache && cacheFile != null) {
            try {
                bitmap.compressToFile(cacheFile)
            } catch (e: Exception) {
                onError(e)
            }
        }

        // Send thumbnail to observers
        (context as? Activity)?.runOnUiThread {
            onNext(UriBitmap(imageUri, bitmap))
        }
    }

    /**
     * Get the thumbnail cache file for a given image file
     * @param imageFile Source full-size image file
     * @return Cached thumbnail file
     */
    private fun getCacheFile(imageFile: File): File? {

        // Create thumbnails cache if it doesn't already exist
        val dir = File(context.cacheDir, "thumbnails")
        if (!dir.exists() && !dir.mkdirs()) return null

        // Generate SHA-256 hash of the image path
        val hash = imageFile.absolutePath.sha256

        // Name image after the SHA-256 hash in the thumbnails cache directory
        return File(dir, "${hash}_${thumbSize}.jpg")
    }
}