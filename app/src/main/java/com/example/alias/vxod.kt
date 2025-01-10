package com.example.alias

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class vxod : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.unreg)

        window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
        window.statusBarColor = ContextCompat.getColor(this, R.color.custom_color)

        // Привязка элементов интерфейса
        val mainWindow: ImageButton = findViewById(R.id.mainWindow)
        val setting: ImageButton = findViewById(R.id.setting)
        val button2: Button = findViewById(R.id.button2)
        val button3: Button = findViewById(R.id.button3)
        val emailEditText: EditText = findViewById(R.id.email)
        val passwordEditText: EditText = findViewById(R.id.password)

        button2.setBackgroundColor(ContextCompat.getColor(this, R.color.button_color))
        button3.setBackgroundColor(ContextCompat.getColor(this, R.color.button_color))

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Кнопка регистрации
        button3.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            registerUser(email, password)
        }

        // Кнопка входа в аккаунт
        button2.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            loginUser(email, password)
        }

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
    }

    private fun registerUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите данные", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    userId?.let {
                        // Создание нового пользователя в Realtime Database
                        val user = User(email, password) // Сохранение пароля (не рекомендуется)
                        database.child(it).setValue(user)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    // Автоматический вход после регистрации
                                    loginUser(email, password)
                                } else {
                                    Toast.makeText(this, "Ошибка создания профиля в базе данных", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(this, "Ошибка регистрации: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loginUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите данные", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Успешный вход в систему
                    Toast.makeText(this, "Вы успешно вошли в аккаунт!", Toast.LENGTH_SHORT).show()
                    // Переход на главный экран
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Закрыть эту активность, чтобы предотвратить возврат
                } else {
                    Toast.makeText(this, "Ошибка входа: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    data class User(val email: String, val password: String) // Добавление пароля
}
