package com.pennywiseai.tracker.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.pennywiseai.tracker.viewmodel.TransactionGroupViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Simple dialog showing group statistics and basic management
 */
class GroupStatsDialog : DialogFragment() {
    
    companion object {
        fun newInstance(): GroupStatsDialog {
            return GroupStatsDialog()
        }
    }
    
    private val groupViewModel: TransactionGroupViewModel by viewModels()
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message = buildString {
            append("Transaction Grouping Status\n\n")
            append("• Groups are automatically created when you have 2+ similar transactions\n")
            append("• Long press any transaction to remove it from a group\n")
            append("• Groups help organize your spending patterns\n\n")
            append("Grouping works by:\n")
            append("1. Exact merchant matches\n")
            append("2. Similar merchant names\n")
            append("3. Same UPI ID transactions\n")
            append("4. Similar amounts in same category\n")
            append("5. Recurring payment patterns")
        }
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Group Management")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Reset All Groups") { _, _ ->
                resetAllGroups()
            }
            .create()
    }
    
    private fun resetAllGroups() {
        // This would trigger regrouping of all transactions
        AlertDialog.Builder(requireContext())
            .setTitle("Reset All Groups")
            .setMessage("This will remove all transaction groups and create new ones automatically. Continue?")
            .setPositiveButton("Reset") { _, _ ->
                groupViewModel.startAutoGrouping()
                android.widget.Toast.makeText(
                    requireContext(),
                    "Regrouping all transactions...",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}