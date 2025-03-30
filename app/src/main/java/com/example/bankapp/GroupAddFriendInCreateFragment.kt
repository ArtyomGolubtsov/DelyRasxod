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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GroupAddFriendInCreateFragment : Fragment() {

    data class User(
        val userId: String = "",
        val name: String = "",
        val email: String = "",
        val UserPhoto: String = "",
        val pin: String? = null
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BestFriendsAdapter
    private val bestFriendList = mutableListOf<User>()
    private val database = FirebaseDatabase.getInstance().reference
    private var currentUserId: String? = null
    private var groupId: String? = null

    companion object {
        fun newInstance(groupId: String): GroupAddFriendInCreateFragment {
            val fragment = GroupAddFriendInCreateFragment()
            val args = Bundle().apply {
                putString("groupId", groupId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = arguments?.getString("groupId")
        Log.d("GroupFragment", "Received groupId: $groupId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend_requests, container, false)
        recyclerView = view.findViewById(R.id.requestsList)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(context, "Необходимо авторизоваться", Toast.LENGTH_SHORT).show()
            return view
        }

        setupRecyclerView()
        loadBestFriends()

        return view
    }

    private fun setupRecyclerView() {
        adapter = BestFriendsAdapter(bestFriendList) { targetUser ->
            // При нажатии на кнопку добавления ищем пользователя по email и добавляем в группу
            findUserByEmailAndAddToGroup(targetUser.email)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun findUserByEmailAndAddToGroup(email: String) {
        database.child("Users")
            .orderByChild("email")
            .equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val userId = userSnapshot.key
                            userId?.let {
                                addUserToGroup(it)
                                return
                            }
                        }
                    }
                    Toast.makeText(context, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SearchUser", "Error searching user: ${error.message}")
                    Toast.makeText(context, "Ошибка поиска пользователя", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun addUserToGroup(targetUserId: String) {
        if (groupId.isNullOrEmpty()) {
            Toast.makeText(context, "Ошибка: ID группы не указан", Toast.LENGTH_SHORT).show()
            Log.e("GroupAdd", "GroupId is null or empty")
            return
        }

        Log.d("GroupAdd", "Adding user $targetUserId to group $groupId")

        // 1. Добавляем пользователя в группу: Groups -> groupId -> Users -> userId: userId
        val groupUserRef = database.child("Groups").child(groupId!!).child("Users").child(targetUserId)

        // 2. Добавляем группу к пользователю: Users -> userId -> Groups -> groupId: groupId
        val userGroupRef = database.child("Users").child(targetUserId).child("Groups").child(groupId!!)

        // Создаем транзакцию для атомарного обновления
        groupUserRef.setValue(targetUserId)
            .addOnSuccessListener {
                userGroupRef.setValue(groupId)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Пользователь добавлен в группу", Toast.LENGTH_SHORT).show()
                        Log.d("GroupAdd", "Successfully added user to group")
                    }
                    .addOnFailureListener { e ->
                        Log.e("GroupAdd", "Error adding group to user: ${e.message}")
                        Toast.makeText(context, "Ошибка добавления группы пользователю", Toast.LENGTH_SHORT).show()
                        // Откатываем изменения
                        groupUserRef.removeValue()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("GroupAdd", "Error adding user to group: ${e.message}")
                Toast.makeText(context, "Ошибка добавления в группу", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadBestFriends() {
        database.child("Users").child(currentUserId!!).child("Friends").child("BestFriend")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    bestFriendList.clear()
                    for (bestFriendSnapshot in snapshot.children) {
                        val bestFriendId = bestFriendSnapshot.key
                        bestFriendId?.let { loadBestFriendInfo(it) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendBest", "Error loading best friends: ${error.message}")
                }
            })
    }

    private fun loadBestFriendInfo(userId: String) {
        database.child("Users").child(userId).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        if (it.userId != currentUserId) {
                            bestFriendList.add(it)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendBest", "Error loading user: ${error.message}")
                }
            })
    }

    inner class BestFriendsAdapter(
        private val users: List<User>,
        private val onAddClick: (User) -> Unit
    ) : RecyclerView.Adapter<BestFriendsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userEmail: TextView = itemView.findViewById(R.id.userPhone)
            val userPhoto: ImageView = itemView.findViewById(R.id.userPhoto)
            val bestFriendCheckbox: CheckBox = itemView.findViewById(R.id.markContactBtn)
            val addButton: ImageButton = itemView.findViewById(R.id.addFriendBtn)

            fun bind(user: User) {
                userName.text = user.name
                userEmail.text = user.email
                bestFriendCheckbox.isChecked = true
                bestFriendCheckbox.isEnabled = false

                Glide.with(itemView.context)
                    .load(user.UserPhoto)
                    .placeholder(R.drawable.ic_person_outline)
                    .into(userPhoto)

                addButton.setImageResource(R.drawable.ic_add)
                addButton.setOnClickListener {
                    onAddClick(user)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.contacts_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(users[position])
        }

        override fun getItemCount(): Int = users.size
    }
}