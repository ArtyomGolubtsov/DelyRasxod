package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TotalExpensesFragment : Fragment() {

    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var database: DatabaseReference
    private lateinit var mainBtn: Button
    private var groupId: String? = null
    private var isAdmin = false
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private val membersList = mutableListOf<GroupInfoFragment.User>()
    private lateinit var membersAdapter: MembersAdapter

    companion object {
        fun newInstance(groupId: String): TotalExpensesFragment {
            return TotalExpensesFragment().apply {
                arguments = Bundle().apply {
                    putString("GROUP_ID", groupId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = arguments?.getString("GROUP_ID")
        database = FirebaseDatabase.getInstance().reference
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_total_expenses, container, false)
        membersRecyclerView = view.findViewById(R.id.membersList)
        mainBtn = view.findViewById(R.id.mainBtn)

        setupRecyclerView()
        checkAdminStatus()
        loadGroupMembers()

        // Обработка нажатия кнопки
        mainBtn.setOnClickListener {
            if (isAdmin) {
                openAddExpensesActivity()
            } else {

            }
        }

        return view
    }

    private fun openAddExpensesActivity() {
        groupId?.let { id ->
            val intent = Intent(requireContext(), AddExpensesForUserActivity::class.java)
            intent.putExtra("GROUP_ID", id)
            startActivity(intent)
        }
    }

    private fun checkAdminStatus() {
        groupId?.let { id ->
            database.child("Groups").child(id).child("Users").child("admin")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val adminId = snapshot.getValue(String::class.java)
                        isAdmin = adminId == currentUserId
                        mainBtn.text = if (isAdmin) "Оплатить долг" else "Добавить расходы:)"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("TotalExpenses", "Error checking admin status: ${error.message}")
                        isAdmin = false
                    }
                })
        }
    }

    private fun setupRecyclerView() {
        membersAdapter = MembersAdapter(membersList)
        membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = membersAdapter
        }
    }

    private fun loadGroupMembers() {
        groupId?.let { id ->
            database.child("Groups").child(id).child("Users")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        membersList.clear()
                        for (memberSnapshot in snapshot.children) {
                            if (memberSnapshot.key != "admin") {
                                val userId = memberSnapshot.key
                                userId?.let { loadUserDetails(it) }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("TotalExpenses", "Error loading group members: ${error.message}")
                    }
                })
        }
    }

    private fun loadUserDetails(userId: String) {
        database.child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(GroupInfoFragment.User::class.java)?.copy(userId = userId)
                    user?.let {
                        if (!membersList.any { existing -> existing.userId == userId }) {
                            membersList.add(it)
                            membersAdapter.notifyDataSetChanged()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TotalExpenses", "Error loading user details: ${error.message}")
                }
            })
    }

    inner class MembersAdapter(private val users: List<GroupInfoFragment.User>) :
        RecyclerView.Adapter<MembersAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userEmail: TextView = itemView.findViewById(R.id.userPhone)
            val userPhoto: ImageView = itemView.findViewById(R.id.userPhoto)
            val addButton: ImageButton = itemView.findViewById(R.id.addFriendBtn)
            val bestFriendCheckbox: CheckBox = itemView.findViewById(R.id.markContactBtn)
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
                .load(user.UserPhoto.ifEmpty { null })
                .placeholder(R.drawable.ic_person_outline)
                .into(holder.userPhoto)

            holder.addButton.visibility = View.GONE
            holder.bestFriendCheckbox.visibility = View.GONE
        }

        override fun getItemCount(): Int = users.size
    }
}