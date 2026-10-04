# Stage 10a — Glass Kit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle the shared UI kit to the "Glass hero" language (no outlines, tonal and glass surfaces, Sora/Unbounded, spring presses, mesh backdrop, floating glass dock) without changing kit APIs, so every screen picks up the new look.

**Architecture:** Tokens grow surface/raised/glass colours and two font roles. `GameBackground` draws a drifting colour mesh and is the Haze source. A `Modifier.glass()` helper blurs it behind panels, chips and the dock. Kit composables keep their names and parameters; only their internals change. Screens are restyled in 10b–10d.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2026.09.00), Haze 2.0.1 (`haze` + `haze-blur`), Robolectric unit tests.

**Spec:** `docs/superpowers/specs/2026-10-04-stage10-modern-ui-design.md`

## Global Constraints

- Kit function names and parameters stay; callers must compile unchanged.
- Colours come from `GameTokens` only. Semantic colours (`CorrectGreen`, `WrongRed`, `Gold`, `danger`) are unchanged.
- System tokens: base `#07070F`, surface `#15103A`, accent `#A98BFF`, accent2 `#5CC8FF`, text `#F2F0FF`, muted `#B9B2E6`.
- Neon tokens: base `#0A0613`, surface `#1A0B2A`, accent `#FF3D9A`, accent2 `#2EE6FF`, text `#FFF0F8`, muted `#C9B3D9`.
- Radii: cards 24dp, sheets 28dp, controls 16dp, chips are pills.
- The type scale is 34 / 24 / 18 / 15 / 13 / 11 sp. Sentence case; capitals only on 11sp labels (tracking 0.12em).
- Press springs to 0.95 scale (stiffness 600, damping ratio 0.5). `LocalReducedMotion` turns off every animation.
- Glass:
  - blur 22dp, tint = the theme surface at 55 % alpha, noise 0.05;
  - below API 31 there is no blur and the tint alpha is at least 0.7.
- Haze coordinates: `dev.chrisbanes.haze:haze:2.0.1` and `dev.chrisbanes.haze:haze-blur:2.0.1`.
  - Source: `Modifier.hazeSource(state)`.
  - Effect: `Modifier.hazeBlur(HazeInput.Sources(state), HazeBlurStyle { blurRadius(..); noiseFactor(..); colorEffects(listOf(HazeColorEffect.tint(c))); fallbackColorEffect(HazeColorEffect.tint(c2)) })`.
- New fonts are OFL and listed in `docs/CREDITS.md`. Build with `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`.
- Commits carry no Co-Authored-By line.

## Review Focus

- **Long labels:** chips and rail tiles with long Uzbek or English labels ("Takrorlash kutyapti", "Mistakes") ellipsize instead of clipping or overflowing. Test: `HudChip` with a long label at 360dp width keeps one line (screenshot check in Task 8).
- **Contrast:** text and muted text stay readable on glass when the backdrop behind is bright (hero art). Test: `contrastRatio(text, surface) >= 4.5` and `contrastRatio(muted, surface) >= 3.0` for both themes (Task 1).
- **Old Android:** below API 31 nothing breaks and the glass is an opaque-enough tint. Test: `glassTintAlpha(30) >= 0.7f`, `glassTintAlpha(31) == 0.55f` (Task 2).
- **Reduced motion:** buttons don't scale and the mesh is still. Test: `pressScale(pressed = true, reduced = true) == 1f` (Task 4).
- **Theme switch at runtime:** tokens, glass tint and mesh colours follow `LocalGameTokens` without a restart. Manual check in Task 8: switch the theme in Settings and Home recolours at once.

---

### Task 1: Fonts and tokens

**Files:**
- Add: `app/src/main/res/font/sora_regular.ttf`, `sora_semibold.ttf`, `sora_extrabold.ttf`, `unbounded_semibold.ttf`, `unbounded_extrabold.ttf` (static TTFs from the Google Fonts CSS API, as done for Cinzel)
- Modify: `app/src/main/java/uz/hangulfriend/ui/theme/GameTheme.kt`
- Modify: `docs/CREDITS.md`
- Test: `app/src/test/java/uz/hangulfriend/ui/GameTokensTest.kt`

