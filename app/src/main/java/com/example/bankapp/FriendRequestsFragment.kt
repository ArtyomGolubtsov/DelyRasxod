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
    private lateinit var adapter: RequestsAdapter
    private val requestList = mutableListOf<User>()
    private val database = FirebaseDatabase.getInstance().reference.child("Users")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend_requests, container, false)
        recyclerView = view.findViewById(R.id.requestsList)

        setupRecyclerView()
        loadFriendRequests()

        return view
    }

    private fun setupRecyclerView() {
        adapter = RequestsAdapter(requestList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadFriendRequests() {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        currentUserEmail?.let { email ->
            database.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val userId =
                                snapshot.children.first().key // Получаем ID текущего пользователя
                            userId?.let { id ->
                                database.child(id).child("Friends").child("Requests")
                                    .addValueEventListener(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            requestList.clear()
                                            for (requestSnapshot in snapshot.children) {
                                                val requestedUserId = requestSnapshot.key
                                                requestedUserId?.let {
                                                    loadUserById(it) // Загружаем информацию о пользователе по его ID
                                                }
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Log.e(
                                                "RequestLoading",
                                                "Error loading requests: ${error.message}"
                                            )
                                            Toast.makeText(
                                                context,
                                                "Error loading requests: ${error.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                            }
                        } else {
                            Log.e("UserLoading", "User not found with email: $email")
                            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RequestLoading", "Error loading user email: ${error.message}")
                        Toast.makeText(
                            context,
                            "Error loading user email: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    private fun loadUserById(userId: String) {
        database.child(userId).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(User::class.java)
            user?.let {
                requestList.add(it)
                Log.d("UserLoading", "Loaded user: ${it.name}, ID: ${it.userId}")
                adapter.notifyDataSetChanged() // Обновляем адаптер при добавлении нового пользователя
            }
        }.addOnFailureListener {
            Log.e("UserLoading", "Error loading user: ${it.message}")
        }
    }

    private fun acceptFriendRequest(currentUserId: String, targetUserId: String) {
        // Удаляем запрос на дружбу
        database.child(currentUserId).child("Friends").child("Requests").child(targetUserId)
            .removeValue { error, _ ->
                if (error == null) {
                    // Добавляем в друзья, только если удаление прошло успешно
                    database.child(currentUserId).child("Friends").child("Frinding")
                        .child(targetUserId).setValue("33")
                    database.child(targetUserId).child("Friends").child("Frinding")
                        .child(currentUserId).setValue("88")

                    Toast.makeText(context, targetUserId, Toast.LENGTH_SHORT).show()

                    // Обновляем список запросов после принятия
                    loadFriendRequests() // Вызываем загрузку запросов вместе с обновлением
                } else {
                    Toast.makeText(context, "Error removing friend request", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun sendFriendRequest(targetUserId: String) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        if (currentUserEmail == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        database.orderByChild("email").equalTo(currentUserEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val currentUserId = snapshot.children.first().key
                        if (currentUserId != null) {
                            val requestsRef =
                                database.child(targetUserId).child("Friends").child("Requests")
                                    .child(currentUserId)

                            requestsRef.setValue(true).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "Friend request sent",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to send friend request: ${task.exception?.localizedMessage}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SendRequestError", "Error sending friend request: ${error.message}")
                }
            })
    }

    inner class RequestsAdapter(private var users: List<User>) :
        RecyclerView.Adapter<RequestsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userPhone: TextView = itemView.findViewById(R.id.userPhone)
            val userPhoto: ImageView = itemView.findViewById(R.id.userPhoto)

            init {
                itemView.findViewById<ImageButton>(R.id.addFriendBtn).setOnClickListener {
                    val user = users[adapterPosition]
                    // Принять запрос на дружбу, передавая оба идентификатора
                    val targetEmail = user.email
                    database.orderByChild("email").equalTo(targetEmail)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    val targetUserId = snapshot.children.first().key
                                    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
                                    currentUserEmail?.let { email ->
                                        database.orderByChild("email").equalTo(email)
                                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(snapshot: DataSnapshot) {
                                                    if (snapshot.exists()) {
                                                        val currentUserId = snapshot.children.first().key
                                                        if (currentUserId != null && targetUserId != null) {
                                                            acceptFriendRequest(currentUserId, targetUserId)
                                                            Log.d("IDE", "Current User ID: $currentUserId")
                                                            Log.d("IDE", "Target User ID: $targetUserId")
                                                            refreshFragment()
                                                        }
                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                    Log.e(
                                                        "AcceptRequestError",
                                                        "Error finding current user: ${error.message}"
                                                    )
                                                }
                                            })
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("FindError", "Error finding user by email: ${error.message}")
                            }
                        })
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
    }

    private fun refreshFragment() {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.detach(this) // Удаляем фрагмент из родительского контейнера
        transaction.attach(this) // Прикрепляем его снова
        transaction.commit()
    }

}
