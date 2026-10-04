package uz.hangulfriend.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerNameTest {
    @Test fun trimsAndCollapsesSpaces() = assertEquals("Ikrom Aka", cleanName("  Ikrom \t  Aka \n"))

    @Test fun capsAtTwentyCharacters() {
        assertEquals(NAME_MAX, cleanName("a".repeat(40)).length)
        assertEquals("abcdefghijklmnopqrs", cleanName("abcdefghijklmnopqrs t"))
    }

    @Test fun blankIsEmpty() = assertEquals("", cleanName("   "))

    @Test fun fallbackOnlyWhenBlank() {
        assertEquals("Ovchi", displayName("", "Ovchi"))
        assertEquals("Ikrom", displayName("Ikrom", "Ovchi"))
    }

    @Test fun personalizeSwapsTheHeroName() {
        assertEquals("Ikrom 'Otam' demoqchi. Ikromning javobi?", personalize("Aziz 'Otam' demoqchi. Azizning javobi?", "Ikrom"))
        assertEquals("Ikromga ayting; Ikrom's turn", personalize("Azizga ayting; Aziz's turn", "Ikrom"))
        assertEquals("Aziz says hi", personalize("Aziz says hi", ""))
        assertEquals("Azizbek stays", personalize("Azizbek stays", "Ikrom"))
    }
}
