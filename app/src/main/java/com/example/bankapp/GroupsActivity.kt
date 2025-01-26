package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// Данные для групп
data class GroupItem(
    val name: String,
    val categories: String,
    val imageUrl: String
)

// Адаптер для отображения групп в RecyclerView
class GroupAdapter(private var groupList: List<GroupItem>) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val groupName: TextView = view.findViewById(R.id.activityName)
        val groupCategory: TextView = view.findViewById(R.id.activityCategory)
        val groupImage: ImageView = view.findViewById(R.id.activityItemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_item, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val currentItem = groupList[position]
        holder.groupName.text = currentItem.name
        holder.groupCategory.text = currentItem.categories

        // Загрузка изображения через Glide
        Glide.with(holder.itemView.context)
            .load(currentItem.imageUrl)
            .placeholder(R.drawable.placeholder)
            .into(holder.groupImage)
    }

    override fun getItemCount() = groupList.size

    // Обновление списка групп
    fun updateList(newList: List<GroupItem>) {
        groupList = newList
        notifyDataSetChanged()
    }
}

// ViewModel для групп
class GroupViewModel : ViewModel() {
    private val _groupList = MutableLiveData<MutableList<GroupItem>>()
    val groupList: LiveData<MutableList<GroupItem>> = _groupList
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("Users")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Загрузка групп пользователя из Firebase
    fun loadUserGroups() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.child(currentUser.uid).child("Groups").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val groups = mutableListOf<GroupItem>()
                    for (groupSnapshot in snapshot.children) {
                        val groupName = groupSnapshot.child("title").getValue(String::class.java) ?: "Без названия"
                        val categories = groupSnapshot.child("categories").children.joinToString(", ") { it.getValue(String::class.java) ?: "" }
                        val imageUrl = groupSnapshot.child("imageUri").getValue(String::class.java) ?: ""

                        groups.add(GroupItem(groupName, categories, imageUrl))
                    }
                    _groupList.value = groups // Обновляем LiveData
                }

                override fun onCancelled(error: DatabaseError) {
                    // Обработать ошибку загрузки
                }
            })
        }
    }
}

// Основная активность для групп
class GroupsActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: GroupAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var noGroupsBox: ConstraintLayout
    private lateinit var viewModel: GroupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_groups)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)

        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)
        val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)

        btnGoBack.setOnClickListener {
            onBackPressed()
            finish()
            btnGoBack.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        // Инициализация RecyclerView
        recyclerView = findViewById(R.id.groupsList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Инициализация списка групп
        adapter = GroupAdapter(mutableListOf())
        recyclerView.adapter = adapter

        // Инициализация View для показа отсутствия групп
        noGroupsBox = findViewById(R.id.noGroupsBox)
        noGroupsBox.visibility = View.GONE

        // Инициализация Firebase Auth и Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Инициализация ViewModel
        viewModel = ViewModelProvider(this).get(GroupViewModel::class.java)
        viewModel.loadUserGroups()

        // Наблюдение за изменениями в списке групп
        viewModel.groupList.observe(this, Observer { groupList ->
            adapter.updateList(groupList)
            updateVisibility(groupList)
        })

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
        }

        // Кнопка для создания новых групп
        val createGroupsBtn: Button = findViewById(R.id.addGroupBtn)
        createGroupsBtn.setOnClickListener {
            createGroupsBtn.startAnimation(clickAnimation)
            val intent = Intent(this, NewGroupActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Использование без ресурсов
        val spacingInPixels = 16 // Укажите нужное значение в пикселях
        recyclerView.addItemDecoration(SpaceItemDecoration(spacingInPixels))

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Обновление видимости списка и блока отсутствия групп
    private fun updateVisibility(groupList: List<GroupItem>) {

        if (groupList.isNotEmpty()) {
            noGroupsBox.visibility = View.GONE // Скрываем блок для отсутствия групп
            recyclerView.visibility = View.VISIBLE // Показываем список групп
        } else {
            noGroupsBox.visibility = View.VISIBLE // Показываем блок, если групп нет
            recyclerView.visibility = View.GONE // Скрываем список групп
        }
    }
}
