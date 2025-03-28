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

class FriendFragment : Fragment() {

    data class User(
        val userId: String = "",
        val name: String = "",
        val email: String = "",
        val UserPhoto: String = "",
        val pin: String? = null,
        var isBestFriend: Boolean = false
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
        loadFriends()
        setupSearch()

        return view
    }

    private fun setupRecyclerView() {
        adapter = ContactsAdapter(userList,
            onFriendClick = { selectedUser ->
                findUserIdByEmail(selectedUser.email)
            },
            onBestFriendCheck = { user, isChecked ->
                if (isChecked) {
                    addBestFriend(user.email)
                } else {
                    removeBestFriend(user.email)
                }
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadFriends() {
        database.child(currentUserId!!).child("Friends").child("Frinding")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userList.clear()
                    for (friendSnapshot in snapshot.children) {
                        val friendId = friendSnapshot.key
                        if (friendId != null) {
                            loadFriendDetails(friendId)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendLoading", "Error loading friends: ${error.message}")
                }
            })
    }

    private fun loadFriendDetails(friendId: String) {
        database.child(friendId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val user = userSnapshot.getValue(User::class.java)?.copy(userId = friendId)
                user?.let {
                    if (it.userId != currentUserId) {
                        checkBestFriendStatus(it) { isBestFriend ->
                            it.isBestFriend = isBestFriend
                            userList.add(it)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FriendLoading", "Error loading friend details: ${error.message}")
            }
        })
    }

    private fun checkBestFriendStatus(user: User, callback: (Boolean) -> Unit) {
        database.child(currentUserId!!).child("Friends").child("BestFriend").child(user.userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.exists())
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("BestFriendCheck", "Error checking best friend status: ${error.message}")
                    callback(false)
                }
            })
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
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserSearch", "Error searching by email: ${error.message}")
                }
            })
    }

    private fun sendFriendRequest(targetUserId: String) {
        if (currentUserId == targetUserId) {
            Toast.makeText(context, "You cannot add yourself", Toast.LENGTH_SHORT).show()
            return
        }

        val requestsRef = database.child(targetUserId).child("Friends").child("Requests").child(currentUserId!!)
        requestsRef.setValue(currentUserId).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Friend request sent", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to send request", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addBestFriend(email: String) {
        database.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val targetUserId = userSnapshot.key
                            if (targetUserId != null && targetUserId != currentUserId) {
                                database.child(currentUserId!!).child("Friends").child("BestFriend")
                                    .child(targetUserId).setValue(targetUserId)
                                    .addOnSuccessListener {
                                        updateBestFriendStatus(targetUserId, true)
                                        Toast.makeText(context, "Added to Best Friends", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to add to Best Friends", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("BestFriendAdd", "Error finding user: ${error.message}")
                }
            })
    }

    private fun removeBestFriend(email: String) {
        database.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val targetUserId = userSnapshot.key
                            if (targetUserId != null) {
                                database.child(currentUserId!!).child("Friends").child("BestFriend")
                                    .child(targetUserId).removeValue()
                                    .addOnSuccessListener {
                                        updateBestFriendStatus(targetUserId, false)
                                        Toast.makeText(context, "Removed from Best Friends", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to remove from Best Friends", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("BestFriendRemove", "Error finding user: ${error.message}")
                }
            })
    }

    private fun updateBestFriendStatus(userId: String, isBestFriend: Boolean) {
        val user = userList.find { it.userId == userId }
        user?.let {
            it.isBestFriend = isBestFriend
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
                addFriendButton.visibility = View.GONE
                bestFriendCheckbox.isChecked = user.isBestFriend

                Glide.with(itemView.context)
                    .load(user.UserPhoto)
                    .placeholder(R.drawable.ic_person_outline)
                    .into(userPhoto)

                bestFriendCheckbox.setOnCheckedChangeListener(null) // Сначала удаляем старый listener
                bestFriendCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    onBestFriendCheck(user, isChecked)
                }

                itemView.setOnClickListener { onFriendClick(user) }
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