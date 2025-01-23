package com.example.bankapp

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.InputStream

data class ActivityItem(
    val name: String,
    val category: String,
    val imageResId: Int
)

class SpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.apply {
            left = space
            right = space
            top = space
            bottom = space
        }
    }
}

class ActivityAdapter(private val activityList: List<ActivityItem>) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val activityName: TextView = view.findViewById(R.id.activityName)
        val activityCategory: TextView = view.findViewById(R.id.activityCategory)
        val activityImage: ImageView = view.findViewById(R.id.activityItemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_item, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val currentItem = activityList[position]
        holder.activityName.text = currentItem.name
        holder.activityCategory.text = currentItem.category
        holder.activityImage.setImageResource(currentItem.imageResId)
    }

    override fun getItemCount() = activityList.size
}

class MainActivity : AppCompatActivity() {
    private lateinit var userNameTextView: TextView
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)

        // Работа с NavigationBar
        val homeButton: ImageView = findViewById(R.id.homeBtnIcon)
        homeButton.setImageResource(R.drawable.ic_home_outline_active)
        val homeTxt: TextView = findViewById(R.id.homeBtnText)
        homeTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))

        val groupsBtn: LinearLayout = findViewById(R.id.groupsBtn)
        groupsBtn.setOnClickListener {
            val intent = Intent(this, GroupsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val usersPhoto: ImageView = findViewById(R.id.userPhoto)
        usersPhoto.setOnClickListener {
            openGallery()
        }

        val AllActivity: TextView = findViewById(R.id.allActivitiesLink)
        AllActivity.setOnClickListener {
            val intent = Intent(this, GroupsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }


        // Инициализация RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.activityList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Пример списка активностей
        val activityList = listOf(
            ActivityItem("Активность 2", "Категория 2", R.drawable.placeholder),
            ActivityItem("Активность 3", "Категория 3", R.drawable.placeholder),
            ActivityItem("Активность 1", "Категория 1", R.drawable.placeholder)
        )

        // Использование без ресурсов
        val spacingInPixels = 16 // укажите нужное значение в пикселях
        recyclerView.addItemDecoration(SpaceItemDecoration(spacingInPixels))

        // Установка адаптера
        val adapter = ActivityAdapter(activityList)
        recyclerView.adapter = adapter

        // Инициализация Firebase Auth и Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Инициализация TextView для имени
        userNameTextView = findViewById(R.id.userNameTextView)

        // Получение текущего пользователя
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadUserName(currentUser.uid)
            loadUserPhoto(currentUser.uid)
        }
    }

    private fun loadUserName(userId: String) {
        database.child(userId).child("name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userName = snapshot.getValue(String::class.java)
                    userNameTextView.text = userName ?: "Имя не найдено"
                } else {
                    userNameTextView.text = "Имя не найдено"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                userNameTextView.text = "Ошибка загрузки имени"
            }
        })
    }

    private fun loadUserPhoto(userId: String) {
        database.child(userId).child("UserPhoto").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val photoUrl = snapshot.getValue(String::class.java)
                    if (photoUrl != null && photoUrl.isNotEmpty()) {
                        // Загружаем изображение с помощью Glide
                        loadImageFromUrl(photoUrl)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadImageFromUrl(imageUrl: String) {
        val usersPhoto: ImageView = findViewById(R.id.userPhoto)
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder) // Заглушка во время загрузки
            .into(usersPhoto)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            if (imageUri != null) {
                loadImageFromUri(imageUri)
            }
        }
    }

    private fun loadImageFromUri(imageUri: Uri) {
        val usersPhoto: ImageView = findViewById(R.id.userPhoto)
        Glide.with(this)
            .load(imageUri)
            .placeholder(R.drawable.placeholder) // Заглушка во время загрузки
            .into(usersPhoto)

        uploadImageToFirebase(imageUri)
    }

    private fun uploadImageToFirebase(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference

        val imageRef = storageRef.child("user_photos/$userId.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveUserPhotoUrlToDatabase(userId, downloadUri.toString())
                }.addOnFailureListener {
                    Toast.makeText(this, "Ошибка получения ссылки", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Ошибка загрузки: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserPhotoUrlToDatabase(userId: String, photoUrl: String) {
        database.child(userId).child("UserPhoto").setValue(photoUrl)
            .addOnSuccessListener {
                Toast.makeText(this, "Фото успешно загружено!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Ошибка сохранения ссылки: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
