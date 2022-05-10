package software.blob.catablob.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import software.blob.catablob.model.product.ProductCategory

/**
 * A spinner that displays the complete list of [ProductCategory] values
 */
class ProductCategorySpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0)
    : AppCompatSpinner(context, attrs, defStyleAttr) {

    private val categoryAdapter = ProductCategoryAdapter(context)

    var category: ProductCategory get() {
        return categoryAdapter.getCategory(selectedItemPosition)
    } set(category) {
        setSelection(categoryAdapter.indexOf(category))
    }

    init {
        adapter = categoryAdapter
    }
}

/**
 * View adapter for each product category
 * @param context Application context
 */
private class ProductCategoryAdapter(context: Context)
    : ArrayAdapter<ProductCategoryItem>(context, android.R.layout.simple_spinner_item) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        for (category in ProductCategory.values())
            add(ProductCategoryItem(context, category))
    }

    /**
     * Get the [ProductCategory] enum value at the given position
     * @param position Position index
     * @return Product category
     */
    fun getCategory(position: Int): ProductCategory {
        val item = getItem(position)
        return item?.category ?: ProductCategory.OTHER
    }

    /**
     * Get the position that a given category is located within this adapter
     * @param category Product category
     * @return Position (zero if not found)
     */
    fun indexOf(category: ProductCategory): Int {
        for (i in 0 until count) {
            val item = getItem(i)
            if (item?.category == category)
                return i
        }
        return 0
    }
}

/**
 * Product category wrapper for proper text display in the spinner
 * @param context Spinner context
 * @param category Category enum value
 */
private class ProductCategoryItem(
    private val context: Context,
    val category: ProductCategory) {

    override fun toString() = category.getDescription(context)
}