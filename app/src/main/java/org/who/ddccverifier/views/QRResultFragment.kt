package org.who.ddccverifier.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.who.ddccverifier.R
import org.who.ddccverifier.databinding.FragmentQrresultBinding

/**
 * Displays a Verifiable Credential after being Scanned by the QRScan Fragment
 */
class QRResultFragment : Fragment() {

    private var _binding: FragmentQrresultBinding? = null
    private val args: QRResultFragmentArgs by navArgs()

    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrresultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvQrresultName.text = args.qr?.subSequence(0,30)
        binding.buttonQrresult.setOnClickListener {
            findNavController().navigate(R.id.action_QRResultFragment_to_HomeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}