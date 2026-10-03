# Stage 9d — Remaining screens — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the remaining screens from the kit:
- Vocabulary
- Arena (games list and the four games)
- Badges
- Mistake notebook
- Stories list
- Onboarding, as the three-step "Awakening"
- The AI tutor, as a `[SYSTEM]` window

The final test already runs as a battle (9b) and starts from the map's gold card (9c), so no change is needed for it here.

**Architecture:**
- New shared pieces in the kit (`ui/kit/Screen.kt`):
  - `ScreenHeader` and `HuntChip`;
  - `SystemDialog`, which replaces the Material `AlertDialog`;
  - `SelectRow`, a lesson row with a diamond mark that replaces the `RadioButton`.
- The game logic gains two pure helpers:
  - `MemoryGame.score(moves)` replaces the inline formula;
  - `SpeedRound.timeFraction(leftMs)` drives the draining timer bar.
- Onboarding steps through `nextStep(step, selected)`.

**Spec:** `docs/superpowers/specs/2026-10-03-stage9-arise-ui-design.md` (sub-stage 9d)

## Global Constraints

- No change to the DB, the rules, XP or the content.
- Every animation respects `LocalReducedMotion`.
- New strings go in `values` and `values-en`.

## Review Focus

1. **Onboarding on a fresh install.**
   - The learner cannot finish before picking a lesson.
   - Back goes to the previous step.
   - The language switch in step 1 restarts the activity, and the flow still starts at step 1.
   - Covered by `OnboardingStepsTest`.
2. **Memory game score.**
   - The best score is 100.
   - The score loses 5 points per move beyond 6 moves and never drops below 10.
   - Covered by `MemoryGameTest.score`.
3. **Speed timer.**
   - The bar drains from 1 to 0 and stays within 0..1.
   - Covered by `SpeedRoundTest.timeFraction`.
4. **The vocabulary add and delete dialogs.** The fields, errors and buttons work as before. Check on the emulator.
5. **The tutor window on a short screen.** The input row stays visible above the keyboard. Check on the emulator.

### Task 1: Kit screen parts and pure helpers

- **`ui/kit/Screen.kt`:**
  - `ScreenHeader(title: String, onBack: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {})`: a back arrow, the upper-case display title and a short glowing underline.
  - `HuntChip(label: String, selected: Boolean, onClick: () -> Unit)`: theme shape, accent when selected, tap sound.
  - `SystemDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit, actions: @Composable RowScope.() -> Unit)`: a `Dialog` holding a `HuntPanel` with a scan line.
  - `SelectRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit)`: a diamond mark that lights up, inside a theme-shaped row.
- **`study/games/MemoryGame.kt`:** `companion fun score(moves: Int): Int`, computed as `(100 - (moves - PAIRS) * 5).coerceAtLeast(10)`.
- **`study/games/SpeedRound.kt`:** `companion fun timeFraction(leftMs: Long): Float`.
- **`ui/onboarding/Onboarding.kt`:** `fun nextStep(step: Int, selected: String?): Int?`.
  - Steps 0 and 1 go forward.
  - Step 2 returns null, which means finish; that is allowed only when a lesson is selected.
  - `ONBOARDING_STEPS = 3`.
- **Tests:**
  - `MemoryGameTest.score`
  - `SpeedRoundTest.timeFraction`
  - `OnboardingStepsTest`

### Task 2: Arena

- **Games list:**
  - The `ScreenHeader` "Arena" with a back button.
  - Each game is a `HuntPanel` card with an icon, the description, a gold best-score chip and a PLAY button. All are disabled when there is no content.
- **Game screens:**
  - The header shows the game title.
  - Memory: tiles flip on rotationY and play TAP. Matched tiles glow green; a mismatch shakes and plays WRONG.
  - Speed:
    - A draining `GlowBar` timer that turns danger under 10 s.
    - The answers are `OptionTile`s.
    - Each answer plays CORRECT or WRONG with an edge flash.
  - Race and chain:
    - The prompt sits in a `QuestCard`.
    - Check is a HuntButton.
    - Each answer plays CORRECT or WRONG.
- **`GameOver`:**
  - Rays behind the score.
  - A gold "+XP".
  - A record badge that pops.
  - The CLEAR sound.
  - Play again is a GOLD button.

### Task 3: Badges, mistakes, stories, vocabulary

- **Badges:** a two-column grid of diamond medallions.
  - Unlocked medallions are gold, glowing and breathing, with a trophy.
  - Locked medallions are muted, with a lock.
  - The title and description sit under each medallion.
- **Mistakes:**
  - The header, then a DANGER practice button.
  - Each mistake is a row in a danger-rimmed panel.
- **Stories:**
  - The header and unit titles in the panel-title style.
  - Episode cards use the theme shape. A NEW episode glows and pulses, a DONE one shows a check, and locked ones are faded.
- **Vocabulary:**
  - `ScreenHeader` with the count.
  - The search field keeps Material.
  - Filters are `HuntChip`s.
  - Each row shows a diamond status mark.
  - The add button is a 3D accent square with the OPEN sound.
  - The add and delete dialogs are `SystemDialog`s with HuntButtons.
  - The word sheet uses a panel colour and a `PanelTitle`.

### Task 4: Awakening onboarding and the tutor window

- **Onboarding:** three steps with a step bar (`FloorBar`).
  1. **Awakening:**
     - `GateRays` behind the "[ SYSTEM ]" line;
     - the title "Awakening" and the intro text;
     - the language picker.
  2. The hero picker.
  3. The lesson picker, built from `SelectRow`s.
  - A Back button (SECONDARY) and a Next button (PRIMARY). On the last step the button reads "ARISE" (GOLD, CLEAR sound).
  - `HeroRow` uses the theme shape.
  - The settings lesson list also uses `SelectRow`.
- **Tutor window:**
  - A `ModalBottomSheet` with an opaque panel colour.
  - The title row reads "[ SYSTEM · AI ]" with a scan line.
  - Bubbles use the theme shape: the user's are accent-tinted, the AI's are panel coloured with an accent2 rim.
  - The send button is a square accent button.
- **Strings:**
  - `awaken_title` ("Uyg'onish" / "Awakening")
  - `awaken_system` ("Tizim sizni tanladi." / "The System has chosen you.")
  - `awaken_arise` ("UYG'ON" / "ARISE")
  - `onboarding_back` ("Orqaga" / "Back")
  - `onboarding_next` ("Keyingi" / "Next")
  - `game_play` ("O'ynash" / "Play")
  - `arena_title` ("Arena" / "Arena")

### Task 5: Verify

- Run all unit tests.
- Check on the emulator in System + Uzbek and Neon + English:
  - each screen above;
  - onboarding, by clearing the app data on the emulator only;
  - Remove animations.
