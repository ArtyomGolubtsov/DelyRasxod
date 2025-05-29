package com.example.bankapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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
    private lateinit var auth: FirebaseAuth
    private var isAdmin = false
    private var groupId: String? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_group_info)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        groupId = intent.getStringExtra("GROUP_ID")

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
        val proflBtn: LinearLayout = findViewById(R.id.profileBtn)
        proflBtn.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
            proflBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        groupName = findViewById(R.id.mainTitle)
        groupImage = findViewById(R.id.groupImage)
        val menuButton: ImageButton = findViewById(R.id.actionGroupBurger)

        loadGroupData(groupId)

        val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)
        btnGoBack.setOnClickListener {
            onBackPressed()
            btnGoBack.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        menuButton.setOnClickListener {
            showGroupMenu(menuButton)
        }

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        adapter = ViewPagerAdapterGroupInfo(this, groupId ?: "")

        val groupInfoFragment = GroupInfoFragment.newInstance(groupId ?: "")
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

    private fun showGroupMenu(anchor: ImageButton) {
        val popupMenu = PopupMenu(this, anchor)
        popupMenu.menuInflater.inflate(R.menu.action_group_menu, popupMenu.menu)

        // Проверяем, является ли пользователь админом
        checkAdminStatus { isAdmin ->
            this.isAdmin = isAdmin

            // Скрываем/показываем пункты меню в зависимости от статуса
            popupMenu.menu.findItem(R.id.editGroupBtn).isVisible = isAdmin
            popupMenu.menu.findItem(R.id.deleteGroupBtn).isVisible = isAdmin
            popupMenu.menu.findItem(R.id.exitGroupBtn).isVisible = !isAdmin

            // Настройка цвета для кнопки удаления
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
                        val intent = Intent(this, NewGroupActivity::class.java)
                        intent.putExtra("GROUP_ID", groupId)
                        startActivity(intent)
                        true
                    }
                    R.id.exitGroupBtn -> {
                        showExitGroupDialog()
                        true
                    }
                    R.id.deleteGroupBtn -> {
                        showDeleteGroupDialog()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.setForceShowIcon(true)
            popupMenu.show()
        }
    }

    private fun checkAdminStatus(callback: (Boolean) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null || groupId == null) {
            callback(false)
            return
        }

        database.child("Groups").child(groupId!!).child("admin")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val adminId = snapshot.getValue(String::class.java)
                    callback(adminId == currentUserId)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false)
                }
            })
    }

    private fun showDeleteGroupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.exit_group_window, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val title = dialogView.findViewById<TextView>(R.id.windowTitle)
        title.text = "Вы уверены, что хотите удалить группу?"

        dialogView.findViewById<AppCompatButton>(R.id.cancelBtn).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<AppCompatButton>(R.id.exitBtn).setOnClickListener {
            deleteGroup()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun deleteGroup() {
        if (groupId == null) return

        // 1. Получаем список всех пользователей группы
        database.child("Groups").child(groupId!!).child("Users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(usersSnapshot: DataSnapshot) {
                    // 2. Для каждого пользователя удаляем группу из его списка
                    for (userSnapshot in usersSnapshot.children) {
                        val userId = userSnapshot.key ?: continue

                        // 3. Проверяем есть ли у пользователя продукты в этой группе
                        database.child("Groups").child(groupId!!).child("Users").child(userId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userProductsSnapshot: DataSnapshot) {
                                    if (userProductsSnapshot.hasChildren()) {
                                        // 4. Если есть продукты - переносим их в EatCheck
                                        val productsMap = mutableMapOf<String, Any>()
                                        for (productSnapshot in userProductsSnapshot.children) {
                                            productsMap[productSnapshot.key!!] = productSnapshot.value!!
                                        }

                                        database.child("Groups").child(groupId!!).child("EatCheck")
                                            .updateChildren(productsMap)
                                    }

                                    // 5. Удаляем группу у пользователя
                                    database.child("Users").child(userId).child("Groups").child(groupId!!)
                                        .removeValue()
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(this@GroupInfoActivity, "Ошибка при переносе продуктов", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }

                    // 6. Удаляем саму группу
                    database.child("Groups").child(groupId!!).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(this@GroupInfoActivity, "Группа удалена", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@GroupInfoActivity, GroupsActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@GroupInfoActivity, "Ошибка удаления группы", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GroupInfoActivity, "Ошибка получения пользователей", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showExitGroupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.exit_group_window, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val title = dialogView.findViewById<TextView>(R.id.windowTitle)
        title.text = "Вы уверены, что хотите выйти из группы?"

        dialogView.findViewById<AppCompatButton>(R.id.cancelBtn).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<AppCompatButton>(R.id.exitBtn).setOnClickListener {
            exitFromGroup()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun exitFromGroup() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null || groupId == null) return

        // 1. Проверяем есть ли у пользователя продукты в этой группе
        database.child("Groups").child(groupId!!).child("Users").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userProductsSnapshot: DataSnapshot) {
                    if (userProductsSnapshot.hasChildren()) {
                        // 2. Если есть продукты - переносим их в EatCheck
                        val productsMap = mutableMapOf<String, Any>()
                        for (productSnapshot in userProductsSnapshot.children) {
                            productsMap[productSnapshot.key!!] = productSnapshot.value!!
                        }

                        database.child("Groups").child(groupId!!).child("EatCheck")
                            .updateChildren(productsMap)
                    }

                    // 3. Удаляем пользователя из группы
                    database.child("Groups").child(groupId!!).child("Users").child(currentUserId)
                        .removeValue()
                        .addOnSuccessListener {
                            // 4. Удаляем группу у пользователя
                            database.child("Users").child(currentUserId).child("Groups").child(groupId!!)
                                .removeValue()
                                .addOnSuccessListener {
                                    Toast.makeText(this@GroupInfoActivity, "Вы вышли из группы", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@GroupInfoActivity, GroupsActivity::class.java))
                                    finish()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@GroupInfoActivity, "Ошибка выхода из группы", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GroupInfoActivity, "Ошибка при переносе продуктов", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadGroupData(groupId: String?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null && groupId != null) {
            database.child("Groups").child(groupId)
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

    private fun openActivity(targetActivity: Class<*>) {
        val intent = Intent(this, targetActivity)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}