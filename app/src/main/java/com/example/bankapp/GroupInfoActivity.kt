package com.example.bankapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.ViewGroup
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
    private lateinit var groupName: TextView  // Для отображения названия группы
    private lateinit var groupImage: ImageView  // Для отображения изображения группы

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
        // Нижнее меню----------------------------------------
        val icogroupsBtn: ImageView = findViewById(R.id.groupsBtnIcon)
        icogroupsBtn.setImageResource(R.drawable.ic_groups_outline_active)
        val groupTxt: TextView = findViewById(R.id.groupsBtnText)
        groupTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))
        val mainBtn: LinearLayout = findViewById(R.id.homeBtn)

        mainBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            mainBtn.startAnimation(clickAnimation)
            finish()
        }


        // Инициализация базы данных
        database = FirebaseDatabase.getInstance().reference

        // Инициализация UI элементов
        groupName = findViewById(R.id.mainTitle)  // Здесь отображаем название группы
        groupImage = findViewById(R.id.groupImage)  // Здесь будем отображать изображение группы
        val menuButton: ImageButton = findViewById(R.id.actionGroupBurger)
        val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)

        // Получаем ID группы, переданное через Intent
        val groupId = intent.getStringExtra("GROUP_ID") ?: "Неизвестный ID"

        // Получаем данные группы из Firebase
        loadGroupData(groupId)

        // Обработчик кнопки назад
        btnGoBack.setOnClickListener {
            onBackPressed()
            finish()
            btnGoBack.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        // Обработчик кнопки меню
        menuButton.setOnClickListener {
            val popupMenu = PopupMenu(this, menuButton)
            popupMenu.menuInflater.inflate(R.menu.action_group_menu, popupMenu.menu)

            // Находим элемент "Удалить группу"
            val deleteMenuItem = popupMenu.menu.findItem(R.id.deleteGroupBtn)

            // Изменяем цвет текста
            val spannableString = SpannableString(deleteMenuItem.title)
            spannableString.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.alert_red)), // Красный цвет
                0,
                spannableString.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            deleteMenuItem.title = spannableString

            // Изменяем цвет иконки
            val deleteIcon = deleteMenuItem.icon
            if (deleteIcon != null) {
                val wrappedIcon = DrawableCompat.wrap(deleteIcon)
                DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(this, R.color.alert_red)) // Красный цвет
                deleteMenuItem.icon = wrappedIcon
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.editGroupBtn -> {
                        val intent = Intent(this, NewGroupActivity::class.java)
                        intent.putExtra("GROUP_ID", groupId) // Передача id группы
                        startActivity(intent)
                        finish()
                        true
                    }
                    R.id.exitGroupBtn -> {
                        val intent = Intent(this, GroupsActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                        mainBtn.startAnimation(clickAnimation)
                        finish()
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

        // Инициализация TabLayout и ViewPager2
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        adapter = ViewPagerAdapterGroupInfo(this, groupId)


        // Создаем фрагмент и передаем groupId
        val groupInfoFragment = GroupInfoFragment.newInstance(groupId) // Используем метод newInstance

        adapter.addFragment(groupInfoFragment)
        viewPager.adapter = adapter

        // Связываем TabLayout с ViewPager2
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

            // Или для каждой вкладки отдельно
            for (i in 0 until tabCount) {
                val tabView = (getChildAt(0) as ViewGroup).getChildAt(i)
                val params = tabView.layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(10, 0, 10, 0)
                tabView.layoutParams = params
            }
        }
    }

    // Загрузка данных группы из Firebase
    private fun loadGroupData(groupId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Запрос к базе данных Firebase
            database.child("Users").child(userId).child("Groups").child(groupId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val groupTitle = snapshot.child("title").getValue(String::class.java)
                        val groupImageUri = snapshot.child("imageUri").getValue(String::class.java)

                        // Устанавливаем название группы в TextView
                        groupName.text = groupTitle ?: "Название не найдено"

                        // Загружаем изображение группы с помощью Glide
                        if (!groupImageUri.isNullOrEmpty()) {
                            Glide.with(this@GroupInfoActivity)
                                .load(groupImageUri)
                                .placeholder(R.drawable.placeholder) // Изображение-заглушка
                                .into(groupImage)
                        } else {
                            groupImage.setImageResource(R.drawable.placeholder) // Если изображения нет
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
}
