# 7d-bosqich — qolgan ekranlarni o'yin uslubiga o'tkazish

Sana: 2026-10-02
Holat: spec (foydalanuvchining "oxirigacha tartib bilan" ko'rsatmasi bo'yicha)
Oldingi: 6-bosqich (temalar, `GamePanel`, `GameBackground`), 7a–7c.

## 1. Maqsad

6-bosqichda faqat Uy, Xarita, Epizod (keyin Lug'at) qayta chizilgan; qolgan ekranlar (dars bosqichlari, rol o'ynash, sessiya va mashqlar,
tutor, o'yinlar, yutuqlar, xatolar daftari, hikoyalar ro'yxati, onboarding, sozlamalar) faqat rang/shrift olgan — Material kartalari
yumaloq va "oq-ko'k" ko'rinadi. Bu bosqich ularni bir xil o'yin uslubiga keltiradi. **Xatti-harakat o'zgarmaydi.**

## 2. O'zgarishlar

1. **Tema shakllari:** `gameShapes(tokens)` — Tizim: barcha `MaterialTheme.shapes` 2 dp (deyarli to'g'ri burchak), Neon: 8/12/14/18/24 dp.
   Tugmalar, matn maydonlari, dialoglar, chiplar shu shakllarni oladi.
2. **Umumiy fon:** `Nav` dagi `Scaffold` konteyneri `GameBackground` ustida (shaffof); ichma-ich `GameBackground` qayta chizmaydi
   (`LocalInGameBackground`), shuning uchun Uy/Xarita/Lug'at/Epizod o'zgarmaydi.
3. **`GameCard`:** `Card` o'rniga — panel foni, 1 dp tema chegarasi, tema shakli, ixtiyoriy `onClick` va `containerColor`/`borderColor`.
   Almashtiriladi: dars so'z kartasi va grammatika kartasi, rol o'ynash, mashq javob paneli (to'g'ri/xato rangli chegara), o'yinlar ro'yxati,
   xotira o'yini kartalari, yutuqlar, xatolar daftari, hikoya ro'yxati.
4. **Sessiya:** progress — `ProgressBar` (tema nuri), yuraklar emoji o'rniga ikonlar (`Favorite`/`FavoriteBorder`, `danger` rangida).
5. **Hikoyalar ro'yxati:** emoji holat belgilari o'rniga ikonlar (`Lock`, `HourglassEmpty`, `MenuBook`, `CheckCircle`) tema ranglarida.

## 3. Qamrov tashqarisida

Ekranlarning tuzilmasini o'zgartirish, yangi animatsiyalar, oq/yorug' tema.

## 4. Testlar

`GameThemeTest.shapesFollowTheme` (Tizim 2 dp, Neon 14 dp medium). Qolgani — mavjud testlar to'liq o'tishi va `assembleDebug`;
qurilmada ko'z bilan tekshirish.
