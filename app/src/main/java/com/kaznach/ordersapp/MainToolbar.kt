package com.kaznach.ordersapp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.github.kittinunf.fuel.Fuel

object MainToolbar {
    fun setupToolbar(activity: AppCompatActivity, toolbarId: Int, toolbarTitle: String) {
        val toolbar: Toolbar = activity.findViewById(toolbarId)
        activity.setSupportActionBar(toolbar)
        activity.supportActionBar?.setDisplayShowTitleEnabled(true)
        activity.supportActionBar?.title = toolbarTitle
        toolbar.setNavigationOnClickListener {
            activity.finish()
        }
    }

    fun handleMenuItemClick(activity: AppCompatActivity, itemId: Int): Boolean {
        return when (itemId) {
            R.id.ItemMyProfile -> {
                val intent = Intent(activity, MyProfileActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
                true
            }
            R.id.ItemMyOrders -> {
                val intent = Intent(activity, MyOrderActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
                true
            }
            R.id.ItemOrders -> {
                val intent = Intent(activity, OrdersActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
                true
            }
            R.id.ItemExit -> {
                closeSession(activity)
                logoutUser(activity)
                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                activity.startActivity(intent)
                true
            }
            else -> false
        }
    }

    private fun closeSession(context: Context) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPrefs = EncryptedSharedPreferences.create(
            "auth",
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = sharedPrefs.getString("token", null)

        if (token != null) {
            Fuel.put(ApiConstants.URLS["users/logout"].toString())
                .header("Authorization" to "Bearer $token")
                .timeout(3000)
                .response { _, _, _ -> }
        }
    }

    private fun logoutUser(context: Context) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPrefs = EncryptedSharedPreferences.create(
            "auth",
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        with(sharedPrefs.edit()) {
            clear()
            apply()
        }
    }
}