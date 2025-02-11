package com.example.bankapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapterContacts(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    private val fragmentList = mutableListOf<Fragment>().apply {
        add(AllContactsFragment())
        add(MarkedContactsFragment())
    }

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) AllContactsFragment() else MarkedContactsFragment()
    }

    fun addFragment(fragment: Fragment) {

    }

}
