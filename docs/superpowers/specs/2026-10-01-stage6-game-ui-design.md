# 6-bosqich — o'yin uslubidagi UI: "Tizim" va "Neon Seul" temalari, evolyutsiya qiluvchi avatar

Sana: 2026-10-01
Holat: tasdiqlangan dizayn (2026-10-01), spec ko'rib chiqilmoqda
Asosiy spec: `2026-09-29-hangul-friend-design.md`
Maketlar: https://claude.ai/artifact/UbDSZSdCsS13kyNhkVCKZh (A1–A3, C1–C3, "Avatar · Tizim", "Avatar · Neon Seul", "Rank oshdi · Tizim")

## 1. Maqsad

Ilova hozir oddiy Material 3 ko'rinishida. Foydalanuvchi bolalarcha bo'lmagan, o'yin uslubidagi
interfeys xohlaydi. Maketlar orasidan tanlov:

- **A — "Tizim"** (Solo Leveling uslubi): qora fon, ko'k nur sochuvchi `[ SYSTEM ]` oynalari. **Standart tema.**
- **C — "Neon Seul"** (tungi kiberpank Seul): magenta/cyan neon, shisha kartalar, metro xaritasi.
  Foydalanuvchi Sozlamalarda A yoki C ni tanlaydi.
- **Avatar** (Aziz) daraja oshgani sari rank bo'yicha yaxshiroq ko'rinishga o'tadi.

### Foydalanuvchi tanlovlari

- Avatar rasmi: **vektor qatlamlar (kodda) + har rank uchun rasm sloti**. Slotga fayl qo'yilsa, vektor o'rniga u chiqadi.
- Ranklar: **6 ta, S kitob oxirida** — E 1–4, D 5–9, C 10–14, B 15–19, A 20–29, S 30+.
- Qamrov: **tema asosi + Sozlamada tanlash + avatar + Bosh sahifa, Xarita, Hikoya epizodi qayta chizilishi + "Rank oshdi" oynasi.**

### Qamrov tashqarisida

Mashq, sessiya, dars, tutor, o'yinlar, yutuqlar, onboarding, sozlamalar ekranlarining tuzilmasini qayta chizish
(ular faqat yangi tema rang/shriftini oladi — keyingi bosqich). Har epizod uchun alohida joy foni.
Ko'nikma ballari (tinglash/o'qish/gapirish/yozish) — ilova ularni hisoblamaydi. Yorug' (light) rejim.
Uchinchi tema. Animatsiyali (Lottie va h.k.) personaj.

## 2. Tema tizimi

### 2.1 Tokenlar

`ui/theme/GameTheme.kt` — `enum class GameThemeId { SYSTEM, NEON }` va `data class GameTokens`:

| Token | Tizim (A) | Neon Seul (C) |
|---|---|---|
| `background` | `#04060C` | `#0A0613` |
| `panel` (oyna foni) | `#E6060E20` (≈ rgba 6,14,32,.9) | `#C7180C2E` (≈ rgba 24,12,46,.78) |
| `panelBorder` | `#BF5CC8FF` | `#592EE6FF` |
| `text` | `#DCEBFF` | `#EDE7FF` |
| `muted` | `#8FA8C8` | `#A79CC8` |
| `accent` | `#5CC8FF` | `#FF3D9A` |
| `accent2` | `#A98BFF` | `#2EE6FF` |
| `top` (S rank rangi) | `#A98BFF` | `#FFE45C` |
| `danger` | `#FF6B85` | `#FF6B85` |
| `panelShape` | to'g'ri burchak (0 dp) | 14 dp yumaloq |
| `displayFont` | Chakra Petch | Oxanium |

`GameTokens` `CompositionLocal` (`LocalGameTokens`) orqali beriladi. `HangulFriendTheme(themeId)` bir vaqtning o'zida:

