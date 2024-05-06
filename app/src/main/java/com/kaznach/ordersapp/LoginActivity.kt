package com.kaznach.ordersapp

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
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.google.android.material.snackbar.Snackbar
import org.json.JSONException
import org.json.JSONObject

const val logURL = "http://192.168.1.104:5000/api/v1/users/log"

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
        val jsonBody = """
                {
                  "email": "${email}",
                  "password": "${password}"
                }
                """.trimIndent()

        Fuel.post(logURL)
            .header("Content-Type" to "application/json")
            .timeoutRead(3000)
            .body(jsonBody)
            .responseString { _, response, result ->
                when (result) {
                    is Result.Failure -> {
                        when (response.statusCode) {
                            401 -> showError("Неверный логин или пароль")
                            409 -> showError("Пользователя с такой почтой не существует")
                            else -> {
                                val ex = result.getException()
                                val errorData = ex.response.data
                                val errorMessage = String(errorData, Charsets.UTF_8) // Получаем сообщение об ошибке из ответа
                                Log.e("Fuel", "Ошибка входа: $errorMessage")
                                showError(errorMessage)
                            }
                        }

                    }
                    is Result.Success -> {
                        when (response.statusCode) {
                            200 -> {
                                val data = result.get()
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
                                startActivity(intent)
                                finish()
                            }

                            else -> {
                                val data = result.get()
                                val errorMessage = "Ошибка: $data. Код ответа: ${response.statusCode}"
                                Log.w("Fuel", errorMessage)
                                showError(errorMessage)
                            }
                        }
                    }
                }
            }

    }
    private fun showError(message: String) {
        Snackbar.make(findViewById(R.id.loginPage), message, Snackbar.LENGTH_LONG).show()
    }
}