package com.example.bankapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AllContactsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter
    private lateinit var searchEditText: EditText
    private val userList = mutableListOf<User>()
    private val database = FirebaseDatabase.getInstance().reference.child("Users")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_all_contacts, container, false)
        recyclerView = view.findViewById(R.id.allContactsList)
        searchEditText = requireActivity().findViewById(R.id.membersSearch)

        setupRecyclerView()
        loadUsers()
        setupSearch()

        return view
    }

    private fun setupRecyclerView() {
        adapter = ContactsAdapter(userList) { selectedUser ->
            findUserIdByEmail(selectedUser.email)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadUsers() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Log.e("UserLoading", "User not authenticated")
            return
        }

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let {
                        Log.d("UserLoading", "Loaded user: ${it.name}, ID: ${it.userId}")
                        userList.add(it)
                    }
                }
                adapter.notifyDataSetChanged()
                checkFriendshipStatusForUsers(currentUserId)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserLoading", "Error loading users: ${error.message}")
                Toast.makeText(context, "Error loading users: ${error.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun checkFriendshipStatusForUsers(currentUserId: String) {
        for ((index, user) in userList.withIndex()) {
            val requestsRef = database.child("Users").child(user.userId).child("Friends").child("Requests").child(currentUserId)
            val friendRef = database.child(currentUserId).child("Friends").child("Friending").child(user.userId)

            requestsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(requestsSnapshot: DataSnapshot) {
                    if (requestsSnapshot.exists()) {
                        // Заявка в друзья уже отправлена
                        adapter.markButtonAsHidden(index)
                    } else {
                        // Проверяем, являются ли они друзьями
                        friendRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(friendSnapshot: DataSnapshot) {
                                if (friendSnapshot.exists()) {
                                    // Пользователи уже друзья
                                    adapter.markButtonAsHidden(index)
                                } else {
                                    // Ничего не делаем, кнопка "Добавить в друзья" останется видимой
                                    adapter.markButtonAsVisible(index)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("CheckFriendStatus", "Error: ${error.message}")
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CheckRequestError", "Error: ${error.message}")
                }
            })
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filter(s.toString())
            }

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
                    } else {
                        Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserSearch", "Error searching by email: ${error.message}")
                }
            })
    }

    private fun sendFriendRequest(targetUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val trimmedTargetUserId = targetUserId.trim()
        Log.d("FriendRequestDebug", "Selected user ID: $trimmedTargetUserId")

        if (trimmedTargetUserId.isNotEmpty()) {
            val requestsRef = FirebaseDatabase.getInstance().reference
                .child("Users")
                .child(trimmedTargetUserId)
                .child("Friends")
                .child("Requests")
                .child(currentUserId)

            requestsRef.setValue(currentUserId).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Friend request sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        "Failed to send friend request: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(context, "Invalid user selected. Please try again.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    inner class ContactsAdapter(
        private var users: List<User>,
        private val clickListener: (User) -> Unit
    ) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userPhone: TextView = itemView.findViewById(R.id.userPhone)
            val userPhoto: ImageView = itemView.findViewById(R.id.userPhoto)
            val addFriendButton: ImageButton = itemView.findViewById(R.id.addFriendBtn)

            init {
                addFriendButton.setOnClickListener {
                    val user = users[adapterPosition]
                    clickListener(user)
                }
            }

            fun bind(user: User) {
                userName.text = user.name
                userPhone.text = user.email

                Glide.with(itemView.context)
                    .load(user.UserPhoto)
                    .placeholder(R.drawable.ic_person_outline)
                    .into(userPhoto)

                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                Log.d("TESTFRND", "Current user ID: $currentUserId")

                if (currentUserId != null && user.userId == currentUserId) {
                    addFriendButton.visibility = View.GONE
                } else {
                    checkFriendStatusAndRequest(user.userId, currentUserId, addFriendButton)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.contacts_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.bind(user)
        }

        override fun getItemCount() = users.size

        fun updateList(newList: List<User>) {
            users = newList
            notifyDataSetChanged()
        }

        fun markButtonAsHidden(index: Int) {
            notifyItemChanged(index)
        }

        fun markButtonAsVisible(index: Int) {
            notifyItemChanged(index)
        }

        private fun checkFriendStatusAndRequest(
            targetUserId: String,
            currentUserId: String?,
            button: ImageButton
        ) {
            if (currentUserId != null) {
                val requestRef = FirebaseDatabase.getInstance().reference
                    .child("Users")
                    .child(targetUserId)
                    .child("Friends")
                    .child("Requests")
                    .child(currentUserId)

                val friendRef = FirebaseDatabase.getInstance().reference
                    .child(currentUserId)
                    .child("Friends")
                    .child("Friending")
                    .child(targetUserId)

                Log.d("TESTFRND", "Checking friend status for targetUserId: $targetUserId")
                Log.d("TESTFRND", "Current user ID: $currentUserId")

                // Проверяем, существует ли заявка в друзья
                requestRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Log.d("TESTFRND", "Friend request already sent to ID: $targetUserId")
                            button.visibility = View.GONE
                        } else {
                            // Проверяем, являются ли они друзьями
                            friendRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(friendSnapshot: DataSnapshot) {
                                    if (friendSnapshot.exists()) {
                                        Log.d("TESTFRND", "Already friends with ID: $targetUserId")
                                        button.visibility = View.GONE
                                    } else {
                                        Log.d("TESTFRND", "You can add ID: $targetUserId as a friend")
                                        button.visibility = View.VISIBLE
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e("CheckFriendStatus", "Error: ${error.message}")
                                }
                            })
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("CheckRequestError", "Error: ${error.message}")
                    }
                })
            }
        }
    }
}

data class User(
    val userId: String = "",
    val Groups: Any? = null,
    val UserPhoto: String = "",
    val email: String = "",
    val name: String = "",
    val pin: String? = null
)
