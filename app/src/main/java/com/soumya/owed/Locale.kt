package com.soumya.owed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Currency
import java.util.Locale

object LocaleHub {
    var lang by mutableStateOf("en")
    var currency by mutableStateOf("INR")
}

data class LanguageOption(val code: String, val native: String)

val Languages = listOf(
    LanguageOption("en", "English"),
    LanguageOption("hi", "हिन्दी"),
    LanguageOption("hing", "Hinglish"),
    LanguageOption("es", "Español"),
    LanguageOption("fr", "Français"),
    LanguageOption("pt", "Português"),
    LanguageOption("de", "Deutsch"),
    LanguageOption("ar", "العربية"),
    LanguageOption("zh", "中文"),
    LanguageOption("bn", "বাংলা")
)

data class CurrencyOption(val code: String, val name: String)

val Currencies = listOf(
    CurrencyOption("INR", "Indian rupee"),
    CurrencyOption("USD", "US dollar"),
    CurrencyOption("EUR", "Euro"),
    CurrencyOption("GBP", "British pound"),
    CurrencyOption("JPY", "Japanese yen"),
    CurrencyOption("AUD", "Australian dollar"),
    CurrencyOption("CAD", "Canadian dollar"),
    CurrencyOption("CNY", "Chinese yuan"),
    CurrencyOption("AED", "UAE dirham"),
    CurrencyOption("SAR", "Saudi riyal"),
    CurrencyOption("SGD", "Singapore dollar"),
    CurrencyOption("BDT", "Bangladeshi taka"),
    CurrencyOption("NPR", "Nepalese rupee"),
    CurrencyOption("KRW", "South Korean won"),
    CurrencyOption("BRL", "Brazilian real"),
    CurrencyOption("CHF", "Swiss franc")
)

fun localeFor(code: String): Locale = when (code) {
    "hi" -> Locale("hi", "IN")
    "hing" -> Locale.ENGLISH
    "es" -> Locale("es")
    "fr" -> Locale("fr")
    "pt" -> Locale("pt")
    "de" -> Locale("de")
    "ar" -> Locale("ar")
    "zh" -> Locale.SIMPLIFIED_CHINESE
    "bn" -> Locale("bn")
    else -> Locale.ENGLISH
}

fun currencySymbol(code: String): String = try {
    Currency.getInstance(code).symbol
} catch (_: Exception) {
    code
}

fun t(en: String, vararg args: Any): String {
    val lang = LocaleHub.lang
    val pattern = if (lang == "en") en else Pack[lang]?.get(en) ?: More[lang]?.get(en) ?: SettingsLines[lang]?.get(en) ?: TextLines[lang]?.get(en) ?: en
    if (args.isEmpty()) return pattern
    return try {
        String.format(Locale.getDefault(), pattern, *args)
    } catch (_: Exception) {
        pattern
    }
}
