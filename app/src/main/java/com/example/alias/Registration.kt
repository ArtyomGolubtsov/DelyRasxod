package com.example.alias

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Registration : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registration)

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Установка цветов для статус-бара и нижней панели
        window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
        window.statusBarColor = ContextCompat.getColor(this, R.color.custom_color)

        val mainWindow: ImageButton = findViewById(R.id.mainWindow)
        val setting: ImageButton = findViewById(R.id.setting)
        val button32: Button = findViewById(R.id.dddddddddddddddd)

        // Обработчик нажатия на кнопку "Главное окно"
        mainWindow.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        // Обработчик нажатия на кнопку "Настройки"
        setting.setOnClickListener {
            val intent = Intent(this, Activity2::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        // Обработчик нажатия на кнопку выхода
        button32.setOnClickListener {
            logoutUser()
        }
    }

    // Метод для выхода из аккаунта
    private fun logoutUser() {
        auth.signOut()  // Теперь это безопасно, так как auth инициализирован
        Toast.makeText(this, "Вы вышли из аккаунта!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java) // Измените на нужный вам экран
        startActivity(intent)
        finish()
    }
}
