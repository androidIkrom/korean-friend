# Stage 10: Modern UI ("Glass Hero") — design

**Date:** 2026-10-04 · **Status:** approved in chat, awaiting spec review

## Why

A friend of the user reviewed the app: "the UI is good, but for 2017". The stage 9 "Arise" look leans on
neon outlines around every box, several glowing accents at once, cut-corner shapes, wide-tracked capitals
everywhere, a grid backdrop and 3D buttons with a bottom edge. That is the 2016–2019 neon-HUD style.

Three modern Home directions were shown as live mockups
(<https://claude.ai/artifact/CcgLPTJttEiNUAmZYbzrG1>): A "Glass hero", B "Bento", C "Game lobby". The user
and the friend chose **A**.

## Decisions (from the user)

- **Scope:** the whole app, in four stages (10a–10d), one PR each, each checked on the phone.
- **Themes:** both stay. System becomes violet + blue, Neon Seoul becomes pink + cyan. Both share one structure.
- **Fonts:** Sora for UI text, Unbounded for big numbers and the rank letter. Korean keeps the system font.
- **Glass:** the Haze library (`dev.chrisbanes.haze:haze`, 2.0.1) for real frosted glass. Below Android 12
  it shows a translucent tint without blur.

## Visual language

- **No outlines.** Surfaces separate by tone:
  - background → surface → raised;
  - panels laid over art or the backdrop are glass (Haze blur, about 22dp, saturation 1.3, tint of the theme surface at about 55 %);
  - every panel gets a 1px inner top highlight (white at 8–12 %);
  - borders appear only on focus.
- **Shapes:** cards 24dp, sheets 28dp, controls 16dp, chips are full pills. `CutShape` and `t.shape()`
  cut corners go away; `t.shape(r)` returns a rounded shape.
- **Colour:**
  - **Backdrop:** a deep base with two or three large, slowly drifting colour blobs ("mesh"), which replaces the grid lines. Reduced motion keeps the blobs still.
  - **Accent:** one per theme. It is used for primary actions, progress and key numbers only.
  - **Unchanged:** semantic colours (correct, wrong, gold, danger).
  - **Tokens:**
    - System: base `#07070F`, surface `#15103A`, accent `#A98BFF`, accent2 `#5CC8FF`, text `#F2F0FF`, muted `#B9B2E6`.
    - Neon: base `#0A0613`, surface `#1A0B2A`, accent `#FF3D9A`, accent2 `#2EE6FF`, text `#FFF0F8`, muted `#C9B3D9`.
- **Type:**
  - **Faces:** Sora 400/600/800 for UI text and Unbounded 600/800 for numbers and the rank letter.
  - **Case:** sentence case everywhere. Capitals stay only on tiny labels (11sp, tracking 0.12em). The kit stops forcing `uppercase()`, and the `bracketTitles` token goes.
  - **Scale:** 34 / 24 / 18 / 15 / 13 / 11 sp.
- **Motion:**
  - Presses spring to 0.95 scale (stiffness about 600, damping about 0.5).
  - The hero floats by about 6dp over 6 s, and the aura breathes.
  - Screens change with a short fade and slide (200 ms).
  - `LocalReducedMotion` turns all of this off.
  - SFX and haptics stay as they are.

## Kit (stage 10a)

Kit function names and parameters stay, so screens change little. Internals are rewritten.

- **`GameTokens`:** gains `surface`, `raised` and `glassTint`. It drops `panelBorder`, `bracketTitles` and `panelCorner`. Fonts become `ui` (Sora) and `numbers` (Unbounded), and `display` maps to Sora 800 for compatibility.
- **`GameBackground` / `Backdrop`:** draws the mesh blobs and is the Haze **source** for the whole screen, via `LocalHazeState` provided by `GameBackground`.
- **`HuntPanel`:**
  - It becomes a glass card: Haze effect, 24dp, inner highlight, no border.
  - `PanelTitle` turns into a plain title row with no brackets and no scan line.
- **`HuntButton`:**
  - It loses the 3D bottom edge and becomes a flat fill with the spring press.
  - Styles:
    - PRIMARY: a solid light fill (text colour) with dark label text;
    - SECONDARY: glass;
    - GOLD: gold fill;
    - SUCCESS and DANGER: semantic fills.
  - Height stays 52dp and radius is 16dp.
- **Controls:**
  - `HudChip`, `HuntChip` and `RailButton` become glass pills or tiles.
  - `GlowBar` and `FloorBar` become rounded tracks with a gradient fill (accent2 → accent) and lose the outer glow.
  - `HuntToggle` and `SegmentSlider` restyle to match.
- **Screen kit:**
  - `ScreenHeader` is a plain sentence-case title with a round back button.
  - `SystemDialog` is a glass sheet.
  - `SelectRow` and `IconAction` follow the new controls.
- **Battle kit:**
  - `OptionTile` is a raised card with a letter badge, and its states recolour the fill instead of the border.
  - `HeartRow`, `ComboChip`, `FloatingText` and `EdgeFlash` keep their behaviour and take the new type and shapes.
- **Dock:** a floating glass pill, 16dp from the bottom edge. The centre "Hunt" button stays, as a raised accent circle.
- **Fonts:** `res/font/sora_*.ttf` and `unbounded_*.ttf` (OFL), both added to `docs/CREDITS.md`. Chakra Petch and Oxanium are removed once no screen uses them.

## Screens

- **10b Home**, as mockup A:
  - The full-height hero (the drawn avatar, or the vector fallback) sits over the mesh, floating.
  - A big Unbounded rank letter with "Novice · LV 2" sits to its left.
  - Glass top pills show the streak, due cards and badges.
  - A bottom glass sheet holds the XP track, the daily-quest row with a ring, a five-icon row (Quest, Words, Mistakes, Arena, Badges) and a light "Open the map" button.
  - Share moves into the Badges and profile area.
  - `RankUpDialog` and `ShareCardButton` follow the new kit.
- **10c Battle:** the session HUD, all exercise views, `FeedbackPanel` and Gate Cleared.
- **10d The rest:** map (both skins), lesson floors and stages, episode (the gate scene stays, and its UI moves to glass), Arena and games, achievements, mistakes, vocabulary, stories list, settings, onboarding, tutor sheet and every dialog.

## Error handling and edge cases

- **Haze below Android 12 (API < 31):** a translucent tint and no blur. Text contrast must hold without blur: tint alpha is at least 0.7 there.
- **Long Uzbek or English labels:** sentence case widens words less than tracked capitals, but every pill and tile still wraps or ellipsizes instead of clipping.
- **Reduced motion:** the mesh, the hero float and the aura are static, and presses don't scale.
- **Missing drawn avatars (git-ignored files):** Home uses the vector avatar in the same hero slot.

## Testing

- **Unit tests:** the existing suite stays green. Pure helpers get tests:
  - the token sets for both themes, including a contrast check that text on surface is at least 4.5:1;
  - the Haze fallback choice by SDK level.
- **Each stage:** emulator screenshots in both themes and both languages, a frame-time check (`dumpsys gfxinfo`) on the phone, and the user's check on the phone before merge.

## Out of scope

- New features and content.
- Changes to navigation structure, except the dock look and the Share move.
