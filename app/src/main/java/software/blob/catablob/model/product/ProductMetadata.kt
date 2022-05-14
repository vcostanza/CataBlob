package software.blob.catablob.model.product

import android.content.Context
import androidx.core.math.MathUtils.clamp
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
    var dbid: Long = -1) {

    /**
     * For setting category by index via a Spinner view
     */
    var categoryIndex: Int get() {
        return category.ordinal
    } set(index) {
        if (index != category.ordinal)
            category = categories[clamp(index, 0, categories.size - 1)]
    }

        /**
         * Get the localized name of the category
         * @param context Application context used for localized string lookup
         * @return Category name
         */
        fun getCategoryName(context: Context) = category.getLocalizedName(context)

    companion object {
        private val categories = ProductCategory.values()
    }
}