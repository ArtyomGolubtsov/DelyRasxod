package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class AddExpensesForUserActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var userContactBox: RecyclerView
    private lateinit var chooseExpensesList: RecyclerView
    private lateinit var saveExpensesBtn: AppCompatButton
    private var groupId: String = ""
    private var userId: String = ""
    private var isUserPayed: Boolean = false

    private val expenseItems = mutableListOf<ExpenseItem>()
    private lateinit var expenseAdapter: ExpenseAdapter

    data class User(
        val userId: String = "",
        val name: String = "",
        val email: String = "",
        val UserPhoto: String = "",
        val pin: String? = null
    )

    data class ExpenseItem(
        val name: String,
        val price: Int,
        var availableQuantity: Int,
        var selectedQuantity: Int = 0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_expenses_for_user)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        groupId = intent.getStringExtra("GROUP_ID") ?: return
        userId = intent.getStringExtra("USER_ID") ?: return
        database = FirebaseDatabase.getInstance().reference

        initViews()
        setupAdapters()
        loadData()
        setupClickListeners()
    }

    private fun initViews() {
        userContactBox = findViewById(R.id.userContactBox)
        chooseExpensesList = findViewById(R.id.chooseExpensesList)
        saveExpensesBtn = findViewById(R.id.saveExpensesBtn)
    }

    private fun setupAdapters() {
        userContactBox.layoutManager = LinearLayoutManager(this)
        userContactBox.adapter = UserAdapter(emptyList())

        expenseAdapter = ExpenseAdapter(expenseItems,
            onAddClick = { item, position -> handleAddItem(item, position) },
            onIncrement = { item, position -> handleQuantityChange(item, position, 1) },
            onDecrement = { item, position -> handleQuantityChange(item, position, -1) }
        )
        chooseExpensesList.layoutManager = LinearLayoutManager(this)
        chooseExpensesList.adapter = expenseAdapter
    }

    private fun loadData() {
        loadUserPaymentStatus()
        loadUserData()
        loadGroupExpenses()
    }

    private fun loadUserPaymentStatus() {
        database.child("Groups").child(groupId).child("PayUsers").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isUserPayed = snapshot.getValue(Boolean::class.java) ?: false
                    // Обновляем адаптер после загрузки статуса оплаты
                    userContactBox.adapter?.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Если узла PayUsers нет, считаем что пользователь не оплатил
                    isUserPayed = false
                }
            })
    }

    private fun loadUserData() {
        database.child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)?.copy(userId = userId)
                    user?.let { userContactBox.adapter = UserAdapter(listOf(it)) }
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Ошибка загрузки данных пользователя: ${error.message}")
                }
            })
    }

    private fun loadGroupExpenses() {
        database.child("Groups").child(groupId).child("EatCheck")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    expenseItems.clear()
                    snapshot.children.forEach { product ->
                        val name = product.key ?: return@forEach
                        val price = product.child("Price").getValue(Int::class.java) ?: 0
                        val quantity = product.child("quantity").getValue(Int::class.java) ?: 0
                        expenseItems.add(ExpenseItem(name, price, quantity))
                    }
                    loadUserSelectedItems()
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Ошибка загрузки продуктов: ${error.message}")
                }
            })
    }

    private fun loadUserSelectedItems() {
        database.child("Groups").child(groupId).child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { product ->
                        val name = product.key ?: return@forEach
                        if (name != userId) { // Пропускаем запись о самом пользователе
                            val quantity = product.child("quantity").getValue(Int::class.java) ?: 0
                            expenseItems.find { it.name == name }?.let {
                                it.selectedQuantity = quantity
                                it.availableQuantity -= quantity
                            }
                        }
                    }
                    expenseAdapter.notifyDataSetChanged()
                    updateTotalSum()
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Ошибка загрузки выбранных товаров: ${error.message}")
                }
            })
    }

    private fun handleAddItem(item: ExpenseItem, position: Int) {
        if (item.availableQuantity > 0) {
            updateItemQuantity(item, position, 1)
        } else {
            showError("Нет доступного количества")
        }
    }

    private fun handleQuantityChange(item: ExpenseItem, position: Int, delta: Int) {
        when {
            delta > 0 && item.availableQuantity <= 0 -> showError("Недостаточно товара")
            delta < 0 && item.selectedQuantity <= 0 -> return
            else -> updateItemQuantity(item, position, delta)
        }
    }

    private fun updateItemQuantity(item: ExpenseItem, position: Int, delta: Int) {
        val eatCheckRef = database.child("Groups").child(groupId).child("EatCheck").child(item.name)
        val userRef = database.child("Groups").child(groupId).child("Users").child(userId).child(item.name)

        eatCheckRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.child("quantity").getValue(Int::class.java) ?: return Transaction.abort()
                if (current - delta < 0) return Transaction.abort()
                currentData.child("quantity").value = current - delta
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (committed) {
                    userRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentUserData: MutableData): Transaction.Result {
                            val currentUserQty = currentUserData.child("quantity").getValue(Int::class.java) ?: 0
                            currentUserData.child("quantity").value = currentUserQty + delta
                            currentUserData.child("Price").value = item.price * (currentUserQty + delta)
                            return Transaction.success(currentUserData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                            runOnUiThread {
                                if (committed) {
                                    item.availableQuantity -= delta
                                    item.selectedQuantity += delta
                                    expenseAdapter.notifyItemChanged(position)
                                    updateTotalSum()
                                } else {
                                    showError("Ошибка обновления количества")
                                }
                            }
                        }
                    })
                } else {
                    showError("Ошибка обновления количества")
                }
            }
        })
    }

    private fun updateTotalSum() {
        val total = expenseItems.sumOf { it.price * it.selectedQuantity }
        val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))
        saveExpensesBtn.text = "Сохранить сумму (${formatter.format(total)} ₽)"
    }

    private fun setupClickListeners() {
        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)

        findViewById<ImageButton>(R.id.btnGoBack).setOnClickListener {
            finishWithAnimation()
        }

        saveExpensesBtn.setOnClickListener {
            finishWithAnimation()
        }

        initBottomMenu(clickAnimation)
    }

    private fun initBottomMenu(clickAnimation: android.view.animation.Animation) {
        findViewById<ImageView>(R.id.groupsBtnIcon).setImageResource(R.drawable.ic_groups_outline_active)
        findViewById<TextView>(R.id.groupsBtnText).setTextColor(ContextCompat.getColor(this, R.color.dely_blue))

        listOf(
            R.id.homeBtn to MainActivity::class.java,
            R.id.groupsBtn to GroupsActivity::class.java
        ).forEach { (viewId, activityClass) ->
            findViewById<LinearLayout>(viewId).setOnClickListener {
                startActivity(Intent(this, activityClass))
                overridePendingTransition(0, 0)
                it.startAnimation(clickAnimation)
            }
        }
        val proflBtn: LinearLayout = findViewById(R.id.profileBtn)
        proflBtn.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
            proflBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }
    }

    private inner class UserAdapter(private val users: List<User>) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userEmail: TextView = itemView.findViewById(R.id.userPhone)
            val userPhoto: ImageView = itemView.findViewById(R.id.userPhoto)
            val addButton: ImageButton = itemView.findViewById(R.id.addFriendBtn)
            val markContactBtn: CheckBox = itemView.findViewById(R.id.markContactBtn)
            val payButton: AppCompatButton = itemView.findViewById(R.id.payUsers)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.contacts_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.userName.text = user.name
            holder.userEmail.text = user.email
            Glide.with(holder.itemView)
                .load(user.UserPhoto.ifEmpty { null })
                .placeholder(R.drawable.ic_person_outline)
                .into(holder.userPhoto)
            holder.addButton.visibility = View.GONE
            holder.markContactBtn.visibility = View.GONE

            // Настройка кнопки оплаты
            holder.payButton.visibility = if (!isUserPayed) View.VISIBLE else View.GONE
            if (!isUserPayed) {
                holder.payButton.setOnClickListener {
                    updatePaymentStatus(true)
                    holder.payButton.setBackgroundResource(R.drawable.btn_back_btn_bg)
                    holder.payButton.visibility = View.GONE
                }
            }
        }

        override fun getItemCount() = users.size
    }

    private fun updatePaymentStatus(payed: Boolean) {
        database.child("Groups").child(groupId).child("PayUsers").child(userId)
            .setValue(payed)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    isUserPayed = payed
                    Toast.makeText(this, "Статус оплаты обновлен", Toast.LENGTH_SHORT).show()
                } else {
                    showError("Ошибка обновления статуса оплаты")
                }
            }
    }

    private inner class ExpenseAdapter(
        private val items: List<ExpenseItem>,
        private val onAddClick: (ExpenseItem, Int) -> Unit,
        private val onIncrement: (ExpenseItem, Int) -> Unit,
        private val onDecrement: (ExpenseItem, Int) -> Unit
    ) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.expenseTitle)
            val price: TextView = itemView.findViewById(R.id.expensePrice)
            val quantity: TextView = itemView.findViewById(R.id.expenseNum)
            val addButton: AppCompatButton = itemView.findViewById(R.id.addExpenseBtn)
            val countBox: LinearLayout = itemView.findViewById(R.id.countBox)
            val counter: TextView = itemView.findViewById(R.id.tv_counter)
            val incrementBtn: ImageButton = itemView.findViewById(R.id.btn_increment)
            val decrementBtn: ImageButton = itemView.findViewById(R.id.btnAddExpense)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.add_purchase_for_user, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            with(holder) {
                title.text = item.name
                price.text = "${item.price} ₽"
                quantity.text = " × ${item.availableQuantity}"

                if (item.selectedQuantity > 0) {
                    addButton.visibility = View.GONE
                    countBox.visibility = View.VISIBLE
                    counter.text = item.selectedQuantity.toString()
                } else {
                    addButton.visibility = View.VISIBLE
                    countBox.visibility = View.GONE
                }

                addButton.setOnClickListener { onAddClick(item, position) }
                incrementBtn.setOnClickListener { onIncrement(item, position) }
                decrementBtn.setOnClickListener { onDecrement(item, position) }
            }
        }

        override fun getItemCount() = items.size
    }

    private fun finishWithAnimation() {
        finish()
        overridePendingTransition(0, 0)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e("AddExpenses", message)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}