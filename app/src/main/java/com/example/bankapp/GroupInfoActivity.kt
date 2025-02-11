package com.example.bankapp

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroupInfoActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ViewPagerAdapterGroupInfo
    private var isPageChangeProgrammatic = false
    private var isChatSheetShown = false
    private lateinit var database: DatabaseReference
    private lateinit var groupName: TextView
    private lateinit var groupImage: ImageView

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_group_info)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)
        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)

        val icogroupsBtn: ImageView = findViewById(R.id.groupsBtnIcon)
        icogroupsBtn.setImageResource(R.drawable.ic_groups_outline_active)
        val groupTxt: TextView = findViewById(R.id.groupsBtnText)
        groupTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))
        val mainBtn: LinearLayout = findViewById(R.id.homeBtn)

        mainBtn.setOnClickListener {
            openActivity(MainActivity::class.java)
            mainBtn.startAnimation(clickAnimation)
            finish()
        }
        val contactsBtn: LinearLayout = findViewById(R.id.contactsBtn)
        contactsBtn.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
            contactsBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        database = FirebaseDatabase.getInstance().reference

        groupName = findViewById(R.id.mainTitle)
        groupImage = findViewById(R.id.groupImage)
        val menuButton: ImageButton = findViewById(R.id.actionGroupBurger)
        val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)

        val groupId = intent.getStringExtra("GROUP_ID") ?: "Неизвестный ID"
        loadGroupData(groupId)

        btnGoBack.setOnClickListener {
            onBackPressed()
            btnGoBack.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        menuButton.setOnClickListener {
            val popupMenu = PopupMenu(this, menuButton)
            popupMenu.menuInflater.inflate(R.menu.action_group_menu, popupMenu.menu)
            val deleteMenuItem = popupMenu.menu.findItem(R.id.deleteGroupBtn)
            val spannableString = SpannableString(deleteMenuItem.title)
            spannableString.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.alert_red)),
                0, spannableString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            deleteMenuItem.title = spannableString

            val deleteIcon = deleteMenuItem.icon
            if (deleteIcon != null) {
                val wrappedIcon = DrawableCompat.wrap(deleteIcon)
                DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(this, R.color.alert_red))
                deleteMenuItem.icon = wrappedIcon
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.editGroupBtn -> {
                        openActivity(NewGroupActivity::class.java)
                        true
                    }
                    R.id.exitGroupBtn -> {
                        showExitGroupDialog()
                        true
                    }
                    R.id.deleteGroupBtn -> {
                        // Обработка удаления группы
                        true
                    }
                    else -> false
                }
            }
            popupMenu.setForceShowIcon(true)
            popupMenu.show()
        }

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        adapter = ViewPagerAdapterGroupInfo(this, groupId)

        val groupInfoFragment = GroupInfoFragment.newInstance(groupId)
        adapter.addFragment(groupInfoFragment)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Информация"
                1 -> "Итого"
                2 -> "Чат"
                else -> ""
            }
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (!isPageChangeProgrammatic) {
                    if (position == 2) {
                        showChatBottomSheet()
                        resetViewPagerPosition()
                    }
                }
            }
        })

        tabLayout.apply {
            for (i in 0 until tabCount) {
                val tabView = (getChildAt(0) as ViewGroup).getChildAt(i)
                val params = tabView.layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(10, 0, 10, 0)
                tabView.layoutParams = params
            }
        }
    }

    private fun loadGroupData(groupId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            database.child("Users").child(userId).child("Groups").child(groupId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val groupTitle = snapshot.child("title").getValue(String::class.java)
                        val groupImageUri = snapshot.child("imageUri").getValue(String::class.java)

                        groupName.text = groupTitle ?: "Название не найдено"

                        if (!groupImageUri.isNullOrEmpty()) {
                            Glide.with(this@GroupInfoActivity)
                                .load(groupImageUri)
                                .placeholder(R.drawable.placeholder)
                                .into(groupImage)
                        } else {
                            groupImage.setImageResource(R.drawable.placeholder)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        groupName.text = "Ошибка загрузки данных"
                    }
                })
        } else {
            groupName.text = "Пользователь не найден"
        }
    }

    private fun showChatBottomSheet() {
        if (isChatSheetShown) return

        isChatSheetShown = true
        val bottomSheet = ChatBottomSheetFragment().apply {
            setDismissListener {
                isChatSheetShown = false
            }
        }
        bottomSheet.show(supportFragmentManager, "ChatBottomSheet")
    }

    private fun resetViewPagerPosition() {
        isPageChangeProgrammatic = true
        viewPager.setCurrentItem(1, true)
        isPageChangeProgrammatic = false
    }

    private fun showExitGroupDialog() {
        val exitGroupDialogFragment = ExitGroupDialogFragment()

        exitGroupDialogFragment.setExitGroupListener(object : ExitGroupDialogFragment.ExitGroupListener {
            override fun onExitConfirmed() {
                // Здесь реализуйте логику выхода из группы
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    database.child("Groups").child(intent.getStringExtra("GROUP_ID")!!).child("Members").child(userId).removeValue()
                }
                val intent = Intent(this@GroupInfoActivity, GroupsActivity::class.java)
                intent.putExtra("GROUP_ID", intent.getStringExtra("GROUP_ID")) // Здесь передаем ID группы
                startActivity(intent)
                finish()
            }

            override fun onExitCancelled() {
                // Логика для отмены выхода из группы, если необходимо
            }
        })

        exitGroupDialogFragment.show(supportFragmentManager, "ExitGroupDialog")
    }

    private fun openActivity(targetActivity: Class<*>) {
        val intent = Intent(this, targetActivity)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}
