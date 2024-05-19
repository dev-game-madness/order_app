package com.kaznach.ordersapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.github.kittinunf.fuel.Fuel
import com.google.android.material.snackbar.Snackbar
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar

const val checkURL = "http://192.168.1.104:5000/api/v1/sessions/check"
const val checkConnURL = "http://192.168.1.104:5000/api/v1/connection/check"
const val logoutURL = "http://192.168.1.104:5000/api/v1/users/logout"
const val profileURL = "http://192.168.1.104:5000/api/v1/users/profile"

class MainActivity : AppCompatActivity() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var snackbar: Snackbar
    private lateinit var nettimer: CountDownTimer
    private lateinit var conntimer: CountDownTimer

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ItemMyProfile -> {
                val intent = Intent(this, MyProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.ItemMyOrders -> {
                true
            }
            R.id.ItemOrders -> {
                true
            }
            R.id.ItemExit -> {
                closeSession()
                logoutUser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.appToolbar)
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        snackbar = Snackbar.make(findViewById(R.id.mainPage), "Нет подключения к интернету", Snackbar.LENGTH_INDEFINITE)

        nettimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                snackbar.setText("Нет подключения к интернету. Повторная попытка через ${millisUntilFinished / 1000} секунд")
            }

            override fun onFinish() {
                checkNetworkAndConnect()
            }
        }

        conntimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                snackbar.setText("Сервер недоступен. Повторная попытка через ${millisUntilFinished / 1000} секунд")
            }

            override fun onFinish() {
                checkNetworkAndConnect()
            }
        }

        checkNetworkAndConnect()

    }

    private fun closeSession() {
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
            Fuel.put(logoutURL)
                .header("Authorization" to "Bearer $token")
                .timeout(3000)
                .response { _, _, _ -> }
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun logoutUser() {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPrefs = EncryptedSharedPreferences.create(
            "auth",
            masterKeyAlias,
            applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
            with(sharedPrefs.edit()) {
                clear()
                apply()
            }
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun checkConnectionServer(completion: (Boolean) -> Unit) {
        if (isNetworkAvailable()) {
            Fuel.head(checkConnURL)
                .timeout(2000)
                .response { request, response, result ->
                    result.fold(
                        { data ->
                            completion(response.statusCode == 200)
                        },
                        { error ->
                            completion(false)
                        }
                    )
                }
        }
    }

    private fun checkNetworkAndConnect() {
        when {
            isNetworkAvailable() -> {
                snackbar.dismiss()
                checkConnectionServer { isServerAvailable ->
                    runOnUiThread {
                        when {
                            isServerAvailable -> {
                                snackbar.dismiss()
                                checkToken()
                            }
                            else -> {
                                snackbar.show()
                                conntimer.start()
                            }
                        }
                    }
                }
            }
            else -> {
                snackbar.show()
                nettimer.start()
            }
        }
    }

    private fun checkToken() {
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
            Fuel.get(checkURL)
                .header("Authorization" to "Bearer ${token}")
                .response { _, response, result ->
                    if (response.statusCode == 200) {
                        val message = "Сессия найдена"
                        Snackbar.make(findViewById(R.id.mainPage), message, Snackbar.LENGTH_LONG).show()
                    } else {
                        startActivity(Intent(this, LoginActivity::class.java))
                        val data = result.get()
                        Log.e("Fuel", "${data}")
                        finish()
                    }
                }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}