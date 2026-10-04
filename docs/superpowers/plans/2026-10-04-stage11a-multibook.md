# Stage 11a — Multi-book Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the app hold books 1, 2 and 3 side by side: book-aware ids, catalog, boss and final tests, a map book selector and grouping by book. No new lessons yet; books 1 and 3 show "coming soon".

**Architecture:** One `LessonId` parser/formatter replaces every place that slices lesson ids. `CatalogEntry.book` comes from three catalog files. Every per-book view (map, final test, onboarding, stories, share card) filters or groups by `book`. Book 2 keeps its ids, so saved data stays valid.

**Tech Stack:** Kotlin, Compose, kotlinx.serialization, Robolectric unit tests.

**Spec:** `docs/superpowers/specs/2026-10-04-stage11-books-design.md`

## Global Constraints

- Book 2 ids stay `uNN_lN`. New books use `bB_uNN_lN`, with B in 1 or 3.
- Lesson codes shown to people: `2-1` for book 2 (as today), `1·3-1` for other books.
- Final-test score ids: `final_test` for book 2 (the existing value), `final_test_b1` and `final_test_b3`.
- The boss route argument is `"<book>:<unit>"`. A bare number from an older route means book 2.
- Every string exists in uz and en (StringsParityTest).
- Commits carry no Co-Authored-By line.

## Review Focus

- **Old ids:** `u02_l1` (and `user`, `final_…` refs) parse exactly as before. Test: `LessonId.parse("u02_l1") == LessonId(2, 2, 1)`, `lessonOfRef("u02_l1_e07") == "u02_l1"`.
- **New ids in flags:** `lessonOfRef("b1_u03_l2_w004") == "b1_u03_l2"`, and its header reads "1·3-2".
- **Empty books:** a book with no catalog entries renders "coming soon" without crashing; `buildUnits(emptyList(), null)` is empty.
- **Final per book:** book 2's best score keeps reading `final_test`. Test: `finalTestId(2) == "final_test"`.
- **Old backups:** a backup with only book 2 lessons still imports (existing BackupServiceTest stays green).

---

### Task 1: `LessonId`

**Files:** Create `study/LessonId.kt`. Modify `ui/session/SessionController.kt`, `data/FlagRepository.kt`. Test `study/LessonIdTest.kt`.

**Interfaces:**
- `data class LessonId(val book: Int, val unit: Int, val lesson: Int)`
  - `val id: String`: the stored form;
  - `val code: String`: the label shown to people;
  - `companion object { fun parse(id: String): LessonId?; fun of(book: Int, unit: Int, lesson: Int): String }`.
- `fun finalTestId(book: Int): String`.
- `SessionController.unitLessonId(unit, lesson)` is replaced by `LessonId.of(book, unit, lesson)`. BOSS reads `book:unit`.
- `lessonOfRef` uses the regex `^(?:b\d_)?u\d{2}_l\d`, and the export header uses `LessonId.parse(...)?.code`.

- [ ] Tests:
  - parse and format round trip for `u02_l1` and `b1_u03_l2`;
  - garbage → null;
  - `code` gives `2-1` and `1·3-2`;
  - `finalTestId`: 2 → `final_test`, 1 → `final_test_b1`;
  - `lessonOfRef` for old and new refs.
- [ ] Implement, then run the tests (green).
- [ ] Commit `feat(content): LessonId for book-aware lesson ids`.

### Task 2: Catalog with books, and finals per book

**Files:** Modify `content/Model.kt` (`CatalogEntry.book = 2`), `content/ContentRepository.kt`, `study/GameRules.kt` usage, `ui/session/SessionController.kt`, `ui/map/BookMap.kt`, `ui/Nav.kt`.

**Interfaces:**
- `catalog()` returns book1, then book 2, then book3, each entry with its file's book.
- `finalTest(book: Int = 2)` reads `final_test.json` for book 2 and `final_test_b$book.json` otherwise.
- The FINAL session's `lessonId` is the book number (null means 2), and the score goes to `finalTestId(book)`.

- [ ] Tests:
  - the catalog orders books and stamps `book` (temp assets dir, as `SessionControllerTest` does);
  - FINAL with book 2 reads `final_test.json`;
  - the boss with `"2:2"` builds from `u02_l1` and `u02_l2`.
- [ ] Implement, then run the tests.
- [ ] Commit.

### Task 3: Map book selector

**Files:** Modify `ui/map/BookMap.kt`, `ui/map/MapSkins.kt`, strings.

**Interfaces:**
- `BookMapViewModel.book: StateFlow<Int>` starts at the current lesson's book, or 2.
- `selectBook(b)`.
- Rows are filtered to the book.
- `finalBest` is per book.
- The header shows glass pills "1-kitob · 2-kitob · 3-kitob" (`book_label`). An empty book shows `book_soon`.

- [ ] Run the existing `buildUnits` tests, plus a new one: empty → empty.
- [ ] Implement.
- [ ] Check on the emulator: book 2 unchanged; books 1 and 3 say "Tez kunda".
- [ ] Commit.

### Task 4: Grouping by book elsewhere

**Files:** `ui/onboarding/Onboarding.kt` (book headers in the picker), `ui/story/StoryList.kt` (group by book and unit), `ui/vocab/Vocab.kt` (chip tag = `LessonId.code`), `ui/home/Home.kt` and `ui/lesson/LessonScreen.kt` (codes through `LessonId`), `share/ProgressStats.kt` (statuses of the current lesson's book only).

- [ ] Test: `ShareSnapshot.statuses` has only book-2 lessons when the current lesson is in book 2.
- [ ] Implement, then run all tests.
- [ ] Emulator pass: Home, Map, Stories, Vocabulary and Onboarding in Uzbek.
- [ ] Commit, push, open the PR "Stage 11a: multi-book foundation".
