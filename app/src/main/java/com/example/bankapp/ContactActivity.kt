package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ContactActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ViewPagerAdapterContacts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_group_members_choice)

        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)

        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)

        // Нажатие на кнопки нижнего меню
        setupBottomMenu(clickAnimation)

        val mainTitle: TextView = findViewById(R.id.mainTitle)
        mainTitle.text = "Контакты"

        // Инициализация TabLayout и ViewPager2
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        adapter = ViewPagerAdapterContacts(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Все контакты" else "Избранные"
        }.attach()

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)



        viewPager.adapter = adapter


        //Ненужные кнопки в слоях
        val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)
        btnGoBack.visibility = View.INVISIBLE
        val btnDelete: LinearLayout = findViewById(R.id.btnBox)
        btnDelete.visibility = View.INVISIBLE
        //-----------------------------
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupBottomMenu(clickAnimation: android.view.animation.Animation) {
        val mainBtn: LinearLayout = findViewById(R.id.homeBtn)
        val contactsIcon: ImageView = findViewById(R.id.contactsBtnIcon)
        val contactsTxt: TextView = findViewById(R.id.contactsBtnText)

        contactsIcon.setImageResource(R.drawable.ic_contacts_active)
        contactsTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))

        mainBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            mainBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        val groupsBtn: LinearLayout = findViewById(R.id.groupsBtn)
        groupsBtn.setOnClickListener {
            startActivity(Intent(this, GroupsActivity::class.java))
            groupsBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        val contactsBtn: LinearLayout = findViewById(R.id.contactsBtn)
        contactsBtn.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
            contactsBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }
    }
}
