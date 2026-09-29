# Hangul Friend — dizayn spetsifikatsiyasi

Sana: 2026-09-29
Holat: foydalanuvchi ko'rib chiqishi kutilmoqda

## 1. Maqsad

**Hangul Friend** — "사랑해요 한국어 2" (I Love Korean 2, Seul Milliy universiteti Til ta'limi instituti, Student's Book + Workbook) kitobiga bog'langan shaxsiy Android ilova. Foydalanuvchi o'zbek tilida so'zlashadi, koreys tilini kursda shu kitob bilan o'rganadi.

Ilova ikki vaziyat uchun:

1. **Darsni qoldirganda** — dars o'rnini bosadi: lug'at, grammatika tushuntirishi, dialog, mashqlar.
2. **Darsni tushunmaganda / darsdan keyin** — mustahkamlash: interaktiv mashqlar, takrorlash (spaced repetition), AI yordamchi.

**Muvaffaqiyat mezoni:** foydalanuvchi qoldirilgan darsni faqat ilova orqali o'zlashtira oladi va darsdan keyingi mustahkamlash uchun kitobga qaytishi shart bo'lmaydi.

### Cheklovlar

- Faqat shaxsiy foydalanish. Tarqatilmaydi, Play Store'ga chiqarilmaydi.
- Kitob matni, dialoglari, qahramonlari va audiosi **ko'chirilmaydi**. Ilova kontenti kitob darslarining mavzusi va grammatikasiga mos ravishda yangidan yoziladi.
- Faqat Android telefon.

### Qamrov tashqarisida

iOS, bir nechta foydalanuvchi, bulutli sinxronizatsiya, boshqa kitoblar (1, 3–6), erkin AI suhbat rejimi, Play Store'ga chiqarish.

## 2. Kontent qamrovi

9 bo'lim × 2 dars = 18 dars. Mundarija kitob do'konlaridagi (YES24) ma'lumotdan olingan:

| Bo'lim | 1-dars | 2-dars |
|---|---|---|
| 1. 가족 | 이분은 우리 아버지세요 | 어머니는 전에 무슨 일을 하셨어요? |
| 2. 쇼핑 | 한번 입어 보세요 | 더 긴 치마는 없어요? |
| 3. 여행 | Havo yaxshi bo'lsa, Hallasan'ga boraman | Makguksu — Gangvondoda ko'p yeyiladigan taom |
| 4. 취미 | 테니스를 배우고 싶어요 | 저는 등산하는 걸 좋아해요 |
| 5. 은행과 우체국 | 통장을 만들고 싶은데요 | 소포를 부치러 왔어요 |
| 6. 교통 | 청계천에 어떻게 가야 돼요? | Piyodalar o'tish joyidan o'tib, o'ngga buriling |
| 7. 병원 | 내일 모임에 올 수 있어요? | 목이 아파서 왔어요 |
| 8. 한국 생활 | Stress bo'lganda musiqa tinglayman yoki do'stim bilan gaplashaman | 시험을 볼 때 연필로 써도 돼요? |
| 9. 전화 | Allo, Seul universitetimi? | Choy ichib turib kitob o'qiyapman |

Har bir darsning grammatika ro'yxati dars nomidan taxmin qilingan (masalan, `-(으)세요`, `-아/어 보다`, `-(으)면`, `-(으)ㄴ데요`, `-(으)러 오다`, `-아/어야 되다`, `-(으)ㄹ 수 있다`, `-아/어서`, `-거나`, `-(으)ㄹ 때`, `-아/어도 되다`, `-(으)면서`). Har bir dars yozilishidan oldin foydalanuvchi o'z kitobidan tasdiqlaydi yoki tuzatadi.

## 3. Foydalanuvchi tajribasi

### 3.1 Birinchi kirish (onboarding)

