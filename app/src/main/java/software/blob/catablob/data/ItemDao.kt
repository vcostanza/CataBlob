package software.blob.catablob.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * The Data Access Object for the [Item] class
 */
@Dao
interface ItemDao {

    /**
     * Get all items in the database
     * @return item List of items
     */
    @Query("SELECT * FROM $TBL_ITEMS")
    fun getItems(): Flow<List<Item>>

    /**
     * Query both the [Product] and [Item] tables and handle the object mapping
     * @return List of products mapped to their respective list of items
     */
    @Transaction
    @Query("SELECT * FROM $TBL_PRODUCTS WHERE $COL_UID IN (SELECT DISTINCT($COL_PRODUCT_UID) FROM $TBL_ITEMS)")
    fun getProductItems(): Flow<List<ProductAndItem>>

    /**
     * Insert/update an [Item] in the database
     * @param item Item to insert/update
     * @return Row ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    /**
     * Delete an item from the database
     * @param item Item to delete
     */
    @Delete
    suspend fun deleteItem(item: Item)
}