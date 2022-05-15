package software.blob.catablob.net

import android.util.Log
import com.google.zxing.BarcodeFormat
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import software.blob.catablob.data.Product
import software.blob.catablob.model.product.ProductCategory
import software.blob.catablob.model.product.ProductCode
import software.blob.catablob.model.product.ProductNotFoundException
import software.blob.catablob.util.readJsonObject

/**
 * A [HttpClient] for handling REST responses from upcitemdb.com
 */
open class UpcItemDbClient : HttpClient<Product>() {

    /**
     * Perform a lookup on a given UPC code
     * @param code UPC code
     */
    fun request(code: ProductCode) {
        val req = Request.Builder()
            .url("https://api.upcitemdb.com/prod/trial/lookup?upc=${code.code}")
            .tag(ProductCode::class.java, code)
            .build()
        request(req)
    }

    /**
     * Handle JSON response
     * @param res UPC response containing JSON metadata
     * @return Product metadata or null if failed
     */
    override fun onResponse(res: Response): Product? {
        try {
            res.body?.byteStream().use { stream ->
                if (stream == null) return null
                val json = readJsonObject(stream)
                val code = res.request.tag(ProductCode::class.java)
                return parseJsonResponse(json, code)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get UPC metadata for ${res.request.url}", e)
            onNext(PRODUCT_NOT_FOUND)
        }
        return null
    }

    /**
     * Parse product JSON metadata from the REST service
     * @param json JSON metadata object
     * @param code Input product code (null to read from JSON)
     * @return Product metadata
     */
    fun parseJsonResponse(json: JSONObject, code: ProductCode? = null): Product {
        val items = json.getJSONArray("items")

        // Product not found
        if (items.length() != 1)
            throw ProductNotFoundException("Could not find product from code: $code")

        val item = items.getJSONObject(0)
        val imageLinks = item.getJSONArray("images")
        val imageUri = if (imageLinks.length() > 0) imageLinks.getString(0) else ""
        val codeText = code?.code ?: item.getString("upc")
        val codeType = code?.format ?: BarcodeFormat.UPC_A
        val category = parseCategory(item.getString("category"))

        return Product(
            name = item.getString("title"),
            notes = item.getString("description"),
            brand = item.getString("brand"),
            category = category,
            imageUri = imageUri,
            code = codeText,
            codeType = codeType)
    }

    /**
     * Parse the category string returned from the server
     *
     * Example input: Food, Beverages & Tobacco > Food Items > Snack Foods > Snack Cakes
     * We only care about the first word in this category ("Food" in this case),
     * which would be parsed as [ProductCategory.FOOD]
     *
     * @param category Category string
     * @return Category in [ProductCategory] form
     */
    private fun parseCategory(category: String): ProductCategory {

        // If there's no category specified then default to other
        if (category.isEmpty()) return ProductCategory.OTHER

        // Read the top-level keyword
        val spaceIdx = category.indexOf(" ")
        var keyword = if (spaceIdx != -1) category.substring(0, spaceIdx) else category

        // Remove trailing commas and periods
        keyword = keyword.trimEnd(',', '.')

        return try {
            // Attempt to convert the keyword directly into a product category
            ProductCategory.valueOf(keyword.uppercase())
        } catch (ignored: Exception) {
            // Fallback cases
            when (keyword) {
                "Sporting" -> ProductCategory.SPORT
                "Software" -> ProductCategory.MEDIA
                "Hardware" -> ProductCategory.TOOLS
                "Apparel" -> ProductCategory.CLOTHING
                "Animals" -> ProductCategory.PET
                else -> ProductCategory.OTHER
            }
        }
    }

    companion object {
        private const val TAG = "UpcItemDbClient"
    }
}

/**
 * Constant used to identify if a product is not found/valid
 */
val PRODUCT_NOT_FOUND = Product(uid = "", name = "No Product")