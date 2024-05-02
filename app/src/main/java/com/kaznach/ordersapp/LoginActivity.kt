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
                if (text.isEmpty()) setError(errorMessage)
            }

            if (listOf(userEmail, userPassword).any { it.text.isEmpty() }) {
                userEmail.checkEmpty("Почта не может быть пустой!")
                userPassword.checkEmpty("Пароль не может быть пустым!")
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
            .body(jsonBody)
            .responseString { _, response, result ->
                when (result) {
                    is Result.Failure -> {
                        val ex = result.getException()
                        val statusCode = ex.response.statusCode
                        val errorBody = ex.response.body().asString("application/json")  // Получаем тело ответа как JSON
                        val errorMessage = when (statusCode) {
                            400 -> "Ошибка: Необработанное исключение"
                            401 -> "Ошибка: Неверный логин или пароль"
                            409 -> "Ошибка: Пользователя с таким email не существует"
                            else -> try {
                                // Попытаемся получить сообщение об ошибке из JSON
                                JSONObject(errorBody).getString("login_error")
                            } catch (e: JSONException) {
                                "Ошибка: ${ex.message}"  // Если JSON невалиден, используем общее сообщение
                            }
                        }
                        Log.e("Fuel", errorMessage)
                        Snackbar.make(findViewById(R.id.loginPage), errorMessage, Snackbar.LENGTH_LONG).show()
                    }
                    is Result.Success -> {
                        val statusCode = response.statusCode
                        if (statusCode == 200) {
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