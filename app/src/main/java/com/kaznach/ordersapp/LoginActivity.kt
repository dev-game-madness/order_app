package com.kaznach.ordersapp

import ApiRequestConstructor
import SnackbarHelper
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.github.kittinunf.fuel.core.Method
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val linkToReg: TextView = findViewById(R.id.regLink)
        linkToReg.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        val userEmail: EditText = findViewById(R.id.logInEmail)
        val userPassword: EditText = findViewById(R.id.logInPassword)

        val logButton: Button = findViewById(R.id.logButton)

        logButton.setOnClickListener {
            val email = userEmail.text.toString().trim()
            val password = userPassword.text.toString().trim()

            fun EditText.checkEmpty(errorMessage: String) {
                if (text.isEmpty()) error = errorMessage
            }

            if (listOf(userEmail, userPassword).any { it.text.isEmpty() }) {
                userEmail.checkEmpty("Почта не может быть пустой")
                userPassword.checkEmpty("Пароль не может быть пустым")
            }
            else
                userLogin(email, password)
        }
    }

    private fun userLogin(email: String, password: String) {

        val body = """
                {
                  "email": "${email}",
                  "password": "${password}"
                }
                """.trimIndent()

        val postRequest = ApiRequestConstructor(
            context = this,
            endpoint = "users/log",
            method = Method.POST,
            body = body,
            onSuccess = { data ->
                runOnUiThread {
                    val jsonObject = JSONObject(data)
                    val receivedToken = jsonObject.getString("token")
                    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                    val sharedPrefs = EncryptedSharedPreferences.create(
                        "auth",
                        masterKeyAlias,
                        applicationContext,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                    with(sharedPrefs.edit()) {
                        putString("token", receivedToken)
                        apply()
                    }
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                }
                Log.d("API", "Успешный POST запрос: $data")
            },
            onFailure = { errorMessage, statusCode ->
                runOnUiThread {
                    val message = when (statusCode) {
                        401 -> "Неверный логин или пароль"
                        409 -> "Пользователя с такой почтой не существует"
                        else -> "Ошибка входа: Код $statusCode"
                    }
                    SnackbarHelper.showSnackbar(this, message, Snackbar.LENGTH_LONG, "ERROR")
                }
                Log.e("API", "Ошибка POST запроса: $errorMessage, код: $statusCode")
            }
        )
        postRequest.execute()
    }
}