package com.example.bankapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import java.util.concurrent.Executor

class EntryPINActivity : AppCompatActivity() {

    private lateinit var pinDigits: MutableList<TextView>
    private var currentIndex = 0
    private var pinCode = "" // Переменная для хранения введенного PIN-кода
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var userId: String // Переменная для хранения идентификатора пользователя

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("EntryPINActivity", "onCreate: Activity started")
        setContentView(R.layout.activity_create_pin)

        initializeUI()
        setupBiometricPrompt()
        setupPINInput()

        // Получение идентификатора пользователя
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid ?: "unknown" // Получите текущий идентификатор пользователя
    }

    private fun initializeUI() {
        // Настройка Firebase и UI компонентов
        database = FirebaseDatabase.getInstance().getReference("Users")
        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)

        // Установка заголовка действия
        val enterPassword: TextView = findViewById(R.id.actionTitle)
        enterPassword.text = "Введите PIN-код!"

        // Инициализация полей ввода PIN-кода
        pinDigits = mutableListOf(
            findViewById(R.id.PINDigit1),
            findViewById(R.id.PINDigit2),
            findViewById(R.id.PINDigit3),
            findViewById(R.id.PINDigit4)
        )

        // Настройка кнопки удаления
        val deleteBtn: ImageButton = findViewById(R.id.btn_delete)
        deleteBtn.setOnClickListener { onDeleteButtonClicked() }
    }

    private fun setupPINInput() {
        // Установка onClickListeners для кнопок чисел
        for (i in 0..9) {
            val buttonId = resources.getIdentifier("btn$i", "id", packageName)
            val button: AppCompatButton = findViewById(buttonId)
            button.setOnClickListener { onPinDigitButtonClicked(i) }
        }
    }

    private fun onPinDigitButtonClicked(digit: Int) {
        if (currentIndex < pinDigits.size) {
            pinCode += digit.toString() // Добавление цифры к строке PIN-кода
            pinDigits[currentIndex].text = digit.toString()
            currentIndex++

            // Проверка PIN-кода, когда код полностью введен
            if (currentIndex == pinDigits.size) {
                verifyPinCode()
            }
        }
    }

    private fun onDeleteButtonClicked() {
        if (currentIndex > 0) {
            currentIndex--
            pinCode = pinCode.dropLast(1) // Удаление последней цифры
            pinDigits[currentIndex].text = ""
        }
    }

    private fun setupBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                showToast("Ошибка аутентификации: $errString")
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                showToast("Ошибка: не распознан отпечаток")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Получение PIN-кода из базы данных и автоматический переход на MainActivity
                database.child(userId).child("pin").get().addOnSuccessListener { dataSnapshot ->
                    val savedPin = dataSnapshot.getValue(String::class.java)

                    // Проверяем, что PIN-код успешно получен
                    if (savedPin != null) {
                        pinCode = savedPin // Устанавливаем значение кода PIN
                        navigateToMainActivity() // Переход на главную страницу
                    } else {
                        showToast("Ошибка: PIN-код не найден")
                    }
                }.addOnFailureListener {
                    showToast("Ошибка при получении PIN-кода из базы данных.")
                }
            }
        })

        // Создание информации о запросе для биометрической аутентификации
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Аутентификация через отпечаток")
            .setSubtitle("Используйте отпечаток пальца для входа")
            .setDeviceCredentialAllowed(true) // Разрешить использование PIN-кода, если отпечаток не распознан
            .build()

        // Проверка доступности биометрии и запуск аутентификации
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                showToast("Устройство не поддерживает биометрию")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                showToast("Биометрия недоступна")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                showToast("Не настроены данные для биометрии")
            }
            else -> {
                // Автоматически начать аутентификацию по отпечатку пальца, если доступно
                biometricPrompt.authenticate(promptInfo)
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun verifyPinCode() {
        // Получение PIN-кода из базы данных
        database.child(userId).child("pin").get().addOnSuccessListener { dataSnapshot ->
            val savedPin = dataSnapshot.getValue(String::class.java) // Получение сохраненного PIN-кода

            if (savedPin != null && pinCode == savedPin) { // Проверка, совпадает ли введенный PIN-код с сохраненным
                showToast("PIN-код верный!")
                // Переход к следующей активности (например, главный экран банка)
                navigateToMainActivity()
            } else {
                showToast("Неверный PIN-код!")
                resetInput()
            }
        }.addOnFailureListener {
            showToast("Ошибка при получении PIN-кода из базы данных.")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun resetInput() {
        pinCode = "" // Очистка PIN-кода
        currentIndex = 0 // Сброс индекса
        pinDigits.forEach { it.text = "" } // Очистка полей ввода PIN
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Опционально закройте текущую активность
    }
}
