package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent

// Данные о групповой активности
data class GroupActivityItem(val name: String, val category: String, val imageResId: Int)

// Адаптер для группы активностей
class GroupActivityAdapter(private val activityList: List<GroupActivityItem>) : RecyclerView.Adapter<GroupActivityAdapter.ActivityViewHolder>() {

    inner class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val layoutId = when (viewType) {
            1 -> R.layout.ctgr_travel
            2 -> R.layout.ctgr_party
            3 -> R.layout.ctgr_event
            4 -> R.layout.ctgr_family
            5 -> R.layout.ctgr_other
            else -> throw IllegalArgumentException("Invalid view type")
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ActivityViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return when (activityList[position].category) {
            "Travel" -> 1
            "Party" -> 2
            "Event" -> 3
            "Family" -> 4
            "Other" -> 5
            else -> throw IllegalArgumentException("Invalid category: ${activityList[position].category}")
        }
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
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

        // Установка адаптера
        recyclerView = findViewById(R.id.categoryList) // Убедитесь, что вы используете правильный ID
        recyclerView.layoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
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

        // Создание списка групп активностей
        val activityList = listOf(
            GroupActivityItem("Путешествие", "Travel", R.layout.ctgr_travel),
            GroupActivityItem("Вечеринка", "Party", R.layout.ctgr_party),
            GroupActivityItem("Событие", "Event", R.layout.ctgr_event),
            GroupActivityItem("Семья", "Family", R.layout.ctgr_family),
            GroupActivityItem("Другое", "Other", R.layout.ctgr_other)
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
