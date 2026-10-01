# Stage 5 — Story, Reminders, Backup, Share Card Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add interactive story episodes, a daily reminder, progress export/import and a shareable progress image to Hangul Friend.

**Architecture:** Pure Kotlin logic (story player state machine, reminder policy, backup codec, progress stats) lives beside the existing `study`/`data` code and is unit-tested on the JVM; Android glue (WorkManager worker, SAF launchers, Canvas renderer, FileProvider) stays thin. Story content goes in each lesson JSON's `story` field; story completion is a new Room table (DB v3).

**Tech Stack:** Kotlin 2.4.20, Compose Material 3, Room (KSP) with `MigrationTestHelper` under Robolectric, DataStore, kotlinx.serialization, WorkManager (new dependency), edge-tts (`tools/audio_gen.py`).

**Spec:** `docs/superpowers/specs/2026-10-01-stage5-story-reminders-backup-share-design.md`

## Global Constraints

- Package `uz.hangulfriend`; minSdk 26, compileSdk/targetSdk 37; manual DI in `AppContainer`.
- UI copy is Uzbek, in `app/src/main/res/values/strings.xml`; apostrophes escaped as `\'`.
- Room: no destructive fallback; every schema change has a migration plus a `MigrationTest` case; schemas export to `app/schemas` (already wired into debug assets).
- "Instrumented" tests from the spec run as Robolectric JVM tests (`@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])`), like the existing `MigrationTest`.
- XP reasons are strings on `GameRepository` (`REASON_*`); story XP uses a new `REASON_STORY = "story"`, `GameRules.XP_STORY = 30`.
- "Learned words" on the share card = `CardDao.countLearnedWords()` (same number the achievements use).
- Story step limits: 10–16 `line` steps; 2–3 interactive steps (`choose_reply` + `quiz`), at least one `choose_reply`; first step is a `line`; exactly 3 distinct options containing `answer`.
- Reminder defaults: off, 20:00 (`reminderMinutes = 1200`). Unique work name `daily_reminder`, channel id `reminders`.
- Backup: `format = "hangul-friend-backup"`, `version = 1`; file name `hangul-friend-YYYY-MM-DD.json`; MIME `application/json`.
- Share image: 1080×1350 PNG at `cacheDir/share/progress.png`; FileProvider authority `uz.hangulfriend.fileprovider`.
- Git: branch `feat/stage5` (already created), no worktrees, no subagents, no `Co-Authored-By` lines; the user merges PRs.

## Review Focus

1. **Wrong-answer exhaustion in a story choice** — after the learner taps both wrong options, only the answer remains and the episode can still continue. Test in Task 4 (`StoryPlayerTest.bothWrongOptionsEliminatedThenAnswerAdvances`).
2. **Replaying a finished episode** — it must play again but never award XP twice. Test in Task 3 (`StoryRepositoryTest.secondCompletionAwardsNothing`).
3. **Reminder time already passed today, or exactly now** — the next run is tomorrow at that time, never a zero or negative delay. Test in Task 6 (`ReminderPolicyTest.delayWhenTimePassedIsTomorrow`, `delayWhenExactlyNowIsTomorrow`).
4. **A backup file from a future app version or with extra keys** — newer `version` is rejected with nothing changed; unknown extra keys are tolerated. Tests in Task 7 (`BackupCodecTest.rejectsNewerVersion`, `acceptsUnknownKeys`).
5. **Share card for a brand-new learner** (no cards, no XP, no progress) — stats are all zero and every lesson is NOT_STARTED, no crash. Test in Task 8 (`ProgressStatsTest.emptyDatabaseGivesZeros`).

---

### Task 1: Story model and validation

