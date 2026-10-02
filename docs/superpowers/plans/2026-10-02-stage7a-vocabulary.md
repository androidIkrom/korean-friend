# Stage 7a — Vocabulary Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A vocabulary screen listing every book word plus the user's own words (search, lesson filter, status), with own words joining FSRS review.

**Architecture:** Own words live in a new Room table (DB v4). `UserWordRepository` validates input, creates or deletes their cards and exposes them as a pseudo-lesson `"user"`. `LessonLookup` resolves either a book lesson or that pseudo-lesson, so review and mistakes work unchanged. The screen's filtering and status are pure functions.

**Tech Stack:** Kotlin, Room 2.8 (KSP, schema export), DataStore, Compose M3, Robolectric `@Config(sdk = [35])`.

**Spec:** `docs/superpowers/specs/2026-10-02-stage7a-vocabulary-design.md`

## Global Constraints

- DB version 4, `MIGRATION_3_4` creates `user_words`; no destructive fallback; schema `app/schemas/.../4.json` is exported and copied to the debug assets like earlier versions.
- `USER_LESSON_ID = "user"`; word ids `user_w<rowId>`; cards `lessonOrder = -1`, `origin = LESSON`.
- Limits: ko 1–40, uz 1–80, note ≤ 120 characters after trim; a duplicate `ko` (case- and space-insensitive) against the book or own words is rejected.
- Backup: `user_words` default empty, `BackupService.DB_VERSION = 4`, `BackupCodec.VERSION` unchanged.
- UI copy Uzbek; no subagents, no worktrees, no `Co-Authored-By`.
- Test command: `./gradlew :app:assembleDebug :app:testDebugUnitTest`.

## Review Focus

