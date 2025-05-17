package com.example.bankapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapterGroupInfo(
    fragmentActivity: FragmentActivity,
    private val groupId: String // Параметр для передачи groupId
) : FragmentStateAdapter(fragmentActivity) {

    private val fragmentList = mutableListOf<Fragment>().apply {
        add(GroupInfoFragment.newInstance(groupId)) // Передаем groupId
        add(TotalExpensesFragment.newInstance(groupId))
        add(EmptyFragment())
    }

    override fun getItemCount(): Int = fragmentList.size

    override fun createFragment(position: Int): Fragment = fragmentList[position]

    fun addFragment(fragment: Fragment) {
    }
}
