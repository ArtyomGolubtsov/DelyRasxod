package com.example.bankapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent

// Данные о групповой активности
data class GroupActivityItem(val name: String, val category: String, val imageResId: Int)

// Адаптер для группы активностей
class GroupActivityAdapter(
    private val activityList: List<GroupActivityItem>,
    private val onCategorySelected: () -> Unit
) : RecyclerView.Adapter<GroupActivityAdapter.ActivityViewHolder>() {

    inner class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Определите кнопки
        val ctgrEventBtn: AppCompatButton? = view.findViewById(R.id.CtgrEventBtn)
        val ctgrFamilyBtn: AppCompatButton? = view.findViewById(R.id.CtgrFamilyBtn)
        val ctgrOtherBtn: AppCompatButton? = view.findViewById(R.id.CtgrOtherBtn)
        val ctgrPartyBtn: AppCompatButton? = view.findViewById(R.id.CtgrPartyBtn)
        val ctgrTravelBtn: AppCompatButton? = view.findViewById(R.id.CtgrTravelBtn)
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

    private val selectedButtons = mutableSetOf<AppCompatButton>()

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        resetButtonBackgrounds(holder)

        // Установка обработчиков для кнопок
        holder.ctgrEventBtn?.setOnClickListener {
            val color = ContextCompat.getColor(holder.itemView.context, R.color.ctgr_event)
            onCategoryButtonClick(holder.ctgrEventBtn, color, ContextCompat.getColor(holder.itemView.context, R.color.back_btn))
        }
        holder.ctgrFamilyBtn?.setOnClickListener {
            val color = ContextCompat.getColor(holder.itemView.context, R.color.ctgr_family)
            onCategoryButtonClick(holder.ctgrFamilyBtn, color, ContextCompat.getColor(holder.itemView.context, R.color.back_btn))
        }
        holder.ctgrOtherBtn?.setOnClickListener {
            val color = ContextCompat.getColor(holder.itemView.context, R.color.ctgr_other)
            onCategoryButtonClick(holder.ctgrOtherBtn, color, ContextCompat.getColor(holder.itemView.context, R.color.back_btn))
        }
        holder.ctgrPartyBtn?.setOnClickListener {
            val color = ContextCompat.getColor(holder.itemView.context, R.color.ctgr_party)
            onCategoryButtonClick(holder.ctgrPartyBtn, color, ContextCompat.getColor(holder.itemView.context, R.color.back_btn))
        }
        holder.ctgrTravelBtn?.setOnClickListener {
            val color = ContextCompat.getColor(holder.itemView.context, R.color.dely_blue)
            onCategoryButtonClick(holder.ctgrTravelBtn, color, ContextCompat.getColor(holder.itemView.context, R.color.back_btn))
        }
    }

    private fun resetButtonBackgrounds(holder: ActivityViewHolder) {
        val defaultColor = ContextCompat.getColor(holder.itemView.context, R.color.back_btn)
        holder.ctgrEventBtn?.backgroundTintList = ColorStateList.valueOf(defaultColor)
        holder.ctgrFamilyBtn?.backgroundTintList = ColorStateList.valueOf(defaultColor)
        holder.ctgrOtherBtn?.backgroundTintList = ColorStateList.valueOf(defaultColor)
        holder.ctgrPartyBtn?.backgroundTintList = ColorStateList.valueOf(defaultColor)
        holder.ctgrTravelBtn?.backgroundTintList = ColorStateList.valueOf(defaultColor)
    }

    private var selectedCount: Int = 0


    private fun onCategoryButtonClick(button: AppCompatButton?, color: Int, defaultColor: Int) {
        if (button != null) {
            // Если кнопка еще не выбрана и максимум не достигнут
            if (!selectedButtons.contains(button) && selectedCount < 2) {
                button.backgroundTintList = ColorStateList.valueOf(color)
                selectedButtons.add(button)
                selectedCount++ // Увеличиваем количество выбранных категорий
                onCategorySelected() // Вызываем коллбэк для уведомления о выборе категории
            } else {
                // Если кнопка уже выбрана, сбрасываем цвет
                if (selectedButtons.contains(button)) {
                    button.backgroundTintList = ColorStateList.valueOf(defaultColor)
                    selectedButtons.remove(button)
                    selectedCount-- // Уменьшаем количество выбранных категорий
                } else {
                    Toast.makeText(button.context, "Вы уже выбрали максимальное количество категорий", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getItemCount() = activityList.size
}

class NewGroupActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var groupTitle: EditText
    private lateinit var groupDescription: EditText
    private lateinit var groupImage: ImageView
    private var imageUri: Uri? = null
    private var selectedCount: Int = 0
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)
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
        // Инициализация полей
        groupTitle = findViewById(R.id.groupTitle)
        groupDescription = findViewById(R.id.groupDescription)
        groupImage = findViewById(R.id.groupImage)

        // Кнопка для выбора изображения
        groupImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Навигационные кнопки
        val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)
        btnGoBack.setOnClickListener {
            onBackPressed()
            overridePendingTransition(0, 0)
        }
        val cancelBtn: AppCompatButton = findViewById(R.id.CancelBtn)
        cancelBtn.setOnClickListener {
            onBackPressed()
            overridePendingTransition(0, 0)
        }

        // Настройка RecyclerView и остального интерфейса
        setupUi()

        // Кнопка продолжить
        findViewById<AppCompatButton>(R.id.continueBtn).setOnClickListener {
            onContinueButtonClicked()
        }
    }

    private fun setupUi() {
        // Настройка RecyclerView для категорий, как у вас было ранее
        recyclerView = findViewById(R.id.categoryList)
        recyclerView.layoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
        }

        // Установка адаптера
        val activityList = listOf(
            GroupActivityItem("Путешествие", "Travel", R.layout.ctgr_travel),
            GroupActivityItem("Вечеринка", "Party", R.layout.ctgr_party),
            GroupActivityItem("Событие", "Event", R.layout.ctgr_event),
            GroupActivityItem("Семья", "Family", R.layout.ctgr_family),
            GroupActivityItem("Другое", "Other", R.layout.ctgr_other)
        )

        val adapter = GroupActivityAdapter(activityList) { onCategorySelected() }
        recyclerView.adapter = adapter

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun onCategorySelected() {
        selectedCount++
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            imageUri = data?.data
            groupImage.setImageURI(imageUri)
        }
    }

    private fun onContinueButtonClicked() {
        val title = groupTitle.text.toString().trim()
        val description = groupDescription.text.toString().trim()

        val isTitleValid = title.isNotEmpty()
        val isDescriptionValid = description.isNotEmpty()
        val isImageValid = imageUri != null
        val isCategorySelected = selectedCount > 0

        if (!isTitleValid) {
            Toast.makeText(this, "Введите заголовок группы", Toast.LENGTH_SHORT).show()
        } else if (!isDescriptionValid) {
            Toast.makeText(this, "Введите описание группы", Toast.LENGTH_SHORT).show()
        } else if (!isImageValid) {
            Toast.makeText(this, "Выберите фотографию группы", Toast.LENGTH_SHORT).show()
        } else if (!isCategorySelected) {
            Toast.makeText(this, "Выберите хотя бы одну категорию", Toast.LENGTH_SHORT).show()
        } else {
            // Все проверки пройдены, продолжаем
            val intent = Intent(this, GroupMembersChoiceActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }
}
