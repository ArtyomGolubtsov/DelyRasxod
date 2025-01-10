package com.example.alias

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.os.Handler
import android.widget.ProgressBar
import com.google.firebase.auth.FirebaseAuth

class Activity2 : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar // Объявляем прогресс-бар
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_2)
        getWindow().setNavigationBarColor(getResources().getColor(R.color.white));

        // Инициализация элементов
        val mainWindow: ImageButton = findViewById(R.id.mainWindow)
        val registr: ImageButton = findViewById(R.id.imageButton9)

        // Обработчик нажатия для главного окна
        mainWindow.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        registr.setOnClickListener {
            if (currentUser != null) {
                // Если пользователь зарегистрирован, перенаправьте его на страницу регистрации
                val intent = Intent(this, Registration::class.java)
                startActivity(intent)
            } else {
                // Если пользователь не зарегистрирован, перенаправьте его на экран входа
                val intent = Intent(this, vxod::class.java)
                startActivity(intent)
            }

            overridePendingTransition(0, 0)
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
