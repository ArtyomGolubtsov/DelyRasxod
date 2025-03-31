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
        val pin: String? = null,
        var isBestFriend: Boolean = false,
        var isAddedToGroup: Boolean = false // Новое поле для отслеживания добавления в группу
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FriendsAdapter
    private val friendsList = mutableListOf<User>()
    private val bestFriendIds = mutableSetOf<String>()
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
        loadBestFriendsFirst()

        return view
    }

    private fun setupRecyclerView() {
        adapter = FriendsAdapter(friendsList) { targetUser ->
            if (!targetUser.isAddedToGroup) {
                addUserToGroup(targetUser)
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun addUserToGroup(targetUser: User) {
        if (groupId.isNullOrEmpty()) {
            Toast.makeText(context, "Ошибка: ID группы не указан", Toast.LENGTH_SHORT).show()
            Log.e("GroupAdd", "GroupId is null or empty")
            return
        }

        Log.d("GroupAdd", "Adding user ${targetUser.userId} to group $groupId")

        val groupUserRef = database.child("Groups").child(groupId!!).child("Users").child(targetUser.userId).child(targetUser.userId)
        val userGroupRef = database.child("Users").child(targetUser.userId).child("Groups").child(groupId!!).child(groupId!!)

        groupUserRef.setValue(true)
            .addOnSuccessListener {
                userGroupRef.setValue(true)
                    .addOnSuccessListener {
                        // Обновляем статус добавления в группу
                        updateUserAddedStatus(targetUser.userId, true)
                        Toast.makeText(context, "Пользователь добавлен в группу", Toast.LENGTH_SHORT).show()
                        Log.d("GroupAdd", "Successfully added user to group")
                    }
                    .addOnFailureListener { e ->
                        Log.e("GroupAdd", "Error adding group to user: ${e.message}")
                        groupUserRef.removeValue()
                        Toast.makeText(context, "Ошибка добавления группы пользователю", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("GroupAdd", "Error adding user to group: ${e.message}")
                Toast.makeText(context, "Ошибка добавления в группу", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserAddedStatus(userId: String, isAdded: Boolean) {
        val position = friendsList.indexOfFirst { it.userId == userId }
        if (position != -1) {
            friendsList[position].isAddedToGroup = isAdded
            adapter.notifyItemChanged(position)
        }
    }

    private fun loadBestFriendsFirst() {
        database.child("Users").child(currentUserId!!).child("Friends").child("BestFriend")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { friendSnapshot ->
                        friendSnapshot.key?.let { friendId ->
                            bestFriendIds.add(friendId)
                        }
                    }
                    loadAllRegularFriends()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendLoad", "Best friends load error", error.toException())
                    loadAllRegularFriends()
                }
            })
    }

    private fun loadAllRegularFriends() {
        database.child("Users").child(currentUserId!!).child("Friends").child("Frinding")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    friendsList.clear()
                    snapshot.children.forEach { friendSnapshot ->
                        friendSnapshot.key?.let { friendId ->
                            loadFriendDetails(friendId)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendLoad", "Regular friends load error", error.toException())
                }
            })
    }

    private fun loadFriendDetails(userId: String) {
        database.child("Users").child(userId).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)?.copy(
                        userId = userId,
                        isBestFriend = bestFriendIds.contains(userId)
                    )

                    user?.let {
                        if (it.userId != currentUserId) {
                            // Проверяем, добавлен ли уже пользователь в группу
                            checkIfUserInGroup(it)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendLoad", "User details load error", error.toException())
                }
            })
    }

    private fun checkIfUserInGroup(user: User) {
        if (groupId.isNullOrEmpty()) {
            friendsList.add(user)
            adapter.notifyDataSetChanged()
            return
        }

        database.child("Groups").child(groupId!!).child("Users").child(user.userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isInGroup = snapshot.exists()
                    val updatedUser = user.copy(isAddedToGroup = isInGroup)
                    friendsList.add(updatedUser)
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    friendsList.add(user)
                    adapter.notifyDataSetChanged()
                }
            })
    }

    inner class FriendsAdapter(
        private val users: List<User>,
        private val onAddClick: (User) -> Unit
    ) : RecyclerView.Adapter<FriendsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userEmail: TextView = itemView.findViewById(R.id.userPhone)
            val userPhoto: ImageView = itemView.findViewById(R.id.userPhoto)
            val friendTypeCheckbox: CheckBox = itemView.findViewById(R.id.markContactBtn)
            val addButton: ImageButton = itemView.findViewById(R.id.addFriendBtn)

            fun bind(user: User) {
                userName.text = if (user.isBestFriend) " ${user.name}" else user.name
                userEmail.text = user.email
                friendTypeCheckbox.isChecked = user.isBestFriend
                friendTypeCheckbox.isEnabled = false

                Glide.with(itemView.context)
                    .load(user.UserPhoto)
                    .placeholder(R.drawable.ic_person_outline)
                    .into(userPhoto)

                // Изменяем вид кнопки в зависимости от статуса добавления
                if (user.isAddedToGroup) {
                    addButton.setBackgroundResource(R.drawable.activity_item_bg)
                    addButton.isEnabled = false
                } else {
                    addButton.setImageResource(R.drawable.ic_add)
                    addButton.setBackgroundResource(R.drawable.btn_blue_bg)
                    addButton.isEnabled = true
                }

                addButton.setOnClickListener { onAddClick(user) }
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