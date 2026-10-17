package com.broll.poc

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleStore {
    private const val PREFS = "jingku_prefs"
    private const val KEY_LANG = "app_lang"

    const val LANG_ZH = "zh"
    const val LANG_EN = "en"

    fun getLanguage(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, LANG_ZH) ?: LANG_ZH
    }

    fun setLanguage(context: Context, lang: String) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, lang)
            .apply()
    }

    fun wrap(context: Context): Context {
        val lang = getLanguage(context)
        val locale = when (lang) {
            LANG_EN -> Locale.ENGLISH
            else -> Locale.SIMPLIFIED_CHINESE
        }
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
