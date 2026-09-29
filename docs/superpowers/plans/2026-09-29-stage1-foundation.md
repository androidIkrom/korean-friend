# Hangul Friend — 1-bosqich (Asos) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Telefonda ishlaydigan birinchi versiya. Onboarding, kitob xaritasi (18 dars), 2-bo'limning ikkala darsi (lug'at, grammatika, dialog, matnli mashqlar, dars testi), FSRS takrorlash va asosiy sozlamalar bo'ladi. Audio, AI va o'yin elementlari keyingi bosqichlarda qo'shiladi.

**Architecture:**
- `:core:hangul` (javob tekshiruvchi) va `:core:srs` (FSRS) — sof Kotlin JVM modullari.
- `:app` quyidagi qatlamlardan iborat:
  - `content` — `assets`dagi JSON fayllarni o'qiydi;
  - `data` — Room va DataStore;
  - `study` — onboarding, sessiya tuzish, baholash;
  - `ui` — Compose.
- Qaramliklar qo'lda `AppContainer` orqali ulanadi.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Navigation Compose, Room (KSP), DataStore Preferences, kotlinx.serialization, `com.mikepenz:multiplatform-markdown-renderer-m3`, JUnit4, Robolectric, Python 3 + `fsrs` (faqat test vektorlari uchun).

**Spec:** `docs/superpowers/specs/2026-09-29-hangul-friend-design.md`

## Global Constraints

- **Paket va SDK:** paket `uz.hangulfriend`; `minSdk` 26; `compileSdk` 36; `targetSdk` 36. JDK 21. Android SDK yo'li: `C:\Users\user\AppData\Local\Android\Sdk`.
- **Kutubxona versiyalari:** barcha versiyalar `gradle/libs.versions.toml`da turadi. Har biri loyiha yaratilgan kundagi eng so'nggi barqaror va bir-biriga mos versiya bo'ladi. Agar KSP Kotlin'ning eng so'nggi versiyasini hali qo'llamasa, KSP qo'llaydigan Kotlin versiyasi olinadi.
- **Til:** barcha UI matnlari o'zbek tilida (lotin yozuvida), `res/values/strings.xml`da saqlanadi. Koreyscha matnlar faqat kontent JSON fayllaridan keladi.
- **Kitob kontenti:** kitob matni, dialoglari va qahramonlari ko'chirilmaydi. Kontent yangidan yoziladi.
- **ID'lar:** kontent ID'lari barqaror va global miqyosda yagona bo'ladi. Format: `uUU_lL`, `uUU_lL_wNNN`, `uUU_lL_gN`, `uUU_lL_eNNN`.
- **Karta ID'lari:** so'z tanish kartasi `"<wordId>#R"`, so'z yozish kartasi `"<wordId>#P"`, grammatika kartasi `"<grammarId>#G"`.
- **FSRS sozlamalari:** desired retention 0.9; learning steps [1 daqiqa, 10 daqiqa]; relearning steps [10 daqiqa]; maksimal interval 36500 kun; fuzz o'chirilgan.
- **Avtomatik baho:** noto'g'ri javob → `AGAIN`; to'g'ri, lekin hint ishlatilgan yoki javob 15 000 ms dan uzoq davom etgan → `HARD`; qolgan to'g'ri javoblar → `GOOD`. `flashcard` mashqida baholash qo'lda, 4 ta tugma orqali.
- **Test chegarasi va limit:** dars testi 80% va undan yuqori bo'lsa, dars `COMPLETED` bo'ladi. Kunlik yangi karta limiti standart holatda 20 ta.
- **Git:** commit xabarlariga `Co-Authored-By` qatori qo'shilmaydi. Worktree ishlatilmaydi.

## Review Focus

1. **Gboard'dan keladigan Hangul.** Gboard ba'zan NFD ko'rinishidagi (ajratilgan) Hangul yuboradi. Bunday javob ham to'g'ri deb qabul qilinishi kerak. Test: Task 2 `normalize_nfdInput`.
2. **Kontenti yozilmagan oldingi darslar.** Onboarding'da joriy dars tanlanganda oldingi darslarning kontenti hali yo'q bo'lishi mumkin (1-bo'lim). Ilova qulamasligi kerak: bu darslar `PASSED` holatini oladi, lekin karta yaratilmaydi. Test: Task 8 `onboarding_skipsLessonsWithoutContent`.
3. **Joriy darsni qayta tanlash.** Sozlamalardan joriy darsni orqaga qaytarib qayta tanlash kartalarni ko'paytirmasligi va mavjud FSRS holatini buzmasligi kerak. Test: Task 7 `ensureCards_isIdempotentAndKeepsState`.
4. **Kunlik yangi karta limiti.** Limit mahalliy vaqt zonasidagi sana bo'yicha qayta hisoblanadi: 23:59 dagi va 00:01 dagi kirish har xil kunga tegishli. Test: Task 7 `dueQueue_newLimitResetsAtLocalMidnight`.
5. **Faqat bitta mashq turi.** Sessiyada faqat bitta turdagi mashqlar bo'lsa, "bir xil tur ketma-ket kelmaydi" qoidasi cheksiz siklga tushmasligi kerak. Test: Task 8 `arrange_singleTypeTerminates`.

---

### Task 1: Loyiha skeleti

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `.gitignore`, `local.properties` (commit qilinmaydi)
- Create: `core/hangul/build.gradle.kts`, `core/srs/build.gradle.kts`, `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`, `app/src/main/java/uz/hangulfriend/MainActivity.kt`, `app/src/main/java/uz/hangulfriend/HangulFriendApp.kt`, `app/src/main/res/values/strings.xml`
- Test: `core/hangul/src/test/kotlin/uz/hangulfriend/hangul/SmokeTest.kt`

**Interfaces:**
- Produces: `:core:hangul` va `:core:srs` modullari (`kotlin("jvm")`, JUnit4). `:app` ikkala modulga bog'langan. `HangulFriendApp : Application`, `MainActivity : ComponentActivity`.

- [ ] **Step 1: Gradle wrapper'ni yaratish.** Kompyuterda `gradle` o'rnatilmagan. Gradle'ning eng so'nggi barqaror binary zip'ini scratchpad papkaga yuklab oching, so'ng repo ildizida `<gradle>/bin/gradle wrapper` buyrug'ini ishga tushiring.
- [ ] **Step 2: Build fayllarini yozish.** `local.properties` fayliga `sdk.dir=C\:\\Users\\user\\AppData\\Local\\Android\\Sdk` yoziladi. `.gitignore` ga `local.properties`, `build/`, `.gradle/`, `.idea/`, `*.iml` qo'shiladi. `:app` uchun Compose, `applicationId "uz.hangulfriend"` va `android:allowBackup="true"` sozlanadi. Room sxemasi `app/schemas` papkasiga eksport qilinadi.
- [ ] **Step 3: `SmokeTest` yozish.** Test `assertEquals(2, 1 + 1)` ni tekshiradi. Keyin `./gradlew :core:hangul:test :core:srs:test :app:assembleDebug` buyrug'ini ishga tushiring.
  Kutilgan natija: `BUILD SUCCESSFUL`.
