# Stage 6b — Boy/Girl Hero, Eyes, Flame Aura, Icon, "Hangul Hunt" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user pick a boy or girl hero whose eyes and rounded flame aura evolve with rank, ship the "Monarch gaze" launcher icon and rename the app to Hangul Hunt.

**Architecture:** A `HeroGender` setting (DataStore + backup) feeds `Avatar(rank, hero, animated)`. Layer choice, eye style and flame geometry are pure functions (`layersFor`, `eyeStyleFor`, `flameOutline`) so they are unit-tested. Drawing stays a `Canvas` over SVG path constants, and the aura animates with `rememberInfiniteTransition`. The icon is an adaptive vector icon with a monochrome layer.

**Tech Stack:** Kotlin 2.4, Compose, DataStore, kotlinx.serialization, Android VectorDrawable (gradients API 24+), Robolectric `@Config(sdk = [35])`.

**Spec:** `docs/superpowers/specs/2026-10-01-stage6b-hero-avatar-icon-design.md`
(builds on stage 6, PR #17; this branch `feature/stage6b-avatar-icon` is stacked on `feature/stage6-game-ui`)

## Global Constraints

- DataStore key `hero`, values `"boy"`/`"girl"`, unknown → BOY; backup `settings.hero` default `"boy"`; `BackupCodec.VERSION` unchanged.
- Package `uz.hangulfriend`, class names and repo name do not change; only user-visible name becomes "Hangul Hunt".
- Story episodes always draw Aziz as the BOY hero.
- Aura animates only on Home and in the rank-up dialog (`animated = true`); everywhere else static.
- Avatar slot name: `assets/avatar/<theme>_<hero>_<rank>.webp`, lower case.
- minSdk 26: no `RenderEffect`; blur via `BlurMaskFilter` (unblurred below API 28 is accepted).
- UI copy Uzbek; code/commits/PR English; no `Co-Authored-By`; no worktrees, no subagents.
- Test command: `./gradlew :app:testDebugUnitTest` (+ `:app:assembleDebug` for resource tasks).

## Review Focus

1. Old backups without `hero` must import as BOY — Task 1 `decodesOldFileWithoutHero`.
2. A corrupted `hero` value must not crash — Task 1 `unknownHeroIsBoy`.
3. Flame outline must have no corners (the user rejected spiky shapes twice) — Task 2 `jointsAreSmooth`.
4. The girl must keep the girl-only parts and never get the boy's blades/hood — Task 2 `girlNeverGetsBoyParts`.
5. The launcher icon must build on every density and have a monochrome layer — Task 5 `assembleDebug` + XML check step.

## Shared values (from the approved mockups, generator `evo2.py` / `icons.py`)

Eye style per rank (left eye; right eye = mirror around x = 100; girl eyes translated (+3, +2), mouth (+1, +2)):

| Rank | lid | brow (width) | mouth | bloom r / alpha | core | extra |
|---|---|---|---|---|---|---|
| E | `M74 104 C78 98 90 97 95 102 C90 108 79 109 74 104 Z` sclera `#C9D4E6` α .85, iris (85,103.5) r 3.6 `#22304D`, highlight (86.3,102.3) r 1 white | `M72 93 Q84 91 95 85` (2.4, dim) | `M93 134 Q100 131 107 134` | – | – | – |
| D | `M74 104 C78 99 90 99 95 103 C90 107 79 108 74 104 Z` same sclera/iris | `M72 91 Q84 88 95 90` (2.4, accent) | `M94 133 L106 133` | – | – | – |
| C | `M74 103 L95 100 C91 106 80 107 74 103 Z` filled accent | `M72 89 Q84 89 96 95` (2.6) | `M94 132 Q101 133.5 107 130` | 9 / .35 | (85.5,102.6) 3.2×2.2 `#AEE8FF` | – |
| B | `M73 103 L96 99 L94 104 C88 107 80 107 73 103 Z` | `M70 86 L97 96` (3.2) | as C | 12 / .5 | `#AEE8FF` | – |
| A | as B | as B | as C | 15 / .6 | `#E9DDFF` | streak `M73 103 L54 98` (2.4) |
| S | as B, colour `top` | as B, colour `top` | `M94 132.5 Q100 134 106 132` | 19 / .75 (`top`) | `#E9DDFF` | trail fill `M73 103 C60 99 48 94 34 80 C46 90 58 96 73 100 Z` (gradient hot→top→transparent) + line `M73 103 C62 100 52 96 40 86` (1.2, hot) |

Girl lash: E/D upper-lid curve of that rank's lid drawn in black 1.6; C+ `M72 102 L95 98.5` black 1.8.

Girl paths: back hair loose = existing `MINJI_BACK`; tied `M56 104 C50 56 76 34 100 34 C124 34 150 56 144 104 C144 122 140 136 134 146 L66 146 C60 136 56 122 56 104 Z`;
ponytail `M122 46 C150 34 170 60 166 100 C162 136 152 160 166 196 C140 178 132 140 138 104 C142 80 136 62 118 56 Z`;
short locks `M66 92 C62 112 64 128 70 140 L77 136 C74 122 73 108 73 96 Z` / `M134 92 C138 112 136 128 130 140 L123 136 C126 122 127 108 127 96 Z`;
band `M120 50 L130 46 L132 56 L122 58 Z` (accent2); tiara `M78 46 L86 30 L94 42 L100 24 L106 42 L114 30 L122 46` (stroke `top` 2.2);
sword `M150 186 L56 40 L61 37 L156 182 Z`, hilt `M54 38 L46 26` (5), guard `M48 46 L64 34` (3); girl E shoulder hood `M40 214 C44 190 70 176 100 176 C130 176 156 190 160 214 Z`; body = `MINJI_BODY`, face = `MINJI_FACE`, bangs = `MINJI_BANGS`, long locks = `MINJI_LOCK_*`.

Flame outline (`flameOutline(k, scale, seed)`, view-box units, centre (100,150), rx = 88·scale, ry = 118·scale):
`lobes = (6 + 4k).toInt()`, `span = 230 / lobes`; `at(deg, out, lift)` = ellipse point at `deg` pushed `out` along its normal and raised `lift`.
Points: `(cx − 1.02·rx, 282)`, then per lobe j: `a0 = 205 − span·j`, `var = 0.75 + 0.5·(((j+seed)·37) % 11)/10`, `h = (10 + 24k)·var·scale`, `sway = ±3k` (sign by parity of j+seed):
`at(a0, 3, 0)`, `at(a0 − .22span, .35h, .45h)`, `at(a0 − .5span, .5h, .8h) + (sway, 0)`, `at(a0 − .78span, .35h, .45h)`; then `at(205 − span·lobes, 3, 0)`, `(cx + 1.02·rx, 282)`.
Count = `4·lobes + 3`. Path = closed Catmull-Rom (`c1 = p1 + (p2 − p0)/6`, `c2 = p2 − (p3 − p1)/6`). Outer layer scale 1 seed 0; inner scale .86, k·.8, seed 3.
Embers (B+): `(2 + 4k).toInt()` teardrops at x = 40 + (j·53) % 120, y = −4 + (j·29) % 40 − 30k.

Icon (108×108): background radial `#3A1F78` (centre 50%,60%) → `#120A28` (55%) → `#04060C`; hair `M20 44 C18 20 40 8 56 9 C76 10 92 24 88 44 L84 34 L78 38 L74 26 L66 34 L58 22 L52 32 L44 22 L40 34 L32 26 L28 38 Z` fill `#0A0716` stroke `#3B2A6B`; jaw `M24 60 C24 86 38 100 54 102 C70 100 84 86 84 60` stroke `#3B2A6B` 1.2;
left eye `M30 57 L50 51 L48.5 57.5 C42 61.5 35 61.5 30 57 Z` `#A98BFF`, bloom ellipse (40,56) 15×7 `#A98BFF` α .35, core (41,55.4) 3.6×2.4 `#E9DDFF`, brow `M28 47 L52 52` 2.6; right side mirrored around x = 54; trail `M30 57 C22 53 14 48 6 36 C14 44 22 50 30 54 Z` gradient `#E9DDFF → #A98BFF → transparent`.

---

### Task 1: Hero setting and backup field

**Files:** Modify `data/SettingsRepository.kt`, `data/Backup.kt`; Test `SettingsRepositoryTest.kt`, `BackupCodecTest.kt`, `BackupServiceTest.kt`

**Interfaces:**
- Produces: `enum class HeroGender(val key: String) { BOY("boy"), GIRL("girl"); companion object { fun from(key: String?): HeroGender } }`; `Settings.hero: HeroGender = HeroGender.BOY`; `SettingsRepository.setHero(g: HeroGender)`; `BackupSettings.hero: String = "boy"`.

- [ ] **Step 1: Failing tests** — `SettingsRepositoryTest`: `heroDefaultsToBoy`, `setHeroPersists` (GIRL), `unknownHeroIsBoy` (raw `"robot"`), `replaceAllWritesHero`. `BackupCodecTest`: `decodesOldFileWithoutHero` (strip `"hero"` → `"boy"`), `heroRoundTrip` (`"girl"`). `BackupServiceTest`: `exportCarriesHero`.
- [ ] **Step 2: Run, expect FAIL** (unresolved `HeroGender`/`hero`).
- [ ] **Step 3: Implement** like `GameThemeId`/`theme` in stage 6 (key `hero`, `replaceAll` writes it, `toBackup`/`toSettings` map it).
- [ ] **Step 4: Run, expect PASS; full suite green.**
- [ ] **Step 5: Commit** `feat(settings): boy/girl hero choice; backup carries it`

### Task 2: Avatar model — layers, eye style, flame geometry

**Files:** Modify `ui/avatar/AvatarLayers.kt`, `ui/avatar/AvatarPaths.kt`, `ui/avatar/AvatarAssets.kt`, `assets/avatar/README.md`; Create `ui/avatar/EyeStyle.kt`, `ui/avatar/Flame.kt`; Test `AvatarTest.kt` (extend), create `EyeStyleTest.kt`, `FlameShapeTest.kt`

**Interfaces:**
- Consumes: `Rank`, `HeroGender`, `GameThemeId`.
- Produces:
  - `enum class AvatarLayer { HOOD, LONG_HAIR, BASE, JACKET, RIM, LONG_COAT, PONYTAIL, FLAME, PAULDRONS, EMBERS, CAPE, TWIN_BLADES, SWORD, HAIR_LIGHT, MONARCH, TIARA, COMPANIONS }`;
  - `fun layersFor(rank: Rank, hero: HeroGender): Set<AvatarLayer>` — E: BOY {HOOD, BASE}, GIRL {HOOD, LONG_HAIR, BASE}; D: −HOOD +{JACKET, RIM}; C: +{LONG_COAT, FLAME}, GIRL −LONG_HAIR +PONYTAIL; B: +{PAULDRONS, EMBERS}; A: +{CAPE, HAIR_LIGHT} + BOY TWIN_BLADES / GIRL SWORD; S: +{MONARCH, COMPANIONS} + GIRL TIARA;
  - `fun newLayersAt(rank: Rank, hero: HeroGender): List<Int>` (string res; C girl adds "Soch jangovar bog'landi", A girl "Uzun qilich", S girl "Nurli toj");
  - `data class EyeStyle(val lid: String, val brow: String, val browWidth: Float, val mouth: String, val bloomRadius: Float, val bloomAlpha: Float, val glowing: Boolean, val hotCore: Boolean, val streak: Boolean, val flameTrail: Boolean, val monarch: Boolean)` and `fun eyeStyleFor(rank: Rank): EyeStyle` with the table values;
  - `fun flameOutline(k: Float, scale: Float, seed: Int): List<Offset>`; `fun catmullRom(points: List<Offset>): List<CubicSegment>` with `data class CubicSegment(val c1: Offset, val c2: Offset, val end: Offset)`; `fun emberCenters(k: Float): List<Offset>`;
  - `AvatarAssets.slotFor(theme: GameThemeId, hero: HeroGender, rank: Rank): String?`.

- [ ] **Step 1: Failing tests**
  - `AvatarLayersTest`: `cumulativeForBoth` (each hero, D⊂C⊂B⊂A⊂S), `hoodOnlyAtE`, `girlHairTiesAtC` (`LONG_HAIR` in E/D, `PONYTAIL` in C+ and never both), `girlNeverGetsBoyParts` (GIRL never `TWIN_BLADES`, BOY never `SWORD`/`TIARA`/`PONYTAIL`), `eachRankAfterEAnnouncesSomething` (both heroes).
  - `EyeStyleTest`: `noGlowBelowC` (E, D `glowing == false`), `bloomGrows` (C<B<A<S radius), `streakOnlyAtA`, `trailOnlyAtS`, `monarchOnlyAtS`.
  - `FlameShapeTest`: `pointCount` (`flameOutline(.3f,1f,0).size == 4*6+3`… use `lobes = (6+4k).toInt()`), `crestsRiseAboveShoulders` (for every lobe crest.y < both shoulders' y), `jointsAreSmooth` (for every joint i: cross(end_i − c2_i, c1_{i+1} − end_i) ≈ 0 within 1e-3·|v|·|w|, i.e. tangents collinear), `deterministic` (same args → same points).
  - `AvatarPathsTest.allPathsParse` covers the new constants. `AvatarAssetsTest`: `system_girl_c.webp` found for (SYSTEM, GIRL, C), not for (SYSTEM, BOY, C).
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement** with the shared values above; add the girl/eye path constants to `AvatarPaths` and `all`; update README slot names.
- [ ] **Step 4: Run, expect PASS; full suite green.**
- [ ] **Step 5: Commit** `feat(avatar): boy/girl layers, rank eye styles and rounded flame geometry`

### Task 3: Avatar drawing and animation

**Files:** Modify `ui/avatar/Avatar.kt`, `ui/home/HomeScene.kt`, `ui/home/RankUpDialog.kt`, `ui/story/Episode.kt`, `strings.xml` (`avatar_new_*`)

**Interfaces:**
- Consumes: everything from Task 2; `Settings.hero`.
- Produces: `@Composable fun Avatar(rank: Rank, hero: HeroGender, modifier: Modifier = Modifier, animated: Boolean = false)`.

- [ ] **Step 1: Implement drawing.** Order: radial aura glow → flame outer (blurred copy at α .6 + dark gradient fill + 1.6 rim in aura colour) → flame inner (dark, rim α .7) → embers → companions/tiara-free back layers (S) → cape → blades or sword → hood/girl shoulder hood → back hair (girl, loose or tied + ponytail) → body (boy/girl) → collars → pauldrons → neck → shirt or hoodie strings → face → hair/bangs/locks → band → hair light → tiara → eyes (bloom, lid, iris/core, brow, lash, streak/trail, mirrored) → mouth. Aura colour: `accent` (C–A), `top` at S (System violet, Neon gold). Rim glow D+: draw body+face+hair silhouettes once more first with `BlurMaskFilter(3f)` in aura colour. Animation when `animated`: `rememberInfiniteTransition` → outer `scaleY 0.95↔1.05`, `skewX ±1.5°` (1400 ms, reverse), inner opposite phase (1100 ms), embers rise 28 view-units over 2600 ms and fade; static otherwise (phase 0.5).
- [ ] **Step 2: Update callers.** HomeScene and RankUpDialog pass `settings.hero` and `animated = true` (Home VM exposes `hero` in `HomeStats`); Episode always `HeroGender.BOY`, static. `newLayersAt(to, hero)` in the dialog; replace `avatar_new_eye_glow`/`aura`/`aura_strong`/`monarch_aura`/`eye_trail` texts with "Nigoh qat'iylashdi", "Olov aurasi uyg'ondi", "Uchqunlar", "Monarx alangasi", "Ko'zdan alanga izi".
- [ ] **Step 3:** `./gradlew :app:assembleDebug :app:testDebugUnitTest` — build OK, suite green.
- [ ] **Step 4: Commit** `feat(avatar): draw boy/girl hero with rank eyes and animated flame aura`

### Task 4: Hero picker in onboarding and settings; name plate

**Files:** Create `ui/settings/HeroRow.kt`; Modify `ui/onboarding/Onboarding.kt` (VM takes `SettingsRepository`, `hero` state, saved in `complete`), `ui/settings/Settings.kt`, `ui/home/Home.kt` (name plate without "AZIZ"), `ui/Nav.kt`, `strings.xml` (`hero_title` "Personajingiz", `hero_boy` "Yigit", `hero_girl` "Qiz")

**Interfaces:**
- Produces: `@Composable fun HeroRow(current: HeroGender, onPick: (HeroGender) -> Unit)` — two `selectable` cards (Role.RadioButton) each with a static `Avatar(Rank.D, hero)` 72×88 dp and its label.

- [ ] **Step 1: Write failing test** `OnboardingHeroTest` (Robolectric, temp DataStore + fake `OnboardingService` path as in existing `OnboardingServiceTest` setup if reusable; else test `SettingsViewModel`-free): `completeSavesHero` — VM `pickHero(GIRL)`, `select("u02_l1")`, `complete {}` → `settings.settings.first().hero == GIRL`.
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement** row, onboarding (HeroRow above the lesson question), settings (below ThemeRow), name plate (unvon + RankBadge only).
- [ ] **Step 4:** test PASS; `assembleDebug` + full suite green.
- [ ] **Step 5: Commit** `feat(ui): pick boy or girl hero in onboarding and settings`

### Task 5: Launcher icon and "Hangul Hunt" name

**Files:** Create `res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`; Modify `AndroidManifest.xml` (`android:icon`, `android:roundIcon`), `strings.xml` (`app_name`, `onboarding_intro`, `share_footer`), `share/ShareCardRenderer.kt` (title "Hangul Hunt 한글")

- [ ] **Step 1:** Write the vectors with the shared icon values (VectorDrawable `<aapt:attr name="android:fillColor"><gradient …/></aapt:attr>` for gradients; bloom = plain ellipse α .35; mirrored right eye written out with x' = 108 − x). Monochrome = eyes, brows and trail only, white. Adaptive XML: `<background>`, `<foreground>`, `<monochrome>`.
- [ ] **Step 2:** Rename user-visible strings; grep `Hangul Friend` in `app/src/main` → only code identifiers may remain.
- [ ] **Step 3:** `./gradlew :app:assembleDebug :app:testDebugUnitTest` green; `grep -c monochrome app/src/main/res/mipmap-anydpi-v26/*.xml` → 1 each.
- [ ] **Step 4: Commit** `feat(brand): Monarch gaze launcher icon; app name Hangul Hunt`

### Task 6: Docs

- [ ] Main spec stage list: stage 6b line; memory/README notes only. Full suite green. Commit `docs: stage 6b status`.
