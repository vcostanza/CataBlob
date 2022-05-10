package software.blob.catablob.util.rxjava

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.subjects.PublishSubject
import software.blob.catablob.model.product.ProductMetadata

/**
 * An [Observable] that has a default [PublishSubject]
 */
open class SimpleObservable<T : Any> : Observable<T>() {

    private val publisher = PublishSubject.create<T>()

    /**
     * Implementation for subscribing to this observable
     * @param observer Observer to subscribe
     */
    override fun subscribeActual(observer: Observer<in T>) = publisher.subscribe(observer)

    fun onNext(value: T) = publisher.onNext(value)

    fun onError(value: Throwable) = publisher.onError(value)

    fun onComplete() = publisher.onComplete()
}