**Interfaces:**
- Produces: `GameTokens` gains `surface: Color`, `raised: Color`, `glassTint: Color`, `ui: FontFamily`, `numbers: FontFamily`. `display` stays (now Sora 800). `bracketTitles = false` and `panelCorner = 24.dp` in both themes. `panelBorder` becomes a hairline: white at 10 %. Also `fun contrastRatio(a: Color, b: Color): Float` (WCAG relative luminance) in `GameTheme.kt`.
- `gameShapes(t)` returns rounded shapes for both themes (8/12/16/24/28dp).
- `gameTypography` uses `t.ui` for every style, with sizes from the type scale.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun textReadsOnSurface() = GameThemeId.entries.forEach { id ->
    val t = tokensFor(id)
    assertTrue("$id text", contrastRatio(t.text, t.surface) >= 4.5f)
    assertTrue("$id muted", contrastRatio(t.muted, t.surface) >= 3.0f)
}
@Test fun systemPalette() = with(tokensFor(GameThemeId.SYSTEM)) {
    assertEquals(Color(0xFF07070F), background); assertEquals(Color(0xFFA98BFF), accent); assertEquals(Color(0xFF5CC8FF), accent2)
}
@Test fun neonPalette() = with(tokensFor(GameThemeId.NEON)) {
    assertEquals(Color(0xFF0A0613), background); assertEquals(Color(0xFFFF3D9A), accent); assertEquals(Color(0xFF2EE6FF), accent2)
}
@Test fun noBracketTitles() = GameThemeId.entries.forEach { assertFalse(tokensFor(it).bracketTitles) }
@Test fun contrastOfBlackOnWhiteIs21() = assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.01f)
```

- [ ] **Step 2: Run them.** `gradlew.bat testDebugUnitTest --tests "*GameTokensTest*"`. Expected: they fail to compile (no `surface`, no `contrastRatio`).
- [ ] **Step 3: Implement the token changes, the fonts, `contrastRatio`, the shapes and the typography.** `top` stays the theme's third highlight: System `#A98BFF`, Neon `#FFE45C`. `raised` = `lerp(surface, text, 0.06f)`. `glassTint` = `surface.copy(alpha = 0.55f)`.
- [ ] **Step 4: Run all unit tests.** Expected: PASS. Existing tests that assert old colours (if any) are updated to the new values.
- [ ] **Step 5: Commit.** `feat(ui): stage 10a tokens, Sora and Unbounded fonts`

