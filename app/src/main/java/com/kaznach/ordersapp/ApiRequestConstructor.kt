import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.result.Result
import com.kaznach.ordersapp.ApiConstants
import com.kaznach.ordersapp.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ApiRequestConstructor constructor(
    private val context: Context,
    private val endpoint: String,
    private val method: Method,
    private val body: String? = null,
    private val onSuccess: (data: String) -> Unit,
    private val onFailure: (errorMessage: String, statusCode: Int) -> Unit
) {

    private val sharedPrefs = EncryptedSharedPreferences.create(
        "auth",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun execute() {
        val token = sharedPrefs.getString("token", null)

        if (token != null) {
            val request = Fuel.request(method, ApiConstants.URLS[endpoint].toString())
                .header("Authorization" to "Bearer $token")
                .timeoutRead(3000)

            if (method == Method.POST || method == Method.PUT) {
                request.header("Content-Type" to "application/json")
                body?.let { request.body(it) }
            }

            CoroutineScope(Dispatchers.IO).launch {
                val (_, response, result) = request.responseString()

                when (result) {
                    is Result.Failure -> {
                        val ex = result.getException()
                        val errorData = ex.response.data
                        val errorMessage = String(errorData, Charsets.UTF_8)
                        Log.e("Fuel", "Ошибка запроса: $errorMessage")
                        onFailure(errorMessage, response.statusCode)
                    }
                    is Result.Success -> {
                        if (response.statusCode == 200 || response.statusCode == 201) {
                            onSuccess(result.get())
                        } else {
                            val data = result.get()
                            val errorMessage = "Ошибка: $data. Код ответа: ${response.statusCode}"
                            Log.w("Fuel", errorMessage)
                            onFailure(errorMessage, response.statusCode)
                        }
                    }
                }
            }
        } else {
            val errorMessage = "Ошибка авторизации. Пожалуйста, войдите снова."
            onFailure(errorMessage, 401)
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }
}


//Пример использования:
//// Создание запроса GET
//val getRequest = ApiRequestConstructor(
//    context = this,
//    endpoint = ,
//    method = Method.GET,
//    onSuccess = { data ->
//        // Обработка успешного ответа
//        Log.d("API", "Успешный GET запрос: $data")
//    },
//    onFailure = { errorMessage, statusCode ->
//        // Обработка ошибки
//        Log.e("API", "Ошибка GET запроса: $errorMessage, код: $statusCode")
//    }
//)
//
//// Выполнение запроса
//getRequest.execute()
//
//// Создание запроса POST с телом запроса
//val body = """
//    {
//      "name": "Название заказа",
//      "mainCategory": "Главная категория",
//      "subCategory": "Подкатегория",
//      "date": "2023-12-31",
//      "budget": 1000,
//      "description": "Описание заказа"
//    }
//""".trimIndent()
//
//val postRequest = ApiRequestConstructor(
//    context = this,
//    endpoint = ,
//    method = Method.POST,
//    body = body,
//    onSuccess = { data ->
//        runOnUiThread {
//                    SnackbarHelper.showSnackbar(this, message, Snackbar.LENGTH_LONG, "SUCCESS")
//                }
//        // Обработка успешного ответа
//        Log.d("API", "Успешный POST запрос: $data")
//    },
//    onFailure = { errorMessage, statusCode ->
//        runOnUiThread {
////                    SnackbarHelper.showSnackbar(this, message, Snackbar.LENGTH_LONG, "ERROR")
////                }
//        // Обработка ошибки
//        Log.e("API", "Ошибка POST запроса: $errorMessage, код: $statusCode")
//    }
//)
//
//// Выполнение запроса
//postRequest.execute()