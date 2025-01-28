package com.example.bankapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapterGroupInfo(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GroupInfoFragment()
            1 -> EmptyFragment()
            2 -> TotalExpensesFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
