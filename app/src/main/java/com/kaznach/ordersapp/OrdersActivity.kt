package com.kaznach.ordersapp

import ApiRequestConstructor
import SnackbarHelper
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.kittinunf.fuel.core.Method
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject

data class Order(
    val id: Int,
    val order_name: String,
    val order: String,
    val category: String,
    val subcategory: String,
    val order_deadline: Int,
    val order_budget: Int,
    val order_create: String,
    val order_address: String
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

        MainToolbar.setupToolbar(this, R.id.appToolbar, "Заказы")

        val navbarClickListener = MainNavbar(this)
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_orders).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_add).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_myorders).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener(navbarClickListener)

        val connectionAndAuthManager = ConnectAndTokenManager(this, this)
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
        val getRequest = ApiRequestConstructor(
            context = this,
            endpoint = "orders/all",
            method = Method.GET,
            onSuccess = { data ->
                runOnUiThread {
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
                            orderObject.getInt("order_deadline"),
                            orderObject.getInt("order_budget"),
                            orderObject.getString("order_create"),
                            orderObject.getString("order_address"),
                        )
                        val orderView = createOrderView(order)
                        ordersContainer.addView(orderView)
                    }
                    Log.d("API", "Успешный GET запрос: $data")
                }
            },
            onFailure = { errorMessage, statusCode ->
                runOnUiThread {
                    val message = when (statusCode) {
                        404 -> "Нет ни одного заказа"
                        else -> "Ошибка получения данных о заказах: Код $statusCode"
                    }
                    ordersContainer.addView(emptyOrders(message))
                }
                Log.e("API", "Ошибка GET запроса: $errorMessage, код: $statusCode")
            }
        )
        getRequest.execute()
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
        regionTextView.text = "${order.order_address}"
        innerLayout.addView(regionTextView)

        val categoryTextView = TextView(this)
        categoryTextView.text = "${order.category}, ${order.subcategory}"
        innerLayout.addView(categoryTextView)

        val budgetAndDeadlineTextView = TextView(this)
        budgetAndDeadlineTextView.text = "\nБюджет: ${order.order_budget} рублей\nСрок выполения: ${order.order_deadline} дней"
        innerLayout.addView(budgetAndDeadlineTextView)

        val createTextView = TextView(this)
        createTextView.text = "\n${order.order_create}\n"
        createTextView.setTextColor(0xFFFF0000.toInt())
        innerLayout.addView(createTextView)

        val button = MaterialButton(this)
        button.text = "Подробнее"
        button.setOnClickListener {
            // Создаем Intent для перехода на OrderDetailsActivity
            val intent = Intent(this, OrderDetailsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            // Добавляем информацию о заказе в Intent
            intent.putExtra("order_id", order.id)
            intent.putExtra("order_name", order.order_name)
            intent.putExtra("order_description", order.order)
            intent.putExtra("category", order.category)
            intent.putExtra("subcategory", order.subcategory)
            intent.putExtra("order_deadline", order.order_deadline)
            intent.putExtra("order_budget", order.order_budget)
            intent.putExtra("order_create", order.order_create)
            intent.putExtra("order_region", order.order_address)

            startActivity(intent)
        }
        innerLayout.addView(button)

        return orderLayout
    }

    private fun emptyOrders(message: String): View {
        val textView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = android.view.Gravity.CENTER
                setMargins(16, 16, 16, 16)
            }
            text = message
            setTypeface(null, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
        return textView
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return MainToolbar.handleMenuItemClick(this, item.itemId) || super.onOptionsItemSelected(item)
    }
}