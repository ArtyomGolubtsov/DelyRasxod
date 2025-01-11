package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PasswordResetActivity : AppCompatActivity() {

    private lateinit var btnContinue: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_password_reset)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Обработка кнопок и нажатий------------------------------------------------
        val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)
        btnGoBack.setOnClickListener {
            val intent = Intent(this, GreatingActivity::class.java)
            startActivity(intent)
        }
        btnContinue = findViewById(R.id.btnContinue)
        btnContinue.setOnClickListener{
            val intent = Intent(this@PasswordResetActivity, CodeConfirmActivity::class.java)
            startActivity(intent)
        }
        //............................................................................
    }
}