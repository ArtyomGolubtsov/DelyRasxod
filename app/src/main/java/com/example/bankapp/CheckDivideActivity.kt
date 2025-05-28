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
    private lateinit var depositPhoneTextView: TextView

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

        depositPhoneTextView = findViewById(R.id.depositPhone)

        val proflBtn: LinearLayout = findViewById(R.id.profileBtn)
        proflBtn.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
            overridePendingTransition(0, 0)
        }

        loadExpenses()
        loadAdminPhoneNumber()

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

    private fun loadAdminPhoneNumber() {
        if (groupId == null) return

        val dbRef = FirebaseDatabase.getInstance().reference
        // Шаг 1: Получаем ID администратора группы
        dbRef.child("Groups").child(groupId!!).child("admin")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(adminSnapshot: DataSnapshot) {
                    val adminId = adminSnapshot.getValue(String::class.java)
                    if (adminId != null) {
                        // Шаг 2: Получаем номер телефона администратора
                        dbRef.child("Users").child(adminId).child("phone")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(phoneSnapshot: DataSnapshot) {
                                    val phoneNumber = phoneSnapshot.getValue(String::class.java)
                                    phoneNumber?.let {
                                        depositPhoneTextView.text = formatPhoneNumber(it)
                                    } ?: run {
                                        depositPhoneTextView.text = "Номер не указан"
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    depositPhoneTextView.text = "Ошибка загрузки"
                                }
                            })
                    } else {
                        depositPhoneTextView.text = "Админ не найден"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    depositPhoneTextView.text = "Ошибка загрузки"
                }
            })
    }

    private fun formatPhoneNumber(phone: String): String {
        // Простая форматировка номера телефона
        return if (phone.length == 11 && phone.startsWith("8")) {
            "+7 (${phone.substring(1, 4)}) ${phone.substring(4, 7)}-${phone.substring(7, 9)}-${phone.substring(9)}"
        } else {
            phone
        }
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