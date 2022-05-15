package software.blob.catablob.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * The Data Access Object for the [ItemCollection] class
 */
@Dao
interface ItemCollectionDao {

    /**
     * Get a collection given its UID
     * @param uid Collection UID
     * @return Collection
     */
    @Query("SELECT * FROM $TBL_COLLECTIONS WHERE uid = :uid")
    fun getCollection(uid: String): Flow<ItemCollection>

    /**
     * Get all collections in the database
     * @return Collection list
     */
    @Query("SELECT * FROM $TBL_COLLECTIONS ORDER BY name")
    fun getCollections(): Flow<List<ItemCollection>>

    /**
     * Insert a collection into the database
     * @param collection Collection to insert
     * @return Row ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: ItemCollection): Long

    /**
     * Delete a collection from the database
     * @param collection Collection to delete
     */
    @Delete
    suspend fun deleteCollection(collection: ItemCollection)
}