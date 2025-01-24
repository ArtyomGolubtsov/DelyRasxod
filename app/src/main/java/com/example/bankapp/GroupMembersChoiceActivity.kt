    package com.example.bankapp

    import android.content.Intent
    import android.os.Bundle
    import android.text.Editable
    import android.text.TextWatcher
    import android.util.Log
    import android.widget.EditText
    import android.widget.ImageButton
    import android.widget.ImageView
    import android.widget.LinearLayout
    import android.widget.TextView
    import androidx.activity.enableEdgeToEdge
    import androidx.appcompat.app.AppCompatActivity
    import androidx.appcompat.widget.AppCompatButton
    import androidx.core.content.ContextCompat
    import androidx.core.view.ViewCompat
    import androidx.core.view.WindowInsetsCompat
    import androidx.viewpager2.widget.ViewPager2
    import com.google.android.material.tabs.TabLayout
    import com.google.android.material.tabs.TabLayoutMediator
    import com.google.firebase.database.*

    class GroupMembersChoiceActivity : AppCompatActivity() {

        private lateinit var database: DatabaseReference
        private lateinit var searchEditText: EditText
        private lateinit var mainTitle: TextView
        private lateinit var tabLayout: TabLayout
        private lateinit var viewPager: ViewPager2
        private lateinit var adapter: ViewPagerAdapter

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
            window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)
            setContentView(R.layout.activity_group_members_choice)
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            // Инициализация TabLayout и ViewPager2
            tabLayout = findViewById(R.id.tabLayout)
            viewPager = findViewById(R.id.viewPager)
            // Настройка адаптера для ViewPager2
            adapter = ViewPagerAdapter(this)
            viewPager.adapter = adapter
            // Связываем TabLayout с ViewPager2
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = if (position == 0) "Все контакты" else "Избранное"
            }.attach()

            // Инициализация базы данных
            database = FirebaseDatabase.getInstance().getReference("Users")


            // Настройка нижнего меню
            val icogroupsBtn: ImageView = findViewById(R.id.groupsBtnIcon)
            icogroupsBtn.setImageResource(R.drawable.ic_groups_outline_active)
            val groupTxt: TextView = findViewById(R.id.groupsBtnText)
            groupTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))
            val mainBtn: LinearLayout = findViewById(R.id.homeBtn)
            mainBtn.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }

            // Навигационные кнопки
            val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)
            btnGoBack.setOnClickListener {
                onBackPressed()
                overridePendingTransition(0, 0)
            }
            val cancelBtn: AppCompatButton = findViewById(R.id.CancelBtn)
            cancelBtn.setOnClickListener {
                onBackPressed()
                overridePendingTransition(0, 0)
            }

            // Инициализация элементов
            searchEditText = findViewById(R.id.membersSearch)
            mainTitle = findViewById(R.id.mainTitle)

            // Установка слушателя для изменения текста
            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s != null) {
                        searchUserByName(s.toString())
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

        private fun searchUserByName(name: String) {
            val query = database.orderByChild("name").startAt(name).endAt(name + "\uf8ff")

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (userSnapshot in dataSnapshot.children) {
                            val userName = userSnapshot.child("name").getValue(String::class.java)
                            if (!userName.isNullOrEmpty()) {
                                mainTitle.text = userName // Измените текст заголовка на имя пользователя
                                return
                            }
                        }
                    } else {
                        mainTitle.text = "Выбор участников" // Вернуть заголовок, если никого не нашли
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("FirebaseError", "Error occurred: " + databaseError.message)
                }
            })
        }
    }
