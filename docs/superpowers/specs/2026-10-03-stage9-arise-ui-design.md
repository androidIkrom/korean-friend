# Stage 9 — "Arise" game UI (design)

## Goal

Make Hangul Hunt look and feel like a mobile game built by a senior game team, in the Solo Leveling: Arise style:

- Chamfered 3D buttons that sink when pressed
- `[SYSTEM]` windows
- A hunter lobby with a raised centre button in the dock
- Portal transitions between screens
- Rewards that land with impact: combo, `+XP`, rank letter, stars
- Short sound effects and haptics

Learning logic, content and the database stay as they are.

Approved mockups: https://claude.ai/artifact/4XkdHjaRo7xVRv2qEPDt3S. Navigation follows variant **A · Hunter Lobby**.

## Decisions

1. **One kit, two themes.**
   - Every screen is built from `ui/kit`.
   - The shape comes from the theme: "Tizim" uses a chamfer (two opposite corners cut), "Neon Seul" uses its rounded corners.
   - Colours come from `GameTokens`, so both themes upgrade together.
2. **Existing helpers become the kit.**
   - `GameButton`, `GamePanel` and `GameCard` keep their signatures but draw with the kit.
   - So every screen improves in 9a, before it gets its own redesign.
3. **Sound and haptics are on by default.** Each has its own switch in Settings and both are part of the backup.
   - **Sounds:** 8 SFX are synthesized by `tools/sfx_gen.py`, so they are original work with no licence question. They are stored as 16-bit mono WAV in `res/raw/` and played through `SoundPool`.
   - **Haptics:** `VibrationEffect` presets on API 29+, and a short one-shot below that.
4. **Motion follows the phone.** When the system animator scale is 0 ("Remove animations"), ambient loops stop and transitions become instant. Ambient loops are the aura, scan line and pulse.
5. **Dock:** LOBBY · GATES · ◆ HUNT · STORY · SYSTEM.
   - ◆ HUNT opens the current lesson, or the map when there is none.
   - Review, words, mistakes, arena, badges and share are side rails in the lobby.
6. **Lesson stages become gate floors (9c), but nothing is locked.** Floors show cleared state only, so the learner can still open any stage, as today.

## Sub-stages (one PR each)

### 9a — Kit, feedback, lobby, system menu

- `ui/kit`:
  - `GameShapes`: `cutShape()` and the theme's `shape(cut)`.
  - `HuntButton`: styles PRIMARY, SECONDARY, GOLD, SUCCESS, DANGER. A 3D lip, a 4 dp press, and a tap sound and haptic.
  - `HuntPanel`: chamfered glass with a glowing border and an optional scan line.
  - `Hud`
  - `Dock`
  - `RailButton`
  - `SegmentSlider`
  - `HuntToggle`
  - `Motion`: durations, `rememberReducedMotion()`, `Modifier.breathing()`, `Modifier.pulseRing()`, `Modifier.scanLine()`.
- `GameFeedback`:
  - `Sfx { TAP, CORRECT, WRONG, COMBO, XP, CLEAR, RANK_UP, OPEN }`.
  - Implemented with `SoundPool` and `Vibrator`, gated by settings.
  - Exposed as `LocalGameFeedback`, which is a no-op in tests and previews.
- `SettingsRepository`: `soundOn` and `hapticsOn`, DataStore keys `sound` and `haptics`, both default true. The backup gets matching `sound` and `haptics` fields, defaulting to true when an old file lacks them.
- Navigation:
  - The custom `Dock` replaces the Material bar.
  - NavHost transitions: tabs cross-fade with a 24 dp slide; pushed screens use a "portal" scale from 0.92 with a fade; pop is the reverse.
- Home becomes the **Hunter Lobby**:
  - HUD with level diamond, rank title, XP bar, and streak/due/badges chips
  - The hero scene in the centre with a breathing aura
  - Rails: left QUEST (scrolls to the quests), WORDS, ERRORS; right ARENA, BADGES, SHARE
  - A compact daily-quest panel with a scan line
  - An ENTER GATE button with a pulse
- Settings becomes a `[SYSTEM]` window with sections: Language, Interface, Hero, Daily target, Feedback (sound, haptics, reminder), Data, Content reports.

### 9b — Battle

- Exercises:
  - The question sits in a `[SYSTEM] QUEST` card.
  - Answers are 3D option buttons with letters.
  - A floor progress bar with hearts and a combo chip.
- Correct answer: a green flash with a glow, `+XP` floats up, the combo pops, CORRECT/COMBO sound.
- Wrong answer: shake, a red edge flash, the heart cracks, WRONG sound.
- A new **Gate Cleared** result screen:
  - Spinning rays and the rank letter dropping in
  - Stars landing one by one, and the XP bar counting up
  - New words shown as loot, CLAIM button, CLEAR sound
- The rank-up dialog gets the RANK_UP sound and an aura flare.

### 9c — Gates

- The lesson screen becomes the portal with F1–F5 floors; the test floor is the BOSS.
- The map and the story episode are restyled with the kit.

### 9d — Remaining screens

- Vocabulary, Arena (games), Badges, Mistakes
- Onboarding as "Awakening"
- The tutor sheet as a `[SYSTEM]` window
- The final test

## Testing

- **Pure units:**
  - Chamfer outline, the `Dock` tab for a route, the HUNT target
  - Feedback gating (sound/haptics flags, reduced motion)
  - Settings and backup round trip for `sound` and `haptics`
  - Every `Sfx` has its raw file
- **Robolectric:** the kit composables render in both themes.
- **Emulator, after each sub-stage, in both languages and both themes:**
  - Every screen of that sub-stage
  - Press feedback, transitions, sounds on and off, and Remove animations
