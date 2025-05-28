package com.example.bankapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class UserProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage

    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private lateinit var userPhone: EditText
    private lateinit var userBank: Spinner
    private lateinit var userPhoto: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadImageToFirebase(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        // Инициализация элементов интерфейса
        userName = findViewById(R.id.userName)
        userEmail = findViewById(R.id.userEmail)
        userPhone = findViewById(R.id.userPhone)
        userBank = findViewById(R.id.userBank)
        userPhoto = findViewById(R.id.userPhoto)

        // Загрузка данных пользователя
        loadUserData()

        // Настройка спиннера банков
        setupBankSpinner()

        // Обработчики кликов
        findViewById<ImageButton>(R.id.editProfileBtn).setOnClickListener {
            startActivity(Intent(this, PersonalUserInfoActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.personalInfoBox).setOnClickListener {
            startActivity(Intent(this, PersonalUserInfoActivity::class.java))
        }

        userPhone.setOnClickListener {
            startActivity(Intent(this, PersonalUserInfoActivity::class.java))
        }

        userPhoto.setOnClickListener {
            openGallery()
        }

        // Обработка нажатия кнопки назад
        findViewById<ImageButton>(R.id.btnGoBack).setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Загрузка имени и почты
            userEmail.text = user.email

            database.child("Users").child(user.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userName.text = snapshot.child("name").getValue(String::class.java) ?: "Имя не указано"
                    userPhone.setText(snapshot.child("phone").getValue(String::class.java) ?: "")

                    // Загрузка фото пользователя
                    snapshot.child("UserPhoto").getValue(String::class.java)?.let { photoUrl ->
                        Glide.with(this@UserProfileActivity)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_person_outline)
                            .into(userPhoto)
                    }

                    // Установка выбранного банка
                    val bank = snapshot.child("bank").getValue(String::class.java)
                    if (!bank.isNullOrEmpty()) {
                        val adapter = userBank.adapter as ArrayAdapter<String>
                        val position = adapter.getPosition(bank)
                        if (position >= 0) {
                            userBank.setSelection(position)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@UserProfileActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun setupBankSpinner() {
        val banks = listOf("Т-Банк", "Сбер", "ЦентрИнвест", "АльфаБанк")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, banks)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        userBank.adapter = adapter

        userBank.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedBank = parent?.getItemAtPosition(position).toString()
                saveBankToDatabase(selectedBank)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun saveBankToDatabase(bank: String) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            database.child("Users").child(user.uid).child("bank").setValue(bank)
                .addOnSuccessListener {
                    // Успешно сохранено
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Ошибка сохранения банка", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun uploadImageToFirebase(uri: Uri) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val storageRef = storage.reference
            val imageRef = storageRef.child("user_photos/${user.uid}.jpg")

            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveUserPhotoUrlToDatabase(user.uid, downloadUri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveUserPhotoUrlToDatabase(userId: String, photoUrl: String) {
        database.child("Users").child(userId).child("UserPhoto").setValue(photoUrl)
            .addOnSuccessListener {
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_person_outline)
                    .into(userPhoto)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка сохранения фото", Toast.LENGTH_SHORT).show()
            }
    }
}