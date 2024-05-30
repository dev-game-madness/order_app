package com.kaznach.ordersapp

import ApiRequestConstructor
import SnackbarHelper
import android.os.Bundle
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
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.kittinunf.fuel.core.Method
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject


class MyProfileActivity : AppCompatActivity() {

    private lateinit var elementsToDisable: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.my_profile_page)

        MainToolbar.setupToolbar(this, R.id.appToolbar, "Мой профиль")

        val navbarClickListener = MainNavbar(this)
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_orders).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_add).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_myorders).setOnClickListener(navbarClickListener)
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener(navbarClickListener)

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
        scrollView.visibility = View.GONE

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

        val connectionAndAuthManager = ConnectAndTokenManager(this, this)
        connectionAndAuthManager.checkConnectionAndToken { success ->
            if (success) {
                scrollView.visibility = View.VISIBLE
                loadProfileData()
            } else {
                // Отображение ошибки
            }
        }

        disableElements()
    }

    private fun loadProfileData() {

        val profileEmail: EditText = findViewById(R.id.profileEmail)
        profileEmail.isEnabled = false
        val profileName: EditText = findViewById(R.id.profileName)
        val profilePhone: EditText = findViewById(R.id.profilePhone)
        val profileRegion: AutoCompleteTextView = findViewById(R.id.profileRegion)
        val profileCity: AutoCompleteTextView = findViewById(R.id.profileCity)
        val profileSpecialization: EditText = findViewById(R.id.profileSpecialization)


        val getRequest = ApiRequestConstructor(
            context = this,
            endpoint = "users/profile",
            method = Method.GET,
            onSuccess = { data ->
                runOnUiThread {
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
                Log.d("API", "Успешный GET запрос: $data")
            },
            onFailure = { errorMessage, statusCode ->
                runOnUiThread {
                    val message = "Ошибка получения данных профиля. Код: $statusCode"
                    SnackbarHelper.showSnackbar(this, message, Snackbar.LENGTH_LONG, "ERROR")
                }
                Log.e("API", "Ошибка GET запроса: $errorMessage, код: $statusCode")
            }
        )

        // Выполнение запроса
        getRequest.execute()
    }

    private fun userFullReg(profileNameDB: String, profilePhoneDB: String, profileRegionDB: String, profileCityDB: String, profileSpecializationDB: String) {

        val body = """
            {
              "profileNameDB": "${profileNameDB}",
              "profilePhoneDB": "${profilePhoneDB}",
              "profileRegionDB": "${profileRegionDB}",
              "profileCityDB": "${profileCityDB}",
              "profileSpecializationDB": "${profileSpecializationDB}"
            }
            """.trimIndent()

        val postRequest = ApiRequestConstructor(
            context = this,
            endpoint = "users/profile",
            method = Method.POST,
            body = body,
            onSuccess = { data ->
                runOnUiThread {
                    val message = "Данные успешно обновлены!"
                    SnackbarHelper.showSnackbar(this, message, Snackbar.LENGTH_LONG, "SUCCESS")
                }
                // Обработка успешного ответа
                Log.d("API", "Успешный POST запрос: $data")
            },
            onFailure = { errorMessage, statusCode ->
                runOnUiThread {
                    val message = "Ошибка обновления профиля. Код:"
                    SnackbarHelper.showSnackbar(this, message, Snackbar.LENGTH_LONG, "ERROR")
                }
                    Log.e("API", "Ошибка POST запроса: $errorMessage, код: $statusCode")
                }
        )

        postRequest.execute()
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