- [ ] **Step 4: Emulator yoki telefonda tekshirish.** `MainActivity` ekranida `Text(stringResource(R.string.app_name))` ("Hangul Friend") ko'rinadi. Buyruq: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
- [ ] **Step 5: Commit**
```bash
git add -A && git commit -m "build: scaffold Android project with core modules"
```

### Task 2: `:core:hangul` — jamo'larga ajratish va normallashtirish

**Files:**
- Create: `core/hangul/src/main/kotlin/uz/hangulfriend/hangul/Hangul.kt`
- Test: `core/hangul/src/test/kotlin/uz/hangulfriend/hangul/HangulTest.kt`

**Interfaces:**
- Produces:
  - `data class SyllableParts(val initial: Char, val medial: Char, val final: Char?)` — Hangul Compatibility Jamo belgilari (ㄱ, ㅏ, ㅆ, …).
  - `object Hangul`:
    - `fun isSyllable(c: Char): Boolean` — U+AC00..U+D7A3 oralig'ida bo'lsa `true`.
    - `fun parts(c: Char): SyllableParts?`
    - `fun jamo(s: String): List<Char>` — bo'g'inlar jamo'larga ajratiladi, boshqa belgilar o'zgarishsiz qoladi.
    - `fun normalize(s: String): String` — NFC; chetdagi bo'sh joylarni olib tashlaydi; ichki bo'sh joylarni bittaga qisqartiradi; oxiridagi `.?!` belgilarini olib tashlaydi; lotin harflarini kichik harfga o'tkazadi.

- [ ] **Step 1: Failing testlarni yozish**
```kotlin
@Test fun parts_withFinal() = assertEquals(SyllableParts('ㄱ','ㅏ','ㅆ'), Hangul.parts('갔'))
@Test fun parts_noFinal() = assertEquals(SyllableParts('ㅁ','ㅕ',null), Hangul.parts('며'))
@Test fun parts_nonHangul() = assertNull(Hangul.parts('a'))
@Test fun jamo_mixed() = assertEquals(listOf('ㄱ','ㅏ',' ','ㅁ','ㅕ','ㄴ','?'), Hangul.jamo("가 면?"))
@Test fun normalize_spacesAndPunctuation() = assertEquals("입어 보세요", Hangul.normalize("  입어   보세요. "))
@Test fun normalize_nfdInput() = assertEquals("가면", Hangul.normalize(java.text.Normalizer.normalize("가면", java.text.Normalizer.Form.NFD)))
```
- [ ] **Step 2: Testlarni ishga tushirish.** Buyruq: `./gradlew :core:hangul:test`. Kutilgan natija: FAIL (unresolved reference `Hangul`).
- [ ] **Step 3: `Hangul.kt`ni yozish.** Unicode formulasidan foydalaniladi: `code = c - 0xAC00`, `initial = code / 588`, `medial = (code % 588) / 28`, `final = code % 28`. Natija compatibility jamo jadvallari (19 ta boshlang'ich, 21 ta unli, 27 ta oxirgi undosh) orqali belgiga aylantiriladi.
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Buyruq: `./gradlew :core:hangul:test`. Kutilgan natija: PASS.
- [ ] **Step 5: Commit**
```bash
git add core/hangul && git commit -m "feat(hangul): jamo decomposition and answer normalization"
```

### Task 3: `:core:hangul` — `AnswerChecker`

**Files:**
- Create: `core/hangul/src/main/kotlin/uz/hangulfriend/hangul/AnswerChecker.kt`
- Test: `core/hangul/src/test/kotlin/uz/hangulfriend/hangul/AnswerCheckerTest.kt`

**Interfaces:**
- Consumes: `Hangul.normalize`, `Hangul.parts`, `Hangul.jamo`.
- Produces:
  - `enum class Feedback { CORRECT, SPACING, FINAL_CONSONANT, VOWEL, ONE_LETTER, WRONG }`
  - `data class CheckResult(val feedback: Feedback, val closest: String) { val correct get() = feedback == Feedback.CORRECT }`
  - `object AnswerChecker { fun check(input: String, answers: List<String>): CheckResult }`

**Algoritm:**
1. Foydalanuvchi javobi va barcha to'g'ri javoblar normallashtiriladi. Javob ro'yxatdagi biror to'g'ri javobga teng bo'lsa, natija `CORRECT`.
2. Aks holda `closest` tanlanadi: bu `Hangul.jamo` ro'yxatlari bo'yicha Levenshtein masofasi eng kichik bo'lgan to'g'ri javob.
3. Xato turi quyidagi tartibda aniqlanadi (birinchi mos kelgani olinadi):
   - javob va `closest` bo'sh joylarsiz bir xil bo'lsa → `SPACING`;
   - bo'g'inlar soni teng va farq faqat `final` qismida bo'lsa → `FINAL_CONSONANT`;
   - bo'g'inlar soni teng va farq faqat `medial` qismida bo'lsa → `VOWEL`;
   - jamo masofasi 1 ga teng bo'lsa → `ONE_LETTER`;
   - qolgan hollarda → `WRONG`.

- [ ] **Step 1: Failing testlarni yozish**
```kotlin
private fun fb(input: String, vararg answers: String) = AnswerChecker.check(input, answers.toList()).feedback
@Test fun correct_exact() = assertEquals(Feedback.CORRECT, fb("가면", "가면"))
@Test fun correct_anyOfAnswers() = assertEquals(Feedback.CORRECT, fb("입어 봐요", "입어 보세요", "입어 봐요"))
@Test fun correct_ignoresTrailingPunctuation() = assertEquals(Feedback.CORRECT, fb("가면.", "가면"))
@Test fun spacing() = assertEquals(Feedback.SPACING, fb("입어보세요", "입어 보세요"))
@Test fun finalConsonant() = assertEquals(Feedback.FINAL_CONSONANT, fb("갔면", "가면"))
@Test fun vowel() = assertEquals(Feedback.VOWEL, fb("거면", "가면"))
@Test fun oneLetter() = assertEquals(Feedback.ONE_LETTER, fb("가면ㅇ", "가면"))
@Test fun wrong() = assertEquals(Feedback.WRONG, fb("학교", "가면"))
@Test fun closest_picksNearest() = assertEquals("입어 봐요", AnswerChecker.check("입어 박요", listOf("입어 보세요", "입어 봐요")).closest)
```
- [ ] **Step 2: Testlarni ishga tushirish.** Buyruq: `./gradlew :core:hangul:test`. Kutilgan natija: FAIL.
- [ ] **Step 3: `AnswerChecker`ni yozish.** Yuqoridagi algoritm bo'yicha; Levenshtein `private` funksiya sifatida yoziladi.
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit**
```bash
git add core/hangul && git commit -m "feat(hangul): answer checker with jamo-level feedback"
```

### Task 4: `:core:srs` — FSRS va avtomatik baho

**Files:**
- Create: `tools/gen_fsrs_vectors.py`, `core/srs/src/test/resources/fsrs_vectors.json` (skript yaratadi)
- Create: `core/srs/src/main/kotlin/uz/hangulfriend/srs/Fsrs.kt`, `core/srs/src/main/kotlin/uz/hangulfriend/srs/AutoRating.kt`
- Test: `core/srs/src/test/kotlin/uz/hangulfriend/srs/FsrsTest.kt`, `AutoRatingTest.kt`

**Interfaces:**
- Produces:
  - `enum class Rating(val value: Int) { AGAIN(1), HARD(2), GOOD(3), EASY(4) }`
  - `enum class CardState(val value: Int) { LEARNING(1), REVIEW(2), RELEARNING(3) }`
  - `data class SrsCard(val state: CardState, val step: Int?, val stability: Double?, val difficulty: Double?, val due: java.time.Instant, val lastReview: java.time.Instant?)`
  - `class FsrsScheduler`:
    - konstruktor: `(parameters: DoubleArray = DEFAULT_PARAMETERS, desiredRetention: Double = 0.9, learningSteps: List<Duration> = listOf(1.minutes, 10.minutes), relearningSteps: List<Duration> = listOf(10.minutes), maximumInterval: Int = 36500)`;
    - `fun newCard(now: Instant): SrsCard` — natija: `LEARNING`, `step = 0`, `due = now`;
    - `fun review(card: SrsCard, rating: Rating, now: Instant): SrsCard`;
    - `fun retrievability(card: SrsCard, now: Instant): Double`.
  - `object AutoRating { fun from(correct: Boolean, usedHint: Boolean, elapsedMs: Long): Rating }`

- [ ] **Step 1: Test vektorlarini yaratish.** `pip install fsrs` bilan eng so'nggi `fsrs` 6.x versiyasini o'rnating. `tools/gen_fsrs_vectors.py` yozing. U `Scheduler(enable_fuzzing=False)` ni standart qiymatlar bilan ishlatadi va 6 ta reyting ketma-ketligini `2026-01-01T00:00:00Z` dan boshlab qat'iy vaqtlarda o'tkazadi. Ketma-ketliklar:
  - `[3,3,3,3,3]`;
  - `[1,1,3,3,4]`;
  - `[4,4,4]`;
  - `[3,3,1,3,3,3]`;
  - `[2,2,3,1,3]`;
  - `[3,3,3,1,1,3,4]`.

  Har bir review oldingi kartaning `due` vaqtida o'tkaziladi. Har bir qadamdan keyin `state`, `step`, `stability`, `difficulty` va `due` (epoch soniyalarda) JSON'ga yoziladi. Faylning boshiga ishlatilgan `fsrs` versiyasi yoziladi.
- [ ] **Step 2: Failing testlarni yozish**
```kotlin
@Test fun matchesPyFsrsVectors()  // har bir qadam: state, step aynan teng; stability/difficulty ±1e-6; due soniyagacha teng
@Test fun newCard_isLearningDueNow() { val c = FsrsScheduler().newCard(T0); assertEquals(CardState.LEARNING, c.state); assertEquals(0, c.step); assertEquals(T0, c.due) }
@Test fun autoRating_wrong() = assertEquals(Rating.AGAIN, AutoRating.from(false, false, 1000))
@Test fun autoRating_hint() = assertEquals(Rating.HARD, AutoRating.from(true, true, 1000))
@Test fun autoRating_slow() = assertEquals(Rating.HARD, AutoRating.from(true, false, 15_001))
@Test fun autoRating_boundary() = assertEquals(Rating.GOOD, AutoRating.from(true, false, 15_000))
```
- [ ] **Step 3: Testlarni ishga tushirish.** Buyruq: `./gradlew :core:srs:test`. Kutilgan natija: FAIL.
- [ ] **Step 4: `Fsrs.kt`ni yozish.** py-fsrs'ning `fsrs/scheduler.py` fayli (Step 1'dagi versiya) Kotlin'ga port qilinadi. `DEFAULT_PARAMETERS` va barcha formulalar undan olinadi, fuzz qismi olib tashlanadi. Keyin `AutoRating.kt` ni Global Constraints'dagi qoidaga ko'ra yozing.
- [ ] **Step 5: Testlarni qayta ishga tushirish.** Kutilgan natija: PASS.
- [ ] **Step 6: Commit**
```bash
git add tools/gen_fsrs_vectors.py core/srs && git commit -m "feat(srs): FSRS scheduler ported from py-fsrs with vector tests"
```

