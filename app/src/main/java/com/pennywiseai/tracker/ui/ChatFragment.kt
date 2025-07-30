package com.pennywiseai.tracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
// Regular Fragment, not dialog anymore
import androidx.fragment.app.viewModels
// Navigation import removed - using activity finish instead
import androidx.recyclerview.widget.LinearLayoutManager
import com.pennywiseai.tracker.data.ChatMessageType
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.databinding.FragmentChatBinding
import com.pennywiseai.tracker.ui.adapter.ChatAdapter
import com.pennywiseai.tracker.viewmodel.ChatViewModel
import com.pennywiseai.tracker.firebase.FirebaseHelper
import com.pennywiseai.tracker.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Log screen view
        FirebaseHelper.logScreenView("Chat", "ChatFragment")

        // Adjust window soft input mode for better keyboard behavior
        requireActivity().window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN or
            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )

        setupHeader()
        setupRecyclerView()
        setupInput()
        observeData()
        setupEmptyStateSuggestions()
        setupKeyboardListener()

        // Show welcome message
        viewModel.addWelcomeMessage()
    }

    private fun setupHeader() {
        // Setup options menu if button exists
        binding.optionsButton?.setOnClickListener {
            showOptionsMenu()
        }
        
        // Setup quick suggestion chips
        // Quick suggestions removed for minimal design
    }
    
    private fun showOptionsMenu() {
        val options = arrayOf(
            "Clear Chat History",
            "Export Chat",
            "AI Settings",
            "Help & Tips"
        )
        
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chat Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showClearChatConfirmation()
                    1 -> showExportOptions()
                    2 -> showAISettings()
                    3 -> showHelpDialog()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showClearChatConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear Chat History")
            .setMessage("Are you sure you want to clear all chat messages? This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                viewModel.clearChatHistory()
                Toast.makeText(requireContext(), "Chat history cleared", Toast.LENGTH_SHORT).show()
                FirebaseHelper.logEvent("chat_cleared", null)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showExportOptions() {
        val exportFormats = arrayOf("Text File (.txt)", "Markdown (.md)")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Export Chat")
            .setItems(exportFormats) { _, which ->
                when (which) {
                    0 -> exportChatAsText()
                    1 -> exportChatAsMarkdown()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun exportChatAsText() {
        try {
            val messages = viewModel.messages.value ?: emptyList()
            if (messages.isEmpty()) {
                Toast.makeText(requireContext(), "No messages to export", Toast.LENGTH_SHORT).show()
                return
            }
            
            val exportDir = File(requireContext().filesDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "chat_export_${dateFormat.format(Date())}.txt"
            val file = File(exportDir, fileName)
            
            FileWriter(file).use { writer ->
                writer.appendLine("PennyWise AI Chat Export")
                writer.appendLine("Exported on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                writer.appendLine("=" * 50)
                writer.appendLine()
                
                messages.forEach { message ->
                    val sender = if (message.type == ChatMessageType.USER) "You" else "AI"
                    val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                    writer.appendLine("[$timestamp] $sender: ${message.content}")
                    writer.appendLine()
                }
            }
            
            shareFile(file, "text/plain")
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun exportChatAsMarkdown() {
        try {
            val messages = viewModel.messages.value ?: emptyList()
            if (messages.isEmpty()) {
                Toast.makeText(requireContext(), "No messages to export", Toast.LENGTH_SHORT).show()
                return
            }
            
            val exportDir = File(requireContext().filesDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "chat_export_${dateFormat.format(Date())}.md"
            val file = File(exportDir, fileName)
            
            FileWriter(file).use { writer ->
                writer.appendLine("# PennyWise AI Chat Export")
                writer.appendLine()
                writer.appendLine("**Exported on:** ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                writer.appendLine()
                writer.appendLine("---")
                writer.appendLine()
                
                messages.forEach { message ->
                    val sender = if (message.type == ChatMessageType.USER) "**You**" else "**AI Assistant**"
                    val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                    writer.appendLine("### $sender - $timestamp")
                    writer.appendLine()
                    writer.appendLine(message.content)
                    writer.appendLine()
                    writer.appendLine("---")
                    writer.appendLine()
                }
            }
            
            shareFile(file, "text/markdown")
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PennyWise AI Chat Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(Intent.createChooser(intent, "Share Chat Export"))
    }
    
    private fun showAISettings() {
        val currentSettings = viewModel.getAISettings()
        val options = arrayOf(
            "Response Style: ${currentSettings.responseStyle}",
            "Context Memory: ${currentSettings.contextMemory}",
            "Auto-suggestions: ${if (currentSettings.autoSuggestions) "On" else "Off"}",
            "Financial Tips: ${if (currentSettings.showFinancialTips) "On" else "Off"}"
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("AI Assistant Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showResponseStyleDialog()
                    1 -> showContextMemoryDialog()
                    2 -> toggleAutoSuggestions()
                    3 -> toggleFinancialTips()
                }
            }
            .setPositiveButton("Done", null)
            .show()
    }
    
    private fun showResponseStyleDialog() {
        val styles = arrayOf("Concise", "Detailed", "Conversational")
        val currentStyle = viewModel.getAISettings().responseStyle
        val selectedIndex = styles.indexOf(currentStyle)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Response Style")
            .setSingleChoiceItems(styles, selectedIndex) { dialog, which ->
                viewModel.updateResponseStyle(styles[which])
                dialog.dismiss()
                showAISettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showContextMemoryDialog() {
        val options = arrayOf("Last 5 messages", "Last 10 messages", "Full conversation")
        val currentSetting = viewModel.getAISettings().contextMemory
        val selectedIndex = when (currentSetting) {
            "5" -> 0
            "10" -> 1
            "full" -> 2
            else -> 1
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Context Memory")
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                val value = when (which) {
                    0 -> "5"
                    1 -> "10"
                    2 -> "full"
                    else -> "10"
                }
                viewModel.updateContextMemory(value)
                dialog.dismiss()
                showAISettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun toggleAutoSuggestions() {
        viewModel.toggleAutoSuggestions()
        showAISettings()
    }
    
    private fun toggleFinancialTips() {
        viewModel.toggleFinancialTips()
        showAISettings()
    }
    
    private fun showHelpDialog() {
        val helpText = """
            💡 **Tips for using PennyWise AI Chat:**
            
            • Ask about your spending patterns
            • Get insights on specific categories
            • Track subscription costs
            • Analyze spending trends
            • Get budget recommendations
            
            **Example questions:**
            - "What did I spend on food this month?"
            - "Show me my biggest expenses"
            - "How much do I spend on subscriptions?"
            - "Compare my spending to last month"
            - "Help me create a budget"
            
            **Quick Actions:**
            Use the suggestion chips for common queries
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("AI Assistant Help")
            .setMessage(helpText)
            .setPositiveButton("Got it", null)
            .show()
    }
    
    private operator fun String.times(count: Int): String = this.repeat(count)
    
    private fun setupQuickSuggestions() {
        // Quick suggestion chips removed for minimal design
    }
    
    private fun sendQuickMessage(message: String) {
        binding.messageInput.setText(message)
        sendMessage()
    }
    
    private fun setupKeyboardListener() {
        // Simple keyboard detection
        var isKeyboardShowing = false
        
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            
            // If keyboard is shown (keypad height > 200dp)
            val threshold = resources.getDimensionPixelSize(R.dimen.keyboard_height_threshold)
            if (keypadHeight > threshold) {
                if (!isKeyboardShowing) {
                    isKeyboardShowing = true
                    (requireActivity() as? MainActivity)?.hideBottomNavigation()
                    // Scroll to bottom when keyboard appears
                    if (chatAdapter.itemCount > 0) {
                        binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                    }
                }
            } else {
                if (isKeyboardShowing) {
                    isKeyboardShowing = false
                    (requireActivity() as? MainActivity)?.showBottomNavigation()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            
            // Add keyboard listener to scroll to bottom when keyboard appears
            addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                if (bottom < oldBottom && chatAdapter.itemCount > 0) {
                    post {
                        smoothScrollToPosition(chatAdapter.itemCount - 1)
                    }
                }
            }
        }
    }

    private fun setupInput() {
        // Setup send button click
        binding.sendButton.setOnClickListener {
            sendMessage()
        }

        // Send on enter key
        binding.messageInput.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
        
        // Show/hide suggestions based on input
        binding.messageInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.messageInput.text.toString().isEmpty()) {
                showQuickActions()
            }
        }
        
        // Setup quick actions
        binding.actionSpending.setOnClickListener {
            sendQuickMessage("How much did I spend this month?")
        }
        
        binding.actionCategories.setOnClickListener {
            sendQuickMessage("What are my top spending categories?")
        }
        
        binding.actionSave.setOnClickListener {
            sendQuickMessage("How can I save money?")
        }
    }
    
    private fun sendMessage() {
        val message = binding.messageInput.text.toString().trim()
        if (message.isNotEmpty()) {
            viewModel.sendMessage(message)
            binding.messageInput.text?.clear()
            hideQuickActions()
        }
    }
    
    private fun showQuickActions() {
        binding.quickActions.visibility = View.VISIBLE
    }
    
    private fun hideQuickActions() {
        binding.quickActions.visibility = View.GONE
    }

    private fun observeData() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages) {
                // Auto-scroll to bottom when new message is added
                if (messages.isNotEmpty()) {
                    binding.chatRecyclerView.smoothScrollToPosition(messages.size - 1)
                    hideEmptyState()
                } else {
                    showEmptyState()
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.sendButton.isEnabled = !isLoading
            if (isLoading) {
                binding.sendButton.alpha = 0.5f
            } else {
                binding.sendButton.alpha = 1.0f
            }
            binding.messageInput.isEnabled = !isLoading
        }
    }

    private fun showEmptyState() {
        binding.apply {
            chatRecyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        }
    }
    
    private fun hideEmptyState() {
        binding.apply {
            chatRecyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }
    
    private fun sendMessage(message: String) {
        binding.messageInput.setText(message)
        // Trigger send
        sendMessage()
    }
    
    private fun setupEmptyStateSuggestions() {
        // Set up click listeners for suggestion chips
        binding.suggestionSpending?.setOnClickListener {
            sendQuickMessage("What's my spending this month?")
        }
        
        binding.suggestionSave?.setOnClickListener {
            sendQuickMessage("How can I save money?")
        }
        
        binding.suggestionSubscriptions?.setOnClickListener {
            sendQuickMessage("Show me my subscriptions")
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // Restore default window soft input mode
        requireActivity().window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
        )
        
        // Ensure bottom navigation is visible when leaving chat
        (requireActivity() as? MainActivity)?.showBottomNavigation()
        
        _binding = null
    }
    
    companion object {
        fun newInstance(): ChatFragment {
            return ChatFragment()
        }
    }
}