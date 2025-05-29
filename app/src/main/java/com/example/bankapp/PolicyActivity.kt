package com.example.bankapp

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class PolicyActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var policyTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_policy)

        // Инициализация Firebase Auth
        auth = Firebase.auth

        // Обработка нажатия кнопки назад
        findViewById<ImageButton>(R.id.btnGoBack).setOnClickListener {
            overridePendingTransition(0, 0)
            finish()
        }

        policyTxt = findViewById(R.id.policyText)

        policyTxt.text = """
    Политика конфиденциальности приложения DelyRasxod

    Здравствуйте, дорогие пользователи DelyRasxod! Мы рады, что вы решили разделить с нами свои расходы (и, возможно, даже свои секреты о том, кто съел последний кусок пиццы). Пожалуйста, ознакомьтесь с нашей Политикой конфиденциальности, прежде чем погрузиться в мир совместного ведения финансов!

    1. Какие данные мы собираем?
    Когда вы решитесь завести профили в DelyRasxod, мы можем попросить у вас следующие данные:

    *Имя, фамилия и отчество*: Мы хотим знать, с кем делим расходы, но не волнуйтесь, мы не собираемся использовать ваши данные для создания лотереи на отпуск!
    *Номер телефона*: На случай, если мы захотим позвонить вам и спросить, кто трижды забыл вернуть деньги за такси...
    *Электронная почта*: Чтобы держать вас в курсе всех новостей, акций и, возможно, загадочных шуток!
    *Банковская информация*: Чтобы мы могли безопасно обрабатывать ваши транзакции и не тратить ваши деньги на бессмысленные запросы на пиццу (хотя мы очень любим пиццу).
    *Фотография профиля*: Это поможет вашим друзьям узнать, кто же решает, кто должен платить за ужин (и не забывайте, мы любим, когда вы улыбаетесь на фото!).

    2. Как мы используем собранные данные?
    Ваши данные помогают нам:

    * Средоточить все усилия на том, чтобы вы могли без стресса делиться расходами с друзьями.
    * Отправлять вам полезную информацию и уведомления (без спама, обещаем!).
    * Улучшать наше приложение и предлагать вам еще более крутые функции.

    3. Как мы защищаем ваши данные?
    Ваши данные — это наши сокровища! Мы используем современные методы шифрования и системы безопасности, чтобы не допустить, чтобы ваш телефон не стал площадкой для кражи информации. Мы не хотим, чтобы кто-то использовал ваши данные для того, чтобы тратить ваши средства на что-то бесполезное, вроде пластиковых слонов!

    4. Передача данных третьим лицам
    Не бойтесь, ваши данные не попадают к незнакомцам на улице, даже если они очень хотят их. Мы не будем передавать вашу информацию третьим лицам, кроме случаев, когда это необходимо для оказания услуг или в соответствии с законом (например, если вдруг появится охотник за сокровищами, требующий наши данные!).

    5. Согласие на политику конфиденциальности
    С использованием нашего приложения вы соглашаетесь с условиями данной политики. Если вы не согласны с чем-то (например, с тем, как мы пишем шутки), вы всегда можете покинуть нас... но мы будем очень, очень грустить.

    6. Контактная информация
    Если у вас есть вопросы, комментарии или просто желание обсудить, кто же должен оплачивать следующий обед, напишите нам на нашу электронную почту: support@delyrasxod.com. Мы всегда на связи!

    Спасибо, что выбрали DelyRasxod! Давайте сделаем совместные расходы веселыми!
""".trimIndent()

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
    }

    override fun onStart() {
        super.onStart()
        // Обновляем UI при возобновлении активности
        updateUI(auth.currentUser)
    }

    private fun downmenu() {
        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.keyboardfirst)

        val mainBtn: LinearLayout = findViewById(R.id.homeBtn)
        mainBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            mainBtn.startAnimation(clickAnimation)
        }

        val groupsBtn: LinearLayout = findViewById(R.id.groupsBtn)
        groupsBtn.setOnClickListener {
            startActivity(Intent(this, GroupsActivity::class.java))
            groupsBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)

        }

        val contactsBtn: LinearLayout = findViewById(R.id.contactsBtn)
        contactsBtn.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
            contactsBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }

        val proflBtn: LinearLayout = findViewById(R.id.profileBtn)
        proflBtn.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
            proflBtn.startAnimation(clickAnimation)
            overridePendingTransition(0, 0)
        }
    }

    private fun isUserRegistered(): Boolean {
        return auth.currentUser != null
    }

    private fun updateUI(user: FirebaseUser?) {
        val ptofilico: ImageView = findViewById(R.id.profileBtnIcon)
        val profilTxtBtn: TextView = findViewById(R.id.profileBtnText)

        if (user != null) {
            // Пользователь вошел в систему
            profilTxtBtn.setTextColor(ContextCompat.getColor(this, R.color.dely_blue))
            ptofilico.setImageResource(R.drawable.ic_person_outline_active)
            downmenu()
        } else {
        }
    }

}