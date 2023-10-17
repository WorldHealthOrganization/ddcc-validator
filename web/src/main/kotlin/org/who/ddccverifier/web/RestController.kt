package org.who.ddccverifier.web

import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.who.ddccverifier.QRDecoder
import org.who.ddccverifier.trust.CompoundRegistry
import org.who.ddccverifier.trust.TrustRegistryFactory
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.String
import kotlin.apply


@RestController
class RestController {
    companion object {
        var registry = CompoundRegistry(TrustRegistryFactory.getTrustRegistries()).apply {
            init()
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

        val image = if (StringUtils.endsWithIgnoreCase(file.originalFilename, "pdf")) {
            val doc = PDDocument.load(ByteArrayInputStream(file.bytes))
            val pdfRenderer = PDFRenderer(doc)
            pdfRenderer.renderImageWithDPI(0, 300f)
        } else {
            ImageIO.read(ByteArrayInputStream(file.bytes))
        }

        val binaryBitmap = BinaryBitmap(HybridBinarizer(
            BufferedImageLuminanceSource(addBordertoImage(image))
        ))

        val qrContents = try {
            MultiFormatReader().decode(binaryBitmap)
        } catch (e: NotFoundException) {
            return QRDecoder.VerificationResult(QRDecoder.Status.NOT_FOUND, null, null, "", null)
        }

        return this.verify(QRContents(qrContents.text))
    }

    fun addBordertoImage(source: BufferedImage, color: Color = Color.WHITE, borderLeft: Int = 50, borderTop: Int = 50): BufferedImage {
        val borderedImageWidth: Int = source.width + borderLeft * 2
        val borderedImageHeight: Int = source.height + borderTop * 2
        val img = BufferedImage(borderedImageWidth, borderedImageHeight, source.type)
        img.createGraphics()
        val g = img.graphics as Graphics2D
        g.color = color
        g.fillRect(0, 0, borderedImageWidth, borderedImageHeight)
        g.drawImage(
            source,
            borderLeft,
            borderTop,
            source.width + borderLeft,
            source.height + borderTop,
            0,
            0,
            source.width,
            source.height,
            Color.BLACK,
            null
        )
        return img
    }
}