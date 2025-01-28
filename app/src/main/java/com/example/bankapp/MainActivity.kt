package com.example.bankapp

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.InputStream

// Make ActivityItem open for inheritance
open class ActivityItem(
    val name: String,
    val category: String,
    val imageUrl: String
)

class StaticActivityItem(
    imageUrl: String
) : ActivityItem("У вас еще нет групп(", "Создайте их!", imageUrl)

// item decoration for recycler view
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

// Custom adapter for displaying ActivityItems
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

        Glide.with(holder.itemView.context)
            .load(currentItem.imageUrl)
            .placeholder(R.drawable.logo)
            .into(holder.activityImage)

        if (currentItem is StaticActivityItem) {
            holder.itemView.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, NewGroupActivity::class.java)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = activityList.size
}

// Main activity for the application
class MainActivity : AppCompatActivity() {
    private lateinit var userNameTextView: TextView
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var activityList: MutableList<ActivityItem>
    private lateinit var adapter: ActivityAdapter

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)

        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)

        val groupsBtn: LinearLayout = findViewById(R.id.groupsBtn)
        groupsBtn.setOnClickListener {
            startActivity(Intent(this, GroupsActivity::class.java))
            groupsBtn.startAnimation(clickAnimation)
        }

        val usersPhoto: ImageView = findViewById(R.id.userPhoto)
        usersPhoto.setOnClickListener {
            openGallery()
            usersPhoto.startAnimation(clickAnimation)
        }

        // Нижнее меню----------------------------------------
        val homeBtnIcon: ImageView = findViewById(R.id.homeBtnIcon)
        homeBtnIcon.setImageResource(R.drawable.ic_home_outline_active)
        val homeTxt: TextView = findViewById(R.id.homeBtnText)
        homeTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))
        val mainBtn: LinearLayout = findViewById(R.id.homeBtn)

        mainBtn.setOnClickListener {
            val intent = Intent(this, NewGroupActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            mainBtn.startAnimation(clickAnimation)
        }

        val TextAllGroup: TextView = findViewById(R.id.allActivitiesLink)
        TextAllGroup.setOnClickListener{
            val intent = Intent(this, NewGroupActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            mainBtn.startAnimation(clickAnimation)
        }


        userNameTextView = findViewById(R.id.userNameTextView)

        // Initialize RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.activityList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize activity list
        activityList = mutableListOf()
        adapter = ActivityAdapter(activityList)
        recyclerView.adapter = adapter

        // Add spacing to RecyclerView
        val spacingInPixels = 16
        recyclerView.addItemDecoration(SpaceItemDecoration(spacingInPixels))

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        val currentUser = auth.currentUser
        currentUser?.let {
            loadUserName(it.uid)
            loadUserPhoto(it.uid) // Ensure this function is defined
            loadUserActivities(it.uid)
        }
    }

    private fun loadUserActivities(userId: String) {
        database.child(userId).child("Groups").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                activityList.clear()
                for (groupSnapshot in snapshot.children) {
                    val groupName = groupSnapshot.child("title").getValue(String::class.java) ?: "Без названия"
                    val categories = groupSnapshot.child("categories").children.joinToString(", ") { it.getValue(String::class.java) ?: "" }
                    val imageUrl = groupSnapshot.child("imageUri").getValue(String::class.java) ?: ""

                    activityList.add(ActivityItem(groupName, categories, imageUrl))
                }

                if (activityList.isEmpty()) {
                    val defaultImageUrl = "drawable/logo"
                    activityList.add(StaticActivityItem(defaultImageUrl))
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Ошибка загрузки активностей", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserName(userId: String) {
        database.child(userId).child("name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userNameTextView.text = snapshot.getValue(String::class.java) ?: "Имя не найдено"
            }

            override fun onCancelled(error: DatabaseError) {
                userNameTextView.text = "Ошибка загрузки имени"
            }
        })
    }

    private fun loadUserPhoto(userId: String) {
        database.child(userId).child("UserPhoto").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val photoUrl = snapshot.getValue(String::class.java)
                val usersPhoto: ImageView = findViewById(R.id.userPhoto)
                if (photoUrl != null) {
                    Glide.with(this@MainActivity)
                        .load(photoUrl)
                        .placeholder(R.drawable.placeholder)
                        .into(usersPhoto)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
            }
        })
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
            imageUri?.let { loadImageFromUri(it) }
        }
    }

    private fun loadImageFromUri(imageUri: Uri) {
        val usersPhoto: ImageView = findViewById(R.id.userPhoto)
        Glide.with(this)
            .load(imageUri)
            .placeholder(R.drawable.placeholder)
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
