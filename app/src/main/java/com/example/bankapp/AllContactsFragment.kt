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
        searchEditText = requireActivity().findViewById(R.id.membersSearch) // Исправлено на 'view' вместо 'requireActivity()'

        setupRecyclerView()
        loadUsers()
        setupSearch()

        return view
    }

    private fun setupRecyclerView() {
        adapter = ContactsAdapter(userList) { selectedUser ->
            sendFriendRequest(selectedUser)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadUsers() {
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
                adapter.notifyDataSetChanged() // Обновляем адаптер после загрузки данных
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserLoading", "Error loading users: ${error.message}")
                Toast.makeText(context, "Error loading users: ${error.message}", Toast.LENGTH_SHORT).show() // Добавлено сообщение для пользователя
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

    private fun sendFriendRequest(selectedUser: User) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val targetUserId = selectedUser.userId
        if (targetUserId.isNotEmpty()) {
            val requestsRef = FirebaseDatabase.getInstance().reference
                .child("Users")
                .child(targetUserId)
                .child("Friends")
                .child("Requests")
                .child(currentUserId)

            requestsRef.setValue(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Friend request sent to ${selectedUser.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to send friend request: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Selected user ID is invalid", Toast.LENGTH_SHORT).show()
        }
    }

    inner class ContactsAdapter(private var users: List<User>, private val clickListener: (User) -> Unit) :
        RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

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
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.contacts_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.userName.text = user.name
            holder.userPhone.text = user.email

            Glide.with(holder.itemView.context)
                .load(user.UserPhoto)
                .placeholder(R.drawable.ic_person_outline)
                .into(holder.userPhoto)
        }

        override fun getItemCount() = users.size

        fun updateList(newList: List<User>) {
            users = newList
            notifyDataSetChanged() // Обновляем адаптер после изменения списка
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
