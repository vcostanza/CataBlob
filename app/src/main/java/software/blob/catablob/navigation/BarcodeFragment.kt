package software.blob.catablob.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import software.blob.catablob.R
import software.blob.catablob.barcode.BarcodeReader
import software.blob.catablob.databinding.FragmentBarcodeBinding

const val BARCODE_REQUEST_KEY = "barcode_scan"

/**
 * Camera preview for capturing a product barcode
 */
class BarcodeFragment : BaseFragment(R.string.scan_barcode) {

    private lateinit var viewBinding: FragmentBarcodeBinding
    private lateinit var barcodeReader: BarcodeReader

    private var selector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)

        viewBinding = FragmentBarcodeBinding.inflate(inflater)

        // Create the barcode reader
        barcodeReader = BarcodeReader()
        barcodeReader.subscribe { res ->
            Log.d(TAG, "Barcode format = ${res.barcodeFormat} | Text = ${res.text}")

            // Send the result back to the product manager fragment
            val result = Bundle()
            result.putString("barcode", res.text)
            result.putSerializable("format", res.barcodeFormat)
            setFragmentResult(BARCODE_REQUEST_KEY, result)

            // Stop the barcode reader
            barcodeReader.dispose()

            // Back out of this fragment
            requireActivity().runOnUiThread {
                findNavController().popBackStack()
            }
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        viewBinding.switchCamera.setOnClickListener {
            selector = if (selector == CameraSelector.DEFAULT_FRONT_CAMERA)
                CameraSelector.DEFAULT_BACK_CAMERA
            else
                CameraSelector.DEFAULT_FRONT_CAMERA
            startCamera()
        }

        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        barcodeReader.dispose()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val windowManager = requireActivity().windowManager

            // Preview
            val preview = Preview.Builder()
                .setTargetRotation(windowManager.defaultDisplay.rotation)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            viewBinding.viewFinder.scaleType = PreviewView.ScaleType.FIT_CENTER

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, selector, preview, barcodeReader.useCase)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "BarcodeFragment"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }.toTypedArray()
    }
}