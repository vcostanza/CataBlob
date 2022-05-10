package software.blob.catablob.ui.image

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import software.blob.catablob.net.DownloadClient
import software.blob.catablob.net.FileDownload
import software.blob.catablob.util.fileExtension
import software.blob.catablob.util.rxjava.SimpleObservable
import software.blob.catablob.util.sha256
import software.blob.catablob.util.tempDirectory
import java.io.File
import java.io.FileNotFoundException

/**
 * Reads images from a URI and decodes to a [UriBitmap]
 * @param context Application context
 */
open class ImageUriReader(private val context: Context) : SimpleObservable<UriBitmap>() {

    private val client = DownloadClient(context.tempDirectory, true)

    init {
        client.subscribe(
            { download -> onDownloadComplete(download) },
            { err -> onError(err) }
        )
    }

    /**
     * Shutdown the download client
     */
    open fun shutdown() = client.shutdown()

    /**
     * Image has finished being downloaded - time to read it to a bitmap
     * @param download Downloaded image file
     */
    open fun onDownloadComplete(download: FileDownload) = readBitmap(download.url, download.file)

    /**
     * Request an image from the given file
     * @param file Image file
     */
    open fun request(file: File) = readBitmap(file.absolutePath, file)

    /**
     * Request image for the given image URL
     * @param url Image URL
     */
    open fun request(url: HttpUrl) {
        // Check if the file already exists in the download folder
        val imageFile = getDownloadedImage(url)
        if (imageFile.exists()) {
            // Perform thumbnail generation on the already downloaded image
            readBitmap(url.toString(), imageFile)
        } else {
            // Download the image for processing
            client.request(url)
        }
    }

    /**
     * Request an image from the given image URI
     * @param uri Image URI
     */
    open fun request(uri: Uri) = request(uri.toString())

    /**
     * Request an image from the given image URI
     * @param uri Image URI string
     */
    open fun request(uri: String) {
        if (uri.isEmpty()) return
        when (Uri.parse(uri).scheme ?: "file") {
            "file" -> request(File(uri))
            "http", "https" -> uri.toHttpUrlOrNull()?.let { request(it) }
        }
    }

    /**
     * Get the downloaded image file for a given URL
     * @param url Image URL
     */
    fun getDownloadedImage(url: HttpUrl): File {
        val path = url.encodedPath
        return File(context.tempDirectory, "${path.sha256}.${path.fileExtension}")
    }

    /**
     * Read a bitmap from an image file
     * @param imageUri Source image URI
     * @param imageFile File where the image is saved
     */
    protected open fun readBitmap(imageUri: String, imageFile: File) {

        // Decode the bitmap from the image file
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

        // Failed to decode bitmap
        if (bitmap == null) {
            onError(FileNotFoundException("Image bitmap could not be read: $imageFile"))
            return
        }

        // Send the URI and bitmap to the subscribers
        onNext(UriBitmap(imageUri, bitmap))
    }
}