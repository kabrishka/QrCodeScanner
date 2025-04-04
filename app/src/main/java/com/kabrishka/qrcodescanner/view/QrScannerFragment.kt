package com.kabrishka.qrcodescanner.view

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import com.google.android.material.snackbar.Snackbar
import com.kabrishka.qrcodescanner.R
import com.kabrishka.qrcodescanner.TAG
import com.kabrishka.qrcodescanner.databinding.FragmentQrScannerBinding
import com.kabrishka.qrcodescanner.hide
import com.kabrishka.qrcodescanner.model.Error
import com.kabrishka.qrcodescanner.model.Loading
import com.kabrishka.qrcodescanner.model.Success
import com.kabrishka.qrcodescanner.show
import com.kabrishka.qrcodescanner.viewmodel.QrScannerViewModel

class QrScannerFragment : Fragment() {

    private var _binding: FragmentQrScannerBinding? = null
    private val binding: FragmentQrScannerBinding
        get() = _binding ?: throw RuntimeException("FragmentQrScannerBinding == null")

    private val viewModel: QrScannerViewModel by viewModels()

    private val cameraPermission = Manifest.permission.CAMERA
    private val storagePermission = Manifest.permission.READ_EXTERNAL_STORAGE

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) binding.scanner.resume()
        else Log.d(TAG, "Camera permission denied")
    }

    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openGallery()
        else Log.d(TAG, "Gallery permission denied")
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let(viewModel::decodeImageFromUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentQrScannerBinding.inflate(inflater, container, false).also {
        _binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupUi()
    }

    override fun onResume() {
        super.onResume()
        requestCameraPermission.launch(cameraPermission)
    }

    override fun onPause() {
        super.onPause()
        binding.scanner.pauseAndWait()
    }

    override fun onDestroyView() {
        binding.scanner.stopDecoding()
        _binding = null
        super.onDestroyView()
    }

    private fun setupUi() = with(binding) {
        result.setOnClickListener {
            copyToClipboard(result.text)
        }
        scanner.decodeContinuous(viewModel.barcodeCallback)

        btnFlashlight.setOnClickListener { scanner.switchTorch() }
        btnFlipCamera.setOnClickListener { scanner.flip() }
        btnGallery.setOnClickListener(::handleGalleryClick)
    }

    private fun copyToClipboard(text: CharSequence) {
        val clip = ClipData.newPlainText("Copied String", text)
        requireContext().clipboardManager.setPrimaryClip(clip)
    }

    private fun handleGalleryClick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) openGallery()
        else requestStoragePermission.launch(storagePermission)
    }

    private fun observeViewModel() {
        viewModel.decodingResult.observe(viewLifecycleOwner) { decoding ->
            when (decoding) {
                Loading -> showLoadingState()
                is Error -> showErrorState(decoding)
                is Success -> showSuccessState(decoding)
            }
        }
    }

    private fun showLoadingState() = with(binding) {
        result.hide()
        scanner.pauseAndWait()
        showSnackbar(R.string.recognizing)
    }

    private fun showErrorState(error: Error) = with(binding) {
        result.hide()
        scanner.resume()
        showSnackbar(R.string.recognize_failed)
        Log.e(TAG, error.message)
    }

    private fun showSuccessState(success: Success) = with(binding) {
        result.show()
        result.text = success.result
        result.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_copy, 0, 0, 0)
        scanner.resume()
    }


    private fun showSnackbar(@StringRes messageRes: Int) {
        Snackbar.make(binding.scanner, getString(messageRes), Snackbar.LENGTH_SHORT).show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private val Context.clipboardManager get() = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

    companion object {
        fun navigate(controller: NavController) {
            controller.navigate(R.id.action_to_qr_scanner)
        }
    }
}