package software.blob.catablob.model.item

import software.blob.catablob.model.product.ProductMetadata

/**
 * An individual product item
 * @param product Product metadata
 * @param condition Item condition
 * @param price The price of the item (null = not applicable)
 * @param notes Any additional notes for this item
 */
data class Item(
    val product: ProductMetadata,
    val condition: ItemCondition,
    val price: ItemPrice? = null,
    val notes: String = "")