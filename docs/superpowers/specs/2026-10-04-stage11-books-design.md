# Stage 11: Books 1 and 3, and a Hangul course — design

**Date:** 2026-10-04 · **Status:** approved in chat, awaiting spec review

## Why

The app covers only 사랑해요 한국어 2 (18 lessons). The user wants beginners to be able to start from zero, so
it needs book 1 (Hangul plus basic Korean). It also needs to continue past book 2, with book 3.

Sources: [SNU Press, book 1](https://eng.snupress.com/book/category?md=view&goodsidx=3066),
[Yes24, book 3](https://m.yes24.com/Goods/Detail/74362207).

- **Book 1 units:**
  1. Hangeul (한글)
  2. Basic Korean (기본 한국어)
  3. Introductions (소개)
  4. Items and objects (물건)
  5. Food and ordering (음식과 주문)
  6. Daily life (일상생활)
  7. Shopping (쇼핑)
  8. Time and date (시간과 날짜)
  9. Weather and life (날씨와 생활)
- **Book 3 units:**
  1. Plans (계획)
  2. Inquiry (문의)
  3. Experience (경험)
  4. Employment (취업)
  5. Health (건강)
  6. Broken (고장)
  7. Meetings (모임)
  8. Change (변화)
  9. Holidays (명절)

## Decisions (from the user)

- **Scope:** book 1 first, then book 3, built unit by unit with one PR per unit, as book 2 was.
- **Hangul:** a dedicated Hangul module, not ordinary lessons.
- **Existing learners:** when book 1 arrives, a learner already in book 2 sees book 1 lessons **open but not started**. Nothing is auto-passed.

## 1. Data model

- **Book of a lesson:** `CatalogEntry` gains `book: Int` (default 2, so the current `book.json` still parses).
  - The catalog becomes three files: `book1.json`, `book.json` (book 2, unchanged path) and `book3.json`.
  - `catalog()` returns them concatenated in book order.
- **Lesson ids:**
  - Book 2 keeps `u02_l1`, so saved progress, FSRS cards, flags and backups stay valid.
  - New books use `b1_u03_l1` and `b3_u01_l1`.
  - One parser, `LessonId.parse(id): LessonId(book, unit, lesson)?`, with `LessonId.format(book, unit, lesson)`, replaces every place that slices ids today: `SessionController.unitLessonId`, `FlagRepository` (`substring(1, 3)`), and the boss route that carries a bare unit number. The boss route now carries `"<book>:<unit>"`.
- **Final tests:** one per book.
  - Files: `final_test.json` (book 2), `final_test_b1.json` and `final_test_b3.json`.
  - The FINAL session and the map's final gate take a book.
  - Best scores are kept per book, under the ids `final` (book 2, as today), `final_b1` and `final_b3`.
- **Hangul module:** `assets/hangul.json`.
  - Letters: `id`, `jamo`, `kind` (`vowel`, `consonant` or `final`), romanization, `name_uz` / `name_en`, a short tip (`tip_uz` / `tip_en`, which replaces stroke-order animation), audio, and an example word with its audio.
  - Five Hangul lessons group the letters:
    1. basic vowels (ㅏ ㅓ ㅗ ㅜ ㅡ ㅣ ㅐ ㅔ …);
    2. basic consonants;
    3. aspirated and tense consonants;
    4. compound vowels;
    5. final consonants (받침).
  - The module is book 1, unit 1 on the map (ids `b1_u01_l1` … `b1_u01_l5`) and opens the Hangul screen instead of the lesson pager.
- **FSRS:** letters become cards with item ids `h_<jamo>` (RECOGNIZE kind) in pseudo-lesson `hangul`. Review sessions can show them, and `LessonLookup` resolves `hangul` like `user`.

## 2. Screens

- **Map:**
  - A book selector at the top ("1-kitob · 2-kitob · 3-kitob", glass pills). It opens on the book of the current lesson.
  - Each book shows its units with the existing skins, then its final gate.
  - A book without content yet shows "Tez kunda" (coming soon).
- **Hangul screen:**
  - Per lesson, a grid of letter cards. Tapping a card plays its sound and opens a sheet with the name, romanization, tip and example word.
  - "Mashq" (practice) starts a session of the new Hangul exercises below.
- **New exercise items:**
  - `LetterListen`: hear a letter or syllable, pick it among 4.
  - `LetterSound`: see a letter, pick its romanization among 4.
  - `BuildSyllable`: tap an initial, a vowel and an optional final to build the target syllable. Shown as text and audio, e.g. ㄱ + ㅏ = 가.
  - `ReadWord`: read a short word made of learned letters, pick its romanization.
- **Onboarding:** the lesson picker groups lessons by book. "I am a complete beginner" picks Hangul lesson 1.
- **Home, stories, tutor, vocabulary, Arena:** all work across books.
  - Vocabulary filter chips gain the book: "1·3-1".
  - Arena word pools include every open lesson of every book.

## 3. Content production

- **Grammar maps:** `docs/content/grammar-map-b1.md` and `grammar-map-b3.md`, researched online the same way as book 2's `grammar-map.md`. The online 1급 and 3급 courses come first, then publisher tables of contents.
- **Per unit (books 1 and 3, except Hangul):** two lesson JSONs in the book 2 format:
  - 32 words;
  - 2 grammar points, or 3 where the map says so;
  - an 11-line dialogue;
  - 24 practice items, 3 of them `listen_question`;
  - 15 test items;
  - a story with `gate_uz` / `gate_en`;
  - every text in Uzbek and English.

  Then `python tools/audio_gen.py`, then `python tools/validate_content.py`, then one PR per unit. Everything stays `reviewed:false` until the user checks it.
- **Hangul audio:** letter and syllable sounds and example words, from edge-tts like all other audio.
- **Characters:** new speakers are added to `characters.json` with a voice, as book 2 did (clerk, doctor…).

## 4. Stages (one PR each, more PRs for content)

- **11a — Multi-book foundation:**
  - `book` field and three catalogs;
  - `LessonId` everywhere;
  - per-book final tests and scores;
  - map book selector with "coming soon";
  - onboarding grouped by book.
  - No new lessons yet, so book 1 and book 3 show "coming soon".
- **11b — Hangul module:**
  - `hangul.json`, the letter audio, the Hangul screen and the four exercise items;
  - letter cards in FSRS;
  - book 1 unit 1 on the map.
- **11c — Book 1 content:**
  - the grammar map;
  - units 2–9 (8 PRs);
  - the book 1 final test.
- **11d — Book 3 content:**
  - the grammar map;
  - units 1–9 (9 PRs);
  - the book 3 final test.

## Error handling and edge cases

- **Unknown or malformed lesson id** (old backups, flags): `LessonId.parse` returns null, and callers skip or fall back as they do today for a missing lesson.
- **A book with no lesson files yet:** it shows "coming soon" and its final gate stays locked.
- **Backups:**
  - They export and import lessons of every book.
  - Old backups (book 2 only) import unchanged.
  - Progress for `b1_*` and `b3_*` lessons restores like any other lesson id.
- **Hangul cards in reviews:** a `hangul` card whose letter is missing from `hangul.json` is skipped, like a missing word.

## Testing

- **`LessonId`:** round trips and rejects.
  - `u02_l1` → (2, 2, 1) and back to `u02_l1`.
  - `b1_u03_l2` → (1, 3, 2).
  - Garbage → null.
- **Catalog:** concatenates the books in order; old `book.json` entries default to book 2.
- **Boss and final:** per book (session controller tests), with scores stored under per-book ids.
- **Hangul:**
  - validator: every letter has audio, every lesson's letters exist, `BuildSyllable` targets are composable from their parts (Unicode Hangul composition);
  - the new exercise builders.
- **Content:** the existing validators (`ContentAssetsTest`, `validate_content.py`) cover the new lessons unchanged.
- **Per PR:** an emulator check, taking turns with DevSuhbat through `emulator.lock`. A phone check happens at the user's request.

## Out of scope

- Books 4–6.
- Stroke-order animation.
- Handwriting recognition.
