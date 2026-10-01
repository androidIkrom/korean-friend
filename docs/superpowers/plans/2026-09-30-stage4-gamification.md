# Hangul Friend — 4-bosqich (O'yin elementlari) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (subagentsiz). Steps use checkbox (`- [ ]`) syntax.

**Goal:** Spec §3.5 va §7 dagi 4-bosqichni amalga oshirish: XP, daraja, streak, kunlik maqsad halqasi, combo, nishonlar, 4 ta mini o'yin, Boss jangi, xatolar daftari va tezkor tekshiruv.

**Architecture:**
- Sof qoidalar `study/GameRules.kt` faylida.
- O'yin ma'lumotlari Room'ning yangi versiyasida saqlanadi (v1 → v2 migratsiyasi) va `GameRepository` orqali o'qiladi.
- XP va nishonlar `SessionController` ichida beriladi.
- Yangi sessiya rejimlari: `boss`, `quickCheck`, `mistakes`.
- Mini o'yinlarning mantig'i sof klasslarda, ko'rinishi Compose ekranlarida.

**Spec:** `docs/superpowers/specs/2026-09-29-hangul-friend-design.md` §3.2, §3.3 (Tezkor tekshiruv, Boss), §3.5, §5 (Room migratsiyasi).

## Global Constraints

- Oldingi bosqichlarning barcha cheklovlari amal qiladi: o'zbekcha UI, commit'larda `Co-Authored-By` yo'q, worktree yo'q, subagent yo'q.
- **XP qoidalari (spec §3.5):**
  - to'g'ri javob — **10**;
  - combo bonusi — ketma-ket 3-to'g'ri javobdan boshlab har bir to'g'ri javobga **+5**;
  - dars birinchi marta tugatilganda — **100**;
  - Boss g'alabasi — **200**;
  - kunlik maqsad bajarilganda — **50** (kuniga bir marta);
  - mini o'yinda har bir to'g'ri javob — **5**.
- **Kunlik maqsad:** standart qiymati **50 XP**, sozlamalarda 20–200 oralig'ida, qadam 10.
- **Daraja formulasi:** daraja `L` uchun jami kerakli XP `50·L·(L−1)`. Masalan, 1-daraja 0 XP, 2-daraja 100 XP, 3-daraja 300 XP, 4-daraja 600 XP.
- **Streak:** kunlik maqsad bajarilgan ketma-ket mahalliy kunlar soni. Hisob bugundan boshlanadi; bugun maqsad hali bajarilmagan bo'lsa, kechadan boshlanadi.
- **Nishonlar (ID'lar):**
  - `first_lesson` — birinchi dars tugatildi;
  - `first_unit` — bir bo'limning ikkala darsi tugatildi;
  - `words_100`, `words_500` — tanish kartasi (`#R`) `REVIEW` holatiga yetgan so'zlar soni;
  - `streak_7`, `streak_30`;
  - `first_boss` — birinchi Boss g'alabasi.
- **Boss jangi:** bo'limning ikkala darsidan 15 ta authored mashq (avval test ro'yxatidagilari), 3 ta jon. Jonlar tugasa, jang yutqazilgan hisoblanadi.
- **Tezkor tekshiruv:** 10 ta mashq. 80% va undan yuqori natijada dars holati `VERIFIED` ga o'tadi (faqat `PASSED` yoki `NOT_STARTED` holatidan).
- **Xatolar daftari:** oxirgi 30 kunda eng ko'p `AGAIN` bahosi olgan 20 ta karta.
- **Room:** versiya 2, faqat qo'lda yozilgan va testlangan `MIGRATION_1_2` orqali. Destructive fallback yo'q.

## Review Focus

