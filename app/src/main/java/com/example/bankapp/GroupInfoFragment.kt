package com.example.bankapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroupInfoFragment : Fragment() {

    private lateinit var groupDescription: TextView
    private lateinit var groupCategories: TextView
    private lateinit var groupTitle: TextView
    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var database: DatabaseReference
    private var groupId: String? = null

    private val membersList = mutableListOf<User>()
    private lateinit var membersAdapter: GroupMembersAdapter

    data class User(
        val userId: String = "",
        val name: String = "",
        val email: String = "",
        val UserPhoto: String = "",
        val pin: String? = null
    )

    companion object {
        fun newInstance(groupId: String) = GroupInfoFragment().apply {
            arguments = Bundle().apply {
                putString("GROUP_ID", groupId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = arguments?.getString("GROUP_ID")
        database = FirebaseDatabase.getInstance().reference
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_info, container, false)

        // Инициализация UI элементов
        groupTitle = view.findViewById(R.id.groupTitle)
        groupDescription = view.findViewById(R.id.groupDescription)
        groupCategories = view.findViewById(R.id.groupDescription)
        membersRecyclerView = view.findViewById(R.id.membersList)

        // Настройка RecyclerView
        membersAdapter = GroupMembersAdapter(membersList)
        membersRecyclerView.layoutManager = LinearLayoutManager(context)
        membersRecyclerView.adapter = membersAdapter

        // Загрузка данных
        groupId?.let {
            loadGroupData(it)
            loadGroupMembers(it)
        }

        return view
    }

    private fun loadGroupData(groupId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            database.child("Groups").child(groupId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Получаем название группы
                        val groupTitleText = snapshot.child("title").getValue(String::class.java)
                        // Получаем описание группы
                        val groupDescText = snapshot.child("description").getValue(String::class.java)
                        // Получаем список категорий
                        val groupCategoriesList = snapshot.child("categories").children
                            .map { it.getValue(String::class.java) }
                            .filterNotNull()

                        // Устанавливаем название группы
                        groupTitle.text = groupDescText ?: "Название не найдено"
                        // Устанавливаем описание группы
                        groupDescription.text = groupDescText ?: "Описание не найдено"

                        // Устанавливаем категории
                        if (groupCategoriesList.isNotEmpty()) {
                            groupCategories.text = groupCategoriesList.joinToString(", ")
                        } else {
                            groupCategories.text = "Категории не указаны"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("GroupInfo", "Error loading group data: ${error.message}")
                        groupTitle.text = "Ошибка загрузки данных"
                    }
                })
        } else {
            groupTitle.text = "Пользователь не найден"
        }
    }

    private fun loadGroupMembers(groupId: String) {
        database.child("Groups").child(groupId).child("Users")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    membersList.clear()
                    for (memberSnapshot in snapshot.children) {
                        val userId = memberSnapshot.key
                        userId?.let { loadUserDetails(it) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("GroupInfo", "Error loading group members: ${error.message}")
                }
            })
    }

    private fun loadUserDetails(userId: String) {
        database.child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)?.copy(userId = userId)
                    user?.let {
                        if (!membersList.any { existing -> existing.userId == userId }) {
                            membersList.add(it)
                            membersAdapter.notifyDataSetChanged()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("GroupInfo", "Error loading user details: ${error.message}")
                }
            })
    }

    inner class GroupMembersAdapter(private val users: List<User>) :
        RecyclerView.Adapter<GroupMembersAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userEmail: TextView = itemView.findViewById(R.id.userPhone)
            val userPhoto: ImageView = itemView.findViewById(R.id.userPhoto)
            val addButton: ImageButton = itemView.findViewById(R.id.addFriendBtn)
            val bestFriendCheckbox: CheckBox = itemView.findViewById(R.id.markContactBtn)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.contacts_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]

            holder.userName.text = user.name
            holder.userEmail.text = user.email

            Glide.with(holder.itemView.context)
                .load(user.UserPhoto)
                .placeholder(R.drawable.ic_person_outline)
                .into(holder.userPhoto)

            // Скрываем ненужные элементы для списка участников
            holder.addButton.visibility = View.GONE
            holder.bestFriendCheckbox.visibility = View.GONE
        }

        override fun getItemCount(): Int = users.size
    }
}