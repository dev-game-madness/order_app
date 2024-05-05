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

const val checkURL = "http://192.168.1.104:5000/api/v1/sessions/check"

class MainActivity : AppCompatActivity() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var snackbar: Snackbar
    private lateinit var timer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Создаем Snackbar для уведомления
        snackbar = Snackbar.make(findViewById(R.id.mainPage), "Нет подключения к интернету", Snackbar.LENGTH_INDEFINITE)

        // Создаем таймер для обратного отсчета
        timer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                snackbar.setText("Нет подключения к интернету. Повторная попытка через ${millisUntilFinished / 1000} секунд")
            }

            override fun onFinish() {
                checkConnectionAndToken()
            }
        }

        // Запускаем первую проверку подключения
        checkConnectionAndToken()

        val linkToReg: TextView = findViewById(R.id.button2)
        linkToReg.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        val linkToLog: TextView = findViewById(R.id.button3)
        linkToLog.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkConnectionAndToken() {
        if (isNetworkAvailable()) {
            // Интернет доступен, проверяем токен
            snackbar.dismiss() // Скрываем Snackbar, если он отображается
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
        } else {
            // Интернет недоступен, отображаем уведомление и запускаем таймер
            snackbar.show()
            timer.start()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

}