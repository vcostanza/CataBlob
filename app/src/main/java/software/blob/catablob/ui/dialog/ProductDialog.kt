package software.blob.catablob.ui.dialog

import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import software.blob.catablob.R
import software.blob.catablob.database.ProductManager
import software.blob.catablob.databinding.DialogProductBinding
import software.blob.catablob.model.product.ProductMetadata
import software.blob.catablob.navigation.BaseFragment
import software.blob.catablob.ui.ZoomImageView
import software.blob.catablob.ui.image.ImageUriReader
import software.blob.catablob.ui.image.ThumbnailGenerator
import software.blob.catablob.util.*
import java.io.File

/**
 * A dialog for viewing [ProductMetadata]
 * @param fragment Fragment that is showing this dialog
 * @param product The product to view/edit
 */
class ProductDialog(
    private val fragment: BaseFragment,
    product: ProductMetadata)
    : DialogInterface {

    private val context = fragment.requireContext()

    // Create a copy of the product that will be persisted when the user confirms the changes
    private val product = product.copy()

    // Data binding
    private val binding = DialogProductBinding.inflate(LayoutInflater.from(context))

    // Used to generate the product image thumbnail
    private var thumbGen: ThumbnailGenerator? = null

    // The dialog instance
    private var dialog: AlertDialog? = null

    // Called when the dialog is dismissed
    private var onDismiss: DialogInterface.OnDismissListener? = null

    // Track whether the product image is being viewed in full-screen
    private var showingFullscreenImage = false

    /**
     * Show the dialog
     * @return The dialog itself
     */
    fun show(): ProductDialog {

        // Dialog is already showing
        if (dialog?.isShowing == true) return this

        // Setup the text views for product name, brand, etc.
        binding.product = product

        // Initialize the image placeholder (thumbnail loaded asynchronously)
        binding.image.setImageResource(R.drawable.ic_placeholder)

        // Generate and update the image thumbnail
        val thumbSize = context.resources.getDimensionPixelSize(R.dimen.thumb_large)
        val thumbGen = ThumbnailGenerator(context, thumbSize, false)
        thumbGen.subscribe(
            { thumb -> binding.image.setImageBitmap(thumb.bitmap) },
            { e -> Log.e(TAG, "Failed to generate thumbnail", e) }
        )
        thumbGen.request(product.imageURI)
        this.thumbGen = thumbGen

        // Prompt the user to change the product image when they tap on it
        binding.image.tag = product.imageURI
        binding.image.setOnClickListener {
            // If there's no image set then prompt the user to add one
            // Otherwise bring up the fullscreen image viewer
            val uri = binding.image.tag as String
            if (uri.isEmpty())
                promptSetProductImage()
            else
                showFullscreenImage(uri)
        }

        // Show the dialog
        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                // Update the product in the database
                ProductManager.addProduct(product)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()

        // Dispose the thumbnail generator subscription when we finish
        dialog.setOnDismissListener {
            thumbGen.shutdown()
            this.thumbGen = null
            onDismiss?.onDismiss(it)
        }

        this.dialog = dialog

        return this
    }

    /**
     * Set the event listener for when the dialog is dismissed
     * @param onDismiss Dismiss event callback
     */
    fun setOnDismissListener(onDismiss: DialogInterface.OnDismissListener) {
        this.onDismiss = onDismiss
    }

    /**
     * Cancel the dialog
     */
    override fun cancel() {
        dialog?.cancel()
    }

    /**
     * Dismiss the dialog
     */
    override fun dismiss() {
        dialog?.dismiss()
    }

    /**
     * Show a product image in fullscreen
     * @param uri Image uri
     */
    private fun showFullscreenImage(uri: String) {

        // Can't show an empty URI
        if (uri.isEmpty()) return

        // Setup zoomable image view
        val imageView = ZoomImageView(context)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Create and show the dialog
        val dialog = AlertDialog.Builder(context)
            .setView(imageView)
            .setPositiveButton(R.string.edit) { _, _ -> promptSetProductImage() }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .show()

        // Make the dialog semi-fullscreen
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)

        // Used for reading images from a URL
        val reader = ImageUriReader(context)
        reader.subscribe(
            { image -> imageView.setImageBitmap(image.bitmap) },
            { e -> Log.e(TAG, "Failed to read image: $uri", e) }
        )

        // Shutdown the reader when the dialog is closed
        dialog.setOnDismissListener {
            showingFullscreenImage = false
            reader.shutdown()
        }
        showingFullscreenImage = true

        // Load the image
        reader.request(uri)
    }

    /**
     * Prompt the user to set a new product image using either the camera or file browsing
     */
    private fun promptSetProductImage() {
        TileButtonDialog.Builder(context)
            .setTitle(R.string.set_product_image)
            .addButton(R.drawable.ic_gallery, R.string.find_image)
            .addButton(R.drawable.ic_camera, R.string.take_photo)
            .setOnClickListener { _, which ->
                when (which) {
                    0 -> fragment.launchGallery { uri ->
                        if (uri != null)
                            context.decodeBitmap(uri)?.let { updateProductImage(it) }
                    }
                    1 -> fragment.launchCamera{ bmp ->
                        if (bmp != null) updateProductImage(bmp)
                    }
                }
            }
            .show()
    }

    /**
     * Update the product image with the given bitmap
     * @param bmp Bitmap to use
     */
    private fun updateProductImage(bmp: Bitmap) {
        // Make sure we're in a valid state - only to be called when the dialog is showing
        val dir = context.getOrCreateDirectory(Environment.DIRECTORY_PICTURES)

        // Save the image to the temp folder
        val file = File(dir, getDateTimeFileName("jpg"))
        bmp.compressToFile(file)

        // Update product image display
        thumbGen?.request(file)
        binding.image.tag = file.absolutePath
    }

    // The current view state
    var viewState get() = ViewState(this)
        set(value) {
            // Restore the image dialog
            if (value.fsImage) showFullscreenImage(product.imageURI)
        }

    /**
     * Used to save the state of the dialog between configuration changes
     * @param dialog The product dialog
     */
    class ViewState(dialog: ProductDialog) {

        // The product being edited
        val product = dialog.product

        // Track when the product image is being viewed
        val fsImage = dialog.showingFullscreenImage
    }

    companion object {
        private const val TAG = "ProductDialog"
    }
}