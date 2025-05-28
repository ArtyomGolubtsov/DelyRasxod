package com.example.bankapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
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
import androidx.core.content.ContextCompat
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

class PersonalUserInfoActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage

    private lateinit var userName: TextView
    private lateinit var editUserName: EditText
    private lateinit var editUserPhone: EditText
    private lateinit var userProfileImage: ImageView
    private lateinit var spinnerDay: Spinner
    private lateinit var spinnerMonth: Spinner
    private lateinit var spinnerYear: Spinner

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
        setContentView(R.layout.activity_personal_user_info)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()


        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)
        // Инициализация элементов
        userName = findViewById(R.id.userName)
        editUserName = findViewById(R.id.editUserName)
        editUserPhone = findViewById(R.id.editUserPhone)
        userProfileImage = findViewById(R.id.userProfileImage)
        spinnerDay = findViewById(R.id.spinner1)
        spinnerMonth = findViewById(R.id.spinner2)
        spinnerYear = findViewById(R.id.spinner3)

        // Настройка навигации
        downmenu()

        // Загрузка данных пользователя
        loadUserData()

        // Обработчик кнопки "Назад"
        findViewById<ImageButton>(R.id.btnGoBack).setOnClickListener {
            finish()
        }

        // Обработчик кнопки сохранения
        findViewById<ImageButton>(R.id.editProfileBtn).setOnClickListener {
            saveUserData()
        }

        // Обработчик клика на аватар
        userProfileImage.setOnClickListener {
            openGallery()
        }

        // Настройка спиннеров для даты рождения
        setupDateSpinners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            database.child("Users").child(user.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Загрузка основных данных
                    snapshot.child("name").getValue(String::class.java)?.let {
                        userName.text = it
                        editUserName.setText(it)
                    }

                    snapshot.child("phone").getValue(String::class.java)?.let {
                        editUserPhone.setText(it)
                    }

                    // Загрузка аватарки
                    snapshot.child("UserPhoto").getValue(String::class.java)?.let { photoUrl ->
                        Glide.with(this@PersonalUserInfoActivity)
                            .load(photoUrl)
                            .placeholder(R.drawable.placeholder)
                            .into(userProfileImage)
                    }

                    // Загрузка даты рождения
                    snapshot.child("BirthDay").getValue(String::class.java)?.let { birthDate ->
                        val parts = birthDate.split("-")
                        if (parts.size == 3) {
                            spinnerDay.setSelection(parts[0].toInt() - 1)
                            spinnerMonth.setSelection(parts[1].toInt() - 1)
                            spinnerYear.setSelection(getYearPosition(parts[2]))
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PersonalUserInfoActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun setupDateSpinners() {
        // Дни (1-31)
        val days = (1..31).map { it.toString() }
        spinnerDay.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days)

        // Месяцы
        val months = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
        spinnerMonth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)

        // Годы (1900-текущий год)
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val years = (1900..currentYear).map { it.toString() }.reversed()
        spinnerYear.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
    }

    private fun getYearPosition(year: String): Int {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return currentYear - year.toInt()
    }

    private fun saveUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val updates = hashMapOf<String, Any>(
                "name" to editUserName.text.toString(),
                "phone" to editUserPhone.text.toString(),
                "BirthDay" to "${spinnerDay.selectedItem}-${spinnerMonth.selectedItemPosition + 1}-${spinnerYear.selectedItem}"
            )

            database.child("Users").child(user.uid).updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
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
                    .placeholder(R.drawable.placeholder)
                    .into(userProfileImage)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка сохранения фото", Toast.LENGTH_SHORT).show()
            }
    }

    fun downmenu() {
        val ptofilico: ImageView = findViewById(R.id.profileBtnIcon)
        val profilTxtBtn: TextView = findViewById(R.id.profileBtnText)
        profilTxtBtn.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))
        ptofilico.setImageResource(R.drawable.ic_person_outline_active)

        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)
        val mainBtn: LinearLayout = findViewById(R.id.homeBtn)
        mainBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            mainBtn.startAnimation(clickAnimation)
        }

        val groupsBtn: LinearLayout = findViewById(R.id.groupsBtn)
        groupsBtn.setOnClickListener {
            startActivity(Intent(this, GroupsActivity::class.java))
            groupsBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        val contactsBtn: LinearLayout = findViewById(R.id.contactsBtn)
        contactsBtn.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
            contactsBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        val proflBtn: LinearLayout = findViewById(R.id.profileBtn)
        proflBtn.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
            proflBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }
    }
}