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
import com.google.firebase.database.*

class AllContactsFragment : Fragment() {

    // Модель пользователя должна быть определена в этом же файле или импортирована
    data class User(
        val userId: String = "",
        val name: String = "",
        val email: String = "",
        val UserPhoto: String = "",
        val pin: String? = null
    )
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
        loadFriends()
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

    private fun loadFriends() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Log.e("UserLoading", "User not authenticated")
            return
        }

        // Загружаем друзей текущего пользователя
        database.child(currentUserId).child("Friends").child("Frinding")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userList.clear()
                    for (friendSnapshot in snapshot.children) {
                        val friendId = friendSnapshot.key
                        if (friendId != null) {
                            // Загружаем данные о друге по его ID
                            database.child(friendId).addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnapshot: DataSnapshot) {
                                    val user = userSnapshot.getValue(User::class.java)
                                    user?.let {
                                        if (it.userId != currentUserId) { // Исключаем текущего пользователя
                                            Log.d("FriendLoading", "Loaded friend: ${it.name}, ID: ${it.userId}")
                                            userList.add(it)
                                            adapter.notifyDataSetChanged()
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e("FriendLoading", "Error loading friend: ${error.message}")
                                }
                            })
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendLoading", "Error loading friends: ${error.message}")
                    Toast.makeText(context, "Error loading friends", Toast.LENGTH_SHORT).show()
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

        if (currentUserId == targetUserId) {
            Toast.makeText(context, "You cannot add yourself as a friend", Toast.LENGTH_SHORT).show()
            return
        }

        val requestsRef = database.child(targetUserId).child("Friends").child("Requests").child(currentUserId)
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

                // Для списка друзей скрываем кнопку добавления
                addFriendButton.visibility = View.GONE
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
    }
}