**Files:**
- Modify: `app/src/main/java/uz/hangulfriend/content/Model.kt` (replace `Story`)
- Modify: `app/src/main/java/uz/hangulfriend/content/LessonValidator.kt`
- Modify: `app/src/test/java/uz/hangulfriend/content/LessonValidatorTest.kt`
- Modify: `app/src/test/java/uz/hangulfriend/content/ContentAssetsTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class Story(@SerialName("title_uz") val titleUz: String, val steps: List<StoryStep>)`
  - `@Serializable @JsonClassDiscriminator("type") sealed interface StoryStep` with
    - `@SerialName("line") data class StoryLine(val speaker: String, val ko: String, val uz: String, val audio: String? = null) : StoryStep`
    - `@SerialName("choose_reply") data class ChooseReply(val speaker: String, @SerialName("prompt_uz") val promptUz: String, val options: List<String>, val answer: String, val uz: String, @SerialName("why_uz") val whyUz: String, val audio: String? = null) : StoryStep`
    - `@SerialName("quiz") data class StoryQuiz(@SerialName("prompt_uz") val promptUz: String, val options: List<String>, val answer: String, @SerialName("why_uz") val whyUz: String) : StoryStep`
  - `ContentAssetsTest.audioNames` includes `StoryLine.audio` and `ChooseReply.audio`; speakers of `StoryLine`/`ChooseReply` are checked against `characters.json`.

- [ ] **Step 1: Write failing validator tests** in `LessonValidatorTest` (build a valid story fixture with 10 lines + 1 choose_reply + 1 quiz, then mutate):
  - `storyValidPasses` → `validate(...)` returns no story errors.
  - `storyTooFewLines` (9 lines) → error contains `"story: 10–16 line steps"`.
  - `storyNoChooseReply` (2 quizzes) → error contains `"story: needs a choose_reply"`.
  - `storyTooManyInteractive` (4 interactive) → error contains `"story: 2–3 interactive steps"`.
  - `storyFirstStepMustBeLine` → error contains `"story: first step must be a line"`.
  - `storyAnswerNotInOptions` → error contains `"answer must be one of 3 distinct options"`.
  - `storyParsesFromJson` → `ContentJson.decodeFromString<Story>` of a JSON with one step of each `type` yields `StoryLine`, `ChooseReply`, `StoryQuiz` in order.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests "*LessonValidatorTest*"` — expected FAIL (compile errors: `StoryStep` missing).
- [ ] **Step 3: Implement** the model above and `LessonValidator` story checks (errors prefixed `"${lesson.id}: story: ..."`, only when `lesson.story != null`). `Story` needs `@OptIn(ExperimentalSerializationApi::class)` for `@JsonClassDiscriminator`.
- [ ] **Step 4: Extend `ContentAssetsTest`**: story speakers exist; story audio counted in `audioNames`.
- [ ] **Step 5: Run** `./gradlew :app:testDebugUnitTest` — expected PASS (no lesson has `story` yet).
- [ ] **Step 6: Commit** `feat(story): story step model and validation`.

### Task 2: Audio generator for story steps

**Files:**
- Modify: `tools/audio_gen.py` (`collect`)
- Modify: `tools/test_audio_gen.py`

**Interfaces:**
- Consumes: Task 1 JSON shape (`story.steps[].type`).
- Produces: `collect()` yields a clip for every `line.ko` and every `choose_reply.answer` (voice = `voices[speaker]`), writing the file name into that step's `audio`; `quiz` steps yield nothing.

- [ ] **Step 1: Write failing test** `test_collect_story_steps`: a lesson with steps `[line(minji), choose_reply(aziz, answer "네"), quiz]` → collected texts include the line's `ko` with the female voice and `"네"` with the male voice; exactly 2 story clips.
- [ ] **Step 2: Run** `python -m pytest tools/test_audio_gen.py -q` — expected FAIL.
- [ ] **Step 3: Implement**: replace the `story.lines` loop with a `story.steps` loop (`choose_reply` uses `_clip(step, "answer", "audio", voice)`).
- [ ] **Step 4: Run** the tests — expected PASS.
- [ ] **Step 5: Commit** `feat(story): generate audio for story steps`.

### Task 3: Story progress storage (DB v3)

**Files:**
- Modify: `app/src/main/java/uz/hangulfriend/data/Entities.kt`, `Daos.kt`, `AppDatabase.kt`
- Create: `app/src/main/java/uz/hangulfriend/data/StoryRepository.kt`
- Modify: `app/src/main/java/uz/hangulfriend/data/GameRepository.kt` (`REASON_STORY`), `study/GameRules.kt` (`XP_STORY = 30`), `AppContainer.kt`
- Create: `app/schemas/uz.hangulfriend.data.AppDatabase/3.json` (generated by KSP on build; commit it)
- Modify: `app/src/test/java/uz/hangulfriend/data/MigrationTest.kt`
- Create: `app/src/test/java/uz/hangulfriend/data/StoryRepositoryTest.kt`

**Interfaces:**
- Produces:
  - `@Entity(tableName = "story_progress") data class StoryProgressEntity(@PrimaryKey val lessonId: String, val completedAtMs: Long)`
  - `@Dao interface StoryDao { @Insert(onConflict = IGNORE) suspend fun insertIgnore(p: StoryProgressEntity): Long; @Query("SELECT * FROM story_progress") fun observeAll(): Flow<List<StoryProgressEntity>>; @Query("SELECT * FROM story_progress") suspend fun all(): List<StoryProgressEntity> }`, `AppDatabase.story(): StoryDao`
  - `val MIGRATION_2_3` (creates `story_progress`), DB `version = 3`, `open()` adds it.
  - `enum class EpisodeState { LOCKED, COMING_SOON, NEW, DONE }`
  - `object StoryRules { fun state(status: LessonStatus?, hasStory: Boolean, done: Boolean): EpisodeState }` — `!hasStory` → COMING_SOON; status in {PASSED, COMPLETED, VERIFIED} → NEW/DONE; otherwise LOCKED.
  - `class StoryRepository(db: AppDatabase, game: GameRepository, clock: Clock) { fun observeDone(): Flow<Set<String>>; suspend fun complete(lessonId: String, goal: Int): Int }` — returns XP added (0 when already done).

- [ ] **Step 1: Write failing tests**:
  - `MigrationTest.migrate2to3_addsStoryProgress`: create v2 with a card row, migrate to 3 with `MIGRATION_2_3`, assert card count 1 and `SELECT COUNT(*) FROM story_progress` = 0.
  - `StoryRepositoryTest` (Robolectric, in-memory Room, `TestClock`): `firstCompletionAwards30Xp` (returns ≥ 30, `observeDone()` contains the id); `secondCompletionAwardsNothing` (second call returns 0, total XP unchanged).
  - `StoryRulesTest` (plain JVM): LOCKED for `null`/NOT_STARTED/IN_PROGRESS; NEW for PASSED/COMPLETED/VERIFIED when not done; DONE when done; COMING_SOON when `hasStory = false` regardless of status.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests "*Story*" --tests "*MigrationTest*"` — expected FAIL.
