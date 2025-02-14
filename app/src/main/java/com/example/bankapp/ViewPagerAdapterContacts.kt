package com.example.bankapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapterContacts(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AllContactsFragment()
            1 -> MarkedContactsFragment() // Реализуйте аналогично AllContactsFragment
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}