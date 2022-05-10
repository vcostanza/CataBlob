package software.blob.catablob.navigation

import android.os.Bundle
import android.view.*
import software.blob.catablob.R
import software.blob.catablob.databinding.FragmentOptionsBinding

class OptionsFragment : BaseFragment() {

    private lateinit var viewBinding: FragmentOptionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        viewBinding = FragmentOptionsBinding.inflate(inflater)
        viewBinding.products.setOnClickListener {
            navTo(R.id.action_optionsFragment_to_productsFragment)
        }

        return viewBinding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.collections_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_collection ->
                true
            else -> super.onOptionsItemSelected(item)
        }
    }
}