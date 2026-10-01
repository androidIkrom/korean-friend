# Stage 6 — Game UI (System / Neon themes, evolving avatar) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the plain Material look with two dark game themes (System = default, Neon Seoul = optional), an avatar that evolves with rank, a rank-up dialog, and redesigned Home, Map and Episode screens.

**Architecture:** Theme tokens live in a `CompositionLocal` and also feed a dark Material `ColorScheme`/`Typography`, so untouched screens restyle for free. Small skin components (`GamePanel`, `GameButton`, `RankBadge`, `GameBackground`) read the tokens; each redesigned screen is written once. Rank and avatar layer selection are pure functions; the avatar draws SVG path strings on a `Canvas`, with an optional per-rank WebP slot.

**Tech Stack:** Kotlin 2.4, Compose Material 3, DataStore Preferences, kotlinx.serialization, Robolectric (`@Config(sdk = [35])`).

**Spec:** `docs/superpowers/specs/2026-10-01-stage6-game-ui-design.md`
(mockups: https://claude.ai/artifact/UbDSZSdCsS13kyNhkVCKZh)

## Global Constraints

- minSdk 26 — no `RenderEffect` or other API 31+ calls; glow via `BlurMaskFilter`/`setShadowLayer` or translucent layers.
- Both themes are dark; `isSystemInDarkTheme()` is no longer used.
- Default theme `SYSTEM`; DataStore key `ui_theme` values `"system"` / `"neon"`; unknown → `SYSTEM`.
- DataStore key `last_seen_rank` (rank name `"E"`…`"S"`); unknown → `null`.
- Ranks: E 1–4, D 5–9, C 10–14, B 15–19, A 20–29, S 30+. Titles: Boshlovchi, Shogird, Ovchi, Ritsar, Qo'mondon, Monarx.
- Backup: `BackupSettings.theme: String = "system"` (`@SerialName("theme")`); `BackupCodec.VERSION` unchanged.
- Fonts: Chakra Petch (400/600/700) for System, Oxanium (400/600/800) for Neon; Korean uses the system font. OFL licence texts under `app/src/main/assets/licenses/`.
- Avatar slot path: `assets/avatar/<theme>_<rank>.webp`, lower case (`system_c.webp`, `neon_s.webp`).
- UI copy is Uzbek; code, commits and PR text English. No `Co-Authored-By` lines. No worktrees, no subagents.
- Test command: `./gradlew :app:testDebugUnitTest` (single class: add `--tests '<FQCN>'`).

## Review Focus

1. A backup exported before this stage (no `theme` key) must import with theme System — Task 2 `decodesOldFileWithoutTheme`.
2. A corrupted/unknown `ui_theme` or `last_seen_rank` value must not crash and must fall back — Task 2 `unknownThemeFallsBackToSystem`, `unknownRankIsNull`.
3. Importing a high-XP backup or installing fresh must not pop a rank-up dialog — Task 1 `noDialogWithoutLastSeen`, Task 2 `importSetsLastSeenRank`.
4. Every avatar path string must parse; one bad string would crash Home on draw — Task 4 `allPathsParse`.
5. Map with no current lesson / all units locked must still mark a sensible active unit or none — Task 7 `firstUnclearedOpenUnitIsActiveWithoutCurrent`, `noActiveWhenEverythingLocked`.

## File Structure

| File | Responsibility |
|---|---|
| `study/RankRules.kt` (new) | `Rank` enum, `rankFor`, `rankUpToShow` |
| `data/SettingsRepository.kt` | + `GameThemeId`, `Settings.theme`, `setTheme`, `lastSeenRank`, `setLastSeenRank` |
| `data/Backup.kt`, `data/BackupService.kt` | theme field; import stores last-seen rank |
| `ui/theme/GameTheme.kt` (new) | `GameTokens`, `tokensFor`, `gameColorScheme`, `gameTypography`, `LocalGameTokens` |
| `ui/theme/Theme.kt` | `HangulFriendTheme(themeId)` |
| `ui/theme/GameComponents.kt` (new) | `GamePanel`, `GameButton`, `ProgressBar`, `RankBadge`, `GameBackground` |
| `res/font/*.ttf` (new) | Chakra Petch, Oxanium |
| `ui/avatar/AvatarLayers.kt`, `AvatarPaths.kt`, `AvatarAssets.kt`, `Avatar.kt` (new) | layer rules, path data, slot lookup, drawing |
| `ui/settings/ThemeRow.kt` (new), `ui/settings/Settings.kt` | theme picker |
| `ui/Nav.kt` | 4 tabs, themed bar |
| `ui/home/Home.kt`, `ui/home/RankUpDialog.kt` (new) | redesigned Home, rank-up dialog |
| `ui/map/BookMap.kt`, `ui/map/UnitState.kt` (new), `ui/map/MapSkins.kt` (new) | unit state, tower / metro |
| `ui/story/Episode.kt`, `ui/avatar/SpeakerArt.kt` (new) | redesigned episode |

---

### Task 1: Rank rules

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/study/RankRules.kt`
- Modify: `app/src/main/res/values/strings.xml` (rank titles)
- Test: `app/src/test/java/uz/hangulfriend/study/RankRulesTest.kt`

**Interfaces:**
- Produces: `enum class Rank(val minLevel: Int) { E(1), D(5), C(10), B(15), A(20), S(30) }`; `object RankRules { fun rankFor(level: Int): Rank; fun rankUpToShow(current: Rank, lastSeen: Rank?): Rank? }`; strings `rank_title_e` … `rank_title_s`; `@StringRes fun Rank.titleRes(): Int` (in `ui/avatar/AvatarLayers.kt`, Task 4 — not here).

- [ ] **Step 1: Write the failing test**

```kotlin
class RankRulesTest {
    @Test fun boundaries() {
        mapOf(0 to Rank.E, 1 to Rank.E, 4 to Rank.E, 5 to Rank.D, 9 to Rank.D, 10 to Rank.C, 14 to Rank.C,
            15 to Rank.B, 19 to Rank.B, 20 to Rank.A, 29 to Rank.A, 30 to Rank.S, 99 to Rank.S)
            .forEach { (lv, r) -> assertEquals("level $lv", r, RankRules.rankFor(lv)) }
    }
    @Test fun noDialogWithoutLastSeen() = assertNull(RankRules.rankUpToShow(Rank.C, null))
    @Test fun dialogWhenHigher() = assertEquals(Rank.C, RankRules.rankUpToShow(Rank.C, Rank.D))
    @Test fun noDialogWhenSameOrLower() {
        assertNull(RankRules.rankUpToShow(Rank.C, Rank.C))
        assertNull(RankRules.rankUpToShow(Rank.D, Rank.C))
    }
}
```

- [ ] **Step 2: Run, expect FAIL** (`Unresolved reference: Rank`). `./gradlew :app:testDebugUnitTest --tests 'uz.hangulfriend.study.RankRulesTest'`
- [ ] **Step 3: Implement `RankRules`** — `rankFor` = last `Rank` whose `minLevel <= level`, else `E`. Add the six `rank_title_*` strings (spec titles).
- [ ] **Step 4: Run, expect PASS.**
- [ ] **Step 5: Commit** `feat(game): rank rules E..S by level`

### Task 2: Theme + last-seen-rank settings, backup field

**Files:**
- Modify: `data/SettingsRepository.kt`, `data/Backup.kt`, `data/BackupService.kt`
- Test: create `test/.../data/SettingsRepositoryTest.kt`; modify `BackupCodecTest.kt`, `BackupServiceTest.kt`

**Interfaces:**
- Consumes: `Rank`, `RankRules.rankFor` (Task 1); `GameRules.level(totalXp).level`.
- Produces: `enum class GameThemeId(val key: String) { SYSTEM("system"), NEON("neon"); companion object { fun from(key: String?): GameThemeId } }` in `data/SettingsRepository.kt` (data layer must not depend on `ui`); `Settings.theme: GameThemeId = GameThemeId.SYSTEM`; `SettingsRepository.setTheme(id: GameThemeId)`; `val lastSeenRank: Flow<Rank?>`; `suspend fun setLastSeenRank(rank: Rank)`; `BackupSettings.theme: String = "system"`.

- [ ] **Step 1: Write the failing tests**

`SettingsRepositoryTest` (Robolectric, same temp-file DataStore setup as `BackupServiceTest`):
```kotlin
@Test fun themeDefaultsToSystem() = runTest { assertEquals(GameThemeId.SYSTEM, settings.settings.first().theme) }
@Test fun setThemePersists() = runTest { settings.setTheme(GameThemeId.NEON); assertEquals(GameThemeId.NEON, settings.settings.first().theme) }
@Test fun unknownThemeFallsBackToSystem() = runTest {
    store.edit { it[stringPreferencesKey("ui_theme")] = "pink" }
    assertEquals(GameThemeId.SYSTEM, settings.settings.first().theme)
}
@Test fun lastSeenRankRoundTrip() = runTest {
    assertNull(settings.lastSeenRank.first()); settings.setLastSeenRank(Rank.B); assertEquals(Rank.B, settings.lastSeenRank.first())
}
@Test fun unknownRankIsNull() = runTest {
    store.edit { it[stringPreferencesKey("last_seen_rank")] = "Z" }; assertNull(settings.lastSeenRank.first())
}
@Test fun replaceAllWritesTheme() = runTest {
    settings.replaceAll(Settings(theme = GameThemeId.NEON)); assertEquals(GameThemeId.NEON, settings.settings.first().theme)
}
```
`BackupCodecTest`: `decodesOldFileWithoutTheme` — encode a file, remove `"theme"` from the JSON text, decode → `settings.theme == "system"`; `themeRoundTrip` — `"neon"` survives encode/decode.
`BackupServiceTest`: `importSetsLastSeenRank` — import a file whose `xpEvents` total 4 500 XP (level 10) → `settings.lastSeenRank.first() == Rank.C`; `exportCarriesTheme` — `setTheme(NEON)`, export → `settings.theme == "neon"`.

- [ ] **Step 2: Run the three classes, expect FAIL** (unresolved `GameThemeId`, `theme`, `lastSeenRank`).
- [ ] **Step 3: Implement.** `GameThemeId.from` = `entries.firstOrNull { it.key == key } ?: SYSTEM`. Keys `ui_theme`, `last_seen_rank`. `lastSeenRank` = `Rank.entries.firstOrNull { it.name == p[LAST_SEEN_RANK] }`. `replaceAll` also writes theme (not `last_seen_rank`). `toBackup()`/`toSettings()` map `theme.key` ↔ `GameThemeId.from`. `BackupService.import` ends with `settings.setLastSeenRank(RankRules.rankFor(GameRules.level(file.xpEvents.sumOf { it.amount }).level))`.
- [ ] **Step 4: Run, expect PASS; then full suite green.**
- [ ] **Step 5: Commit** `feat(settings): theme choice and last seen rank; backup carries theme`

### Task 3: Theme tokens, fonts, skin components

**Files:**
- Create: `ui/theme/GameTheme.kt`, `ui/theme/GameComponents.kt`, `res/font/chakra_petch_regular.ttf`, `chakra_petch_semibold.ttf`, `chakra_petch_bold.ttf`, `oxanium_*.ttf`, `assets/licenses/OFL-ChakraPetch.txt`, `assets/licenses/OFL-Oxanium.txt`
- Modify: `ui/theme/Theme.kt`, `MainActivity.kt`
- Test: `test/.../ui/GameThemeTest.kt`

**Interfaces:**
- Consumes: `GameThemeId` (Task 2).
- Produces: `data class GameTokens(background, panel, panelBorder, text, muted, accent, accent2, top, danger: Color, panelCorner: Dp, display: FontFamily, bracketTitles: Boolean, id: GameThemeId)`; `fun tokensFor(id: GameThemeId): GameTokens`; `fun gameColorScheme(t: GameTokens): ColorScheme`; `val LocalGameTokens`; `@Composable fun HangulFriendTheme(themeId: GameThemeId, content)`; components:
  `GamePanel(title: String?, modifier: Modifier = Modifier, borderColor: Color? = null, content: @Composable ColumnScope.() -> Unit)`,
  `GameButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)`,
  `ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color? = null)`,
  `RankBadge(letter: String, state: BadgeState, modifier: Modifier = Modifier)` with `enum class BadgeState { DONE, ACTIVE, LOCKED }`,
  `GameBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit)`.

Token values (spec §2.1):

| | SYSTEM | NEON |
|---|---|---|
| background | 0xFF04060C | 0xFF0A0613 |
| panel | 0xE6060E20 | 0xC7180C2E |
| panelBorder | 0xBF5CC8FF | 0x592EE6FF |
| text | 0xFFDCEBFF | 0xFFEDE7FF |
| muted | 0xFF8FA8C8 | 0xFFA79CC8 |
| accent | 0xFF5CC8FF | 0xFFFF3D9A |
| accent2 | 0xFFA98BFF | 0xFF2EE6FF |
| top | 0xFFA98BFF | 0xFFFFE45C |
| danger | 0xFFFF6B85 | 0xFFFF6B85 |
| panelCorner | 0.dp | 14.dp |
| bracketTitles | true | false |

- [ ] **Step 1: Write the failing test**

```kotlin
class GameThemeTest {
    @Test fun systemTokens() { val t = tokensFor(GameThemeId.SYSTEM); assertEquals(Color(0xFF5CC8FF), t.accent); assertTrue(t.bracketTitles) }
    @Test fun neonTokens() { val t = tokensFor(GameThemeId.NEON); assertEquals(Color(0xFFFF3D9A), t.accent); assertEquals(14.dp, t.panelCorner) }
    @Test fun colorSchemeFollowsTokens() {
        val t = tokensFor(GameThemeId.NEON); val c = gameColorScheme(t)
        assertEquals(t.accent, c.primary); assertEquals(t.accent2, c.secondary); assertEquals(t.background, c.background)
        assertEquals(t.danger, c.error); assertEquals(t.text, c.onBackground); assertEquals(1f, c.surface.alpha)
    }
}
```
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement.** Download TTFs from `https://github.com/google/fonts/raw/main/ofl/chakrapetch/` (static `ChakraPetch-Regular/SemiBold/Bold.ttf`) and `.../ofl/oxanium/` (if only the variable `Oxanium[wght].ttf` exists, ship it once as `oxanium_variable.ttf` and build the family with `Font(R.font.oxanium_variable, weight, variationSettings = FontVariation.Settings(FontVariation.weight(w)))`); copy each `OFL.txt`. `gameColorScheme` = `darkColorScheme(primary = accent, onPrimary = background, secondary = accent2, background, onBackground = text, surface = panel.copy(alpha = 1f), onSurface = text, surfaceVariant = panel.copy(alpha = 1f), onSurfaceVariant = muted, outline = panelBorder, error = danger)`. `gameTypography(display)` = default `Typography()` with display*/headline*/title* styles using `display`. `HangulFriendTheme(themeId)` provides `LocalGameTokens` + `MaterialTheme`. `MainActivity`: `val theme by container.settings.settings.map { it.theme }.collectAsState(GameThemeId.SYSTEM)` → `HangulFriendTheme(theme)`. Components draw per mockups A/C: panel = background `panel`, 1 dp `panelBorder` border, corner `panelCorner`; System adds an outer glow (`drawBehind` + `BlurMaskFilter(12f, OUTER)` in `accent` at 35 %); title shown as `[ TITLE ]` upper-case when `bracketTitles`, else plain bold. `GameBackground`: System = grid lines every 32 dp at 5 % accent + top radial accent glow; Neon = vertical gradient `0xFF26104A → background`.
- [ ] **Step 4: Run test PASS; `./gradlew :app:assembleDebug` succeeds; full suite green.**
- [ ] **Step 5: Commit** `feat(theme): System and Neon game themes with fonts and skin components`

