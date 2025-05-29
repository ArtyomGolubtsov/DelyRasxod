package com.example.bankapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bankapp.databinding.ActivityRecommendProductsBinding
import com.example.bankapp.databinding.ProductItemBinding

data class Category(
    val title: String,
    val items: List<String>,
    val imageResId: Int,
    var isExpanded: Boolean = false
)

class CategoryAdapter(private val categories: List<Category>) :
    RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(val binding: ProductItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ProductItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        val binding = holder.binding

        Glide.with(binding.root.context)
            .load(category.imageResId)
            .placeholder(R.drawable.placeholder)
            .into(binding.categoryImage)



        binding.categoryTitle.text = category.title

        // Очистить предыдущие элементы
        binding.expandableLayout.removeAllViews()

        // Добавить продукты
        for (item in category.items) {
            val textView = TextView(binding.root.context).apply {
                text = "• $item"
                setTextColor(Color.LTGRAY)
                setPadding(0, 4, 0, 4)
            }
            binding.expandableLayout.addView(textView)
        }

        binding.expandableLayout.visibility = if (category.isExpanded) View.VISIBLE else View.GONE

        // Нажатие на карточку
        binding.root.setOnClickListener {
            category.isExpanded = !category.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = categories.size
}


class RecommendProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecommendProductsBinding
    private val categories = listOf(
        Category("Отдохнуть под сериал",
            listOf("Чипсы (картофельные, кукурузные)",
            "Сухарики, крекеры", "Орешки (фисташки, арахис)",
            "Попкорн (соленый, карамельный, сырный)", "Лапша быстрого приготовления",
            "Чай, кофе", "Газировка", "Сок", "Вода", "Шоколадные батончики",
            "Печенье", "Мороженое"), R.drawable.category_first),
        Category("Майские", listOf("Шашлык", "Соусы", "Овощи"), R.drawable.category_second),
        Category("Пикник", listOf("Корзина", "Фрукты", "Напитки"), R.drawable.category_third),
        Category("Путешествие", listOf("Снэки", "Вода", "Шоколадки"), R.drawable.category_fourth)
    )



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recommend_products)

        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_bg)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_bg)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityRecommendProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.categoryList.layoutManager = LinearLayoutManager(this)
        binding.categoryList.adapter = CategoryAdapter(categories)

    }
}