# Stage 11b — Hangul Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Book 1, unit 1 becomes a Hangul course: five lessons of letters with sounds, tips and example words, a letter screen, four letter exercises, and letter cards in FSRS reviews.

**Architecture:**
- `assets/hangul.json` holds the letters and the five lessons.
- `ContentRepository.lesson("b1_u01_lN")` turns a Hangul lesson into an ordinary `Lesson` whose words are its letters, with word id `h_<letter id>`. So FSRS cards, reviews, progress and the map work unchanged.
- The LESSON route opens `HangulScreen` for these ids. A PRACTICE session on them uses `SessionBuilder.hangulPractice`, and its score completes the lesson.

**Tech Stack:** Kotlin, Compose, kotlinx.serialization, Robolectric, Python (edge-tts).

**Spec:** `docs/superpowers/specs/2026-10-04-stage11-books-design.md` (§1 Hangul module, §2 Hangul screen and new items)

## Global Constraints

- Hangul lesson ids: `b1_u01_l1` … `b1_u01_l5`.
- Letter card item ids: `h_<letter id>`, where the letter id is ASCII (`a`, `eo`, `g`, `kk`, `f_k`).
- **Deviation from the spec:** cards keep the real lesson id, not a pseudo-lesson `hangul`. `LessonLookup` already resolves it through `content.lesson`.
- Letter cards are RECOGNIZE only. There are no PRODUCE cards for letters.
- Letter audio says a syllable, not the letter's name:
  - a vowel with silent ㅇ: ㅏ is read as 아;
  - a consonant with ㅏ: ㄱ is read as 가;
  - a final after 아: final ㄱ is read as 악.
- Every string exists in uz and en.
- Commits carry no Co-Authored-By line.

## Review Focus

- **Vocabulary and the Arena must not list letters as words.** Hangul lessons are excluded from word pools. Test: the VOCAB session pool has no `h_` ids.
- **Lesson 1 has only vowels, so there is no consonant to build a syllable with.** `hangulPractice` must not crash and makes no BuildSyllable item. Test: lesson 1 practice has no BuildSyllable.
- **Distractors:** every choice item has 4 distinct options, or fewer when fewer letters are known, with the answer always among them.
- **A Hangul card whose letter was removed from `hangul.json`** is skipped in review, as a missing word already is.
- **Old book-2 learners:** the Hangul lessons show as NOT_STARTED and nothing is auto-passed.

---

### Task 1: `Hangul.compose`

**Files:** `core/hangul/.../Hangul.kt`, `HangulTest.kt`.

- [ ] `fun compose(initial: Char, medial: Char, final: Char? = null): Char?`
  - Tests: `compose('ㄱ','ㅏ') == '가'`, `compose('ㅎ','ㅏ','ㄴ') == '한'`, `compose('ㅏ','ㅏ') == null`.
  - Round trip with `parts`.

### Task 2: Content — `hangul.json`, model, repository, catalog, audio

**Files:** `content/Model.kt` (`HangulCourse`, `Letter`, `LetterExample`, `HangulLesson`), `content/ContentRepository.kt`, `content/Localize.kt`, `assets/hangul.json`, `assets/book1.json`, `tools/audio_gen.py` (+ test), `tools/validate_content.py`.

**Interfaces:**
- `ContentRepository.hangul(): HangulCourse?`
- `lesson(id)` synthesizes Hangul lessons.
- `isAvailable(id)` is true for them.
- `fun isHangulLesson(id: String): Boolean` (book 1, unit 1).
- `Letter.asWord(): Word`: id `h_<id>`, ko = jamo, uz = romanization, pos = kind, example = the example word, audio = the letter audio.
- **Lessons:**
  1. ㅏ ㅓ ㅗ ㅜ ㅡ ㅣ ㅐ ㅔ ㅑ ㅕ ㅛ ㅠ
  2. ㄱ ㄴ ㄷ ㄹ ㅁ ㅂ ㅅ ㅇ ㅈ ㅎ
  3. ㅋ ㅌ ㅍ ㅊ ㄲ ㄸ ㅃ ㅆ ㅉ
  4. ㅒ ㅖ ㅘ ㅙ ㅚ ㅝ ㅞ ㅟ ㅢ
  5. finals ㄱ ㄴ ㄷ ㄹ ㅁ ㅂ ㅇ
- [ ] Tests:
  - `lesson("b1_u01_l2")` has 10 words with `h_` ids;
  - the catalog lists 5 book-1 entries;
  - the audio_gen test collects letter and example clips;
  - the validator flags a lesson naming an unknown letter.

### Task 3: Items and builder

**Files:** `study/ExerciseItem.kt`, `study/SessionBuilder.kt`, `data/StudyRepository.kt`, `ui/session/SessionController.kt`.

**Interfaces:**
- `LetterListen(letter: Letter, options: List<Letter>)`: hear the letter, pick the jamo.
- `LetterSound(letter, options)`: see the jamo, pick the romanization.
- `ReadWord(letter, options)`: read the letter's example word, pick its romanization.
- `BuildSyllable(target: Char, letters: List<Letter>, initials: List<Char>, medials: List<Char>, finals: List<Char>)`: `letters` are the target's parts that belong to the lesson; the card ids are theirs.
- `SessionBuilder.hangulPractice(lesson: List<Letter>, known: List<Letter>): List<ExerciseItem>`, where `known` = the letters of this lesson and all earlier ones.
- `ensureCards` makes only RECOGNIZE cards for Hangul lessons.
- [ ] Tests:
  - lesson 2 practice has all four kinds, and every option list holds its answer;
  - lesson 1 practice has no BuildSyllable;
  - a PRACTICE session on a Hangul lesson records the score, so ≥ the pass mark gives COMPLETED;
  - the VOCAB pool has no `h_` ids.

### Task 4: Screens

**Files:**
- Create `ui/hangul/HangulScreen.kt` and `ui/exercise/LetterViews.kt`.
- Modify `ui/exercise/ExerciseView.kt`, `ui/Nav.kt`, `ui/onboarding/Onboarding.kt` and strings.
- **HangulScreen:**
  - a grid of glass letter tiles; tapping one plays its audio and opens a bottom sheet with the jamo, romanization, tip and example word (with audio);
  - a "Mashq" button that starts PRACTICE.
- **Onboarding:** a "Men butunlay yangi boshlovchiman" chip that picks `b1_u01_l1`.
- [ ] Emulator pass, taking turns with DevSuhbat through `emulator.lock`:
  - map book 1 → lesson → grid → sheet → practice → finished;
  - check both themes.
- [ ] Commit, push, open the PR "Stage 11b: Hangul module".
