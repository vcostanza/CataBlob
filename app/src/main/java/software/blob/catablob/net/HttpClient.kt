package software.blob.catablob.net

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import software.blob.catablob.util.rxjava.SimpleObservable
import java.util.concurrent.Executors

/**
 * Basic REST client tied into a [SimpleObservable]
 * @param <T> Processed response type
 */
abstract class HttpClient<T : Any> : SimpleObservable<T>() {

    private val client = OkHttpClient()
    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Schedule an HTTP request to execute
     * @param req Request to execute
     */
    fun request(req: Request) {
        executor.execute {
            try {
                Log.d(TAG, "HTTP request: ${req.url}")
                client.newCall(req).execute().use { response ->
                    val result = onResponse(response)
                    if (result != null) onNext(result)
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /**
     * Execute an HTTP request
     * @param url URL to execute request for
     */
    open fun request(url: String) = request(Request.Builder().url(url).build())

    /**
     * Execute an HTTP request
     * @param url URL to execute request for
     */
    open fun request(url: HttpUrl) = request(url.toString())

    /**
     * Method that is called when a response is received
     * @param res Raw response object
     * @return Converted response object or null if failed
     */
    abstract fun onResponse(res: Response) : T?

    /**
     * Shutdown this REST client instance
     */
    fun shutdown() {
        executor.shutdown()
        onComplete()
    }

    companion object {
        private const val TAG = "HttpClient"
    }
}