package software.blob.catablob.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

private const val DB_NAME = "catablob"

// Table names
const val TBL_PRODUCTS = "products"
const val TBL_COLLECTIONS = "collections"
const val TBL_ITEMS = "items"

// Column names
const val COL_NAME = "name"
const val COL_UID = "uid"

// Item specific
const val COL_PRODUCT_UID = "product_uid"
const val COL_COLLECTION_UID = "collection_uid"

/**
 * The Room database for this app
 */
@Database(entities = [Product::class, Item::class, ItemCollection::class],
    version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Get the product data access object (DAO)
     * @return Product DAO
     */
    abstract fun productDao(): ProductDao

    /**
     * Get the item data access object (DAO)
     * @return Item DAO
     */
    abstract fun itemDao(): ItemDao

    /**
     * Get the collection data access object (DAO)
     * @return Collection DAO
     */
    abstract fun collectionDao(): ItemCollectionDao

    companion object {

        // Singleton instantiation
        @Volatile private var instance: AppDatabase? = null

        /**
         * Get the app database instance
         * @param context Application context
         * @return App database
         */
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .build()
        }
    }
}