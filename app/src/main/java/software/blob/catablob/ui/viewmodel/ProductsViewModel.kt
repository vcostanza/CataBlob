package software.blob.catablob.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import software.blob.catablob.data.AppDatabase
import software.blob.catablob.data.Product
import software.blob.catablob.navigation.ProductsFragment
import software.blob.catablob.ui.dialog.ProductDialog

/**
 * View model used for product-related fragments such as [ProductsFragment]
 * @param database Application database instance
 */
class ProductsViewModel(database: AppDatabase) : ViewModel() {

    private val productDao = database.productDao()
    private val itemDao = database.itemDao()

    // Observable products list
    val products = productDao.getProducts().asLiveData()

    // The product dialog view state
    var productDialog: ProductDialog.ViewState? = null

    // Search terms
    var searchTerms: String? = null

    // List of products currently selected in delete mode
    var deleteProducts: List<Product>? = null

    /**
     * Add a product to the database
     * @param product Product to add
     */
    fun addProduct(product: Product) {
        viewModelScope.launch { productDao.insertProduct(product) }
    }

    /**
     * Remove a product from the database
     * @param product Product to remove
     */
    fun removeProduct(product: Product) {
        viewModelScope.launch { productDao.deleteProduct(product) }
    }

    /**
     * Remove a list of products from the database
     * @param products Products to remove
     */
    fun removeProducts(products: List<Product>) {
        viewModelScope.launch { productDao.deleteProducts(products) }
    }

    /**
     * Search the list of products using a set of terms
     * @param terms Search terms (case insensitive)
     * @param callback Result callback containing search terms and list of results
     */
    fun searchProducts(terms: String, callback: (String, List<Product>) -> Unit) {
        viewModelScope.launch {
            productDao.searchProducts(terms).collect { products -> callback(terms, products) }
        }
    }
}