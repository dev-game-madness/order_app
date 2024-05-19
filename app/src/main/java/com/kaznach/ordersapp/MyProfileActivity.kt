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

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var snackbar: Snackbar
    private lateinit var nettimer: CountDownTimer
    private lateinit var conntimer: CountDownTimer

    // Список элементов, которые нужно деактивировать
    private lateinit var elementsToDisable: List<View>

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ItemMyProfile -> {
                val intent = Intent(this, MyProfileActivity::class.java)
                startActivity(intent)
                finish()
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
        setContentView(R.layout.my_profile_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.myProfilePage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.appToolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.backarrow)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        snackbar = Snackbar.make(findViewById(R.id.myProfilePage), "Нет подключения к интернету", Snackbar.LENGTH_INDEFINITE)

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

        val profileEmail: EditText = findViewById(R.id.profileEmail)
        profileEmail.isEnabled = false

        elementsToDisable = listOf(
            findViewById(R.id.profilePhone),
            findViewById(R.id.saveProfileButton),
            findViewById(R.id.profileName),
            findViewById(R.id.profileINN),
            findViewById(R.id.profileRegion),
            findViewById(R.id.profileCity),
            findViewById(R.id.profileSpecialization)
        )

        disableElements()

        checkNetworkAndConnect()
    }

    private fun layoutPage(){
        loadProfileData()
        enableElements()
        val profileEmail: EditText = findViewById(R.id.profileEmail)
        profileEmail.isEnabled = false

        val saveButton: Button = findViewById(R.id.saveProfileButton)
        saveButton.isEnabled = false

        val profilePhone: EditText = findViewById(R.id.profilePhone)
        profilePhone.setText("+7")
        profilePhone.filters = arrayOf(EditTextFilters())


        val profileName: EditText = findViewById(R.id.profileName)
        val profileINN: EditText = findViewById(R.id.profileINN)
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
        profileINN.addTextChangedListener(textWatcher)
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
            profileINN,
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

        saveButton.setOnClickListener {
            saveButton.isEnabled = false

            val profileNameDB = profileName.text.toString()
            val profilePhoneDB = profilePhone.text.toString().trim()
            val profileINNDB = profileINN.text.toString().trim()
            val profileRegionDB = profileRegion.text.toString()
            val profileCityDB = profileCity.text.toString()
            val profileSpecializationDB = profileSpecialization.text.toString()

            userFullReg(profileNameDB, profilePhoneDB, profileINNDB, profileRegionDB, profileCityDB, profileSpecializationDB)
        }
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(R.id.myProfilePage), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun loadProfileData() {

        val profileEmail: EditText = findViewById(R.id.profileEmail)
        profileEmail.isEnabled = false
        val profileName: EditText = findViewById(R.id.profileName)
        val profilePhone: EditText = findViewById(R.id.profilePhone)
        val profileINN: EditText = findViewById(R.id.profileINN)
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
            Fuel.get(profileURL) // profileDataURL - URL для получения данных профиля
                .header("Authorization" to "Bearer $token") // Добавляем токен авторизации
                .timeoutRead(3000)
                .responseString { _, response, result ->
                    runOnUiThread {
                        when (result) {
                            is Result.Failure -> {
                                // Обработка ошибок
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
                                        profileINN.setText(jsonObject.optString("inn", ""))
                                        profileRegion.setText(jsonObject.optString("region", ""))
                                        profileCity.setText(jsonObject.optString("city", ""))
                                        profileSpecialization.setText(jsonObject.optString("category", ""))
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
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun userFullReg(profileNameDB: String, profilePhoneDB: String, profileINNDB: String, profileRegionDB: String, profileCityDB: String, profileSpecializationDB: String) {
        val jsonBody = """
                {
                  "profileNameDB": "${profileNameDB}",
                  "profilePhoneDB": "${profilePhoneDB}",
                  "profileINNDB": "${profileINNDB}",
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
            Fuel.put(profileURL) // registrationURL - URL для регистрации/обновления данных
                .header("Content-Type" to "application/json")
                .header("Authorization" to "Bearer $token") // Добавляем токен авторизации
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
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun showSuccess(emessageSuccess: String) {
        Snackbar.make(findViewById(R.id.myProfilePage), emessageSuccess, Snackbar.LENGTH_SHORT).show()
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
                .response { _, response, result ->
                    result.fold(
                        {
                            completion(response.statusCode == 200)
                        },
                        {
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
                                layoutPage()
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

    private fun enableElements() {
        elementsToDisable.forEach { it.isEnabled = true }
    }

    private fun disableElements() {
        elementsToDisable.forEach { it.isEnabled = false }
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
                        Snackbar.make(findViewById(R.id.myProfilePage), message, Snackbar.LENGTH_LONG).show()
                    } else {
                        val data = result.get()
                        Log.e("Fuel", "${data}")
                        // Перенаправляем пользователя на страницу входа только если токен недействителен
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }



}