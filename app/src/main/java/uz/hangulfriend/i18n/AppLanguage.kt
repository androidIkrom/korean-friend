package uz.hangulfriend.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** The learner's interface and explanation language (stage 8). Uzbek unless the learner picks English. */
enum class AppLanguage(val code: String) {
    UZ("uz"),
    EN("en"),
    ;

    val locale: Locale get() = Locale.forLanguageTag(code)

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: UZ
    }
}

/** The chosen language, read synchronously so it can be applied before the activity draws. */
class LanguageStore(context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun get(): AppLanguage = AppLanguage.fromCode(prefs.getString(KEY, null))

    /** Written with commit() because the activity restarts right after and must read the new value. */
    fun set(lang: AppLanguage) {
        prefs.edit().putString(KEY, lang.code).commit()
    }

    private companion object {
        const val FILE = "language"
        const val KEY = "language"
    }
}

object Localized {
    /** [context] with resources in [lang], whatever the phone's own language is. */
    fun wrap(context: Context, lang: AppLanguage): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(lang.locale)
        return context.createConfigurationContext(config)
    }
}
