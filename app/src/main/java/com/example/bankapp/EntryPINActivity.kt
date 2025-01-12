package com.example.bankapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class EntryPINActivity : AppCompatActivity() {
    private lateinit var pinDigits: MutableList<TextView>
    private var currentIndex: Int = 0
    private var pinCode: String = "" // Переменная для хранения введенного PIN-кода
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("EntryPINActivity", "onCreate: Activity started")
        setContentView(R.layout.activity_create_pin)
        database = FirebaseDatabase.getInstance().getReference("Users")
        auth = FirebaseAuth.getInstance()
        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)

        val enterPassword: TextView = findViewById(R.id.actionTitle)
        enterPassword.text = "Введите PIN-код!"

        // Инициализация полей для ввода PIN-кода
        pinDigits = mutableListOf(
            findViewById(R.id.PINDigit1),
            findViewById(R.id.PINDigit2),
            findViewById(R.id.PINDigit3),
            findViewById(R.id.PINDigit4)
        )

        // Инициализация кнопок
        val deleteBtn: ImageButton = findViewById(R.id.btn_delete)

        // Установка обработчиков для кнопок чисел
        for (i in 0..9) {
            val buttonId = resources.getIdentifier("btn$i", "id", packageName)
            val button: AppCompatButton = findViewById(buttonId)
            button.setOnClickListener {
                if (currentIndex < pinDigits.size) {
                    pinCode += i.toString() // Добавляем цифру к строке PIN-кода
                    pinDigits[currentIndex].text = i.toString()
                    currentIndex++

                    // Если PIN-код введен полностью
                    if (currentIndex == pinDigits.size) {
                        verifyPinCode(enterPassword)
                    }
                }
            }
        }

        // Установка обработчика для кнопки удаления
        deleteBtn.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                pinCode = pinCode.dropLast(1) // Удаляем последнюю цифру
                pinDigits[currentIndex].text = ""
            }
        }
    }

    private fun verifyPinCode(enterPassword: TextView) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child(userId).child("pin").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val storedPin = snapshot.getValue(String::class.java)

                    if (storedPin != null && storedPin == pinCode) {
                        Toast.makeText(this@EntryPINActivity, "PIN-код верный!", Toast.LENGTH_SHORT).show()
                        // Переход на новое активити (например, MainActivity)
                        startActivity(Intent(this@EntryPINActivity, MainActivity::class.java))
                        finish() // Закрываем текущую активность
                    } else {
                        Toast.makeText(this@EntryPINActivity, "Неверный PIN-код! Попробуйте еще раз.", Toast.LENGTH_SHORT).show()
                        resetInput(enterPassword) // Сбрасываем введенные значения
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EntryPINActivity, "Ошибка доступа к базе данных!", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "Ошибка: пользователь не аутентифицирован.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun resetInput(enterPassword: TextView) {
        pinCode = "" // Очищаем PIN-код
        currentIndex = 0 // Сбрасываем индекс
        pinDigits.forEach { it.text = "" } // Очищаем текстовые поля
        enterPassword.text = "Введите PIN-код заново!" // Уводим пользователя, что нужно повторить ввод
    }
}
