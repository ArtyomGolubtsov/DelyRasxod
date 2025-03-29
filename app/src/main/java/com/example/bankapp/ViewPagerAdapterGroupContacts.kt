package com.example.bankapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapterGroupContacts(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2  // Количество вкладок: Группы и Контакты

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FriendFragment()
            1 -> FriendBestFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}
