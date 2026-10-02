package uz.hangulfriend.i18n

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.hangulfriend.R

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalizedTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Before fun clear() {
        ctx.getSharedPreferences("language", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun fromCode() {
        assertEquals(AppLanguage.EN, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.UZ, AppLanguage.fromCode("uz"))
        assertEquals(AppLanguage.UZ, AppLanguage.fromCode("xx"))
        assertEquals(AppLanguage.UZ, AppLanguage.fromCode(null))
    }

    @Test fun storeRoundTrip() {
        assertEquals(AppLanguage.UZ, LanguageStore(ctx).get())
        LanguageStore(ctx).set(AppLanguage.EN)
        assertEquals(AppLanguage.EN, LanguageStore(ctx).get())
    }

    @Test fun englishWrap() = assertEquals("en", Localized.wrap(ctx, AppLanguage.EN).getString(R.string.lang_code))

    @Test @Config(qualifiers = "en")
    fun uzbekEvenWhenDefaultLocaleIsEnglish() {
        assertEquals("en", ctx.getString(R.string.lang_code))
        assertEquals("uz", Localized.wrap(ctx, AppLanguage.UZ).getString(R.string.lang_code))
    }
}
