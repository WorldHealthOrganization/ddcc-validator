package org.who.ddccverifier.web

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.who.ddccverifier.trust.pathcheck.PCFTrustRegistry
import org.who.ddccverifier.QRDecoder
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

@RestController
class RestController {
    companion object {
        var registry = PCFTrustRegistry().apply {
            init(PCFTrustRegistry.PRODUCTION_REGISTRY, PCFTrustRegistry.ACCEPTANCE_REGISTRY)
        }
    }

    data class QRContents(
        val uri: String,
    )

    @PostMapping("/verify")
    fun verify(@RequestBody qr: QRContents): QRDecoder.VerificationResult {
        return QRDecoder(registry).decode(qr.uri)
    }

    @PostMapping("/findAndVerify")
    fun findAndVerify(@RequestParam("file") file: MultipartFile): QRDecoder.VerificationResult {
        if (file.isEmpty()) {
            return QRDecoder.VerificationResult(QRDecoder.Status.NOT_FOUND, null, null, "", null)
        }

        val binaryBitmap = BinaryBitmap(HybridBinarizer(
            BufferedImageLuminanceSource(ImageIO.read(ByteArrayInputStream(file.bytes)))
        ))

        val qrContents = try {
            MultiFormatReader().decode(binaryBitmap)
        } catch (e: NotFoundException) {
            return QRDecoder.VerificationResult(QRDecoder.Status.NOT_FOUND, null, null, "", null)
        }

        return this.verify(QRContents(qrContents.text))
    }
}