package software.blob.catablob.net

import okhttp3.Response
import software.blob.catablob.util.fileExtension
import software.blob.catablob.util.fileName
import software.blob.catablob.util.sha256
import java.io.File
import java.io.FileOutputStream

/**
 * File downloader client
 * @param outDir Download directory for files
 * @param hashNames True to use the hash as the file name
 */
class DownloadClient(
    private val outDir: File,
    private val hashNames: Boolean = false)
    : HttpClient<FileDownload>() {

    override fun onResponse(res: Response): FileDownload? {

        val path = res.request.url.encodedPath
        val name = path.fileName
        val ext = path.fileExtension
        val outFile = File(outDir, if (hashNames) "${path.sha256}.$ext" else name)
        val buf = ByteArray(8196)
        val stream = res.body?.byteStream() ?: return null

        // Write downloaded content to file
        stream.use { input ->
            FileOutputStream(outFile).use { output ->
                var read: Int
                do {
                    read = input.read(buf)
                    if (read > 0) output.write(buf, 0, read)
                } while (read > -1)
            }
        }

        return FileDownload(res.request.url.toString(), outFile)
    }
}