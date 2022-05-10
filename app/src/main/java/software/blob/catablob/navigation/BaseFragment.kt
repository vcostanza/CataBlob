package software.blob.catablob.navigation

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import software.blob.catablob.R
import java.util.*

/**
 * Base fragment class used in this app
 * @param titleId Fragment title resource ID (app name by default)
 * @param menuId Menu resource ID (null to ignore)
 */
open class BaseFragment(
    @StringRes titleId: Int = R.string.app_name,
    @MenuRes menuId: Int? = null)
    : Fragment() {

    private val defaultMode = Mode(titleId, menuId)

    private var title: String? = null
    private var menu: Menu? = null

    // Display mode
    private val modeStack = Stack<Mode>()
    protected var currentMode: Mode = defaultMode
        private set(value) {
            if (field != value) {
                field = value
                onSetMode(value)
            }
        }

    // Back button behavior override
    private val backButtonHandler = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            popMode()
        }
    }

    // Used for taking photos
    private var cameraCallback: ActivityResultCallback<Bitmap?>? = null
    private val cameraPrompt = registerForActivityResult(TakePicturePreview()) { bmp: Bitmap? ->
        cameraCallback?.onActivityResult(bmp)
        cameraCallback = null
    }

    // Used for importing images
    private var contentCallback: ActivityResultCallback<Uri?>? = null
    private val contentPrompt = registerForActivityResult(GetContent()) { uri: Uri? ->
        contentCallback?.onActivityResult(uri)
        contentCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Override back button behavior to handle display mode changes
        activity?.onBackPressedDispatcher?.addCallback(this, backButtonHandler)
    }

    /**
     * Set the default title when the view state is restored
     */
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Restore default title where the string takes precedence over the default ID
        setDefaultTitle()
    }

    /**
     * Set the default menu
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        currentMode.menuId?.let {
            this.menu = menu
            inflater.inflate(it, menu)
        }
    }

    /**
     * Set the title of the fragment shown in the action bar
     * @param title Title string
     * @param persist True if this title should be persisted between mode changes
     */
    protected fun setTitle(title: String, persist: Boolean = true) {
        if (persist) this.title = title
        activity?.title = title
    }

    /**
     * Set the title of the fragment shown in the action bar
     * @param titleId Title string resource ID
     * @param persist True if this title should be persisted between mode changes
     */
    protected fun setTitle(@StringRes titleId: Int, persist: Boolean = true) {
        context?.getString(titleId)?.let { setTitle(it, persist) }
    }

    /**
     * Restore the default title for this fragment
     */
    private fun setDefaultTitle() {
        val title = this.title
        if (title != null)
            setTitle(title, false)
        else
            setTitle(defaultMode.titleId, false)
    }

    /**
     * Push a new display mode onto the stack
     * @param mode Mode to push
     */
    protected fun pushMode(mode: Mode) {
        modeStack.push(currentMode)
        currentMode = mode
        backButtonHandler.isEnabled = modeStack.isNotEmpty()
    }

    /**
     * Pop to the previous display mode in the stack
     * @return True if the pop succeeded
     */
    protected fun popMode(): Boolean {
        if (modeStack.isNotEmpty()) {
            currentMode = modeStack.pop()
            backButtonHandler.isEnabled = modeStack.isNotEmpty()
            return true
        }
        return false
    }

    /**
     * Called when the display mode has been changed
     */
    protected open fun onSetMode(mode: Mode) {

        // Display the title of this mode
        setTitle(mode.titleId, false)

        // Update menu
        activity?.invalidateOptionsMenu()
    }

    /**
     * Navigate to another fragment using the specified action ID
     * @param actionId Navigation action resource ID
     */
    protected fun navTo(@IdRes actionId: Int) = findNavController().navigate(actionId)

    /**
     * Launch the camera app to take a photo
     * @param callback Callback to invoke when finished
     */
    fun launchCamera(callback: ActivityResultCallback<Bitmap?>) {
        cameraCallback = callback
        cameraPrompt.launch(null)
    }

    /**
     * Launch the gallery app to select an image
     * @param callback Callback to invoke when finished
     */
    fun launchGallery(callback: ActivityResultCallback<Uri?>) {
        contentCallback = callback
        contentPrompt.launch("image/*")
    }

    /**
     * Fragment display mode
     * @param titleId The title of this mode (string resource ID)
     * @param menuId Menu to display in this mode (menu resource ID)
     */
    class Mode(@StringRes val titleId: Int, @MenuRes val menuId: Int? = null)
}