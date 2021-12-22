package org.who.ddccverifier.services

import android.annotation.SuppressLint
import android.util.Base64
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Turns camera preview frames into QR Codes when found.
 */
class QRFinder(private val onQRCodeFound: (Set<String>)->Unit): ImageAnalysis.Analyzer {

    private var onlyTheFirstFrame: Boolean = false

    override fun analyze(imageProxy: ImageProxy) {
        scanBarcode(imageProxy)
    }
 
    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private fun scanBarcode(imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()
            scanner.process(inputImage)
                .addOnCompleteListener {
                    imageProxy.close()
                    if (it.isSuccessful) {
                        readBarcodeData(it.result as List<Barcode>)
                    } else {
                        it.exception?.printStackTrace()
                    }
                }
        }
    }

    /**
     * DIVOC QR codes are binary.
     */
    private fun bytesToStr(array: ByteArray?): String {
        if (array == null) return ""
        return "B64:" + Base64.encodeToString(array, Base64.DEFAULT)
    }
 
    private fun readBarcodeData(barcodes: List<Barcode>) {
        if (barcodes.isEmpty()) return
        synchronized (this) {
            if (onlyTheFirstFrame) return
            onlyTheFirstFrame = true
        }

        val qrStrings = barcodes.groupBy { it.rawValue ?: bytesToStr(it.rawBytes) }.keys

        onQRCodeFound(qrStrings)
    }
}