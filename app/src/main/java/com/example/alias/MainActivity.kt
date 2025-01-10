package com.example.alias

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.content.Intent
import androidx.core.content.ContextCompat
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var textView3: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private var count: Int = 0
    private lateinit var auth: FirebaseAuth
    private val COUNT_KEY = "count_key"
    private val TAG = "MainActivity"

    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Alias)
        setContentView(R.layout.activity_main)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
        window.statusBarColor = ContextCompat.getColor(this, R.color.custom_color)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        // Загрузка сохраненного значения счетчика из SharedPreferences
        count = sharedPreferences.getInt(COUNT_KEY, 0)
        // Инициализация Firebase Realtime Database
        database = FirebaseDatabase.getInstance()

        auth = FirebaseAuth.getInstance()

        // Инициализация текстового поля и кнопки
        textView = findViewById(R.id.textView)
        textView3 = findViewById(R.id.textView3)
        val button: Button = findViewById(R.id.button)
        val imageButton: ImageButton = findViewById(R.id.imageButton)
        val imageButton4: ImageButton = findViewById(R.id.imageButton4)
        val imageButton3: ImageButton = findViewById(R.id.imageButton3)

        // Проверяем, есть ли текущий пользователь
        val currentUser = auth.currentUser
        if (currentUser == null) {
            textView3.text = "Учтите, пока вы не зарегистрируетесь ваши данные не сохранятся"
            textView3.textSize = 20f
        }
        button.setBackgroundColor(ContextCompat.getColor(this, R.color.button_color))

        // Проверка и сброс счетчика, если изменилась дата
        count = checkAndResetCounter(this)

        textView.text = count.toString()

        button.setOnClickListener {
            count++
            textView.text = count.toString()

            // Сохраняем значение счетчика в Firebase, если пользователь авторизован
            val currentUser = auth.currentUser
            if (currentUser != null) {
                saveCounterToDatabase(currentUser.uid)
            }

            // Меняем текст и цвет кнопки в зависимости от значения счетчика
            when {
                count == 50 -> textView.text = "вау"
                count > 250 -> button.setBackgroundColor(Color.RED)
                count > 100 -> {
                    textView3.text = "Девять минут ты потратил на тяжки оно тебе надо?"
                    textView3.textSize = 24f
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.button_color_yellow))
                }
                else -> button.setBackgroundColor(ContextCompat.getColor(this, R.color.button_color))
            }
        }

        imageButton.setOnClickListener {
            textView.text = "xD"
        }

        imageButton4.setOnClickListener {
            val intent = Intent(this, Activity2::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        imageButton3.setOnClickListener {
            if (currentUser != null) {
                // Если пользователь зарегистрирован, перенаправьте его на страницу регистрации
                val intent = Intent(this, Registration::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            } else {
                // Если пользователь не зарегистрирован, перенаправьте его на экран входа
                val intent = Intent(this, vxod::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }
        }

        // Сохраняем значение счетчика в базе данных, если пользователь зарегистрирован
        if (currentUser != null) {
            saveCounterToDatabase(currentUser.uid)
        }
    }

    private fun saveCounterToDatabase(userId: String) {
        val currentDate = Calendar.getInstance()
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH) + 1 // Месяцы начинаются с 0
        val day = currentDate.get(Calendar.DAY_OF_MONTH) - 1

        val counterData = mapOf("count" to count)

        // Путь к данным в Realtime Database
        val userRef = database.getReference("Users").child(userId)
        val yearRef = userRef.child("years").child(year.toString())
        val monthRef = yearRef.child("months").child(month.toString())
        val dayRef = monthRef.child("days").child(day.toString())

        // Запись данных в Realtime Database
        dayRef.setValue(counterData)
            .addOnSuccessListener { Log.d(TAG, "Counter saved successfully") }
            .addOnFailureListener { e -> Log.w(TAG, "Error saving counter", e) }
    }

    private fun getCurrentDay(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    }

    private fun saveCurrentDay(context: Context) {
        val sharedPrefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putInt("current_day", getCurrentDay())
        editor.apply()
    }

    private fun checkAndResetCounter(context: Context): Int {
        val sharedPrefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val savedDay = sharedPrefs.getInt("current_day", -1)
        val currentDay = getCurrentDay()

        // Если день изменился
        if (savedDay != currentDay) {
            // Сначала сохраняем текущий счетчик в базу данных
            val currentUser = auth.currentUser
            if (currentUser != null) {
                saveCounterToDatabase(currentUser.uid) // Сохраняем данные перед сбросом
            }

            // Сбрасываем счетчик до нуля
            resetCounter()

            // Сохраняем новый день
            saveCurrentDay(context)
            return 0 // Возвращаем сброшенный счетчик
        }

        return count // Возвращаем текущее значение счетчика
    }

    private fun resetCounter() {
        count = 0
        textView.text = count.toString()
        saveCount()
    }

    private fun saveCount() {
        val editor = sharedPreferences.edit()
        editor.putInt(COUNT_KEY, count)
        editor.apply()
    }

    override fun onPause() {
        super.onPause()
        saveCount() // Сохранение значения счетчика при уходе из активности
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
