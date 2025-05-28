package com.example.bankapp

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.security.MessageDigest
import java.util.UUID

data class GroupActivityItem(val name: String, val category: String, val imageResId: Int)

data class Group(
    val title: String = "",
    val description: String = "",
    val imageUri: String = "",
    val categories: List<String> = emptyList(),
    val admin: String = ""
)

class GroupActivityAdapter(
    private val activityList: List<GroupActivityItem>,
    private val onCategorySelected: (List<String>) -> Unit
) : RecyclerView.Adapter<GroupActivityAdapter.ActivityViewHolder>() {

    inner class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ctgrEventBtn: AppCompatButton? = view.findViewById(R.id.CtgrEventBtn)
        val ctgrFamilyBtn: AppCompatButton? = view.findViewById(R.id.CtgrFamilyBtn)
        val ctgrOtherBtn: AppCompatButton? = view.findViewById(R.id.CtgrOtherBtn)
        val ctgrPartyBtn: AppCompatButton? = view.findViewById(R.id.CtgrPartyBtn)
        val ctgrTravelBtn: AppCompatButton? = view.findViewById(R.id.CtgrTravelBtn)
    }

    private val selectedButtons = mutableSetOf<AppCompatButton>()
    var selectedCategories = mutableListOf<String>()

    fun updateSelectedCategories(categories: List<String>) {
        selectedCategories.clear()
        selectedCategories.addAll(categories)
        selectedButtons.clear()
        notifyDataSetChanged()
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
            "Путешествия" -> 1
            "Вечеринка" -> 2
            "Событие" -> 3
            "Семья" -> 4
            "Другое" -> 5
            else -> throw IllegalArgumentException("Invalid category: ${activityList[position].category}")
        }
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        resetButtonBackgrounds(holder)
        updateButtonStates(holder)
        setupCategoryButtons(holder)
    }

    private fun resetButtonBackgrounds(holder: ActivityViewHolder) {
        val defaultColor = ContextCompat.getColor(holder.itemView.context, R.color.back_btn)
        holder.ctgrEventBtn?.backgroundTintList = ColorStateList.valueOf(defaultColor)
        holder.ctgrFamilyBtn?.backgroundTintList = ColorStateList.valueOf(defaultColor)
        holder.ctgrOtherBtn?.backgroundTintList = ColorStateList.valueOf(defaultColor)
        holder.ctgrPartyBtn?.backgroundTintList = ColorStateList.valueOf(defaultColor)
        holder.ctgrTravelBtn?.backgroundTintList = ColorStateList.valueOf(defaultColor)
    }

    private fun updateButtonStates(holder: ActivityViewHolder) {
        holder.ctgrEventBtn?.let { btn ->
            btn.backgroundTintList = if (selectedCategories.contains("Событие")) {
                ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.ctgr_event))
            } else {
                ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.back_btn))
            }
        }

        holder.ctgrFamilyBtn?.let { btn ->
            btn.backgroundTintList = if (selectedCategories.contains("Семья")) {
                ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.ctgr_family))
            } else {
                ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.back_btn))
            }
        }

        holder.ctgrOtherBtn?.let { btn ->
            btn.backgroundTintList = if (selectedCategories.contains("Другое")) {
                ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.ctgr_other))
            } else {
                ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.back_btn))
            }
        }

        holder.ctgrPartyBtn?.let { btn ->
            btn.backgroundTintList = if (selectedCategories.contains("Вечеринка")) {
                ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.ctgr_party))
            } else {
                ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.back_btn))
            }
        }

        holder.ctgrTravelBtn?.let { btn ->
            btn.backgroundTintList = if (selectedCategories.contains("Путешествия")) {
                ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.dely_blue))
            } else {
                ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.back_btn))
            }
        }
    }

    private fun setupCategoryButtons(holder: ActivityViewHolder) {
        holder.ctgrEventBtn?.setOnClickListener { toggleCategory(holder.ctgrEventBtn, "Событие", R.color.ctgr_event) }
        holder.ctgrFamilyBtn?.setOnClickListener { toggleCategory(holder.ctgrFamilyBtn, "Семья", R.color.ctgr_family) }
        holder.ctgrOtherBtn?.setOnClickListener { toggleCategory(holder.ctgrOtherBtn, "Другое", R.color.ctgr_other) }
        holder.ctgrPartyBtn?.setOnClickListener { toggleCategory(holder.ctgrPartyBtn, "Вечеринка", R.color.ctgr_party) }
        holder.ctgrTravelBtn?.setOnClickListener { toggleCategory(holder.ctgrTravelBtn, "Путешествия", R.color.dely_blue) }
    }

    private fun toggleCategory(button: AppCompatButton?, category: String, colorRes: Int) {
        button?.let { btn ->
            val context = btn.context
            val wasSelected = selectedCategories.contains(category)

            if (wasSelected) {
                selectedCategories.remove(category)
                selectedButtons.remove(btn)
                btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.back_btn))
            } else {
                if (selectedCategories.size < 2) {
                    val clickAnimation = AnimationUtils.loadAnimation(context, R.anim.keyboardfirst)
                    btn.startAnimation(clickAnimation)
                    selectedCategories.add(category)
                    selectedButtons.add(btn)
                    btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
                } else {
                    val shakeAnimation = AnimationUtils.loadAnimation(context, R.anim.shake)
                    btn.startAnimation(shakeAnimation)
                }
            }

            onCategorySelected(selectedCategories)
        }
    }

    override fun getItemCount() = activityList.size
}

class NewGroupActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var groupTitle: EditText
    private lateinit var groupDescription: EditText
    private lateinit var groupImage: ImageView
    private lateinit var mainTitle: TextView
    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private var selectedCategories: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)
        setContentView(R.layout.activity_new_group)

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid ?: return

        mainTitle = findViewById(R.id.mainTitle)

        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)
        val icogroupsBtn: ImageView = findViewById(R.id.groupsBtnIcon)
        icogroupsBtn.setImageResource(R.drawable.ic_groups_outline_active)
        val groupTxt: TextView = findViewById(R.id.groupsBtnText)
        groupTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))
        val mainBtn: LinearLayout = findViewById(R.id.homeBtn)

        mainBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            mainBtn.startAnimation(clickAnimation)
        }

        val proflBtn: LinearLayout = findViewById(R.id.profileBtn)
        proflBtn.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
            proflBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        val contactsBtn: LinearLayout = findViewById(R.id.contactsBtn)
        contactsBtn.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
            contactsBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        val continueBtn: AppCompatButton = findViewById(R.id.continueBtn)
        val groupId = intent.getStringExtra("GROUP_ID")
        if (!groupId.isNullOrEmpty()) {
            getGroupDetailsFromDatabase(groupId)
            continueBtn.text = "Сохранить"
            mainTitle.text = "Изменить"
        }

        groupTitle = findViewById(R.id.groupTitle)
        groupDescription = findViewById(R.id.groupDescription)
        groupImage = findViewById(R.id.groupImage)

        val addGroupImageBtn: ConstraintLayout = findViewById(R.id.addGroupImageBtn)
        addGroupImageBtn.setOnClickListener {
            launchImagePicker()
            contactsBtn.startAnimation(clickAnimation)
        }

        val btnGoBack: ImageButton = findViewById(R.id.btnGoBack)
        btnGoBack.setOnClickListener {
            onBackPressed()
            contactsBtn.startAnimation(clickAnimation)
        }

        val cancelBtn: AppCompatButton = findViewById(R.id.CancelBtn)
        cancelBtn.setOnClickListener {
            onBackPressed()
            contactsBtn.startAnimation(clickAnimation)
        }

        setupUi()

        findViewById<AppCompatButton>(R.id.continueBtn).setOnClickListener {
            onContinueButtonClicked()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun setupUi() {
        recyclerView = findViewById(R.id.categoryList)
        recyclerView.layoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
        }

        val activityList = listOf(
            GroupActivityItem("Путешествия", "Путешествия", R.layout.ctgr_travel),
            GroupActivityItem("Вечеринка", "Вечеринка", R.layout.ctgr_party),
            GroupActivityItem("Событие", "Событие", R.layout.ctgr_event),
            GroupActivityItem("Семья", "Семья", R.layout.ctgr_family),
            GroupActivityItem("Другое", "Другое", R.layout.ctgr_other)
        )

        val adapter = GroupActivityAdapter(activityList) { categories ->
            selectedCategories.clear()
            selectedCategories.addAll(categories)
        }
        recyclerView.adapter = adapter
    }

    private fun getGroupDetailsFromDatabase(groupId: String) {
        val database = FirebaseDatabase.getInstance().getReference("Groups/$groupId")
        database.get().addOnSuccessListener { dataSnapshot ->
            val title = dataSnapshot.child("title").getValue(String::class.java)
            val description = dataSnapshot.child("description").getValue(String::class.java)
            val imageUri = dataSnapshot.child("imageUri").getValue(String::class.java)

            groupTitle.setText(title ?: "")
            groupDescription.setText(description ?: "")

            if (imageUri != null) {
                Glide.with(this).load(imageUri).into(groupImage)
            } else {
                groupImage.setImageResource(R.drawable.placeholder)
            }

            val selectedCategoriesList = mutableListOf<String>()
            for (categorySnapshot in dataSnapshot.child("categories").children) {
                categorySnapshot.getValue(String::class.java)?.let { category ->
                    selectedCategoriesList.add(category)
                }
            }

            selectedCategories.clear()
            selectedCategories.addAll(selectedCategoriesList)

            (recyclerView.adapter as? GroupActivityAdapter)?.updateSelectedCategories(selectedCategoriesList)
        }.addOnFailureListener {
            Toast.makeText(this, "Ошибка загрузки данных группы", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            imageUri = data?.data
            imageUri?.let { uri ->
                Glide.with(this).load(uri).into(groupImage)
            }
        }
    }

    private fun onContinueButtonClicked() {
        val title = groupTitle.text.toString().trim()
        val description = groupDescription.text.toString().trim()
        val groupId = intent.getStringExtra("GROUP_ID")

        when {
            title.isEmpty() -> Toast.makeText(this, "Введите заголовок группы", Toast.LENGTH_SHORT).show()
            description.isEmpty() -> Toast.makeText(this, "Введите описание группы", Toast.LENGTH_SHORT).show()
            imageUri == null && groupId.isNullOrEmpty() -> Toast.makeText(this, "Выберите фотографию группы", Toast.LENGTH_SHORT).show()
            selectedCategories.isEmpty() -> Toast.makeText(this, "Выберите хотя бы одну категорию", Toast.LENGTH_SHORT).show()
            else -> {
                if (groupId != null) {
                    updateExistingGroup(groupId, title, description)
                } else {
                    createNewGroup(title, description)
                }
            }
        }
    }

    private fun updateExistingGroup(groupId: String, title: String, description: String) {
        val updates = hashMapOf<String, Any>(
            "title" to title,
            "description" to description,
            "categories" to selectedCategories
        )

        if (imageUri != null) {
            val imageHash = hashString(imageUri.toString())
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("group_images/${imageHash}.jpg")

            imageRef.putFile(imageUri!!).addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    updates["imageUri"] = downloadUrl.toString()
                    saveGroupUpdates(groupId, updates)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Ошибка при загрузке изображения", Toast.LENGTH_SHORT).show()
            }
        } else {
            saveGroupUpdates(groupId, updates)
        }
    }

    private fun saveGroupUpdates(groupId: String, updates: HashMap<String, Any>) {
        FirebaseDatabase.getInstance().getReference("Groups/$groupId")
            .updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Изменения сохранены", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, GroupInfoActivity::class.java).apply {
                    putExtra("GROUP_ID", groupId)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка при сохранении изменений", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNewGroup(title: String, description: String) {
        val imageHash = hashString(imageUri.toString())
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("group_images/${imageHash}.jpg")

        imageRef.putFile(imageUri!!).addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                val groupId = UUID.randomUUID().toString()
                val group = Group(
                    title = title,
                    description = description,
                    imageUri = downloadUrl.toString(),
                    categories = selectedCategories,
                    admin = userId
                )

                FirebaseDatabase.getInstance().getReference("Groups/$groupId")
                    .setValue(group)
                    .addOnSuccessListener {
                        FirebaseDatabase.getInstance().getReference("Users/$userId/Groups/$groupId")
                            .setValue(true)
                            .addOnSuccessListener {
                                FirebaseDatabase.getInstance().getReference("Groups/$groupId/Users/$userId")
                                    .setValue(true)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Группа успешно создана", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, GroupMembersChoiceActivity::class.java)
                                        intent.putExtra("GROUP_ID", groupId)
                                        startActivity(intent)
                                        overridePendingTransition(0, 0)
                                    }
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Ошибка при создании группы", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Ошибка при загрузке изображения", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { String.format("%02x", it) }
    }
}