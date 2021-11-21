package org.who.ddccverifier.views

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.upokecenter.cbor.CBORObject
import org.who.ddccverifier.R
import org.who.ddccverifier.databinding.FragmentResultBinding
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.qr != null) {
            val DDCC : CBORObject? = DDCCVerifier().unpackAndVerify(args.qr!!);
            if (DDCC != null) {
                binding.tvResultDob.text = DDCC["birthDate"].toString();
                binding.tvResultName.text = DDCC["name"].toString();
                binding.tvResultValidUntil.text = DDCC["valid_until"].toString();
            }
        }

        /**
         * {
        "manufacturer": {
        "code": "TEST",
        "system": "http://worldhealthorganization.github.io/ddcc/CodeSystem/DDCC-Example-Test-CodeSystem"
        },
        "hw": "http://www.acme.org/practitioners/23",
        "centre": "Vaccination Site",
        "due_date": "2021-07-29",
        "lot": "PT123F",
        "dose": 1,
        "valid_from": "2021-07-08",
        "name": "Eddie Murphy",
        "disease": {
        "code": "840539006",
        "system": "http://snomed.info/sct"
        },
        "sex": {
        "code": "male",
        "system": "http://hl7.org/fhir/administrative-gender"
        },
        "brand": {
        "code": "TEST",
        "system": "http://worldhealthorganization.github.io/ddcc/CodeSystem/DDCC-Example-Test-CodeSystem"
        },
        "vaccine_valid": "2021-07-22",
        "hcid": "US111222333444555666",
        "pha": "wA69g8VD512TfTTdkTNSsG",
        "identifier": "1234567890",
        "vaccine": {
        "code": "1119349007",
        "system": "http://snomed.info/sct"
        },
        "ma_holder": {
        "code": "TEST",
        "system": "http://worldhealthorganization.github.io/ddcc/CodeSystem/DDCC-Example-Test-CodeSystem"
        },
        "total_doses": 2,
        "valid_until": "2022-07-08",
        "birthDate": "1986-09-19",
        "country": {
        "code": "USA",
        "system": "urn:iso:std:iso:3166"
        },
        "date": "2021-07-08"
        }
         */

        binding.btResultClose.setOnClickListener {
            findNavController().navigate(R.id.action_ResultFragment_to_HomeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}