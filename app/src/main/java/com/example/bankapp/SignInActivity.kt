package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth // Переменная для работы с Firebase Auth

    private lateinit var forgotPassBtn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        // Установка отступов для системы
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Обработка кнопок и нажатий
        val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)
        btnGoBack.setOnClickListener {
            val intent = Intent(this, GreatingActivity::class.java)
            startActivity(intent)
        }

        val registrBtn: TextView = findViewById(R.id.signUpLink)
        registrBtn.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        forgotPassBtn = findViewById(R.id.forgotPassBtn)
        forgotPassBtn.setOnClickListener {
            val intent = Intent(this, PasswordResetActivity::class.java)
            startActivity(intent)
            finish()
        }

        val signInButton: AppCompatButton = findViewById(R.id.btnSignIn)
        signInButton.setOnClickListener {
            val emailEditText: EditText = findViewById(R.id.eMail)
            val passwordEditText: EditText = findViewById(R.id.pass)
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            signInUser(email, password) // Вызов функции для аутентификации пользователя
        }
    }

    // Функция для аутентификации пользователя
    private fun signInUser(email: String, password: String) {
        // Проверка на пустые поля
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите данные", Toast.LENGTH_SHORT).show()
            return
        }

        // Вызов метода signInWithEmailAndPassword для аутентификации
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Успешный вход в систему
                    Toast.makeText(this, "Вы успешно вошли в аккаунт!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, CreatePINActivity::class.java) // Переход на основной экран
                    startActivity(intent)
                    finish() // Закрыть текущую активность
                } else {
                    Toast.makeText(this, "Ошибка входа: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