- [ ] **Step 3: Implement** entity, DAO, migration (SQL mirrors generated `3.json`), `StoryRules`, `StoryRepository.complete` (insertIgnore; if inserted, `game.award(GameRules.XP_STORY, REASON_STORY, goal)`, else 0), wire `val story = StoryRepository(db, game, clock)` in `AppContainer`.
- [ ] **Step 4: Run** the same tests — expected PASS; confirm `app/schemas/.../3.json` exists.
- [ ] **Step 5: Commit** `feat(story): story progress table and repository`.

### Task 4: Story player and screens

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/story/StoryPlayer.kt`
- Create: `app/src/main/java/uz/hangulfriend/ui/story/StoryList.kt`, `ui/story/Episode.kt`
- Modify: `app/src/main/java/uz/hangulfriend/ui/Nav.kt`, `ui/home/Home.kt`, `res/values/strings.xml`
- Create: `app/src/test/java/uz/hangulfriend/story/StoryPlayerTest.kt`

**Interfaces:**
- Consumes: Task 1 `Story`/`StoryStep`, Task 3 `StoryRepository`, `StoryRules`, `EpisodeState`.
- Produces:
  - `class StoryPlayer(story: Story)` — immutable-state machine:
    - `val shown: List<StoryStep>` (steps revealed so far; a solved `ChooseReply` stays in the list and is rendered as Aziz's line)
    - `val current: StoryStep?` (the step awaiting input or "next"; `null` when finished)
    - `val eliminated: Set<String>` (wrong options removed for the current interactive step)
    - `val lastWrongWhy: String?`
    - `val finished: Boolean`
    - `fun next(): StoryPlayer` (only valid when `current` is a `StoryLine`)
    - `fun choose(option: String): StoryPlayer` (wrong → adds to `eliminated`, sets `lastWrongWhy`; right → clears both and advances)
    - `val progress: Float` (revealed / total)
  - Routes `Routes.STORIES = "stories"`, `Routes.EPISODE = "story/{lessonId}"`, `fun episode(id: String)`.
  - `HomeScreen(..., onStories: () -> Unit)` — new entry next to Games/Mistakes/Achievements.

- [ ] **Step 1: Write failing `StoryPlayerTest`**: `startsOnFirstLine`; `nextRevealsFollowingStep`; `wrongChoiceEliminatesAndExplains` (eliminated = {wrong}, `lastWrongWhy` = `whyUz`, still on same step); `rightChoiceAdvances`; `bothWrongOptionsEliminatedThenAnswerAdvances`; `finishedAfterLastStep` (`finished`, `current == null`, `progress == 1f`).
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests "*StoryPlayerTest*"` — expected FAIL.
- [ ] **Step 3: Implement `StoryPlayer`.**
- [ ] **Step 4: Run** — expected PASS.
- [ ] **Step 5: Build the screens**:
  - `StoryListScreen` / `StoryListViewModel(content, progress, story)`: 9 units × 2 rows with title_uz, `EpisodeState` badge (lock icon + "Avval darsni o'ting", "Tez orada", "Yangi", "✓"); tapping NEW/DONE opens the episode.
  - `EpisodeScreen` / `EpisodeViewModel(lessonId, content, story, settings)`: chat bubbles for `shown`; line bubbles play audio via `LocalAudioPlayer` when revealed and have a translation toggle; interactive step shows `promptUz` and option buttons (eliminated ones disabled) with `lastWrongWhy` under them; "Keyingi" button for lines; top `LinearProgressIndicator(progress)`; on `finished` call `story.complete(lessonId, goal)` once and show a result card with the XP returned ("+30 XP" or "Qayta o'tildi").
  - Nav entries and Home button; strings in `strings.xml`.
