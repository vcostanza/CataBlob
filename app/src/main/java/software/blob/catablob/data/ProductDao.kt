package software.blob.catablob.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * The Data Access Object for the [Product] class
 */
@Dao
interface ProductDao {

    /**
     * Get a product given its UID
     * @param uid Product UID
     * @return Matching product
     */
    @Query("SELECT * FROM $TBL_PRODUCTS WHERE uid = :uid")
    fun getProduct(uid: String): Flow<Product>

    /**
     * Get all products in the database
     * @return List of products
     */
    @Query("SELECT * FROM $TBL_PRODUCTS ORDER BY name")
    fun getProducts(): Flow<List<Product>>

    /**
     * Search the list of products in the database
     * @param terms Search terms
     * @return List of matches
     */
    @Query("SELECT * FROM $TBL_PRODUCTS WHERE $COL_NAME LIKE '%' || :terms || '%' ORDER BY name")
    fun searchProducts(terms: String): Flow<List<Product>>

    /**
     * Insert/update a product in the database
     * @param product Product to add
     * @return Row ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    /**
     * Insert/update a list of products into the database
     * @param products List of products to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    /**
     * Delete a product from the database
     * @param product Product to delete
     */
    @Delete
    suspend fun deleteProduct(product: Product)

    /**
     * Delete a list of products from the database
     * @param products Products to delete
     */
    @Delete
    suspend fun deleteProducts(products: List<Product>)
}