# Stage 9a — Hunter Kit, Feedback, Lobby, System Menu — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the game UI kit (chamfered 3D buttons, panels, HUD, dock, motion, sound and haptics), then use it for the Hunter Lobby (Home) and a `[SYSTEM]` Settings window.

**Architecture:**
- The new package `ui/kit` holds the components.
- The existing `GameButton`, `GamePanel` and `GameCard` delegate to the kit, so every screen improves at once.
- `GameFeedback` plays the SFX, which are generated WAV files in `res/raw`, and the haptics. It reads the `sound` and `haptics` settings.

**Tech Stack:** Compose (foundation, animation), SoundPool, Vibrator, DataStore, Python for SFX synthesis.

**Spec:** `docs/superpowers/specs/2026-10-03-stage9-arise-ui-design.md` (sub-stage 9a)

## Global Constraints

- Learning logic, content and the DB stay unchanged. The backup gets two settings fields that default to true.
- Both themes come from tokens:
  - System uses a chamfer with a cut of `panelCorner + 8 dp`.
  - Neon uses `RoundedCornerShape(panelCorner)`.
- When the system animator scale is 0, no infinite animation runs and transitions are instant.
- All new strings go in `values` and `values-en`, so `StringsParityTest` keeps passing.
- Apostrophes are added through the Edit tool or Python. Build with `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`.

## Review Focus

1. **Older devices without `VibrationEffect` presets (API < 29).** The fallback is a 20 ms one-shot. `GameFeedbackTest.hapticFallback` covers it.
2. **Sound off but haptics on, and the reverse.** Each flag works on its own. `FeedbackPolicyTest` covers it.
3. **The lobby on short screens (640 dp).** The content scrolls, and nothing overlaps the dock. Check it on the emulator.
4. **Tapping HUNT with no current lesson.** It opens the map. `DockTest.huntTarget` covers it.
5. **Restoring a backup made before stage 9.** Sound and haptics stay on. `BackupCodecTest.oldFileDefaultsFeedbackOn` covers it.

### Task 1: Kit shapes, motion, buttons, panels

- **Files:** `ui/kit/Shapes.kt`, `ui/kit/Motion.kt`, `ui/kit/HuntButton.kt`, `ui/kit/HuntPanel.kt`. Modify `ui/theme/GameComponents.kt` so that `GameButton`, `GamePanel` and `GameCard` delegate to the kit.
- **Produces:**
  - `fun cutShape(cut: Dp): Shape`: top-left and bottom-right corners cut.
  - `@Composable fun GameTokens.shape(cut: Dp = …): Shape`
  - `enum class HuntStyle { PRIMARY, SECONDARY, GOLD, SUCCESS, DANGER }`
  - `@Composable fun HuntButton(text: String, onClick: () -> Unit, modifier, style = PRIMARY, enabled = true, icon: ImageVector? = null, sfx: Sfx = Sfx.TAP)`
  - `@Composable fun HuntPanel(modifier, title: String? = null, accent: Color? = null, scan: Boolean = false, content: ColumnScope.() -> Unit)`
  - `@Composable fun rememberReducedMotion(): Boolean`
  - `Modifier.breathing()`, `Modifier.pulseRing(color)`, `Modifier.pressDepth(pressed)`
- **Test:** `KitShapeTest.cutCornersAreCut`. On a 100×50 box with a 10 px cut, the outline does not contain (1, 1) or (99, 49), but contains (50, 25), (99, 1) and (1, 49).

### Task 2: Feedback (SFX and haptics)

- **Files:**
  - `tools/sfx_gen.py` with a test `tools/test_sfx_gen.py`
  - `res/raw/sfx_{tap,correct,wrong,combo,xp,clear,rank_up,open}.wav`
  - `ui/kit/Feedback.kt`
  - `data/SettingsRepository.kt`: `soundOn`, `hapticsOn`, `setSound`, `setHaptics`
  - `data/Backup.kt`: `sound` and `haptics` fields
  - `MainActivity.kt`: provides `LocalGameFeedback`
- **Produces:**
  - `enum class Sfx(val raw: Int)`
  - `fun interface GameFeedback { fun play(sfx: Sfx) }` with a no-op default
  - `class AndroidGameFeedback(context, scope, settings: Flow<Settings>)`
  - `object FeedbackPolicy { fun sound(s): Boolean; fun haptic(sfx): HapticKind }`
- **Tests:**
  - `FeedbackPolicyTest`
  - `SettingsRepositoryTest.soundAndHapticsDefaultOnAndRoundTrip`
  - `BackupCodecTest.oldFileDefaultsFeedbackOn`
  - `SfxAssetsTest.everySfxHasRawFile`
  - Python `test_sfx_gen`: every clip is a valid mono 16-bit WAV of 0.05–1.2 s and under 60 KB

### Task 3: Dock and transitions

- **Files:** `ui/kit/Dock.kt`; modify `ui/Nav.kt`.
- **Produces:**
  - `enum class DockTab(route, label, icon) { LOBBY, GATES, STORY, SYSTEM }`
  - `fun dockTabFor(route: String?): DockTab?`
  - `fun huntTarget(currentLessonId: String?): String`: the lesson route, or `Routes.MAP` when there is no current lesson
  - The NavHost gets enter, exit, popEnter and popExit transitions:
    - Between tabs: a fade of 220 ms.
    - Pushed screens: scaleIn from 0.92 with a fade of 320 ms.
    - With reduced motion: none.
- **Test:** `DockTest`.

### Task 4: Hunter Lobby

- **Files:** `ui/home/Home.kt`, rewritten in place. Keep `HomeViewModel`; replace the layout.
- **Layout:**
  - HUD: level diamond, rank title and XP, then chips for streak, due and badges.
  - Hero scene with breathing.
  - Two rails of `RailButton`:
    - Left: QUEST, WORDS, ERRORS.
    - Right: ARENA, BADGES, SHARE. ShareCardButton becomes a rail action.
  - The quest panel with a scan line.
  - An ENTER GATE `HuntButton` with a pulse ring, showing the current gate.
  - The column scrolls and has bottom padding for the dock.
- **New strings:** `rail_quest`, `rail_words`, `rail_errors`, `rail_arena`, `rail_badges`, `rail_share`, `lobby_enter_gate`, `lobby_gate_label`, `dock_hunt`, plus dock labels.

### Task 5: System menu (Settings)

- **Files:** `ui/settings/Settings.kt` (LazyColumn of `[SYSTEM]` sections), `ui/kit/Controls.kt` (`SegmentSlider`, `HuntToggle`), `ui/settings/FeedbackRow.kt`, and `ui/onboarding/Onboarding.kt` (adds `lessonPickerItems`).
- **New strings:** `settings_section_*`, `settings_sound`, `settings_haptics`.

### Task 6: Verify and ship

- Build and run all tests.
- On the emulator, check the Lobby, the Dock and its transitions, and Settings:
  - in both themes and both languages;
  - with sound off and on;
  - with Remove animations on.
- Push and open the PR.
