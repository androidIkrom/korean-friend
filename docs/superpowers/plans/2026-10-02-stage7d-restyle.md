# Stage 7d — Restyle Remaining Screens Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Bring every remaining screen to the game look without changing behaviour.
**Spec:** `docs/superpowers/specs/2026-10-02-stage7d-restyle-design.md`

## Global Constraints
- No behaviour change; the full suite must stay green.
- Use the theme tokens only, with no hard-coded colours except `CorrectGreen`/`WrongRed`.
- No subagents, no worktrees, no `Co-Authored-By`.
- Test: `./gradlew :app:assembleDebug :app:testDebugUnitTest`.

### Task 1: Shapes, shared backdrop, GameCard
- Add `fun gameShapes(t: GameTokens): Shapes` and pass it to `MaterialTheme` in `HangulFriendTheme`.
- Add `LocalInGameBackground`; a nested `GameBackground` only lays out its content.
- Wrap the Nav `Scaffold` in `GameBackground` with `containerColor = Color.Transparent`.
- Add `GameCard(modifier, onClick: (() -> Unit)? = null, enabled = true, containerColor: Color? = null, borderColor: Color? = null, content: ColumnScope.() -> Unit)` in `GameComponents.kt`.
- [ ] Write the failing test `GameThemeTest.shapesFollowTheme`, implement, run until green, then commit `feat(theme): game shapes, shared backdrop and GameCard`.

### Task 2: Replace cards and restyle screens
- Replace `Card` with `GameCard` in `Stages.kt` (word, grammar), `Roleplay.kt`, `Parts.kt` (FeedbackPanel: container = verdict colour α .15, border = verdict colour), `Games.kt`, `GameScreens.kt` (memory cards), `AchievementsAndMistakes.kt` and `StoryList.kt`.
- In the session, replace the progress indicator with `ProgressBar` and the heart emoji with icons. In StoryList, replace the emoji with icons.
- [ ] Build and run the suite, then commit `feat(ui): game-style cards and session bar across remaining screens`.

### Task 3: Docs
- [ ] Add the status line to the main spec and commit.
