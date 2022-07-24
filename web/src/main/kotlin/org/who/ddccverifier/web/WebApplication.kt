package org.who.ddccverifier.web

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.web.servlet.view.RedirectView
import org.who.ddccverifier.trust.TrustRegistry
import org.who.ddccverifier.trust.pathcheck.KeyUtils
import org.who.ddccverifier.trust.pathcheck.PCFTrustRegistry
import org.who.ddccverifier.verify.QRDecoder
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO


@SpringBootApplication
open class WebApplication

fun main(args: Array<String>) {
    runApplication<WebApplication>(*args)
}

@Controller
class WebController {
    @RequestMapping("/index")
    fun index() = "index"

    @GetMapping("/showCredential")
    fun showCredential() = "showCredential"
}

@RestController
class QRProcessor {
    companion object {
        var registry = PCFTrustRegistry().apply {
            init(PCFTrustRegistry.PRODUCTION_REGISTRY, PCFTrustRegistry.ACCEPTANCE_REGISTRY)
        }
        var fhirJsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
        var jsonMapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    }

    data class QRContents(
        val uri: String,
    )

    data class VerifyResult(
        var status: QRDecoder.Status,
        var contents: String?, // the DDCC Composition
        var issuer: IssuerInfo?,
        var qr: String,
    )

    @PostMapping("/verify")
    fun post(@RequestBody qr: QRContents): VerifyResult {
        val result = QRDecoder(registry).decode(qr.uri);

        return VerifyResult(
            result.status,
            fhirJsonParser.encodeResourceToString(result.contents!!),
            convert(result.issuer),
            result.qr)
    }

    @PostMapping("/upload")
    fun uploadQRImage(@RequestParam("file") file: MultipartFile, redirectAttributes: RedirectAttributes): RedirectView {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "QR Not Found")
            return RedirectView("showCredential")
        }

        val binaryBitmap = BinaryBitmap(HybridBinarizer(
            BufferedImageLuminanceSource(ImageIO.read(ByteArrayInputStream(file.bytes)))
        ))

        val qrContents = try {
            MultiFormatReader().decode(binaryBitmap)
        } catch (e: NotFoundException) {
            redirectAttributes.addFlashAttribute("error", "QR Not Found")
            return RedirectView("showCredential")
        }

        val result = QRDecoder(registry).decode(qrContents.text)
        val fhirBundle = fhirJsonParser.setPrettyPrint(true).encodeResourceToString(result.contents!!)
        val issuerData = jsonMapper.writeValueAsString(convert(result.issuer))

        redirectAttributes.addFlashAttribute("status", result.status)
        redirectAttributes.addFlashAttribute("qr", result.qr)
        redirectAttributes.addFlashAttribute("contents", fhirBundle)
        redirectAttributes.addFlashAttribute("issuer", issuerData)

        return RedirectView("showCredential")
    }

    data class IssuerInfo(
        val displayName: Map<String, String>,
        val displayLogo: String?,
        val status: TrustRegistry.Status,
        val scope: TrustRegistry.Scope,
        val validFrom: Date?,
        val validUntil: Date?,
        val publicKey: String
    )

    private fun convert(issuer: TrustRegistry.TrustedEntity?): IssuerInfo? {
        if (issuer == null) return null;
        return IssuerInfo(
            issuer.displayName,
            issuer.displayLogo,
            issuer.status,
            issuer.scope,
            issuer.validFrom,
            issuer.validUntil,
            KeyUtils.pemFromPublicKey(issuer.publicKey)
        )
    }
}