### Task 5: Kontent modeli, o'qish va validator

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/content/Model.kt`, `AssetSource.kt`, `ContentRepository.kt`, `LessonValidator.kt`
- Test: `app/src/test/java/uz/hangulfriend/content/ContentRepositoryTest.kt`, `LessonValidatorTest.kt`, `app/src/test/java/uz/hangulfriend/Fixtures.kt` (umumiy `validLesson()` fixture'i, keyingi tasklarda ham ishlatiladi)

**Interfaces:**
- Produces: `@Serializable` modellar. Maydon nomlari spec 4.4 dagi JSON nomlari bilan bir xil, `@SerialName` orqali snake_case'dan camelCase'ga o'giriladi.
  - `Lesson(id, unit, lesson, titleKo, titleUz, topicUz, reviewed, words: List<Word>, grammar: List<Grammar>, dialogue: Dialogue, exercises: List<Exercise>, test: List<String>, story: Story? = null)`
  - `Word(id, ko, uz, pos, exampleKo, exampleUz, audio: String? = null, exampleAudio: String? = null)`
  - `Grammar(id, pattern, meaningUz, explanationMd, formation: List<Formation>, examples: List<Example>, mistakes: List<Mistake>, uzCompare)`
  - `Formation(conditionUz, rule, example)`
  - `Example(ko, uz, audio: String? = null)`
  - `Mistake(wrong, right, whyUz)`
  - `Dialogue(lines: List<Line>)`, `Line(speaker, ko, uz, audio: String? = null)`
  - `Story(titleUz, lines: List<Line>)`
  - `enum class ExerciseType { SITUATION_CHOICE, CONJUGATE, FILL_BLANK, BUILD_SENTENCE, FIND_ERROR, TRANSLATE }` — JSON'da snake_case nomlar bilan (`"fill_blank"`).
  - `Exercise(id, type, targets: List<String>, promptUz, answers: List<String>, sentence: String? = null, base: String? = null, form: String? = null, options: List<String>? = null, tokens: List<String>? = null, sourceUz: String? = null, hintUz: String? = null, whyUz: String? = null)`
  - `CatalogEntry(id, unit, lesson, titleKo, titleUz, topicUz)` — `assets/book.json` faylida `{ "lessons": [...] }` ko'rinishida.
  - `Character(id, nameUz, nameKo, voice)` — `assets/characters.json` faylida.
  - `interface AssetSource { fun read(path: String): String?; fun list(dir: String): List<String> }`, ikkita implementatsiya bilan: `AndroidAssetSource(assets: AssetManager)` va `DirAssetSource(root: java.io.File)` (testlar uchun).
  - `class ContentRepository(source: AssetSource, strict: Boolean)`:
    - `fun catalog(): List<CatalogEntry>`;
    - `fun isAvailable(id: String): Boolean` — `lessons/<id>.json` fayli mavjud bo'lsa `true`;
    - `fun lesson(id: String): Lesson?` — fayl bo'lmasa `null`; JSON buzilgan bo'lsa `strict` holatda `ContentException`, aks holda `null`;
    - `fun characters(): List<Character>`.
  - `object LessonValidator { fun validate(lessons: List<Lesson>): List<String> }` — bo'sh ro'yxat qaytsa, kontent to'g'ri.
- `Json { ignoreUnknownKeys = false }`. Maydon nomidagi xato jimgina o'tib ketmasligi kerak.

**Validator qoidalari** (har bir buzilish uchun xato matni `"<id>: <sabab>"` ko'rinishida):
- Barcha darslar bo'ylab so'z, grammatika va mashq ID'lari yagona bo'ladi.
- Har bir `targets` elementi shu darsdagi so'z yoki grammatika ID'siga ishora qiladi.
- Har bir `test` elementi shu darsdagi mashq ID'siga ishora qiladi.
- `answers` bo'sh bo'lmaydi.
- `CONJUGATE` mashqida `base` va `form` bo'lishi shart.
- `FILL_BLANK` mashqida `sentence` bo'ladi va unda `___` bor.
- `SITUATION_CHOICE` mashqida `options` kamida 2 ta, va barcha `answers` `options` ichida bo'ladi.
- `BUILD_SENTENCE` mashqida `tokens` to'plami `answers[0]`ni bo'sh joy bo'yicha bo'lingandagi bo'laklar to'plamiga teng (tartib va takror e'tiborga olinadi, multiset).
- `FIND_ERROR` mashqida `sentence` bo'ladi.
- `TRANSLATE` mashqida `sourceUz` bo'ladi.
- Har bir grammatikani test ro'yxatida bo'lmagan kamida bitta mashq tekshirishi (`targets`da ko'rsatishi) kerak.

- [ ] **Step 1: Failing testlarni yozish.** `validLesson()` test fixture'ini Kotlin'da quring: 2 so'z, 1 grammatika, har bir mashq turidan bittadan. Testlar:
  - `validate_validLesson_noErrors`;
  - `validate_duplicateIds`;
  - `validate_danglingTarget`;
  - `validate_danglingTestRef`;
  - `validate_fillBlankWithoutBlank`;
  - `validate_choiceAnswerNotInOptions`;
  - `validate_buildSentenceTokenMismatch`;
  - `validate_grammarWithoutPracticeExercise`.

  Har biri xato matnida kutilgan `id`ni tekshiradi. `ContentRepositoryTest` testlari `@TempDir` / `TemporaryFolder` + `DirAssetSource` bilan ishlaydi:
  - `lesson_missingFile_returnsNull`;
  - `lesson_malformed_strictThrows`;
  - `lesson_malformed_lenientNull`;
  - `lesson_unknownKey_strictThrows`;
  - `catalog_keepsOrder`.
- [ ] **Step 2: Testlarni ishga tushirish.** Buyruq: `./gradlew :app:testDebugUnitTest --tests "uz.hangulfriend.content.*"`. Kutilgan natija: FAIL.
- [ ] **Step 3: Model, `AssetSource`, `ContentRepository` va `LessonValidator`ni yozish.**
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/uz/hangulfriend/content app/src/test && git commit -m "feat(content): lesson model, asset loading and validator"
```