- [ ] **Step 6: Run** `./gradlew :app:testDebugUnitTest :app:assembleDebug` — expected BUILD SUCCESSFUL.
- [ ] **Step 7: Commit** `feat(story): story list and interactive episode screen`.

### Task 5: First two episodes (unit 2)

**Files:**
- Modify: `app/src/main/assets/lessons/u02_l1.json`, `u02_l2.json` (add `story`)
- Generated: new MP3s in `app/src/main/assets/audio/`

**Interfaces:**
- Consumes: Task 1 format and limits, Task 2 generator.

- [ ] **Step 1: Write the episodes.** u02_l1: Aziz shops for clothes with Minji (uses -아/어 보세요, ㅡ 탈락, unit 1 honorifics); u02_l2: Aziz compares sizes/colours with the seller (A-(으)ㄴ N, ㄹ 탈락, N보다 더). Each: 12–14 lines, 1 `choose_reply` + 1 `quiz` (or 2 + 1), only vocabulary/grammar from units 1–2 (book order).
- [ ] **Step 2: Run** `python tools/audio_gen.py` then `python tools/validate_content.py` — expected `BUILD SUCCESSFUL`, `ContentAssetsTest` green.
- [ ] **Step 3: Install and play both episodes on the phone** (`adb install -r app/build/outputs/apk/debug/app-debug.apk`); check audio voices, wrong/right choices and +30 XP once.
- [ ] **Step 4: Commit** `content(story): unit 2 episodes with audio`.