1. `LocalGameTokens` ni beradi;
2. `MaterialTheme` ga shu tokenlardan qurilgan **qorong'i** `ColorScheme` (`background`, `surface` = `panel` rangi shaffofsiz,
   `primary` = `accent`, `secondary` = `accent2`, `error` = `danger`, `onX` = `text`) va `Typography` (sarlavhalar `displayFont`,
   matn tizim shrifti) beradi — shuning uchun qayta chizilmaydigan ekranlar ham yangi ko'rinishga o'tadi.

`isSystemInDarkTheme()` endi ishlatilmaydi: ikkala tema ham qorong'i. `CorrectGreen`/`WrongRed` qoladi.

### 2.2 Shriftlar

`res/font/` ga Chakra Petch (400, 600, 700) va Oxanium (400, 600, 800) `.ttf` fayllari qo'shiladi (SIL OFL;
litsenziya matni `app/src/main/assets/licenses/` ga). Koreyscha matn uchun alohida shrift qo'shilmaydi — Android'ning
tizim CJK shrifti ishlatiladi (`FontFamily.Default` ga fallback).

### 2.3 Tema-teri komponentlari

`ui/theme/GameComponents.kt` — hamma qayta chizilgan ekranlar ishlatadigan kichik komponentlar; ular tokenlarga qarab chiziladi:

- `GamePanel(title: String?, modifier, content)` — `[ TITLE ]` sarlavhali oyna (Tizim: ingichka nurli ramka + ichki nur;
  Neon: yumaloq shisha karta). Sarlavha Tizim'da `[ … ]` qavsli va katta harfli, Neon'da oddiy.
- `GameButton(text, onClick, modifier, enabled)` — asosiy tugma (Tizim: ramkali gradient; Neon: magenta→binafsha gradient).
- `ProgressBar(fraction)` — ingichka nurli chiziq.
- `RankBadge(rank, size)` — Tizim: 45° burilgan romb; Neon: doira.
- `GameBackground(content)` — ekran foni: Tizim — to'r chiziqlari + yuqorida ko'k nur; Neon — tungi osmon gradienti.

Nur (glow) effektlari `Modifier.drawBehind` + `BlurMaskFilter` yoki yarim shaffof qatlamlar bilan chiziladi; API 26 da ishlashi kerak
(`RenderEffect` ishlatilmaydi, u API 31+).

## 3. Sozlama: tema tanlash

