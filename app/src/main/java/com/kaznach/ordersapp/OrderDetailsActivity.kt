package com.kaznach.ordersapp

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OrderDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.order_details_page)
        val ordersContainer: LinearLayout = findViewById(R.id.detailsOrderContainer)

        // Получаем информацию о заказе из Intent
        val orderId = intent.getIntExtra("order_id", 0)
        val orderName = intent.getStringExtra("order_name") ?: ""
        val orderDescription = intent.getStringExtra("order_description") ?: ""
        val orderCategory = intent.getStringExtra("category") ?: ""
        val orderSubcategory = intent.getStringExtra("subcategory") ?: ""
        val orderDeadline = intent.getIntExtra("order_deadline", 0)
        val orderBudget = intent.getIntExtra("order_budget", 0)
        val orderCreate = intent.getStringExtra("order_create") ?: ""
        val orderRegion = intent.getStringExtra("order_region") ?: ""
        val orderCity = intent.getStringExtra("order_city") ?: ""

        MainToolbar.setupToolbar(this, R.id.appToolbar, "Заказ №${orderId}")

        val navbarClickListener = MainNavbar(this)
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_orders).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_add).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_myorders).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener(navbarClickListener)

        displayOrderDetails(orderId,
            orderName,
            orderDescription,
            orderCategory,
            orderSubcategory,
            orderDeadline,
            orderBudget,
            orderCreate,
            orderRegion,
            orderCity, ordersContainer)
    }

    private fun displayOrderDetails(
        orderId: Int,
        orderName: String,
        orderDescription: String,
        orderCategory: String,
        orderSubcategory: String,
        orderDeadline: Int,
        orderBudget: Int,
        orderCreate: String,
        orderRegion: String,
        orderCity: String,
        ordersContainer: LinearLayout
    ) {
        val innerLayout = LinearLayout(this)
        innerLayout.orientation = LinearLayout.VERTICAL
        innerLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        ordersContainer.addView(innerLayout)

        createTextView(orderName, innerLayout, true, 22f)
        createTextView(orderCreate, innerLayout, true)
        createTextView("$orderRegion, $orderCity", innerLayout)
        createTextView("\nДанные заказа", innerLayout, true, 18f)
        createTextView("\nКатегория: $orderCategory, $orderSubcategory", innerLayout)
        createTextView("\nБюджет: $orderBudget рублей\nСрок выполения: $orderDeadline дней", innerLayout)
        createTextView("\nОписание", innerLayout, true, 18f)
        createTextView("\n$orderDescription", innerLayout)
    }

    private fun createTextView(text: String, parentLayout: LinearLayout, bold: Boolean = false, size: Float = 16f) {
        val textView = TextView(this)
        textView.text = text
        if (bold) {
            textView.setTypeface(null, Typeface.BOLD)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        }
        parentLayout.addView(textView)
    }
}