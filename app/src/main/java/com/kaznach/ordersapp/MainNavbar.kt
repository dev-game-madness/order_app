package com.kaznach.ordersapp

import android.app.Activity
import android.content.Intent
import android.view.View

class MainNavbar(private val activity: Activity) : View.OnClickListener {
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.nav_home -> {
                val intent = Intent(activity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                activity.startActivity(intent)
                true
            }
            R.id.nav_orders -> {
                val intent = Intent(activity, OrdersActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
                true
            }
            R.id.nav_add -> {
                val intent = Intent(activity, CreateOrderActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
            }
            R.id.nav_myorders -> {
                val intent = Intent(activity, MyOrderActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
                true
            }
            R.id.nav_profile -> {
                val intent = Intent(activity, MyProfileActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
                true
            }
        }
    }
}