### Task 6: 2-bo'lim kontenti, katalog va qahramonlar

**Files:**
- Create: `app/src/main/assets/book.json`, `app/src/main/assets/characters.json`, `app/src/main/assets/lessons/u02_l1.json`, `app/src/main/assets/lessons/u02_l2.json`
- Create: `tools/validate_content.py`
- Test: `app/src/test/java/uz/hangulfriend/content/ContentAssetsTest.kt`

**Interfaces:**
- Consumes: Task 5 modellari, `ContentRepository`, `LessonValidator`.

- [ ] **Step 1: Grammatika ro'yxatini tasdiqlatish.** Foydalanuvchiga taklif qilinadigan ro'yxat:
  - `u02_l1` (한번 입어 보세요): `-아/어 보다` ("sinab ko'rmoq"); kiyim fe'llari `입다 / 신다 / 쓰다 / 끼다 / 하다` farqi (lug'at va grammatika sifatida).
  - `u02_l2` (더 긴 치마는 없어요?): `-(으)ㄴ` (sifatning aniqlovchi shakli); `N보다 (더)` (qiyoslash).

  Foydalanuvchidan o'z kitobidagi dars sarlavhalari bilan solishtirishni so'rang. Ro'yxat faqat u tasdiqlagan yoki tuzatgan ko'rinishda qabul qilinadi. Tasdiq olinmaguncha keyingi qadamga o'tilmaydi.
- [ ] **Step 2: Katalog va qahramonlarni yozish.**
  - `book.json`: spec 2-bo'limdagi jadvaldan 18 ta yozuv. Koreyscha nomi ma'lum bo'lmagan darslar uchun `titleKo` qisqa koreyscha tavsif bo'ladi, `titleUz` jadvaldagi o'zbekcha nom.
  - `characters.json`:
    - `aziz` — Aziz, 아지즈, Seulga o'qishga kelgan o'zbek talaba;
    - `minji` — Minji, 민지, koreys kursdoshi;
    - `seller` — Sotuvchi, 점원;
    - `teacher` — O'qituvchi, 선생님.

    `voice` maydoni hozircha `"female"` yoki `"male"`.
- [ ] **Step 3: `ContentAssetsTest` yozish.** Test `DirAssetSource(File("src/main/assets"))` orqali `catalog()` va mavjud barcha darslarni `strict = true` bilan yuklaydi va quyidagilarni tekshiradi:
  - `LessonValidator.validate(...)` bo'sh ro'yxat qaytaradi;
  - katalogda 18 ta dars bor;
  - har bir dialog qatori `speaker`i `characters.json`da mavjud.

  Ishga tushiring. Kutilgan natija: FAIL, chunki dars fayllari hali yo'q.