- `Settings.theme: GameThemeId = SYSTEM`; DataStore kaliti `ui_theme` (`"system"` / `"neon"`; noma'lum qiymat → `SYSTEM`).
- `SettingsRepository.setTheme(id)`.
- Sozlamalar ekranida "Interfeys uslubi" qatori: ikki variant (Tizim / Neon Seul), har biri kichik rang namunasi bilan.
- `MainActivity` sozlamani kuzatadi va `HangulFriendTheme(themeId)` ga beradi; tema darhol almashadi (qayta ishga tushirish shart emas).
- **Zaxira nusxa:** `BackupSettings` ga `theme: String = "system"` (`@SerialName("theme")`) qo'shiladi. Eski fayllarda maydon yo'q →
  standart qiymat; `BackupCodec.VERSION` o'zgarmaydi (maydon ixtiyoriy, `ignoreUnknownKeys` allaqachon yoqilgan).

## 4. Rank va avatar

### 4.1 Rank qoidalari

`study/RankRules.kt` (sof funksiyalar):

```kotlin
enum class Rank(val minLevel: Int) { E(1), D(5), C(10), B(15), A(20), S(30) }
fun rankFor(level: Int): Rank   // level < 1 bo'lsa ham E
```

Unvonlar (`strings.xml`): E — Boshlovchi, D — Shogird, C — Ovchi, B — Ritsar, A — Qo'mondon, S — Monarx.

### 4.2 "Rank oshdi" oynasi

- DataStore'da `last_seen_rank` (ixtiyoriy string). `SettingsRepository`: `lastSeenRank: Rank?`, `setLastSeenRank(rank)`.
- `RankRules.rankUpToShow(current: Rank, lastSeen: Rank?): Rank?`:
  - `lastSeen == null` → `null` (birinchi ishga tushirish yoki yangilanishdan keyin — oyna chiqmaydi; chaqiruvchi `current` ni saqlaydi);
  - `current > lastSeen` → `current`;
  - aks holda `null` (rank pasaymaydi; XP kamaymaydi).
- Bosh sahifa `refresh()` da hisoblaydi. Oyna ko'rsatilsa, "Qabul qilish" bosilganda `setLastSeenRank(current)`.
  `lastSeen == null` bo'lsa, oyna ko'rsatmasdan darhol `current` saqlanadi.
- Zaxira nusxadan tiklash (`BackupService.import`) oxirida `setLastSeenRank(rankFor(level(totalXp)))` — tiklangan progress uchun oyna chiqmaydi.
- Oyna (maket "Rank oshdi · Tizim"): to'liq ekran dialog — `[ SYSTEM ]`, "Darajangiz N ga yetdi. Rank oshdi!", `D → C`,
  yangi unvon, yangi rankda qo'shilgan qatlamlar ro'yxati (4.3 dagi tavsiflar), yangi avatar, "Qabul qilish" tugmasi.
  Bir nechta rank birdan oshsa (masalan, tiklashsiz katta XP) — faqat oxirgisi ko'rsatiladi.

### 4.3 Avatar qatlamlari

`ui/avatar/AvatarLayers.kt` — sof funksiya `layersFor(rank: Rank): Set<AvatarLayer>`; qatlamlar faqat qo'shiladi:

| Rank | Qo'shiladigan qatlamlar | Tavsif (oynada) |
|---|---|---|
| E | `HOOD`, `BASE` | Oddiy kapyushon |
| D | `JACKET`, `RIM` (`HOOD` olib tashlanadi — E dan D ga yagona almashtirish) | Kurtka, yengil kontur nuri |
| C | `LONG_COAT`, `EYE_GLOW`, `AURA_1` | Uzun plash, nurli ko'zlar, aura |
| B | `PAULDRONS`, `AURA_2`, `PARTICLES` | Yelka zirhlari, kuchli aura |
| A | `CAPE`, `BLADES`, `HAIR_LIGHT`, `AURA_3` | Mantiya, egizak xanjar, sochda nur |
| S | `MONARCH_AURA`, `EYE_TRAIL`, `COMPANIONS` | Tizim: binafsha aura, ko'z alangasi, soya askarlari; Neon: oltin halo, dronlar |

`AURA_n` — eng kattasi chiziladi. `COMPANIONS` Tizim'da soya askarlari, Neon'da dronlar sifatida chiziladi.

`ui/avatar/Avatar.kt`:

- `@Composable fun Avatar(rank: Rank, modifier)` — tokenlardan palitra oladi.
- Chizish: `Canvas` ichida maketdagi SVG path'lari (`viewBox -20 -10 240 290`) `PathParser().parsePathString(d).toPath()` orqali,
  qatlamlar tartibda (aura → companions → cape/blades → hood → body → collar → pauldrons → neck/shirt → face → hair →
  hair light → eye glow → eyes → eye trail). Path satrlari `AvatarPaths.kt` da konstanta.
- Palitra: Tizim — maketdagi `EVO_A`; Neon — `EVO_C` (kontur, ko'z nuri, aura, pichoq ranglari).
- **Rasm sloti:** `assets/avatar/<theme>_<rank>.webp` (masalan `system_c.webp`, `neon_s.webp`; kichik harf). Ilova ishga tushganda
  `AssetManager.list("avatar")` bir marta o'qiladi (fon oqimida) va `AvatarAssets` to'plamida saqlanadi. Mos fayl bo'lsa — u
  `Image` bilan chiziladi; bo'lmasa — vektor. Slotlar hozir bo'sh (`assets/avatar/README.md` formatni tushuntiradi: shaffof fon,
  480×576 px, WebP).

## 5. Qayta chiziladigan ekranlar

### 5.1 Pastki menyu

`Nav.kt` dagi tablar: **Uy, Xarita, Hikoyalar, Sozlamalar** (hozirgi uchtasiga `Routes.STORIES` qo'shiladi). Bar `GameTokens` rangida,
faol tab `accent`. Bosh sahifadagi "Hikoyalar" tugmasi olib tashlanadi (tabga ko'chdi).

### 5.2 Bosh sahifa (maket A1 / C1)

Ma'lumot `HomeViewModel` dan; yangi maydonlar `HomeStats` ga qo'shiladi: `learnedWords`, `completedLessons`, `storiesDone`,
`achievements` (mavjud manbalar: `countLearnedWords()`, `LessonStatus`, `StoryRepository.observeDone()`, `observeAchievements()`).

Tuzilma (yuqoridan pastga, `verticalScroll` saqlanadi):

1. Yuqori qator: `Lv. N` (katta, `accent`), o'ngda seriya chipi (`N kun`) va mavjud ulashish tugmasi (`headerAction`).
2. Avatar sahnasi: Tizim — portal halqasi ichida, Neon — tungi shahar silueti (oddiy to'rtburchak binolar + neon belgilar, `Canvas`) oldida.
   Ostida ism plitasi: unvon + `RankBadge`.
3. `GamePanel("STATUS")`: XP chizig'i (`xpIntoLevel / xpForNext`), 4 ustun — So'zlar, Darslar, Hikoyalar, Yutuqlar.
4. `GamePanel("KUNLIK TOPSHIRIQ")`: 3 qator, har biri belgi + matn + `[x/y]`:
   - Kunlik XP: `todayXp/goal`;
   - Takrorlash: due kartalar soni (0 bo'lsa bajarilgan);
   - Joriy dars: keyingi bosqich nomi (joriy dars bo'lmasa qator yo'q).
   Pastda: "Mukofot" o'rniga haqiqiy qoida — "Maqsad bajarilsa seriya davom etadi".
5. `GameButton("DAVOM ETISH · …")` — joriy darsga; joriy dars bo'lmasa — Xaritaga.
6. Ikkilamchi tugmalar qatori: Takrorlash (due bilan), O'yinlar, Xatolar, Yutuqlar.

### 5.3 Xarita (maket A2 / C2)

Ma'lumot `BookMapScreen` dagi `buildRows` dan; bo'lim (unit) holati sof funksiya bilan hisoblanadi:
`unitState(rows) = CLEARED` (ikkala dars COMPLETED/VERIFIED), `ACTIVE` (joriy dars shu bo'limda, yoki joriy yo'q bo'lsa —
birinchi tozalanmagan ochiq bo'lim), `OPEN` (ochiq, lekin faol emas), `LOCKED` (ikkala dars ham `available == false`).

- **Tizim:** "Darvozalar minorasi" — 9-bo'lim tepada, 1-bo'lim pastda; har bo'lim romb `RankBadge` bilan (bo'lim ranklari doimiy:
  1–2 E, 3–4 D, 5–6 C, 7 B, 8 A, 9 S). `ACTIVE` bo'lim kengaytirilgan panel: ikki dars, har biri bosqich chizig'i va
  "DARVOZAGA KIRISH" tugmasi. Ekran ochilganda faol bo'limga aylantiriladi.
- **Neon:** "Metro xaritasi" — 1-bekat tepada; vertikal chiziq (o'tilgan qismi `accent2` nur, qolgani punktir), bekat nuqtalari;
  `ACTIVE` bekat yonida karta. Bo'lim nomlari o'zbekcha + koreyscha (`unit_names` string-array).
- Ikkala variantda: bo'lim ichidagi darsga bosish → dars ekrani; mavjud "Tezkor tekshiruv" (PASSED) tugmasi saqlanadi.

### 5.4 Hikoya epizodi (maket A3 / C3)

`Episode.kt` mantig'i (`StoryPlayer`) o'zgarmaydi, faqat ko'rinish:

- Fon: `GameBackground` + pastga qorayuvchi gradient; gapiruvchi personaj siluet-avatari (Minji, ustoz va boshqalar — bitta
  umumiy siluet, kontur rangi personajga qarab). Aziz gapirganda — joriy rankdagi `Avatar`.
- Yuqorida: yopish tugmasi, segmentli progress, `n/N`, sarlavha chipi `EPIZOD n · nomi`.
- Gap oynasi: `GamePanel` ichida ism, koreyscha matn, tarjima, audio tugma.
- Tanlov: `GamePanel("TANLOV")`, prompt, 3 variant (raqamli tugmalar); xato variant hozirgidek o'chadi va `why_uz` ko'rinadi.

## 6. Xatolar va chekka holatlar

- `ui_theme` yoki `last_seen_rank` da noma'lum qiymat → standart (SYSTEM / `null`), ilova yiqilmaydi.
- Avatar rasm fayli buzilgan bo'lsa (decode xatosi) → vektorga qaytadi.
- Juda kichik ekran (360×640 dp): Bosh sahifa scroll qilinadi; epizod tanlov paneli scroll qilinadi.
- Shrift yuklanmasa → tizim shrifti (Compose standart xatti-harakati).
- `xpForNext` 0 bo'lmaydi (formula bo'yicha ≥ 100), lekin bo'lish `coerceIn(0f, 1f)` bilan himoyalanadi.

## 7. Testlar

JVM (Robolectric `@Config(sdk=[35])` kerak bo'lganda):

- `RankRulesTest`: chegaralar (1,4→E; 5→D; 9→D; 10→C; 15→B; 20→A; 29→A; 30→S; 0→E); `rankUpToShow` uch holati.
- `AvatarLayersTest`: har rank oldingisining qatlamlarini o'z ichiga oladi (`HOOD` dan tashqari); E da `HOOD` bor, D+ da yo'q.
- `SettingsRepositoryTest` (yoki mavjud test kengaytmasi): `setTheme` saqlanadi; noma'lum qiymat → SYSTEM; `lastSeenRank` round-trip.
- `BackupCodecTest`: `theme` maydonisiz eski JSON → `"system"`; `"neon"` round-trip.
- `BackupServiceTest`: import'dan keyin `lastSeenRank` = tiklangan XP bo'yicha rank.
- `UnitStateTest`: CLEARED / ACTIVE / OPEN / LOCKED holatlari, joriy dars yo'q bo'lganda birinchi tozalanmagan bo'lim faol.
- `AvatarPathsTest`: har path satri `PathParser` bilan xatosiz parse bo'ladi.
- `AvatarAssetsTest`: `system_c.webp` bor bo'lsa C uchun slot topiladi, boshqa rank uchun topilmaydi.

Qurilmada qo'lda tekshirish: ikki tema almashishi, uch ekran ko'rinishi, rank oyna (debug XP bilan), kichik ekranda scroll.

## 8. Qabul mezonlari

1. Yangi o'rnatishda ilova "Tizim" temasida ochiladi; Sozlamada "Neon Seul" tanlansa, hamma ekranlar darhol o'zgaradi va qayta ishga tushirishdan keyin saqlanadi.
2. Bosh sahifa, Xarita, Epizod ikkala temada maketlarga mos ko'rinadi; boshqa ekranlar tema rangi va shriftini oladi.
3. Avatar joriy rankka mos qatlamlar bilan chiziladi; `assets/avatar/system_c.webp` qo'shilsa, C rankda Tizim temasida o'sha rasm chiqadi.
4. Rank oshganda oyna bir marta chiqadi; yangi o'rnatish va zaxiradan tiklashda chiqmaydi.
5. Eski zaxira fayli import qilinadi (tema = Tizim).
6. Barcha mavjud va yangi testlar o'tadi.
