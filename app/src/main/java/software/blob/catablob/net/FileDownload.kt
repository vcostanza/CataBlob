package software.blob.catablob.net

import java.io.File

/**
 * A downloaded [File] and its associated URL string
 * @param url URL the file was downloaded from
 * @param file The downloaded file path
 */
data class FileDownload(val url: String, val file: File)