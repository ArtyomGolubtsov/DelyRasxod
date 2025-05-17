package com.example.bankapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AddExpenseBottomSheet : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "AddExpenseBottomSheet"
        private const val ARG_GROUP_ID = "GROUP_ID"
        private const val ARG_USER_ID = "USER_ID"

        fun newInstance(groupId: String, userId: String): AddExpenseBottomSheet {
            return AddExpenseBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_GROUP_ID, groupId)
                    putString(ARG_USER_ID, userId)
                }
            }
        }
    }

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance().reference
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.add_expense_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputName = view.findViewById<EditText>(R.id.inputPurchaseName)
        val inputNum = view.findViewById<EditText>(R.id.inputPurchaseNum)
        val inputPrice = view.findViewById<EditText>(R.id.inputPurchasePrice)
        val addButton = view.findViewById<Button>(R.id.addPurchaseBtn)

        addButton.setOnClickListener {
            val name = inputName.text.toString().trim()
            val quantityStr = inputNum.text.toString().trim()
            val priceStr = inputPrice.text.toString().trim()

            if (name.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantity = quantityStr.toDoubleOrNull() ?: 0.0
            val price = priceStr.toDoubleOrNull() ?: 0.0

            if (quantity <= 0 || price <= 0) {
                Toast.makeText(context, "Значения должны быть больше нуля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val groupId = arguments?.getString(ARG_GROUP_ID) ?: run {
                Toast.makeText(context, "Ошибка: группа не определена", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Подготавливаем данные для сохранения
            val productData = hashMapOf<String, Any>(
                "Price" to price,
                "quantity" to quantity
            )

            // Создаем обновления для обоих узлов
            val updates = hashMapOf<String, Any>(
                "Groups/$groupId/Eat/$name" to productData,
                "Groups/$groupId/EatCheck/$name" to productData
            )

            // Атомарное обновление обоих узлов
            database.updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(context, "Покупка добавлена в оба списка", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}