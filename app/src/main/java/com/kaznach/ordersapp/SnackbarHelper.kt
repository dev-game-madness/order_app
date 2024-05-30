
import android.app.Activity
import android.graphics.Color
import com.google.android.material.snackbar.Snackbar

class SnackbarHelper {

    companion object {

        fun showSnackbar(
            activity: Activity,
            message: String,
            duration: Int,
            type: String
        ) {
            val snackbar = Snackbar.make(
                activity.findViewById(android.R.id.content),
                message,
                duration
            )

            val color = when (type) {
                "SUCCESS" -> Color.parseColor("#4CAF50") // Зеленый
                "ERROR" -> Color.parseColor("#F44336") // Красный
                "WARNING" -> Color.parseColor("#FFEB3B") // Желтый
                "INFO" -> Color.parseColor("#2196F3") // Синий
                else -> throw IllegalArgumentException("Неверный тип Snackbar: $type")
            }
            snackbar.view.setBackgroundColor(color)

            snackbar.show()
        }
    }
}

// Пример
// val message = "Сообщение"
// SnackbarHelper.showSnackbar(this, message, Snackbar.LENGTH_LONG, "SUCCESS")
// SnackbarHelper.showSnackbar(this, message, Snackbar.LENGTH_LONG, "ERROR")
// SnackbarHelper.showSnackbar(this, message, Snackbar.LENGTH_LONG, "WARNING")
// SnackbarHelper.showSnackbar(this, message, Snackbar.LENGTH_LONG, "INFO")