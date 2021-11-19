package org.who.ddccverifier

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class QRCodeAnalyzer(private val onQRCodeFound: (Set<String>)->Unit): ImageAnalysis.Analyzer {

    var onlyTheFirstQR: Boolean = false;

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
 
    private fun readBarcodeData(barcodes: List<Barcode>) {
        if (barcodes.isEmpty()) return;
        synchronized (this) {
            if (onlyTheFirstQR) return;
            onlyTheFirstQR = true;
        }

        val qrStrings = barcodes.groupBy { it.rawValue }.keys
        onQRCodeFound(qrStrings)
    }
}