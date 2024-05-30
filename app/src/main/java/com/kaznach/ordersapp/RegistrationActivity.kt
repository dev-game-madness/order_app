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
import com.google.android.material.snackbar.Snackbar

class RegistrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.registration_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registrationPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val linkToLog: TextView = findViewById(R.id.logLink)
        linkToLog.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
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
                if (text.isEmpty()) error = errorMessage
            }

            if (listOf(userEmail, userPassword, userRePassword).any { it.text.isEmpty() }) {
                userEmail.checkEmpty("Почта не может быть пустой")
                userPassword.checkEmpty("Пароль не может быть пустым")
                userRePassword.checkEmpty("Повторите пароль")
            }
            else
                if (password != repassword) {
                    userRePassword.error = "Пароли не совпадают"
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

        Fuel.post(ApiConstants.URLS["users/reg"].toString())
            .header("Content-Type" to "application/json")
            .timeoutRead(3000)
            .body(jsonBody)
            .responseString { _, response, result ->
                when (response.statusCode) {
                    201 -> {
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                    }
                    409 -> showError("Пользователь с такой почтой уже существует")
                    else -> {
                        val data = result.get()
                        val errorMessage = "Непредвиденная ошибка ${data}. Код ответа: ${response.statusCode}"
                        Log.w("Fuel", errorMessage)
                        showError(errorMessage)
                    }
                }
            }
    }
    private fun showError(message: String) {
        Snackbar.make(findViewById(R.id.registrationPage), message, Snackbar.LENGTH_LONG).show()
    }
}