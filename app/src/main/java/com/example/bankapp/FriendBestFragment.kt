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

class FriendBestFragment : Fragment() {

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
            targetUser.email?.let { targetEmail ->
                removeFromBestFriends(targetEmail)
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
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

    private fun removeFromBestFriends(targetEmail: String) {
        database.child("Users").orderByChild("email").equalTo(targetEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val targetUser = snapshot.children.firstOrNull()
                        targetUser?.let {
                            val targetUserId = it.key
                            if (targetUserId != null && targetUserId != currentUserId) {
                                database.child("Users").child(currentUserId!!).child("Friends")
                                    .child("BestFriend").child(targetUserId).removeValue()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Удален из лучших друзей", Toast.LENGTH_SHORT).show()
                                        loadBestFriends()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("FriendBest", "Error removing best friend: ${e.message}")
                                        Toast.makeText(context, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendBest", "Error finding user: ${error.message}")
                }
            })
    }

    inner class BestFriendsAdapter(
        private val users: List<User>,
        private val onRemoveClick: (User) -> Unit
    ) : RecyclerView.Adapter<BestFriendsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userEmail: TextView = itemView.findViewById(R.id.userPhone)
            val userPhoto: ImageView = itemView.findViewById(R.id.userPhoto)
            val bestFriendCheckbox: CheckBox = itemView.findViewById(R.id.markContactBtn)
            val removeButton: ImageButton = itemView.findViewById(R.id.addFriendBtn)

            fun bind(user: User) {
                userName.text = user.name
                userEmail.text = user.email

                // CheckBox отмечен, так как это лучший друг
                bestFriendCheckbox.isChecked = true
                bestFriendCheckbox.isEnabled = false // Делаем невозможным снятие отметки

                removeButton.visibility = View.GONE


                Glide.with(itemView.context)
                    .load(user.UserPhoto)
                    .placeholder(R.drawable.ic_person_outline)
                    .into(userPhoto)
                /*
                // Настраиваем кнопку для удаления из лучших друзей
                removeButton.setImageResource(R.drawable.ic_remove) // Установите свою иконку удаления
                removeButton.setOnClickListener {
                    onRemoveClick(user)
                }*/
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