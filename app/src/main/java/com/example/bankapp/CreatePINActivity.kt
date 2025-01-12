package com.example.bankapp

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth

class CreatePINActivity : AppCompatActivity() {
    private lateinit var pinDigits: MutableList<TextView>
    private var currentIndex: Int = 0
    private var pinCode: String = "" // Переменная для хранения введенного PIN-кода
    private var confirmPinCode: String = "" // Переменная для хранения второго введенного PIN-кода
    private var isConfirming: Boolean = false // Флаг, указывающий, находимся ли мы в режиме подтверждения
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_pin)
        database = FirebaseDatabase.getInstance().getReference("Users")
        auth = FirebaseAuth.getInstance()
        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)

        val InserPassword: TextView = findViewById(R.id.actionTitle)
        InserPassword.text = "Придумайте пароль!"

        // Инициализация полей для ввода PIN-кода
        pinDigits = mutableListOf(
            findViewById(R.id.PINDigit1),
            findViewById(R.id.PINDigit2),
            findViewById(R.id.PINDigit3),
            findViewById(R.id.PINDigit4)
        )

        // Инициализация кнопок
        val DltBtn: ImageButton = findViewById(R.id.btn_delete)

        // Установка обработчиков для кнопок чисел
        for (i in 0..9) {
            val buttonId = resources.getIdentifier("btn$i", "id", packageName)
            val button: AppCompatButton = findViewById(buttonId)
            button.setOnClickListener {
                if (currentIndex < pinDigits.size) {
                    if (isConfirming) {
                        confirmPinCode += i.toString() // Добавляем цифру к строке второго PIN-кода
                    } else {
                        pinCode += i.toString() // Добавляем цифру к строке первого PIN-кода
                    }
                    pinDigits[currentIndex].text = i.toString()
                    currentIndex++
                }

                // Если PIN-код введен полностью
                if (currentIndex == pinDigits.size) {
                    if (isConfirming) {
                        onConfirmPinCodeEntered(InserPassword)
                    } else {
                        prepareForConfirmation(InserPassword)
                    }
                }
            }
        }

        // Установка обработчика для кнопки удаления
        DltBtn.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                if (isConfirming) {
                    confirmPinCode = confirmPinCode.dropLast(1) // Удаляем последнюю цифру из второго PIN-кода
                } else {
                    pinCode = pinCode.dropLast(1) // Удаляем последнюю цифру из первого PIN-кода
                }
                pinDigits[currentIndex].text = ""
            }
        }
    }

    private fun prepareForConfirmation(InserPassword: TextView) {
        InserPassword.text = "Повторите пароль!" // Уводим пользователя, что нужно повторить ввод
        currentIndex = 0 // Сбрасываем индекс
        confirmPinCode = "" // Очищаем строку для второго PIN-кода
        isConfirming = true // Устанавливаем режим подтверждения
        pinDigits.forEach { it.text = "" } // Очищаем текстовые поля
    }

    private fun onConfirmPinCodeEntered(InserPassword: TextView) {
        // Здесь вы можете использовать переменную pinCode и confirmPinCode
        if (pinCode == confirmPinCode) {
            Toast.makeText(this, "Пароль успешно установлен!", Toast.LENGTH_SHORT).show()
            // Запись PIN-кода в Firebase
            val userId = auth.currentUser?.uid
            if (userId != null) {
                // Запись PIN-кода в Firebase
                database.child(userId).child("pin").setValue(pinCode)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            println("PIN-код успешно сохранен в базу данных!")
                            // Здесь можете перейти на следующий экран или выполнить другие действия
                        } else {
                            println("Ошибка сохранения PIN-кода: ${task.exception}")
                            Toast.makeText(this, "Ошибка сохранения PIN-кода.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Ошибка: пользователь не аутентифицирован.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Пароли не совпадают! Попробуйте еще раз.", Toast.LENGTH_SHORT).show()
            resetInput(InserPassword) // Сбрасываем введенные значения

        }
    }

    private fun resetInput(InserPassword: TextView) {
        pinCode = "" // Очищаем первую строку
        confirmPinCode = "" // Очищаем вторую строку
        currentIndex = 0 // Сбрасываем индекс
        isConfirming = false // Устанавливаем режим ввода пароля
        pinDigits.forEach { it.text = "" } // Очищаем текстовые поля
        InserPassword.text = "Введите пароль заново!" // Уводим пользователя, что нужно повторить ввод
    }
}
