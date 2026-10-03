# Stage 9b — Battle and Gate Cleared — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn every exercise session into a battle:
- a `[ QUEST ]` card;
- 3D lettered answer options;
- a floor bar with hearts and a combo chip;
- hit and miss effects with sound;
- a Gate Cleared reward screen;
- a louder rank-up.

**Architecture:**
- Session logic stays the same. `SessionState` gains three read-only facts for the UI:
  - `lastCorrect`, the verdict of the current item;
  - `loot`, the words won;
  - `totalXpAfter`, total XP after the wrap-up.
- Grades and stars are pure functions in `GameRules`.
- The visual pieces live in `ui/kit/Battle.kt`. The exercise views and `SessionScreen` use them.

**Tech Stack:** Compose animation (`Animatable`, `animateFloatAsState`, infinite transitions) and the 9a kit.

**Spec:** `docs/superpowers/specs/2026-10-03-stage9-arise-ui-design.md` (sub-stage 9b)

## Global Constraints

- No change to grading, XP amounts, FSRS or the DB.
- Every animation obeys `LocalReducedMotion`: with it on, effects show their end state at once and loops stop.
- New strings go in `values` and `values-en`. Apostrophes are written as `\'`.
- Build with `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`.

## Review Focus

1. **The final test (30 items, timer).**
   - The floor bar must still fit.
   - No XP appears, since the final test gives none.
   - The grade comes from the percentage.
   - This is checked on the emulator.
2. **Boss lost.**
   - The screen says "not cleared" with grade E and 0 stars.
   - It plays no CLEAR sound.
   - `GameRulesTest.failedIsE` covers it.
3. **Flashcard "Again" is not a WRONG hit**, because the learner rates themselves. `verdictSfx` returns null for `Rated`. `BattleLogicTest` covers it.
4. **Skipped items (no microphone)** give no verdict: `lastCorrect` stays null. `SessionControllerTest.skippedHasNoVerdict` covers it.
5. **Rotation in the middle of a battle.** Effects are keyed on state, so they do not replay old hits. This is checked on the emulator.

### Task 1: Battle logic

- **Files:**
  - `study/GameRules.kt`: `clearGrade(percent: Int, failed: Boolean): String` and `clearStars(percent: Int, failed: Boolean): Int`.
  - `ui/session/SessionController.kt`:
    - `SessionState`: `lastCorrect: Boolean?`, `loot: List<Word>`, `totalXpAfter: Int?`.
    - `fun SessionState.cleared(mode): Boolean`.
    - `fun ExerciseOutcome.verdictSfx(): Sfx?`.
- **Rules:**
  - **Grade:** a failed run gets E. Otherwise S from 95%, A from 85, B from 70, C from 55, D from 40, and E below that.
  - **Stars:** a failed run gets 0. Otherwise 3 from 90%, 2 from 70, 1 from 40, and 0 below that.
  - **Cleared:**
    - Boss: not failed.
    - TEST: at least `ProgressRepository.PASS_PERCENT`.
    - FINAL: TOPIK level ≥ 1.
    - Every other mode: true when it has items.
  - **Loot:** words of word items answered correctly, in session order, distinct by id.
    - Flashcard: any rating except AGAIN.
    - Match: words matched on the first try.
  - **Verdict sound:** `Checked` gives CORRECT or WRONG. `Matched` gives CORRECT. `Rated` and `Skipped` give null.
- **Tests:**
  - `GameRulesTest`: `gradeBands`, `starBands`, `failedIsE`.
  - `SessionControllerTest`:
    - `lastCorrectFollowsAnswer`: true after a correct submit, false after a wrong one, null after `next`.
    - `skippedHasNoVerdict`
    - `totalXpAfterSetOnFinish`
  - `BattleLogicTest`: `verdictSfx` and `cleared` across modes.

### Task 2: Battle kit (`ui/kit/Battle.kt`)

- **Produces:**
  - `enum OptionState { IDLE, PICKED, RIGHT, WRONG, DIM }`
  - `OptionTile(text, onClick, state, modifier, letter: Char? = null, enabled = true)`: a 3D tile with a lip. The letter sits in a chamfered badge. RIGHT is green with a glow, WRONG is red, DIM is faded.
  - `FloorBar(done: Int, total: Int, modifier)`: segments, with the current one pulsing. Above 20 items it becomes a continuous bar.
  - `HeartRow(hearts: Int, max: Int)`: a heart that is lost cracks (scale, tilt, fade into a broken heart).
  - `ComboChip(combo: Int)`: pops when the combo grows.
  - `FloatingText(text, key, color)`: rises 48 dp and fades out over 900 ms each time `key` changes.
  - `EdgeFlash(color, key)`: the screen edge glows, then fades over 450 ms.
  - `Modifier.shake(key: Int)`: a 6-step horizontal shake when `key` grows.
  - `GateRays(color, modifier)`: slowly turning light rays.

### Task 3: Exercise views

- `Parts.kt`:
  - `QuestCard(prompt, content)` replaces `PromptText`.
  - `FeedbackPanel` uses the HuntPanel accent. Next is a SUCCESS or PRIMARY HuntButton, and the AI question is SECONDARY.
  - `HintButton` becomes a small SECONDARY button.
- `ChoiceView` uses `OptionTile` with letters A–D.
- `MatchView` uses `OptionTile` without letters.
- `FlashcardView` rating buttons are HuntButtons: Again is DANGER, Hard is SECONDARY, Good is PRIMARY, Easy is SUCCESS.
- `BuildSentenceView` tokens become small SECONDARY HuntButtons; Check is PRIMARY.
- `TypingView`: Check is a PRIMARY HuntButton.
- `ExerciseView` wraps `onResult`: it plays `verdictSfx()` and shakes the item on a wrong `Checked` answer.
- New strings: `battle_quest` ("Topshiriq" / "Quest").

### Task 4: Session battle HUD and Gate Cleared

- **HUD:** the close button, a `FloorBar` with "n/N", then `HeartRow`, `ComboChip`, an XP chip and the timer.
- **Hits:**
  - `EdgeFlash` keyed on `index*2 + answered`: green when `lastCorrect`, red otherwise.
  - `FloatingText("+N XP")` keyed on `xpEarned`.
  - COMBO sound after 140 ms when the combo reaches `COMBO_FROM` or more.
- **`GateClearedView`** replaces `ResultView`:
  - rays, then the title "GATE CLEARED" or "GATE NOT CLEARED";
  - the grade letter drops in (scale 2.4 to 1);
  - three stars land 250 ms apart;
  - XP counts up and the level `GlowBar` fills from before to after;
  - the stat lines (score, final sections and level, boss, test pass);
  - new badges;
  - loot word chips, at most 8;
  - a GOLD CLAIM button.
  - The CLEAR sound plays when the run was cleared; otherwise WRONG plays.
  - With no items, the panel shows "No exercises" and a Finish button.
- **Strings:** `clear_title`, `clear_failed`, `clear_claim`, `clear_loot`, `clear_grade`, `clear_stars`, `battle_hearts`, `battle_combo`.

### Task 5: Rank-up

- `RankUpDialog`:
  - plays RANK_UP once;
  - a gold flare grows and fades behind the avatar, followed by `GateRays`;
  - the new rank letter drops in;
  - Accept is a GOLD HuntButton.

### Task 6: Verify

- Run all unit tests.
- Check on the emulator:
  - practice, test, boss win and loss, final, and the grammar check inside a lesson;
  - System + Uzbek and Neon + English;
  - with Remove animations on.
