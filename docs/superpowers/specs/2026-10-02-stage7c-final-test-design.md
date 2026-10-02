# 7c-bosqich — Yakuniy TOPIK I uslubidagi test

Sana: 2026-10-02
Holat: spec (foydalanuvchining "oxirigacha tartib bilan" ko'rsatmasi bo'yicha davom ettirilmoqda)
Asosiy spec: `2026-09-29-hangul-friend-design.md` §3.3 ("Yakuniy test: 18 dars tugagach, TOPIK I uslubidagi test (eshitish va o'qish)").
Ustiga quriladi: 7b (PR #20). DB o'zgarmaydi.

## 1. Maqsad va tanlov

Foydalanuvchi tanlovi: **qisqa — 30 savol (15 eshitish + 15 o'qish), ~25 daqiqa**, butun kitobdan, natija bo'limlar bo'yicha,
qayta topshirsa bo'ladi.

## 2. Kontent

`assets/final_test.json`:

```json
{ "listening": [ /* 15 ta listen_question */ ], "reading": [ /* 15 ta read_choice */ ] }
```

- ID'lar: `final_l01…final_l15`, `final_r01…final_r15`; `targets` bo'sh (FSRS'ga ta'sir qilmaydi).
- **Eshitish** (`listen_question`): `audio_text` — 1–3 qatorli qisqa suhbat yoki e'lon (A/B qatorlari ` / ` bilan ajratiladi),
  `prompt_uz` — savol, 4 ta variant, bitta javob, `why_uz` — izoh. Audio `tools/audio_gen.py` bilan (ayol ovozi), endi u
  `final_test.json`ni ham qayta ishlaydi.
- **O'qish** (yangi tur `read_choice`): `sentence` — koreyscha matn (e'lon, xabar, qisqa matn; 1–5 qator), `prompt_uz` — savol,
  4 ta variant, bitta javob, `why_uz`.
- Mavzular 1–9-bo'limlardan teng taqsimlanadi (har bo'limdan 3–4 savol), grammatika `docs/content/grammar-map.md` dagilar.
- `LessonValidator` `read_choice` ni biladi: `sentence` bo'sh emas, 4 ta turli variant, javob variantlar ichida.
- `ContentAssetsTest.finalTestIsValid`: 15 + 15, ID'lar noyob va formatga mos, har eshitish savolida mavjud audio fayl,
  har savol validatordan o'tadi.

## 3. Sessiya

- `SessionMode.FINAL("final")`. Savollar: avval 15 eshitish, keyin 15 o'qish (aralashtirilmaydi, TOPIK tartibi).
- Javob uchun XP berilmaydi va FSRS yangilanmaydi; yuraklar yo'q; "Xato bor" bayrog'i ishlaydi.
- **Vaqt:** 25:00 teskari sanoq sessiya ekranining tepasida. Vaqt tugasa, qolgan savollar noto'g'ri hisoblanadi va natija chiqadi.
- **Natija ekrani:** umumiy `N/30`, Eshitish `x/15`, O'qish `y/15`, foiz, taxminiy daraja:
  ≥ 70% — "TOPIK I 2-daraja darajasida", ≥ 40% — "TOPIK I 1-daraja darajasida", aks holda "Hali erta — darslarni takrorlang".
  Eng yaxshi natija `best_scores` jadvalida (`final_test`, foiz). Birinchi marta ≥ 70% bo'lganda +100 XP (`REASON_FINAL = "final"`).
- Sof funksiyalar: `GameRules.topikLevel(percent): Int` (0/1/2), `FinalScore(listening, reading)`.

## 4. Kirish

Xaritaning oxirida (Tizim: minoraning eng tepasida, 9-bo'limdan yuqorida; Neon: metro chizig'ining oxirgi bekati) **"Yakuniy sinov"**
kartasi: savollar soni, vaqt, eng yaxshi natija (bo'lsa), "Boshlash" tugmasi. Test har doim ochiq; 18 dars tugamagan bo'lsa,
kartada "Kitob oxirida topshirish tavsiya etiladi (N/18 dars tugagan)" yozuvi.

## 5. Testlar

- `GameRulesTest.topikLevel` (39 → 0, 40 → 1, 69 → 1, 70 → 2, 100 → 2).
- `LessonValidatorTest` — `read_choice` to'g'ri/noto'g'ri holatlari.
- `ContentAssetsTest.finalTestIsValid`.
- `SessionBuilderTest.finalKeepsOrder` (eshitish oldin, tartib saqlanadi, `cardIds` bo'sh).
- `SessionControllerTest`: FINAL rejimida XP berilmaydi; `timeUp()` qolganlarini noto'g'ri deb tugatadi; bo'lim ballari to'g'ri;
  birinchi ≥ 70% da +100 XP, ikkinchi marta yo'q.
- `test_audio_gen.py`: `final_test.json` dagi eshitish klipi yig'iladi.

## 6. Qabul mezonlari

1. Xaritada "Yakuniy sinov" kartasi bor; test 30 savoldan iborat, eshitish audiolari ijro etiladi.
2. 25 daqiqa tugasa test o'zi yakunlanadi; natija bo'limlar va taxminiy daraja bilan chiqadi; eng yaxshi natija saqlanadi.
3. Test XP/FSRS'ni buzmaydi (faqat birinchi ≥ 70% bonusi). Barcha testlar o'tadi.
