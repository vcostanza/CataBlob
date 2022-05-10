package software.blob.catablob.ui.dialog

import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import software.blob.catablob.R
import software.blob.catablob.util.isPortrait
import software.blob.catablob.util.isTablet

/**
 * A dialog that displays a grid of buttons with icons and labels
 * @param dialog The base dialog
 */
class TileButtonDialog private constructor(
    private val dialog: AlertDialog,
    private val adapter: TileButtonAdapter)
    : DialogInterface {

    init {
        adapter.dialog = dialog
    }

    /**
     * Show the dialog
     */
    fun show() = dialog.show()

    override fun cancel() = dialog.cancel()

    override fun dismiss() = dialog.dismiss()

    /**
     * Set the default button click listener for this dialog
     * Note: The [TileButton] click listener takes priority over this listener
     * @param onClick Click listener
     */
    fun setOnClickListener(onClick: DialogInterface.OnClickListener) {
        adapter.onClick = onClick
    }

    /**
     * Set the callback that's invoked when the dialog is dismissed
     * @param onDismiss Dismiss listener
     */
    fun setOnDismissListener(onDismiss: DialogInterface.OnDismissListener) {
        dialog.setOnDismissListener(onDismiss)
    }

    /**
     * Set the callback that's invoked when the dialog is canceled
     * @param onCancel Cancel listener
     */
    fun setOnCancelListener(onCancel: DialogInterface.OnCancelListener) {
        dialog.setOnCancelListener(onCancel)
    }

    /**
     * Helper class used for creating [TileButtonDialog] instances
     * @param context Application context
     */
    class Builder(private val context: Context) {

        private var title: String? = ""
        private val buttons = ArrayList<TileButton>()
        private var onClick: DialogInterface.OnClickListener? = null

        /**
         * Set the title displayed at the top of the dialog
         * @param titleId Title string resource ID
         * @return The dialog builder
         */
        fun setTitle(@StringRes titleId: Int) = setTitle(context.getString(titleId))

        /**
         * Set the title displayed at the top of the dialog
         * @param title Title string
         * @return The dialog builder
         */
        fun setTitle(title: String): Builder {
            this.title = title
            return this
        }

        /**
         * Add a button to this dialog
         * @param iconId Icon drawable resource ID
         * @param labelId Label string resource ID
         * @return The dialog builder
         */
        fun addButton(@DrawableRes iconId: Int, @StringRes labelId: Int): Builder {

            // Lookup icon and make sure it's valid
            val icon = AppCompatResources.getDrawable(context, iconId)
            if (icon == null) {
                Log.e(TAG, "Failed to use invalid drawable for tile button", Throwable())
                return this
            }

            // Add the button
            return addButton(icon, context.getString(labelId))
        }

        /**
         * Add a button to this dialog
         * @param icon Icon drawable
         * @param label Label string
         * @return The dialog builder
         */
        fun addButton(icon: Drawable, label: String) = addButton(TileButton(icon, label))

        /**
         * Add a button to this dialog
         * @param button The button to add
         * @return The dialog builder
         */
        fun addButton(button: TileButton): Builder {
            buttons += button
            return this
        }

        /**
         * Add a button to this dialog
         * @param button The button to add
         */
        operator fun plusAssign(button: TileButton) {
            addButton(button)
        }

        /**
         * Set the default button click listener for this dialog
         * Note: The [TileButton] click listener takes priority over this listener
         * @param onClick Click listener
         * @return The dialog builder
         */
        fun setOnClickListener(onClick: DialogInterface.OnClickListener): Builder {
            this.onClick = onClick
            return this
        }

        /**
         * Build the [TileButtonDialog] using the set parameters
         * @return Tile button dialog
         */
        fun create(): TileButtonDialog {

            // First determine the max number of buttons we can fit per row
            val maxColumns = when {
                context.isTablet -> 4
                context.isPortrait -> 2
                else -> 3
            }

            // Create view to hold the buttons
            val v = RecyclerView(context)
            val adapter = TileButtonAdapter(buttons, onClick)
            v.layoutManager = GridLayoutManager(context, maxColumns.coerceAtMost(buttons.size))
            v.adapter = adapter

            // Setup the dialog using the builder parameters
            val b = AlertDialog.Builder(context)
            if (title != null) b.setTitle(title)
            b.setView(v)
            b.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            return TileButtonDialog(b.create(), adapter)
        }

        /**
         * Build and show the [TileButtonDialog] using the set parameters
         * @return Tile button dialog
         */
        fun show(): TileButtonDialog {
            val dialog = create()
            dialog.show()
            return dialog
        }
    }

    companion object {
        private const val TAG = "TileButtonDialog"
    }
}

/**
 * The view model for a button icon and label
 * @param icon Icon drawable
 * @param label Label string
 * @param onClick Listener fired when this button is tapped (optional)
 */
class TileButton(
    val icon: Drawable,
    val label: String,
    val onClick: ((TileButton) -> Unit)? = null) {

    override fun toString() = label
}

/**
 * Adapts [TileButton] to [TileButtonViewHolder]
 * @param buttons List of buttons to display
 * @param onClick Listener for when a button is tapped
 */
class TileButtonAdapter(
    buttons: List<TileButton>,
    var onClick: DialogInterface.OnClickListener? = null)
    : ListAdapter<TileButton, TileButtonViewHolder>(TileButtonDiffCallback) {

    var dialog: AlertDialog? = null

    init {
        submitList(buttons)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileButtonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tile_button, parent, false)
        return TileButtonViewHolder(view, this)
    }

    override fun onBindViewHolder(holder: TileButtonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

/**
 * The view holder for [TileButton]
 */
class TileButtonViewHolder(itemView: View, private val adapter: TileButtonAdapter)
    : RecyclerView.ViewHolder(itemView) {

    private val button: View = itemView.findViewById(R.id.button)
    private val icon: ImageView = itemView.findViewById(R.id.icon)
    private val label: TextView = itemView.findViewById(R.id.label)
    private var model: TileButton? = null

    init {
        button.setOnClickListener { model?.let {
            // Check if the button has its own click listener
            // and if not default to the adapter's click listener
            if (it.onClick != null)
                it.onClick.invoke(it)
            else
                adapter.onClick?.onClick(adapter.dialog, adapterPosition)
            adapter.dialog?.dismiss()
        }}
    }

    /**
     * Bind tile button icon and label to the view
     * @param button Button to bind
     */
    fun bind(button: TileButton) {
        model = button
        label.text = button.label
        icon.setImageDrawable(button.icon)
        adapterPosition
    }

    override fun toString() = model?.toString() ?: "<empty>"
}

object TileButtonDiffCallback : DiffUtil.ItemCallback<TileButton>() {
    override fun areItemsTheSame(old: TileButton, new: TileButton): Boolean {
        return old == new
    }

    override fun areContentsTheSame(old: TileButton, new: TileButton): Boolean {
        return old.label == new.label
    }
}