### Task 4: Avatar

**Files:**
- Create: `ui/avatar/AvatarLayers.kt`, `ui/avatar/AvatarPaths.kt`, `ui/avatar/AvatarAssets.kt`, `ui/avatar/Avatar.kt`, `assets/avatar/README.md`
- Modify: `AppContainer.kt` (`val avatarAssets`), `strings.xml` (layer descriptions)
- Test: `test/.../ui/AvatarLayersTest.kt`, `AvatarPathsTest.kt`, `AvatarAssetsTest.kt`

**Interfaces:**
- Consumes: `Rank` (Task 1), `GameTokens`, `LocalGameTokens` (Task 3).
- Produces: `enum class AvatarLayer { HOOD, BASE, JACKET, RIM, LONG_COAT, EYE_GLOW, AURA_1, PAULDRONS, AURA_2, PARTICLES, CAPE, BLADES, HAIR_LIGHT, AURA_3, MONARCH_AURA, EYE_TRAIL, COMPANIONS }`; `fun layersFor(rank: Rank): Set<AvatarLayer>`; `fun newLayersAt(rank: Rank): List<Int>` (string res of the descriptions shown in the rank-up dialog); `@StringRes fun Rank.titleRes(): Int`; `class AvatarAssets(files: Set<String>) { fun slotFor(theme: GameThemeId, rank: Rank): String? }` (+ `companion fun load(assets: AssetManager): AvatarAssets`, reads `list("avatar")`, empty on error); `@Composable fun Avatar(rank: Rank, modifier: Modifier = Modifier)`; `val LocalAvatarAssets`.

