package org.who.ddccverifier.services

import android.annotation.SuppressLint
import android.util.Base64
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions




/**
 * Turns camera preview frames into QR Codes when found.
 */
class QRFinder(private val onQRCodeFound: (Set<String>)->Unit = { /* nop */ } ): ImageAnalysis.Analyzer {

    private var onlyTheFirstFrame: Boolean = false
    private val scanningOptions = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()

    override fun analyze(imageProxy: ImageProxy) {
        scanBarcode(imageProxy)
    }
 
    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private fun scanBarcode(imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
            scanBarcodes(inputImage, {
                imageProxy.close()
                finishOnFirstFrame(it)
            }, {
                imageProxy.close()
            })
        }
    }

    fun scanBarcodes(
        inputImage: InputImage,
        onQRsFound: (Set<String>) -> Unit = { /* nop */ },
        onQRsNotFound: () -> Unit = { /* nop */ }) {

        BarcodeScanning.getClient(scanningOptions).process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    onQRsFound(barToStr(barcodes))
                } else {
                    onQRsNotFound()
                }
            }
            .addOnCanceledListener(onQRsNotFound)
            .addOnFailureListener {
                it.printStackTrace()
                onQRsNotFound()
            }
    }

    fun scanBarcodesSync(inputImage: InputImage): Set<String> {
        val scanner = BarcodeScanning.getClient(scanningOptions)
        val task = scanner.process(inputImage)
        Tasks.await(task)
        task.exception?.printStackTrace()
        if (task.result.isNotEmpty()) {
            return barToStr(task.result)
        }
        return emptySet()
    }

    /**
     * DIVOC QR codes are binary.
     */
    private fun bytesToStr(array: ByteArray?): String {
        if (array == null) return ""
        return "B64:" + Base64.encodeToString(array, Base64.NO_WRAP)
    }

    private fun barToStr(barcodes: List<Barcode>): Set<String> {
        return barcodes.groupBy { it.rawValue ?: bytesToStr(it.rawBytes) }.keys
    }
 
    private fun finishOnFirstFrame(barcodes: Set<String>) {
        synchronized (this) {
            if (onlyTheFirstFrame) return
            onlyTheFirstFrame = true
        }

        onQRCodeFound(barcodes)
    }
}