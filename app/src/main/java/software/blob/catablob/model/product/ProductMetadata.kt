package software.blob.catablob.model.product

import com.google.zxing.BarcodeFormat
import java.util.*

/**
 * Metadata for a product
 * @param uid Unique identifier string
 * @param name Name of this product
 * @param description Brief description of the product
 * @param brand Brand name
 * @param category Category
 * @param imageURI Link to an image of the product
 * @param code Product code (if applicable)
 * @param dbid Database identifier number (from database)
 */
data class ProductMetadata(
    val uid: String = UUID.randomUUID().toString(),
    var name: String,
    var description: String = "",
    var brand: String = "",
    var category: ProductCategory = ProductCategory.OTHER,
    var imageURI: String = "",
    var code: ProductCode = ProductCode("", BarcodeFormat.UPC_A),
    var dbid: Long = -1)