Layer table (spec §4.3) — cumulative except `HOOD`: E `{HOOD, BASE}`; D = E − HOOD + `{JACKET, RIM}`; C + `{LONG_COAT, EYE_GLOW, AURA_1}`; B + `{PAULDRONS, AURA_2, PARTICLES}`; A + `{CAPE, BLADES, HAIR_LIGHT, AURA_3}`; S + `{MONARCH_AURA, EYE_TRAIL, COMPANIONS}`.

Path data (viewBox `-20 -10 240 290`; copy into `AvatarPaths.kt` as `const val`):

```
BODY      M14 240 C18 196 52 172 100 170 C148 172 182 196 186 240 Z
HOOD      M56 130 C44 86 68 52 100 52 C132 52 156 86 144 130 L158 186 L42 186 Z
NECK      M86 136 L114 136 L116 176 L84 176 Z
SHIRT     M78 170 L100 206 L122 170 L132 176 L100 238 L68 176 Z
HOODIE_NECK M80 172 L100 190 L120 172
STRINGS   M90 182 L88 208 M110 182 L112 208
FACE      M64 96 C64 62 84 46 100 46 C116 46 136 62 136 96 C136 126 120 150 100 152 C80 150 64 126 64 96 Z
HAIR      M58 104 C50 58 76 32 104 34 C134 36 154 60 144 104 L138 86 L130 92 L124 74 L114 90 L104 70 L94 88 L86 70 L78 90 L70 78 L66 96 Z
COLLAR_L  M66 176 L54 134 L88 164 Z
COLLAR_R  M134 176 L146 134 L112 164 Z
PAULD_L   M16 222 C20 194 44 180 70 178 L74 196 C52 200 36 210 30 230 Z
PAULD_R   M184 222 C180 194 156 180 130 178 L126 196 C148 200 164 210 170 230 Z
CAPE      M34 194 C16 226 6 250 -4 270 L204 270 C194 250 184 226 166 194 Z
BLADE_L   M64 180 L26 88 L33 85 L70 176 Z
HILT_L    M24 84 L18 70
BLADE_R   M136 180 L174 88 L167 85 L130 176 Z
HILT_R    M176 84 L182 70
HAIR_LIGHT M84 44 C92 52 96 60 96 72 M114 42 C122 50 126 62 124 74
EYE_L     M76 104 L94 100 L93 105 L78 107 Z
EYE_R     M124 104 L106 100 L107 105 L122 107 Z
EYE_TRAIL M122 103 C136 98 150 100 164 90
MOUTH     M94 132 L106 132
CROWN     M30 70 L42 30 L54 62 L68 8 L82 52 L100 0 L118 52 L132 8 L146 62 L158 30 L170 70 C140 50 60 50 30 70 Z
SOLDIER   M10 270 C8 236 14 222 19 214 L19 200 C10 200 8 186 19 180 C30 186 28 200 19 200 L19 214 C24 222 30 236 28 270 Z   (translate x −6 and +168)
DRONE     M0 -10 L10 0 L0 10 L-10 0 Z   (translate to 16,120 and 184,100)
MINJI_BACK  M52 104 C46 54 74 32 100 32 C126 32 154 54 148 104 C150 150 158 196 152 220 L126 206 L74 206 L48 220 C42 196 50 150 52 104 Z
MINJI_BODY  M24 240 C28 204 60 184 100 182 C140 184 172 204 176 240 Z
MINJI_FACE  M68 98 C68 66 86 52 100 52 C114 52 132 66 132 98 C132 126 118 148 100 150 C82 148 68 126 68 98 Z
MINJI_LOCK_L M64 92 C60 122 62 152 68 176 L78 172 C74 144 72 118 72 96 Z
MINJI_LOCK_R M136 92 C140 122 138 152 132 176 L122 172 C126 144 128 118 128 96 Z
MINJI_BANGS M62 100 C58 60 80 42 100 42 C122 42 144 60 138 100 L132 82 L122 92 L114 74 L102 90 L92 74 L82 92 L72 82 L68 100 Z
MINJI_EYE_L M80 106 C84 101 90 101 95 105 C90 108 84 108 80 106 Z
MINJI_EYE_R M120 106 C116 101 110 101 105 105 C110 108 116 108 120 106 Z
```

