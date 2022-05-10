package software.blob.catablob.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import software.blob.catablob.BuildConfig
import software.blob.catablob.R
import software.blob.catablob.databinding.FragmentSplashBinding

/**
 * The app's splash screen
 */
class SplashFragment : BaseFragment() {

    private lateinit var binding: FragmentSplashBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentSplashBinding.inflate(inflater)

        if (BuildConfig.DEBUG) {
            // Immediately jump to products list in debug mode
            navTo(R.id.action_navHostFragment_to_productsFragment)
        } else {
            // Temporary splash screen
            binding.root.postDelayed({
                try {
                    navTo(R.id.action_navHostFragment_to_optionsFragment)
                } catch (ignore: Exception) {
                    // App has been closed
                }
            }, 2000)
        }
        return binding.root
    }
}