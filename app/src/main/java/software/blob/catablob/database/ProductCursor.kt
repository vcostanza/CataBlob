package software.blob.catablob.database

import android.database.Cursor
import software.blob.catablob.model.product.ProductMetadata
import java.io.Closeable

/**
 * A database cursor for traversing product metadata queries via [ProductMetadata]
 */
class ProductCursor(private val cursor: Cursor) : Closeable {

    override fun close() {
        TODO("Not yet implemented")
    }
}