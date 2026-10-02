# 7b-bosqich — "Xato bor" belgisi va uning eksporti

Sana: 2026-10-02
Holat: spec ko'rib chiqilmoqda
Asosiy spec: `2026-09-29-hangul-friend-design.md` §3.4 ("Har bir mashqda 'Xato bor' belgisi bor. U element ID'sini mahalliy ro'yxatga
yozadi"), §3.2 (Sozlamalar: "'Xato bor' belgilari eksporti"), §6 ("belgilar eksport qilinadi va ular asosida kontent tuzatiladi").
Ustiga quriladi: 7a (DB v4) — bu bosqich DB v5.

## 1. Maqsad

Foydalanuvchi darsda yoki takrorlashda kontentda xato ko'rsa (noto'g'ri tarjima, audio, javob, imlo), bir tegish bilan belgilaydi;
keyin Sozlamalardan hamma belgilarni **o'qiladigan matn** sifatida Telegram/email orqali yuboradi (foydalanuvchi tanlovi).

### Qamrov tashqarisida

Hikoya epizodi, dialog va grammatika sahifalaridagi belgilar (faqat mashqlar); serverga avtomatik yuborish; JSON eksport;
belgini tahrirlash.

## 2. Belgi qo'yish

- `ExerciseView` (dars bosqichlaridagi tekshiruv va barcha sessiyalar shu orqali chiziladi) yuqori o'ng burchagida kichik bayroq tugmasi
  (`Icons.Outlined.Flag`, 44 dp tegish maydoni, `contentDescription` "Xato bor").
- Bosilganda dialog: sabab (bitta tanlanadi) — **Tarjima xato**, **Audio xato**, **To'g'ri javob qabul qilinmadi**, **Imlo / matn xato**,
  **Boshqa**; ixtiyoriy izoh (≤ 300 belgi); "Yuborish ro'yxatiga qo'shish". Saqlangach snackbar/Toast "Belgilandi, rahmat!".
- Bir xil mashq + sabab qayta belgilansa — yangi yozuv emas, izoh yangilanadi (takrorlarni oldini olish).
- Belgi qo'yish mashq natijasiga, XP va FSRS'ga ta'sir qilmaydi.
- Dialogni ochish uchun `LocalFlagReporter` (CompositionLocal) — `MainActivity` beradi; testlarda va Preview'da yo'q bo'lsa tugma ko'rinmaydi.

## 3. Ma'lumot

`content_flags` jadvali (DB **5**, `MIGRATION_4_5`):

| Ustun | Tur | Izoh |
|---|---|---|
| `id` | INTEGER PK autoincrement | |
| `ref` | TEXT | mashq/so'z ID'si: `Authored` → `exercise.id`; so'z mashqlari → `word.id`; `Match` → so'zlar ID'lari vergul bilan; `Speak` → koreyscha matn |
| `lessonId` | TEXT | ID prefiksidan (`u02_l1_…` → `u02_l1`), o'z so'zi → `user`, topilmasa `""` |
| `type` | TEXT | `ExerciseItem.typeKey` |
| `snapshot` | TEXT | foydalanuvchi ko'rgan matn: so'z → `ko — uz`; `Authored` → `prompt_uz` + `sentence`/`base·form`/`source_uz` + to'g'ri javoblar |
| `reason` | TEXT | `TRANSLATION`, `AUDIO`, `ANSWER_REJECTED`, `TYPO`, `OTHER` |
| `comment` | TEXT NULL | |
| `createdMs` | INTEGER | |

Noyob indeks `(ref, reason)` — `upsert`.

`FlagRepository`: `flag(item: ExerciseItem, reason, comment)`, `observeCount(): Flow<Int>`, `all()`, `clear()`, `exportText(appVersion: String, now): String`.
Sof funksiyalar: `flagRef(item)`, `flagSnapshot(item)`, `lessonOfRef(ref)`.

Zaxira nusxa: `BackupFile.contentFlags` (`content_flags`, default bo'sh) — telefon almashsa belgilar yo'qolmasin.

## 4. Eksport (Sozlamalar)

- Sozlamalarda "Xato belgilari" qatori: `N ta belgi`; tugmalar **"Yuborish"** (N > 0 bo'lsa yoqiladi) va **"Tozalash"** (tasdiq bilan).
- "Yuborish" → `Intent.ACTION_SEND`, `text/plain`, `EXTRA_SUBJECT` "Hangul Hunt — kontent xatolari (N)", `EXTRA_TEXT` = `exportText`.
- Matn formati (darslar bo'yicha guruhlangan, kitob tartibida; `user` oxirida, `""` — "Boshqa"):

```
Hangul Hunt — kontent xatolari
Sana: 2026-10-02 · Ilova: 1.0 · Jami: 3

[u02_l1] 2-bo'lim, 1-dars
1) u02_l1_e07 · fill_blank · Tarjima xato
   Savol: … 
   Izoh: …
2) …
```

- Yuborilgandan keyin belgilar avtomatik o'chmaydi (foydalanuvchi "Tozalash" bilan o'chiradi).

## 5. Testlar

- `MigrationTest.migrate4to5_addsContentFlags`.
- `FlagModelTest`: `flagRef`/`flagSnapshot`/`lessonOfRef` har `ExerciseItem` turi uchun; `lessonOfRef("user_w3") == "user"`.
- `FlagRepositoryTest`: qo'shish; bir xil `(ref, reason)` yangilanadi, son oshmaydi; boshqa sabab — yangi yozuv; `clear`; `exportText` guruhlash,
  tartib, sabab nomlari, izohsiz yozuv.
- `BackupCodecTest`/`BackupServiceTest`: `content_flags` round-trip, eski faylda bo'sh.

## 6. Qabul mezonlari

1. Har bir mashqda bayroq tugmasi bor; belgi saqlanadi va Sozlamalarda soni ko'rinadi.
2. "Yuborish" Telegram/email'ga o'qiladigan matnni beradi; "Tozalash" ro'yxatni bo'shatadi.
3. DB 4 → 5 migratsiyasi ma'lumotni saqlaydi; zaxira nusxa belgilarni ham tiklaydi. Barcha testlar o'tadi.