Palettes (spec "EVO_A"/"EVO_C"): SYSTEM coat `0xFF070B16`, shirt `0xFF0E1628`, skin `0xFF18213A`, shade `0xFF10182C`, hair `0xFF04060E`, dim line `0xFF2B3A55`, eye `0xFF7F93B5`, eyeGlow `0xFFAEE8FF`, blade `0xFF9FB3D1`, glow `0xFF2E8BFF`; NEON coat `0xFF140A24`, shirt `0xFF1D1036`, skin `0xFF2A1838`, shade `0xFF1F122B`, hair `0xFF0D0618`, dim `0xFF4A3360`, eye `0xFF9B8FC0`, eyeGlow `0xFF2EE6FF`, blade `0xFF2EE6FF`, glow `0xFFFF3D9A`. Contour = dim at E, `top` at S, `accent` otherwise; stroke width `1.2 + 0.15 × rankIndex`. Aura radial alpha C .28, B .42, A .58, S .80 (S in `top`). S companions: SYSTEM soldiers + `CROWN` at 35 %, NEON gold halo ellipse (cx 100, cy 30, rx 54, ry 12) + drones.

- [ ] **Step 1: Write the failing tests**

```kotlin
class AvatarLayersTest {
    @Test fun eHasHoodOthersNot() { assertTrue(AvatarLayer.HOOD in layersFor(Rank.E)); Rank.entries.drop(1).forEach { assertFalse(AvatarLayer.HOOD in layersFor(it)) } }
    @Test fun cumulative() { Rank.entries.zipWithNext().drop(1).forEach { (a, b) -> assertTrue("$a ⊂ $b", layersFor(b).containsAll(layersFor(a))) } }
    @Test fun sHasCompanions() = assertTrue(layersFor(Rank.S).containsAll(setOf(AvatarLayer.MONARCH_AURA, AvatarLayer.EYE_TRAIL, AvatarLayer.COMPANIONS)))
    @Test fun eachRankAfterEAnnouncesSomething() = Rank.entries.drop(1).forEach { assertTrue(newLayersAt(it).isNotEmpty()) }
}
class AvatarPathsTest { @Test fun allPathsParse() = AvatarPaths.all.forEach { (name, d) -> assertTrue(name, PathParser().parsePathString(d).toNodes().isNotEmpty()) } }
class AvatarAssetsTest {
    private val a = AvatarAssets(setOf("system_c.webp", "README.md", "neon_S.webp"))
    @Test fun findsSlot() = assertEquals("avatar/system_c.webp", a.slotFor(GameThemeId.SYSTEM, Rank.C))
    @Test fun missingSlot() = assertNull(a.slotFor(GameThemeId.SYSTEM, Rank.B))
    @Test fun caseMustMatch() = assertNull(a.slotFor(GameThemeId.NEON, Rank.S))
}
```
`AvatarPaths.all: List<Pair<String, String>>` lists every constant above.

- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement.** `Avatar` draws in a `Canvas` scaled from the 240×290 viewBox (aspect kept), layers in the order: aura → crown/companions → cape → blades+hilts → hood → body → collars → pauldrons → neck → shirt or hoodie neck+strings → face → hair → hair light → eye glow (two ellipses at (86,103) and (114,103), 13×5, 35 % eyeGlow) → eyes → eye trail → mouth. Parse each path once (`remember { PathParser().parsePathString(d).toPath() }` per constant in a lazily built map). If `LocalAvatarAssets.current.slotFor(theme, rank)` is non-null, decode it once with `BitmapFactory.decodeStream` inside `produceState` (IO dispatcher); on null/exception fall back to vector. `AppContainer.avatarAssets` loaded lazily; provided in `MainActivity` via `LocalAvatarAssets`. Descriptions (strings `avatar_new_*`): D "Kurtka, yengil kontur nuri"; C "Uzun plash", "Ko'zlar endi nur sochadi", "Kuchsiz aura"; B "Yelka zirhlari", "Kuchli aura"; A "Mantiya", "Egizak xanjar", "Sochda nur"; S "Monarx aurasi", "Ko'z alangasi", "Hamrohlar". `README.md` documents the slot format (transparent background, 480×576 px, WebP, names `system_e.webp` … `neon_s.webp`).
- [ ] **Step 4: Run, expect PASS; full suite green.**
- [ ] **Step 5: Commit** `feat(avatar): rank-evolving vector avatar with optional image slots`