### Task 6: Daily reminder

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts` (add `androidx.work:work-runtime-ktx`, latest stable from `https://dl.google.com/android/maven2/androidx/work/work-runtime-ktx/maven-metadata.xml`)
- Modify: `app/src/main/java/uz/hangulfriend/data/SettingsRepository.kt`
- Create: `app/src/main/java/uz/hangulfriend/reminders/ReminderPolicy.kt`, `ReminderWorker.kt`, `ReminderScheduler.kt`
- Modify: `AppContainer.kt`, `HangulFriendApp.kt`, `MainActivity.kt`, `ui/Nav.kt`, `ui/settings/Settings.kt`, `AndroidManifest.xml`, `strings.xml`
- Create: `app/src/test/java/uz/hangulfriend/reminders/ReminderPolicyTest.kt`

**Interfaces:**
- Produces:
  - `Settings.reminderEnabled: Boolean = false`, `Settings.reminderMinutes: Int = 1200`; `SettingsRepository.setReminder(enabled: Boolean, minutes: Int)`.
  - `object ReminderPolicy { fun message(todayXp: Int, goal: Int, dueCount: Int, streak: Int): String?; fun delayUntilNext(now: ZonedDateTime, minutes: Int): Duration }` — `message` returns `null` when `todayXp >= goal`; texts exactly as the spec (§3), streak part omitted when `streak == 0`.
  - `class ReminderScheduler(context: Context) { fun apply(s: Settings) }` — enabled → enqueue unique `daily_reminder` `OneTimeWorkRequest` with `delayUntilNext`, `ExistingWorkPolicy.REPLACE`; disabled → cancel.
  - `class ReminderWorker(ctx, params) : CoroutineWorker` — reads settings/game/study from `(applicationContext as HangulFriendApp).container`; skips when not onboarded; posts on channel `reminders` with a `PendingIntent` to `MainActivity` carrying extra `EXTRA_OPEN = "open"` value `"review"`; then calls `ReminderScheduler.apply` again.
  - `MainActivity` passes the extra to `HangulFriendNav(container, openReview: Boolean)`, which navigates to `Routes.session(SessionMode.REVIEW)` once after start.

