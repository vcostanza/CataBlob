package software.blob.catablob.ui

import android.view.View
import androidx.databinding.BindingAdapter

/**
 * Defines custom "app:visible" attribute for views
 * @param view View to set visibility on
 * @param visible True if visible, false if gone
 */
@BindingAdapter("visible")
fun bindVisible(view: View, visible: Boolean) {
    view.visibility = if (visible) View.VISIBLE else View.GONE
}