### Task 5: Theme picker and four-tab bar

**Files:**
- Create: `ui/settings/ThemeRow.kt`
- Modify: `ui/settings/Settings.kt` (`SettingsViewModel.setTheme`, row placed first), `ui/Nav.kt` (tabs Home, Map, Stories, Settings; bar colours from tokens), `ui/home/Home.kt` (drop the Stories button and `onStories` param), `strings.xml` (`nav_home` "Uy", `nav_book` "Xarita", new `nav_stories` "Hikoyalar", `settings_theme` "Interfeys uslubi", `theme_system` "Tizim", `theme_neon` "Neon Seul")
- Test: existing suite (no new pure logic)

**Interfaces:**
- Consumes: `SettingsRepository.setTheme` (Task 2), `tokensFor` (Task 3).
- Produces: `@Composable fun ThemeRow(current: GameThemeId, onPick: (GameThemeId) -> Unit)` — two selectable cards (`Modifier.selectable`, `Role.RadioButton`), each with a 3-swatch preview (`background`, `accent`, `accent2`) and its name.

- [ ] **Step 1:** Implement the row, view-model method and nav changes as listed.
- [ ] **Step 2:** `./gradlew :app:assembleDebug :app:testDebugUnitTest` — build OK, suite green.
- [ ] **Step 3: Commit** `feat(ui): theme picker in settings; stories tab`

### Task 6: Home redesign and rank-up dialog

**Files:**
- Create: `ui/home/RankUpDialog.kt`, `ui/home/HomeScene.kt`
- Modify: `ui/home/Home.kt`, `ui/Nav.kt` (pass new deps), `AppContainer.kt` if needed, `strings.xml`
- Test: `test/.../ui/HomeQuestTest.kt`

