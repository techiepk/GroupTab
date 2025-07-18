package com.pennywiseai.tracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.databinding.ModalUserMenuBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class UserMenuBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: ModalUserMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ModalUserMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.menuSettings.setOnClickListener {
            // Open the comprehensive settings page
            val settingsFragment = OrganizedSettingsFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, settingsFragment)
                .addToBackStack("settings")
                .commit()
            dismiss()
        }
        
        binding.menuHelp.setOnClickListener {
            // Show help dialog - no navigation needed
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("💡 Quick Help")
                .setMessage("🚀 Getting Started:\n" +
                        "1. Grant SMS permissions when prompted\n" +
                        "2. Tap 'Scan SMS Messages' to find transactions\n" +
                        "3. Review your financial data automatically\n\n" +
                        "🤖 AI Features:\n" +
                        "• Smart transaction categorization\n" +
                        "• Proactive spending insights\n" +
                        "• All processing happens on your device\n\n" +
                        "📊 Analytics:\n" +
                        "• Check Analytics tab for spending trends\n" +
                        "• View category breakdowns and insights\n" +
                        "• Track your financial habits over time\n\n" +
                        "🔧 For detailed settings, AI preferences, and privacy options, tap Settings above.")
                .setPositiveButton("Got it", null)
                .show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "UserMenuBottomSheet"
        
        fun newInstance() = UserMenuBottomSheetFragment()
    }
}