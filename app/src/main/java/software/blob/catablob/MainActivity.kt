package software.blob.catablob

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import software.blob.catablob.database.CatalogDatabase
import software.blob.catablob.database.ProductManager
import software.blob.catablob.databinding.ActivityMainBinding

/**
 * The main UI activity
 */
class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Initialize main database
        val database = CatalogDatabase(this)

        // Initialize product manager
        ProductManager.init(database)
    }
}