### Task 2: Mesh backdrop and glass

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts` (add Haze)
- Modify: `app/src/main/java/uz/hangulfriend/ui/theme/GameComponents.kt` (`GameBackground`, `Backdrop`)
- Create: `app/src/main/java/uz/hangulfriend/ui/kit/Glass.kt`
- Test: `app/src/test/java/uz/hangulfriend/ui/GlassTest.kt`

**Interfaces:**
- Produces:
  - `val LocalHazeState: ProvidableCompositionLocal<HazeState?>`, provided by `Backdrop`.
  - `fun glassTintAlpha(sdk: Int): Float`: 0.55f for API 31+, 0.78f below.
  - `fun Modifier.glass(shape: Shape, tint: Color? = null): Modifier`. It clips to `shape` and applies `hazeBlur` from `LocalHazeState` with the global glass style. With no state it falls back to `background(tint)`. It always draws the 1px inner top highlight (white 10 %).
  - `fun meshBlobs(id: GameThemeId): List<MeshBlob>`, where `data class MeshBlob(val x: Float, val y: Float, val radius: Float, val color: Color, val drift: Float)`. Two or three blobs per theme, in accent and accent2.
- `Backdrop` layers:
  1. base colour;
  2. a sibling `Canvas` with `hazeSource(state)` that draws the blobs, drifting with a frame clock read only in draw (as `GateScene` does) and still under reduced motion;
  3. the content.
- The grid lines and the old Neon glow go.

- [ ] **Step 1: Write the failing tests.** `glassTintAlpha(30) >= 0.7f`; `glassTintAlpha(31) == 0.55f`; `meshBlobs` gives 2–3 blobs per theme, every blob colour is the theme's accent or accent2 (at any alpha), and every radius is in 0.3..0.8.
- [ ] **Step 2: Run them.** Expected: they fail to compile.
- [ ] **Step 3: Add Haze, `Glass.kt` and the new `Backdrop`.**
- [ ] **Step 4: Run all tests and `assembleDebug`.** Expected: PASS.
- [ ] **Step 5: Commit.** `feat(ui): mesh backdrop and Haze glass`

### Task 3: Shapes, panels and titles

**Files:**
- Modify: `ui/kit/Shapes.kt`, `ui/kit/HuntPanel.kt`, `ui/kit/Motion.kt` (`scanLine`)
- Test: the existing `CutShape` test (if any) is kept or deleted together with `CutShape`.

**Interfaces:**
- `GameTokens.shape(cut: Dp = 10.dp): Shape` returns `RoundedCornerShape(if (cut >= 10.dp) 24.dp else 16.dp)`. `cutShape(cut)` returns the same.
- `cornerTicks()` and `scanLine()` become no-ops: the Modifier is returned unchanged and is deleted in 10d once no caller is left.
- `shapeGlow` stays as is; screens drop it in 10b–10d.
- `HuntPanel` becomes `Modifier.glass(RoundedCornerShape(24.dp))` with padding and no border. `accent` only tints the title. `scan` is ignored.
- `PanelTitle` shows `title` as given in `t.ui` SemiBold 15sp, with no brackets and no `uppercase()`.

- [ ] **Step 1: Write the failing test.** `GameTokens.shape()` is a `RoundedCornerShape` for both themes.
- [ ] **Step 2: Run it.** Expected: FAIL (System returns `CutShape`).
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run tests and assemble.** Expected: PASS.
- [ ] **Step 5: Commit.** `feat(ui): rounded glass panels, plain titles`

### Task 4: Button

**Files:**
- Modify: `ui/kit/HuntButton.kt`, `ui/kit/Motion.kt`
- Test: `app/src/test/java/uz/hangulfriend/ui/HuntButtonStyleTest.kt`

**Interfaces:**
- Produces: `fun pressScale(pressed: Boolean, reduced: Boolean): Float` (0.95f / 1f), and `fun Modifier.springPress(interaction: MutableInteractionSource): Modifier` (animates `pressScale` with `spring(dampingRatio = 0.5f, stiffness = 600f)`), both in `Motion.kt`.
- `HuntPalette` becomes `(fill: Color, text: Color)`. The palettes per style:
  - PRIMARY: fill `t.text`, text `t.background`;
  - SECONDARY: glass, text `t.text`;
  - GOLD: `#F5C451` with text `#2A1A00`;
  - SUCCESS: `CorrectGreen` with text `#04140A`;
  - DANGER: `t.danger` with text white.
- `HuntButton` keeps its signature. Its look: a 16dp rounded fill, no lip or rim or glow, label in `t.ui` 800 at `fontSize`. The disabled state is the fill at 40 % alpha. Sound and haptic stay.

- [ ] **Step 1: Write the failing tests.** `pressScale(true, false) == 0.95f`; `pressScale(true, true) == 1f`; `huntPalette(PRIMARY, t).fill == t.text` for both themes; `contrastRatio(p.text, p.fill) >= 4.5f` for every style and both themes, except SECONDARY, which is checked against `t.surface`.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run tests and assemble.** Expected: PASS.
- [ ] **Step 5: Commit.** `feat(ui): flat spring-press buttons`

### Task 5: HUD, controls and screen kit

**Files:**
- Modify: `ui/kit/Hud.kt` (`LevelBadge`, `GlowBar`, `HudChip`, `RailButton`), `ui/kit/Controls.kt` (`HuntToggle`, `SegmentSlider`), `ui/kit/Screen.kt` (`ScreenHeader`, `HuntChip`, `SystemDialog`, `DiamondMark`, `SelectRow`, `IconAction`)

