package software.blob.catablob.util.rxjava

import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer

/**
 * A wrapper for [LambdaObserver] that supports nullable consumers
 */
class LambdaObserver<T : Any>(
    private val onNext: Consumer<in T>? = null,
    private val onError: Consumer<in Throwable>? = null,
    private val onComplete: Action? = null,
    private val onSubscribe: Consumer<in Disposable>? = null)
    : Observer<T> {

    private val lambdaObserver = io.reactivex.rxjava3.internal.observers.LambdaObserver<T>(
        { res -> onNext?.accept(res) },
        { error -> onError?.accept(error) },
        { onComplete?.run() },
        { d -> onSubscribe?.accept(d) }
    )

    override fun onSubscribe(d: Disposable) = lambdaObserver.onSubscribe(d)

    override fun onNext(t: T)  = lambdaObserver.onNext(t)

    override fun onError(e: Throwable) = lambdaObserver.onError(e)

    override fun onComplete() = lambdaObserver.onComplete()
}