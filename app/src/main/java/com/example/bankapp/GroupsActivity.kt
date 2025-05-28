package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class GroupItem(
    val id: String,
    val name: String,
    val categories: String,
    val imageUrl: String
)

class GroupAdapter(
    private var groupList: List<GroupItem>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(groupId: String)
    }

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val groupName: TextView = view.findViewById(R.id.activityName)
        val groupCategory: TextView = view.findViewById(R.id.activityCategory)
        val groupImage: ImageView = view.findViewById(R.id.activityItemImage)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item, parent, false)

        // Устанавливаем отступы для каждого элемента
        val params = view.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(16, 16, 16, 16)
        view.layoutParams = params

        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val currentItem = groupList[position]
        holder.groupName.text = currentItem.name
        holder.groupCategory.text = currentItem.categories

        Glide.with(holder.itemView.context)
            .load(currentItem.imageUrl)
            .placeholder(R.drawable.placeholder)
            .into(holder.groupImage)

        holder.itemView.setOnClickListener {
            listener.onItemClick(currentItem.id)
        }
    }

    override fun getItemCount() = groupList.size

    fun updateList(newList: List<GroupItem>) {
        groupList = newList
        notifyDataSetChanged()
    }
}

class GroupViewModel : ViewModel() {
    private val _groupList = MutableLiveData<MutableList<GroupItem>>()
    val groupList: LiveData<MutableList<GroupItem>> = _groupList
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun loadUserGroups() {
        val currentUser = auth.currentUser ?: return

        database.child("Users").child(currentUser.uid).child("Groups")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userGroupsSnapshot: DataSnapshot) {
                    val groupIds = mutableListOf<String>()

                    for (groupSnapshot in userGroupsSnapshot.children) {
                        groupSnapshot.key?.let { groupIds.add(it) }
                    }

                    loadGroupsData(groupIds)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Обработка ошибки
                }
            })
    }

    private fun loadGroupsData(groupIds: List<String>) {
        val groups = mutableListOf<GroupItem>()
        var loadedCount = 0

        if (groupIds.isEmpty()) {
            _groupList.postValue(groups)
            return
        }

        for (groupId in groupIds) {
            database.child("Groups").child(groupId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(groupSnapshot: DataSnapshot) {
                        val groupName = groupSnapshot.child("title").getValue(String::class.java) ?: "Без названия"
                        val categories = groupSnapshot.child("categories").children
                            .joinToString(", ") { it.getValue(String::class.java) ?: "" }
                        val imageUrl = groupSnapshot.child("imageUri").getValue(String::class.java) ?: ""

                        groups.add(GroupItem(groupId, groupName, categories, imageUrl))
                        loadedCount++

                        if (loadedCount == groupIds.size) {
                            _groupList.postValue(groups)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadedCount++
                        if (loadedCount == groupIds.size) {
                            _groupList.postValue(groups)
                        }
                    }
                })
        }
    }
}

class GroupsActivity : AppCompatActivity(), GroupAdapter.OnItemClickListener {
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: GroupAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var noGroupsBox: ConstraintLayout
    private lateinit var viewModel: GroupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_groups)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)

        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)
        val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)

        btnGoBack.setOnClickListener {
            onBackPressed()
            finish()
            btnGoBack.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        // Инициализация RecyclerView
        recyclerView = findViewById(R.id.groupsList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Устанавливаем отступы для RecyclerView
        recyclerView.setPadding(16, 16, 16, 16)
        recyclerView.clipToPadding = false

        adapter = GroupAdapter(mutableListOf(), this)
        recyclerView.adapter = adapter

        noGroupsBox = findViewById(R.id.noGroupsBox)
        noGroupsBox.visibility = View.GONE

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        viewModel = ViewModelProvider(this).get(GroupViewModel::class.java)
        viewModel.loadUserGroups()

        viewModel.groupList.observe(this, Observer { groupList ->
            adapter.updateList(groupList)
            updateVisibility(groupList)
        })

        // Нижнее меню
        val icogroupsBtn: ImageView = findViewById(R.id.groupsBtnIcon)
        icogroupsBtn.setImageResource(R.drawable.ic_groups_outline_active)
        val groupTxt: TextView = findViewById(R.id.groupsBtnText)
        groupTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))
        val mainBtn: LinearLayout = findViewById(R.id.homeBtn)

        mainBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            mainBtn.startAnimation(clickAnimation)
        }
        val proflBtn: LinearLayout = findViewById(R.id.profileBtn)
        proflBtn.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
            proflBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        val contactsBtn: LinearLayout = findViewById(R.id.contactsBtn)
        contactsBtn.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
            contactsBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        val createGroupsBtn: Button = findViewById(R.id.addGroupBtn)
        createGroupsBtn.setOnClickListener {
            createGroupsBtn.startAnimation(clickAnimation)
            val intent = Intent(this, NewGroupActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        val createGroupsBtnPlus: ImageButton = findViewById(R.id.addGroupBtnPlus)
        createGroupsBtnPlus.setOnClickListener {
            createGroupsBtnPlus.startAnimation(clickAnimation)
            val intent = Intent(this, NewGroupActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onItemClick(groupId: String) {
        val intent = Intent(this, GroupInfoActivity::class.java)
        intent.putExtra("GROUP_ID", groupId)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun updateVisibility(groupList: List<GroupItem>) {
        if (groupList.isNotEmpty()) {
            noGroupsBox.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        } else {
            noGroupsBox.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
    }
}