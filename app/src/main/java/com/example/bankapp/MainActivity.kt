package com.example.bankapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class ActivityItem(
    val name: String,
    val category: String,
    val imageResId: Int
)

class ActivityAdapter(private val activityList: List<ActivityItem>) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val activityName: TextView = view.findViewById(R.id.activityName)
        val activityCategory: TextView = view.findViewById(R.id.activityCategory)
        val activityImage: ImageView = view.findViewById(R.id.activityItemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_item, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val currentItem = activityList[position]
        holder.activityName.text = currentItem.name
        holder.activityCategory.text = currentItem.category
        holder.activityImage.setImageResource(currentItem.imageResId)
    }

    override fun getItemCount() = activityList.size
}

class MainActivity : AppCompatActivity() {
    private lateinit var userNameTextView: TextView
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.activityList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Пример списка активностей
        val activityList = listOf(
            ActivityItem("Активность 1", "Категория 1", R.drawable.placeholder),
            ActivityItem("Активность 2", "Категория 2", R.drawable.placeholder),
            ActivityItem("Активность 3", "Категория 3", R.drawable.placeholder)
        )

        // Установка адаптера
        val adapter = ActivityAdapter(activityList)
        recyclerView.adapter = adapter

        // Инициализация Firebase Auth и Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Инициализация TextView для имени
        userNameTextView = findViewById(R.id.userNameTextView)

        // Получение текущего пользователя
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadUserName(currentUser.uid)
        }
    }

    private fun loadUserName(userId: String) {
        database.child(userId).child("name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userName = snapshot.getValue(String::class.java)
                    userNameTextView.text = userName
                } else {
                    userNameTextView.text = "Имя не найдено"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                userNameTextView.text = "Ошибка загрузки имени"
            }
        })
    }
}
