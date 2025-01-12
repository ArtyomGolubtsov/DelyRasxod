package com.example.bankapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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
        val homeButton: ImageButton = findViewById(R.id.HomeBtn)

        // Получите текущие параметры компоновки
        val layoutParams = homeButton.layoutParams
        layoutParams.width = 200 // Установите желаемую ширину в пикселях
        layoutParams.height = 200 // Установите желаемую высоту в пикселях
        homeButton.layoutParams = layoutParams

        // Инициализация Firebase Auth и Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Инициализация TextView для имени
        userNameTextView = findViewById(R.id.userNameTextView)

        // Получение текущего пользователя
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Загрузка имени пользователя
            loadUserName(currentUser.uid)
        }
    }

    private fun loadUserName(userId: String) {
        database.child(userId).child("name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userName = snapshot.getValue(String::class.java)
                    // Установка имени в TextView
                    userNameTextView.text = userName
                } else {
                    userNameTextView.text = "Имя не найдено"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Обработка ошибок
                userNameTextView.text = "Ошибка загрузки имени"
            }
        })
    }
}
