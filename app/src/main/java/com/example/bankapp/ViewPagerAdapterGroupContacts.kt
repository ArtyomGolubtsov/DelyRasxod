package com.example.bankapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapterGroupContacts(
    fragmentActivity: FragmentActivity,
    private val groupId: String // Добавляем параметр groupId в конструктор
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2  // Количество вкладок: Все контакты и Избранные

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GroupAddFriendInCreateFragment.newInstance(groupId) // Передаем groupId в первый фрагмент
            1 -> GroupAddBestFriendInCreateFragment.newInstance(groupId) // Передаем groupId во второй фрагмент
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}