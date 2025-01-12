package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Привязка элементов интерфейса
        val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)
        val signUpButton: androidx.appcompat.widget.AppCompatButton = findViewById(R.id.btnSignUp)
        val emailEditText: EditText = findViewById(R.id.eMail)
        val passwordEditText: EditText = findViewById(R.id.pass)
        val userNameEditText: EditText = findViewById(R.id.userName) // Поле для имени
        val agreeCheckbox: CheckBox = findViewById(R.id.iAgreeCheckbox)
        val SignIN: TextView = findViewById(R.id.signUpLink)

        // Обработка кнопки возврата
        btnGoBack.setOnClickListener {
            val intent = Intent(this, GreatingActivity::class.java)
            startActivity(intent)
        }
        SignIN.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        // Регистрация пользователя
        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val name = userNameEditText.text.toString() // Получаем имя из поля ввода

            if (agreeCheckbox.isChecked) {
                registerUser(email, password, name) // Передаем имя в метод регистрации
            } else {
                Toast.makeText(this, "Пожалуйста, примите политику конфиденциальности", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(email: String, password: String, name: String) {
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) { // Проверка имени
            Toast.makeText(this, "Пожалуйста, введите данные", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    userId?.let {
                        // Создание нового пользователя в Realtime Database
                        val user = User(email, name) // Передаем имя в объект пользователя
                        database.child(it).setValue(user)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    // Успешная регистрация
                                    Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                                    // Переход на главный экран
                                    val intent = Intent(this, CreatePINActivity::class.java) // Замените на ваш основной экран
                                    startActivity(intent)
                                    finish()
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

    data class User(val email: String, val name: String) // Добавлено поле для имени
}
