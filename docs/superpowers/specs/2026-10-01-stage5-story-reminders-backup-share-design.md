# 5-bosqich — hikoya, eslatmalar, eksport/import, natija kartasi

Sana: 2026-10-01
Holat: foydalanuvchi ko'rib chiqishi kutilmoqda
Asosiy spec: `2026-09-29-hangul-friend-design.md` (7-bo'lim, 5-bosqich)

## 1. Maqsad

18 darsning kontenti tayyor. Bu bosqich ilovani uzoq muddat ishlatishga moslaydi:

1. **Hikoya rejimi** — har bir dars uchun bitta interaktiv epizod: o'rganilganini "jonli" vaziyatda ko'rish.
2. **Eslatmalar** — kunlik maqsad bajarilmagan kuni bitta bildirishnoma.
3. **Eksport/import** — progressni faylga saqlash va tiklash (telefon almashganda yoki ilova qayta o'rnatilganda).
4. **Natija kartasi** — progressni rasm qilib Telegram, Instagram va boshqa ilovalarga ulashish (foydalanuvchining yangi so'rovi).

### Foydalanuvchi tanlovlari

- Ulashish: faqat **natija kartasi (rasm)**. Matnli hisobot va faylni boshqa telefonga ulashish shart emas (eksport fayli baribir Android ulashish oynasi orqali saqlanishi mumkin).
- Hikoya: **interaktiv** (tarmoqlanmaydi).

### Qamrov tashqarisida

Tarmoqlanuvchi hikoya, bir kunda bir nechta eslatma, bulutli sinxronizatsiya, matnli hisobot, avtomatik zaxira nusxa jadvali.

## 2. Hikoya rejimi

### 2.1 Kontent formati

Epizod dars JSON'idagi `story` maydonida turadi. Hozirgi `Story(titleUz, lines)` modeli `steps` ro'yxati bilan almashtiriladi (hech bir darsda hali `story` yo'q, shuning uchun moslik muammosi yo'q).

```json
"story": {
  "title_uz": "Do'kondagi birinchi kun",
  "steps": [
    { "type": "line", "speaker": "seller", "ko": "어서 오세요.", "uz": "Xush kelibsiz.", "audio": "…" },
    {
      "type": "choose_reply",
      "speaker": "aziz",
      "prompt_uz": "Aziz ko'ylakni kiyib ko'rmoqchi. U nima deydi?",
      "options": ["이거 입어 봐도 돼요?", "이거 입어요 봐도 돼요?", "이거 입으러 가요?"],
      "answer": "이거 입어 봐도 돼요?",
      "uz": "Buni kiyib ko'rsam bo'ladimi?",
      "why_uz": "-아/어 보다 + ruxsat so'rash.",
      "audio": "…"
    },
    {
      "type": "quiz",
      "prompt_uz": "Sotuvchi qaysi rangni taklif qildi?",
      "options": ["Ko'k", "Qizil", "Qora"],
      "answer": "Ko'k",
      "why_uz": "파란색 = ko'k."
    }
  ]
}
```

Qadam turlari:

| `type` | Maydonlar | Xatti-harakat |
|---|---|---|
| `line` | `speaker`, `ko`, `uz`, `audio` | Qahramon gapiradi; audio avtomatik ijro etiladi; tarjima ochiladi/yopiladi |
| `choose_reply` | `speaker` (doim `aziz`), `prompt_uz`, `options` (3 ta, koreyscha), `answer`, `uz`, `why_uz`, `audio` | Variant tanlanadi. Noto'g'ri bo'lsa `why_uz` chiqadi, variant o'chiriladi va qayta tanlanadi. To'g'ri javobdan keyin u `line` kabi ko'rsatiladi va ijro etiladi |
| `quiz` | `prompt_uz`, `options` (3 ta, o'zbekcha), `answer`, `why_uz` | Tushunishni tekshirish. Noto'g'ri bo'lsa izoh va qayta urinish |

Modelda bu `@JsonClassDiscriminator("type")` bilan `sealed interface StoryStep` bo'ladi.

### 2.2 Validator qoidalari (`LessonValidator`, `ContentAssetsTest`)

`story` bor bo'lsa:

- `line` qadamlari 10–16 ta; `choose_reply` + `quiz` jami 2–3 ta, kamida bittasi `choose_reply`;
- birinchi qadam `line`;
- `options` aniq 3 ta, takrorlanmaydi, `answer` ular orasida;
- har bir `speaker` `characters.json`da bor;
- har bir `line` va `choose_reply` uchun audio fayl mavjud.

18 ta epizod yozib bo'lingach, "har bir darsda `story` bor" qoidasi qo'shiladi.

### 2.3 Audio

`tools/audio_gen.py` `story.steps` dan `line.ko` va `choose_reply.answer` matnlarini oladi, `speaker` ovozida generatsiya qiladi va `audio` maydonini to'ldiradi.

### 2.4 Ochilish va progress

- Epizod dars holati `PASSED`, `COMPLETED` yoki `VERIFIED` bo'lganda ochiladi.
- Yangi Room jadvali `story_progress(lessonId PK, completedAtMs)`; DB versiyasi 2 → 3, `MIGRATION_2_3` va migratsiya testi.
- Epizod oxirigacha o'tilganda yozuv qo'shiladi. Faqat birinchi marta yangi `XpSource.STORY` turidagi +30 XP beriladi.
- Epizod o'rtasida chiqib ketilsa, progress saqlanmaydi (epizod 2–3 daqiqalik).

### 2.5 Ekranlar

- **Hikoya ro'yxati** (bosh ekrandan kirish): 9 bo'lim × 2 epizod. Holatlar: qulflangan (dars nomi bilan "Avval darsni o'ting"), yangi, o'tilgan ✓. `story` maydoni hali yozilmagan darslar "tez orada" deb ko'rsatiladi.
- **Epizod ekrani:** chat ko'rinishida, qadamlar birin-ketin chiqadi. "Keyingi" tugmasi yoki audio tugaganda avtomatik davom etish. Yuqorida progress chizig'i. Oxirida natija va XP.

### 2.6 Kontent yozish

18 epizodni Claude yozadi. Har bir epizod faqat shu darsgacha (kitob tartibida) o'rganilgan so'z va grammatikadan foydalanadi. Qahramonlar: Aziz, Minji va mavjud yordamchi qahramonlar.

## 3. Eslatmalar

- **Sozlamalar:** "Kunlik eslatma" kaliti (standart: o'chiq) va vaqt (standart 20:00). Yoqilganda Android 13+ da `POST_NOTIFICATIONS` ruxsati so'raladi. Rad etilsa, kalit o'chiq qoladi va izoh chiqadi.
- **Rejalashtirish:** `WorkManager` `OneTimeWorkRequest` keyingi eslatma vaqtigacha `initialDelay` bilan, `enqueueUniqueWork("daily_reminder", REPLACE)`. Worker ishlagach keyingi kunga o'zini qayta rejalashtiradi. Sozlama o'zgarganda va ilova ishga tushganda ham rejalashtiriladi. Qayta yuklashdan keyin WorkManager ishni o'zi tiklaydi.
- **Qaror** (sof funksiya `ReminderPolicy`, JVM test qilinadi): bugungi XP ≥ kunlik maqsad bo'lsa, bildirishnoma chiqmaydi. Aks holda matn tuziladi:
  - takrorlash kartalari bor: "Bugun 23 ta karta kutyapti. Streak: 12 kun 🔥";
  - karta yo'q: "Bugungi maqsadga hali yetmadingiz. 5 daqiqa mashq qilamizmi?";
  - streak 0: streak qismi yozilmaydi.
- **Bildirishnoma:** `reminders` kanali; bosilganda `MainActivity` takrorlash sessiyasini ochadi.

## 4. Eksport/import

- **Sozlamalar ekranida:** "Faylga saqlash" (`ActivityResultContracts.CreateDocument("application/json")`, nom `hangul-friend-YYYY-MM-DD.json`) va "Fayldan tiklash" (`OpenDocument`).
- **Format:**

```json
{
  "format": "hangul-friend-backup",
  "version": 1,
  "exported_at_ms": 0,
  "db_version": 3,
  "settings": { "onboarded": true, "current_lesson_id": "u02_l1", "daily_new_limit": 20, "daily_goal_xp": 100, "reminder_enabled": false, "reminder_minutes": 1200 },
  "cards": [], "review_logs": [], "lesson_progress": [], "xp_events": [],
  "achievements": [], "best_scores": [], "story_progress": []
}
```

- Har bir Room jadvali uchun alohida `@Serializable` DTO (Entity'lar serializatsiyaga bog'lanmaydi). Mapping funksiyalari JVM testida "entity → DTO → entity" aylanishi bilan tekshiriladi.
- **Import:**
  1. Fayl o'qiladi va parse qilinadi. `format` noto'g'ri, `version` qo'llab-quvvatlanmaydi yoki JSON buzilgan bo'lsa, "Fayl mos emas" xabari chiqadi va hech narsa o'zgarmaydi.
  2. Tasdiq dialogi: "Joriy progress o'chiriladi va fayldagi bilan almashtiriladi".
  3. Bitta Room tranzaksiyasida hamma jadval tozalanadi va fayldagi ma'lumot yoziladi; keyin sozlamalar yoziladi va eslatma qayta rejalashtiriladi.
- `BackupService` (`data` paketi) — `export(): String` va `import(json): Result` funksiyalari. UI faqat fayl o'qish/yozishni bajaradi.

## 5. Natija kartasi

- **Kirish:** bosh ekranda "Ulashish" ikonka-tugmasi.
- **Ma'lumot:** `ProgressStats` (sof funksiya, JVM test) `ShareSnapshot` qaytaradi:
  - daraja va jami XP;
  - streak;
  - o'rganilgan so'zlar (kamida bir marta takrorlangan `#R` kartalar soni);
  - tugatilgan (`COMPLETED` va `VERIFIED`) darslar soni / 18;
  - har bir darsning holati;
  - joriy dars nomi.
- **Chizish:** `ShareCardRenderer` `android.graphics.Canvas` bilan 1080×1350 `Bitmap` chizadi:
  - sarlavha "Hangul Friend 한글";
  - katta raqamlar: streak 🔥, daraja/XP, so'zlar;
  - 9×2 dars xaritasi (holat ranglari ilova temasi bilan bir xil);
  - joriy dars;
  - pastda sana.
- **Ulashish:** PNG `cacheDir/share/progress.png` ga yoziladi va `FileProvider` (`uz.hangulfriend.fileprovider`) orqali `ACTION_SEND` (`image/png`) chooser bilan yuboriladi. Ulashishdan oldin rasm oldindan ko'rish oynasida ko'rsatiladi.

## 6. Xatolar va chekka holatlar

| Holat | Xatti-harakat |
|---|---|
| Bildirishnoma ruxsati yo'q | Kalit o'chiq, "Ruxsat berilmagan" izohi va tizim sozlamalariga o'tish tugmasi |
| Worker ishlagan paytda DB bo'sh (onboarding qilinmagan) | Bildirishnoma chiqmaydi |
| Import fayli buzilgan yoki boshqa format | "Fayl mos emas", ma'lumot o'zgarmaydi |
| Import fayli yangiroq versiyada | "Bu fayl ilovaning yangiroq versiyasida yaratilgan", ma'lumot o'zgarmaydi |
| Import tranzaksiyasi xato bilan tugadi | Tranzaksiya qaytariladi, eski ma'lumot saqlanadi, xato xabari |
| Ulashish uchun ilova topilmadi | "Ulashish uchun ilova topilmadi" toast |
| Epizod audiosi yo'q | Matn ko'rsatiladi, audio tugmasi o'chiq (validator buni oldini oladi) |

## 7. Test strategiyasi

- **JVM:**
  - `StoryStep` parse va validator qoidalari;
  - ochilish qoidasi (holat → qulflangan/ochiq);
  - birinchi marta tugatilganda XP faqat bir marta beriladi;
  - `ReminderPolicy` (maqsad bajarilgan, karta bor/yo'q, streak 0);
  - keyingi eslatma vaqtigacha kechikishni hisoblash (bugun o'tib ketgan / hali kelmagan);
  - backup DTO aylanishi va format/versiya rad etilishi;
  - `ProgressStats`.
- **Instrumented:** `MIGRATION_2_3` testi; `BackupService` import tranzaksiyasi (haqiqiy Room bilan).
- **Kontent:** `ContentAssetsTest` hikoya qoidalari bilan kengaytiriladi.
- **Qo'lda (telefonda):** epizodni o'tish, bildirishnoma (vaqtni 1–2 daqiqa keyinga qo'yib), eksport → ilova ma'lumotini tozalash → import, natija kartasini Telegramga yuborish.

## 8. Amalga oshirish tartibi

1. **Kod PR:** hikoya modeli, validator, audio generatori, DB migratsiyasi, hikoya ekranlari; eslatmalar; eksport/import; natija kartasi. Sinov uchun 2-bo'lim (2 ta epizod) kontenti bilan.
2. **Kontent PR'lari:** qolgan 16 epizod, 3 ta PR (1, 3, 4-bo'limlar; 5–7-bo'limlar; 8–9-bo'limlar). Oxirgi PR'da "har bir darsda `story` bor" qoidasi yoqiladi.
