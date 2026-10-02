# 7a-bosqich — Lug'at bazasi ekrani

Sana: 2026-10-02
Holat: spec ko'rib chiqilmoqda
Asosiy spec: `2026-09-29-hangul-friend-design.md` §3.2 ("Lug'at bazasi: barcha so'zlar; qidiruv, dars bo'yicha filtr,
foydalanuvchi qo'shgan so'zlar")

## 1. Maqsad

Kitobdagi barcha so'zlarni bitta joyda ko'rish, qidirish va dars bo'yicha filtrlash; o'z so'zlarini qo'shish.

### Foydalanuvchi tanlovlari

- O'z so'zlari **takrorlashga qo'shiladi** (FSRS kartalari: tanish va yozish).
- Ekranga **Bosh sahifadagi tugma** orqali kiriladi; pastki menyu 4 tabligicha qoladi.

### Qamrov tashqarisida

So'zni tahrirlash (o'chirib, qayta qo'shiladi), o'z so'ziga audio/misol, so'zlarni import qilish, kitob so'zini yashirish.

## 2. Ma'lumot

### 2.1 O'z so'zlari jadvali

`user_words` (Room, DB versiyasi **4**, `MIGRATION_3_4`):

| Ustun | Tur | Izoh |
|---|---|---|
| `id` | INTEGER PK autoincrement | |
| `ko` | TEXT | trim qilingan, bo'sh emas, ≤ 40 belgi |
| `uz` | TEXT | trim qilingan, bo'sh emas, ≤ 80 belgi |
| `note` | TEXT NULL | ixtiyoriy izoh, ≤ 120 belgi |
| `createdMs` | INTEGER | |

`UserWordDao`: `insert(e): Long`, `delete(id)`, `observeAll(): Flow<List<…>>` (yangisi tepada), `all()`, `insertAll`, `deleteAll`.
Bir xil `ko` ikki marta qo'shilmaydi (repository tekshiradi, katta-kichik harf va bo'shliqlar hisobga olinmaydi).

### 2.2 Takrorlashga qo'shish

- Soxta dars: `USER_LESSON_ID = "user"`. `UserWordRepository.asLesson(): Lesson` — o'z so'zlaridan `Word` ro'yxati
  (`id = "user_w<id>"`, `pos = ""`, `exampleKo = note ?: ""`, `exampleUz = ""`, audio yo'q), qolgan maydonlar bo'sh.
- Qo'shilganda `StudyRepository.ensureCards(asLesson-dagi bitta so'z…)` — RECOGNIZE va PRODUCE kartalari,
  `lessonId = "user"`, `origin = LESSON`, `lessonOrder = -1` (yangi kartalar navbatida kitob so'zlaridan oldin).
- O'chirilganda shu so'zning kartalari va ularning `review_logs` yozuvlari ham o'chadi (bitta tranzaksiya).
- `SessionController.lessonsOf` `"user"` uchun `asLesson()` ni qo'shadi; shuning uchun takrorlash, xatolar mashqi ishlaydi.
  Xatolar daftari ro'yxatida o'z so'zlari ham ko'rinadi (`MistakesViewModel` ham shu manbadan oladi).
- Grammatika kartasi yo'q.

### 2.3 Zaxira nusxa

`BackupFile.userWords: List<UserWordDto> = emptyList()` (`@SerialName("user_words")`); import'da jadval tozalanib qayta yoziladi;
`BackupService.DB_VERSION = 4`. Eski fayllar — bo'sh ro'yxat.

## 3. Lug'at ekrani

Marshrut `vocab`; Bosh sahifadagi ikkilamchi tugmalar 2×3 bo'ladi: Takrorlash, O'yinlar, Xatolar, Yutuqlar, **Lug'at**, (bo'sh joy yo'q —
oxirgi qatorda Lug'at to'liq kenglikda).

Tuzilma (tema komponentlari bilan, `GameBackground` ustida):

1. Sarlavha "Lug'at" + jami so'zlar soni (`N ta so'z`).
2. Qidiruv maydoni: koreyscha yoki o'zbekcha bo'yicha, katta-kichik harfga sezgir emas, bo'shliqlar e'tiborsiz; o'zbekcha uchun
   `'`/`ʻ`/`’` bir xil hisoblanadi.
