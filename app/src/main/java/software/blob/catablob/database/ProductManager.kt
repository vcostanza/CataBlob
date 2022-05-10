package software.blob.catablob.database

import android.util.Log
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.functions.Consumer
import software.blob.catablob.model.product.ProductMetadata
import software.blob.catablob.util.rxjava.LambdaObserver
import software.blob.catablob.util.rxjava.SimpleObservable
import java.util.PriorityQueue
import kotlin.collections.HashMap

private const val MAX_QUEUE = 1000

/**
 * Contains a cache of [ProductMetadata] pulled from the [CatalogDatabase]
 */
object ProductManager {

    private const val TAG = "ProductManager"

    private lateinit var database: CatalogDatabase

    private val queue = PriorityQueue<ProductMetadata>(MAX_QUEUE)
    private val idMap = HashMap<Long, ProductMetadata>(MAX_QUEUE)
    private val uidMap = HashMap<String, ProductMetadata>(MAX_QUEUE)

    // Event subscribers
    private val addObservable = SimpleObservable<List<ProductMetadata>>()
    private val removeObservable = SimpleObservable<List<ProductMetadata>>()

    // Products list getter
    val products: List<ProductMetadata> get() {
        synchronized(this) {
            return uidMap.values.toList()
        }
    }

    // Check if products list is empty
    val productsEmpty get() = uidMap.isEmpty()

    /**
     * Initialize the manager by setting the database instance
     * @param database Catalog database
     */
    fun init(database: CatalogDatabase) {
        this.database = database
        database.queryProducts(
            { products -> initProducts(products) },
            { error ->
                Log.e(TAG, "Failed to initialize products list", error)
                addObservable.onError(error)
            }
        )
    }

    /**
     * Subscribe to when products get added to the manager and database
     * @param observer Event observer
     */
    fun subscribeOnAdd(observer: Observer<in List<ProductMetadata>>) {
        addObservable.subscribe(observer)
        observer.onNext(this.products)
    }

    /**
     * Subscribe to when products get added to the manager and database
     * @param onAdd Callback containing newly added products
     * @param onError Error callback
     */
    fun subscribeOnAdd(onAdd: Consumer<in List<ProductMetadata>>,
                       onError: Consumer<in Throwable>) {
        subscribeOnAdd(LambdaObserver(onAdd, onError))
    }

    /**
     * Subscribe to when products get added to the manager and database
     * @param onAdd Callback containing newly added products
     */
    fun subscribeOnAdd(onAdd: Consumer<in List<ProductMetadata>>) {
        subscribeOnAdd(LambdaObserver(onAdd))
    }

    /**
     * Subscribe to when products get removed from the manager and database
     * @param observer Event observer
     */
    fun subscribeOnRemove(observer: Observer<in List<ProductMetadata>>) {
        removeObservable.subscribe(observer)
    }

    /**
     * Subscribe to when products get removed from the manager and database
     * @param onRemove Callback containing removed products
     * @param onError Error callback
     */
    fun subscribeOnRemove(onRemove: Consumer<in List<ProductMetadata>>,
                       onError: Consumer<in Throwable>) {
        subscribeOnRemove(LambdaObserver(onRemove, onError))
    }

    /**
     * Subscribe to when products get removed from the manager and database
     * @param onRemove Callback containing removed products
     */
    fun subscribeOnRemove(onRemove: Consumer<in List<ProductMetadata>>) {
        subscribeOnRemove(LambdaObserver(onRemove))
    }

    /**
     * Register product to this manager and the database
     * @param product Product to register
     */
    fun addProduct(product: ProductMetadata) {

        // Track if this product already exists and simply needs to be updated
        var update: Boolean

        // Register product to the manager
        synchronized(this) {
            update = product.uid in uidMap
            addProductImpl(product)
        }

        // Add the product to the database
        database.addProduct(product, update, { p ->
            // Register product by its database row ID
            synchronized(this) {
                idMap[product.dbid] = product
            }

            // Invoke callback
            addObservable.onNext(listOf(p))
        })
    }

    /**
     * Register a list of products to the manager and database
     * @param products Products to register
     */
    fun addProducts(products: List<ProductMetadata>) {
        // Register each product
        synchronized(this) {
            for (product in products) addProductImpl(product)
        }

        // Add products to the database
        database.addProducts(products, { p ->
            // Invoke callback
            addObservable.onNext(p)
        })
    }

    /**
     * Initialize the list of products read from the database
     * @param products Products to add
     */
    private fun initProducts(products: List<ProductMetadata>) {
        // Register each product
        synchronized(this) {
            for (product in products) addProductImpl(product)
        }

        // Invoke callback
        addObservable.onNext(products)
    }

    /**
     * Register product by its UID and (if available) database ID
     * @param product Product to register
     */
    private fun addProductImpl(product: ProductMetadata) {
        uidMap[product.uid] = product
        if (product.dbid != -1L) idMap[product.dbid] = product
    }

    /**
     * Remove a product from the manager and database
     * @param product Product to remove
     */
    fun removeProduct(product: ProductMetadata) {
        // Remove product mapping
        synchronized(this) {
            removeProductImpl(product)
        }

        // Remove product from the database
        database.removeProduct(product, { p ->
            removeObservable.onNext(listOf(p))
        })
    }

    /**
     * Remove a list of products from the manager and database
     * @param products Products to remove
     */
    fun removeProducts(products: List<ProductMetadata>) {
        // Register each product
        synchronized(this) {
            for (product in products) removeProductImpl(product)
        }

        // Add products to the database
        database.removeProducts(products, { p ->
            // Invoke callback
            removeObservable.onNext(p)
        })
    }

    /**
     * Remove a product from the ID/UID mapping
     * @param product Product to remove
     */
    private fun removeProductImpl(product: ProductMetadata) {
        uidMap.remove(product.uid)
        if (product.dbid != -1L) idMap.remove(product.dbid)
    }
}