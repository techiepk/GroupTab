package com.pennywiseai.tracker.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.pennywiseai.tracker.ui.GroupedTransactionsTabFragment
import com.pennywiseai.tracker.ui.UnknownTransactionsTabFragment

class TransactionsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    
    override fun getItemCount(): Int = 2
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GroupedTransactionsTabFragment.newInstance()
            1 -> UnknownTransactionsTabFragment.newInstance()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
    
    fun getPageTitle(position: Int): String {
        return when (position) {
            0 -> "Groups"
            1 -> "Unknown"
            else -> ""
        }
    }
}