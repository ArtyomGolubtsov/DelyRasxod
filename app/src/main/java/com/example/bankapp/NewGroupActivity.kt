package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Данные о групповой активности
data class GroupActivityItem(val name: String, val category: String, val imageResId: Int)

// Адаптер для группы активностей
class GroupActivityAdapter(private val activityList: List<GroupActivityItem>) : RecyclerView.Adapter<GroupActivityAdapter.ActivityViewHolder>() {

    inner class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val button: Button = view.findViewById(R.id.CtrgBtn) // Убедитесь, что у вас есть ID для кнопки
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val layoutId = when (viewType) {
            1 -> R.layout.ctgr_party
            2 -> R.layout.ctgr_travel
            3 -> R.layout.ctgr_other
            4 -> R.layout.ctgr_event
            5 -> R.layout.ctgr_family
            else -> throw IllegalArgumentException("Invalid view type")
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
    }

    override fun getItemViewType(position: Int): Int {
        return when (activityList[position].category) {
            "Event" -> 1
            "Family" -> 2
            "Other" -> 3
            "Party" -> 4
            "Travel" -> 5
            else -> 0 // Это может быть код по умолчанию
        }
    }

    override fun getItemCount() = activityList.size
}


class NewGroupActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_group)

        // Настройка нижнего меню
        val icogroupsBtn: ImageView = findViewById(R.id.groupsBtnIcon)
        icogroupsBtn.setImageResource(R.drawable.ic_groups_outline_active)
        val groupTxt: TextView = findViewById(R.id.groupsBtnText)
        groupTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))
        val mainBtn: LinearLayout = findViewById(R.id.homeBtn)
        mainBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Кнопки для навигации
        val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)
        btnGoBack.setOnClickListener {
            onBackPressed()
            finish()
            overridePendingTransition(0, 0)
        }
        val CancelBtn: AppCompatButton = findViewById(R.id.CancelBtn)
        CancelBtn.setOnClickListener {
            onBackPressed()
            finish()
            overridePendingTransition(0, 0)
        }

        val continueBtn: AppCompatButton = findViewById(R.id.continueBtn)
        continueBtn.setOnClickListener {
            val intent = Intent(this, GroupMembersChoiceActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Установка адаптера
        recyclerView = findViewById(R.id.categoryList) // Убедитесь, что вы используете правильный ID
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        // Создание списка групп активностей
        val activityList = listOf(
            GroupActivityItem("Event Activity 1", "Event", R.drawable.placeholder),
            GroupActivityItem("Family Activity 1", "Family", R.drawable.placeholder),
            GroupActivityItem("Other Activity 1", "Other", R.drawable.placeholder),
            GroupActivityItem("Party Activity 1", "Party", R.drawable.placeholder),
            GroupActivityItem("Travel Activity 1", "Travel", R.drawable.placeholder)
        )

        // Установка адаптера
        val adapter = GroupActivityAdapter(activityList)
        recyclerView.adapter = adapter

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