**Interfaces:**
- Consumes: `RankRules`, `Rank` (1); `lastSeenRank`, `setLastSeenRank` (2); components (3); `Avatar`, `newLayersAt`, `titleRes` (4); `StoryRepository.observeDone()`, `GameRepository.observeAchievements()`, `db.cards().countLearnedWords()` (via `ProgressStats.snapshot()`).
- Produces: `HomeStats` + `learnedWords: Int`, `completedLessons: Int`, `storiesDone: Int`, `achievements: Int`, `rank: Rank`; `HomeViewModel.rankUp: StateFlow<Rank?>`, `fun acceptRankUp()`; `data class QuestLine(val label: Int, val done: Int, val target: Int)` and pure `fun questLines(todayXp: Int, goal: Int, due: Int, stageName: Int?): List<QuestLine>`.

- [ ] **Step 1: Write the failing test**

```kotlin
class HomeQuestTest {
    @Test fun threeLinesWithLesson() {
        val q = questLines(todayXp = 40, goal = 80, due = 12, stageName = R.string.stage_practice)
        assertEquals(listOf(40 to 80, 0 to 12, 0 to 1), q.map { it.done to it.target })
    }
    @Test fun noLessonLineWithoutCurrent() = assertEquals(2, questLines(0, 80, 0, null).size)
    @Test fun reviewDoneWhenNothingDue() = assertEquals(0 to 0, questLines(0, 80, 0, null)[1].let { it.done to it.target })
    @Test fun xpCappedAtGoal() = assertEquals(80, questLines(130, 80, 0, null)[0].done)
}
```
(Use whichever existing string id names the current lesson stage; if none exists, add `quest_lesson_stage` "Joriy dars: keyingi bosqich" and pass it.)
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement.** Home layout per spec §5.2 (top row `Lv. N` + streak chip + `headerAction`; scene 300 dp tall: System portal ring / Neon skyline drawn on `Canvas`, `Avatar(rank)` centred; name plate `Aziz` + `RankBadge(rank.name, ACTIVE)` + title; `GamePanel("STATUS")` with `ProgressBar` and 4 columns So'zlar / Darslar / Hikoyalar / Yutuqlar; `GamePanel("KUNLIK TOPSHIRIQ")` with quest lines and the footer "Maqsad bajarilsa seriya davom etadi"; `GameButton("DAVOM ETISH · …")` → current lesson, or Map when none (new `onOpenMap` param); secondary row Takrorlash (due) / O'yinlar / Xatolar / Yutuqlar). `refresh()` also computes `rank` and `rankUp`: `lastSeen == null` → `setLastSeenRank(rank)` and no dialog; else `rankUpToShow(rank, lastSeen)`. `RankUpDialog(from: Rank?, to: Rank, level: Int, onAccept)` = full-screen `Dialog(usePlatformDefaultWidth = false)` per mockup "Rank oshdi · Tizim": `[ SYSTEM ]`, "Darajangiz N ga yetdi. Rank oshdi!", `from → to`, "UNVON: …", `newLayersAt(to)` as checked lines, `Avatar(to)`, `GameButton("QABUL QILISH")` → `acceptRankUp()` (stores rank, clears state).
- [ ] **Step 4: Run test PASS; full suite green.**
- [ ] **Step 5: Commit** `feat(home): game-style home with status, daily quest and rank-up dialog`

### Task 7: Map — gate tower / metro line

**Files:**
- Create: `ui/map/UnitState.kt`, `ui/map/MapSkins.kt`
- Modify: `ui/map/BookMap.kt` (VM takes `SettingsRepository` for current lesson), `ui/Nav.kt`, `strings.xml` (`unit_topics_ko` string-array of 9: 가족, 쇼핑, 여행, 취미, 은행·우체국, 길 찾기, 약속·병원, 감정·규칙, 전화·일상; `map_tower_title` "Darvozalar minorasi", `map_metro_title` "Metro xaritasi", `map_enter_gate` "DARVOZAGA KIRISH", `map_enter_station` "Bekatga tushish", `map_cleared` "TOZALANDI")
- Test: `test/.../ui/UnitStateTest.kt`; update `BookMapViewModelTest.kt` constructor

**Interfaces:**
- Consumes: `LessonRow`, `buildRows` (existing); components (3).
- Produces: `enum class UnitState { CLEARED, ACTIVE, OPEN, LOCKED }`; `data class UnitRow(val unit: Int, val topicUz: String, val lessons: List<LessonRow>, val state: UnitState)`; `fun buildUnits(rows: List<LessonRow>, currentLessonId: String?): List<UnitRow>`; `val UNIT_RANKS = listOf("E","E","D","D","C","C","B","A","S")`; `BookMapViewModel.units: StateFlow<List<UnitRow>>`.