1. Qisqa salomlashuv, keyin kitob xaritasi ochiladi va "Hozir qaysi darsdasiz?" deb so'raladi.
2. Tanlangan darsdan oldingi darslar **"o'tilgan"** holatini oladi (test bilan tasdiqlanmagan, alohida belgi bilan ko'rsatiladi).
3. O'tilgan darslarning kartalari takrorlash navbatiga **"yangi"** sifatida qo'shiladi. Ular kunlik yangi karta limiti bo'yicha asta-sekin chiqadi (standart: kuniga 20 ta, sozlamalarda o'zgartiriladi).
4. Tanlangan dars "Darsni davom ettirish" tugmasiga ulanadi. Joriy darsni sozlamalardan keyinroq o'zgartirish mumkin.

### 3.2 Ekranlar

- **Bugun (asosiy ekran):** takrorlash kerak bo'lgan kartalar soni va "Boshlash" tugmasi; "Darsni davom ettirish" (qaysi dars va bosqich); kunlik maqsad halqasi, streak, XP.
- **Kitob xaritasi:** 9 bo'lim, 18 dars. Har bir darsda progress (0–100%) va holat: boshlanmagan / o'tilgan / jarayonda / tugatilgan / tasdiqlangan. Darslar qulflanmaydi.
- **Dars ekrani:** 5 bosqich (3.3-bo'lim), hamda "Shu darsni takrorlash" va "Tezkor tekshiruv" tugmalari.
- **Takrorlash:** FSRS navbati bo'yicha aralash sessiya.
- **Lug'at bazasi:** barcha so'zlar; qidiruv, dars bo'yicha filtr, foydalanuvchi qo'shgan so'zlar.
- **Xatolar daftari:** eng ko'p xato qilingan elementlar va ular bo'yicha maxsus mashq.
- **Hikoya:** ochilgan epizodlar ro'yxati.
- **Nishonlar.**
- **Sozlamalar:** Claude API kaliti, kunlik maqsad, kunlik yangi karta limiti, eslatma vaqti, joriy dars, progress eksport/import, "Xato bor" belgilari eksporti.

### 3.3 Dars bosqichlari

1. **Lug'at.** Har bir so'z uchun karta: koreyscha, audio, o'zbekcha ma'no, misol gap (audio bilan). Har 5–6 so'zdan keyin mini-mashq.
2. **Grammatika.** Har bir grammatik shakl uchun: ma'nosi, qo'shilish qoidasi (받침 bor/yo'q, ㄹ istisnosi va boshqalar), audio bilan 4–6 ta misol, ko'p uchraydigan xatolar, o'zbek tili bilan taqqoslash, keyin nazorat mashqi. Shu yerda **"Tushunmadim"** tugmasi (AI) bor.
3. **Dialog.** Dars mavzusidagi o'z qahramonlarimiz bilan suhbat, 8–14 qator. Rejimlar: eshitish, o'qish, tarjimani ochish/yopish, rol o'ynash.
4. **Mashqlar.** 15–25 ta aralash mashq (3.4-bo'lim).
5. **Dars testi.** 15–20 savol. 80% va undan yuqori natija bo'lsa, dars "tugatilgan" bo'ladi. Xato qilingan elementlarning FSRS kartalari `Again` bahosini oladi.

**Tezkor tekshiruv** (o'tilgan darslar uchun): 10–12 savol. 80% va undan yuqori bo'lsa, dars "tasdiqlangan" bo'ladi; to'g'ri javob berilgan kartalar `Good` bahosi bilan boshlanadi.

**Bo'lim oxiri — "Boss jangi":** bo'limning 2 darsidan 15 savol, 3 ta jon, har bir xato bitta jonni oladi. Barcha jonlar tugasa, qayta urinish taklif qilinadi.

**Yakuniy test:** 18 dars tugagach, TOPIK I uslubidagi test (eshitish va o'qish).

### 3.4 Mashq turlari

| Turi (`type`) | Maqsad | Tavsif |
|---|---|---|
| `flashcard` | lug'at | ko→uz; foydalanuvchi o'zini baholaydi (Qayta/Qiyin/Yaxshi/Oson) |
| `reverse_typing` | lug'at | uz→ko, klaviaturada yoziladi |
| `match` | lug'at | 5 so'zni 5 ma'noga ulash |
| `listen_choose` | lug'at | audioni eshitib variant tanlash |
| `dictation` | lug'at/gap | audioni eshitib yozish |
| `situation_choice` | lug'at | vaziyatga mos so'zni tanlash |
| `conjugate` | grammatika | asos + shakl → natijani yozish (`가다` + `-(으)면` → `가면`) |
| `fill_blank` | grammatika | gapdagi bo'sh joyga shaklni yozish |
| `build_sentence` | grammatika | aralashtirilgan bloklardan gap yig'ish |
| `find_error` | grammatika | xatoni topib tuzatish |
| `translate` | grammatika | uz→ko tarjima; AI tekshiradi, offline'da namunaviy javoblar bilan solishtiriladi |
| `speak` | talaffuz | gapni aytish, nutqni tanish orqali tekshirish |
| `roleplay` | dialog | ilova bir tomonni gapiradi, foydalanuvchi ikkinchisini aytadi |
| `listen_question` | eshitish | dialog/gapni eshitib savolga javob berish |

Umumiy qoidalar:

- Sessiya ichida bir xil tur ketma-ket ikki marta kelmaydi.
- Har bir noto'g'ri javobdan keyin qisqa o'zbekcha izoh (`why_uz` yoki `checker` feedback) va "AI'dan so'rash" tugmasi chiqadi.
- Har bir mashqda "Xato bor" belgisi bor. U element ID'sini mahalliy ro'yxatga yozadi.

### 3.5 Interaktivlik va motivatsiya

- **Tezkor javob:** to'g'ri/noto'g'ri animatsiya, ovoz, vibratsiya, combo hisoblagichi (3, 5, 10 ketma-ket).
- **Mini o'yinlar:** Tezlik raundi (60 s, rekord saqlanadi), Xotira o'yini (kartalarni ochib juftlash), Hangul poygasi (tez va xatosiz yozish), Grammatika zanjiri (bitta fe'lni ketma-ket shakllarga solish). Mini o'yinlar FSRS jadvaliga ta'sir qilmaydi, faqat XP beradi.
- **Hikoya rejimi:** o'z qahramonlarimiz (Seulga o'qishga kelgan o'zbek talaba va uning tanishlari). Har bir dars tugaganda bitta epizod ochiladi. Epizod faqat shu darsgacha o'rganilgan so'z va grammatikadan tuziladi, audio bilan beriladi.
- **XP:** to'g'ri javob 10, combo bonusi +5, dars tugatish 100, Boss g'alabasi 200, kunlik maqsad 50. Daraja = XP chegaralari jadvali bo'yicha.
- **Streak:** kunlik maqsad bajarilgan ketma-ket kunlar.
- **Nishonlar:** belgilangan ro'yxat (birinchi dars, birinchi bo'lim, 100/500 so'z, 7/30 kunlik streak, Boss g'alabalari va h.k.).

## 4. Arxitektura

### 4.1 Texnologiyalar

- Kotlin, Jetpack Compose (Material 3), Navigation Compose.
- Room (ma'lumotlar bazasi), DataStore (sozlamalar).
- kotlinx.serialization (kontent JSON).
- Media3 ExoPlayer (audio).
- Android `SpeechRecognizer` (`ko-KR`).
- WorkManager (eslatmalar).
- Rasmiy Anthropic Java SDK (`com.anthropic:anthropic-java`, Kotlin'dan chaqiriladi).
- DI: qo'lda yozilgan `AppContainer` (Hilt ishlatilmaydi — bitta foydalanuvchili kichik ilova uchun ortiqcha).
- `minSdk` 26; `compileSdk` va `targetSdk` loyiha yaratilgan paytdagi eng so'nggi barqaror versiya.
- Paket: `uz.hangulfriend`.

### 4.2 Gradle modullari

| Modul | Turi | Vazifasi |
|---|---|---|
| `:core:srs` | sof Kotlin (JVM) | FSRS algoritmi (eng so'nggi FSRS versiyasi, standart parametrlar, `open-spaced-repetition` reference implementatsiyasidan port qilinadi) |
| `:core:hangul` | sof Kotlin (JVM) | Hangul bo'g'inini jamo'larga ajratish, javobni normallashtirish, jamo darajasidagi farq va o'xshashlik (Levenshtein) |
| `:app` | Android | qolgan hammasi |

`:app` ichidagi paketlar:

| Paket | Vazifasi |
|---|---|
| `content` | `assets/lessons/*.json` ni o'qish va modelga aylantirish; kontent faqat o'qiladi |
| `data` | Room: progress, FSRS karta holati, javoblar logi, XP, nishonlar, foydalanuvchi so'zlari, "Xato bor" belgilari |
| `session` | kontent va FSRS navbatidan sessiya tuzish; mashq turlarini almashtirish qoidasi |
| `exercises` | har bir mashq turi uchun Composable va tekshiruvchi |
| `speech` | `SpeechRecognizer` o'rami va natijani baholash |
| `audio` | audio fayllarni ijro etish |
| `ai` | Claude API mijozi, promptlar, xatolarni boshqarish |
| `game` | XP, streak, nishonlar, mini o'yinlar, Boss |
| `story` | hikoya epizodlari |
| `reminders` | WorkManager eslatmalari |
| `ui` | ekranlar, navigatsiya, tema |

### 4.3 Ma'lumotlar oqimi

```
assets/lessons/*.json ──► content ──► session ──► exercises (UI)
                                         ▲              │ javob
                                         │              ▼
                         data (Room) ◄── srs ◄── checker (:core:hangul)
                                         │
                                         └──► game (XP, nishon, streak)
```

### 4.4 Kontent formati

Har bir dars — bitta fayl: `app/src/main/assets/lessons/u05_l1.json`.

```json
{
  "id": "u05_l1",
  "unit": 5,
  "lesson": 1,
  "title_ko": "통장을 만들고 싶은데요",
  "title_uz": "Hisob ochmoqchi edim",
  "topic_uz": "Bank va pochta",
  "reviewed": false,
  "words": [
    {
      "id": "u05_l1_w001",
      "ko": "통장",
      "uz": "bank daftarchasi, hisob",
      "pos": "noun",
      "example_ko": "통장을 만들고 싶어요.",
      "example_uz": "Hisob ochmoqchiman.",
      "audio": "a1b2c3.ogg",
      "example_audio": "d4e5f6.ogg"
    }
  ],
  "grammar": [
    {
      "id": "u05_l1_g1",
      "pattern": "-(으)ㄴ데요",
      "meaning_uz": "Muloyim so'rov yoki vaziyatni tushuntirib, javob kutish",
      "explanation_md": "…o'zbekcha tushuntirish (Markdown)…",
      "formation": [
        { "condition_uz": "Sifat, 받침 bor", "rule": "-은데요", "example": "작다 → 작은데요" },
        { "condition_uz": "Sifat, 받침 yo'q", "rule": "-ㄴ데요", "example": "크다 → 큰데요" },
        { "condition_uz": "Fe'l", "rule": "-는데요", "example": "가다 → 가는데요" }
      ],
      "examples": [ { "ko": "…", "uz": "…", "audio": "…" } ],
      "mistakes": [ { "wrong": "…", "right": "…", "why_uz": "…" } ],
      "uz_compare": "…o'zbek tilidagi o'xshash qurilma…"
    }
  ],
  "dialogue": {
    "lines": [ { "speaker": "aziz", "ko": "…", "uz": "…", "audio": "…" } ]
  },
  "exercises": [
    {
      "id": "u05_l1_e001",
      "type": "fill_blank",
      "targets": ["u05_l1_g1"],
      "prompt_uz": "Bo'sh joyni to'ldiring",
      "sentence": "통장을 만들고 ___ (싶다)",
      "answers": ["싶은데요"],
      "hint_uz": "싶다 — sifatdek tuslanadi",
      "why_uz": "…"
    }
  ],
  "test": ["u05_l1_e010", "u05_l1_e011"],
  "story": {
    "title_uz": "…",
    "lines": [ { "speaker": "…", "ko": "…", "uz": "…", "audio": "…" } ]
  }
}
```

Qoidalar:

- Barcha ID'lar global miqyosda yagona va barqaror bo'ladi. FSRS holati ID'ga bog'lanadi, shuning uchun ID o'zgarsa progress yo'qoladi.
- `answers` — qabul qilinadigan barcha to'g'ri javoblar ro'yxati.
- `targets` — mashq qaysi so'z yoki grammatika ID'larini tekshirayotgani.
- Audio fayl nomi = `sha1(voice + "|" + text)` ning birinchi 12 belgisi. Generator faqat yangi matnlar uchun audio yaratadi.
- Qahramonlar: `app/src/main/assets/characters.json` (ID, ism, TTS ovozi).

### 4.5 Takrorlash (FSRS)

- **Karta turlari:** har bir so'z uchun 2 ta karta (`ko→uz` tanish, `uz→ko` yozish); har bir grammatika uchun 1 ta karta.
- **Grammatika kartasi takrorlanganda** shu grammatikani `targets`da ko'rsatgan mashqlar hovuzidan tasodifiy bittasi beriladi.
- **Baho qanday olinadi:**
  - `flashcard` mashqida foydalanuvchi o'zi baholaydi (4 tugma).
  - Boshqa mashq turlarida baho avtomatik: noto'g'ri → `Again`; to'g'ri, lekin yordam (hint) ishlatilgan yoki javob 15 soniyadan uzoq davom etgan → `Hard`; to'g'ri → `Good`.
- **Kunlik navbat:** avval muddati kelgan kartalar, keyin kunlik limit doirasida yangi kartalar.
- **Karta qachon yaratiladi:** dars lug'at yoki grammatika bosqichida ko'rilganda, yoki onboarding'da "o'tilgan" deb belgilanganda.

### 4.6 Javob tekshiruvchi (`:core:hangul`)

- **Normallashtirish:** chetdagi bo'sh joylar olib tashlanadi, ichki bo'sh joylar bittaga qisqartiriladi, oxirgi tinish belgilari (`. ? !`) e'tiborga olinmaydi, Unicode NFC.
- **Solishtirish:** normallashtirilgan javob `answers` ro'yxatidagi biror javobga to'liq teng bo'lsa, javob to'g'ri.
- **Xato bo'lsa**, eng yaqin to'g'ri javob bilan jamo darajasida solishtiriladi va aniq izoh beriladi:
  - "받침 xato" (faqat oxirgi undosh farq qiladi);
  - "unli xato";
  - "bo'sh joy xato" (bo'sh joylarsiz solishtirilganda to'g'ri chiqadi);
  - "bitta harf xato" (jamo masofasi 1 ga teng);
  - boshqa holatlarda umumiy "noto'g'ri".
- **Gapirish mashqi:** nutqni tanish natijasidagi eng yaxshi variant kutilgan gap bilan jamo darajasida solishtiriladi. O'xshashlik 85% va undan yuqori bo'lsa, javob o'tadi. Farq qilgan bo'g'inlar qizil rangda ko'rsatiladi.

### 4.7 AI yordamchi

- **Model:** `claude-opus-5-5`. Server tomonidagi zaxira model (fallback) yoqilgan: `fallbacks: "default"`.
- **Chaqiruv:** rasmiy Anthropic Java SDK orqali, javoblar streaming bilan chiqariladi.
- **Funksiyalar:**
  1. **"Tushunmadim" chat.** Grammatika yoki mashq ekranidan ochiladi. System prompt'ga quyidagilar kiradi: "o'zbek tilida so'zlashuvchi TOPIK I/2-daraja o'quvchisi uchun koreys tili o'qituvchisi" roli, joriy grammatika kartasining to'liq JSON'i va dars so'zlari. Javob o'zbek tilida, qisqa, misollar bilan bo'ladi. Suhbat tarixi faqat ekran ochiq turganda saqlanadi.
  2. **Tarjimani tekshirish.** Structured output bilan javob qaytadi: `{ correct: bool, corrected_ko: string, explanation_uz: string }`.
  3. **Xatoni tushuntirish.** Noto'g'ri javobdan keyingi "AI'dan so'rash" tugmasi. Chat ochiladi va unga mashq, foydalanuvchi javobi va to'g'ri javob konteksti yuboriladi.
- **Effort:** `low`. Sifat yetmasa, `medium` ga ko'tariladi.
- **Prompt caching:** system prompt va dars konteksti keshlanadi.
- **Kalit:** Android Keystore bilan shifrlangan DataStore'da saqlanadi. Kod va repo ichiga yozilmaydi.
- **Taxminiy narx:** $4 / $20 per 1M token hisobida bitta savol taxminan $0.01–0.03 turadi.

### 4.8 Audio

- **Generator:** `tools/audio_gen.py`. Google Cloud Text-to-Speech ishlatiladi: `ko-KR` neural ovozlar, bitta ayol va bitta erkak ovozi, OGG Opus formatida.
- Generator barcha dars JSON'laridagi matnlarni yig'adi va audiosi yo'q matnlar uchun fayl yaratadi. Natija `app/src/main/assets/audio/` ga yoziladi.
- Dialog va hikoyada har bir qahramon o'z ovozida gapiradi (`characters.json` bo'yicha).
- Taxminiy hajm: 1500–2000 fayl, 25–40 MB. Google Cloud'ning bepul limiti yetadi.
- Google Cloud kaliti faqat kompyuterda, muhit o'zgaruvchisi orqali beriladi.

### 4.9 Yordamchi skriptlar (`tools/`, Python)

- `validate_content.py` — JSON sxemasini, ID'larning yagonaligini, `targets` havolalarini, `test` havolalarini va audio fayllar mavjudligini tekshiradi.
- `audio_gen.py` — 4.8-bo'limda tavsiflangan.

## 5. Xatolar va chekka holatlar

| Holat | Xatti-harakat |
|---|---|
| Internet yo'q | AI tugmalarida "offline" holati ko'rsatiladi; `translate` mashqi namunaviy javoblar bilan solishtiriladi; qolgan hammasi ishlaydi |
| API kaliti yo'q | AI tugmasi bosilganda sozlamalarga yo'naltiriladi |
| API xatosi (429, 5xx, tarmoq) | SDK o'zi qayta urinadi; baribir bo'lmasa, o'zbekcha xabar va "Qayta urinish" tugmasi |
| API xatosi (401) | "API kaliti noto'g'ri" xabari va sozlamalarga o'tish |
| Model rad etdi (`refusal`) | "Bu savolga javob bera olmayman" xabari chiqadi, ilova ishlashda davom etadi |
| `ko-KR` nutqni tanish yo'q | `speak` va `roleplay` mashqlari sessiyadan chiqariladi; sozlamalarda o'rnatish yo'riqnomasi |
| Mikrofon ruxsati berilmagan | Ruxsat so'raladi; rad etilsa, gapirish mashqlari o'tkazib yuboriladi |
| Koreys klaviaturasi yo'q | Birinchi yozish mashqida Gboard'da koreys tilini yoqish yo'riqnomasi; "Qayta ko'rsatma" belgisi |
| Audio fayl yo'q | Audio tugmasi o'chirilgan holda ko'rsatiladi; release build'da bunday holat validator tufayli bo'lmasligi kerak |
| Kontent JSON buzilgan | Debug build'da ilova ishga tushganda xato beradi; release'da bu dars "mavjud emas" deb ko'rsatiladi |
| Progress yo'qolishi | Android Auto Backup yoqilgan; sozlamalarda JSON faylga eksport/import |
| Room sxemasi o'zgarishi | Faqat yozilgan va testlangan migratsiyalar orqali; destructive fallback taqiqlangan |

## 6. Test strategiyasi

- **Unit (JVM):**
  - `:core:srs` — FSRS reference implementatsiyasining test vektorlari bilan solishtirish;
  - `:core:hangul` — jamo'larga ajratish, normallashtirish, har bir feedback turi;
  - `session` — mashq turlari almashinuvi va kunlik limit;
  - avtomatik baholash qoidalari;
  - XP va streak hisoblash.
- **Kontent testi:** barcha `assets/lessons/*.json` fayllari xatosiz yuklanadi va validator qoidalariga javob beradi. Bu test ham `tools/validate_content.py` da, ham JVM testi sifatida ishlaydi.
- **Room:** DAO va migratsiya testlari (instrumented).
- **UI (Compose):** onboarding, bitta darsni to'liq o'tish, takrorlash sessiyasi.
- **AI:** mijoz interfeys ortida turadi va testlarda soxta implementatsiya bilan almashtiriladi. Haqiqiy API faqat qo'lda tekshiriladi.
- **Qo'lda tekshiruv:** har bir bosqich oxirida foydalanuvchining telefonida.

**Kontent sifati:**

- Har bir dars JSON'ida `reviewed` maydoni bor. Foydalanuvchi yoki o'qituvchi darsni tekshirganidan keyin u `true` qilinadi.
- "Xato bor" belgilari eksport qilinadi va ular asosida kontent tuzatiladi.

## 7. Qurish bosqichlari

Har bir bosqich tugaganda telefonda ishlatsa bo'ladigan ilova bo'ladi. Har bir bosqich uchun alohida amalga oshirish rejasi yoziladi.

1. **Asos.** Loyiha va modullar yaratiladi; `:core:hangul`, `:core:srs`, kontent formati va validator tayyorlanadi; foydalanuvchi hozir o'qiyotgan 2-bo'lim (쇼핑) ning ikkala darsi (`u02_l1`, `u02_l2`) to'liq yoziladi. Kitob xaritasida 18 darsning hammasi ko'rinadi, kontenti hali yozilmaganlari "tez orada" deb belgilanadi. Shu bosqichda onboarding, kitob xaritasi, dars bosqichlari (lug'at, grammatika, dialog — hozircha audiosiz), matnli mashq turlari (`flashcard`, `reverse_typing`, `match`, `situation_choice`, `conjugate`, `fill_blank`, `build_sentence`, `find_error`, `translate` offline rejimda), dars testi, FSRS takrorlash va sozlamalar (asosiy) quriladi.
2. **Audio va nutq.** `audio_gen.py`, audio ijrosi, `listen_choose`, `dictation`, `listen_question`, `speak`, `roleplay`.
3. **AI yordamchi.** "Tushunmadim" chat, `translate` tekshiruvi, xatoni tushuntirish.
4. **O'yin elementlari.** XP, daraja, streak, nishonlar, combo, mini o'yinlar, Boss jangi, xatolar daftari, tezkor tekshiruv.
5. **Hikoya va eslatmalar.** Hikoya rejimi, WorkManager eslatmalari, progress eksport/import.
6. **Qolgan 16 dars kontenti.** Tartib: avval 1-bo'lim (o'tilgan, takrorlash uchun), keyin 3–9-bo'limlar ketma-ket. 2-bosqichdan boshlab parallel yozilishi mumkin; maqsad — kitobning to'liq 18 darsi ilovada bo'lishi. Har bir dars telefonda tekshiriladi. Oxirida TOPIK I uslubidagi yakuniy test qo'shiladi.
