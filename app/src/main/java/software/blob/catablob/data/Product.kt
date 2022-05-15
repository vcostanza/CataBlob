package software.blob.catablob.data

import android.content.Context
import androidx.core.math.MathUtils
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.zxing.BarcodeFormat
import software.blob.catablob.model.product.ProductCategory
import java.util.*

/**
 * Metadata for a given store product
 * @param uid Unique ID
 * @param name Product name
 * @param brand Product brand/manufacturer
 * @param category Product category (i.e. Food, Electronics, etc.)
 * @param code Barcode (if applicable)
 * @param codeType Barcode type (if applicable)
 * @param imageUri URI pointing to an image of the product
 * @param notes Miscellaneous notes related to this product
 */
@Entity(tableName = TBL_PRODUCTS)
data class Product(
    @PrimaryKey val uid: String = UUID.randomUUID().toString(),
    var name: String,
    var brand: String = "",
    var category: ProductCategory = ProductCategory.OTHER,
    var code: String = "",
    var codeType: BarcodeFormat = BarcodeFormat.UPC_A,
    var imageUri: String = "",
    var notes: String = "") {

    /**
     * For setting category by index via a Spinner view
     */
    var categoryIndex: Int get() {
        return category.ordinal
    } set(index) {
        if (index != category.ordinal)
            category = categories[MathUtils.clamp(index, 0, categories.size - 1)]
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

    override fun toString() = name
}