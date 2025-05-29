package com.example.bankapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_password_reset)

        auth = FirebaseAuth.getInstance()
        emailEditText = findViewById(R.id.eMail)

        val continueButton: Button = findViewById(R.id.btnContinue)
        continueButton.setOnClickListener { sendPasswordResetEmail() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnGoBack).setOnClickListener {
            overridePendingTransition(0, 0)
            finish()
        }

        val Txtmain: TextView = findViewById(R.id.mainTitle)
        Txtmain.text = "Изменение пароля"
    }

    private fun sendPasswordResetEmail() {
        val email = emailEditText.text.toString().trim()
        if (email.isEmpty()) {
            Toast.makeText(this, "Введите ваш E-Mail", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Проверьте вашу почту для сброса пароля", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("ResetPassword", "Ошибка отправки email: ${task.exception?.message}")
                    Toast.makeText(this, "Ошибка: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
