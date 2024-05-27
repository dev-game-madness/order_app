package com.kaznach.ordersapp

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject


class MyProfileActivity : AppCompatActivity() {

    private lateinit var elementsToDisable: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.my_profile_page)

        MainToolbar.setupToolbar(this, R.id.appToolbar, "Мой профиль")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.myProfilePage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val profileEmail: EditText = findViewById(R.id.profileEmail)
        profileEmail.isEnabled = false
        val saveButton: Button = findViewById(R.id.saveProfileButton)
        saveButton.isEnabled = false
        val profilePhone: EditText = findViewById(R.id.profilePhone)
        profilePhone.filters = arrayOf(EditTextFilters())
        val profileName: EditText = findViewById(R.id.profileName)
        val profileRegion: AutoCompleteTextView = findViewById(R.id.profileRegion)
        val profileCity: AutoCompleteTextView = findViewById(R.id.profileCity)
        val profileSpecialization: EditText = findViewById(R.id.profileSpecialization)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val isDataChanged = true
                saveButton.isEnabled = isDataChanged
            }
        }
        profileEmail.addTextChangedListener(textWatcher)
        profileName.addTextChangedListener(textWatcher)
        profilePhone.addTextChangedListener(textWatcher)
        profileRegion.addTextChangedListener(textWatcher)
        profileCity.addTextChangedListener(textWatcher)
        profileSpecialization.addTextChangedListener(textWatcher)

        val regionAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Russia.regionsAndCities.keys.toList())
        profileRegion.setAdapter(regionAdapter)
        profileRegion.setOnItemClickListener { _, _, position, _ ->
            val selectedRegion = regionAdapter.getItem(position) ?: ""
            val cities = Russia.regionsAndCities[selectedRegion] ?: emptyList()
            val cityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)
            profileCity.setAdapter(cityAdapter)
        }

        val scrollView = findViewById<ScrollView>(R.id.scrollViewProfile)

        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val keyboardHeight = imeInsets.bottom

            if (keyboardHeight > 0) {
                scrollView.setPadding(0, 0, 0, keyboardHeight)
            } else {
                scrollView.setPadding(0, 0, 0, 0)
            }

            insets
        }

        val editTextFields = listOf(
            profileEmail,
            profileName,
            profilePhone,
            profileRegion,
            profileCity,
            profileSpecialization
        )

        editTextFields.forEach { editText ->
            editText.setOnFocusChangeListener { view: View, hasFocus: Boolean ->
                if (hasFocus) {
                    scrollView.post {
                        val editTextCenterY = (view.top + view.bottom) / 2
                        val scrollViewCenterY = scrollView.height / 2
                        val scrollToY = editTextCenterY - scrollViewCenterY
                        scrollView.smoothScrollTo(0, scrollToY)
                    }
                }
            }
        }

        elementsToDisable = listOf(
            profilePhone,
            saveButton,
            profileName,
            profileRegion,
            profileCity,
            profileSpecialization
        )

        saveButton.setOnClickListener {
            saveButton.isEnabled = false
            val profileNameDB = profileName.text.toString()
            val profilePhoneDB = profilePhone.text.toString().trim()
            val profileRegionDB = profileRegion.text.toString()
            val profileCityDB = profileCity.text.toString()
            val profileSpecializationDB = profileSpecialization.text.toString()
            userFullReg(profileNameDB, profilePhoneDB, profileRegionDB, profileCityDB, profileSpecializationDB)
        }

        val connectionAndAuthManager = ConnectAndTokenManager(this, findViewById(R.id.myProfilePage))
        connectionAndAuthManager.checkConnectionAndToken { success ->
            if (success) {
                loadProfileData()
            } else {
                // Отображение ошибки
            }
        }

        disableElements()
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(R.id.myProfilePage), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun loadProfileData() {

        val profileEmail: EditText = findViewById(R.id.profileEmail)
        profileEmail.isEnabled = false
        val profileName: EditText = findViewById(R.id.profileName)
        val profilePhone: EditText = findViewById(R.id.profilePhone)
        val profileRegion: AutoCompleteTextView = findViewById(R.id.profileRegion)
        val profileCity: AutoCompleteTextView = findViewById(R.id.profileCity)
        val profileSpecialization: EditText = findViewById(R.id.profileSpecialization)

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
            Fuel.get(ApiConstants.PROFILE_URL)
                .header("Authorization" to "Bearer $token")
                .timeoutRead(3000)
                .responseString { _, response, result ->
                    runOnUiThread {
                        when (result) {
                            is Result.Failure -> {
                                val ex = result.getException()
                                val errorData = ex.response.data
                                val errorMessage = String(errorData, Charsets.UTF_8)
                                Log.e("Fuel", "Ошибка получения данных профиля: $errorMessage")
                                showError(errorMessage)
                            }
                            is Result.Success -> {
                                when (response.statusCode) {
                                    200 -> {
                                        // Успешное получение данных
                                        val data = result.get()
                                        val jsonObject = JSONObject(data)
                                        profileEmail.setText(jsonObject.optString("email", ""))
                                        profileName.setText(jsonObject.optString("company_name", ""))
                                        profilePhone.setText(jsonObject.optString("phone_num", ""))
                                        profileRegion.setText(jsonObject.optString("region", ""))
                                        profileCity.setText(jsonObject.optString("city", ""))
                                        profileSpecialization.setText(jsonObject.optString("category", ""))
                                        enableElements()
                                        profileEmail.isEnabled = false
                                    }
                                    else -> {
                                        val data = result.get()
                                        val errorMessage =
                                            "Ошибка: $data. Код ответа: ${response.statusCode}"
                                        Log.w("Fuel", errorMessage)
                                        showError(errorMessage)
                                    }
                                }
                            }
                        }
                    }
                }
        } else {
            runOnUiThread {
                showError("Ошибка авторизации. Пожалуйста, войдите снова.")
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun userFullReg(profileNameDB: String, profilePhoneDB: String, profileRegionDB: String, profileCityDB: String, profileSpecializationDB: String) {
        val jsonBody = """
                {
                  "profileNameDB": "${profileNameDB}",
                  "profilePhoneDB": "${profilePhoneDB}",
                  "profileRegionDB": "${profileRegionDB}",
                  "profileCityDB": "${profileCityDB}",
                  "profileSpecializationDB": "${profileSpecializationDB}"
                }
                """.trimIndent()
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
            Fuel.put(ApiConstants.PROFILE_URL)
                .header("Content-Type" to "application/json")
                .header("Authorization" to "Bearer $token")
                .timeoutRead(3000)
                .body(jsonBody)
                .responseString { _, response, result ->
                    runOnUiThread { // Добавляем runOnUiThread
                        when (result) {
                            is Result.Failure -> {
                                // Обработка ошибок
                                val ex = result.getException()
                                val errorData = ex.response.data
                                val errorMessage = String(errorData, Charsets.UTF_8)
                                Log.e("Fuel", "Ошибка регистрации: $errorMessage")
                                showError(errorMessage)
                            }

                            is Result.Success -> {
                                when (response.statusCode) {
                                    200 -> {
                                        // Успешная регистрация
                                        showSuccess("Данные успешно обновлены!")
                                    }

                                    else -> {
                                        // Обработка других кодов ответа
                                        val data = result.get()
                                        val errorMessage =
                                            "Ошибка: $data. Код ответа: ${response.statusCode}"
                                        Log.w("Fuel", errorMessage)
                                        showError(errorMessage)
                                    }
                                }
                            }
                        }
                    }
                }
        } else {
            runOnUiThread {
                // Обработка случая, когда токен отсутствует
                showError("Ошибка авторизации. Пожалуйста, войдите снова.")
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun showSuccess(emessageSuccess: String) {
        Snackbar.make(findViewById(R.id.myProfilePage), emessageSuccess, Snackbar.LENGTH_SHORT).show()
    }

    private fun enableElements() {
        elementsToDisable.forEach { it.isEnabled = true }
    }

    private fun disableElements() {
        elementsToDisable.forEach { it.isEnabled = false }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return MainToolbar.handleMenuItemClick(this, item.itemId) || super.onOptionsItemSelected(item)
    }
}