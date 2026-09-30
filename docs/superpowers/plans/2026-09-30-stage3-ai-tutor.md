# Hangul Friend — 3-bosqich (AI yordamchi, Gemini) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (foydalanuvchi subagentsiz ishlashni so'ragan). Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** "Tushunmadim" chati, xato javobni AI orqali tushuntirish va tarjimani AI orqali tekshirish. Barchasi Google Gemini bilan ishlaydi; kalit limiti tugasa keyingi kalitga o'tiladi.

**Architecture:**
- `ai` paketi uch qismdan iborat:
  - `GeminiClient` — kalit va modellar ketma-ketligini boshqaradi, testlarda soxta `GeminiTransport` bilan ishlaydi;
  - `TutorPrompts` — sof matn yig'uvchi;
  - `TutorService` — yuqori darajadagi amallar.
- UI: `TutorSheet` (pastdan ochiladigan chat oynasi). `TutorService` ekranlarga `LocalTutor` orqali beriladi.
- Kalitlar build paytida `.env`dan `BuildConfig`ga yoziladi.

**Tech Stack:** Gemini API v1beta REST, `HttpURLConnection`, kotlinx.serialization, Compose `ModalBottomSheet`.

**Spec:** `docs/superpowers/specs/2026-09-29-hangul-friend-design.md` §4.7 (2026-09-30 o'zgarishi) va §5.

## Global Constraints

- 1- va 2-bosqichlarning barcha cheklovlari amal qiladi: o'zbekcha UI, `strings.xml`, commit'larda `Co-Authored-By` yo'q, worktree ishlatilmaydi, subagent ishlatilmaydi.
- **Kalitlar:** faqat `.env`da (`.gitignore`da). Kalit logga, testga yoki commit'ga yozilmaydi. Testlarda soxta kalitlar (`k1`, `k2`, `k3`) ishlatiladi.
- **Modellar:** `listOf("gemini-3.8-flash", "gemini-3.5-flash")`.
- **HTTP holat kodlari:**
  - 429, 401, 403 → keyingi kalitga o'tiladi;
  - 500, 503 → shu kalit bilan keyingi modelga o'tiladi;
  - boshqa kodlar → `AiResult.Failed`.
- **So'rov:** `POST {base}/models/{model}:generateContent`, `x-goog-api-key` sarlavhasi. Tana `systemInstruction` va `contents` (`role`: `user` yoki `model`), JSON rejimida `generationConfig.responseMimeType = "application/json"`.
- **Vaqt chegarasi:** ulanish 15 s, o'qish 60 s.

## Review Focus

1. **Hamma kalitlar limitga yetdi.** Tushunarli xabar chiqadi, app qulamaydi va cheksiz qayta urinmaydi. Test: `generate_allKeysExhausted`.
2. **Internet yo'q** (`IOException`). "Offline" natijasi qaytadi, tarjima esa namunaviy javoblar bilan tekshiriladi. Testlar: `generate_ioErrorIsOffline`, `checkTranslation_failureReturnsNull`.
3. **AI tarjimani tekshirishda buzilgan yoki JSON bo'lmagan javob qaytardi.** Natija `null` bo'ladi va namunaviy javoblar bilan tekshiruvga qaytiladi. Test: `parseVerdict_malformedIsNull`.
4. **Kalit sozlanmagan** (`.env` yo'q). "AI sozlanmagan" xabari chiqadi. Test: `generate_noKeys`.
5. **AI javobi xavfsizlik filtri tufayli bo'sh qaytdi** (`candidates` yo'q yoki `finishReason = SAFETY`). Natija `Failed` bo'ladi. Test: `generate_emptyCandidatesFails`.

---

### Task 1: Kalitlarni build'ga ulash

**Files:** `app/build.gradle.kts`

- [ ] **Step 1: `.env`ni o'qish.** `rootProject.file(".env")` bo'lsa, `KEY=VALUE` qatorlari o'qiladi. `GEMINI_API_KEY` bilan boshlanadigan kalitlar fayldagi tartibda olinadi va vergul bilan birlashtiriladi, natija `buildConfigField("String", "GEMINI_KEYS", "\"...\"")` bo'ladi. Fayl yo'q bo'lsa, qiymat `""`.
- [ ] **Step 2: Tekshirish.** `./gradlew :app:assembleDebug`, keyin `grep -c GEMINI_KEYS app/build/generated/source/buildConfig/debug/uz/hangulfriend/BuildConfig.java`. Kutilgan natija: 1. Kalit qiymatini ekranga chiqarmang.
- [ ] **Step 3: Commit** — `build: inject Gemini keys from .env into BuildConfig` (`.gitignore`dagi `.env` qatori bilan birga).

### Task 2: `GeminiClient`

**Files:** Create `app/src/main/java/uz/hangulfriend/ai/Gemini.kt`; Test `app/src/test/java/uz/hangulfriend/ai/GeminiClientTest.kt`

**Interfaces:**
- `data class HttpResult(val code: Int, val body: String)`
- `fun interface GeminiTransport { suspend fun post(model: String, key: String, body: String): HttpResult }` — `IOException` tashlashi mumkin. `HttpGeminiTransport` — haqiqiy implementatsiya.
- `data class ChatMessage(val fromUser: Boolean, val text: String)`
- `sealed interface AiResult`:
  - `data class Success(val text: String)`;
  - `data object QuotaExhausted`;
  - `data object Offline`;
  - `data object NotConfigured`;
  - `data class Failed(val message: String)`.
- `class GeminiClient(keys: List<String>, transport: GeminiTransport, models: List<String> = DEFAULT_MODELS)`:
  - `suspend fun generate(system: String, messages: List<ChatMessage>, json: Boolean = false): AiResult`;
  - ishlagan kalitning indeksi `@Volatile var` sifatida eslab qolinadi va keyingi chaqiruvlar shu kalitdan boshlanadi;
  - `DEFAULT_MODELS` Global Constraints'dagi ro'yxat.
- So'rov va javob `@Serializable` modellar bilan yoziladi. Javob matni `candidates[0].content.parts[*].text` qismlarini birlashtirib olinadi.

- [ ] **Step 1: Failing testlarni yozish** (soxta transport chaqiruvlarni `(model, key)` ro'yxatiga yozib boradi):
  - `generate_successReturnsText`;
  - `generate_quotaRotatesToNextKey` — `k1` uchun 429, `k2` uchun 200 → natija `Success`, chaqiruvlar `[(m1,k1),(m1,k2)]`;
  - `generate_unavailableTriesNextModelSameKey` — `m1` uchun 503 → `(m2,k1)`;
  - `generate_invalidKeyRotates` — 403;
  - `generate_allKeysExhausted` — hamma chaqiruv 429 → `QuotaExhausted`, jami 3 ta chaqiruv;
  - `generate_ioErrorIsOffline`;
  - `generate_noKeys` → `NotConfigured`, transport chaqirilmaydi;
  - `generate_stickyKey` — `k1` 429, `k2` 200; ikkinchi `generate` chaqiruvi `k2`dan boshlanadi;
  - `generate_emptyCandidatesFails`;
  - `generate_requestBodyShape` — tanada `systemInstruction`, `contents[].role` (`user` yoki `model`) va JSON rejimida `responseMimeType` bor.
- [ ] **Step 2: Testlarni ishga tushirish.** `./gradlew :app:testDebugUnitTest --tests "uz.hangulfriend.ai.*"`. Kutilgan natija: FAIL.
- [ ] **Step 3: Kodni yozish.**
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit** — `feat(ai): Gemini client with key and model fallback`.

### Task 3: `TutorPrompts` va `TutorService`

**Files:** Create `app/src/main/java/uz/hangulfriend/ai/Tutor.kt`; Test `app/src/test/java/uz/hangulfriend/ai/TutorTest.kt`

**Interfaces:**
- `data class TranslationVerdict(val correct: Boolean, val correctedKo: String, val explanationUz: String)`
- `object TutorPrompts`:
  - `const val ROLE` — o'qituvchi roli (o'zbek tilida so'zlashuvchi TOPIK I/2-daraja o'quvchisi uchun, javob faqat o'zbekcha, qisqa, koreyscha misollar tarjimasi bilan);
  - `fun grammarContext(g: Grammar, lesson: Lesson): String` — pattern, ma'no, qo'shilish qoidasi, misollar va dars so'zlari (`ko — uz`);
  - `fun mistakeQuestion(e: Exercise, userAnswer: String): String`;
  - `fun translationCheck(e: Exercise, userAnswer: String): String`.
- `fun parseVerdict(json: String): TranslationVerdict?` — kalitlar `correct`, `corrected_ko`, `explanation_uz`; buzilgan JSON uchun `null`.
- `class TutorService(client: GeminiClient)`:
  - `suspend fun ask(system: String, history: List<ChatMessage>): AiResult`;
  - `suspend fun checkTranslation(e: Exercise, userAnswer: String): TranslationVerdict?` — `json = true` bilan chaqiradi; har qanday xatoda `null` qaytaradi.

- [ ] **Step 1: Failing testlarni yozish.**
  - `grammarContext_containsPatternExamplesAndWords`;
  - `mistakeQuestion_containsAnswerAndCorrect`;
  - `parseVerdict_valid`;
  - `parseVerdict_malformedIsNull`;
  - `checkTranslation_usesJsonAndParses` — soxta transport JSON qaytaradi;
  - `checkTranslation_failureReturnsNull` — transport `IOException` tashlaydi.
- [ ] **Step 2: Testlarni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: Kodni yozish.**
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit** — `feat(ai): tutor prompts and translation verdicts`.

### Task 4: UI — chat oynasi, "Tushunmadim", "AI'dan so'rash", AI bilan tarjima tekshiruvi

**Files:**
- Create: `ui/tutor/TutorSheet.kt`
- Modify: `AppContainer.kt`, `MainActivity.kt` (`LocalTutor`), `ui/lesson/Stages.kt` (grammatika sahifasi), `ui/exercise/Parts.kt` (`FeedbackPanel`), `ui/exercise/TypingView.kt`, `ui/exercise/ExerciseView.kt`, `res/values/strings.xml`
- Test: `app/src/test/java/uz/hangulfriend/ui/TutorTextTest.kt`

**Interfaces:**
- `val LocalTutor = staticCompositionLocalOf<TutorService?> { null }`
- `fun aiStatusText(r: AiResult): Int?` — `Success` uchun `null`, qolgan holatlar uchun alohida string resurs.
- `@Composable fun TutorSheet(system: String, firstQuestion: String, onDismiss: () -> Unit)`:
  - ochilganda `firstQuestion` avtomatik yuboriladi;
  - xabarlar ro'yxati (AI javoblari Markdown bilan), matn maydoni va "Yuborish" tugmasi;
  - kutish paytida progress ko'rsatiladi;
  - xato bo'lsa `aiStatusText` matni chiqadi.
- Grammatika sahifasida "🤖 Tushunmadim" tugmasi `TutorSheet(grammarContext, "Shu grammatikani sodda qilib tushuntiring")` ni ochadi.
- `FeedbackPanel` ga ixtiyoriy `ask: (() -> Unit)?` parametri qo'shiladi. Javob noto'g'ri bo'lsa, "🤖 AI'dan so'rash" tugmasi chiqadi. `TypingView`, `ChoiceView` va `BuildSentenceView` shu tugma orqali `TutorSheet(mistakeQuestion)` ni ochadi.
- `TypingView` ichidagi `translate` rejimi (`aiCheck` parametri):
  - `AnswerChecker` natijasi to'g'ri bo'lmasa va `LocalTutor` mavjud bo'lsa, `checkTranslation` chaqiriladi;
  - kutish paytida "AI tekshiryapti…" ko'rsatiladi;
  - `verdict.correct == true` bo'lsa, javob to'g'ri hisoblanadi va `explanationUz` ko'rsatiladi;
  - verdict noto'g'ri bo'lsa, `correctedKo` to'g'ri javob sifatida ko'rsatiladi;
  - `null` bo'lsa, eski tekshiruv natijasi qoladi.

- [ ] **Step 1: Failing test yozish.** `aiStatusText_coversNonSuccess` — `Success` uchun `null`; qolgan 4 holat uchun turli, 0 dan farqli ID'lar.
- [ ] **Step 2: Testni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: UI'ni yozish.**
- [ ] **Step 4: Test va qurish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit** — `feat(ui): AI tutor chat, ask-AI on mistakes, AI translation check`.

### Task 5: Qurilmada tekshiruv va PR

- [ ] **Step 1: To'liq testlar va build.**
- [ ] **Step 2: Telefonga o'rnatish.** `adb install -r`.
- [ ] **Step 3: Haqiqiy chaqiruvni tekshirish.** Grammatika sahifasida "Tushunmadim" tugmasi o'zbekcha javob qaytaradi. Kalit qiymatlari logcat'ga chiqmasligi tekshiriladi.
- [ ] **Step 4: Branch'ni push qilish va PR ochish.**
