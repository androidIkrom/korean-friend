# Stage 7b — "Xato bor" Content Flags Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A flag button on every exercise stores content-error reports locally; Settings shares them as readable text and can clear them.

**Architecture:** A new Room table (DB v5) holds the flags, and `FlagRepository` upserts them by `(ref, reason)`. Pure helpers derive `ref`, `snapshot` and `lessonId` from an `ExerciseItem`, and a pure `exportText` formats the report. `ExerciseView` shows the button whenever a `LocalFlagReporter` is provided.

**Spec:** `docs/superpowers/specs/2026-10-02-stage7b-content-flags-design.md`

## Global Constraints

- DB version 5, `MIGRATION_4_5`, unique index `(ref, reason)`; schema 5.json exported.
- Reasons: `TRANSLATION` "Tarjima xato", `AUDIO` "Audio xato", `ANSWER_REJECTED` "To'g'ri javob qabul qilinmadi", `TYPO` "Imlo / matn xato", `OTHER` "Boshqa".
- Comment ≤ 300 chars (trimmed, blank → null).
- Backup `content_flags` default empty; `BackupService.DB_VERSION = 5`.
- Flagging never touches XP or FSRS.
- Test command `./gradlew :app:assembleDebug :app:testDebugUnitTest`. No subagents, no worktrees, no `Co-Authored-By`.

## Review Focus

1. DB 4 → 5 keeps data — `migrate4to5_addsContentFlags`.
2. Re-flagging the same item and reason must not duplicate — `sameRefAndReasonUpdates`.
3. Export text must stay readable when a flag has no comment or an unknown lesson — `exportGroupsAndOrders`, `exportWithoutComment`.
4. Every `ExerciseItem` variant yields a non-blank ref and snapshot — `refAndSnapshotForEveryType`.
5. Old backups import — `decodesOldFileWithoutFlags`.

### Task 1: Table, model, repository
**Files:** `data/Entities.kt`, `data/Daos.kt`, `data/AppDatabase.kt`, create `data/FlagRepository.kt`, `AppContainer.kt`; tests `MigrationTest.kt`, create `FlagModelTest.kt`, `FlagRepositoryTest.kt`.
- Produces: `enum class FlagReason { TRANSLATION, AUDIO, ANSWER_REJECTED, TYPO, OTHER }`;
  `@Entity("content_flags", indices = [Index(value = ["ref", "reason"], unique = true)]) data class ContentFlagEntity(id: Long = 0, ref: String, lessonId: String, type: String, snapshot: String, reason: String, comment: String?, createdMs: Long)`;
  `FlagDao { upsertByKey via @Query? → use find(ref, reason) + insert/update in repository; observeCount(); all(); insertAll; deleteAll }`;
  `fun flagRef(item: ExerciseItem): String`, `fun flagSnapshot(item: ExerciseItem): String`, `fun lessonOfRef(ref: String): String`;
  `class FlagRepository(db, clock) { suspend fun flag(item, reason, comment: String?); fun observeCount(): Flow<Int>; suspend fun all(); suspend fun clear(); }`;
  `fun exportText(flags: List<ContentFlagEntity>, appVersion: String, today: LocalDate, reasonLabel: (FlagReason) -> String): String`.
- [ ] Failing tests:
  - `migrate4to5_addsContentFlags`;
  - `FlagModelTest`: `refAndSnapshotForEveryType`, `lessonOfRefPrefix` (`u02_l1_e07` → `u02_l1`, `user_w3` → `user`, `xyz` → `""`);
  - `FlagRepositoryTest`: `flagStores`, `sameRefAndReasonUpdates`, `otherReasonAddsRow`, `clearEmpties`, `commentTrimmedAndCapped`;
  - `exportGroupsAndOrders`: book lessons in id order, then `user`, then other; numbering restarts at 1 per group; header line `Jami: N`;
  - `exportWithoutComment`.
  - Then implement until GREEN, keep the suite green and commit `feat(data): content flags table (DB v5) and export text`.

### Task 2: Backup
- `ContentFlagDto` (`@SerialName` snake case), `BackupFile.contentFlags` (`content_flags`, default empty), export/import, `DB_VERSION = 5`.
- [ ] Failing tests:
  - `decodesOldFileWithoutFlags`, `flagsRoundTrip` (codec);
  - `exportThenImportRestoresFlags` (service).
  - Then implement until GREEN and commit `feat(backup): carry content flags`.

### Task 3: UI
**Files:** create `ui/exercise/FlagButton.kt` (`fun interface FlagReporter { suspend fun flag(item: ExerciseItem, reason: FlagReason, comment: String?) }`, `LocalFlagReporter` default null, `FlagButton(item)` + dialog with radio reasons, comment field, save → Toast "Belgilandi, rahmat!"), modify `ui/exercise/ExerciseView.kt` (Column: right-aligned FlagButton row above the existing `when`), `MainActivity.kt` (provide reporter from `container.flags`), create `ui/settings/FlagsRow.kt` (count, "Yuborish" share intent, "Tozalash" with confirm), modify `ui/settings/Settings.kt` (VM gets `FlagRepository`; row placed after BackupRow), `ui/Nav.kt`, `strings.xml`.
- [ ] Implement, then `assembleDebug` and the suite must be green; commit `feat(ui): "Xato bor" flag on exercises; share flags from settings`.

### Task 4: Docs
- [ ] Add the 7b status line to the main spec and commit `docs: stage 7b status`.
