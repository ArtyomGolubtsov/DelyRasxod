package com.example.bankapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class FriendRequestsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FriendRequestsAdapter
    private val requestList = mutableListOf<User>()
    private val database = FirebaseDatabase.getInstance().reference
    private var currentUserId: String? = null

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
        loadFriendRequests()

        return view
    }

    private fun setupRecyclerView() {
        adapter = FriendRequestsAdapter(requestList) { targetUser ->
            targetUser.email?.let { targetEmail ->
                addFriend(currentUserId!!, targetEmail)
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadFriendRequests() {
        database.child("Users").child(currentUserId!!).child("Friends").child("Requests")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    requestList.clear()
                    for (requestSnapshot in snapshot.children) {
                        val requesterId = requestSnapshot.key
                        requesterId?.let { loadRequesterInfo(it) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendRequests", "Error loading requests: ${error.message}")
                }
            })
    }

    private fun loadRequesterInfo(userId: String) {
        database.child("Users").child(userId).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        if (it.userId != currentUserId) {
                            requestList.add(it)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendRequests", "Error loading user: ${error.message}")
                }
            })
    }

    private fun addFriend(currentUserId: String, targetEmail: String) {
        // Находим ID пользователя по email
        database.child("Users").orderByChild("email").equalTo(targetEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val targetUser = snapshot.children.firstOrNull()
                        targetUser?.let {
                            val targetUserId = it.key
                            if (targetUserId != null && targetUserId != currentUserId) {
                                // Добавляем взаимную дружбу
                                addMutualFriendship(currentUserId, targetUserId)
                            } else {
                                Toast.makeText(context, "Нельзя добавить самого себя", Toast.LENGTH_SHORT).show()
                            }
                        } ?: run {
                            Toast.makeText(context, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendRequests", "Error finding user: ${error.message}")
                    Toast.makeText(context, "Ошибка поиска пользователя", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun addMutualFriendship(currentUserId: String, targetUserId: String) {
        // Добавляем в друзья у текущего пользователя
        database.child("Users").child(currentUserId).child("Friends").child("Frinding")
            .child(targetUserId).setValue(targetUserId)
            .addOnSuccessListener {
                // Добавляем в друзья у целевого пользователя
                database.child("Users").child(targetUserId).child("Friends").child("Frinding")
                    .child(currentUserId).setValue(currentUserId)
                    .addOnSuccessListener {
                        // Удаляем запрос на дружбу
                        database.child("Users").child(currentUserId).child("Friends")
                            .child("Requests").child(targetUserId).removeValue()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Друг успешно добавлен", Toast.LENGTH_SHORT).show()
                                loadFriendRequests()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FriendRequests", "Error adding friend: ${e.message}")
                        Toast.makeText(context, "Ошибка добавления друга", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FriendRequests", "Error adding friend: ${e.message}")
                Toast.makeText(context, "Ошибка добавления друга", Toast.LENGTH_SHORT).show()
            }
    }

    inner class FriendRequestsAdapter(
        private val users: List<User>,
        private val onAcceptClick: (User) -> Unit
    ) : RecyclerView.Adapter<FriendRequestsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userEmail: TextView = itemView.findViewById(R.id.userPhone)
            val userPhoto: ImageView = itemView.findViewById(R.id.userPhoto)
            val acceptButton: ImageButton = itemView.findViewById(R.id.addFriendBtn)
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

            holder.acceptButton.setOnClickListener {
                onAcceptClick(user)
            }
        }

        override fun getItemCount(): Int = users.size
    }
}