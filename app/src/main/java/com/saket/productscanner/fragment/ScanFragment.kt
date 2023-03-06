package com.saket.productscanner.fragment

import android.Manifest
import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.budiyev.android.codescanner.*
import com.saket.productscanner.ProductApplication
import com.saket.productscanner.R
import com.saket.productscanner.databinding.FragmentScanBinding
import com.saket.productscanner.models.Product
import com.saket.productscanner.utils.Constants.TAG
import com.saket.productscanner.utils.Utility
import com.saket.productscanner.viewmodel.ScanViewModel
import com.saket.productscanner.viewmodel.ScanViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding
    private var codeScanner: CodeScanner? = null
    lateinit var scanViewModel: ScanViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentScanBinding.inflate(inflater, container, false)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Utility.instance?.writeLogSpp(TAG, "granted")
            } else {
                Utility.instance?.writeLogSpp(TAG, "not granted")
            }
        }

        permissionLauncher.launch(Manifest.permission.CAMERA)


        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repository = (requireActivity().application as ProductApplication).productRepository
        scanViewModel = ViewModelProvider(
            requireActivity(),
            ScanViewModelFactory(repository)
        ).get(ScanViewModel::class.java)

        codeScanner = binding?.scannerView?.let { CodeScanner(requireActivity(), it) }!!
        codeScanner?.camera = CodeScanner.CAMERA_BACK
        codeScanner?.formats = CodeScanner.ALL_FORMATS
        codeScanner?.scanMode = ScanMode.SINGLE
        codeScanner?.autoFocusMode = AutoFocusMode.SAFE
        codeScanner?.isAutoFocusEnabled = true
        codeScanner?.isFlashEnabled = false

        startScanning()


        binding?.close?.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                findNavController().popBackStack()
            }
        }
    }

    private fun startScanning() {

        codeScanner?.decodeCallback = DecodeCallback {
            CoroutineScope(Dispatchers.Default).launch {
                var result: List<String> = it.text.split("%").map { it.trim() }
                // Get the Vibrator system service
                val vibrator =
                    requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                // Vibrate for 500 milliseconds
                vibrator.vibrate(100)

                // Or, you can specify a pattern for the vibration
                val pattern = longArrayOf(0, 200, 200)
                vibrator.vibrate(pattern, -1)

                Utility.instance?.writeLogSpp(TAG, result.toString())
                if (result.size == 4) {
                    try {
                        val productId = result[0].split(":").map { it.trim() }[1]
                        val productName = result[1].split(":").map { it.trim() }[1]
                        val productDesc = result[2].split(":").map { it.trim() }[1]
                        val productCost = result[3].split(":").map { it.trim() }[1].toDouble()
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        val current = LocalDateTime.now().format(formatter)



                        scanViewModel.saveProduct(
                            Product(
                                productId,
                                productName,
                                current,
                                current,
                                productDesc,
                                quantity = 1,
                                productCost
                            )
                        )
                        codeScanner?.stopPreview()



                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                requireActivity(),
                                "Product Added in cart",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        delay(1500)
                        codeScanner?.startPreview()

                    } catch (e: Exception) {
                        Utility.instance?.writeLogError(TAG, e.localizedMessage)
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(requireActivity(), "Error", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }
        codeScanner?.errorCallback = ErrorCallback {
            Utility.instance?.writeLogError(TAG, it.message.toString())
        }
        codeScanner?.startPreview()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        codeScanner?.releaseResources()
        if (codeScanner != null) {
            codeScanner = null
        }
        _binding = null
        viewModelStore.clear()
    }
}