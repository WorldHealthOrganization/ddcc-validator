package org.who.ddccverifier.web

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.fasterxml.jackson.databind.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.web.servlet.view.RedirectView
import org.who.ddccverifier.QRDecoder

@Controller
class UIController {
    @Autowired
    lateinit var json: ObjectMapper

    @RequestMapping("/index")
    fun index() = "index"

    @GetMapping("/showCredential")
    fun showCredential() = "showCredential"

    @PostMapping("/upload")
    fun uploadQRImage(file: MultipartFile, redirect: RedirectAttributes): RedirectView {
        val result = RestController().findAndVerify(file);

        if (result.status == QRDecoder.Status.NOT_FOUND) {
            redirect.addFlashAttribute("error", "QR Not Found")
            return RedirectView("showCredential");
        }

        val fhir = FhirContext.forCached(FhirVersionEnum.R4)
            .newJsonParser()
            .setPrettyPrint(true);

        val json = json.enable(SerializationFeature.INDENT_OUTPUT);

        redirect.addFlashAttribute("status", result.status)
        redirect.addFlashAttribute("qr", result.qr)
        if (result.contents != null)
            redirect.addFlashAttribute("contents", fhir.encodeResourceToString(result.contents))
        if (result.issuer != null)
            redirect.addFlashAttribute("issuer", json.writeValueAsString(result.issuer))
        if (result.unpacked != null)
            redirect.addFlashAttribute("unpacked", json.readTree(result.unpacked).toPrettyString())

        return RedirectView("showCredential")
    }
}