1. **Eski ma'lumotli foydalanuvchi yangilanishi.** Mavjud v1 bazasidagi kartalar va progress migratsiyadan keyin saqlanib qoladi. Test: `migrate1to2_keepsCards`.
2. **Kun chegarasi.** Streak va "bugungi XP" mahalliy vaqt bo'yicha hisoblanadi: 23:59 va 00:01 har xil kun. Test: `streak_localDays`.
3. **Kunlik maqsad bonusi takrorlanmasligi.** Bir kunda +50 faqat bir marta beriladi, maqsaddan keyingi XP'lar uni qayta bermaydi. Test: `dailyGoalBonusOncePerDay`.
4. **Dars qayta tugatilishi.** Tugatilgan darsning testini qayta o'tish +100 ni yana bermaydi. Test: `lessonCompleteXpOnlyFirstTime`.
5. **Kontenti bor darsi bo'lmagan bo'lim.** Boss tugmasi faqat ikkala darsning kontenti bor bo'lsa ko'rinadi. `SessionBuilder.boss` bo'sh ro'yxat bilan ham qulamaydi. Test: `boss_missingLessonGivesEmpty`.

---

### Task 1: `GameRules` — sof qoidalar

**Files:** Create `app/src/main/java/uz/hangulfriend/study/GameRules.kt`; Test `study/GameRulesTest.kt`

**Interfaces:**
- `object GameRules`:
  - `fun xpForAnswer(correct: Boolean, comboAfter: Int): Int` — to'g'ri javob uchun 10, `comboAfter >= 3` bo'lsa +5; noto'g'ri javob uchun 0.
  - `data class LevelInfo(val level: Int, val xpIntoLevel: Int, val xpForNext: Int)`
  - `fun level(totalXp: Int): LevelInfo` — daraja formulasi bo'yicha.
  - `fun streak(dailyXp: Map<LocalDate, Int>, today: LocalDate, goal: Int): Int`
  - `data class Stats(val completedLessons: Set<String>, val unitsCompleted: Int, val wordsLearned: Int, val streak: Int, val bossWins: Int)`
  - `fun achievements(s: Stats): Set<String>` — Global Constraints'dagi ID'lar.
- Konstantalar:
  - `XP_CORRECT = 10`, `XP_COMBO = 5`, `COMBO_FROM = 3`;
  - `XP_LESSON = 100`, `XP_BOSS = 200`, `XP_GOAL = 50`, `XP_GAME = 5`;
  - `DEFAULT_GOAL = 50`.

- [ ] **Step 1: Failing testlarni yozish.**
  - `xpForAnswer_comboFromThird` — `(true,1)=10`, `(true,3)=15`, `(false,5)=0`.
  - `level_thresholds` — XP 0 → 1-daraja (0/100), 99 → 1, 100 → 2 (0/200), 299 → 2, 300 → 3.
  - `streak_countsBackFromTodayOrYesterday`.
  - `streak_brokenByMissedDay`.
  - `streak_localDays` — sanalar `LocalDate` sifatida beriladi; bugun maqsad bajarilmagan bo'lsa, hisob kechadan boshlanadi.
  - `achievements_thresholds` — `words 99 → words_100 yo'q`, `100 → bor`, `streak 7 → streak_7`, `bossWins 1 → first_boss`.
- [ ] **Step 2: Testlarni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: Kodni yozish.**
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit** — `feat(game): XP, level, streak and achievement rules`.

### Task 2: Ma'lumotlar — Room v2, `GameRepository`, progress hodisalari

**Files:**
- Modify: `data/Entities.kt`, `data/Daos.kt`, `data/AppDatabase.kt` (version 2, `MIGRATION_1_2`), `data/ProgressRepository.kt`, `data/SettingsRepository.kt` (`dailyGoalXp`), `app/build.gradle.kts` (`sourceSets["test"].assets.srcDir("$projectDir/schemas")`, `androidx.room:room-testing`)
- Create: `data/GameRepository.kt`
- Test: `data/MigrationTest.kt`, `data/GameRepositoryTest.kt`, `data/ProgressRepositoryTest.kt` ga qo'shimcha

