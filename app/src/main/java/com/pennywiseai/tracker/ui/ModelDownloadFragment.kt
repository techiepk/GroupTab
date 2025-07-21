package com.pennywiseai.tracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pennywiseai.tracker.databinding.FragmentModelDownloadBinding
import com.pennywiseai.tracker.viewmodel.SettingsViewModel
import com.pennywiseai.tracker.ui.view.ModelDownloadStatusView

/**
 * Example fragment showing how to use the ModelDownloadStatusView
 * This can be integrated into existing settings or shown as a separate screen
 */
class ModelDownloadFragment : Fragment() {
    
    private var _binding: FragmentModelDownloadBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var settingsViewModel: SettingsViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModelDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        
        setupDownloadStatusView()
        observeViewModel()
    }
    
    private fun setupDownloadStatusView() {
        binding.modelDownloadStatus.setCallbacks(
            onDownload = {
                settingsViewModel.startModelDownload()
            },
            onCancel = {
                settingsViewModel.cancelModelDownload()
            },
            onDelete = {
                settingsViewModel.deleteModel()
            }
        )
    }
    
    private fun observeViewModel() {
        // Observe model status
        settingsViewModel.modelStatus.observe(viewLifecycleOwner) { status ->
            val progress = settingsViewModel.modelDownloadProgress.value
            binding.modelDownloadStatus.updateStatus(status, progress)
        }
        
        // Observe download progress
        settingsViewModel.modelDownloadProgress.observe(viewLifecycleOwner) { progress ->
            progress?.let {
                val status = settingsViewModel.modelStatus.value ?: SettingsViewModel.ModelStatus.UNKNOWN
                binding.modelDownloadStatus.updateStatus(status, progress)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