- [ ] **Step 4: `u02_l1.json` va `u02_l2.json`ni yozish.** Har bir darsda:
  - 25–35 so'z (`example_ko` va `example_uz` bilan);
  - tasdiqlangan grammatikalar, har biri to'liq: `explanation_md` o'zbekcha, 150–400 so'z; 4–6 ta `examples`; 2–4 ta `mistakes`; `uz_compare`;
  - 8–14 qatorli dialog, faqat o'z qahramonlarimiz bilan;
  - kamida 20 ta amaliy mashq: har bir grammatika uchun kamida 4 ta; 6 turning har biri kamida 2 martadan;
  - `test` ro'yxatida 15 ta alohida mashq (bu mashqlar amaliy mashqlar orasida ishlatilmaydi);
  - `reviewed: false`.
- [ ] **Step 5: `tools/validate_content.py` yozish.** Skript `gradlew(.bat) :app:testDebugUnitTest --tests "*ContentAssetsTest"` buyrug'ini chaqiradi va chiqish kodini qaytaradi. Validatsiya mantig'i bitta joyda, Kotlin'da qoladi. Ishga tushiring: `python tools/validate_content.py`. Kutilgan natija: PASS.
- [ ] **Step 6: Commit**
```bash
git add app/src/main/assets tools/validate_content.py app/src/test && git commit -m "content: unit 2 lessons, book catalog and characters"
```

