package com.example.bankapp

import android.content.Intent
import android.os.Bundle
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

class newGroupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_group)
        //Нижнее меню----------------------------------------
        val icogroupsBtn: ImageView = findViewById(R.id.groupsBtnIcon)
        icogroupsBtn.setImageResource(R.drawable.ic_groups_outline_active)
        val groupTxt: TextView = findViewById(R.id.groupsBtnText)
        groupTxt.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))
        val mainBtn: LinearLayout =  findViewById(R.id.homeBtn)
        mainBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        //------------------------------------------------

        val continueBtn: AppCompatButton = findViewById(R.id.continueBtn)
        continueBtn.setOnClickListener {
            val intent = Intent(this, GroupMembersChoiceActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}