Rules: CLEARED = every lesson COMPLETED or VERIFIED; LOCKED = no lesson `available`; ACTIVE = the unit holding `currentLessonId` if that unit is not CLEARED/LOCKED, else the first unit that is neither CLEARED nor LOCKED; all remaining = OPEN. At most one ACTIVE.

- [ ] **Step 1: Write the failing test**

```kotlin
class UnitStateTest {
    // helper row(unit, lesson, status, available = true)
    @Test fun clearedWhenBothDone() ...            // u1 both COMPLETED/VERIFIED -> CLEARED
    @Test fun currentLessonUnitIsActive() ...      // current u07_l1, u7 in progress -> u7 ACTIVE, others OPEN/CLEARED
    @Test fun firstUnclearedOpenUnitIsActiveWithoutCurrent() ... // current null, u1 cleared, u2 open -> u2 ACTIVE
    @Test fun clearedCurrentUnitMovesActiveOn() ... // current in cleared u3 -> first uncleared open unit ACTIVE
    @Test fun lockedWhenNoLessonAvailable() ...    // u9 both unavailable -> LOCKED
    @Test fun noActiveWhenEverythingLocked() ...   // all unavailable -> no ACTIVE
}
```
(Each test asserts `buildUnits(rows, current).map { it.state }` against an explicit list.)
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement.** `BookMapScreen` renders `TowerMap` (SYSTEM, units 9→1 top-down, `RankBadge(UNIT_RANKS[unit-1], …)`) or `MetroMap` (NEON, units 1→9, vertical line: solid `accent2` with glow up to the active station, dashed `muted` after, station dots: done = white with `accent2` ring, active = `accent` ring, locked = hollow) from `MapSkins.kt`. ACTIVE unit expands into a `GamePanel` listing both lessons (title, stage `ProgressBar`, Quick-check button when PASSED) plus the enter button (→ first not-completed available lesson). The existing boss button stays on every unit whose two lessons are available. `LazyColumn` scrolls to the ACTIVE unit on first composition.
- [ ] **Step 4: Run tests PASS (incl. updated `BookMapViewModelTest`); full suite green.**
- [ ] **Step 5: Commit** `feat(map): gate tower (System) and metro line (Neon) unit maps`

### Task 8: Episode redesign

**Files:**
- Create: `ui/avatar/SpeakerArt.kt`
- Modify: `ui/story/Episode.kt`, `ui/Nav.kt` (current rank into episode), `strings.xml` (`episode_choice` "TANLOV")
- Test: existing `StoryPlayerTest` (logic untouched)

**Interfaces:**
- Consumes: `Avatar`, path constants (4); components (3); `Rank` via `HomeViewModel`-independent read: `RankRules.rankFor(GameRules.level(totalXp).level)` in `EpisodeViewModel` (inject `GameRepository`).
- Produces: `@Composable fun SpeakerArt(voice: String, modifier: Modifier = Modifier)` — `"female"` draws the `MINJI_*` paths, anything else draws the BODY/NECK/SHIRT/FACE/HAIR/EYES set; contour `accent2`, fills from the current theme palette.

- [ ] **Step 1:** Implement per spec §5.4: `GameBackground` fills the screen; top 260 dp scene shows `Avatar(rank)` when the current speaker is `aziz`, else `SpeakerArt(voice)` (voice from `characters.json`; quiz steps keep the last speaker); top bar = close button (44 dp), segmented progress (`shown/total` segments), `n/N`, title chip `EPIZOD n · title`. Below, the history list keeps its current behaviour but each line is a `GamePanel(name)` with Korean (bodyLarge), Uzbek (muted) and the audio button; the active choice/quiz is `GamePanel(stringResource(R.string.episode_choice))` with numbered option buttons, eliminated options disabled and `why_uz` shown as today. Finish view unchanged apart from tokens.
- [ ] **Step 2:** `./gradlew :app:assembleDebug :app:testDebugUnitTest` — build OK, suite green.
- [ ] **Step 3: Commit** `feat(story): game-style episode screen with speaker art`

### Task 9: Docs and memory

**Files:**
- Modify: `docs/superpowers/specs/2026-09-29-hangul-friend-design.md` (stage list: stage 6 done), `README`-level notes only if they exist.

- [ ] **Step 1:** Mark stage 6 in the main spec's stage list; note the avatar slot folder.
- [ ] **Step 2:** Full suite + `assembleDebug` green.
- [ ] **Step 3: Commit** `docs: stage 6 status`
