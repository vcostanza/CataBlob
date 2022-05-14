package software.blob.catablob.navigation

import android.os.Bundle
import android.util.Log
import android.view.*

import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import software.blob.catablob.R
import software.blob.catablob.database.ProductManager
import software.blob.catablob.databinding.FragmentProductsBinding
import software.blob.catablob.model.product.ProductCode
import software.blob.catablob.model.product.ProductMetadata
import software.blob.catablob.net.PRODUCT_NOT_FOUND
import software.blob.catablob.net.UpcItemDbClient
import software.blob.catablob.ui.ProductRowAdapter
import software.blob.catablob.ui.dialog.ProductDialog
import software.blob.catablob.ui.dialog.TileButtonDialog
import software.blob.catablob.util.*
import java.util.concurrent.Executors

/**
 * The screen for viewing and managing registered products
 */
class ProductsFragment : BaseFragment(R.string.products, R.menu.products_menu),
    Observer<List<ProductMetadata>> {

    private lateinit var viewModel: ProductsViewModel
    private lateinit var binding: FragmentProductsBinding
    private lateinit var adapter: ProductRowAdapter
    private lateinit var upcLookup: UpcItemDbClient

    private val disposables = ArrayList<Disposable>()
    private var productDialog: ProductDialog? = null

    // Display modes
    private val deleteMode = Mode(R.string.remove_products, R.menu.delete_menu)
    private var searchEnabled = false

    // Search function
    private val searchExecutor = Executors.newSingleThreadExecutor()
    private val searchTerms get() = binding.searchBar.searchTerms.text.toString()

    /**
     * Fragment created - initialize view model and
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create/load view model
        viewModel = ViewModelProvider(this).get(ProductsViewModel::class.java)

        // REST service for UPC lookup
        upcLookup = UpcItemDbClient()
        upcLookup.subscribe { product ->

            // If the product wasn't found the notify the user
            if (product == PRODUCT_NOT_FOUND) {
                Snackbar.make(requireView(), R.string.no_product_found, Snackbar.LENGTH_SHORT)
                    .show()
                return@subscribe
            }

            // Open the product dialog showing this new scan result
            activity?.runOnUiThread { openProductDialog(product) }
        }

        // Barcode scanner callback
        setFragmentResultListener(BARCODE_REQUEST_KEY) { _, bundle ->
            val barcode = bundle.getString("barcode")
            val format = bundle.getSerializable("format") as? BarcodeFormat ?: BarcodeFormat.UPC_A

            if (barcode != null) {
                // Valid barcode found - attempt lookup using REST service
                Log.d(TAG, "Requesting barcode information: $barcode")
                upcLookup.request(ProductCode(barcode, format))
            } else {
                // Invalid result
                Log.d(TAG, "Invalid barcode returned by camera")
                Snackbar.make(requireView(), R.string.no_barcode_found, Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    /**
     * Create the fragment view
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)

        binding = FragmentProductsBinding.inflate(inflater)

        val ctx = requireContext()

        // Setup products list
        val productList = binding.productList
        adapter = ProductRowAdapter(ctx, productList) { product -> openProductDialog(product) }
        productList.layoutManager = LinearLayoutManager(ctx)
        productList.adapter = adapter
        ProductManager.subscribeOnAdd(this)
        ProductManager.subscribeOnRemove(this)

        // Setup search bar
        binding.searchBar.let {
            it.searchTerms.addTextChangedListener { refresh() }
            it.cancel.setOnClickListener { toggleSearch(false) }
        }

        return binding.root
    }

    /**
     * Restore view state from view model
     */
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Reload product dialog state
        viewModel.productDialog?.let {
            openProductDialog(it.product)
            productDialog?.viewState = it
        }
        viewModel.productDialog = null

        // Restore search bar
        viewModel.searchTerms?.let {
            toggleSearch(true)
            binding.searchBar.searchTerms.setText(it)
        }

        // Restore delete mode
        viewModel.deleteProducts?.let {
            pushMode(deleteMode)
            adapter.selected = it
        }
    }

    /**
     * The fragment view has been destroyed - save view state to view model
     */
    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        adapter.dispose()
        for (d in disposables) d.dispose()
        disposables.clear()

        // Save state of the product dialog
        productDialog?.let {
            viewModel.productDialog = it.viewState
            it.dismiss()
        }

        // Save search terms if there are any
        viewModel.searchTerms = if (searchEnabled) searchTerms else null

        // Save products selected in delete mode
        viewModel.deleteProducts = if (currentMode == deleteMode) adapter.selected else null
    }

    /**
     * The fragment has been destroyed - shut down services
     */
    override fun onDestroy() {
        super.onDestroy()
        upcLookup.shutdown()
    }

    /**
     * Callback for when a menu button is selected
     * @param item The menu button
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            // Prompt to add a new product
            R.id.add_product -> promptAddProduct()

            // Remove a product from the list
            R.id.remove_products -> {
                if (ProductManager.productsEmpty)
                    Snackbar.make(requireView(),
                        R.string.no_products_to_remove,
                        Snackbar.LENGTH_SHORT).show()
                else
                    pushMode(deleteMode)
            }

            // Open the product search bar
            R.id.search_products -> toggleSearch()

            // Confirm removal on selected products
            R.id.confirm_removal -> promptRemoval()

            // Cancel out of current mode
            R.id.cancel -> popMode()

            // Unhandled button
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * Update view based on display mode
     * @param mode Display mode
     */
    override fun onSetMode(mode: Mode) {
        super.onSetMode(mode)

        // Update multi-select state
        adapter.multiSelect = mode == deleteMode
    }

    /**
     * Track the list of subscriptions we must dispose on shutdown
     */
    override fun onSubscribe(d: Disposable) {
        disposables += d
    }

    /**
     * The list of products has been modified - refresh the view
     * @param products Products added or removed
     */
    override fun onNext(products: List<ProductMetadata>) {
        activity?.runOnUiThread { refresh() }
    }

    /**
     * A fatal error has occurred while adding/removing products
     * @param e Error
     */
    override fun onError(e: Throwable) {
        Log.e(TAG, "Failed to process products change", e)
    }

    /**
     * Product manager is shutdown
     */
    override fun onComplete() {
    }

    /**
     * Refresh the current displayed list of products
     */
    private fun refresh() {

        // If search is disabled or the search terms are empty just display
        // the entire products list
        val terms = searchTerms
        if (!searchEnabled || terms.isEmpty()) {
            val products = ProductManager.products

            // Show the full list of products
            adapter.submitList(products)

            // Show the "No Products Found" message when there are none
            binding.noProductsFound.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE

            return
        }

        // Push search request
        val act = requireActivity()
        searchExecutor.submit {

            // The terms have since changed this request - skip
            if (searchTerms != terms) return@submit

            Log.d(TAG, "Performing search: $terms")
            val products = ProductManager.products

            // Filter products by name, brand, etc.
            val results = ArrayList<ProductMetadata>(products.size)
            for (product in products) {
                if (product.name.contains(terms, true) ||
                    product.brand.contains(terms, true) ||
                    product.category.getLocalizedName(act).contains(terms, true))
                        results += product
            }

            // Push search results to the list
            act.runOnUiThread {
                // Only update results if the search terms haven't changed since
                if (searchTerms == terms) adapter.submitList(results)
            }
        }
    }

    /**
     * Open the product details dialog
     * @param product Product to view
     */
    private fun openProductDialog(product: ProductMetadata) {
        // Dismiss existing product dialog if one is already opened
        productDialog?.dismiss()

        // Open product dialog
        val dialog = ProductDialog(this, product).show()
        productDialog = dialog
        dialog.setOnDismissListener {
            // Clear the active product when the dialog is dismissed
            productDialog = null
        }
    }

    /**
     * Prompt the user to add a product to the list
     */
    private fun promptAddProduct() {
        TileButtonDialog.Builder(requireContext())
            .setTitle(R.string.add_product)
            .addButton(R.drawable.ic_add, R.string.new_product)
            .addButton(R.drawable.ic_barcode, R.string.scan_barcode)
            .setOnClickListener { _, which ->
                when (which) {
                    0 -> openProductDialog(ProductMetadata(name = "New Product"))
                    1 -> navTo(R.id.action_productsFragment_to_barcodeFragment)
                }
            }
            .show()
    }

    /**
     * Prompt the user to remove the selected products
     */
    private fun promptRemoval() {
        val ctx = requireContext()
        val toRemove = adapter.selected

        // Nothing to remove
        if (toRemove.isEmpty()) {
            Snackbar.make(requireView(),
                R.string.select_product_to_remove,
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        // Show dialog confirmation
        AlertDialog.Builder(ctx)
            .setTitle(R.string.confirm_removal)
            .setMessage(ctx.getQuantityString(R.plurals.confirm_product_removal,
                toRemove.size, toRemove.size))
            .setPositiveButton(R.string.yes) { _, _ ->
                ProductManager.removeProducts(toRemove)
                popMode()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Toggle the search bar
     * @param enabled True to show the search bar
     */
    private fun toggleSearch(enabled: Boolean) {
        binding.searchBar.let {
            it.root.visibility = if (enabled) View.VISIBLE else View.GONE
            if (enabled) {
                it.searchBar.requestFocus()
                it.searchBar.toggleSoftKeyboard()
            } else {
                it.searchTerms.setText("")
                it.searchBar.toggleSoftKeyboard()
                it.searchBar.clearFocus()
            }
        }
        searchEnabled = enabled
    }

    /**
     * Toggle the search bar
     */
    private fun toggleSearch() = toggleSearch(!searchEnabled)

    /**
     * View model used for saving and restoring state
     */
    class ProductsViewModel : ViewModel() {

        // The product dialog view state
        var productDialog: ProductDialog.ViewState? = null

        // Search terms
        var searchTerms: String? = null

        // Delete mode
        var deleteProducts: List<ProductMetadata>? = null
    }

    companion object {
        private const val TAG = "ProductsFragment"
    }
}