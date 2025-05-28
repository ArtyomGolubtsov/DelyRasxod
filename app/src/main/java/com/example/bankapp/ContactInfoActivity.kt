package com.example.bankapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ContactInfoActivity : AppCompatActivity() {

    private lateinit var contactProfileImage: ImageView
    private lateinit var contactUserName: TextView
    private lateinit var contactUserPhone: TextView
    private lateinit var commonGroupsRecyclerView: RecyclerView
    private lateinit var btnGoBack: ImageButton
    private lateinit var database: FirebaseDatabase

    private val commonGroupsList = mutableListOf<Group>()
    private lateinit var commonGroupsAdapter: CommonGroupsAdapter

    data class Group(
        val groupId: String = "",
        val title: String = "",
        val imageUri: String = "",
        val categories: List<String> = emptyList()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_contact_info)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)
        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)

        //------------------------Меню
        val mainBtn: LinearLayout = findViewById(R.id.homeBtn)
        val contactsIcon: ImageView = findViewById(R.id.contactsBtnIcon)
        val contactsTxt: TextView = findViewById(R.id.contactsBtnText)
        contactsIcon.setImageResource(R.drawable.ic_contacts_active)
        contactsTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))

        mainBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            mainBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        val proflBtn: LinearLayout = findViewById(R.id.profileBtn)
        proflBtn.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
            proflBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
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
        //----------------------------

        initViews()

        val userId = intent.getStringExtra("USER_ID")
        if (userId != null) {
            loadUserData(userId)
            loadCommonGroups(userId)
        } else {
            Log.e("ContactInfo", "No user ID provided")
            finish()
        }

        btnGoBack.setOnClickListener { finish() }
    }

    private fun initViews() {
        contactProfileImage = findViewById(R.id.contactProfileImage)
        contactUserName = findViewById(R.id.contactUserName)
        contactUserPhone = findViewById(R.id.contactUserPhone)
        commonGroupsRecyclerView = findViewById(R.id.commonGroupsList)
        btnGoBack = findViewById(R.id.btnGoBack)

        // Настройка RecyclerView
        commonGroupsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Установка отступов для RecyclerView
        commonGroupsRecyclerView.setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
        commonGroupsRecyclerView.clipToPadding = false

        commonGroupsAdapter = CommonGroupsAdapter(commonGroupsList)
        commonGroupsRecyclerView.adapter = commonGroupsAdapter

        database = FirebaseDatabase.getInstance()
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun loadUserData(userId: String) {
        database.getReference("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Неизвестно"
                    val email = snapshot.child("email").getValue(String::class.java) ?: "Нет почты"
                    val photoUrl = snapshot.child("UserPhoto").getValue(String::class.java)

                    contactUserName.text = name
                    contactUserPhone.text = email

                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this@ContactInfoActivity)
                            .load(photoUrl)
                            .placeholder(R.drawable.placeholder)
                            .into(contactProfileImage)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ContactInfo", "Error loading user data: ${error.message}")
                }
            })
    }

    private fun loadCommonGroups(userId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Log.e("ContactInfo", "Current user not authenticated")
            return
        }

        database.getReference("Users").child(currentUserId).child("Groups")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(currentUserGroupsSnapshot: DataSnapshot) {
                    val currentUserGroups = mutableListOf<String>()
                    currentUserGroupsSnapshot.children.forEach { groupSnapshot ->
                        groupSnapshot.key?.let { currentUserGroups.add(it) }
                    }

                    database.getReference("Users").child(userId).child("Groups")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(targetUserGroupsSnapshot: DataSnapshot) {
                                val targetUserGroups = mutableListOf<String>()
                                targetUserGroupsSnapshot.children.forEach { groupSnapshot ->
                                    groupSnapshot.key?.let { targetUserGroups.add(it) }
                                }

                                val commonGroupIds = currentUserGroups.intersect(targetUserGroups)
                                loadGroupsInfo(commonGroupIds.toList())
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("ContactInfo", "Error loading target user groups: ${error.message}")
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ContactInfo", "Error loading current user groups: ${error.message}")
                }
            })
    }

    private fun loadGroupsInfo(groupIds: List<String>) {
        if (groupIds.isEmpty()) {
            Log.d("ContactInfo", "No common groups found")
            return
        }

        groupIds.forEach { groupId ->
            database.getReference("Groups").child(groupId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val title = snapshot.child("title").getValue(String::class.java) ?: "Без названия"
                        val imageUri = snapshot.child("imageUri").getValue(String::class.java) ?: ""
                        val categories = snapshot.child("categories").children
                            .mapNotNull { it.getValue(String::class.java) }

                        val group = Group(groupId, title, imageUri, categories)
                        if (!commonGroupsList.any { it.groupId == groupId }) {
                            commonGroupsList.add(group)
                            commonGroupsAdapter.notifyDataSetChanged()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("ContactInfo", "Error loading group info: ${error.message}")
                    }
                })
        }
    }
    inner class CommonGroupsAdapter(private val groups: List<Group>) :
        RecyclerView.Adapter<CommonGroupsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val groupImage: ImageView = itemView.findViewById(R.id.activityItemImage)
            val groupName: TextView = itemView.findViewById(R.id.activityName)
            val groupCategory: TextView = itemView.findViewById(R.id.activityCategory)

            init {
                // Устанавливаем отступы для каждого элемента
                val params = itemView.layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(
                    itemView.context.dpToPx(4), itemView.context.dpToPx(8),
                    itemView.context.dpToPx(8), itemView.context.dpToPx(4)
                )
                itemView.layoutParams = params
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.activity_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val group = groups[position]
            holder.groupName.text = group.title

            val categoryText = group.categories.firstOrNull() ?: "Без категории"
            holder.groupCategory.text = categoryText

            if (group.imageUri.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(group.imageUri)
                    .placeholder(R.drawable.placeholder)
                    .into(holder.groupImage)
            } else {
                holder.groupImage.setImageResource(R.drawable.placeholder)
            }
        }

        override fun getItemCount(): Int = groups.size

        // Move this extension function outside the adapter or make it a regular function
        private fun Context.dpToPx(dp: Int): Int {
            return (dp * this.resources.displayMetrics.density).toInt()
        }
    }}