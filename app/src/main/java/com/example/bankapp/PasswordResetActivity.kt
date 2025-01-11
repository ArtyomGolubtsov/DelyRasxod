package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.graphics.toColor
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bankapp.GreatingActivity
import com.example.bankapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class PasswordResetActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var btnContinue: AppCompatButton
    private lateinit var emailEditText: EditText
    private lateinit var userNameEditText: EditText // Поле для ввода ФИО

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_password_reset)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Инициализация UI-элементов
        userNameEditText = findViewById(R.id.userName) // Поле для ввода ФИО
        emailEditText = findViewById(R.id.eMail) // Замените на актуальное id для поля email
        btnContinue = findViewById(R.id.btnContinue)

        // Обработка кнопок и нажатий
        val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)
        btnGoBack.setOnClickListener {
            val intent = Intent(this, GreatingActivity::class.java)
            startActivity(intent)
        }

        btnContinue.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val fullName = userNameEditText.text.toString().trim() // Получаем введенные ФИО
            if (email.isNotEmpty() && fullName.isNotEmpty()) {
                verifyUserNameAndSendEmail(email, fullName)
            } else {
                Toast.makeText(this, "Пожалуйста, введите email и ФИО", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyUserNameAndSendEmail(email: String, fullName: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Получаем ссылку на базу данных
            val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId)

            database.child("name").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val storedName = snapshot.getValue(String::class.java)
                    if (storedName != null && storedName == fullName) {
                        // Если ФИО совпадает, отправляем ссылку для сброса пароля
                        sendPasswordResetEmail(email)
                    } else {
                        Toast.makeText(this@PasswordResetActivity, "ФИО не совпадает с записями", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PasswordResetActivity, "Ошибка доступа к базе данных", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Ссылка для сброса пароля отправлена на $email", Toast.LENGTH_SHORT).show()
                    finish() // Закрыть текущий экран
                } else {
                    Toast.makeText(this, "Ошибка: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
