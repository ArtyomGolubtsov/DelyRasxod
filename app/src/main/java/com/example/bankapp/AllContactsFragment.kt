package com.example.bankapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
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

class AllContactsFragment : Fragment() {

    data class User(
        val userId: String = "",
        val name: String = "",
        val email: String = "",
        val UserPhoto: String = "",
        val pin: String? = null,
        var isFriend: Boolean = false,
        var isBestFriend: Boolean = false,
        var hasPendingRequest: Boolean = false // Новое поле для отслеживания отправленных запросов
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private lateinit var searchEditText: EditText
    private val userList = mutableListOf<User>()
    private val database = FirebaseDatabase.getInstance().reference.child("Users")
    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_all_contacts, container, false)
        recyclerView = view.findViewById(R.id.allContactsList)
        searchEditText = requireActivity().findViewById(R.id.membersSearch)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return view
        }

        setupRecyclerView()
        loadAllUsers()
        setupSearch()

        return view
    }

    private fun setupRecyclerView() {
        adapter = ContactsAdapter(
            userList,
            onFriendClick = { selectedUser -> findUserIdByEmail(selectedUser.email) },
            onBestFriendCheck = { user, isChecked ->
                if (isChecked) {
                    addToBestFriends(user.userId)
                } else {
                    removeFromBestFriends(user.userId)
                }
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadAllUsers() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)?.copy(userId = userSnapshot.key ?: "")
                    user?.let {
                        if (it.userId != currentUserId) {
                            checkUserStatus(it) { isFriend, isBestFriend, hasPendingRequest ->
                                it.isFriend = isFriend
                                it.isBestFriend = isBestFriend
                                it.hasPendingRequest = hasPendingRequest
                                userList.add(it)
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserLoading", "Error loading users: ${error.message}")
                Toast.makeText(context, "Error loading users", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkUserStatus(user: User, callback: (Boolean, Boolean, Boolean) -> Unit) {
        if (currentUserId == null) {
            callback(false, false, false)
            return
        }

        // Проверяем, является ли пользователь другом
        database.child(currentUserId!!).child("Friends").child("Frinding").child(user.userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(friendSnapshot: DataSnapshot) {
                    val isFriend = friendSnapshot.exists()

                    // Проверяем, является ли пользователь лучшим другом
                    database.child(currentUserId!!).child("Friends").child("BestFriend").child(user.userId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(bestFriendSnapshot: DataSnapshot) {
                                val isBestFriend = bestFriendSnapshot.exists()

                                // Проверяем, отправили ли мы запрос этому пользователю
                                database.child(user.userId).child("Friends").child("Requests").child(currentUserId!!)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(requestSnapshot: DataSnapshot) {
                                            callback(isFriend, isBestFriend, requestSnapshot.exists())
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            callback(isFriend, isBestFriend, false)
                                        }
                                    })
                            }

                            override fun onCancelled(error: DatabaseError) {
                                callback(isFriend, false, false)
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false, false, false)
                }
            })
    }

    private fun addToBestFriends(targetUserId: String) {
        if (currentUserId == null) return

        database.child(currentUserId!!).child("Friends").child("Frinding").child(targetUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        database.child(currentUserId!!).child("Friends").child("BestFriend").child(targetUserId)
                            .setValue(targetUserId)
                            .addOnSuccessListener {
                                updateBestFriendStatus(targetUserId, true)
                                Toast.makeText(context, "Added to Best Friends", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to add to Best Friends", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context,
                            "Add user to friends first",
                            Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error checking friendship", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun removeFromBestFriends(targetUserId: String) {
        if (currentUserId == null) return

        database.child(currentUserId!!).child("Friends").child("BestFriend").child(targetUserId)
            .removeValue()
            .addOnSuccessListener {
                updateBestFriendStatus(targetUserId, false)
                Toast.makeText(context, "Removed from Best Friends", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to remove from Best Friends", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateBestFriendStatus(userId: String, isBestFriend: Boolean) {
        val user = userList.find { it.userId == userId }
        user?.let {
            it.isBestFriend = isBestFriend
            adapter.notifyItemChanged(userList.indexOf(it))
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filter(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filter(text: String) {
        val filteredList = if (text.isEmpty()) {
            userList
        } else {
            userList.filter {
                it.name.contains(text, true) || it.email.contains(text, true)
            }
        }
        adapter.updateList(filteredList)
    }

    private fun findUserIdByEmail(email: String) {
        database.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val targetUserId = userSnapshot.key
                            if (targetUserId != null) {
                                sendFriendRequest(targetUserId)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserSearch", "Error searching by email: ${error.message}")
                }
            })
    }

    private fun sendFriendRequest(targetUserId: String) {
        if (currentUserId == null || currentUserId == targetUserId) return

        database.child(targetUserId).child("Friends").child("Requests").child(currentUserId!!)
            .setValue(currentUserId)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Обновляем статус запроса в локальном списке
                    updateRequestStatus(targetUserId, true)
                    Toast.makeText(context, "Friend request sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to send request", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateRequestStatus(userId: String, hasRequest: Boolean) {
        val user = userList.find { it.userId == userId }
        user?.let {
            it.hasPendingRequest = hasRequest
            adapter.notifyItemChanged(userList.indexOf(it))
        }
    }

    inner class ContactsAdapter(
        private var users: List<User>,
        private val onFriendClick: (User) -> Unit,
        private val onBestFriendCheck: (User, Boolean) -> Unit
    ) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userPhone: TextView = itemView.findViewById(R.id.userPhone)
            val userPhoto: ImageView = itemView.findViewById(R.id.userPhoto)
            val addFriendButton: ImageButton = itemView.findViewById(R.id.addFriendBtn)
            val bestFriendCheckbox: CheckBox = itemView.findViewById(R.id.markContactBtn)

            fun bind(user: User) {
                userName.text = user.name
                userPhone.text = user.email

                Glide.with(itemView.context)
                    .load(user.UserPhoto)
                    .placeholder(R.drawable.ic_person_outline)
                    .into(userPhoto)

                // Скрываем кнопку, если пользователь уже в друзьях или есть отправленный запрос
                if (user.isFriend) {
                    addFriendButton.visibility = View.GONE
                } else {
                    addFriendButton.visibility = View.VISIBLE
                    // Изменяем фон кнопки в зависимости от состояния запроса
                    addFriendButton.setBackgroundResource(
                        if (user.hasPendingRequest) R.drawable.activity_item_bg // фон для отправленного запроса
                        else R.drawable.btn_blue_bg // обычный фон
                    )
                }



                bestFriendCheckbox.isChecked = user.isBestFriend
                bestFriendCheckbox.visibility = if (user.isFriend) View.VISIBLE else View.GONE
                bestFriendCheckbox.isEnabled = user.isFriend

                addFriendButton.setOnClickListener { onFriendClick(user) }
                bestFriendCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    if (user.isFriend) {
                        onBestFriendCheck(user, isChecked)
                    } else {
                        bestFriendCheckbox.isChecked = false
                        Toast.makeText(
                            itemView.context,
                            "Add user to friends first",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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

        override fun getItemCount() = users.size

        fun updateList(newList: List<User>) {
            users = newList
            notifyDataSetChanged()
        }
    }
}