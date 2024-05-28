package com.kaznach.ordersapp

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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
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

        val scrollView = findViewById<ScrollView>(R.id.scrollViewNewOrder)

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

        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPrefs = EncryptedSharedPreferences.create(
            "auth",
            masterKeyAlias,
            applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = sharedPrefs.getString("token", null)

        val jsonBody = """
                {
                  "name": "${name}",
                  "mainCategory": "${mainCategory}",
                  "subCategory": "${subCategory}",
                  "date": "${date}",
                  "budget": "${budget}",
                  "description": "${description}"
                }
                """.trimIndent()

        Fuel.post(ApiConstants.CREATE_ORDER_URL)
            .header("Authorization", "Bearer $token")
            .header("Content-Type" to "application/json")
            .timeoutRead(3000)
            .body(jsonBody)
            .responseString { _, response, result ->
                when (result) {
                    is Result.Failure -> {
                        val ex = result.getException()
                        val errorData = ex.response.data
                        val errorMessage = String(errorData, Charsets.UTF_8)
                        Log.e("Fuel", "Ошибка создания заказа. Код: ${response.statusCode}")
                        showError(errorMessage)
                    }
                    is Result.Success -> {
                        when (response.statusCode) {
                            201 -> {
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
                            }
                            else -> {
                                val errorMessage = "Ошибка создания заказа. Код: ${response.statusCode}"
                                Log.w("Fuel", errorMessage)
                                showError(errorMessage)
                            }
                        }
                    }
                }
            }

    }
    private fun showError(message: String) {
        Snackbar.make(findViewById(R.id.createOrderPage), message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return MainToolbar.handleMenuItemClick(this, item.itemId) || super.onOptionsItemSelected(item)
    }
}