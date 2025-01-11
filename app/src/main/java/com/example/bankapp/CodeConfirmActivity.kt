package com.example.bankapp

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CodeConfirmActivity : AppCompatActivity() {

    private lateinit var codeDigit1: EditText
    private lateinit var codeDigit2: EditText
    private lateinit var codeDigit3: EditText
    private lateinit var codeDigit4: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_code_confirm)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        codeDigit1 = findViewById(R.id.codeDigit1)
        codeDigit2 = findViewById(R.id.codeDigit2)
        codeDigit3 = findViewById(R.id.codeDigit3)
        codeDigit4 = findViewById(R.id.codeDigit4)

        codeDigit1.setOnKeyListener(getOnKeyListener(codeDigit2))
        codeDigit2.setOnKeyListener(getOnKeyListener(codeDigit3))
        codeDigit3.setOnKeyListener(getOnKeyListener(codeDigit4))
        codeDigit4.setOnKeyListener(getOnKeyListener(null))

        setFocusChangeListener(codeDigit1, codeDigit2)
        setFocusChangeListener(codeDigit2, codeDigit3)
        setFocusChangeListener(codeDigit3, codeDigit4)
    }

    private fun getOnKeyListener(nextEditText: EditText?): View.OnKeyListener {
        return View.OnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                // Игнорируем клавиши ENTER и DELETE
                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DEL) {
                    return@OnKeyListener false
                }

                // Если нажата любая другая клавиша, вводим символ
                val editText = v as EditText
                if (event.isPrintingKey()) {
                    editText.append(event.getDisplayLabel().toString())

                    // Переключаемся на следующий EditText
                    nextEditText?.requestFocus()
                    return@OnKeyListener true // Предотвращаем дальнейшую обработку
                }
            }
            false
        }
    }

    private fun setFocusChangeListener(currentEditText: EditText, nextEditText: EditText) {
        currentEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && currentEditText.text.length == 1 && nextEditText.hasFocus().not()) {
                nextEditText.requestFocus()
            }
            currentEditText.setBackgroundResource(
                if (hasFocus) R.drawable.input_pin_digit_bg_focus
                else R.drawable.input_pin_digit_bg
            )
        }
    }
}