# Stage 9c — Gates (lesson floors, map, episode) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:**
- The lesson screen becomes a gate with five floors: F1 words, F2 grammar, F3 dialogue, F4 practice, and the BOSS test.
- The map and the story episode are rebuilt from the kit.

**Architecture:**
- A pure `floorStates(stage, status)` decides how each floor looks: CLEARED, CURRENT or OPEN.
- Nothing is locked, so every floor stays tappable.
- The screens swap Material widgets for kit pieces:
  - `HuntButton`, `HuntPanel`, `OptionTile`, `FloorBar`, `GlowBar`, `HuntToggle`
  - `pulseRing`, `GateRays`

**Spec:** `docs/superpowers/specs/2026-10-03-stage9-arise-ui-design.md` (sub-stage 9c)

## Global Constraints

- No change to the DB, the lesson flow, grading or XP.
- Floors are never locked.
- Every animation respects `LocalReducedMotion`.
- New strings go in `values` and `values-en`.

## Review Focus

1. **Going back to an earlier floor.** That floor becomes CURRENT, and floors after it show OPEN, not CLEARED, unless the lesson is complete. `LessonControllerTest.floorStates` covers it.
2. **A completed lesson.** Every floor except the current one shows CLEARED. Covered by the same test.
3. **The floor rail at 360 dp width in Uzbek.** The labels must fit. Check this on the emulator.
4. **A story choice where every wrong option is already eliminated.** Only the answer stays tappable, and it must still play the CORRECT sound.
5. **The map in both skins.** The System tower and the Neon metro must keep their layouts. Check this on the emulator.

### Task 1: Floor logic

- **Files:**
  - `ui/lesson/LessonController.kt`:
    - `enum FloorState { CLEARED, CURRENT, OPEN }`
    - `fun floorStates(stage: Int, status: LessonStatus): List<FloorState>`
    - `LessonState.status`
- **Rule:** for each floor `i` in `0..4`:
  - CURRENT when `i == stage`;
  - CLEARED when the lesson is COMPLETED or VERIFIED, or when `i < stage`;
  - OPEN otherwise.
- **Tests:** `LessonControllerTest`: `floorStatesFollowStage`, `completedLessonClearsAll`, and `loadExposesStatus`.

### Task 2: Lesson gate screen

- **Header:**
  - "GATE u-l" in small accent letters, then the Korean title and the localized title.
  - A `FloorRail` of 5 chamfered floor tiles (F1…F4 and BOSS) joined by a line that is lit up to the last cleared floor.
  - The current tile glows and pulses.
  - The BOSS tile uses the danger colour.
  - Every tile is a button that opens its floor.
- **Pager:**
  - The page count shows as "1 / 8" over a `GlowBar`.
  - Back is a SECONDARY HuntButton and Next is a PRIMARY HuntButton.
- **Words:** each word card is a `HuntPanel` with the accent rim.
- **Grammar:**
  - The tutor button is SECONDARY with an AI icon.
  - "Check yourself" uses `PanelTitle`.
- **Dialogue:**
  - The Listen and Roleplay modes are two HuntButtons, PRIMARY for the selected one and SECONDARY for the other.
  - The translation switch is a `HuntToggle`.
  - The speech bubbles use the theme shape.
- **Start floors:**
  - Practice is a portal panel with a PRIMARY button.
  - The test is the BOSS panel: danger rim, a "BOSS" title, and a DANGER button with a `pulseRing`.
- **Lesson review:** a SECONDARY HuntButton.
- **Strings:** `gate_code` ("DARVOZA %1$d-%2$d" / "GATE %1$d-%2$d") and `floor_boss` ("BOSS" in both languages).

### Task 3: Map

- **Unit rows:**
  - Compact rows use the theme shape and play the tap sound.
  - The open unit is a `HuntPanel` with a scan line when it is ACTIVE.
- **Lessons:** each lesson row uses a `GlowBar`.
- **Buttons:**
  - Quick check is a small SECONDARY HuntButton.
  - Boss is a DANGER HuntButton with a sword icon.
  - Enter is PRIMARY with a `pulseRing`.
- **Final card:** a GOLD button.
- **Header:** the map title is shown in the panel-title style, with the cleared count.

### Task 4: Episode

- **Progress:** the progress line is a `FloorBar` of `shown/total`.
- **Choices:**
  - Choices are `OptionTile`s lettered A, B, C…
  - An eliminated option is WRONG with a shake, then turns DIM.
  - Picking an option plays CORRECT or WRONG.
- **Line panels:** they keep their accents.
- **Finish:**
  - Rays behind a gold "+XP".
  - The CLEAR sound plays once when XP was earned.
  - A GOLD close button.

### Task 5: Verify

- Run all unit tests.
- Check on the emulator:
  - the lesson floors and pager;
  - the dialogue;
  - the practice and BOSS floors;
  - the map in System and Neon;
  - an episode, in Uzbek and in English.
