package com.kaznach.ordersapp

import ApiRequestConstructor
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Method

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

        val getRequest = ApiRequestConstructor(
            context = activity,
            endpoint = "sessions/check",
            method = Method.GET,
            onSuccess = { data ->
                activity.runOnUiThread {
                    onSuccess(true)
                }
                Log.d("API", "Успешный GET запрос: $data")
            },
            onFailure = { errorMessage, statusCode ->
                activity.runOnUiThread {
                    startLoginActivity()
                    onSuccess(false)
                }
                Log.e("API", "Ошибка GET запроса: $errorMessage, код: $statusCode")
            }
        )
        getRequest.execute()
    }

    private fun startLoginActivity() {
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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