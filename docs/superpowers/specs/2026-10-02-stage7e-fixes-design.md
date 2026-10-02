# 7e-bosqich — keyinga qoldirilgan kichik kamchiliklarni tuzatish

Sana: 2026-10-02
Holat: spec (foydalanuvchining "oxirigacha tartib bilan" ko'rsatmasi bo'yicha)

## Tuzatiladi

| # | Muammo (qayerdan) | Tuzatish | Tekshiruv |
|---|---|---|---|
| 1 | Dars bosqichida "Orqaga" bosib tekshiruv sahifasiga qaytilsa, javob qayta baholanadi — FSRS ikki marta yangilanadi (4-bosqich) | `GradeOnce` — bir bosqich ichida har mashq faqat bir marta baholanadi | `GradeOnceTest` |
| 2 | Bo'sh sahifali bosqich (masalan, so'zsiz dars) `Pager` ni yiqitadi (4-bosqich) | `pageCount == 0` bo'lsa sahifa chizilmaydi, bosqich darhol tugaydi | build (Compose UI testi yo'q) |
| 3 | Kunlik yangi so'z slayderi bitta pastroq qiymatga tushishi mumkin (`toInt`) (4-bosqich) | `roundToInt` | build |
| 4 | O'lik kod: `StudyRepository.observeDueCount`, `Nav.Placeholder`, `placeholder_screen` (4-bosqich) | o'chiriladi | build |
| 5 | Validator `choose_reply` gapiruvchisi `aziz` ekanini tekshirmaydi (5-bosqich) | qoida qo'shiladi | `StoryValidationTest.chooseReplyMustBeAziz` |
| 6 | Bildirishnoma ruxsati keyin bekor qilinsa, eslatma tugmasi yoqiq qoladi (5-bosqich) | Sozlamalar ochilganda ruxsat yo'q bo'lsa eslatma o'chiriladi; qaror sof funksiyada | `ReminderPolicyTest.disableWhenPermissionRevoked` |
| 7 | Ilova ochilganda oq ekran chaqnaydi (oyna temasi yorug') (6-bosqich) | `res/values/themes.xml` — qorong'i `Theme.HangulHunt` (`windowBackground` `#04060C`, status/navigatsiya paneli qorong'i), manifestda ishlatiladi | build |
| 8 | Avatar har animatsiya kadrida yangi `Paint` yaratadi (6b) | blur `Paint` lari radius/rang bo'yicha keshlanadi | build |
| 9 | Lug'atda kitob ro'yxati yuklanmasdan so'z qo'shilsa, kitobdagi takror so'z o'tib ketadi (7a) | `add` kitob ro'yxati yuklanishini kutadi (`CompletableDeferred`) | build |

## Tuzatilmaydi (sababi bilan)

- Ekran burilganda mashq holati yo'qolishi — har mashq ko'rinishining holatini `rememberSaveable`ga o'tkazish katta ish; alohida bosqich.
- Epizodda burilganda klip qayta ijro etilishi, so'z kartasi yopilishi — kichik noqulaylik, xavfsiz.
- Eshitish dialoglari bitta ovozda — `audio_gen` ga ko'p ovozli klip kerak; alohida ish.
- u02 kontent nitlari (`봐 보다`, `밝은 색 옷`) — hozirgi fayllarda topilmadi (avval tuzatilgan); `한번 입어 보세요` kitob nomi, to'g'ri.
- "Rank oshdi" oynasidagi personaj/asinxron saqlash — juda kam holat.
