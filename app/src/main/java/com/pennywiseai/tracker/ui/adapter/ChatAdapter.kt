package com.pennywiseai.tracker.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pennywiseai.tracker.databinding.ItemChatMessageBinding
import com.pennywiseai.tracker.databinding.ItemTypingIndicatorBinding
import com.pennywiseai.tracker.data.ChatMessage
import com.pennywiseai.tracker.data.ChatMessageType
import android.animation.AnimatorInflater
import android.view.animation.AnimationUtils
import java.text.SimpleDateFormat
import java.util.*
import android.text.Html
import android.os.Build

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_MESSAGE = 0
        private const val VIEW_TYPE_TYPING = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).type) {
            ChatMessageType.TYPING_INDICATOR -> VIEW_TYPE_TYPING
            else -> VIEW_TYPE_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TYPING -> {
                val binding = ItemTypingIndicatorBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                TypingIndicatorViewHolder(binding)
            }
            else -> {
                val binding = ItemChatMessageBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ChatViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChatViewHolder -> holder.bind(getItem(position))
            is TypingIndicatorViewHolder -> holder.bind()
        }
    }

    class ChatViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        
        fun bind(message: ChatMessage) {
            when (message.type) {
                ChatMessageType.USER -> {
                    binding.userMessageLayout.visibility = View.VISIBLE
                    binding.aiMessageLayout.visibility = View.GONE
                    
                    binding.userMessageText.text = message.content
                    // Timestamp hidden for minimal design
                }
                ChatMessageType.AI -> {
                    binding.userMessageLayout.visibility = View.GONE
                    binding.aiMessageLayout.visibility = View.VISIBLE
                    
                    // For minimal design, use TextView for all content
                    binding.aiMessageText.visibility = View.VISIBLE
                    
                    // Convert basic markdown to HTML for better readability
                    val formattedContent = formatMarkdownToHtml(message.content)
                    binding.aiMessageText.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(formattedContent, Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        @Suppress("DEPRECATION")
                        Html.fromHtml(formattedContent)
                    }
                    
                    // Timestamp hidden for minimal design
                    
                }
                ChatMessageType.TYPING_INDICATOR -> {
                    // This should not happen as typing indicators use separate ViewHolder
                    // But added for when expression exhaustiveness
                    binding.userMessageLayout.visibility = View.GONE
                    binding.aiMessageLayout.visibility = View.GONE
                }
            }
        }
        
        private fun formatMarkdownToHtml(content: String): String {
            return content
                // Headers
                .replace(Regex("^# (.+)$", RegexOption.MULTILINE), "<h3>$1</h3>")
                .replace(Regex("^## (.+)$", RegexOption.MULTILINE), "<h4>$1</h4>")
                .replace(Regex("^### (.+)$", RegexOption.MULTILINE), "<h5>$1</h5>")
                // Bold
                .replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
                // Lists
                .replace(Regex("^- (.+)$", RegexOption.MULTILINE), "• $1<br>")
                // Blockquotes
                .replace(Regex("^> (.+)$", RegexOption.MULTILINE), "<i>$1</i><br>")
                // Line breaks
                .replace("\n\n", "<br><br>")
                .replace("\n", "<br>")
        }
    }

    class TypingIndicatorViewHolder(private val binding: ItemTypingIndicatorBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind() {
            // Start typing animation for each dot with different delays
            val animation1 = AnimationUtils.loadAnimation(binding.root.context, com.pennywiseai.tracker.R.anim.typing_animation_dot1)
            val animation2 = AnimationUtils.loadAnimation(binding.root.context, com.pennywiseai.tracker.R.anim.typing_animation_dot2)
            val animation3 = AnimationUtils.loadAnimation(binding.root.context, com.pennywiseai.tracker.R.anim.typing_animation_dot3)
            
            binding.typingDot1.startAnimation(animation1)
            binding.typingDot2.startAnimation(animation2)
            binding.typingDot3.startAnimation(animation3)
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}