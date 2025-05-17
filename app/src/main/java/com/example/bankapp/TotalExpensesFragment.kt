package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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
    private lateinit var saveExpensesBtn: Button
    private var groupId: String? = null
    private var isAdmin = false
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private val membersList = mutableListOf<User>()
    private lateinit var membersAdapter: MembersAdapter

    data class User(
        val userId: String = "",
        val name: String = "",
        val email: String = "",
        val UserPhoto: String = "",
        val isAdmin: Boolean = false
    )

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
        saveExpensesBtn = view.findViewById(R.id.saveExpensesBtn)

        setupRecyclerView()
        checkAdminStatus()
        loadGroupMembers()
        checkProductsExistence()

        val clickAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.keyboardfirst)

        mainBtn.setOnClickListener {
            mainBtn.startAnimation(clickAnimation)
            if (isAdmin) {
                openCheckDivideActivity(null)
            }
        }

        return view
    }

    private fun checkProductsExistence() {
        groupId?.let { id ->
            database.child("Groups").child(id).child("Eat")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val hasProducts = snapshot.childrenCount > 0
                        saveExpensesBtn.visibility = if (hasProducts) View.VISIBLE else View.GONE
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("TotalExpenses", "Error checking products: ${error.message}")
                        saveExpensesBtn.visibility = View.GONE
                    }
                })
        }
    }

    private fun openCheckDivideActivity(userId: String?) {
        groupId?.let { id ->
            val intent = Intent(requireContext(), CheckDivideActivity::class.java)
            intent.putExtra("GROUP_ID", id)
            userId?.let { intent.putExtra("USER_ID", it) }
            startActivity(intent)
            requireActivity().overridePendingTransition(0, 0)
        }
    }

    private fun openAddExpensesForUserActivity(userId: String) {
        groupId?.let { id ->
            val intent = Intent(requireContext(), AddExpensesForUserActivity::class.java)
            intent.putExtra("GROUP_ID", id)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
            requireActivity().overridePendingTransition(0, 0)
        }
    }

    private fun checkAdminStatus() {
        groupId?.let { id ->
            database.child("Groups").child(id).child("admin")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val adminId = snapshot.getValue(String::class.java)
                        isAdmin = adminId == currentUserId
                        mainBtn.text = if (isAdmin) "Добавить расходы:)" else "Оплатить долг"
                        mainBtn.visibility = if (isAdmin) View.VISIBLE else View.GONE
                        Log.d("AdminCheck", "Admin ID: $adminId, Current user: $currentUserId, Is admin: $isAdmin")
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("TotalExpenses", "Error checking admin status: ${error.message}")
                        isAdmin = false
                    }
                })
        } ?: run {
            isAdmin = false
        }
    }

    private fun setupRecyclerView() {
        membersAdapter = MembersAdapter(membersList, isAdmin) { userId ->
            openAddExpensesForUserActivity(userId)
        }
        membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = membersAdapter
        }
    }

    private fun loadGroupMembers() {
        groupId?.let { id ->
            // Сначала загружаем администратора
            database.child("Groups").child(id).child("admin")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(adminSnapshot: DataSnapshot) {
                        val adminId = adminSnapshot.getValue(String::class.java)
                        adminId?.let { loadUserDetails(it, true) }

                        // Затем загружаем остальных пользователей
                        database.child("Groups").child(id).child("Users")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(usersSnapshot: DataSnapshot) {
                                    for (userSnapshot in usersSnapshot.children) {
                                        val userId = userSnapshot.key
                                        userId?.let { loadUserDetails(it, false) }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e("TotalExpenses", "Error loading group members: ${error.message}")
                                }
                            })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("TotalExpenses", "Error loading admin: ${error.message}")
                    }
                })
        }
    }

    private fun loadUserDetails(userId: String, isAdminUser: Boolean) {
        database.child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)?.copy(
                        userId = userId,
                        isAdmin = isAdminUser
                    )
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

    inner class MembersAdapter(
        private val users: List<User>,
        private val isAdmin: Boolean,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<MembersAdapter.ViewHolder>() {

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

            val clickAnimation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.keyboardfirst)

            // Обработка нажатия на элемент списка
            holder.itemView.setOnClickListener {
                it.startAnimation(clickAnimation)
                onItemClick(user.userId)
            }
        }

        override fun getItemCount(): Int = users.size
    }
}