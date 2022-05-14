package software.blob.catablob.ui

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import software.blob.catablob.R
import software.blob.catablob.databinding.ProductRowBinding
import software.blob.catablob.model.product.ProductMetadata
import software.blob.catablob.ui.image.UriBitmap
import software.blob.catablob.ui.image.ThumbnailGenerator

/**
 * Adapter for displaying a list of products using the [ProductMetadata] model
 * @param context Application context
 * @param parent Parent list
 * @param onClick Click listener
 */
class ProductRowAdapter(
    context: Context,
    private val parent: ViewGroup,
    private val onClick: (ProductMetadata) -> Unit)
    : ListAdapter<ProductMetadata, ProductRowViewHolder>(ProductRowDiffCallback) {

    private val thumbGenerator = ThumbnailGenerator(context)
    private val views = ArrayList<ProductRowViewHolder>()
    private val selectMap = HashMap<String, ProductMetadata>()

    // Multi-select mode
    var multiSelect = false
        set(value) {
            if (field != value) {
                field = value
                if (!value) selectMap.clear()
                for (view in views) view.updateCheckbox()
            }
        }

    // The list of selected items
    var selected get() = selectMap.values.toList()
        set(value) {
            selectMap.clear()
            for (product in value)
                selectMap[product.uid] = product
        }

    init {
        // Update thumbnails for each product row
        thumbGenerator.subscribe(
            { thumb ->
                parent.post {
                    for (view in views) view.updateThumbnail(thumb)
                }
            },
            { e -> Log.e(TAG, "Failed to generate thumbnail", e)}
        )
    }

    /**
     * Shutdown the thumbnail generator
     */
    fun dispose() {
        thumbGenerator.shutdown()
        views.clear()
    }

    /**
     * Add a product to the selected list in multi-select mode
     */
    fun toggleSelect(product: ProductMetadata) {
        if (product.uid in selectMap)
            selectMap.remove(product.uid)
        else
            selectMap[product.uid] = product
    }

    /**
     * Check if a given product is selected in multi-select mode
     * @param product Product to check
     */
    fun isSelected(product: ProductMetadata) = multiSelect && selectMap.containsKey(product.uid)

    /**
     * Inflate a new product view holder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductRowViewHolder {
        val binding = ProductRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductRowViewHolder(binding, this, onClick)
    }

    /**
     * Bind the product to a view holder
     * @param holder View holder
     * @param position Position of the product in the list
     */
    override fun onBindViewHolder(holder: ProductRowViewHolder, position: Int) {
        val product = getItem(position)

        // Get cached thumbnail or generate one
        thumbGenerator.request(product.imageURI)

        holder.bind(product)
    }

    /**
     * Track which views are visible
     */
    override fun onViewAttachedToWindow(holder: ProductRowViewHolder) {
        views.add(holder)
    }

    /**
     * Track which views are visible
     */
    override fun onViewDetachedFromWindow(holder: ProductRowViewHolder) {
        views.remove(holder)
    }

    companion object {
        private const val TAG = "ProductAdapter"
    }
}

/**
 * View holder for [ProductMetadata]
 * @param binding Data binding interface
 * @param adapter Product adapter
 * @param onClick Click event listener
 */
class ProductRowViewHolder(
    private val binding: ProductRowBinding,
    private val adapter: ProductRowAdapter,
    private val onClick: (ProductMetadata) -> Unit)
    : RecyclerView.ViewHolder(binding.root) {

    private val imageURI get() = binding.product?.imageURI

    /**
     * Setup the click listener for each product row
     */
    init {
        itemView.setOnClickListener { binding.product?.let {
            if (adapter.multiSelect) {
                adapter.toggleSelect(it)
                updateCheckbox()
            } else
                onClick(it)
        }}
    }

    /**
     * Update views based on latest [ProductMetadata]
     * @param product Product metadata
     */
    fun bind(product: ProductMetadata) {
        binding.product = product
        binding.thumbnail.setImageResource(R.drawable.ic_placeholder)
        updateCheckbox()
    }

    /**
     * Update the checkbox visibility state
     */
    fun updateCheckbox() {
        binding.product?.let {
            binding.checkbox.visibility = if (adapter.multiSelect) View.VISIBLE else View.GONE
            binding.checkbox.isChecked = adapter.isSelected(it)
        }
    }

    /**
     * Update the thumbnail for this row if the image URI matches
     * @param thumb Thumbnail (bitmap + uri)
     */
    fun updateThumbnail(thumb: UriBitmap) {
        if (thumb.uri == imageURI)
            binding.thumbnail.setImageBitmap(thumb.bitmap)
    }

    override fun toString() = binding.product?.toString() ?: "<empty>"
}

/**
 * Callback used to check if 2 products are the same
 */
object ProductRowDiffCallback : DiffUtil.ItemCallback<ProductMetadata>() {
    override fun areItemsTheSame(old: ProductMetadata, new: ProductMetadata): Boolean {
        return old.uid == new.uid
    }

    override fun areContentsTheSame(old: ProductMetadata, new: ProductMetadata): Boolean {
        return old.name == new.name &&
                old.category == new.category &&
                old.brand == new.brand &&
                old.description == new.description &&
                old.code == new.code
    }
}