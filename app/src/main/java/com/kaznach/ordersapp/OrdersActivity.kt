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

data class Order(
    val id: Int,
    val order_name: String,
    val order: String,
    val category: String,
    val subcategory: String,
    val order_deadline: String,
    val order_budget: String,
    val order_create: String,
    val order_region: String,
    val order_city: String
)

class OrdersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.orders_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ordersPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        MainToolbar.setupToolbar(this, R.id.appToolbar, "Мои заказы")

        val newOrderButton: Button = findViewById(R.id.newOrderButton)
        newOrderButton.setOnClickListener {
            val intent = Intent(this, CreateOrderActivity::class.java)
            startActivity(intent)
        }

        val connectionAndAuthManager = ConnectAndTokenManager(this, findViewById(R.id.ordersPage))
        connectionAndAuthManager.checkConnectionAndToken { success ->
            if (success) {
                loadOrdersData()
            } else {
                // Отображение ошибки
            }
        }
    }

    private fun loadOrdersData() {
        val ordersContainer: LinearLayout = findViewById(R.id.ordersContainer)
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

            Fuel.get(ApiConstants.ORDERS_URL)
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
                                        Snackbar.make(findViewById(R.id.ordersPage), errorMessage, Snackbar.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        val errorMessage = "Ошибка получения данных о заказах"
                                        Log.e("Fuel", errorMessage)
                                        Snackbar.make(findViewById(R.id.ordersPage), errorMessage, Snackbar.LENGTH_SHORT).show()
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
                                            val order = Order(
                                                orderObject.getInt("id"),
                                                orderObject.getString("order_name"),
                                                orderObject.getString("order"),
                                                orderObject.getString("category"),
                                                orderObject.getString("subcategory"),
                                                orderObject.getString("order_deadline"),
                                                orderObject.getString("order_budget"),
                                                orderObject.getString("order_create"),
                                                orderObject.getString("order_region"),
                                                orderObject.getString("order_city")
                                            )
                                            val orderView = createOrderView(order)
                                            ordersContainer.addView(orderView)
                                        }
                                    }
                                    404 ->{
                                        val errorMessage = "Нет созданных заказов"
                                        Log.w("Fuel", errorMessage)
                                        Snackbar.make(findViewById(R.id.ordersPage), errorMessage, Snackbar.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        val data = result.get()
                                        val errorMessage =
                                            "Ошибка: $data. Код ответа: ${response.statusCode}"
                                        Log.w("Fuel", errorMessage)
                                        Snackbar.make(findViewById(R.id.ordersPage), errorMessage, Snackbar.LENGTH_SHORT).show()
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


    private fun createOrderView(order: Order): View {
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
        innerLayout.addView(button)

        return orderLayout
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return MainToolbar.handleMenuItemClick(this, item.itemId) || super.onOptionsItemSelected(item)
    }
}