3. Filtr chiplari (gorizontal scroll): **Hammasi**, **Mening so'zlarim**, so'ng har dars: `1-1`, `1-2`, … `9-2`
   (faqat kontenti bor darslar). Bitta chip tanlanadi.
4. Ro'yxat (`LazyColumn`): har qatorda koreyscha (katta), o'zbekcha, kichik dars belgisi (`2-1` yoki `★` o'z so'zi uchun) va holat nuqtasi:
   - **Yangi** (kartasi yo'q yoki hali ko'rilmagan) — kulrang,
   - **O'rganilmoqda** — `accent2`,
   - **O'rganilgan** (RECOGNIZE kartasi `REVIEW` holatida) — `accent`.
   Tartib: kitob tartibi (dars, so'z), o'z so'zlari — eng yangisi tepada; "Hammasi"da o'z so'zlari ro'yxat boshida.
5. Qatorga bosish → pastdan chiqadigan varaq (`ModalBottomSheet`): koreyscha, audio tugma (bo'lsa), o'zbekcha, misol gap va tarjimasi
   (bo'lsa), izoh (o'z so'zida), dars nomi, holat; o'z so'zida **"O'chirish"** (tasdiq so'raydi).
6. "+" tugmasi (FAB) → dialog: Koreyscha*, O'zbekcha*, Izoh; "Qo'shish" faqat ikkala majburiy maydon to'lganda yoqiladi.
   Xatolar maydon ostida: bo'sh, juda uzun, "Bu so'z allaqachon bor" (kitobda yoki o'z so'zlarida). Qo'shilgach snackbar
   "So'z qo'shildi va takrorlashga tushdi".
7. Bo'sh holatlar: qidiruv natijasi yo'q → "Hech narsa topilmadi"; "Mening so'zlarim" bo'sh → "Hali so'z qo'shmagansiz" + "+" ga ishora.

`VocabViewModel`: kitob so'zlari bir marta yuklanadi (fon oqimida, `content.lesson` har dars uchun), o'z so'zlari va kartalar
`Flow` orqali kuzatiladi. Qidiruv va filtr sof funksiya `filterVocab(entries, query, filter)` da.

## 4. Testlar

- `MigrationTest.migrate3to4`: jadval yaratiladi, eski ma'lumot saqlanadi.
- `UserWordRepositoryTest`: qo'shish kartalarni yaratadi (`user_w1#R`, `user_w1#P`, `lessonOrder = -1`); takror `ko` (bo'shliq/harf farqi bilan)
  rad etiladi; kitobdagi so'z rad etiladi; o'chirish kartalar va loglarni o'chiradi; `asLesson()` so'zlarni beradi; validatsiya chegaralari.
- `VocabFilterTest`: koreyscha/o'zbekcha qidiruv, apostrof variantlari, bo'shliqlar, "Mening so'zlarim" va dars filtri, tartib.
- `VocabStatusTest`: kartasiz → Yangi, `NEW` → Yangi, `LEARNING`/`RELEARNING` → O'rganilmoqda, `REVIEW` → O'rganilgan.
- `SessionBuilder`/`SessionController`: o'z so'zi kartasi takrorlash sessiyasida flashcard va yozish mashqiga aylanadi.
- `BackupCodecTest`/`BackupServiceTest`: `user_words` round-trip; eski faylda bo'sh.

## 5. Qabul mezonlari

1. Bosh sahifadagi "Lug'at" tugmasi ekranni ochadi; 18 darsning barcha so'zlari ko'rinadi, qidiruv va filtr ishlaydi.
2. O'z so'zi qo'shiladi, ro'yxatda ★ bilan chiqadi va keyingi takrorlashda paydo bo'ladi; o'chirilsa, takrorlashdan ham chiqadi.
3. Zaxira nusxa o'z so'zlarini saqlaydi va tiklaydi; eski fayllar import qilinadi.
4. DB 3 → 4 migratsiyasi ma'lumotni yo'qotmaydi. Barcha testlar o'tadi.
