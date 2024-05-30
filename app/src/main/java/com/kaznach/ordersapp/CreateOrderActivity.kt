package com.kaznach.ordersapp

import ApiRequestConstructor
import SnackbarHelper
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.kittinunf.fuel.core.Method
import com.google.android.material.snackbar.Snackbar

class CreateOrderActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.create_order_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.createOrderPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        MainToolbar.setupToolbar(this, R.id.appToolbar, "Новый заказ")

        val navbarClickListener = MainNavbar(this)
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_orders).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_add).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_myorders).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener(navbarClickListener)

        val scrollView = findViewById<ScrollView>(R.id.scrollViewNewOrder)
        scrollView.visibility = View.GONE

        val connectionAndAuthManager = ConnectAndTokenManager(this, this)
        connectionAndAuthManager.checkConnectionAndToken { success ->
            if (success) {
                scrollView.visibility = View.VISIBLE
            } else {
                // Отображение ошибки
            }
        }

        val nameNewOrder: EditText = findViewById(R.id.nameNewOrder)
        val mainCategoryNewOrder: Spinner = findViewById(R.id.mainCategoryNewOrder)
        val subCategoryNewOrder: Spinner = findViewById(R.id.subCategoryNewOrder)
        val dateNewOrder: EditText = findViewById(R.id.dateNewOrder)
        val budgetNewOrder: EditText = findViewById(R.id.budgetNewOrder)
        val newOrder: EditText = findViewById(R.id.newOrder)

        // Получаем список основных категорий
        val mainCategories = OrderCategories.categoriesAndSubcategories.keys.toList()

// Создаем ArrayAdapter для Spinner основных категорий
        val mainCategoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mainCategories)
        mainCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

// Устанавливаем адаптер для Spinner основных категорий
        mainCategoryNewOrder.adapter = mainCategoryAdapter

// Добавляем слушатель для выбора значения в Spinner основных категорий
        mainCategoryNewOrder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                // Получаем выбранную основную категорию
                val selectedMainCategory = parent.getItemAtPosition(position).toString()

                // Получаем список подкатегорий для выбранной категории
                val subCategories = OrderCategories.categoriesAndSubcategories[selectedMainCategory]

                // Создаем ArrayAdapter для Spinner подкатегорий
                val subCategoryAdapter = ArrayAdapter(this@CreateOrderActivity, android.R.layout.simple_spinner_item, subCategories ?: emptyList())
                subCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                // Устанавливаем адаптер для Spinner подкатегорий
                subCategoryNewOrder.adapter = subCategoryAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Ничего не выбрано
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val keyboardHeight = imeInsets.bottom

            if (keyboardHeight > 0) {
                scrollView.setPadding(0, 0, 0, keyboardHeight)
            } else {
                scrollView.setPadding(0, 0, 0, 0)
            }

            insets
        }

        val editTextFields = listOf(
            mainCategoryNewOrder,
            subCategoryNewOrder,
            nameNewOrder,
            dateNewOrder,
            budgetNewOrder,
            newOrder
        )

        editTextFields.forEach { editText ->
            editText.setOnFocusChangeListener { view: View, hasFocus: Boolean ->
                if (hasFocus) {
                    scrollView.post {
                        val editTextCenterY = (view.top + view.bottom) / 2
                        val scrollViewCenterY = scrollView.height / 2
                        val scrollToY = editTextCenterY - scrollViewCenterY

                        scrollView.smoothScrollTo(0, scrollToY)
                    }
                }
            }
        }

        val createOrderButton: Button = findViewById(R.id.createOrderButton)
        createOrderButton.setOnClickListener {
            val name = nameNewOrder.text.toString()
            val mainCategory = mainCategoryNewOrder.selectedItem.toString()
            val subCategory = subCategoryNewOrder.selectedItem.toString()
            val date = dateNewOrder.text.toString()
            val budget = budgetNewOrder.text.toString()
            val description = newOrder.text.toString()

            createOrder(name, mainCategory, subCategory, date, budget, description)
        }
     }

    private fun createOrder(name: String, mainCategory: String, subCategory: String, date: String, budget: String, description: String) {

        val body = """
                {
                  "name": "${name}",
                  "mainCategory": "${mainCategory}",
                  "subCategory": "${subCategory}",
                  "date": "${date}",
                  "budget": "${budget}",
                  "description": "${description}"
                }
                """.trimIndent()

        val postRequest = ApiRequestConstructor(
            context = this,
            endpoint = "orders/create",
            method = Method.POST,
            body = body,
            onSuccess = { data ->
                runOnUiThread {
                    val dialog = Dialog(this)
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    dialog.setContentView(R.layout.create_oreder_dialog)
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialog.setCanceledOnTouchOutside(false)
                    dialog.setCancelable(false)
                    val okButton = dialog.findViewById<Button>(R.id.dialog_ok_button)
                    okButton.setOnClickListener {
                        dialog.dismiss()
                        finish()
                    }
                    dialog.show()
                }
                Log.d("API", "Успешный POST запрос: $data")
            },
            onFailure = { errorMessage, statusCode ->
                runOnUiThread {
                    val message = "Ошибка создания заказа. Код: $statusCode"
                    SnackbarHelper.showSnackbar(this, message, Snackbar.LENGTH_LONG, "ERROR")
                }
                Log.e("API", "Ошибка POST запроса: $errorMessage, код: $statusCode")
            }
        )
        postRequest.execute()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return MainToolbar.handleMenuItemClick(this, item.itemId) || super.onOptionsItemSelected(item)
    }
}