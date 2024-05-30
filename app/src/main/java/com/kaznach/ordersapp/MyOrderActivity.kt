package com.kaznach.ordersapp

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject

data class MyOrder(
    val id: Int,
    val order_name: String,
    val order: String,
    val category: String,
    val subcategory: String,
    val order_deadline: Int,
    val order_budget: Int,
    val order_create: String,
    val order_region: String,
    val order_city: String
)

class MyOrderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.my_order_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.myOrderPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        MainToolbar.setupToolbar(this, R.id.appToolbar, "Мои заказы")

        val navbarClickListener = MainNavbar(this)
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_orders).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_add).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_myorders).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener(navbarClickListener)

        val archiveOrderButton: Button = findViewById(R.id.myArchiveOrderButton)
        archiveOrderButton.setOnClickListener {
            val intent = Intent(this, MyArchiveOrdersActivity::class.java)
            startActivity(intent)
        }

        val connectionAndAuthManager = ConnectAndTokenManager(this, this)
        connectionAndAuthManager.checkConnectionAndToken { success ->
            if (success) {
                loadOrdersData()
            } else {
                // Отображение ошибки (уже обрабатывается в ConnectAndTokenManager)
            }
        }
    }

    private fun loadOrdersData() {
        val ordersContainer: LinearLayout = findViewById(R.id.myOrdersContainer)
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPrefs = EncryptedSharedPreferences.create(
            "auth",
            masterKeyAlias,
            applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = sharedPrefs.getString("token", null)

        if (token != null) {

            Fuel.get(ApiConstants.URLS["orders/myorders"].toString())
                .header("Authorization" to "Bearer $token")
                .timeoutRead(3000)
                .responseString { _, response, result ->
                    runOnUiThread {
                        when (result) {
                            is Result.Failure -> {
                                when (response.statusCode) {
                                    404 ->{
                                        val errorMessage = "Нет созданных заказов"
                                        Log.w("Fuel", errorMessage)
                                        Snackbar.make(findViewById(R.id.myOrderPage), errorMessage, Snackbar.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        val errorMessage = "Ошибка получения данных о заказах"
                                        Log.e("Fuel", errorMessage)
                                        Snackbar.make(findViewById(R.id.myOrderPage), errorMessage, Snackbar.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            is Result.Success -> {
                                when (response.statusCode) {
                                    200 -> {
                                        val data = result.get()
                                        val jsonObject = JSONObject(data)
                                        val ordersArray = jsonObject.getJSONArray("orders")

                                        for (i in 0 until ordersArray.length()) {
                                            val orderObject = ordersArray.getJSONObject(i)
                                            val order = MyOrder(
                                                orderObject.getInt("id"),
                                                orderObject.getString("order_name"),
                                                orderObject.getString("order"),
                                                orderObject.getString("category"),
                                                orderObject.getString("subcategory"),
                                                orderObject.getInt("order_deadline"),
                                                orderObject.getInt("order_budget"),
                                                orderObject.getString("order_create"),
                                                orderObject.getString("order_region"),
                                                orderObject.getString("order_city")
                                            )
                                            val orderView = createOrderView(order)
                                            ordersContainer.addView(orderView)
                                        }
                                    }
                                    else -> {
                                        val data = result.get()
                                        val errorMessage =
                                            "Ошибка: $data. Код ответа: ${response.statusCode}"
                                        Log.w("Fuel", errorMessage)
                                        showError(errorMessage)
                                    }
                                }
                            }
                        }
                    }
                }
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }


    private fun createOrderView(order: MyOrder): View {
        val orderLayout = CardView(this)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(16, 16, 16, 16)
        orderLayout.layoutParams = layoutParams
        orderLayout.setContentPadding(32, 32, 32, 32)
        orderLayout.radius = 50f

        val innerLayout = LinearLayout(this)
        innerLayout.orientation = LinearLayout.VERTICAL
        innerLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        orderLayout.addView(innerLayout)

        val idTextView = TextView(this)
        idTextView.text = "Заказ №${order.id}, ${order.order_name}"
        idTextView.setTypeface(null, Typeface.BOLD)
        idTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        innerLayout.addView(idTextView)

        val regionTextView = TextView(this)
        regionTextView.text = "${order.order_region}, ${order.order_city}"
        innerLayout.addView(regionTextView)

        val categoryTextView = TextView(this)
        categoryTextView.text = "${order.category}, ${order.subcategory}"
        innerLayout.addView(categoryTextView)

        val budgetAndDeadlineTextView = TextView(this)
        budgetAndDeadlineTextView.text = "\nБюджет: ${order.order_budget} рублей\nСрок выполенния: ${order.order_deadline} дней"
        innerLayout.addView(budgetAndDeadlineTextView)

        val createTextView = TextView(this)
        createTextView.text = "\nДата размещения: ${order.order_create}\n"
        innerLayout.addView(createTextView)

        val button = MaterialButton(this)
        button.text = "Подробнее"
        button.setOnClickListener {
            // Создаем Intent для перехода на OrderDetailsActivity
            val intent = Intent(this, OrderDetailsActivity::class.java)
            // Добавляем информацию о заказе в Intent
            intent.putExtra("order_id", order.id)
            intent.putExtra("order_name", order.order_name)
            intent.putExtra("order_description", order.order)
            intent.putExtra("category", order.category)
            intent.putExtra("subcategory", order.subcategory)
            intent.putExtra("order_deadline", order.order_deadline)
            intent.putExtra("order_budget", order.order_budget)
            intent.putExtra("order_create", order.order_create)
            intent.putExtra("order_region", order.order_region)
            intent.putExtra("order_city", order.order_city)

            startActivity(intent)
        }
        innerLayout.addView(button)

        return orderLayout
    }

    private fun showError(emessageSuccess: String) {
        Snackbar.make(findViewById(R.id.myOrderPage), emessageSuccess, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return MainToolbar.handleMenuItemClick(this, item.itemId) || super.onOptionsItemSelected(item)
    }
}