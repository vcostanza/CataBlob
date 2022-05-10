package software.blob.catablob.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.zxing.BarcodeFormat
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.functions.Consumer
import software.blob.catablob.model.product.ProductCategory
import software.blob.catablob.model.product.ProductCode
import software.blob.catablob.model.product.ProductMetadata
import software.blob.catablob.util.getString
import software.blob.catablob.util.rxjava.LambdaObserver
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "CatalogDatabase"
private const val DB_NAME = "catalog"
private const val DB_VERSION = 2

// Table names
private const val TBL_PRODUCTS = "products"
private const val TBL_COLLECTIONS = "collections"
private const val TBL_ITEMS = "items"

// Column names
private const val COL_ID = "id"
private const val COL_NAME = "name"
private const val COL_DESC = "description"
// Product specific
private const val COL_UID = "uid"
private const val COL_BRAND = "brand"
private const val COL_CATEGORY = "category"
private const val COL_CODE = "code"
private const val COL_CODE_FORMAT = "code_format"
private const val COL_IMAGE_URI = "image_uri"
// Item specific
private const val COL_PRODUCT_ID = "product_id"
private const val COL_COLLECTION_ID = "collection_id"
private const val COL_CONDITION = "condition"
private const val COL_PRICE = "price"
private const val COL_CURRENCY = "currency"
private const val COL_NOTES = "notes"

/**
 * Database that contains the list of product metadata, collections, and items
 * @param context Application context
 */
class CatalogDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    /**
     * Database operations are run on this thread
     */
    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Create the tables for the database
     * @param db SQLite database instance
     */
    override fun onCreate(db: SQLiteDatabase) {
        // Create products table
        db.execSQL("CREATE TABLE $TBL_PRODUCTS ("
                + "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COL_UID TEXT, "
                + "$COL_NAME TEXT, "
                + "$COL_DESC TEXT, "
                + "$COL_BRAND TEXT, "
                + "$COL_CATEGORY TEXT, "
                + "$COL_CODE TEXT, "
                + "$COL_CODE_FORMAT TEXT, "
                + "$COL_IMAGE_URI TEXT)")

        // Create collections table
        db.execSQL("CREATE TABLE $TBL_COLLECTIONS ("
                + "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COL_NAME TEXT, "
                + "$COL_DESC TEXT)")

        // Create items table
        db.execSQL("CREATE TABLE $TBL_ITEMS ("
                + "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COL_PRODUCT_ID INTEGER, "
                + "$COL_COLLECTION_ID INTEGER, "
                + "$COL_CONDITION TEXT, "
                + "$COL_PRICE DOUBLE, "
                + "$COL_CURRENCY TEXT, "
                + "$COL_NOTES TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Remove all tables
        db.execSQL("DROP TABLE IF EXISTS $TBL_PRODUCTS")
        db.execSQL("DROP TABLE IF EXISTS $TBL_COLLECTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TBL_ITEMS")

        // Create fresh new database
        onCreate(db)
    }

    fun copyDatabaseFile(dest: File) {
        val dbFile = File(writableDatabase.path)
        if (dbFile.exists() && dbFile.isFile) {
            dbFile.copyTo(dest, true)
        }
    }

    /**
     * Add a product to the database
     * @param product Product to add
     * @param update True to update based on uid rather than insert new
     * @param onSuccess Callback invoked when successful
     * @param onError Error callback
     */
    fun addProduct(product: ProductMetadata,
                   update: Boolean = false,
                   onSuccess: Consumer<ProductMetadata>,
                   onError: Consumer<Throwable>? = null) {
        val observer = LambdaObserver(onSuccess, onError, null, null)
        executor.execute { addProductImpl(product, update, observer) }
    }

    /**
     * Add a product to the database
     * @param product Product to add
     * @param update True to update based on uid rather than insert new
     * @param callback Success and error callback
     */
    private fun addProductImpl(product: ProductMetadata,
                               update: Boolean = false,
                               callback: Observer<ProductMetadata>) {
        writableDatabase.beginTransaction()
        try {
            insertProducts(listOf(product), update)
            writableDatabase.setTransactionSuccessful()
            callback.onNext(product)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add product", e)
            callback.onError(e)
        } finally {
            writableDatabase.endTransaction()
        }
        callback.onComplete()
    }

    /**
     * Add a list of products to the database
     * @param products Products to add
     * @param onSuccess Callback invoked when successful
     * @param onError Error callback
     */
    fun addProducts(products: List<ProductMetadata>,
                   onSuccess: Consumer<List<ProductMetadata>>,
                   onError: Consumer<Throwable>? = null) {
        val observer = LambdaObserver(onSuccess, onError, null, null)
        executor.execute { addProductsImpl(products, observer) }
    }

    /**
     * Add a list of products to the database
     * @param products Products to add
     * @param callback Success and error callback
     */
    private fun addProductsImpl(products: List<ProductMetadata>,
                                callback: Observer<List<ProductMetadata>>) {
        writableDatabase.beginTransaction()
        try {
            insertProducts(products)
            writableDatabase.setTransactionSuccessful()
            callback.onNext(products)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add product", e)
            callback.onError(e)
        } finally {
            writableDatabase.endTransaction()
        }
        callback.onComplete()
    }

    /**
     * Insert new product rows into the database
     * @param products List of products to insert
     * @param update True to perform an update based on uid instead of an insert
     */
    private fun insertProducts(products: List<ProductMetadata>, update: Boolean = false) {
        for (product in products) {
            // Setup the values to insert into the new row
            val values = ContentValues()
            values.put(COL_UID, product.uid)
            values.put(COL_NAME, product.name)
            values.put(COL_DESC, product.description)
            values.put(COL_BRAND, product.brand)
            values.put(COL_CATEGORY, product.category.toString())
            values.put(COL_IMAGE_URI, product.imageURI)
            values.put(COL_CODE, product.code?.code ?: "")
            values.put(COL_CODE_FORMAT, product.code?.format?.toString() ?: "")

            // Insert/update the product into the database
            if (update)
                writableDatabase.update(TBL_PRODUCTS, values, "$COL_UID = ?", arrayOf(product.uid))
            else {
                val rowId = writableDatabase.insertOrThrow(TBL_PRODUCTS, null, values)

                // Update the database ID for easy item lookup later
                product.dbid = rowId
            }
        }
    }

    /**
     * Remove a product from the database
     * @param product Product to remove
     * @param onSuccess Callback invoked when successful
     * @param onError Error callback
     */
    fun removeProduct(product: ProductMetadata,
                   onSuccess: Consumer<ProductMetadata>,
                   onError: Consumer<Throwable>? = null) {
        val observer = LambdaObserver(onSuccess, onError, null, null)
        executor.execute { removeProductImpl(product, observer) }
    }

    /**
     * Remove a list of products from the database
     * @param products Products to remove
     * @param onSuccess Callback invoked when successful
     * @param onError Error callback
     */
    fun removeProducts(products: List<ProductMetadata>,
                      onSuccess: Consumer<List<ProductMetadata>>,
                      onError: Consumer<Throwable>? = null) {
        val observer = LambdaObserver(onSuccess, onError, null, null)
        executor.execute { removeProductsImpl(products, observer) }
    }

    /**
     * Remove a list of products from the database
     * @param products Products to remove
     * @param callback Success and error callback
     */
    private fun removeProductsImpl(products: List<ProductMetadata>,
                                   callback: Observer<List<ProductMetadata>>) {
        writableDatabase.beginTransaction()
        try {
            for (product in products)
                writableDatabase.delete(TBL_PRODUCTS, "$COL_UID = ?", arrayOf(product.uid))
            writableDatabase.setTransactionSuccessful()
            callback.onNext(products)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove products", e)
            callback.onError(e)
        } finally {
            writableDatabase.endTransaction()
        }
        callback.onComplete()
    }

    /**
     * Remove a product from the database
     * @param product Product to remove
     * @param callback Success and error callback
     */
    private fun removeProductImpl(product: ProductMetadata,
                                  callback: Observer<ProductMetadata>) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete(TBL_PRODUCTS, "$COL_UID = ?", arrayOf(product.uid))
            writableDatabase.setTransactionSuccessful()
            callback.onNext(product)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove product", e)
            callback.onError(e)
        } finally {
            writableDatabase.endTransaction()
        }
        callback.onComplete()
    }

    /**
     * Query all products in the database
     * @param onSuccess Callback where the products are returned
     * @param onError Error callback (null to ignore)
     */
    fun queryProducts(onSuccess: Consumer<List<ProductMetadata>>,
                      onError: Consumer<Throwable>? = null) {
        val observer = LambdaObserver(onSuccess, onError, null, null)
        executor.execute { queryProductsImpl(observer) }
    }

    /**
     * Query all products in the database
     * @param callback Callback observer to return results and errors
     */
    private fun queryProductsImpl(callback: Observer<List<ProductMetadata>>) {
        try {
            val cursor = readableDatabase.query(TBL_PRODUCTS, null, null, null, null, null, null)
            cursor.use {
                val products = ArrayList<ProductMetadata>()
                while (cursor.moveToNext()) {
                    products += ProductMetadata(
                        uid = cursor.getString(COL_UID),
                        name = cursor.getString(COL_NAME),
                        description = cursor.getString(COL_DESC),
                        brand = cursor.getString(COL_BRAND),
                        category = cursor.getProductCategory(),
                        imageURI = cursor.getString(COL_IMAGE_URI),
                        code = cursor.getProductCode()
                    )
                }
                callback.onNext(products)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query products", e)
            callback.onError(e)
        }
        callback.onComplete()
    }
}

/**
 * Get a [ProductCategory] value from a database cursor using the column name
 * @return Product category in [ProductCategory] form
 * @throws [IllegalArgumentException] if the column does not exist
 */
private fun Cursor.getProductCategory() = ProductCategory.valueOf(getString(COL_CATEGORY))

/**
 * Get a [ProductCode] value from a database cursor using the column name
 * @return Product code
 * @throws [IllegalArgumentException] if the column does not exist
 */
private fun Cursor.getProductCode(): ProductCode {
    return ProductCode(getString(COL_CODE),
        BarcodeFormat.valueOf(getString(COL_CODE_FORMAT)))
}