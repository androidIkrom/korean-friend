# 7f-bosqich — 7e da qoldirilgan uchta ish

Sana: 2026-10-02
Holat: spec (foydalanuvchi: "plan bo'yicha davom eting")

## 1. Ekran burilganda mashq holati saqlanadi

Hozir mashq ko'rinishlaridagi holat `remember` da — burilganda tanlangan javob, yozilgan matn, javob paneli, variantlar tartibi
yo'qoladi, sessiya esa javobni allaqachon qabul qilgan bo'ladi (tugma qayta bosilsa e'tiborsiz). Endi `rememberSaveable`:

- `ChoiceView`: aralashtirilgan variantlar tartibi, tanlangan variant, boshlanish vaqti.
- `BuildSentenceView`: aralashtirilgan tokenlar tartibi, tanlangan tokenlar (`SnapshotStateList<Int>` uchun saver), maslahat, javob paneli.
- `TypingView`: kiritilgan matn, maslahat, javob paneli.
- `FlashcardView`: ochilgan/yopiq.
- `MatchView`: ikki ustun tartibi (so'z ID'lari), topilganlar, xatolar, tanlangan.
- `SpeakView`: yuborilgan bayroq (yozib olish holati boshidan boshlanadi — mikrofon jarayonini saqlab bo'lmaydi).
- `FeedbackInfo` uchun `FeedbackInfoSaver` (`listSaver`).

## 2. Epizodda burilganda klip qayta ijro etilmaydi

`EpisodeViewModel` har klipni `SpeakEvent(id, file)` sifatida chiqaradi; ekran `PlayOnce` orqali faqat yangi `id` ni ijro etadi
(`PlayOnce` VM ichida — burilishda saqlanadi).

## 3. Eshitish dialoglari ikki ovozda

- `Exercise.audioDialogue: List<String>?` (`audio_dialogue`) — dialog qatorlari; navbat bilan ayol va erkak ovozi.
- `tools/audio_gen.py`: `audio_dialogue` bo'lsa, har qator alohida sintez qilinib, MP3 baytlari ketma-ket ulanadi (edge-tts bir xil
  format: 24 kHz mono MP3 — ulash ijroni buzmaydi); fayl nomi barcha (ovoz, matn) juftliklaridan hash.
- Yakuniy testning 15 eshitish savoli qatorlarga bo'linadi (`audio_text` — to'liq matn, izoh uchun qoladi).
- Dars `listen_question` lari o'zgarmaydi.

## Testlar

- `FeedbackInfoSaverTest` (round-trip), `IntListSaverTest`.
- `PlayOnceTest` (bir xil `id` ikkinchi marta ijro etilmaydi, yangi `id` ijro etiladi).
- `test_audio_gen.test_dialogue_alternates_voices_and_joins` (ovozlar navbati, baytlar ulangan, nom barqaror).
- `ContentAssetsTest.finalTestIsValid` — har eshitish savolida `audio_dialogue` (≥ 2 qator) va audio fayl.