**Interfaces:**
- Yangi entity'lar:
  - `XpEventEntity(@PrimaryKey(autoGenerate) id, amount: Int, reason: String, atMs: Long)` — jadval `xp_events`;
  - `AchievementEntity(@PrimaryKey id: String, unlockedMs: Long)` — jadval `achievements`;
  - `BestScoreEntity(@PrimaryKey gameId: String, score: Int)` — jadval `best_scores`.
- `MIGRATION_1_2` uchta jadvalni `CREATE TABLE` bilan yaratadi. SQL Room eksport qilgan `2.json` sxemasi bilan aynan mos bo'lishi kerak.
- `class GameRepository(db: AppDatabase, clock: Clock)`:
  - `suspend fun award(amount: Int, reason: String, goal: Int): Int` — XP yozadi. Agar bu yozuv bugungi XP'ni birinchi marta `goal` dan oshirsa, qo'shimcha `XP_GOAL` (`reason = "goal"`) yozadi. Qaytaradi: jami qo'shilgan XP.
  - `fun observeTotalXp(): Flow<Int>`
  - `suspend fun todayXp(): Int`
  - `suspend fun dailyXp(days: Int = 60): Map<LocalDate, Int>` — mahalliy sanalar bo'yicha, `goal` bonusi ham hisobga olinadi.
  - `suspend fun unlockNew(ids: Set<String>): List<String>` — faqat yangi ochilgan nishonlarni qaytaradi.
  - `fun observeAchievements(): Flow<List<AchievementEntity>>`
  - `suspend fun best(gameId: String): Int`
  - `suspend fun submitScore(gameId: String, score: Int): Boolean` — yangi rekord bo'lsa `true`.
  - `suspend fun stats(): GameRules.Stats` — progress, kartalar va XP ma'lumotlaridan yig'iladi.
  - `suspend fun mistakes(limit: Int = 20): List<CardEntity>` — `review_logs` jadvalidan oxirgi 30 kundagi `rating = 1` sonlari bo'yicha, kamayish tartibida.
- `ProgressRepository.recordTest(...)` endi `Boolean` qaytaradi — dars shu chaqiruvda `COMPLETED` ga o'tgan bo'lsa `true`.
- `ProgressRepository.recordQuickCheck(lessonId: String, scorePercent: Int): Boolean` — natija 80% va undan yuqori bo'lsa va holat `PASSED` yoki `NOT_STARTED` bo'lsa, `VERIFIED` ga o'tkazadi.
- `Settings.dailyGoalXp: Int = GameRules.DEFAULT_GOAL` va `setDailyGoalXp(n)`.

- [ ] **Step 1: Failing testlarni yozish.**
  - `migrate1to2_keepsCards` — `MigrationTestHelper` bilan v1 bazaga bitta karta yoziladi, migratsiyadan keyin karta joyida va `xp_events` jadvali mavjud.
  - `award_writesEvent`.
  - `dailyGoalBonusOncePerDay` — 40 + 20 → bonus beriladi (jami 110); keyin +10 → bonus qayta berilmaydi.
  - `dailyXp_groupsByLocalDate`.
  - `unlockNew_returnsOnlyNew`.
  - `submitScore_recordOnlyWhenHigher`.
  - `mistakes_orderedByAgainCount`.
  - `lessonCompleteXpOnlyFirstTime` — `recordTest` ikkinchi marta `false` qaytaradi.
  - `recordQuickCheck_verifiesPassed`.
- [ ] **Step 2: Testlarni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: Kodni yozish.** `ksp` build `app/schemas/.../2.json` faylini yaratadi, u commit qilinadi.
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit** — `feat(data): room v2 with xp, achievements, best scores`.

### Task 3: Sessiya — XP, combo, Boss, tezkor tekshiruv, xatolar

**Files:**
- Modify: `study/SessionBuilder.kt`, `ui/session/SessionController.kt`, `ui/Nav.kt`, `AppContainer.kt`
- Test: `study/SessionBuilderTest.kt`, `ui/SessionControllerTest.kt`

