package org.who.ddccverifier.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.who.ddccverifier.R
import org.who.ddccverifier.databinding.FragmentResultBinding
import org.who.ddccverifier.services.DDCCFormatter
import org.who.ddccverifier.services.DDCCVerifier

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
        if (text != null && !text.isEmpty()) {
            view.text = text
            line.visibility = TextView.VISIBLE
        } else
            line.visibility = TextView.GONE
    }

    data class ResultCard(
        val cardTitle: String?,
        val personName: String?,
        val personDetails: String?,
        val dose: String?,
        val doseDate: String?,
        val nextDose: String?,
        val vaccineValid: String?,
        val vaccineAgainst: String?,
        val vaccineType: String?,
        val vaccineInfo: String?,
        val vaccineInfo2: String?,
        val location: String?,
        val hcid: String?,
        val pha: String?,
        val identifier: String?,
        val hw: String?,
        val validUntil: String?
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.qr != null) {
            val DDCC = DDCCVerifier().unpackAndVerify(args.qr!!);

            when (DDCC.status) {
                DDCCVerifier.Status.INVALID_BASE45 -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_invalid_base45)
                DDCCVerifier.Status.INVALID_ZIP -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_invalid_zip)
                DDCCVerifier.Status.INVALID_COSE -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_invalid_cose)
                DDCCVerifier.Status.KID_NOT_INCLUDED -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_kid_not_included)
                DDCCVerifier.Status.ISSUER_NOT_TRUSTED -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_issuer_not_trusted)
                DDCCVerifier.Status.TERMINATED_KEYS -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_terminated_keys)
                DDCCVerifier.Status.EXPIRED_KEYS -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_expired_keys)
                DDCCVerifier.Status.REVOKED_KEYS -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_revoked_keys)
                DDCCVerifier.Status.INVALID_SIGNATURE -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_invalid_signature)
                DDCCVerifier.Status.VERIFIED -> binding.tvResultTitle.text = resources.getString(R.string.verification_status_verified)
            }

            if (binding.tvResultTitle.text == resources.getString(R.string.verification_status_verified)) {
                binding.tvResultHeader.setBackground(resources.getDrawable(R.drawable.rounded_pill));
                binding.tvResultTitleIcon.text = resources.getString(R.string.fa_check_circle_solid);
            } else {
                binding.tvResultHeader.setBackground(resources.getDrawable(R.drawable.rounded_pill_invalid));
                binding.tvResultTitleIcon.text = resources.getString(R.string.fa_times_circle_solid);
            }

            if (DDCC.issuer != null) {
                binding.tvResultSignedBy.text = "Signed by " + DDCC.issuer!!.displayName;
                binding.tvResultSignedByIcon.text = resources.getString(R.string.fa_check_circle_solid);
            } else {
                binding.tvResultSignedByIcon.text = resources.getString(R.string.fa_times_circle_solid);
                binding.tvResultSignedBy.text = resources.getString(R.string.verification_status_invalid_signature)
            }

            if (DDCC.contents != null) {
                val card : ResultCard = DDCCFormatter().run(DDCC.contents!!)
                //CBOR2FHIR().run(DDCC.contents!!, requireActivity())

                setTextView(binding.tvResultScanDate, card.cardTitle, binding.tvResultScanDate)
                setTextView(binding.tvResultName, card.personName, binding.tvResultName)
                setTextView(binding.tvResultPersonDetails, card.personDetails, binding.tvResultPersonDetails)
                setTextView(binding.tvResultVaccineType, card.vaccineType, binding.tvResultVaccineType)
                setTextView(binding.tvResultDoseTitle, card.dose, binding.tvResultDoseTitle)
                setTextView(binding.tvResultDoseDate, card.doseDate, binding.llResultDoseDate)
                setTextView(binding.tvResultNextDose, card.nextDose, binding.llResultNextDose)
                setTextView(binding.tvResultVaccineValid, card.vaccineValid, binding.llResultVaccineValid)
                setTextView(binding.tvResultVaccineInfo, card.vaccineInfo, binding.llResultVaccineInfo)
                setTextView(binding.tvResultVaccineInfo2, card.vaccineInfo2, binding.llResultVaccineInfo2)
                setTextView(binding.tvResultCentre, card.location, binding.llResultCentre)
                setTextView(binding.tvResultHcid, card.hcid, binding.llResultHcid)
                setTextView(binding.tvResultPha, card.pha, binding.llResultPha)
                setTextView(binding.tvResultIdentifier, card.identifier, binding.llResultIdentifier)
                setTextView(binding.tvResultHw, card.hw, binding.llResultHw)
                setTextView(binding.tvResultValidUntil, card.validUntil, binding.llResultValidUntil)
            }
        }

        binding.btResultClose.setOnClickListener {
            findNavController().navigate(R.id.action_ResultFragment_to_HomeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}