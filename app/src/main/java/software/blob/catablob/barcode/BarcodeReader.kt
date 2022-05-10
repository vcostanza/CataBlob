package software.blob.catablob.barcode

import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import software.blob.catablob.util.rxjava.SimpleObservable
import java.util.concurrent.Executors

/**
 * Used for reading barcodes from camera preview
 */
class BarcodeReader : SimpleObservable<Result>(), ImageAnalysis.Analyzer {

    private val executor = Executors.newSingleThreadExecutor()
    private val reader = MultiFormatReader()

    /**
     * The image analysis use case for [ProcessCameraProvider.bindToLifecycle]
     */
    val useCase = ImageAnalysis.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also { it.setAnalyzer(executor, this) }

    /**
     * Dispose the barcode reader
     */
    fun dispose() {
        useCase.clearAnalyzer()
        executor.shutdown()
        onComplete()
    }

    /**
     * Analyze a frame of the camera preview for barcodes
     * @param image Preview image proxy
     */
    override fun analyze(image: ImageProxy) {

        // Analyze both non-rotated and rotated camera image
        if (!analyze(image, false)) analyze(image, true)

        // Free the image proxy so we can continue processing frames
        image.close()
    }

    /**
     * Analyze a frame of the camera preview for barcodes
     * @param image Preview image proxy
     * @param rotated True to rotate the image 90 degrees before analyzing
     * @return True if a barcode was successfully analyzed
     */
    private fun analyze(image: ImageProxy, rotated: Boolean = false): Boolean {
        // Convert preview frame to binary bitmap for using with the format reader
        val bitmap = image.toBinaryBitmap(rotated)

        // Decode bitmap to valid barcode
        var res: Result? = null
        try {
            res = reader.decodeWithState(bitmap)
        } catch (re: ReaderException) {
            // No barcode visible (this will happen very often)
        } catch (e: Exception) {
            // Other exception
            onError(e)
        } finally {
            // Reset all the barcode readers
            reader.reset()
        }

        // Send barcode info to the observer and return success
        if (res != null)  {
            onNext(res)
            return true
        }

        // Unsuccessful
        return false
    }

    /**
     * Convert a YUV_420_888 [ImageProxy] to a binary bitmap for barcode scanning
     * @param rotated True to rotate the image 90 degrees
     * @return Binary bitmap
     */
    private fun ImageProxy.toBinaryBitmap(rotated: Boolean = false): BinaryBitmap {

        // The luminance data buffer (we don't care about U and V here)
        val srcY = this.planes[0].buffer
        srcY.rewind()

        // Create a copy for processing
        val dstY = ByteArray(this.width * this.height)
        val dstW: Int
        val dstH: Int

        if (rotated) {
            // 90-degree rotated copy so the user doesn't need to hold the phone at a specific
            // orientation to scan successfully
            var dstIdx = 0
            for (x in 0 until width) {
                var srcIdx = dstY.size - width + x
                while (srcIdx >= 0) {
                    dstY[dstIdx++] = srcY.get(srcIdx)
                    srcIdx -= width
                }
            }
            dstW = this.height
            dstH = this.width
        } else {
            // Non-rotated copy
            srcY.get(dstY)
            dstW = this.width
            dstH = this.height
        }

        val src = PlanarYUVLuminanceSource(dstY, dstW, dstH, 0, 0, dstW, dstH, false)
        return BinaryBitmap(HybridBinarizer(src))
    }

    companion object {
        private const val TAG = "BarcodeReader"
    }
}