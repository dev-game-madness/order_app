package com.kaznach.ordersapp

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.View
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.github.kittinunf.fuel.Fuel
import com.google.android.material.snackbar.Snackbar

class ConnectAndTokenManager(private val context: Context, private val snackbarView: View) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun checkConnectionAndToken(onSuccess: (Boolean) -> Unit) {
        if (isNetworkAvailable()) {
            checkServerAvailability(
                onSuccess = { checkToken(onSuccess) },
                onFailure = {
                    showSnackbarError("Нет связи с сервером")
                    onSuccess(false)
                }
            )
        } else {
            showSnackbarError("Нет подключения к Интернету")
            onSuccess(false)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun checkServerAvailability(onSuccess: () -> Unit, onFailure: () -> Unit) {
        Fuel.head(ApiConstants.CHECK_CONNECTION_URL)
            .timeout(2000)
            .response { _, response, result ->
                result.fold(
                    {
                        if (response.statusCode == 200) {
                            onSuccess()
                        } else {
                            onFailure()
                        }
                    },
                    {
                        onFailure()
                    }
                )
            }
    }

    private fun checkToken(onSuccess: (Boolean) -> Unit) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPrefs = EncryptedSharedPreferences.create(
            "auth",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = sharedPrefs.getString("token", null)

        if (token != null) {
            Fuel.get(ApiConstants.CHECK_SESSION_URL)
                .header("Authorization" to "Bearer $token")
                .response { _, response, result ->
                    if (response.statusCode == 200) {
                        showSnackbarError("Сессия найдена")
                        onSuccess(true)
                    } else {
                        startLoginActivity()
                        val data = result.get()
                        Log.e("Fuel", "$data")
                        onSuccess(false)
                    }
                }
        } else {
            startLoginActivity()
            onSuccess(false)
        }
    }

    private fun showSnackbarError(message: String) {
        Snackbar.make(snackbarView, message, Snackbar.LENGTH_LONG).show()
    }

    private fun startLoginActivity() {
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent)
    }
}