1. Upgrading a phone with DB v3 must keep every card and log — Task 1 `migrate3to4_addsUserWords`.
2. Deleting an own word must remove its cards and logs, or review would crash on a missing word — Task 1 `deleteRemovesCardsAndLogs`.
3. Review must not drop own-word cards (they have no book lesson) — Task 3 `lookupResolvesUserLesson`, `reviewBuildsUserWordExercises`.
4. Uzbek search with any apostrophe variant must match (o'z / oʻz / o’z) — Task 4 `apostropheVariantsMatch`.
5. Old backups without `user_words` must import — Task 2 `decodesOldFileWithoutUserWords`.

---

### Task 1: Table, migration, repository

**Files:** Modify `data/Entities.kt`, `data/Daos.kt`, `data/AppDatabase.kt`, `AppContainer.kt`; Create `data/UserWordRepository.kt`; Test `MigrationTest.kt`, create `UserWordRepositoryTest.kt`.

**Interfaces:**
- Produces: `@Entity("user_words") data class UserWordEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val ko: String, val uz: String, val note: String?, val createdMs: Long)`;
  `UserWordDao { insert(e): Long; delete(id: Long); observeAll(): Flow<List<UserWordEntity>> (createdMs DESC); all(); insertAll(list); deleteAll() }`;
  `CardDao.deleteByItem(itemId: String)`, `ReviewLogDao.deleteForCards(cardIds: List<String>)`;
  `const val USER_LESSON_ID = "user"`; `fun userWordId(rowId: Long) = "user_w$rowId"`;
  `sealed interface AddResult { data class Added(val id: Long); data object Empty; data object TooLong; data object Duplicate }`;
  `class UserWordRepository(db, study: StudyRepository, clock) { suspend fun add(ko: String, uz: String, note: String?, bookKo: Set<String>): AddResult; suspend fun delete(id: Long); fun observe(): Flow<List<UserWordEntity>>; suspend fun asLesson(): Lesson }`;
  `fun normalizeKo(s: String): String` (trim, collapse/remove spaces, lowercase).

- [ ] **Step 1: Failing tests.** `MigrationTest.migrate3to4_addsUserWords` (card row survives, `user_words` empty). `UserWordRepositoryTest`:
  - `addCreatesCards`: ids `user_w1#R`/`user_w1#P`, `lessonId == "user"`, `lessonOrder == -1`;
  - `duplicateOwnWordRejected` (`"사과"` then `" 사 과 "`);
  - `bookWordRejected`;
  - `blankRejected`, `tooLongRejected` (41-char ko);
  - `deleteRemovesCardsAndLogs`;
  - `asLessonListsWords` (id, ko, uz, `exampleKo` = note).
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement.** `add` runs in one transaction: insert, then `study.ensureCards(singleWordLesson, -1, CardOrigin.LESSON)` (which promotes nothing for `"user"`). `delete` runs logs, cards and the row in one transaction. `AppDatabase` version 4, entity added, `userWords()` dao, `MIGRATION_3_4` SQL `CREATE TABLE IF NOT EXISTS user_words (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ko TEXT NOT NULL, uz TEXT NOT NULL, note TEXT, createdMs INTEGER NOT NULL)`. Build once to export `4.json`, then copy it into the debug assets schema folder the same way 3.json is.
- [ ] **Step 4: PASS; suite green.** **Step 5: Commit** `feat(data): user words table (DB v4) with review cards`

### Task 2: Backup

**Files:** `data/Backup.kt`, `data/BackupService.kt`; Tests `BackupCodecTest.kt`, `BackupServiceTest.kt`.
- Produces: `@Serializable data class UserWordDto(val id: Long, val ko: String, val uz: String, val note: String?, @SerialName("created_ms") val createdMs: Long)`; `BackupFile.userWords` (`@SerialName("user_words")`, default `emptyList()`).
- [ ] Failing tests: `decodesOldFileWithoutUserWords`, `userWordsRoundTrip` (codec); `exportThenImportRestoresUserWords` (service). Implement (export reads `all()`, import clears and inserts inside the transaction; `DB_VERSION = 4`). PASS, suite green. Commit `feat(backup): carry user words`.

### Task 3: Review and mistakes see own words

**Files:** Create `data/LessonLookup.kt`; Modify `ui/session/SessionController.kt` (use lookup in `lessonsOf`), `ui/game/AchievementsAndMistakes.kt` (`MistakesViewModel` uses lookup), `ui/Nav.kt`, `AppContainer.kt`; Tests create `LessonLookupTest.kt`, extend `SessionBuilderTest.kt`.
- Produces: `class LessonLookup(content: ContentRepository, userWords: UserWordRepository) { suspend fun lesson(id: String): Lesson? }` (`"user"` → `asLesson()`, else `content.lesson(id)`); `AppContainer.lessons`.
- [ ] Failing tests: `lookupResolvesUserLesson`, `lookupFallsBackToBook`; `SessionBuilderTest.reviewBuildsUserWordExercises` (a `"user"` card + map with a pseudo-lesson → Flashcard / WordTyping). Implement; `SessionController` and `MistakesViewModel` constructors take `LessonLookup` instead of reading content for these lookups. PASS, suite green. Commit `feat(study): own words in review and mistakes`.

### Task 4: Vocabulary model

**Files:** Create `ui/vocab/VocabModel.kt`; Test `VocabModelTest.kt`.
- Produces: `enum class WordStatus { NEW, LEARNING, LEARNED }`; `fun statusOf(recognize: CardEntity?): WordStatus` (null or `firstReviewedMs == null` → NEW, `REVIEW` → LEARNED, else LEARNING);
  `data class VocabEntry(val id: String, val ko: String, val uz: String, val lessonId: String, val lessonTag: String, val own: Boolean, val ownId: Long?, val word: Word, val note: String?)`;
  `sealed interface VocabFilter { data object All; data object Own; data class Lesson(val id: String) }`;
  `fun normalizeUz(s: String): String` (lowercase, `ʻ ’ ‘ \`` → `'`, collapse spaces); `fun filterVocab(entries: List<VocabEntry>, query: String, filter: VocabFilter): List<VocabEntry>` (own words first, then book order; query matches normalized ko substring or normalized uz substring; blank query = no text filter).
- [ ] Failing tests `VocabModelTest`: `koreanSearch`, `uzbekSearchIgnoresCase`, `apostropheVariantsMatch`, `spacesIgnoredInKorean`, `ownFilter`, `lessonFilter`, `ownWordsFirst`, `statusMapping` (4 cases). Implement; PASS; commit `feat(vocab): search, filter and status model`.

### Task 5: Vocabulary screen

**Files:** Create `ui/vocab/Vocab.kt` (`VocabViewModel`, `VocabScreen`, word sheet, add dialog); Modify `ui/Nav.kt` (route `vocab`), `ui/home/Home.kt` (third row: full-width "Lug'at" `SideAction`, new `onVocab`), `strings.xml`.
- `VocabViewModel(content, userWords: UserWordRepository, db: AppDatabase)`: book entries built once on `Dispatchers.IO` from every available lesson (`lessonTag = "u-l"`); own entries and card statuses from flows (`cards().observeAll()` — add `@Query("SELECT * FROM cards WHERE kind = 'RECOGNIZE'") fun observeRecognize(): Flow<List<CardEntity>>`); `query`, `filter` state; `add(ko, uz, note)` returns the `AddResult` for the dialog; `delete(id)`.
- UI per spec §3: header with count, `OutlinedTextField` search (leading search icon, clear button), `LazyRow` filter chips (`FilterChip`), list rows (status dot, ko, uz, tag or ★), `ModalBottomSheet` details (AudioButton for word and example, delete with confirm `AlertDialog` for own words), FAB "+" → `AlertDialog` with 3 fields and inline errors, snackbar on add, empty states.
- Strings: `vocab_title` "Lug'at", `vocab_count` "%1$d ta so'z", `vocab_search` "Qidirish (koreyscha yoki o'zbekcha)", `vocab_all` "Hammasi", `vocab_own` "Mening so'zlarim", `vocab_status_new` "Yangi", `vocab_status_learning` "O'rganilmoqda", `vocab_status_learned` "O'rganilgan", `vocab_add` "So'z qo'shish", `vocab_ko` "Koreyscha", `vocab_uz` "O'zbekcha", `vocab_note` "Izoh (ixtiyoriy)", `vocab_add_ok` "Qo'shish", `vocab_err_empty` "To'ldiring", `vocab_err_long` "Juda uzun", `vocab_err_dup` "Bu so'z allaqachon bor", `vocab_added` "So'z qo'shildi va takrorlashga tushdi", `vocab_delete` "O'chirish", `vocab_delete_q` "Bu so'z va uning takrorlash tarixi o'chiriladi.", `vocab_cancel` "Bekor qilish", `vocab_none` "Hech narsa topilmadi", `vocab_own_empty` "Hali so'z qo'shmagansiz — pastdagi + tugmasini bosing.", `home_action_vocab` "Lug'at".
- [ ] Implement; `assembleDebug` + suite green; commit `feat(vocab): vocabulary screen with own words`.

### Task 6: Docs
- [ ] Main spec stage list line for 7a; commit `docs: stage 7a status`.