- [ ] **Step 1: Write failing `ReminderPolicyTest`**: `noMessageWhenGoalMet` (xp 60, goal 50 → null); `messageWithDueAndStreak` (due 23, streak 12 → `"Bugun 23 ta karta kutyapti. Streak: 12 kun 🔥"`); `messageWithDueNoStreak` (→ `"Bugun 23 ta karta kutyapti."`); `messageWithoutDue` (→ `"Bugungi maqsadga hali yetmadingiz. 5 daqiqa mashq qilamizmi?"`); `delayLaterToday` (now 18:30, 1200 → 1h30m); `delayWhenTimePassedIsTomorrow` (now 21:00 → 23h); `delayWhenExactlyNowIsTomorrow` (now 20:00 → 24h).
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests "*ReminderPolicyTest*"` — expected FAIL.
- [ ] **Step 3: Implement `ReminderPolicy`.**
- [ ] **Step 4: Run** — expected PASS.
- [ ] **Step 5: Wire Android pieces**: dependency; settings keys `reminder_enabled`, `reminder_minutes`; worker, scheduler, notification channel created in `HangulFriendApp.onCreate`; `POST_NOTIFICATIONS` permission in manifest; Settings screen row with `Switch` + time button (`TimePickerDialog`), requesting the permission via `rememberLauncherForActivityResult(RequestPermission())` on API 33+ and showing "Ruxsat berilmagan" with a button to app notification settings when denied; scheduler applied on settings change and on app start.
- [ ] **Step 6: Run** `./gradlew :app:testDebugUnitTest :app:assembleDebug` — expected BUILD SUCCESSFUL.
- [ ] **Step 7: Manual check on phone**: enable, set time 2 minutes ahead with today's goal unmet → notification appears; tap opens review.
- [ ] **Step 8: Commit** `feat(reminders): daily reminder when the goal is not met`.

### Task 7: Export and import

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/data/Backup.kt` (DTOs + codec), `data/BackupService.kt`
- Modify: `data/Daos.kt` (bulk insert/delete), `AppContainer.kt`, `ui/settings/Settings.kt`, `strings.xml`
- Create: `app/src/test/java/uz/hangulfriend/data/BackupCodecTest.kt`, `BackupServiceTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class BackupFile(val format: String, val version: Int, @SerialName("exported_at_ms") val exportedAtMs: Long, @SerialName("db_version") val dbVersion: Int, val settings: BackupSettings, val cards: List<CardDto>, @SerialName("review_logs") val reviewLogs: List<ReviewLogDto>, @SerialName("lesson_progress") val lessonProgress: List<LessonProgressDto>, @SerialName("xp_events") val xpEvents: List<XpEventDto>, val achievements: List<AchievementDto>, @SerialName("best_scores") val bestScores: List<BestScoreDto>, @SerialName("story_progress") val storyProgress: List<StoryProgressDto>)` — one DTO per entity with the same fields (enums as their names), plus `fun XEntity.toDto()` / `fun XDto.toEntity()`.
  - `object BackupCodec { const val FORMAT = "hangul-friend-backup"; const val VERSION = 1; fun encode(f: BackupFile): String; fun decode(text: String): BackupResult }` with `sealed interface BackupResult { data class Ok(val file: BackupFile); data object NotBackup; data object TooNew }`. Decoder uses `ignoreUnknownKeys = true`; malformed JSON or wrong `format` → `NotBackup`; `version > VERSION` → `TooNew`.
  - `class BackupService(db: AppDatabase, settings: SettingsRepository, clock: Clock) { suspend fun export(): String; suspend fun import(file: BackupFile) }` — import deletes all rows of the 7 tables and inserts the file's rows in one `db.withTransaction`, then writes settings.
  - Settings UI: "Faylga saqlash" (`CreateDocument("application/json")`, default name `hangul-friend-YYYY-MM-DD.json`) and "Fayldan tiklash" (`OpenDocument(arrayOf("application/json", "*/*"))`) → decode → confirm dialog → import → `ReminderScheduler.apply`; messages "Fayl mos emas", "Bu fayl ilovaning yangiroq versiyasida yaratilgan", "Progress tiklandi", "Saqlandi".

- [ ] **Step 1: Write failing tests**:
  - `BackupCodecTest`: `roundTrip` (file with one row of each table → encode → decode → `Ok` equal to original); `rejectsNewerVersion` (`version = 2` → `TooNew`); `rejectsOtherFormat` → `NotBackup`; `rejectsMalformedJson` (`"{"`) → `NotBackup`; `acceptsUnknownKeys` (extra top-level `"extra": 1` → `Ok`); `entityDtoRoundTrip` (each entity → dto → entity equals original).
  - `BackupServiceTest` (Robolectric, in-memory Room, DataStore in a temp file): `exportThenImportRestoresEverything` (seed rows + settings, export, wipe by importing an empty file, import the export → all rows and settings equal the seed); `importReplacesExistingRows` (existing card absent from the file is gone after import).
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests "*Backup*"` — expected FAIL.
- [ ] **Step 3: Implement** DTOs, codec, DAO bulk methods (`insertAll`, `deleteAll` per table), `BackupService`, `val backup = BackupService(db, settings, clock)` in `AppContainer`.
- [ ] **Step 4: Run** — expected PASS.
- [ ] **Step 5: Wire the Settings UI** as above.
- [ ] **Step 6: Run** `./gradlew :app:testDebugUnitTest :app:assembleDebug` — expected BUILD SUCCESSFUL.
- [ ] **Step 7: Manual check on phone**: export → clear app data → onboarding appears → import from Settings is unreachable before onboarding, so finish onboarding, import, and confirm XP, streak and map statuses are back.
- [ ] **Step 8: Commit** `feat(backup): export and import progress as JSON`.

### Task 8: Shareable progress card

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/share/ProgressStats.kt`, `share/ShareCardRenderer.kt`, `share/ShareSender.kt`
- Create: `app/src/main/res/xml/file_paths.xml`
- Modify: `AndroidManifest.xml` (FileProvider), `ui/home/Home.kt`, `ui/Nav.kt`, `AppContainer.kt`, `strings.xml`
- Create: `app/src/test/java/uz/hangulfriend/share/ProgressStatsTest.kt`

