# Hangul Friend — 2-bosqich (Audio va nutq) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (foydalanuvchi subagentsiz ishlashni so'ragan) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Barcha so'z, misol va dialoglar uchun tabiiy ovozli audio. Yangi mashqlar: eshitib tanlash, diktant, eshitib savolga javob berish, gapirish. Dialog bosqichida rol o'ynash rejimi.

**Architecture:**
- **Audio yaratish (kompyuterda):** Python skripti `tools/audio_gen.py` Google Cloud Text-to-Speech REST API orqali OGG fayllar yaratadi, ularni `assets/audio/` ga saqlaydi va fayl nomlarini dars JSON'lariga yozib qo'yadi.
- **App tomoni:**
  - `audio` paketi — Media3 pleyer;
  - `speech` paketi — Android `SpeechRecognizer` o'rami;
  - baholash (o'xshashlik va noto'g'ri bo'g'inlar) — sof Kotlin, `:core:hangul` ichida.
- **Sessiyalar:** `SessionBuilder` yangi mashq turlarini audio mavjud bo'lsa va nutqni tanish ishlasagina qo'shadi.

**Tech Stack:** Media3 ExoPlayer, Android `SpeechRecognizer` (`ko-KR`), Google Cloud TTS v1 REST (Python `urllib`, qo'shimcha kutubxonasiz), avvalgi bosqich stack'i.

**Spec:** `docs/superpowers/specs/2026-09-29-hangul-friend-design.md` (3.3, 3.4, 4.6, 4.8, 4.9, 5-bo'limlar)

## Global Constraints

- 1-bosqichdagi barcha global cheklovlar amal qiladi: paket `uz.hangulfriend`, `minSdk 26`, `compileSdk`/`targetSdk 37`, UI matnlari o'zbekcha va `strings.xml`da, kitob kontenti ko'chirilmaydi, commit'larda `Co-Authored-By` yo'q, worktree ishlatilmaydi, subagent ishlatilmaydi.
- **Audio fayl nomi:** `sha1(voice + "|" + text)` hex qiymatining birinchi 12 belgisi va `.ogg` kengaytmasi.
- **Joylashuv va format:** audio `app/src/main/assets/audio/` ga yoziladi; format OGG Opus (`audioEncoding: OGG_OPUS`).
- **Ovozlar:** ayol ovozi `ko-KR-Neural2-A`, erkak ovozi `ko-KR-Neural2-C`.
  - So'zlar, misollar va `listen_question` gaplari ayol ovozida.
  - Dialog qatorlari `characters.json`dagi `voice` maydoni bo'yicha (`female`/`male`).
- **API kaliti:** faqat `GOOGLE_TTS_API_KEY` muhit o'zgaruvchisidan olinadi. Kalit hech qachon faylga, logga yoki commit'ga yozilmaydi.
- **Gapirish mashqi:** o'xshashlik chegarasi **0.85** (jamo darajasida, bo'sh joy va tinish belgilari hisobga olinmaydi).
- **Baholash:** gapirish mashqlari va rol o'ynash FSRS kartalarini baholamaydi (talaffuz mashqi, eslab qolishni tekshirmaydi).
- **Mashg'ulot hajmi:** amaliy mashg'ulot jami 15–25 element bo'lib qoladi (spec 3.3).

## Review Focus

1. **Audio fayli yo'q element** (generator hali ishga tushirilmagan yoki yangi kontent qo'shilgan). Audio tugmasi o'chiq turadi, eshitish/diktant mashqlari yaratilmaydi, app qulamaydi. Test: Task 5 `lessonPractice_noAudioNoListeningItems`.
2. **Nutqni tanish mavjud emas yoki `ko-KR` qo'llanmaydi.** Gapirish mashqlari sessiyaga qo'shilmaydi; `SpeakView` ichida xato bo'lsa, mashqni o'tkazib yuborish mumkin. Test: Task 5 `lessonPractice_noSpeechNoSpeakItems`.
3. **Mikrofon ruxsati rad etilgan.** "O'tkazib yuborish" tugmasi mashqni baholamasdan keyingisiga o'tadi va natija hisobiga ta'sir qilmaydi. Test: Task 5 `session_skippedItemNotGradedNorScored`.
4. **Nutqni tanish natijasida bo'sh joylar, tinish belgilari yoki bo'g'inlar boshqacha bo'lishi.** Taqqoslashda bo'sh joy va tinish belgilari hisobga olinmaydi. Test: Task 1 `similarity_ignoresSpacesAndPunctuation`.
5. **Gap o'zgartirilib, audio qayta yaratilganda** eski fayllar ortiqcha qolib ketadi. `--prune` parametri hech qaysi JSON ishlatmayotgan `.ogg` fayllarni o'chiradi. Test: Task 2 `test_prune_removes_unreferenced`.

---

### Task 1: `:core:hangul` — talaffuz o'xshashligi

**Files:**
- Create: `core/hangul/src/main/kotlin/uz/hangulfriend/hangul/SpeechScore.kt`
- Test: `core/hangul/src/test/kotlin/uz/hangulfriend/hangul/SpeechScoreTest.kt`

**Interfaces:**
- Produces:
  - `data class SpeechScore(val heard: String, val similarity: Double, val passed: Boolean, val mismatched: Set<Int>)`. `mismatched` — `expected` matnidagi xato aytilgan yoki tushib qolgan Hangul bo'g'inlarining indekslari (indeks satrdagi belgi o'rni).
  - `object SpeechScorer`:
    - `const val PASS = 0.85`
    - `fun similarity(expected: String, heard: String): Double` — ikkala satr `Hangul.normalize` qilinadi, keyin barcha bo'sh joy va `.,?!` belgilari olib tashlanadi. Natija: `1 - jamoLevenshtein / max(jamoLen, 1)`, `[0, 1]` oralig'ida.
    - `fun score(expected: String, candidates: List<String>): SpeechScore` — o'xshashligi eng yuqori nomzod tanlanadi. `candidates` bo'sh bo'lsa, `similarity = 0`, `heard = ""`, barcha bo'g'inlar `mismatched` bo'ladi.
    - `mismatched` bo'g'in darajasidagi Levenshtein tekislashi orqali topiladi: almashtirilgan yoki tushib qolgan har bir bo'g'in belgilanadi.
- `AnswerChecker` ichidagi `levenshtein` modul ichida ishlatiladigan umumiy funksiyaga chiqariladi (`internal fun levenshtein(a: List<Char>, b: List<Char>): Int`), ikkala klass shuni ishlatadi.

- [ ] **Step 1: Failing testlarni yozish.**
```kotlin
@Test fun similarity_identical() = assertEquals(1.0, SpeechScorer.similarity("입어 보세요", "입어 보세요"), 1e-9)
@Test fun similarity_ignoresSpacesAndPunctuation() = assertEquals(1.0, SpeechScorer.similarity("입어 보세요.", "입어보세요"), 1e-9)
@Test fun similarity_oneJamoOff() = assertTrue(SpeechScorer.similarity("가면", "거면") in 0.7..0.9)
@Test fun score_picksBestCandidate() = assertEquals("입어 보세요", SpeechScorer.score("입어 보세요", listOf("이버 보세", "입어 보세요")).heard)
@Test fun score_passThreshold() { assertTrue(SpeechScorer.score("치마를 입어 보세요", listOf("치마를 입어 보세요")).passed); assertFalse(SpeechScorer.score("치마를 입어 보세요", listOf("바지")).passed) }
@Test fun score_marksMismatchedSyllables() = assertEquals(setOf(1), SpeechScorer.score("가면", listOf("가먼")).mismatched)
@Test fun score_emptyCandidates() { val s = SpeechScorer.score("가면", emptyList()); assertEquals(0.0, s.similarity, 0.0); assertEquals(setOf(0, 1), s.mismatched) }
```
- [ ] **Step 2: Testlarni ishga tushirish.** Buyruq: `./gradlew :core:hangul:test`. Kutilgan natija: FAIL (unresolved `SpeechScorer`).
- [ ] **Step 3: `SpeechScore.kt`ni yozish, `levenshtein`ni umumiy funksiyaga chiqarish.**
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Kutilgan natija: PASS, `AnswerCheckerTest` ham yashil.
- [ ] **Step 5: Commit**
```bash
git add core/hangul && git commit -m "feat(hangul): pronunciation similarity and mismatched syllables"
```

### Task 2: `tools/audio_gen.py` — audio generator

**Files:**
- Create: `tools/audio_gen.py`, `tools/test_audio_gen.py`

**Interfaces:**
- Produces:
  - **CLI:** `python tools/audio_gen.py [--dry-run] [--prune]`.
    - Hamma `app/src/main/assets/lessons/*.json` fayllarni o'qiydi.
    - Audiosi yo'q matnlar uchun TTS chaqiradi va JSON'ga fayl nomini yozadi.
    - `--dry-run` hech narsa yaratmaydi, faqat nechta yangi klip borligini chiqaradi.
    - `--prune` hech qaysi JSON ishlatmaydigan `.ogg` fayllarni o'chiradi.
  - `file_name(voice: str, text: str) -> str` — Global Constraints'dagi formula bo'yicha.
  - `collect(lesson: dict, voices: dict[str, str]) -> list[Clip]`, bu yerda `Clip = (text, voice, setter)`. `setter(name)` shu klipning audio maydonini dict ichida o'rnatadi. Qamrov:
    - `words[].ko` → `audio`;
    - `words[].example_ko` → `example_audio`;
    - `grammar[].examples[].ko` → `audio`;
    - `dialogue.lines[].ko` → `audio`;
    - `story.lines[].ko` → `audio` (story bo'lsa);
    - `exercises[]` ichidagi `type == "listen_question"` mashqlarining `audio_text` maydoni → `audio`.
  - `voices` — qahramon ID'si → Google ovoz nomi: `characters.json`dagi `female`/`male` qiymatlari Global Constraints'dagi ovozlarga o'giriladi.
  - `synthesize(text, voice, api_key) -> bytes` — `POST https://texttospeech.googleapis.com/v1/text:synthesize?key=...`, so'rov tanasi: `{"input":{"text":text},"voice":{"languageCode":"ko-KR","name":voice},"audioConfig":{"audioEncoding":"OGG_OPUS"}}`. Javobdagi `audioContent` base64'dan dekodlanadi.
  - `run(root: Path, synth: Callable[[str,str],bytes], dry_run: bool, prune: bool) -> Report` — sof mantiq, testda soxta `synth` bilan chaqiriladi.
  - JSON `ensure_ascii=False, indent=2` bilan va LF qator oxiri bilan yoziladi (`newline="\n"`).

- [ ] **Step 1: Failing testlarni yozish** (`unittest`, vaqtinchalik papkada kichik dars JSON'i va `characters.json` bilan).
  - `test_file_name_is_sha1_prefix` — `file_name("ko-KR-Neural2-A","옷")` natijasi `hashlib.sha1("ko-KR-Neural2-A|옷".encode()).hexdigest()[:12] + ".ogg"` ga teng.
  - `test_collect_covers_all_fields` — words/examples/grammar/dialogue/listen_question dan klip soni to'g'ri.
  - `test_dialogue_uses_character_voice` — `aziz` qatori `ko-KR-Neural2-C` bilan.
  - `test_run_writes_files_and_json` — soxta `synth` baytlarni qaytaradi; fayllar yaratiladi va JSON'dagi `audio` maydonlari to'ldiriladi.
  - `test_run_skips_existing` — ikkinchi ishga tushirishda `synth` chaqirilmaydi.
  - `test_dry_run_writes_nothing`.
  - `test_prune_removes_unreferenced` — ortiqcha `zz.ogg` o'chadi, ishlatilayotgan fayllar qoladi.
  - `test_missing_key_exits_with_message` — `GOOGLE_TTS_API_KEY` yo'q bo'lsa `main()` 2 kodi bilan chiqadi va xabar kalitni emas, o'zgaruvchi nomini aytadi.
- [ ] **Step 2: Testlarni ishga tushirish.** Buyruq: `python -m unittest tools/test_audio_gen.py -v`. Kutilgan natija: FAIL (ImportError).
- [ ] **Step 3: `audio_gen.py`ni yozish.**
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Kutilgan natija: 8 ta test OK.
- [ ] **Step 5: Commit**
```bash
git add tools/audio_gen.py tools/test_audio_gen.py && git commit -m "feat(tools): Google TTS audio generator with pruning"
```

### Task 3: Kontent modeli — `listen_question` va audio tekshiruvi

**Files:**
- Modify: `app/src/main/java/uz/hangulfriend/content/Model.kt`, `LessonValidator.kt`
- Modify: `app/src/test/java/uz/hangulfriend/Fixtures.kt`, `content/LessonValidatorTest.kt`, `content/ContentAssetsTest.kt`

**Interfaces:**
- Produces:
  - `ExerciseType.LISTEN_QUESTION` (JSON'da `"listen_question"`).
  - `Exercise` klassiga ikkita maydon qo'shiladi: `@SerialName("audio_text") val audioText: String? = null` va `val audio: String? = null`.
  - Validator qoidasi: `LISTEN_QUESTION` mashqida `audioText` bo'lishi, `options` kamida 2 ta bo'lishi va `answers` `options` ichida bo'lishi shart.
  - `ContentAssetsTest`ga ikkita yangi test:
    - `everyAudioFileExists` — JSON'dagi har bir audio nomi `assets/audio/` ichida bor;
    - `everySpeakableHasAudio` — so'z, misol, grammatika misoli, dialog qatori va `listen_question` matnlarining hammasida `audio` to'ldirilgan. Bu test `@Ignore("Task 9 audio generatsiyasigacha")` bilan yoziladi va Task 9 da yoqiladi.
- `ContentAssetsTest.lessonsMeetSpecSizes` testidagi "har bir turdan kamida 2 ta" tekshiruvidan `LISTEN_QUESTION` Task 9 gacha chiqarib qo'yiladi. Task 9 da u yana qo'shiladi.

- [ ] **Step 1: Failing testlarni yozish.**
  - `validate_listenQuestionNeedsAudioText`.
  - `validate_listenQuestionAnswerInOptions`.
  - `everyAudioFileExists` (hozircha o'tadi, chunki audio nomlari hali yo'q).
  - `Fixtures.validLesson()`ga bitta to'g'ri `LISTEN_QUESTION` qo'shiladi (`e008`, practice). `SessionBuilderTest.lessonPractice_excludesTestExercises` dagi kutilgan son 6 dan 7 ga o'zgartiriladi.
- [ ] **Step 2: Testlarni ishga tushirish.** Buyruq: `./gradlew :app:testDebugUnitTest --tests "uz.hangulfriend.content.*"`. Kutilgan natija: FAIL (unresolved `LISTEN_QUESTION`).
- [ ] **Step 3: Model va validatorni o'zgartirish.**
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit**
```bash
git add app && git commit -m "feat(content): listen_question exercise type and audio checks"
```

### Task 4: Audio pleyer va play tugmalari

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts` (Media3 `media3-exoplayer`, eng so'nggi barqaror versiya)
- Create: `app/src/main/java/uz/hangulfriend/audio/AudioPlayer.kt`, `app/src/main/java/uz/hangulfriend/ui/AudioButton.kt`
- Modify: `AppContainer.kt`, `MainActivity.kt`, `ui/lesson/Stages.kt`, `ui/exercise/ChoiceViews.kt` (`FlashcardView`)
- Test: `app/src/test/java/uz/hangulfriend/audio/AudioPlayerTest.kt`

**Interfaces:**
- Produces:
  - `fun audioAssetUri(file: String): String` — natija `"asset:///audio/$file"`.
  - `class AudioPlayer(context: Context)`:
    - `fun play(file: String)` — oldingi ijroni to'xtatib, yangisini boshlaydi;
    - `fun release()`.
    - Pleyer faqat birinchi `play` chaqiruvida yaratiladi.
  - `val LocalAudioPlayer = staticCompositionLocalOf<AudioPlayer?> { null }` — `MainActivity` qiymat beradi va `onDestroy`da `release()` chaqiradi.
  - `@Composable fun AudioButton(file: String?, modifier: Modifier = Modifier)` — `IconButton` va `Icons.AutoMirrored.Filled.VolumeUp` ikonkasi. `file == null` yoki pleyer yo'q bo'lsa, tugma o'chiq.
- Play tugmalari qo'yiladigan joylar:
  - `WordCard` — so'z va misol;
  - grammatika misollari;
  - dialog qatorlari;
  - `FlashcardView` — so'z, oldingi tomonda.

- [ ] **Step 1: Failing test yozish.** `audioAssetUri_format`: `assertEquals("asset:///audio/ab12.ogg", audioAssetUri("ab12.ogg"))`.
- [ ] **Step 2: Testni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: Pleyer, `AudioButton` va ularni ekranlarga ulashni yozish.**
- [ ] **Step 4: Test va qurish.** Buyruq: `./gradlew :app:testDebugUnitTest :app:assembleDebug`. Kutilgan natija: PASS. Audio hali yo'q, shuning uchun tugmalar o'chiq ko'rinadi.
- [ ] **Step 5: Commit**
```bash
git add gradle app && git commit -m "feat(audio): Media3 player and play buttons"
```

### Task 5: Sessiya — eshitish, diktant, gapirish va o'tkazib yuborish

**Files:**
- Modify: `app/src/main/java/uz/hangulfriend/study/ExerciseItem.kt`, `SessionBuilder.kt`
- Modify: `app/src/main/java/uz/hangulfriend/ui/session/SessionController.kt`, `SessionScreen.kt`, `ui/Nav.kt`, `AppContainer.kt`
- Test: `study/SessionBuilderTest.kt`, `ui/SessionControllerTest.kt`

**Interfaces:**
- Consumes: `Word.audio`, `Example.audio`, `Exercise.audio`.
- Produces:
  - Yangi `ExerciseItem` variantlari:
    - `ListenChoose(word: Word, options: List<Word>)` — `typeKey "listen_choose"`, baholanadigan karta `#R`. `options` — 4 ta turli so'z, to'g'ri so'z ham ichida (lekin kamroq so'z bo'lsa, borlari).
    - `Dictation(word: Word)` — `"dictation"`, karta `#P`.
    - `Speak(ko: String, uz: String, audio: String?)` — `"speak"`, `cardIds = emptyList()`.
  - `LISTEN_QUESTION` mashqlari mavjud `Authored` orqali ishlaydi (`typeKey "listen_question"`).
  - `ExerciseOutcome.Skipped` — baholanmaydi. Natija hisobida `SessionState.scored` qatnashmaydi.
  - `SessionState.scorePercent` endi `correctCount * 100 / scored` ga teng, bu yerda `scored` — o'tkazib yuborilmagan javoblar soni.
  - `SessionBuilder.lessonPractice(lesson: Lesson, speechAvailable: Boolean): List<ExerciseItem>`. Chegaralar:

    | Tur | Chegara |
    |---|---|
    | authored (`LISTEN_QUESTION` ham shu ichida) | 12 ta, har bir grammatikaga kamida 2 ta |
    | `WordTyping` | 3 ta |
    | `Match` | 1 ta |
    | `ListenChoose` | 3 ta, faqat `audio != null` so'zlardan |
    | `Dictation` | 2 ta, faqat `audio != null` so'zlardan |
    | `Speak` | 2 ta, faqat `speechAvailable` bo'lsa va audiosi bor grammatika misollaridan |

  - `SessionBuilder.review(...)` — `PRODUCE` kartasi so'zda audio bo'lsa 50% ehtimol bilan `Dictation`, aks holda `WordTyping`. `RECOGNIZE` kartasi avvalgidek `Flashcard`.
  - `SessionController` konstruktoriga `speechAvailable: Boolean` parametri qo'shiladi.

- [ ] **Step 1: Failing testlarni yozish.**
  - `lessonPractice_noAudioNoListeningItems` — audiosiz fixture'da `listen_choose` yoki `dictation` yo'q.
  - `lessonPractice_withAudioAddsListening` — hamma so'zda audio bor fixture: `listen_choose` va `dictation` elementlari bor.
  - `lessonPractice_noSpeechNoSpeakItems`.
  - `lessonPractice_speakOnlyWithAudio`.
  - `listenChoose_hasFourDistinctOptionsIncludingAnswer` (6 so'zli fixture).
  - `lessonPractice_bigLessonIsCappedAndCoversGrammar` — o'lcham 15..25 oralig'ida qoladi, audio va nutq yoqilgan holatda.
  - `session_skippedItemNotGradedNorScored` — 2 element: birinchisi `Skipped`, ikkinchisi to'g'ri → `scorePercent == 100`, birinchi element kartalari uchun log yo'q.
  - `session_speakDoesNotGrade` — `Speak` elementiga `Checked(correct = true)` yuborilganda log yo'q, `correctCount` 1.
- [ ] **Step 2: Testlarni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: `ExerciseItem`, `SessionBuilder`, `SessionController` va `AppContainer`ni o'zgartirish.** `speechAvailable` qiymati `SpeechRecognizer.isRecognitionAvailable(context)` orqali olinadi (bu Task 6 dagi `SpeechInput.isAvailable()`; bu yerda vaqtincha `false`).
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Buyruq: `./gradlew :app:testDebugUnitTest`. Kutilgan natija: PASS.
- [ ] **Step 5: Commit**
```bash
git add app && git commit -m "feat(study): listening, dictation and speaking items; skipped outcome"
```

### Task 6: Nutqni tanish

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/speech/SpeechInput.kt`
- Modify: `app/src/main/AndroidManifest.xml` (`RECORD_AUDIO` ruxsati; `<queries>` ichida `android.speech.RecognitionService` intent), `AppContainer.kt`
- Test: `app/src/test/java/uz/hangulfriend/speech/SpeechInputTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface SpeechEvent { data class Result(val candidates: List<String>) : SpeechEvent; data class Error(val reason: SpeechError) : SpeechEvent }`
  - `enum class SpeechError { NO_MATCH, LANGUAGE_UNAVAILABLE, NO_PERMISSION, NETWORK, OTHER }`
  - `fun mapRecognizerError(code: Int): SpeechError`:
    - `ERROR_NO_MATCH`, `ERROR_SPEECH_TIMEOUT` → `NO_MATCH`;
    - `ERROR_LANGUAGE_NOT_SUPPORTED`, `ERROR_LANGUAGE_UNAVAILABLE` → `LANGUAGE_UNAVAILABLE`;
    - `ERROR_INSUFFICIENT_PERMISSIONS` → `NO_PERMISSION`;
    - `ERROR_NETWORK`, `ERROR_NETWORK_TIMEOUT` → `NETWORK`;
    - boshqa kodlar → `OTHER`.
  - `class SpeechInput(context: Context)`:
    - `fun isAvailable(): Boolean`;
    - `fun listen(onEvent: (SpeechEvent) -> Unit)` — `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`, `EXTRA_LANGUAGE "ko-KR"`, `EXTRA_MAX_RESULTS 5`, `LANGUAGE_MODEL_FREE_FORM`;
    - `fun cancel()`;
    - `fun destroy()`.
    - Hamma metodlar asosiy oqimda chaqiriladi.
  - `AppContainer.speech: SpeechInput`. Task 5 dagi `speechAvailable` endi `speech.isAvailable()` dan olinadi.

- [ ] **Step 1: Failing test yozish.** `mapRecognizerError_mapsKnownCodes` — yuqoridagi har bir moslik tekshiriladi, noma'lum kod (masalan `999`) `OTHER` beradi.
- [ ] **Step 2: Testni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: `SpeechInput`ni yozish va manifest'ni o'zgartirish.**
- [ ] **Step 4: Test va qurish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit**
```bash
git add app && git commit -m "feat(speech): SpeechRecognizer wrapper for ko-KR"
```

### Task 7: Yangi mashq ko'rinishlari

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/ui/exercise/ListenViews.kt` (`ListenChooseView`, `DictationView`, `ListenQuestionView`), `ui/exercise/SpeakView.kt`
- Modify: `ui/exercise/ExerciseView.kt`, `res/values/strings.xml`
- Test: `app/src/test/java/uz/hangulfriend/ui/SpeakTextTest.kt`

**Interfaces:**
- Consumes: `LocalAudioPlayer`, `AudioButton`, `SpeechInput`, `SpeechScorer`, `ExerciseOutcome.Skipped`, `TypingView`, `ChoiceView`.
- Produces:
  - `ListenChooseView`: ochilganda audio avtomatik bir marta ijro etiladi; 4 ta o'zbekcha ma'no tugma sifatida ko'rsatiladi; `Checked` natija qaytaradi.
  - `DictationView`: `TypingView` + audio tugmasi, ochilganda audio avtomatik ijro etiladi; koreyscha matn ko'rsatilmaydi.
  - `ListenQuestionView`: `ChoiceView` + audio tugmasi, `audioText` ko'rsatilmaydi.
  - `SpeakView`:
    - gap (`ko`) va tarjimasi (`uz`) ko'rsatiladi, "Namuna" audio tugmasi bor;
    - mikrofon tugmasi bosilganda: ruxsat bo'lmasa `rememberLauncherForActivityResult(RequestPermission)` bilan so'raladi, keyin `SpeechInput.listen` chaqiriladi;
    - natijada `SpeechScorer.score` hisoblanadi; gap ko'rsatiladi, `mismatched` bo'g'inlar `WrongRed` rangida; foiz ko'rsatiladi; "Qayta aytish" tugmasi bor;
    - `Checked(correct = passed, …)` faqat bir marta, birinchi urinishda yuboriladi;
    - xato holatlar uchun `speakErrorText(e: SpeechError): Int` matnini ko'rsatadi, yonida "O'tkazib yuborish" tugmasi (`Skipped` + `onNext`).
  - `fun speakErrorText(e: SpeechError): Int` — har bir `SpeechError` uchun alohida string resurs. `LANGUAGE_UNAVAILABLE` matni Google ilovasida koreys tilini (offline nutqni tanish) o'rnatish yo'riqnomasini beradi.

- [ ] **Step 1: Failing test yozish.** `speakErrorText_coversAllErrors`: barcha qiymatlar uchun resurs ID'lari 0 emas va hammasi har xil.
- [ ] **Step 2: Testni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: Ko'rinishlarni yozish va ularni `ExerciseView` dispatch'iga ulash.**
- [ ] **Step 4: Test va qurish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit**
```bash
git add app && git commit -m "feat(ui): listen, dictation, listen-question and speak views"
```

### Task 8: Dialog — rol o'ynash rejimi

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/ui/lesson/Roleplay.kt`
- Modify: `ui/lesson/Stages.kt` (`DialogueStage`ga "Tinglash / Rol o'ynash" almashtirgichi), `res/values/strings.xml`
- Test: `app/src/test/java/uz/hangulfriend/ui/RoleplayTest.kt`

**Interfaces:**
- Produces:
  - `data class RoleplayStep(val index: Int, val line: Line, val mine: Boolean)`
  - `fun roleplaySteps(lines: List<Line>, myRole: String): List<RoleplayStep>`
  - `fun speakers(lines: List<Line>): List<String>` — takrorlanmaydigan, birinchi paydo bo'lish tartibida.
  - `@Composable fun RoleplayView(lesson: Lesson, characters: Map<String, Character>, speech: SpeechInput)`:
    - avval rol tanlanadi (`speakers` ro'yxatidan);
    - keyin qatorlar ketma-ket o'tadi: sherik qatori audio bilan ijro etiladi va avtomatik keyingisiga o'tiladi; foydalanuvchi qatorida uning koreyscha matni ko'rsatiladi, mikrofon orqali aytiladi va `SpeechScorer` natijasi chiqadi;
    - FSRS baholanmaydi;
    - nutqni tanish mavjud bo'lmasa, rol o'ynash tugmasi o'rniga `speakErrorText(LANGUAGE_UNAVAILABLE)` matni chiqadi.

- [ ] **Step 1: Failing testlarni yozish.**
  - `roleplaySteps_marksMyLines` — `aziz` roli uchun `mine` bayrog'i to'g'ri.
  - `speakers_inFirstAppearanceOrder`.
- [ ] **Step 2: Testlarni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: `Roleplay.kt`ni yozish va uni `DialogueStage`ga ulash.**
- [ ] **Step 4: Test va qurish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit**
```bash
git add app && git commit -m "feat(ui): dialogue roleplay with speech scoring"
```

### Task 9: Kontent — `listen_question` va audio generatsiyasi

**Files:**
- Modify: `app/src/main/assets/lessons/u02_l1.json`, `u02_l2.json`
- Create: `app/src/main/assets/audio/*.ogg` (generator yaratadi)
- Modify: `app/src/test/java/uz/hangulfriend/content/ContentAssetsTest.kt`

- [ ] **Step 1: `listen_question` mashqlarini qo'shish.** Har bir darsga 3 ta amaliy `listen_question` qo'shiladi.
  - ID'lar: `uXX_lY_e031`–`e033`.
  - `audio_text` — dars grammatikasidagi qisqa gap yoki savol-javob.
  - `prompt_uz` — o'zbekcha savol, `options` — 4 ta o'zbekcha variant.
  - `targets` — tegishli grammatika yoki so'z.
- [ ] **Step 2: Testlarni yoqish.** `ContentAssetsTest` ichida:
  - `everySpeakableHasAudio` dagi `@Ignore` olib tashlanadi;
  - `lessonsMeetSpecSizes`ga `LISTEN_QUESTION` qaytariladi, lekin "kamida 2 ta" o'rniga "kamida 3 ta" talabi bilan.

  Ishga tushiring: `python tools/validate_content.py`. Kutilgan natija: FAIL (`everySpeakableHasAudio`).
- [ ] **Step 3: Audio yaratish.**
  - Foydalanuvchidan `GOOGLE_TTS_API_KEY` muhit o'zgaruvchisini o'rnatishni so'rang. Kalit chatga yozilmaydi.
  - Avval `python tools/audio_gen.py --dry-run` ishga tushiriladi. Kutilgan natija: klip soni chiqadi, taxminan 250–350 ta.
  - Keyin `python tools/audio_gen.py` ishga tushiriladi.
  - Agar `ko-KR-Neural2-A` yoki `ko-KR-Neural2-C` ovozi mavjud bo'lmasa (xato javob kelsa), `voices.list` natijasidan shu jinsdagi birinchi `Neural2` yoki `Wavenet` ovozi olinadi va bu qaror ledger'ga yoziladi.
- [ ] **Step 4: Validatsiya.** Buyruq: `python tools/validate_content.py`. Kutilgan natija: PASS. `du -sh app/src/main/assets/audio` hajmi 10 MB dan kichik bo'lishi kerak.
- [ ] **Step 5: Commit**
```bash
git add app/src/main/assets app/src/test && git commit -m "content: listen questions and generated audio for unit 2"
```

### Task 10: Qurilmada tekshiruv

**Files:** o'zgarish yo'q. Topilgan xatolar tegishli task fayllarida tuzatiladi va alohida commit qilinadi.

- [ ] **Step 1: Barcha testlar va build.** Buyruq: `./gradlew :core:hangul:test :core:srs:test :app:testDebugUnitTest :app:assembleDebug` va `python -m unittest tools/test_audio_gen.py`. Kutilgan natija: hammasi yashil.
- [ ] **Step 2: O'rnatish.** Buyruq: `adb install -r app/build/outputs/apk/debug/app-debug.apk` (progress saqlanadi).
- [ ] **Step 3: Qo'lda tekshiruv (foydalanuvchi bilan).**
  - So'z kartasi va dialogda ovoz chiqadi.
  - Mashqlarda eshitib tanlash va diktant ishlaydi.
  - Gapirish mashqida mikrofon ruxsati so'raladi. Birinchi marta rad etilsa, "O'tkazib yuborish" ishlaydi.
  - Gapni aytganda foiz va qizil bo'g'inlar ko'rsatiladi.
  - Dialogda rol o'ynash ishlaydi.
- [ ] **Step 4: Branch va PR.** Push qilib, `master`ga PR ochiladi (foydalanuvchi merge qiladi).
