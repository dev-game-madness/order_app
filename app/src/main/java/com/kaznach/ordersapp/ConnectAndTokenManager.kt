package com.kaznach.ordersapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.github.kittinunf.fuel.Fuel

class ConnectAndTokenManager<T : Activity>(private val context: Context, private val activity: T) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun checkConnectionAndToken(onSuccess: (Boolean) -> Unit) {
        if (isNetworkAvailable()) {
            checkServerAvailability(
                onSuccess = {
                    activity.runOnUiThread {  // Обновляем UI в главном потоке
                        checkToken(onSuccess)
                    }
                },
                onFailure = {
                    activity.runOnUiThread {
                        showServerError()
                        onSuccess(false)
                    }
                }
            )
        } else {
            activity.runOnUiThread {
                showNoInternetError()
                onSuccess(false)
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun checkServerAvailability(onSuccess: () -> Unit, onFailure: () -> Unit) {
        Fuel.head(ApiConstants.URLS["connection/check"].toString())
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
            Fuel.get(ApiConstants.URLS["sessions/check"].toString())
                .header("Authorization" to "Bearer $token")
                .response { _, response, result ->
                    if (response.statusCode == 200) {
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

    private fun startLoginActivity() {
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent)
    }

    private fun showNoInternetError() {
        val noConnectionLayout = activity.findViewById<View>(R.id.noConnectionLayout)
        val imageView = noConnectionLayout.findViewById<ImageView>(R.id.imageViewNoConn)
        val textView = noConnectionLayout.findViewById<TextView>(R.id.textViewNoConn)

        noConnectionLayout.visibility = View.VISIBLE
        imageView.setImageResource(R.drawable.iconnointernet)
        textView.setText(R.string.no_internet_message)
    }

    private fun showServerError() {
        val noConnectionLayout = activity.findViewById<View>(R.id.noConnectionLayout)
        val imageView = noConnectionLayout.findViewById<ImageView>(R.id.imageViewNoConn)
        val textView = noConnectionLayout.findViewById<TextView>(R.id.textViewNoConn)

        noConnectionLayout.visibility = View.VISIBLE
        imageView.setImageResource(R.drawable.iconnoconserver)
        textView.setText(R.string.server_error_message)
    }
}