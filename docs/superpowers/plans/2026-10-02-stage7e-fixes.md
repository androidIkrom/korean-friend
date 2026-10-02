# Stage 7e — Deferred Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans.

**Goal:** Fix the deferred minors listed in the spec, adding a failing test first wherever logic is involved.
**Spec:** `docs/superpowers/specs/2026-10-02-stage7e-fixes-design.md`
**Constraints:** no behaviour change beyond the fixes; the suite stays green; no subagents; no `Co-Authored-By`.

### Task 1: Logic fixes with tests (spec #1, #5, #6)
- #1: `class GradeOnce { fun shouldGrade(item: ExerciseItem): Boolean }` (each item once). `LessonViewModel.grade` uses it.
- #5: in the `LessonValidator` story rules, a `choose_reply` whose speaker is not `aziz` is an error `story: choose_reply speaker must be aziz`.
- #6: `ReminderPolicy.shouldDisable(enabled: Boolean, permitted: Boolean): Boolean`. `ReminderRow` checks it on resume and turns the reminder off.
- [ ] Failing tests:
  - `GradeOnceTest`: the same item is graded once, a different item is graded;
  - `StoryValidationTest.chooseReplyMustBeAziz`;
  - `ReminderPolicyTest.disableWhenPermissionRevoked` (true only when enabled and not permitted).

  Implement, get to green, then commit `fix: grade lesson checks once; validate reply speaker; drop reminder without permission`.

### Task 2: Remaining fixes (spec #2–#4, #7–#9)
- [ ] Apply them, build and run the suite, then commit `fix: empty pager, slider rounding, dead code, dark window theme, avatar paints, vocab duplicate check`.

### Task 3: Docs
- [ ] Add the status line to the main spec and commit.
