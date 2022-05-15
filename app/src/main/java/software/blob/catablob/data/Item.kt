package software.blob.catablob.data

import androidx.room.*
import software.blob.catablob.model.item.ItemCondition

/**
 * An item is an individual unit of a given [Product]
 * Items are stored within collections via [ItemCollection]
 * @param id Unique ID
 * @param productUid Associated product UID
 * @param collectionUid Associated collection UID
 * @param condition The condition this item is in
 * @param price The raw price of this item
 * @param currency The currency of the price
 * @param notes Miscellaneous notes related to this item
 */
@Entity(
    tableName = TBL_ITEMS,
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = [COL_UID],
            childColumns = [COL_PRODUCT_UID]
        ),
        ForeignKey(
            entity = ItemCollection::class,
            parentColumns = [COL_UID],
            childColumns = [COL_COLLECTION_UID]
        )
    ],
    indices = [Index(COL_PRODUCT_UID), Index(COL_COLLECTION_UID)]
)
data class Item(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(name = COL_PRODUCT_UID) val productUid: String,
    @ColumnInfo(name = COL_COLLECTION_UID) val collectionUid: String,
    val condition: ItemCondition,
    val price: Double,
    val currency: String,
    val notes: String)