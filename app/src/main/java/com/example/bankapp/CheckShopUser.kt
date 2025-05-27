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
import com.google.firebase.database.ValueEventListener
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CheckShopUserActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var userContactBox: RecyclerView
    private lateinit var userExpensesList: RecyclerView
    private lateinit var confirmButton: AppCompatButton
    private lateinit var questionTxt: TextView
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
        val price: Int,
        val quantity: Int
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
        userExpensesList = findViewById(R.id.chooseExpensesList)
        confirmButton = findViewById(R.id.saveExpensesBtn)
        questionTxt = findViewById(R.id.txtAddQustion)

        findViewById<TextView>(R.id.mainTitle).text = "Ваши покупки"
        confirmButton.text = "Понятно, я должен(а) ###"
        questionTxt.text = "Список продуктов"
    }

    private fun setupAdapters() {
        userContactBox.layoutManager = LinearLayoutManager(this)
        userContactBox.adapter = UserAdapter(emptyList())

        expenseAdapter = ExpenseAdapter(expenseItems)
        userExpensesList.layoutManager = LinearLayoutManager(this)
        userExpensesList.adapter = expenseAdapter
    }

    private fun loadData() {
        loadUserData()
        loadUserPurchases()
    }

    private fun loadUserData() {
        database.child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)?.copy(userId = userId)
                    user?.let {
                        userContactBox.adapter = UserAdapter(listOf(it))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Ошибка загрузки данных пользователя: ${error.message}")
                }
            })
    }

    private fun loadUserPurchases() {
        database.child("Groups").child(groupId).child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    expenseItems.clear()
                    var total = 0

                    snapshot.children.forEach { product ->
                        // Пропускаем служебные поля
                        if (product.key !in listOf("total", "userId", userId)) {
                            val name = product.key ?: return@forEach
                            val price = product.child("Price").getValue(Int::class.java) ?: 0
                            val quantity = product.child("quantity").getValue(Int::class.java) ?: 0

                            expenseItems.add(ExpenseItem(name, price, quantity))
                            total += price * quantity
                        }
                    }

                    updateTotalSum(total)
                    expenseAdapter.notifyDataSetChanged()

                    if (expenseItems.isEmpty()) {
                        questionTxt.text = "У вас нет покупок в этом чеке"
                        confirmButton.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Ошибка загрузки ваших покупок: ${error.message}")
                }
            })
    }

    private fun updateTotalSum(total: Int) {
        val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))
        confirmButton.text = "Понятно, я должен(а) ${formatter.format(total)} ₽"
    }

    private fun setupClickListeners() {
        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)

        findViewById<ImageButton>(R.id.btnGoBack).setOnClickListener {
            finishWithAnimation()
        }

        confirmButton.setOnClickListener {
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
    }

    private inner class UserAdapter(private val users: List<User>) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.userName)
            val userEmail: TextView = itemView.findViewById(R.id.userPhone)
            val userPhoto: ImageView = itemView.findViewById(R.id.userPhoto)
            val addButton: ImageButton = itemView.findViewById(R.id.addFriendBtn)
            val markContactBtn: CheckBox = itemView.findViewById(R.id.markContactBtn)
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

            // Скрываем кнопки
            holder.addButton.visibility = View.GONE
            holder.markContactBtn.visibility = View.GONE

            Glide.with(holder.itemView)
                .load(user.UserPhoto.ifEmpty { null })
                .placeholder(R.drawable.ic_person_outline)
                .into(holder.userPhoto)
        }

        override fun getItemCount() = users.size
    }

    private inner class ExpenseAdapter(
        private val items: List<ExpenseItem>
    ) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.expenseTitle)
            val price: TextView = itemView.findViewById(R.id.expensePrice)
            val quantity: TextView = itemView.findViewById(R.id.expenseNum)
            val addButton: AppCompatButton = itemView.findViewById(R.id.addExpenseBtn)
            val countBox: LinearLayout = itemView.findViewById(R.id.countBox)
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
                quantity.text = " × ${item.quantity}"

                // Скрываем интерактивные элементы
                addButton.visibility = View.GONE
                countBox.visibility = View.GONE
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
        Log.e("CheckShopUser", message)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
/*
package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class CheckDivideActivity : AppCompatActivity() {
    private var groupId: String? = null
    private var userId: String? = null
    private lateinit var expensesList: RecyclerView
    private lateinit var expenseAdapter: ExpenseAdapter
    private val expenseItems = mutableListOf<ExpenseItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_check_divide)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        groupId = intent.getStringExtra("GROUP_ID")
        userId = intent.getStringExtra("USER_ID")

        val icogroupsBtn: ImageView = findViewById(R.id.groupsBtnIcon)
        icogroupsBtn.setImageResource(R.drawable.ic_groups_outline_active)
        val groupTxt: TextView = findViewById(R.id.groupsBtnText)
        groupTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))

        expensesList = findViewById(R.id.expensesList)
        expensesList.layoutManager = LinearLayoutManager(this)
        expenseAdapter = ExpenseAdapter(expenseItems)
        expensesList.adapter = expenseAdapter

        loadExpenses()

        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)

        findViewById<LinearLayout>(R.id.homeBtn).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            it.startAnimation(clickAnimation)
        }

        findViewById<LinearLayout>(R.id.groupsBtn).setOnClickListener {
            startActivity(Intent(this, GroupsActivity::class.java))
            overridePendingTransition(0, 0)
            it.startAnimation(clickAnimation)
        }

        findViewById<ImageButton>(R.id.btnGoBack).setOnClickListener {
            it.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
            finish()
        }

        findViewById<ImageButton>(R.id.btnAddExpense).setOnClickListener {
            val bottomSheet = AddExpenseBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("GROUP_ID", groupId)
                    userId?.let { putString("USER_ID", it) }
                }
            }
            bottomSheet.show(supportFragmentManager, AddExpenseBottomSheet.TAG)
        }
    }

    private fun loadExpenses() {
        if (groupId == null) return
        val dbRef = FirebaseDatabase.getInstance().reference
        dbRef.child("Groups").child(groupId!!).child("Eat")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    expenseItems.clear()
                    for (productSnapshot in snapshot.children) {
                        val name = productSnapshot.key ?: continue
                        val price = productSnapshot.child("Price").getValue(Double::class.java) ?: 0.0
                        val quantity = productSnapshot.child("quantity").getValue(Double::class.java) ?: 0.0
                        expenseItems.add(ExpenseItem(name, price, quantity))
                    }
                    expenseAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    data class ExpenseItem(val name: String, val price: Double, val quantity: Double)

    inner class ExpenseAdapter(private val items: List<ExpenseItem>) :
        RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

        inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.expenseTitle)
            val price: TextView = itemView.findViewById(R.id.expensePrice)
            val quantity: TextView = itemView.findViewById(R.id.expenseNum)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.add_purchase_for_user, parent, false)
            return ExpenseViewHolder(view)
        }

        override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.name
            holder.price.text = "${item.price} ₽"
            holder.quantity.text = " × ${item.quantity}"
        }

        override fun getItemCount() = items.size
    }
}

 */