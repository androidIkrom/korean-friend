# 6b-bosqich — yigit/qiz avatari, ko'z ifodasi, olov aurasi, ikonka va "Hangul Hunt" nomi

Sana: 2026-10-01
Holat: tasdiqlangan dizayn (2026-10-01), spec ko'rib chiqilmoqda
Oldingi bosqich: `2026-10-01-stage6-game-ui-design.md` (PR #17)
Maketlar: https://claude.ai/artifact/UbDSZSdCsS13kyNhkVCKZh — "Ko'z ifodasi", "Aura uslublari" (1 · Soya alangasi),
"Evolyutsiya · Yigit", "Evolyutsiya · Qiz", "Ilova ikonkasi" (Monarx nigohi)

## 1. Maqsad

Foydalanuvchi so'rovlari:

1. Avatar **yigit va qiz uchun alohida** bo'lsin.
2. Ko'z effekti daraja oshgani sari **ishonch va kuchni** bildirsin (Solo Leveling uslubi).
3. Aura — **Soya alangasi**, lekin burchakli emas: **olovga o'xshash, yumaloq bo'laklar**.
4. Ilova ikonkasi — **Monarx nigohi**.
5. Ilova nomi — **Hangul Hunt**.

### Qamrov tashqarisida

Paket nomi (`uz.hangulfriend`) va repo nomi o'zgarmaydi — o'zgarsa, telefondagi ilova yangi ilova sifatida o'rnatiladi va progress yo'qoladi.
Boshqa aura uslublari (energiya oqimi, sehrli doira). Avatar animatsiyasi auradan tashqari (ko'z pirpiratish va h.k.).
Qolgan ekranlarni qayta chizish.

## 2. Personaj tanlovi

- `enum class HeroGender(val key: String) { BOY("boy"), GIRL("girl") }` (`data` paketida, `GameThemeId` yonida).
- `Settings.hero: HeroGender = BOY`; DataStore kaliti `hero` (noma'lum qiymat → `BOY`); `SettingsRepository.setHero(g)`.
- **Onboarding:** "Personajingiz" savoli lesson tanlovidan oldin: ikki karta (Yigit / Qiz), har birida D rankdagi avatar.
  Standart tanlov — Yigit. "Davom etish" bosilganda saqlanadi.
- **Sozlamalar:** tema qatori ostida "Personaj" qatori — xuddi shunday ikki karta.
- Mavjud foydalanuvchilar (onboarding o'tgan): `hero` yo'q → Yigit; Sozlamadan o'zgartiradi.
- **Zaxira nusxa:** `BackupSettings.hero: String = "boy"`; eski fayllar → Yigit; `BackupCodec.VERSION` o'zgarmaydi.
- Bosh sahifadagi ism plitasidan `AZIZ` olib tashlanadi (avatar endi foydalanuvchining o'zi, ismi so'ralmaydi):
  plitada unvon va rank belgisi qoladi.
- Hikoya epizodlarida Aziz gapirganda — har doim yigit avatari (hikoya qahramoni Aziz), tanlovdan qat'i nazar.

## 3. Avatar

### 3.1 Qatlamlar

`layersFor(rank, hero)`; qatlamlar rank bilan qo'shiladi (E→S):

| Rank | Yigit | Qiz |
|---|---|---|
| E | kapyushon, yoyiq kiyim | yoyiq uzun soch, yelkadagi kapyushon |
| D | kurtka, kontur nuri | kurtka, kontur nuri |
| C | uzun plash yoqasi, olov aurasi boshlanadi | + soch baland bog'lanadi (dum soch, binafsha bog'ich), qisqa yon tolalar |
| B | yelka zirhlari, uchqunlar | yelka zirhlari, uchqunlar |
| A | mantiya, egizak xanjar, sochda nur | mantiya, orqada uzun qilich (dastasi o'ng yelka ustida) |
| S | monarx aurasi (binafsha), soya askarlari | monarx aurasi, nurli toj, soya askarlari |

Neon temada: aura `accent` (magenta), S da `top` (oltin); soya askarlari o'rniga dronlar (6-bosqichdagidek).

### 3.2 Ko'z ifodasi (maket "Ko'z ifodasi")

Har rankda ko'z, qosh va og'iz birga o'zgaradi:

| Rank | Ko'z | Qosh | Og'iz | Nur |
|---|---|---|---|---|
| E | dumaloq, oq qorachiq + qora gavhar + yorug' nuqta | ichki uchi ko'tarilgan (ikkilanish) | biroz pastga | yo'q |
| D | dumaloq, sokin | tekis | to'g'ri | yo'q |
| C | qisilgan qovoq, nurli gavhar | ichkariga pastga egilgan (qat'iyat) | yengil kulgi | yengil (r 9) |
| B | o'tkir qovoq | qalin, keskin | ishonchli kulgi | o'rta (r 12) |
| A | o'tkir, oq-qaynoq markaz | keskin | ishonchli | kuchli (r 15) + qisqa nur izi |
| S | binafsha, oq-qaynoq markaz | keskin | xotirjam | eng kuchli (r 19) + ko'zdan orqaga alanga izi |

Qizda yuqori qovoq ustida qora kiprik chizig'i qo'shiladi; ko'z joylashuvi yuz shakliga mos (+3, +2) siljiydi.
Path qiymatlari maket generatoridan (`evo2.py`) olinadi va `AvatarPaths.kt` ga ko'chiriladi.

### 3.3 Olov aurasi (maket "Aura uslublari" 1-variant, yumaloq ko'rinishi)

- Kontur: tana atrofidagi ellips yoyi (205° → −25°) bo'ylab **yumaloq bo'laklar** — har bo'lak yelka → cho'qqi → yelka nuqtalari,
  hammasi Catmull-Rom orqali silliq kubik egri chiziqqa aylantiriladi (o'tkir uch yo'q).
- Bo'laklar soni `6 + 4k`, balandligi `(10 + 24k) × [0.75…1.25]` (deterministik o'zgarish), `k` — kuch: C .3, B .55, A .8, S 1.
- Ikki qatlam: tashqi (qora o'zak gradienti, rangli 1.6 px chet, ostida xira nurli nusxa) va ichki (0.86 masshtab, qora, xira chet).
- Orqada radial nur ellipsi.
- B rankdan: bosh ustida 2–6 ta tomchi-uchqun.
- **Animatsiya** (`rememberInfiniteTransition`): tashqi qatlam `scaleY 0.95↔1.05` + `skewX ±1.5°` 1.4 s, ichki qatlam teskari fazada 1.1 s;
  uchqunlar 2.6 s da 28 dp ko'tarilib so'nadi. Animatsiya faqat Bosh sahifa va "Rank oshdi" oynasida; epizod va Sozlamalardagi
  kichik avatarlar statik (`animated = false`).
- Kontur nuri (D+): tananing silueti orqasida aura rangidagi xiralashgan nusxa (`BlurMaskFilter`, API 28 dan past qurilmalarda
  xiralashmagan yupqa kontur — qabul qilinadi).

### 3.4 Rasm sloti

Nom formati o'zgaradi: `assets/avatar/<theme>_<hero>_<rank>.webp` (`system_girl_c.webp`). README yangilanadi.

## 4. Ilova ikonkasi — Monarx nigohi

- Adaptiv ikonka (`mipmap-anydpi-v26/ic_launcher.xml` va `ic_launcher_round.xml`): 
  - `drawable/ic_launcher_background.xml` — 108×108 vektor, radial gradient `#3A1F78 → #120A28 → #04060C` (markaz 50%, 60%);
  - `drawable/ic_launcher_foreground.xml` — soch silueti (`#0A0716`, chet `#3B2A6B`), ikki o'tkir ko'z (`#A98BFF`, markazi `#E9DDFF`),
    qoshlar, chap ko'zdan alanga izi (gradient `#E9DDFF → #A98BFF → shaffof`). Ko'z nuri (blur) VectorDrawable'da yo'q —
    o'rniga yarim shaffof kattaroq ellips;
  - `drawable/ic_launcher_monochrome.xml` — Android 13 tematik ikonka: faqat ko'zlar, qoshlar va iz (oq).
- Manifest: `android:icon="@mipmap/ic_launcher"`, `android:roundIcon="@mipmap/ic_launcher_round"`.
- Hamma muhim shakllar markaziy 66 dp xavfsiz doira ichida.

## 5. Nom — Hangul Hunt

- `app_name` = `Hangul Hunt`.
- `onboarding_intro`, `share_footer`, `ShareCardRenderer` sarlavhasi va bildirishnoma kanali nomi (agar nom ishlatilsa) — "Hangul Hunt".
- Repo, paket, `HangulFriendApp`/`HangulFriendTheme` kabi kod nomlari o'zgarmaydi.

## 6. Testlar

- `HeroSettingsTest` (yoki `SettingsRepositoryTest` kengaytmasi): standart BOY; `setHero(GIRL)` saqlanadi; noma'lum → BOY; `replaceAll` yozadi.
- `BackupCodecTest`: `hero` maydonisiz eski JSON → `"boy"`; `"girl"` round-trip. `BackupServiceTest`: eksportda `hero`.
- `AvatarLayersTest`: yigit/qiz uchun kumulyativlik; qizda C+ da `PONYTAIL`, E–D da `LONG_HAIR`; A da yigit `TWIN_BLADES`, qiz `SWORD`;
  S da qizda `TIARA`.
- `EyeStyleTest`: `eyeStyleFor(rank)` — E/D da nur yo'q, C–S da nur radiusi o'sib boradi, faqat A da qisqa iz, faqat S da alanga izi va binafsha rang.
- `FlameShapeTest`: `flameOutline(k, scale, seed)` — nuqtalar soni `2 + 4·lobes + 1 + 1`; barcha cho'qqilar asosdan yuqorida;
  ketma-ket ikki nuqta orasidagi burilish burchagi 100° dan oshmaydi (o'tkir uch yo'q).
- `AvatarPathsTest`: yangi pathlar ham parse bo'ladi. `AvatarAssetsTest`: yangi nom formati.
- `ContentAssetsTest` o'zgarmaydi. Ikonka XML'lari `assembleDebug` bilan tekshiriladi.

Qurilmada: ikonka bosh ekranda (doira/kvadrat/tematik), onboarding'da personaj tanlash, Sozlamada almashtirish, olov animatsiyasi.

## 7. Qabul mezonlari

1. Ilova bosh ekranda "Hangul Hunt" nomi va Monarx nigohi ikonkasi bilan ko'rinadi; Android 13+ da tematik ikonka ishlaydi.
2. Onboarding va Sozlamada Yigit/Qiz tanlanadi; Bosh sahifa va "Rank oshdi" oynasi tanlangan personajni ko'rsatadi.
3. Ko'zlar rank bo'yicha maketdagidek o'zgaradi; S da binafsha alanga izi bor.
4. Aura yumaloq olov bo'laklari bilan, Bosh sahifada lipillab turadi; o'tkir uchlar yo'q.
5. Eski zaxira fayli import qilinadi (personaj = Yigit). Barcha testlar o'tadi.
