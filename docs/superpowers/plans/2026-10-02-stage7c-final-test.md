# Stage 7c — Final TOPIK I-style Test Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A timed 30-question final test (15 listening, 15 reading) from the whole book, opened from the end of the map, with section scores and an estimated TOPIK I level.

**Architecture:** The questions are authored as content in `assets/final_test.json`. Listening questions reuse `listen_question`; a new `read_choice` type adds a Korean passage. The test is a new session mode, `FINAL`, that awards no per-answer XP, has a 25-minute timer and computes section scores. A map card starts it.

**Spec:** `docs/superpowers/specs/2026-10-02-stage7c-final-test-design.md`

## Global Constraints

- IDs: `final_l01`…`final_l15` and `final_r01`…`final_r15`. `targets` are empty. Each question has 4 distinct options and one answer that is one of them.
- Timer 25:00. Level: ≥ 70 % → 2, ≥ 40 % → 1, otherwise 0. Best score is stored in `best_scores` under `final_test`. +100 XP (`REASON_FINAL = "final"`) only the first time the score reaches ≥ 70 %.
- No per-answer XP and no FSRS change in FINAL mode; listening questions come first and the order is kept.
- Audio is generated with `tools/audio_gen.py` (edge-tts female voice), which now also processes `final_test.json`.
- Test commands: `./gradlew :app:assembleDebug :app:testDebugUnitTest`; `python -m unittest test_audio_gen` in `tools/`.
- No subagents, no worktrees, no `Co-Authored-By`.

## Review Focus

1. Time running out mid-test must end the test cleanly and count the unanswered questions as wrong — `timeUpEndsTest`.
2. Retaking the test must not pay the bonus again — `bonusOnlyOnce`.
3. Every listening question must have its audio file — `finalTestIsValid`.
4. A broken `final_test.json` must not crash release builds (it falls back to the test being unavailable) — `ContentRepository.finalTest()` returns null like a missing lesson; covered by the existing strict/lenient behaviour.
5. Flagging a final-test question must produce a readable export group — `lessonOfRef("final_l01") == "final"`.

### Task 1: `read_choice` type
- `ExerciseType.READ_CHOICE` (`@SerialName("read_choice")`). Extract `LessonValidator.exerciseErrors(e: Exercise): List<String>`; `validate` then uses it plus the target check. `READ_CHOICE` rule: `sentence` is non-blank, exactly 4 distinct options, and every answer is one of them.
- `ExerciseView`: `READ_CHOICE` renders a passage panel (`KoreanText(sentence)`) followed by `ChoiceView`.
- [ ] Failing tests `validate_readChoiceNeedsPassage`, `validate_readChoiceNeedsFourDistinctOptions`, `validate_readChoiceValid`. Implement until GREEN, then commit `feat(content): read_choice exercise type`.

### Task 2: Content and audio
- `@Serializable data class FinalTest(val listening: List<Exercise>, val reading: List<Exercise>)`; `ContentRepository.finalTest(): FinalTest?`.
- Write `assets/final_test.json`: 15 + 15 questions spread over units 1–9 (3–4 per unit), using the grammar in `docs/content/grammar-map.md`.
- `audio_gen.py`: `collect_final(test)` gathers the listening clips, and `run()` also rewrites `final_test.json`. Add `test_audio_gen.test_final_test_clips`.
- `ContentAssetsTest.finalTestIsValid` (counts, id pattern, no validator errors, audio files exist). `FlagRepository`: `lessonOfRef` maps `final_*` to `final`; the export header for that group is `[final] Yakuniy test`.
- [ ] Run the failing tests, write content and code, run `python tools/audio_gen.py`, get to GREEN, then commit `content: final TOPIK I-style test (30 questions)`.

### Task 3: Rules and session
- `GameRules.topikLevel(percent: Int): Int`; `GameRepository.REASON_FINAL`; `GameRules.XP_FINAL = 100`; `FINAL_TEST_ID = "final_test"`.
- `SessionMode.FINAL("final")`; `SessionBuilder.finalTest(t: FinalTest): List<ExerciseItem>` builds `Authored(e, emptyList())`, listening first.
- `SessionState.final: FinalScore?` with `data class FinalScore(val listening: Int, val listeningTotal: Int, val reading: Int, val readingTotal: Int) { val percent; val level }`.
- In FINAL mode the controller awards no XP per answer. It records correctness per index. `timeUp()` jumps to the end and wraps up. `wrapUp` computes the `FinalScore`, submits the best score and awards the bonus when the previous best was < 70 and the new percent is ≥ 70.
- [ ] Failing tests:
  - `GameRulesTest.topikLevel`;
  - `SessionBuilderTest.finalKeepsOrder`;
  - `SessionControllerTest`: `finalGivesNoAnswerXp`, `finalScoresSections`, `timeUpEndsTest`, `bonusOnlyOnce`.

  Implement until GREEN, then commit `feat(session): timed final test mode with section scores`.

### Task 4: UI
- Map: a "Yakuniy sinov" card, placed at the top of the System tower and at the end of the Neon metro line. It shows the question count, the time, the best score and the recommendation note (completed lessons N/18), with a start button. `BookMapViewModel` gets the best score and the completed count.
- `SessionScreen` in FINAL mode:
  - shows the countdown (mm:ss) and calls `vm.timeUp()` at 0;
  - shows the result card with the total, sections, percent and level text;
  - hides combo and XP chips.
- Route `session/final`, strings.
- [ ] `assembleDebug` and the suite pass; commit `feat(ui): final test card on the map, timer and result`.

### Task 5: Docs
- [ ] Main spec status; commit.
