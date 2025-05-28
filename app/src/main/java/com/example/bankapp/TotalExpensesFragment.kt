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
import android.widget.Toast
import androidx.core.content.ContextCompat
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
    private val membersList = mutableListOf<User>()
    private lateinit var membersAdapter: MembersAdapter

    data class User(
        val userId: String = "",
        val name: String = "",
        val email: String = "",
        val UserPhoto: String = "",
        val isAdmin: Boolean = false,
        var paymentStatus: String? = null // Поле для статуса оплаты
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



        setupRecyclerView()
        checkAdminStatus()
        loadGroupMembers()

        val clickAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.keyboardfirst)

        mainBtn.setOnClickListener {
            mainBtn.startAnimation(clickAnimation)
            if (isAdmin) {
                openCheckDivideActivity(null)
            }
            else
            {
                getAdminIdAndOpenRequesits(currentUserId, groupId)
            }
        }

        return view
    }

    private fun getAdminIdAndOpenRequesits(userId: String?, groupId: String?) {
        groupId?.let { id ->
            database.child("Groups").child(id).child("admin")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val adminId = snapshot.getValue(String::class.java)
                        openRequesitsActivity(userId, id, adminId)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("TotalExpenses", "Error getting admin ID: ${error.message}")
                    }
                })
        }
    }

    // Метод для открытия RequesitsActivity
    private fun openRequesitsActivity(userId: String?, groupId: String?, adminId: String?) {
        val intent = Intent(requireContext(), RequesitsActivity::class.java).apply {
            putExtra("USER_ID", userId)
            putExtra("GROUP_ID", groupId)
            putExtra("ADMIN_ID", adminId)  // Передаем ID админа
        }
        startActivity(intent)
        requireActivity().overridePendingTransition(0, 0)
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

    private fun openCheckShopUser(userId: String) {
        groupId?.let { id ->
            val intent = Intent(requireContext(), CheckShopUserActivity::class.java).apply {
                putExtra("GROUP_ID", id)
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
            requireActivity().overridePendingTransition(0, 0)
        } ?: run {
            Toast.makeText(requireContext(), "Ошибка: ID группы не установлен", Toast.LENGTH_SHORT).show()
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
                        membersAdapter.isAdmin = isAdmin
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
        membersAdapter = MembersAdapter(membersList, currentUserId) { userId ->
            if (isAdmin) {
                openAddExpensesForUserActivity(userId)
            } else {
                currentUserId?.let { openCheckShopUser(it) }
            }
        }
        membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = membersAdapter
        }
    }

    private fun loadGroupMembers() {
        groupId?.let { id ->
            database.child("Groups").child(id).child("admin")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(adminSnapshot: DataSnapshot) {
                        val adminId = adminSnapshot.getValue(String::class.java)
                        adminId?.let { loadUserDetails(it, true) }

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
                            checkPaymentStatus(userId) // Проверка статуса оплаты
                            membersAdapter.notifyDataSetChanged()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TotalExpenses", "Error loading user details: ${error.message}")
                }
            })
    }

    private fun checkPaymentStatus(userId: String) {
        database.child("Groups").child(groupId!!).child("PayUsers").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.getValue(Boolean::class.java)
                    updatePaymentStatus(userId, status)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TotalExpenses", "Error observing payment status: ${error.message}")
                }
            })
    }

    private fun updatePaymentStatus(userId: String, status: Boolean?) {
        val userIndex = membersList.indexOfFirst { it.userId == userId }
        if (userIndex != -1) {
            val paymentStatus = when (status) {
                true -> "Оплатил"
                null -> "Не оплатил"
                false -> "Подтвердить платеж"
            }
            val textColor = when (status) {
                true -> R.color.ctgr_party
                null -> R.color.alert_red
                false -> R.color.ctgr_other
            }

            // Обновляем поле paymentStatus для пользователя
            membersList[userIndex] = membersList[userIndex].copy(paymentStatus = paymentStatus)

            // Уведомление адаптера об изменениях
            membersAdapter.notifyItemChanged(userIndex)

            // Устанавливаем статус оплаты в адаптере
            membersAdapter.updatePaymentText(userIndex, paymentStatus, textColor)
        }
    }

    inner class MembersAdapter(
        private val users: List<User>,
        private val currentUserId: String?,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<MembersAdapter.ViewHolder>() {

        var isAdmin = false

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userEmail: TextView = itemView.findViewById(R.id.userPhone)
            val userPhoto: ImageView = itemView.findViewById(R.id.userPhoto)
            val addButton: ImageButton = itemView.findViewById(R.id.addFriendBtn)
            val bestFriendCheckbox: CheckBox = itemView.findViewById(R.id.markContactBtn)
            val paymentStatusText: TextView = itemView.findViewById(R.id.contactSumOrStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.contacts_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            val isCurrentUser = user.userId == currentUserId
            holder.paymentStatusText.visibility = View.VISIBLE
            // Устанавливаем текст и цвет в зависимости от того, текущий это пользователь или нет
            holder.userName.text = if (isCurrentUser) "Вы" else user.name
            holder.userName.setTextColor(
                if (isCurrentUser) {
                    ContextCompat.getColor(holder.itemView.context, R.color.ctgr_party)
                } else {
                    ContextCompat.getColor(holder.itemView.context, R.color.white)
                }
            )

            holder.userEmail.text = user.email

            Glide.with(holder.itemView.context)
                .load(user.UserPhoto.ifEmpty { null })
                .placeholder(R.drawable.ic_person_outline)
                .into(holder.userPhoto)

            holder.addButton.visibility = View.GONE
            holder.bestFriendCheckbox.visibility = View.GONE

            // Устанавливаем текст статуса оплаты
            holder.paymentStatusText.text = user.paymentStatus
            holder.paymentStatusText.setTextColor(
                ContextCompat.getColor(holder.itemView.context, when (user.paymentStatus) {
                    "Оплатил" -> R.color.ctgr_party
                    "Подтвердить платеж" -> R.color.ctgr_other
                    "Не оплатил" -> R.color.alert_red
                    else -> R.color.white // По умолчанию
                })
            )

            val clickAnimation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.keyboardfirst)

            holder.itemView.setOnClickListener {
                it.startAnimation(clickAnimation)
                onItemClick(user.userId)
            }

            // Показываем иконку администратора
            if (user.isAdmin) {
                holder.paymentStatusText.visibility = View.GONE
                holder.bestFriendCheckbox.visibility = View.VISIBLE
                holder.bestFriendCheckbox.isChecked = true
            }
        }

        // Метод для обновления текста статуса оплаты
        fun updatePaymentText(position: Int, statusText: String, colorId: Int) {
            if (position in users.indices) {
                notifyItemChanged(position)
                val holder = membersRecyclerView.findViewHolderForAdapterPosition(position) as? ViewHolder
                holder?.paymentStatusText?.text = statusText
                holder?.paymentStatusText?.setTextColor(ContextCompat.getColor(membersRecyclerView.context, colorId))
            }
        }

        override fun getItemCount(): Int = users.size
    }
}