**Interfaces:**
- `SessionMode` ga yangi rejimlar: `BOSS("boss")` (`lessonId` o'rniga bo'lim raqami, masalan `"2"`), `QUICK_CHECK("quickCheck")`, `MISTAKES("mistakes")`.
- `SessionBuilder` ga yangi metodlar:
  - `fun boss(lessons: List<Lesson>): List<ExerciseItem>` — `lessons` 2 ta bo'lmasa bo'sh ro'yxat qaytaradi;
  - `fun quickCheck(lesson: Lesson): List<ExerciseItem>` — test ro'yxatidan 10 ta, yetmasa amaliy mashqlardan to'ldiriladi;
  - `fun mistakes(cards, lessons)` = `review(cards, lessons)`.
- `SessionState` ga yangi maydonlar: `combo: Int`, `xpEarned: Int`, `hearts: Int?` (faqat Boss uchun, boshlang'ich qiymati 3), `failed: Boolean`, `newAchievements: List<String>`.
- `SessionController` konstruktoriga `game: GameRepository` qo'shiladi (`settings` orqali `dailyGoalXp` olinadi).
- **Har bir to'g'ri yoki noto'g'ri javobda:**
  - combo yangilanadi (noto'g'ri javobda 0);
  - `xpForAnswer` bo'yicha `game.award` chaqiriladi;
  - Boss'da noto'g'ri javob bitta jonni oladi; jon 0 bo'lsa, `failed = true` va sessiya tugaydi.
- **Sessiya tugaganda:**
  - `TEST` rejimida: `recordTest` `true` qaytarsa, +100 beriladi;
  - `QUICK_CHECK` rejimida: `recordQuickCheck` chaqiriladi;
  - `BOSS` rejimida: g'alaba bo'lsa +200 beriladi va `bossWins` oshadi (`reason = "boss"` bo'lgan XP hodisalari soni orqali);
  - so'ng `game.unlockNew(GameRules.achievements(game.stats()))` natijasi `newAchievements` ga yoziladi.

- [ ] **Step 1: Failing testlarni yozish.**
  - `boss_fifteenFromBothLessons`;
  - `boss_missingLessonGivesEmpty`;
  - `quickCheck_tenItems`;
  - `session_awardsXpAndCombo` — 4 ta to'g'ri javob → XP 10+10+15+15;
  - `session_wrongResetsCombo`;
  - `session_bossLosesAfterThreeWrong` — `failed == true`, sessiya tugagan;
  - `session_bossWinAwards200AndAchievement`;
  - `session_testCompletionAwardsLessonXpOnce`;
  - `session_quickCheckVerifies`.
- [ ] **Step 2: Testlarni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: Kodni yozish.**
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit** — `feat(study): XP, combo, boss, quick check and mistakes sessions`.

### Task 4: Mini o'yinlar mantig'i

**Files:** Create `study/games/MemoryGame.kt`, `study/games/SpeedRound.kt`, `study/games/GrammarChain.kt`; Test `study/games/GamesTest.kt`

**Interfaces:**
- `class MemoryGame(words: List<Word>, random: Random)`:
  - `cards: List<MemoryCard>` — 6 juft (12 ta karta), har bir juft `ko` va `uz` kartadan iborat;
  - `fun flip(index: Int): FlipResult` — natija: `FIRST`, `MATCH`, `MISMATCH` yoki `IGNORED`;
  - `fun hideMismatch()`;
  - `val done: Boolean`, `val moves: Int`.
- `class SpeedRound(words: List<Word>, random: Random)`:
  - `fun next(): SpeedQuestion(word, options: List<String>)` — 4 ta o'zbekcha variant;
  - `fun answer(choice: String): Boolean`;
  - `val score: Int`;
  - davomiyligi `DURATION_MS = 60_000` (vaqtni UI boshqaradi).
- `class GrammarChain(exercises: List<Exercise>, random: Random)` — faqat `CONJUGATE` mashqlaridan tuziladi:
  - `fun current(): Exercise?`;
  - `fun submit(input: String): Boolean` — javob `AnswerChecker` bilan tekshiriladi, xato javob zanjirni tugatadi;
  - `val length: Int`, `val over: Boolean`.
- **Hangul poygasi:** `SpeedRound` bilan bir xil vaqtli tsikl, lekin javob klaviaturada yoziladi (`AnswerChecker`). Alohida mantiq klassi kerak emas: UI `SpeedRound.next()` dagi so'zdan foydalanadi.

- [ ] **Step 1: Failing testlarni yozish.**
  - `memory_hasSixPairs`;
  - `memory_matchAndMismatch`;
  - `memory_doneWhenAllMatched`;
  - `speed_optionsContainAnswer`;
  - `speed_scoreCountsCorrect`;
  - `chain_onlyConjugate`;
  - `chain_endsOnFirstMistake`.
- [ ] **Step 2: Testlarni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: Kodni yozish.**
- [ ] **Step 4: Testlarni qayta ishga tushirish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit** — `feat(game): memory, speed round and grammar chain logic`.

### Task 5: UI — Bugun ekrani, sessiya effektlari, o'yinlar, nishonlar, xatolar daftari

**Files:**
- Modify: `ui/home/Home.kt`, `ui/session/SessionScreen.kt`, `ui/map/BookMap.kt`, `ui/settings/Settings.kt`, `ui/Nav.kt`, `res/values/strings.xml`
- Create: `ui/games/GamesScreen.kt` (4 o'yin ro'yxati va rekordlar), `ui/games/MemoryScreen.kt`, `ui/games/SpeedScreen.kt` (tezlik raundi va Hangul poygasi, `typing: Boolean` parametri bilan), `ui/games/ChainScreen.kt`, `ui/achievements/AchievementsScreen.kt`, `ui/mistakes/MistakesScreen.kt`
- Test: `ui/GameTextTest.kt`

**Interfaces:**
- `fun achievementTitle(id: String): Int` — har bir nishon ID'si uchun string resurs.
- **Bugun ekrani:**
  - daraja va XP chizig'i;
  - kunlik maqsad halqasi (`CircularProgressIndicator`, bugungi XP / maqsad);
  - "🔥 N kun" streak;
  - "O'yinlar", "Xatolar daftari" va "Nishonlar" tugmalari.
- **Sessiya ekrani:**
  - combo 3 va undan yuqori bo'lsa "🔥 N ketma-ket!" belgisi;
  - Boss'da jonlar "❤️❤️🤍" ko'rinishida;
  - natija ekranida "+N XP", yangi nishonlar ro'yxati, Boss'da g'alaba yoki mag'lubiyat.
- **Kitob xaritasi:**
  - `PASSED` holatidagi darsda "Tezkor tekshiruv" tugmasi;
  - ikkala darsining kontenti bor bo'limda "⚔️ Boss jangi" tugmasi.
- **Sozlamalar:** kunlik maqsad slayderi, 20–200 XP, qadam 10.
- **Yangi marshrutlar:** `games`, `game/{id}` (`memory`, `speed`, `race`, `chain`), `achievements`, `mistakes`.
- **O'yin natijasi:** mini o'yin tugaganda `game.award(score * XP_GAME)` chaqiriladi va `submitScore` bilan rekord saqlanadi.

- [ ] **Step 1: Failing test yozish.** `achievementTitle_coversAll` — barcha nishon ID'lari uchun 0 dan farqli, har xil resurslar.
- [ ] **Step 2: Testni ishga tushirish.** Kutilgan natija: FAIL.
- [ ] **Step 3: UI'ni yozish.**
- [ ] **Step 4: Test va qurish.** Kutilgan natija: PASS.
- [ ] **Step 5: Commit** — `feat(ui): gamified home, session effects, games, achievements, mistakes`.

### Task 6: Tekshiruv va PR

- [ ] **Step 1: To'liq testlar va build.**
- [ ] **Step 2: Telefonga o'rnatish** (ulangan bo'lsa). `adb install -r`, eski progress migratsiyadan keyin saqlanganini tekshirish.
- [ ] **Step 3: Push va PR.**
