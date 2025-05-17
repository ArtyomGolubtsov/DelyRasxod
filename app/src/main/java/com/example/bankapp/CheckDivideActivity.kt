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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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

        // Получаем переданные значения
        groupId = intent.getStringExtra("GROUP_ID")
        userId = intent.getStringExtra("USER_ID")

        val icogroupsBtn: ImageView = findViewById(R.id.groupsBtnIcon)
        icogroupsBtn.setImageResource(R.drawable.ic_groups_outline_active)
        val groupTxt: TextView = findViewById(R.id.groupsBtnText)
        groupTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))

        // Инициализация RecyclerView
        expensesList = findViewById(R.id.expensesList)
        expensesList.layoutManager = LinearLayoutManager(this)
        expenseAdapter = ExpenseAdapter(expenseItems)
        expensesList.adapter = expenseAdapter

        // Загрузка данных из Firebase
        loadExpenses()

        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)

        // Lower menu
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

        val searchContactsBtn: ImageButton = findViewById(R.id.btnGoBack)
        searchContactsBtn.setOnClickListener {
            searchContactsBtn.startAnimation(clickAnimation)
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
            bottomSheet.show(
                supportFragmentManager,
                AddExpenseBottomSheet.TAG
            )
        }
    }

    private fun loadExpenses() {
        if (groupId == null) return

        val database = FirebaseDatabase.getInstance().reference
        database.child("Groups").child(groupId!!).child("Eat")
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

                override fun onCancelled(error: DatabaseError) {
                    // Обработка ошибок
                }
            })
    }

    // Модель данных для продукта
    data class ExpenseItem(
        val name: String,
        val price: Double,
        val quantity: Double
    )

    // Адаптер для RecyclerView
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