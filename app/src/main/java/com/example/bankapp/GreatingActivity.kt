package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class GreatingActivity : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()

        database = FirebaseDatabase.getInstance() // Получаем экземпляр FirebaseDatabase
        usersReference = database.getReference("Users") // Получаем ссылку на "Users"



        setContentView(R.layout.activity_greating)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnSignUp = findViewById<AppCompatButton>(R.id.btnSignUp)
        val btnSignIn = findViewById<AppCompatButton>(R.id.btnSignIn)

        btnSignUp.setOnClickListener {
            val intent = Intent(this@GreatingActivity, SignUpActivity::class.java)
            startActivity(intent)
        }

        btnSignIn.setOnClickListener {
            val intent = Intent(this@GreatingActivity, SignInActivity::class.java)
            startActivity(intent)
        }
    }
}