**Interfaces:**
- Produces:
  - `data class ShareSnapshot(val level: Int, val totalXp: Int, val streak: Int, val learnedWords: Int, val completedLessons: Int, val statuses: List<LessonStatus>, val currentLessonTitle: String?, val date: LocalDate)` — `statuses` has 18 entries in catalog order.
  - `class ProgressStats(content: ContentRepository, db: AppDatabase, game: GameRepository, settings: SettingsRepository, clock: Clock) { suspend fun snapshot(): ShareSnapshot }` — completed = COMPLETED + VERIFIED; missing progress rows → NOT_STARTED; current lesson title = `"${unit}-${lesson} ${titleKo}"`.
  - `object ShareCardRenderer { const val WIDTH = 1080; const val HEIGHT = 1350; fun render(s: ShareSnapshot): Bitmap }` — draws header "Hangul Friend 한글", three big numbers (🔥 streak, level/XP, words), a 9×2 grid coloured by status, current lesson, date.
  - `object ShareSender { fun share(context: Context, bitmap: Bitmap) }` — writes `cacheDir/share/progress.png`, `FileProvider.getUriForFile(context, "uz.hangulfriend.fileprovider", file)`, `Intent.createChooser(ACTION_SEND image/png with FLAG_GRANT_READ_URI_PERMISSION)`; `ActivityNotFoundException` → toast "Ulashish uchun ilova topilmadi".
  - Home: share icon in the header → preview dialog (the bitmap via `asImageBitmap()`) with "Ulashish" / "Yopish".

- [ ] **Step 1: Write failing `ProgressStatsTest`** (Robolectric, in-memory Room, fake asset source via existing `Fixtures`): `emptyDatabaseGivesZeros` (level 1, xp 0, streak 0, words 0, completed 0, 18 × NOT_STARTED); `countsCompletedAndVerifiedOnly` (one COMPLETED, one VERIFIED, one PASSED → completed 2); `currentLessonTitleFormatted` (current `u02_l1` → `"2-1 한번 입어 보세요"`).
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests "*ProgressStatsTest*"` — expected FAIL.
- [ ] **Step 3: Implement `ProgressStats`.**
- [ ] **Step 4: Run** — expected PASS.
- [ ] **Step 5: Implement** renderer, sender, FileProvider (`<cache-path name="share" path="share/" />`), Home button and preview dialog.
- [ ] **Step 6: Run** `./gradlew :app:testDebugUnitTest :app:assembleDebug` — expected BUILD SUCCESSFUL.
- [ ] **Step 7: Manual check on phone**: open preview, share to Telegram, image looks right and is readable.
- [ ] **Step 8: Commit** `feat(share): shareable progress card`.

### Task 9: Ship the code PR

- [ ] **Step 1: Update the spec** to match decisions made here (XP reason string `story`, learned words = `countLearnedWords`, Robolectric instead of instrumented) and commit `docs: align stage 5 spec with plan`.
- [ ] **Step 2: Full check**: `./gradlew :app:testDebugUnitTest :app:assembleDebug`, `python -m pytest tools -q`, `python tools/validate_content.py` — all green.
- [ ] **Step 3: Self-review the whole branch diff** against the spec and Review Focus; fix findings.
- [ ] **Step 4: Install on the phone**, push `feat/stage5`, open PR to `master` (body ends with the Claude Code line), update project memory.

### Task 10–12: Remaining 16 episodes (separate content PRs)

Each PR: branch `content/stories-N`, write episodes, `python tools/audio_gen.py`, `python tools/validate_content.py`, build, install, commit, PR.

- [ ] **Task 10:** units 1, 3, 4 (6 episodes).
- [ ] **Task 11:** units 5, 6, 7 (6 episodes).
- [ ] **Task 12:** units 8, 9 (4 episodes) and add `ContentAssetsTest.everyLessonHasStory` (all 18 lessons have a non-null `story`).
