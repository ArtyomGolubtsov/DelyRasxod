package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class AddExpensesForUserActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var userContactBox: RecyclerView
    private lateinit var chooseExpensesList: RecyclerView
    private lateinit var saveExpensesBtn: TextView
    private var groupId: String = ""
    private var userId: String = ""

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
        val price: Double,
        val quantity: Double,
        var isSelected: Boolean = false,
        var count: Int = 0
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

        // Получаем переданные данные
        groupId = intent.getStringExtra("GROUP_ID") ?: ""
        userId = intent.getStringExtra("USER_ID") ?: ""

        // Инициализация Firebase
        database = FirebaseDatabase.getInstance().reference

        // Инициализация UI элементов
        userContactBox = findViewById(R.id.userContactBox)
        chooseExpensesList = findViewById(R.id.chooseExpensesList)
        saveExpensesBtn = findViewById(R.id.saveExpensesBtn)

        // Настройка RecyclerView для пользователя
        userContactBox.layoutManager = LinearLayoutManager(this)
        userContactBox.adapter = UserAdapter(emptyList())

        // Настройка RecyclerView для списка продуктов
        expenseAdapter = ExpenseAdapter(expenseItems) { item, position ->
            updateTotalSum()
            expenseAdapter.notifyItemChanged(position)
        }
        chooseExpensesList.layoutManager = LinearLayoutManager(this)
        chooseExpensesList.adapter = expenseAdapter

        // Загрузка данных
        loadUserData()
        loadExpenses()

        // Анимация клика
        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)

        // Инициализация нижнего меню
        initBottomMenu(clickAnimation)

        // Кнопка "Назад"
        val searchContactsBtn: ImageButton = findViewById(R.id.btnGoBack)
        searchContactsBtn.setOnClickListener {
            searchContactsBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
            finish()
        }

        // Кнопка сохранения
        saveExpensesBtn.setOnClickListener {
            it.startAnimation(clickAnimation)
            saveSelectedExpenses()
        }
    }

    private fun loadUserData() {
        if (userId.isNotEmpty()) {
            database.child("Users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)?.copy(userId = userId)
                        user?.let {
                            userContactBox.adapter = UserAdapter(listOf(it))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("AddExpenses", "Error loading user data: ${error.message}")
                    }
                })
        }
    }

    private fun loadExpenses() {
        if (groupId.isEmpty()) return

        // Загружаем общий список продуктов
        database.child("Groups").child(groupId).child("Eat")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    expenseItems.clear()

                    // Сначала загружаем все доступные продукты
                    for (productSnapshot in snapshot.children) {
                        val name = productSnapshot.key ?: continue
                        val price = productSnapshot.child("Price").getValue(Double::class.java) ?: 0.0
                        val quantity = productSnapshot.child("quantity").getValue(Double::class.java) ?: 0.0

                        expenseItems.add(ExpenseItem(name, price, quantity))
                    }

                    // Затем загружаем уже выбранные пользователем продукты
                    loadUserSelectedExpenses()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AddExpenses", "Error loading expenses: ${error.message}")
                }
            })
    }

    private fun loadUserSelectedExpenses() {
        if (groupId.isEmpty() || userId.isEmpty()) return

        database.child("Groups").child(groupId).child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (productSnapshot in snapshot.children) {
                        val productName = productSnapshot.key ?: continue
                        val quantity = productSnapshot.child("quantity").getValue(Double::class.java) ?: 0.0
                        val price = productSnapshot.child("Price").getValue(Double::class.java) ?: 0.0

                        // Обновляем соответствующий продукт в списке
                        expenseItems.find { it.name == productName }?.let { item ->
                            item.count = quantity.toInt()
                            item.isSelected = quantity > 0
                        }
                    }

                    expenseAdapter.notifyDataSetChanged()
                    updateTotalSum()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AddExpenses", "Error loading user expenses: ${error.message}")
                }
            })
    }

    private fun updateTotalSum() {
        val totalSum = expenseItems.sumOf { item ->
            if (item.isSelected) {
                item.price * item.count // Умножаем цену на количество
            } else {
                0.0
            }
        }

        // Форматируем сумму с разделителями тысяч и двумя знаками после запятой
        val formatter = DecimalFormat("#,###.00", DecimalFormatSymbols(Locale.US))
        val formattedSum = formatter.format(totalSum).replace(",", " ")
        saveExpensesBtn.text = "Сохранить сумму ($formattedSum ₽)"
    }

    private fun saveSelectedExpenses() {
        val selectedItems = expenseItems.filter { it.count > 0 }
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Выберите хотя бы один продукт", Toast.LENGTH_SHORT).show()
            return
        }

        val groupRef = database.child("Groups").child(groupId)
        val updates = HashMap<String, Any>()

        // 1. Проверяем, что всех продуктов достаточно
        for (item in selectedItems) {
            if (item.count > item.quantity) {
                Toast.makeText(this,
                    "Недостаточно ${item.name} (доступно: ${item.quantity})",
                    Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 2. Формируем обновления для базы данных
        for (item in selectedItems) {
            // Уменьшаем общее количество в Eat
            updates["Eat/${item.name}/quantity"] = item.quantity - item.count

            // Добавляем/обновляем у пользователя
            val userProductPath = "Users/$userId/${item.name}"
            updates["$userProductPath/Price"] = item.price * item.count
            updates["$userProductPath/quantity"] = item.count
        }

        // 3. Применяем все обновления атомарно
        groupRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Продукты успешно добавлены", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("SaveExpenses", "Error saving data", e)
            }
    }

    private fun initBottomMenu(clickAnimation: android.view.animation.Animation) {
        // Иконки нижнего меню
        val icogroupsBtn: ImageView = findViewById(R.id.groupsBtnIcon)
        icogroupsBtn.setImageResource(R.drawable.ic_groups_outline_active)
        val groupTxt: TextView = findViewById(R.id.groupsBtnText)
        groupTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))

        // Обработчики нижнего меню
        val mainBtn: LinearLayout = findViewById(R.id.homeBtn)
        mainBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            mainBtn.startAnimation(clickAnimation)
        }

        val groupsBtn: LinearLayout = findViewById(R.id.groupsBtn)
        groupsBtn.setOnClickListener {
            startActivity(Intent(this, GroupsActivity::class.java))
            groupsBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }
    }

    inner class UserAdapter(private val users: List<User>) :
        RecyclerView.Adapter<UserAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userEmail: TextView = itemView.findViewById(R.id.userPhone)
            val userPhoto: ImageView = itemView.findViewById(R.id.userPhoto)
            val addButton: ImageButton = itemView.findViewById(R.id.addFriendBtn)
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
        }

        override fun getItemCount(): Int = users.size
    }

    inner class ExpenseAdapter(
        private val items: List<ExpenseItem>,
        private val onItemClick: (ExpenseItem, Int) -> Unit
    ) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

        inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.expenseTitle)
            val price: TextView = itemView.findViewById(R.id.expensePrice)
            val quantity: TextView = itemView.findViewById(R.id.expenseNum)
            val addButton: AppCompatButton = itemView.findViewById(R.id.addExpenseBtn)
            val countBox: LinearLayout = itemView.findViewById(R.id.countBox)
            val counterText: TextView = itemView.findViewById(R.id.tv_counter)
            val incrementBtn: ImageButton = itemView.findViewById(R.id.btn_increment)
            val decrementBtn: ImageButton = itemView.findViewById(R.id.btnAddExpense)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.add_purchase_for_user, parent, false)
            return ExpenseViewHolder(view)
        }

        override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.name
            holder.price.text = "${"%.2f".format(item.price)} ₽"
            holder.quantity.text = " × ${item.quantity}"

            // Устанавливаем видимость кнопок
            if (item.count > 0) {
                holder.addButton.visibility = View.GONE
                holder.countBox.visibility = View.VISIBLE
                holder.counterText.text = item.count.toString()
            } else {
                holder.addButton.visibility = View.VISIBLE
                holder.countBox.visibility = View.GONE
            }

            // Обработка нажатия на кнопку "Добавить"
            holder.addButton.setOnClickListener {
                item.count = 1
                item.isSelected = true
                holder.addButton.visibility = View.GONE
                holder.countBox.visibility = View.VISIBLE
                holder.counterText.text = "1"
                onItemClick(item, position)
            }

            // Обработка увеличения количества
            holder.incrementBtn.setOnClickListener {
                if (item.count < item.quantity) {
                    item.count++
                    holder.counterText.text = item.count.toString()
                    onItemClick(item, position)
                } else {
                    Toast.makeText(holder.itemView.context,
                        "Максимальное количество: ${item.quantity}",
                        Toast.LENGTH_SHORT).show()
                }
            }

            // Обработка уменьшения количества
            holder.decrementBtn.setOnClickListener {
                if (item.count > 0) {
                    item.count--
                    holder.counterText.text = item.count.toString()

                    if (item.count == 0) {
                        item.isSelected = false
                        holder.addButton.visibility = View.VISIBLE
                        holder.countBox.visibility = View.GONE
                    }
                    onItemClick(item, position)
                }
            }
        }

        override fun getItemCount() = items.size
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}