package com.kaznach.ordersapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.result.Result
import com.google.android.material.snackbar.Snackbar
import org.json.JSONException
import org.json.JSONObject

const val regURL = "http://192.168.1.104:5000/api/v1/users/reg"

class RegistrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.registration_page)

        val linkToLog: TextView = findViewById(R.id.logLink)
        linkToLog.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val userEmail: EditText = findViewById(R.id.regInEmail)
        val userPassword: EditText = findViewById(R.id.regInPassword)
        val userRePassword: EditText = findViewById(R.id.regReInPassword)

        val regButton: Button = findViewById(R.id.regButton)

        regButton.setOnClickListener {
            val email = userEmail.text.toString().trim()
            val password = userPassword.text.toString().trim()
            val repassword = userRePassword.text.toString().trim()

            fun EditText.checkEmpty(errorMessage: String) {
                if (text.isEmpty()) setError(errorMessage)
            }

            if (listOf(userEmail, userPassword, userRePassword).any { it.text.isEmpty() }) {
                userEmail.checkEmpty("Почта не может быть пустой!")
                userPassword.checkEmpty("Пароль не может быть пустым!")
                userRePassword.checkEmpty("Повторите пароль!")
            }
            else
                if (password != repassword) {
                    userRePassword.setError("Пароли не совпадают!")
                }
                else
                    userRegistration(email, password)
        }

    }

    private fun userRegistration(email: String, password: String) {

        val jsonBody = """
                {
                  "email": "${email}",
                  "password": "${password}"
                }
                """.trimIndent()

        Fuel.post(regURL)
            .header("Content-Type" to "application/json")
            .body(jsonBody)
            .responseString { _, response, result ->
                when (result) {
                    is Result.Failure -> {
                        val ex = result.getException()
                        val statusCode = ex.response.statusCode
                        val errorBody = ex.response.body().asString("application/json")  // Получаем тело ответа как JSON
                        val errorMessage = when (statusCode) {
                            400 -> "Ошибка: Неверные данные"
                            409 -> "Ошибка: Пользователь с таким email уже существует"
                            else -> try {
                                // Попытаемся получить сообщение об ошибке из JSON
                                JSONObject(errorBody).getString("error")
                            } catch (e: JSONException) {
                                "Ошибка: ${ex.message}"  // Если JSON невалиден, используем общее сообщение
                            }
                        }
                        Log.e("Fuel", errorMessage)
                        Snackbar.make(findViewById(R.id.registrationPage), errorMessage, Snackbar.LENGTH_LONG).show()
                    }
                    is Result.Success -> {
                        val statusCode = response.statusCode
                        if (statusCode == 201) {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                        } else {
                            // Неожиданный код ответа
                            Log.w("Fuel", "Неожиданный код ответа: $statusCode")
                        }
                    }
                }
            }

    }
}