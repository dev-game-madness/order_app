package com.kaznach.ordersapp

import android.text.InputFilter
import android.text.Spanned

class EditTextFilters : InputFilter {

    private val prefix = "+7"

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val phone = StringBuilder()

        // Всегда начинаем с префикса
        phone.append(prefix)

        var digitsCount = 0

        // Обрабатываем существующий текст, игнорируя префикс
        if (dest != null) {
            for (i in prefix.length until dest.length) {
                if (dest[i].isDigit()) {
                    if (digitsCount == 0 || digitsCount == 3 || digitsCount == 6 || digitsCount == 8) {
                        phone.append(" ")
                    }
                    phone.append(dest[i])
                    digitsCount++
                }
                if (digitsCount >= 9) {
                    break
                }
            }
        }

        // Добавляем новый текст, форматируя номер
        for (i in start until end) {
            if (source?.get(i)?.isDigit() == true) {
                if (digitsCount == 0 || digitsCount == 3 || digitsCount == 6 || digitsCount == 8) {
                    phone.append(" ")
                }
                phone.append(source[i])
                digitsCount++
            }
            if (digitsCount >= 9) {
                break
            }
        }

        // Возвращаем только измененный текст
        if (source.isNullOrEmpty()) {
            // Если удаляем символ, возвращаем пустую строку
            return ""
        } else {
            // Если вводим символ, возвращаем новый текст, начиная с позиции dstart
            return phone.toString().substring(dstart)
        }
    }
}