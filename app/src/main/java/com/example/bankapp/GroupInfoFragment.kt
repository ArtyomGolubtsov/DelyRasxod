package com.example.bankapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroupInfoFragment : Fragment() {

    private lateinit var groupDescription: TextView
    private lateinit var groupCategories: TextView
    private lateinit var groupTitle: TextView
    private lateinit var database: DatabaseReference
    private var groupId: String? = null  // Переменная для хранения groupId

    // Метод для создания нового экземпляра фрагмента с передачей groupId
    companion object {
        fun newInstance(groupId: String) = GroupInfoFragment().apply {
            arguments = Bundle().apply {
                putString("GROUP_ID", groupId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Получаем groupId из аргументов
        groupId = arguments?.getString("GROUP_ID")
        database = FirebaseDatabase.getInstance().reference  // Инициализация базы данных
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Загружаем макет фрагмента
        val view = inflater.inflate(R.layout.fragment_group_info, container, false)

        // Инициализация UI элементов
        groupTitle = view.findViewById(R.id.groupTitle)
        groupDescription = view.findViewById(R.id.groupDescription)
        groupCategories = view.findViewById(R.id.groupDescription) // Возможно, это ошибка; может быть groupCategories?

        // Загружаем данные группы из Firebase
        groupId?.let {
            loadGroupData(it)
        }

        return view
    }

    private fun loadGroupData(groupId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Запрос к базе данных Firebase
            database.child("Users").child(userId).child("Groups").child(groupId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {

                        val groupTitleText = snapshot.child("description").getValue(String::class.java)
                        val groupCategoriesList = snapshot.child("categories").children.map { it.getValue(String::class.java) }.filterNotNull()

                        // Устанавливаем название группы
                        groupTitle.text = groupTitleText ?: "Название не найдено"

                        // Устанавливаем описание группы
                        groupDescription.text = groupTitleText ?: "Описание группы не найдено"

                        // Устанавливаем категории группы
                        if (groupCategoriesList.isNotEmpty()) {
                            groupCategories.text = groupCategoriesList.joinToString(", ")
                        } else {
                            groupCategories.text = "Категории не указаны"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        groupTitle.text = "Ошибка загрузки данных"
                    }
                })
        } else {
            groupTitle.text = "Пользователь не найден"
        }
    }
}
