package software.blob.catablob.data

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Pairs a [Product] with a list of [Item]s that refer to that product
 * @param product The product
 * @param items List of items that use the product
 */
data class ProductAndItem(

    @Embedded
    val product: Product,

    @Relation(parentColumn = COL_UID, entityColumn = COL_PRODUCT_UID)
    val items: List<Item> = emptyList()
)