**Interfaces (signatures unchanged):**
- `GlowBar` is a 6dp rounded track (white at 10 %) with an `accent2 → accent` gradient fill. It has no outer glow.
- `HudChip` and `HuntChip` are glass pills, 13sp, single line with ellipsis. A selected `HuntChip` is filled with accent and has `t.background` text.
- `RailButton` is a 16dp glass tile with an icon and an 11sp label, using `springPress`.
- `LevelBadge` is a round glass badge with the level in `t.numbers`.
- `HuntToggle` is a pill track in the accent when on. `SegmentSlider` gets rounded segments.
- `ScreenHeader` has a round glass back button and a title in `t.ui` 800 24sp, sentence case.
- `SystemDialog` is a 28dp glass sheet over a 60 % black scrim.
- `DiamondMark` is a small rounded square in the accent.
- `SelectRow` is a raised 16dp row with an accent check. `IconAction` is a round accent button.

- [ ] **Step 1:** Restyle each file. These are pure visual changes with no new logic, so they get no new unit tests; the existing suite guards behaviour.
- [ ] **Step 2: Run all tests and `assembleDebug`.** Expected: PASS.
- [ ] **Step 3: Commit.** `feat(ui): glass HUD, controls and screen kit`

### Task 6: Battle kit

**Files:**
- Modify: `ui/kit/Battle.kt`

**Interfaces (signatures unchanged):**
- `OptionTile`:
  - a raised 20dp card with a round letter badge;
  - states recolour the fill: PICKED is accent at 25 %, RIGHT is `CorrectGreen` at 30 %, WRONG is `WrongRed` at 30 %, DIM is 50 % alpha;
  - no border; `springPress`.
- `FloorBar` is rounded segments, or a rounded track when there are more than 20 items.
- `HeartRow`, `ComboChip`, `FloatingText` and `EdgeFlash` keep their behaviour and use `t.ui` / `t.numbers` and pill shapes.
- `GateRays` is unchanged.

- [ ] **Step 1:** Restyle.
- [ ] **Step 2: Run all tests and `assembleDebug`.** Expected: PASS.
- [ ] **Step 3: Commit.** `feat(ui): glass battle kit`

### Task 7: Dock

**Files:**
- Modify: `app/src/main/java/uz/hangulfriend/ui/Dock.kt`

**Interfaces:**
- `Dock` keeps its signature. Its look:
  - a floating glass pill, 16dp from the sides and the bottom, 64dp tall;
  - tab items are an icon plus an 11sp label, with the active tab in the accent;
  - `HuntDiamond` becomes a 56dp raised accent circle with a bolt icon, half above the pill, with `springPress`.
- Screens that pad for the dock keep working, because the dock height plus the bottom gap stays at or below the old dock height. Check in Task 8.

- [ ] **Step 1:** Restyle.
- [ ] **Step 2: Run all tests and `assembleDebug`.** Expected: PASS.
- [ ] **Step 3: Commit.** `feat(ui): floating glass dock`

### Task 8: Verify and open the PR

- [ ] **Step 1:** Run `gradlew.bat -q testDebugUnitTest assembleDebug`. Expected: green, and the test count is higher than 389.
- [ ] **Step 2:** On the emulator (start it with `-memory 2048` if needed), take screenshots of Home, Map, a lesson, a battle item, Vocabulary and Settings in System + Uzbek and in Neon + English. Check:
  - no clipped labels;
  - nothing hidden behind the dock;
  - the glass blurs the mesh;
  - switching the theme recolours at once.
- [ ] **Step 3:** Run `dumpsys gfxinfo` on Home and note the 50th and 90th percentile.
- [ ] **Step 4:** Push the branch and open a PR "Stage 10a: glass kit". It lists what changed, the screenshots and the known leftovers (screen-level `uppercase()`, `shapeGlow`, Home layout), which are for 10b–10d.
