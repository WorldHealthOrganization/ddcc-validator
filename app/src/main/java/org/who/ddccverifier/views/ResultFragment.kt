package org.who.ddccverifier.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.*
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.hl7.fhir.r4.model.Composition
import org.who.ddccverifier.R
import org.who.ddccverifier.databinding.FragmentResultBinding
import org.who.ddccverifier.services.*
import org.who.ddccverifier.services.fhir.CQLEvaluator
import org.who.ddccverifier.services.fhir.FHIRLibraryLoader
import org.who.ddccverifier.services.qrs.QRUnpacker
import java.io.InputStream

/**
 * Displays a Verifiable Credential after being Scanned by the QRScan Fragment
 */
class ResultFragment : Fragment() {
    private var _binding: FragmentResultBinding? = null
    private val args: ResultFragmentArgs by navArgs()

    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setTextView(view: TextView, text: String?, line: View) {
        if (line == null) return;
        if (text != null && !text.isEmpty()) {
            view.text = text
            line.visibility = TextView.VISIBLE
        } else
            line.visibility = TextView.GONE
    }

    data class ResultCard(
        val hcid: String?,
        val cardTitle: String?,
        val validUntil: String?,

        val personName: String?,
        val personDetails: String?,
        val identifier: String?,

        // immunization
        val dose: String?,
        val doseDate: String?,
        val vaccineValid: String?,
        val vaccineAgainst: String?,
        val vaccineType: String?,
        val vaccineInfo: String?,
        val vaccineInfo2: String?,
        val location: String?,
        val pha: String?,
        val hw: String?,

        // recommendations
        val nextDose: String?
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvResultHeader.visibility = TextView.INVISIBLE
        binding.tvResultCard.visibility = TextView.INVISIBLE

        if (args.qr != null) {
            resolveAndShowQR(args.qr!!)
        }

        binding.btResultClose.setOnClickListener {
            findNavController().navigate(R.id.action_ResultFragment_to_HomeFragment)
        }
    }

    fun updateScreen(DDCC: QRUnpacker.VerificationResult) {
        binding.tvResultHeader.visibility = TextView.VISIBLE
        binding.tvResultCard.visibility = TextView.VISIBLE

        when (DDCC.status) {
            QRUnpacker.Status.NOT_SUPPORTED -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_invalid_base45)
            QRUnpacker.Status.INVALID_BASE45 -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_invalid_base45)
            QRUnpacker.Status.INVALID_ZIP -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_invalid_zip)
            QRUnpacker.Status.INVALID_COSE -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_invalid_cose)
            QRUnpacker.Status.KID_NOT_INCLUDED -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_kid_not_included)
            QRUnpacker.Status.ISSUER_NOT_TRUSTED -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_issuer_not_trusted)
            QRUnpacker.Status.TERMINATED_KEYS -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_terminated_keys)
            QRUnpacker.Status.EXPIRED_KEYS -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_expired_keys)
            QRUnpacker.Status.REVOKED_KEYS -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_revoked_keys)
            QRUnpacker.Status.INVALID_SIGNATURE -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_invalid_signature)
            QRUnpacker.Status.VERIFIED -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_verified)
        }

        if (binding.tvResultTitle.text == resources.getString(R.string.verification_status_verified)) {
            binding.tvResultHeader.setBackground(resources.getDrawable(R.drawable.rounded_pill))
            binding.tvResultTitleIcon.text = resources.getString(R.string.fa_check_circle_solid);
        } else {
            binding.tvResultHeader.setBackground(resources.getDrawable(R.drawable.rounded_pill_invalid))
            binding.tvResultTitleIcon.text = resources.getString(R.string.fa_times_circle_solid);
        }

        if (DDCC.issuer != null) {
            binding.tvResultSignedBy.text = "Signed by " + DDCC.issuer!!.displayName["en"]
            binding.tvResultSignedByIcon.text = resources.getString(R.string.fa_check_circle_solid);
        } else {
            binding.tvResultSignedByIcon.text = resources.getString(R.string.fa_times_circle_solid);
            binding.tvResultSignedBy.text = resources.getString(R.string.verification_status_invalid_signature)
        }

        if (DDCC.contents != null) {
            val card = DDCCFormatter().run(DDCC.contents!!)

            // Credential
            setTextView(binding.tvResultScanDate, card.cardTitle, binding.tvResultScanDate)
            setTextView(binding.tvResultValidUntil, card.validUntil, binding.llResultValidUntil)

            // Patient
            setTextView(binding.tvResultName, card.personName, binding.tvResultName)
            setTextView(binding.tvResultPersonDetails, card.personDetails, binding.tvResultPersonDetails)
            setTextView(binding.tvResultIdentifier, card.identifier, binding.tvResultIdentifier)

            // Immunization
            setTextView(binding.tvResultVaccineType, card.vaccineType, binding.tvResultVaccineType)
            setTextView(binding.tvResultDoseTitle, card.dose, binding.tvResultDoseTitle)
            setTextView(binding.tvResultDoseDate, card.doseDate, binding.llResultDoseDate)
            setTextView(binding.tvResultVaccineValid, card.vaccineValid, binding.llResultVaccineValid)
            setTextView(binding.tvResultVaccineInfo, card.vaccineInfo, binding.llResultVaccineInfo)
            setTextView(binding.tvResultVaccineInfo2, card.vaccineInfo2, binding.llResultVaccineInfo2)
            setTextView(binding.tvResultCentre, card.location, binding.llResultCentre)
            setTextView(binding.tvResultHcid, card.hcid, binding.llResultHcid)
            setTextView(binding.tvResultPha, card.pha, binding.llResultPha)
            setTextView(binding.tvResultHw, card.hw, binding.llResultHw)

            // Recommendation
            setTextView(binding.tvResultNextDose, card.nextDose, binding.llResultNextDose)

            // Status
            setTextView(binding.tvResultStatus, "... Processing ...", binding.tvResultStatus)
        }
    }

    fun showStatus(DDCC: Composition) = runBlocking {
        var viewModelJob = Job()
        val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

        uiScope.launch {
            withContext(Dispatchers.IO) {
                val status = resolveStatus(DDCC)
                val statusStr = when (status) {
                    true -> "COVID Safe"
                    false -> "COVID Vulnerable"
                    null -> "Unable to evaluate"
                }
                withContext(Dispatchers.Main){
                    _binding?.let {
                        setTextView(binding.tvResultStatus, statusStr, binding.tvResultStatus)
                    }
                }
            }
        }
    }

    fun resolveAndShowQR(qr: String) = runBlocking {
        var viewModelJob = Job()
        val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

        uiScope.launch {
            withContext(Dispatchers.IO) {
                val result = resolveQR(qr)

                if (result.contents != null)
                    showStatus(result.contents!!)

                withContext(Dispatchers.Main){
                    updateScreen(result)
                }
            }
        }
    }

    private fun open(file: String): InputStream {
        return resources.assets.open(file);
    }

    suspend fun resolveQR(qr: String): QRUnpacker.VerificationResult {
        // Triggers Networking
        return QRUnpacker().decode(qr)
    }

    suspend fun resolveStatus(DDCC: Composition): Boolean? {
        // Might be slow
        return try {
             CQLEvaluator(FHIRLibraryLoader(::open)).resolve(
                "CompletedImmunization",
                VersionedIdentifier().withId("DDCCPass").withVersion("0.0.1"),
                DDCC) as Boolean
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}