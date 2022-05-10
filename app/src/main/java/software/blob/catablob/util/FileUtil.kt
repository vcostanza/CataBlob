package software.blob.catablob.util

import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Get the file extension from a path string
 * @return File extension (dot excluded)
 */
val String.fileExtension: String get() {
    val lastDot = lastIndexOf('.')
    return if (lastDot != -1) substring(lastDot + 1) else ""
}

/**
 * Get the file name from a path string
 * @return File name
 */
val String.fileName: String get() {
    val lastSlash = lastIndexOf('/')
    return if (lastSlash != -1) substring(lastSlash + 1) else this
}

/**
 * Get the file's extension
 * @return File extension (dot excluded)
 */
val File.extension get() = absolutePath.fileExtension

/**
 * Get the SHA-256 hash of a string
 * @return SHA-256 hash of this string
 */
val String.sha256: String get() {
    val sha = sha256Digest.get() ?: return ""
    sha.update(toByteArray())
    return sha.digest().toHexString()
}

/**
 * Convert a byte array to a hex string
 * @return Hex string
 */
fun ByteArray.toHexString(): String {
    val hexString = StringBuilder(2 * size)
    for (i in this.indices) {
        val hex = Integer.toHexString(0xff and this[i].toInt())
        if (hex.length == 1) hexString.append('0')
        hexString.append(hex)
    }
    return hexString.toString()
}

/**
 * Get a date/time formatted file name based on the current UNIX time
 * @param ext File extension (null to ignore)
 * @return File name
 */
fun getDateTimeFileName(ext: String? = null): String {
    val date = Date()
    val name = fileDateFormat.get()?.format(date) ?: date.time.toString()
    return if (ext != null) "$name.$ext" else name
}

/**
 * Read a string from an [InputStream]
 * @param stream Stream to read
 * @param charset Charset to use
 * @return Encoded string
 */
fun readString(stream: InputStream, charset: Charset = Charset.defaultCharset()): String {
    val chunkSize = 8192
    val buf = ByteArray(chunkSize)
    var bytes = ByteBuffer.allocate(chunkSize)
    var len = 0
    var capacity = chunkSize
    var read: Int
    do {
        read = stream.read(buf)
        if (read > 0) {
            if (len + read > capacity) {
                capacity += chunkSize
                val expanded = ByteBuffer.allocate(capacity)
                expanded.put(bytes)
                bytes = expanded
            }
            bytes.put(buf, len, read)
            len += read
        }
    } while (read > 0)

    return String(bytes.array(), charset)
}

fun readJsonObject(stream: InputStream) = JSONObject(readString(stream))

fun readJsonArray(stream: InputStream) = JSONArray(readString(stream))

private val sha256Digest = object : ThreadLocal<MessageDigest>() {
    override fun initialValue() = MessageDigest.getInstance("SHA-256")
}

private val fileDateFormat = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US)
}