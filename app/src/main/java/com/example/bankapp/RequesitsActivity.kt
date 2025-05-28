package com.example.bankapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class RequesitsActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private var groupId: String = ""
    private var userId: String = ""
    private var adminId: String? = null // Добавлено для хранения ID админа
    private val expenseItems = mutableListOf<ExpenseItem>()
    private lateinit var expenseAdapter: ExpenseAdapter

    data class ExpenseItem(
        val name: String,
        val price: Int,
        val quantity: Int
    )
//TODO ёдоделать сохранение пользователя при оплате

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_requesits)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        // Получение данных из Intent
        groupId = intent.getStringExtra("GROUP_ID") ?: ""
        userId = intent.getStringExtra("USER_ID") ?: ""
        adminId = intent.getStringExtra("ADMIN_ID") // Получение ID админа

        database = FirebaseDatabase.getInstance()
        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)
        initViews()
        setupAdapters()
        loadUserData()
        loadExpenses() // Загрузка расходов
        setupClickListeners()
        initBottomMenu(clickAnimation)
        checkIfAlreadyPaid()

    }

    private fun initViews() {
        findViewById<ImageButton>(R.id.btnGoBack).setOnClickListener { finish() }
    }

    private fun checkIfAlreadyPaid() {
        val paymentBtn = findViewById<AppCompatButton>(R.id.confirmPaymentBtn)

        database.getReference("Groups").child(groupId).child("PayUsers").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hasPaid = snapshot.getValue(Boolean::class.java) == true
                    if (hasPaid) {
                        paymentBtn.text = "Вы уже оплатили"
                        paymentBtn.isEnabled = false
                    } else {
                        paymentBtn.text = "Подтвердить платеж"
                        paymentBtn.isEnabled = true
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Ошибка проверки платежа: ${error.message}")
                }
            })
    }


    private fun setupAdapters() {
        expenseAdapter = ExpenseAdapter(expenseItems)
        findViewById<RecyclerView>(R.id.expensesList).apply {
            layoutManager = LinearLayoutManager(this@RequesitsActivity)
            adapter = expenseAdapter
        }
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

    private fun loadUserData() {
        // Используем adminId вместо userId
        database.getReference("Users").child(adminId ?: "")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    findViewById<TextView>(R.id.requesitsName).text =
                        snapshot.child("name").value?.toString() ?: "Не указано"

                    findViewById<TextView>(R.id.requesitsPhone).text =
                        snapshot.child("phone").value?.toString() ?: "Не указан"

                    findViewById<TextView>(R.id.requesitsBank).text =
                        snapshot.child("bank").value?.toString() ?: "Не указан"
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Ошибка загрузки данных: ${error.message}")
                }
            })
    }


    private fun loadExpenses() {
        val userExpensesRef = database.getReference("Groups").child(groupId).child("Users").child(userId)

        userExpensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                expenseItems.clear()
                var total = 0.0

                // Проверяем, есть ли дети у узла с пользовательскими расходами
                snapshot.children.forEach { product ->
                    val name = product.key ?: "Без названия"
                    val price = product.child("Price").getValue(Double::class.java) ?: 0.0
                    val quantity = product.child("quantity").getValue(Int::class.java) ?: 0

                    if (quantity > 0) {
                        expenseItems.add(ExpenseItem(name, price.toInt(), quantity))
                        total += price * quantity
                    }
                }

                updateTotalSum(total.toInt())
                expenseAdapter.notifyDataSetChanged()

                if (expenseItems.isEmpty()) {
                    findViewById<TextView>(R.id.expensesListTitle).text = "Нет покупок"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showError("Ошибка загрузки списка: ${error.message}")
            }
        })
    }

    private fun updateTotalSum(total: Int) {
        val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))
        findViewById<TextView>(R.id.totalSumTitle).text =
            "Итого: ${formatter.format(total)} ₽"
    }

    private fun setupClickListeners() {

        val paymentBtn = findViewById<AppCompatButton>(R.id.confirmPaymentBtn)

        database.getReference("Groups").child(groupId).child("PayUsers").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Проверяем, существует ли запись и её значение
                    val paymentStatus = snapshot.getValue(Boolean::class.java)

                    if (paymentStatus != null) { // или if (snapshot.exists())
                        // Если запись существует (пользователь уже платил)
                        paymentBtn.text = "Вы уже оплатили"
                        paymentBtn.isEnabled = false
                        paymentBtn.setBackgroundResource(R.drawable.btn_back_btn_bg)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Ошибка доступа к базе данных")
                }
            })

        paymentBtn.setOnClickListener {
            // Сначала проверим, платил ли уже пользователь
            database.getReference("Groups").child(groupId).child("PayUsers").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val hasPaid = snapshot.getValue(Boolean::class.java) == true || snapshot.getValue(Boolean::class.java) == false
                        if (hasPaid) {
                            Toast.makeText(this@RequesitsActivity, "Вы уже оплатили", Toast.LENGTH_SHORT).show()
                            paymentBtn.text = "Вы уже оплатили"
                            paymentBtn.isEnabled = false
                            paymentBtn.setBackgroundResource(R.drawable.btn_back_btn_bg)
                        } else {
                            // Сохраняем платеж только если его ещё не было
                            database.getReference("Groups").child(groupId).child("PayUsers").child(userId)
                                .setValue(false)
                                .addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        Toast.makeText(this@RequesitsActivity, "Платеж подтвержден", Toast.LENGTH_SHORT).show()
                                        paymentBtn.text = "Вы уже оплатили"
                                        paymentBtn.isEnabled = false


                                        finish()
                                    } else {
                                        showError("Ошибка подтверждения платежа")
                                    }
                                }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showError("Ошибка при проверке статуса оплаты")
                    }
                })
        }
    }


    private inner class ExpenseAdapter(
        private val items: List<ExpenseItem>
    ) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.expenseTitle)
            val price: TextView = itemView.findViewById(R.id.expensePrice)
            val quantity: TextView = itemView.findViewById(R.id.expenseNum)
            val addExpenseBtn: View? = itemView.findViewById(R.id.addExpenseBtn)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            return ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.add_purchase_for_user, parent, false)

            )
        }


        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            items[position].let {
                holder.title.text = it.name
                holder.price.text = "${it.price} ₽"
                holder.quantity.text = " × ${it.quantity}"
                holder.addExpenseBtn?.visibility = View.GONE
            }
        }

        override fun getItemCount() = items.size

    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e("RequesitsActivity", message)

    }

    override fun finish() {
        super.finish()
    }
}