### Task 7: Ma'lumotlar qatlami — Room, DataStore, repozitoriylar

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/data/AppDatabase.kt`, `Entities.kt`, `Daos.kt`, `StudyRepository.kt`, `ProgressRepository.kt`, `SettingsRepository.kt`
- Test: `app/src/test/java/uz/hangulfriend/data/StudyRepositoryTest.kt`, `ProgressRepositoryTest.kt`. Testlar Robolectric'da (`@Config(sdk = [35])`), in-memory Room bilan ishlaydi.

**Interfaces:**
- Consumes: `FsrsScheduler`, `SrsCard`, `Rating`, `Lesson`.
- Produces:
  - `enum class CardKind(val suffix: String) { RECOGNIZE("R"), PRODUCE("P"), GRAMMAR("G") }`
  - `object CardIds { fun of(itemId: String, kind: CardKind): String }` — natija `"$itemId#${kind.suffix}"`.
  - `enum class CardOrigin { LESSON, BACKLOG }`
  - `@Entity CardEntity(@PrimaryKey id, itemId, kind, lessonId, origin, state, step: Int?, stability: Double?, difficulty: Double?, dueMs: Long, lastReviewMs: Long?, firstReviewedMs: Long?, reps: Int, lapses: Int, lessonOrder: Int)`
  - `@Entity ReviewLogEntity(@PrimaryKey(autoGenerate) id, cardId, rating: Int, reviewedMs: Long, elapsedMs: Long)`
  - `enum class LessonStatus { NOT_STARTED, PASSED, IN_PROGRESS, COMPLETED, VERIFIED }`
  - `@Entity LessonProgressEntity(@PrimaryKey lessonId, status, stage: Int, bestTestScore: Int?)`
  - `class StudyRepository(db: AppDatabase, scheduler: FsrsScheduler, clock: java.time.Clock)`:
    - `suspend fun ensureCards(lesson: Lesson, lessonOrder: Int, origin: CardOrigin)` — har bir so'z uchun R va P kartalari, har bir grammatika uchun G kartasi yaratiladi. Karta allaqachon mavjud bo'lsa, FSRS holati o'zgarmaydi. Mavjud `BACKLOG` karta hali ko'rib chiqilmagan bo'lsa va yangi `origin = LESSON` kelsa, karta `LESSON`ga ko'tariladi.
    - `suspend fun grade(cardId: String, rating: Rating, elapsedMs: Long)` — FSRS `review` chaqiriladi, log yoziladi, birinchi marta baholanganda `firstReviewedMs` o'rnatiladi, `AGAIN` bahosida `lapses` oshiriladi (faqat `REVIEW` holatidan `AGAIN` bo'lganda).
    - `suspend fun dueQueue(dailyNewLimit: Int): List<CardEntity>` — avval faol kartalar (`origin = LESSON` yoki `firstReviewedMs != null`) orasidan `dueMs <= now` bo'lganlari, `dueMs` bo'yicha tartiblangan. Keyin hali ko'rilmagan `BACKLOG` kartalar, `lessonOrder` va keyin `id` bo'yicha. Ularning soni `dailyNewLimit - (bugun birinchi marta ko'rilgan BACKLOG kartalar soni)` dan oshmaydi. "Bugun" `clock.zone` bo'yicha mahalliy sana.
    - `fun observeDueCount(dailyNewLimit: Int): Flow<Int>`
  - `class ProgressRepository(db: AppDatabase)`:
    - `fun observeAll(): Flow<Map<String, LessonProgressEntity>>`;
    - `suspend fun setStage(lessonId: String, stage: Int)` — holat `NOT_STARTED` yoki `PASSED` bo'lsa, `IN_PROGRESS`ga o'tadi;
    - `suspend fun recordTest(lessonId: String, scorePercent: Int)` — `bestTestScore` yangilanadi; natija 80 va undan yuqori bo'lsa va holat `VERIFIED` bo'lmasa, `COMPLETED`ga o'tadi;
    - `suspend fun markPassed(lessonIds: List<String>)` — faqat `NOT_STARTED` holatdagilarga ta'sir qiladi.
  - `data class Settings(val onboarded: Boolean = false, val currentLessonId: String? = null, val dailyNewLimit: Int = 20)`
  - `class SettingsRepository(dataStore: DataStore<Preferences>)`:
    - `val settings: Flow<Settings>`;
    - `suspend fun setCurrentLesson(id: String)`;
    - `suspend fun setDailyNewLimit(n: Int)`;
    - `suspend fun setOnboarded()`.
- Room `version = 1`, `exportSchema = true`, `fallbackToDestructiveMigration` ishlatilmaydi.

- [ ] **Step 1: Failing testlarni yozish.** Testlarda qo'lda boshqariladigan `MutableClock` ishlatiladi (test helper: `Clock`dan meros oladi, `zone = Asia/Tashkent`).
  - `ensureCards_createsRPGCards` — 2 so'z va 1 grammatika uchun 5 ta karta.
  - `ensureCards_isIdempotentAndKeepsState` — `grade(GOOD)` dan keyin `ensureCards` qayta chaqiriladi; kartalar soni o'zgarmaydi, `stability` saqlanadi.
  - `ensureCards_upgradesUnseenBacklogToLesson`.
  - `grade_writesLogAndSetsFirstReviewed`.
  - `dueQueue_backlogLimitedByDailyNew` — 30 ta backlog karta va limit 20 bo'lsa, 20 ta qaytadi.
  - `dueQueue_newLimitResetsAtLocalMidnight` — soat 23:59 da 20 ta karta ko'rilgan bo'lsa, o'sha kuni navbatda backlog yo'q; 00:01 da yana 20 ta.
  - `dueQueue_orderDueFirstThenBacklog`.
  - `recordTest_80CompletesBelowDoesNot`.
  - `recordTest_neverDowngradesVerified`.
  - `markPassed_onlyNotStarted`.
- [ ] **Step 2: Testlarni ishga tushirish.** Buyruq: `./gradlew :app:testDebugUnitTest --tests "uz.hangulfriend.data.*"`. Kutilgan natija: FAIL.
- [ ] **Step 3: Entity, DAO va repozitoriylarni yozish.** `SrsCard` ↔ `CardEntity` o'girish `StudyRepository` ichida `private` bo'ladi.
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Kutilgan natija: PASS. `app/schemas/.../1.json` fayli paydo bo'lganini tekshiring.
- [ ] **Step 5: Commit**
```bash
git add app && git commit -m "feat(data): Room cards/progress, FSRS grading, daily queue, settings"
```

### Task 8: `study` — onboarding va sessiya tuzuvchi

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/study/ExerciseItem.kt`, `SessionBuilder.kt`, `OnboardingService.kt`, `Grader.kt`
- Test: `app/src/test/java/uz/hangulfriend/study/SessionBuilderTest.kt`, `OnboardingServiceTest.kt`, `GraderTest.kt`

**Interfaces:**
- Consumes: `ContentRepository`, `StudyRepository`, `ProgressRepository`, `SettingsRepository`, `CardIds`, `CardEntity`, `AnswerChecker`, `AutoRating`.
- Produces:
  - `sealed interface ExerciseItem { val typeKey: String; val cardIds: List<String> }`, variantlari:
    - `Flashcard(word: Word)` — `typeKey "flashcard"`, karta `#R`;
    - `WordTyping(word: Word)` — `"reverse_typing"`, karta `#P`;
    - `Match(words: List<Word>)` — `"match"`, har bir so'zning `#R` kartasi;
    - `Authored(exercise: Exercise)` — `typeKey` = `exercise.type` nomining kichik harfli ko'rinishi. Kartalari `targets` bo'yicha: grammatika ID'si → `#G`, so'z ID'si → `#R`.
  - `fun arrange(items: List<ExerciseItem>, random: Random): List<ExerciseItem>` — elementlar aralashtiriladi va qo'shni elementlarning `typeKey`i imkon qadar har xil bo'ladi. Ochko'z (greedy) algoritm: har qadamda oxirgi elementdan farqli turlardan eng ko'p qolgan turdagi element olinadi. Bunday tur qolmasa, bir xil tur ketma-ket qo'yiladi. Algoritm har doim tugaydi.
  - `class SessionBuilder(random: Random)`:
    - `fun vocabChunks(lesson: Lesson): List<List<Word>>` — so'zlar 6 tadan bo'laklarga bo'linadi.
    - `fun lessonPractice(lesson: Lesson): List<ExerciseItem>` — test ro'yxatida bo'lmagan barcha mashqlar, har bir so'z uchun `WordTyping`, va so'zlarning 5 talik bo'laklaridan `Match`, `arrange` qilingan holda.
    - `fun lessonTest(lesson: Lesson): List<ExerciseItem>` — `lesson.test` ro'yxatidagi mashqlar, `arrange` qilingan.
    - `fun lessonReview(lesson: Lesson): List<ExerciseItem>` — har bir so'z uchun `Flashcard` va har bir grammatika uchun tasodifiy bitta amaliy mashq.
    - `fun review(cards: List<CardEntity>, lessons: Map<String, Lesson>): List<ExerciseItem>` — `R` → `Flashcard`, `P` → `WordTyping`, `G` → shu grammatikani tekshiradigan tasodifiy amaliy mashq. Darsi yuklanmagan kartalar tashlab ketiladi.
  - `class OnboardingService(content, study, progress, settings) { suspend fun complete(currentLessonId: String) }`:
    - katalogda joriy darsdan oldingi darslar `markPassed` qilinadi;
    - ulardan kontenti mavjudlari uchun `ensureCards(origin = BACKLOG)` chaqiriladi;
    - `setCurrentLesson` va `setOnboarded` saqlanadi.

    Sozlamalardan joriy dars o'zgartirilganda ham xuddi shu metod chaqiriladi.
  - `class Grader(study: StudyRepository)`:
    - `suspend fun gradeAuto(item: ExerciseItem, correct: Boolean, usedHint: Boolean, elapsedMs: Long)` — barcha `cardIds` `AutoRating` bo'yicha baholanadi;
    - `suspend fun gradeFlashcard(item: ExerciseItem.Flashcard, rating: Rating)`;
    - `suspend fun gradeMatch(item: ExerciseItem.Match, firstTryCorrect: Set<String>)` (so'z ID'lari) — birinchi urinishda to'g'ri topilgan so'z → `GOOD`, qolganlari → `AGAIN`.

- [ ] **Step 1: Failing testlarni yozish.** Task 5 dagi `Fixtures.validLesson()` ishlatiladi.
  - `arrange_noAdjacentSameTypeWhenPossible` — 3 tur × 4 element uchun qo'shni turlar hech qachon bir xil emas.
  - `arrange_singleTypeTerminates` — faqat bir turdagi 5 ta elementda natija 5 ta element.
  - `arrange_keepsAllItems`.
  - `lessonPractice_excludesTestExercises`.
  - `review_grammarCardPicksTargetingPracticeExercise`.
  - `review_skipsCardsOfUnavailableLessons`.
  - `onboarding_marksEarlierPassedAndCreatesBacklog`.
  - `onboarding_skipsLessonsWithoutContent` — u01 fayllari yo'q; u01 darslari `PASSED` holatida, ular uchun karta yaratilmagan, xato yo'q.
  - `onboarding_rerunDoesNotDuplicateCards`.
  - `grader_matchGradesEachWord`.

  Repozitoriylar Task 7 dagi kabi Robolectric va in-memory Room bilan ishlaydi.
- [ ] **Step 2: Testlarni ishga tushirish.** Buyruq: `./gradlew :app:testDebugUnitTest --tests "uz.hangulfriend.study.*"`. Kutilgan natija: FAIL.
- [ ] **Step 3: `ExerciseItem`, `arrange`, `SessionBuilder`, `OnboardingService` va `Grader`ni yozish.**
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit**
```bash
git add app && git commit -m "feat(study): session builder, onboarding service, grader"
```

### Task 9: UI qobig'i — `AppContainer`, navigatsiya, onboarding, kitob xaritasi, Bugun, Sozlamalar

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/AppContainer.kt`
- Create: `app/src/main/java/uz/hangulfriend/ui/theme/*.kt`, `ui/Nav.kt`, `ui/onboarding/OnboardingScreen.kt`, `ui/map/BookMapScreen.kt`, `ui/home/HomeScreen.kt`, `ui/settings/SettingsScreen.kt`, har bir ekran uchun `*ViewModel.kt`
- Modify: `MainActivity.kt`, `HangulFriendApp.kt`, `res/values/strings.xml`
- Test: `app/src/test/java/uz/hangulfriend/ui/BookMapViewModelTest.kt`

**Interfaces:**
- Consumes: Task 5–8 dagi barcha repozitoriy va servislar.
- Produces:
  - `class AppContainer(context: Context)`:
    - `content` (`strict = BuildConfig.DEBUG`), `db`, `study`, `progress`, `settings`, `onboarding`, `sessionBuilder`, `grader`;
    - `clock = Clock.systemDefaultZone()`.
  - Navigatsiya marshrutlari: `onboarding`, `home`, `map`, `lesson/{lessonId}`, `session/{mode}/{lessonId?}` (`mode` ∈ `practice|test|review|lessonReview`), `settings`.
  - `data class LessonRow(val entry: CatalogEntry, val available: Boolean, val status: LessonStatus, val percent: Int)`
    - `percent`: `COMPLETED` va `VERIFIED` holatida 100, aks holda `stage * 20`.
  - `BookMapViewModel.rows: StateFlow<List<LessonRow>>`

- [ ] **Step 1: Failing test yozish.** `rows_mergeCatalogAvailabilityAndProgress`: yozilmagan darsda `available = false` va "tez orada" ko'rsatiladi; `stage = 2` da foiz 40; `COMPLETED` da foiz 100.
- [ ] **Step 2: Testni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: Ekranlarni yozish.**
  - **Onboarding:** salomlashuv, bo'limlar bo'yicha guruhlangan 18 dars ro'yxati, bitta dars tanlanadi, "Davom etish" tugmasi `onboarding.complete` ni chaqiradi va `home`ga o'tadi. `settings.onboarded == false` bo'lsa, ilova shu ekrandan boshlanadi.
  - **Kitob xaritasi:** bo'lim sarlavhasi; har bir darsda `titleKo`, `titleUz`, holat belgisi va progress chizig'i. Kontenti yozilmagan dars bosilmaydi va "tez orada" deb ko'rsatiladi.
  - **Bugun:** "Takrorlash: N ta" va "Boshlash" tugmasi (`N == 0` bo'lsa, tugma o'chiq va "Bugun hammasi takrorlandi!" yozuvi chiqadi). "Darsni davom ettirish: <titleKo>" joriy darsning ekraniga olib boradi.
  - **Sozlamalar:** joriy darsni tanlash (`onboarding.complete` qayta chaqiriladi); kunlik yangi karta limiti, 5–50 oralig'ida, 5 qadam bilan.
  - Pastki navigatsiya: Bugun / Kitob / Sozlamalar.
  - `lesson` va `session` marshrutlari uchun hozircha vaqtinchalik `Text` placeholder qo'yiladi. Ular Task 10–11 da haqiqiy ekranlar bilan almashtiriladi.
- [ ] **Step 4: Testni ishga tushirish va qurish.** Buyruq: `./gradlew :app:testDebugUnitTest :app:assembleDebug`. Kutilgan natija: PASS va `BUILD SUCCESSFUL`.
- [ ] **Step 5: Qurilmada tekshirish.** Birinchi kirishda onboarding chiqadi. `u02_l1` tanlangach, xaritada 1-bo'lim "o'tilgan", 3–9-bo'limlar "tez orada" ko'rinadi. Ilova qayta ochilganda onboarding boshqa chiqmaydi.
- [ ] **Step 6: Commit**
```bash
git add app && git commit -m "feat(ui): app shell, onboarding, book map, home, settings"
```

### Task 10: Mashq komponentlari va sessiya ekrani

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/ui/exercise/FlashcardView.kt`, `TypingView.kt`, `MatchView.kt`, `ChoiceView.kt`, `BuildSentenceView.kt`, `FeedbackPanel.kt`, `ExerciseView.kt`
- Create: `app/src/main/java/uz/hangulfriend/ui/session/SessionScreen.kt`, `SessionViewModel.kt`, `feedbackText.kt`
- Test: `app/src/test/java/uz/hangulfriend/ui/SessionViewModelTest.kt`, `FeedbackTextTest.kt`

**Interfaces:**
- Consumes: `ExerciseItem`, `SessionBuilder`, `Grader`, `AnswerChecker`, `ProgressRepository.recordTest`, `StudyRepository.dueQueue`, `ContentRepository`.
- Produces:
  - `@Composable fun ExerciseView(item: ExerciseItem, onResult: (ExerciseOutcome) -> Unit)`
  - `sealed interface ExerciseOutcome`:
    - `Checked(correct: Boolean, usedHint: Boolean, elapsedMs: Long, check: CheckResult?)`;
    - `Rated(rating: Rating)` — flashcard uchun;
    - `Matched(firstTryCorrect: Set<String>)` — birinchi urinishda to'g'ri topilgan so'z ID'lari.
  - `fun feedbackText(f: Feedback): Int` — string resurs ID'si.
  - `SessionViewModel(mode, lessonId?)`:
    - `items`, `index`, `correctCount`;
    - `fun submit(outcome: ExerciseOutcome)` — `Grader` orqali baholaydi;
    - `fun next()`;
    - `val finished`, `val scorePercent`.

    `test` rejimida sessiya tugaganda `recordTest(lessonId, scorePercent)` chaqiriladi. `review` rejimida elementlar `dueQueue(settings.dailyNewLimit)` + `SessionBuilder.review` orqali olinadi.

**Mashq turlarining ko'rinishi:**

| `typeKey` | Komponent | Tekshirish |
|---|---|---|
| `flashcard` | old tomonda `ko`, bosilganda `uz` va misol ochiladi; 4 tugma: Qayta / Qiyin / Yaxshi / Oson | `Rated` |
| `reverse_typing` | `uz` ko'rsatiladi, `TextField`ga koreyscha yoziladi | `AnswerChecker.check(input, listOf(word.ko))` |
| `match` | 2 ustun, tanlab juftlash | `Matched` |
| `situation_choice` | `promptUz` va `options` tugmalari | variant `answers` ichida bo'lsa, to'g'ri (checker ishlatilmaydi) |
| `conjugate` | `base` + `form` → `TextField` | `AnswerChecker` |
| `fill_blank` | `sentence` (`___` belgisi ajratib ko'rsatiladi) → `TextField` | `AnswerChecker` |
| `build_sentence` | `tokens` aralash chiplar, bosib tartiblanadi | `joinToString(" ")` → `AnswerChecker` |
| `find_error` | `sentence` → tuzatilgan gap `TextField`da | `AnswerChecker` |
| `translate` | `sourceUz` → `TextField` | `AnswerChecker`; noto'g'ri bo'lsa, barcha `answers` namuna sifatida ko'rsatiladi |

- "Yordam" tugmasi faqat `hintUz` mavjud bo'lsa ko'rinadi va bosilsa `usedHint = true` bo'ladi.
- Vaqt (`elapsedMs`) mashq ko'rsatilgan paytdan javob tekshirilgan paytgacha hisoblanadi.
- `FeedbackPanel` quyidagilarni ko'rsatadi: `feedbackText(feedback)`, to'g'ri javob (`closest` yoki `answers[0]`), `whyUz`, va "Keyingi" tugmasi.

**`feedbackText` matnlari** (`strings.xml`):

| `Feedback` | Matn |
|---|---|
| `CORRECT` | "To'g'ri!" |
| `SPACING` | "Deyarli! Bo'sh joy xato." |
| `FINAL_CONSONANT` | "Deyarli! 받침 (oxirgi undosh) xato." |
| `VOWEL` | "Deyarli! Unli xato." |
| `ONE_LETTER` | "Deyarli! Bitta harf xato." |
| `WRONG` | "Noto'g'ri." |

Sessiya oxirida natija ekrani chiqadi: "N / M to'g'ri (X%)". Test rejimida 80% va undan yuqori natijada "Dars tugatildi! 🎉", aks holda "Yana urinib ko'ring — 80% kerak" yozuvi chiqadi.

- [ ] **Step 1: Failing testlarni yozish.**
  - `feedbackText_coversAllValues`.
  - `session_testModeRecordsScore` — 15 tadan 12 tasi to'g'ri → `recordTest(…, 80)` → holat `COMPLETED`.
  - `session_wrongAnswerGradesAgain`.
  - `session_reviewModeBuildsFromDueQueue`.
  - `session_emptyReviewFinishesImmediately`.
- [ ] **Step 2: Testlarni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: Komponentlarni va sessiya ekranini yozish.**
- [ ] **Step 4: Testlarni ishga tushirish va qurish.** Buyruq: `./gradlew :app:testDebugUnitTest :app:assembleDebug`. Kutilgan natija: PASS.
- [ ] **Step 5: Commit**
```bash
git add app && git commit -m "feat(ui): exercise views and session screen for practice, test and review"
```

### Task 11: Dars ekrani — lug'at, grammatika, dialog

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/ui/lesson/LessonScreen.kt`, `LessonViewModel.kt`, `VocabStage.kt`, `GrammarStage.kt`, `DialogueStage.kt`
- Test: `app/src/test/java/uz/hangulfriend/ui/LessonViewModelTest.kt`

**Interfaces:**
- Consumes: `ContentRepository.lesson`, `StudyRepository.ensureCards`, `ProgressRepository.setStage`, `SessionBuilder.vocabChunks`, `Grader`. Task 10 dagi `ExerciseView` va `MatchView` komponentlari.
- Produces:
  - `LessonViewModel(lessonId)`:
    - `stage: StateFlow<Int>` — 0 Lug'at, 1 Grammatika, 2 Dialog, 3 Mashqlar, 4 Test;
    - `fun goToStage(i: Int)` — `setStage` chaqiriladi.

    Dars birinchi ochilganda `ensureCards(origin = LESSON)` chaqiriladi.

- [ ] **Step 1: Failing testlarni yozish.**
  - `open_createsLessonCardsAndSetsInProgress`.
  - `goToStage_persistsAndResumes` — yangi ViewModel saqlangan bosqichdan boshlaydi.
- [ ] **Step 2: Testlarni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: Bosqichlarni yozish.**
  - **Lug'at:** pager, har bir karta: `ko` (katta shriftda), `uz`, `pos`, `exampleKo` / `exampleUz`. Har 6 so'zdan keyin shu so'zlardan `Match` mashqi (`Grader.gradeMatch`).
  - **Grammatika:** har bir grammatika uchun `pattern`, `meaningUz`, `formation` jadvali, `explanationMd` (markdown renderer), `examples`, `mistakes` (`wrong` qizil va ustidan chizilgan, `right` yashil, `whyUz`), `uzCompare`. Oxirida shu grammatikani tekshiradigan bitta amaliy mashq (Task 10 dagi `ExerciseView`).
  - **Dialog:** qatorlar chat pufakchalari ko'rinishida, qahramon nomi `characters.json`dan olinadi. "Tarjimani ko'rsatish" almashtirgichi.
  - **Mashqlar** va **Test** bosqichlari `session/practice/{id}` va `session/test/{id}` sahifalariga o'tadi.
  - Yuqorida 5 bosqichli stepper. Istalgan bosqichga bosib o'tish mumkin.
  - Dars ekranida "Shu darsni takrorlash" tugmasi `session/lessonReview/{id}` ga olib boradi.
- [ ] **Step 4: Testlarni ishga tushirish va qurish.** Kutilgan natija: PASS va `BUILD SUCCESSFUL`.
- [ ] **Step 5: Commit**
```bash
git add app && git commit -m "feat(ui): lesson screen with vocab, grammar and dialogue stages"
```

### Task 12: Qurilmada to'liq tekshiruv

**Files:** o'zgarish yo'q. Topilgan xatolar tegishli task fayllarida tuzatiladi va alohida commit qilinadi.

- [ ] **Step 1: Barcha testlar va build.** Buyruq: `./gradlew test :app:testDebugUnitTest :app:assembleDebug`. Kutilgan natija: `BUILD SUCCESSFUL`, 0 failures.
- [ ] **Step 2: Toza o'rnatish.** Buyruq: `adb uninstall uz.hangulfriend; adb install app/build/outputs/apk/debug/app-debug.apk`.
- [ ] **Step 3: Ssenariyni qo'lda o'tish.**
  1. Onboarding'da `u02_l1` tanlanadi.
  2. Lug'at bosqichi o'tiladi (Match mashqi bilan birga).
  3. Grammatika sahifalari ko'riladi (markdown to'g'ri chiqadi).
  4. Dialog'da tarjimani ochib-yopish ishlaydi.
  5. Mashqlar o'tiladi. Gboard koreys klaviaturasida ataylab "갔면" kiritiladi va "받침 xato" izohi chiqishi tekshiriladi.
  6. Dars testi 80% dan yuqori natija bilan o'tiladi; xaritada dars 100% ko'rinadi.
  7. "Bugun" ekranida takrorlash soni 0 dan katta ekani tekshiriladi va takrorlash sessiyasi o'tiladi.
  8. Sozlamalarda joriy dars `u02_l2` ga o'zgartiriladi; takrorlash soni keskin ko'paymasligi tekshiriladi.
  9. Ilova yopilib qayta ochiladi; progress saqlangan bo'lishi kerak.
- [ ] **Step 4: Foydalanuvchiga topshirish.** Foydalanuvchi APK'ni o'z telefonida sinab ko'radi. `u02_l1` va `u02_l2` kontentini tekshirib, topilgan xatolar bo'yicha fikr bildiradi.
- [ ] **Step 5: Commit** (agar tuzatishlar bo'lgan bo'lsa)
```bash
git add -A && git commit -m "fix: issues found in device walkthrough"
```
