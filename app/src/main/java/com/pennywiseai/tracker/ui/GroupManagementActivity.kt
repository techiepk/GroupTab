package com.pennywiseai.tracker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.pennywiseai.tracker.data.TransactionGroup
import com.pennywiseai.tracker.databinding.ActivityGroupManagementBinding
import com.pennywiseai.tracker.ui.adapter.GroupManagementAdapter
import com.pennywiseai.tracker.ui.dialog.CreateGroupDialog
import com.pennywiseai.tracker.ui.dialog.EditGroupDialog
import com.pennywiseai.tracker.viewmodel.GroupManagementViewModel
import com.google.android.material.snackbar.Snackbar
import android.view.Menu
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.GroupSortOrder
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GroupManagementActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGroupManagementBinding
    private val viewModel: GroupManagementViewModel by viewModels()
    private lateinit var adapter: GroupManagementAdapter
    
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, GroupManagementActivity::class.java))
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeData()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Manage Groups"
        }
        binding.toolbar.setNavigationContentDescription("Go back")
    }
    
    private fun setupRecyclerView() {
        adapter = GroupManagementAdapter(
            onGroupClick = { group ->
                // Navigate to group detail
                GroupDetailActivity.start(this, group.id, group.name)
            },
            onEditClick = { group ->
                showEditGroupDialog(group)
            },
            onDeleteClick = { group ->
                showDeleteConfirmation(group)
            },
            onMergeClick = { group ->
                showMergeDialog(group)
            }
        )
        
        binding.recyclerView.apply {
            this.adapter = this@GroupManagementActivity.adapter
            layoutManager = LinearLayoutManager(this@GroupManagementActivity)
        }
    }
    
    private fun setupFab() {
        binding.fabCreateGroup.setOnClickListener {
            showCreateGroupDialog()
        }
        
        // Also handle empty view button
        binding.createGroupButton.setOnClickListener {
            showCreateGroupDialog()
        }
    }
    
    private fun showCreateGroupDialog() {
        val dialog = CreateGroupDialog.newInstance()
        dialog.show(supportFragmentManager, "CreateGroupDialog")
    }
    
    private fun observeData() {
        // Observe all groups
        viewModel.allGroups.observe(this) { groups ->
            adapter.submitList(groups)
            updateEmptyState(groups.isEmpty())
        }
        
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observe errors
        viewModel.error.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun showEditGroupDialog(group: TransactionGroup) {
        val dialog = EditGroupDialog.newInstance(group)
        dialog.show(supportFragmentManager, "EditGroupDialog")
    }
    
    private fun showDeleteConfirmation(group: TransactionGroup) {
        AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("Delete '${group.name}'? This will ungroup all transactions in this group.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteGroup(group)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showMergeDialog(group: TransactionGroup) {
        // Get other groups for merging
        val otherGroups = viewModel.allGroups.value?.filter { it.id != group.id } ?: emptyList()
        
        if (otherGroups.isEmpty()) {
            Snackbar.make(binding.root, "No other groups to merge with", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        val groupNames = otherGroups.map { it.name }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Merge '${group.name}' with...")
            .setItems(groupNames) { _, which ->
                val targetGroup = otherGroups[which]
                confirmMerge(group, targetGroup)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun confirmMerge(sourceGroup: TransactionGroup, targetGroup: TransactionGroup) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Merge")
            .setMessage("Merge '${sourceGroup.name}' into '${targetGroup.name}'? This action cannot be undone.")
            .setPositiveButton("Merge") { _, _ ->
                viewModel.mergeGroups(sourceGroup, targetGroup)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_group_management, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_sort -> {
                showSortOptionsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showSortOptionsDialog() {
        val currentSort = viewModel.sortOrder.value ?: GroupSortOrder.getDefault()
        val sortOptions = GroupSortOrder.values()
        val sortOptionNames = sortOptions.map { it.displayName }.toTypedArray()
        val currentIndex = sortOptions.indexOf(currentSort)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Sort Groups")
            .setSingleChoiceItems(sortOptionNames, currentIndex) { dialog, which ->
                viewModel.setSortOrder(sortOptions[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}