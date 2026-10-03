# Stage 8 — English language option (design)

## Goal

Hangul Hunt gets a second interface and explanation language. In Settings the learner picks
**O'zbekcha** or **English**. With English selected, every screen, every lesson explanation,
translation, hint, story line, quiz, test question, notification, tutor prompt and AI answer is in
natural, correct English. Korean content and audio do not change.

Uzbek stays the default. Nothing changes for a learner who never touches the setting, even when
the phone itself runs in English.

## Decisions

These were made without asking because each has one clearly better option:

1. **The app chooses its own language; it does not follow the phone.** The stored choice
   (default Uzbek) is applied to every context that reads resources. A learner with an English
   phone keeps the Uzbek app until they switch.
2. **No AppCompat.** The choice lives in a small `SharedPreferences` file, read synchronously.
   `MainActivity.attachBaseContext` wraps the base context with the chosen locale.
   Notifications and the share card use the same wrapper. The language is not part of the
   backup, because it is a device preference.
3. **Switching restarts the activity task.** After the choice is saved, `MainActivity` starts
   itself with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`. This drops every ViewModel
   that holds content text (the Home and Map catalog, open lessons). The learner lands on Home,
   in the new language.
4. **Content keeps one file per lesson.** English is stored as `*_en` sibling fields inside the
   existing JSON (`en` next to `uz`, `why_en` next to `why_uz`, and so on). A single file per
   lesson keeps both languages in sync for any later edit.
5. **Localize at load, not at every call site.** `ContentRepository` returns lessons already
   localized: in English mode it copies each `*_en` value into the matching field the app already
   reads (`uz`, `whyUz`, and the rest). Those 86 call sites stay unchanged. Their KDoc explains
   that these fields hold "the learner's language". The cache key includes the language.
6. **The comparison note is rewritten for English.** `uz_compare` compares Korean grammar with
   Uzbek. For English it gets its own `compare_en` note that compares Korean with *English*. It
   is not a translation of the Uzbek note. The heading on that card follows the language
   ("O'zbekcha bilan solishtirish" / "Compared with English").
7. **Uzbek-language options get parallel English lists.** Listening questions, reading questions
   and story quizzes have answer options written in Uzbek. These get `options_en`, a list of the
   same length in the same order. Answers are mapped by index, so grading still compares the
   option the learner picked with the correct one. Options in Korean have no `_en` list.
8. **Parts of speech are mapped in code.** The seven values (`ot`, `fe'l`, `sifat`, `ravish`,
   `ibora`, `olmosh`, `aniqlovchi`) map to noun, verb, adjective, adverb, phrase, pronoun and
   determiner.
9. **Counts use `<plurals>`**, so English reads "1 day" and "3 days". Uzbek has only `other`.
10. **The language picker is in Settings and in the first onboarding screen.** Its labels are
    never translated: "O'zbekcha" and "English".
11. **The AI tutor answers in the chosen language.** `TutorPrompts` gets an English variant of
    its role, context, mistake and translation-check prompts. Internal `AiResult.Failed`
    messages become short English diagnostics; they were never shown to the learner.
12. **Learner data is not translated.** Own words, flag snapshots and tutor chats keep the
    language they were written in.

## Components

| Unit | Responsibility |
|---|---|
| `i18n/AppLanguage` | `enum AppLanguage(code) { UZ("uz"), EN("en") }`, `fromCode` (unknown means UZ), `locale` |
| `i18n/LanguageStore` | Synchronous `SharedPreferences` read and write (`language` key) |
| `i18n/Localized` | `wrap(context, lang)`: a configuration context with that locale; used by the activity, the worker, the scheduler and the share card |
| `content/Localize.kt` | Pure `Lesson.localized(lang)`, `CatalogEntry.localized`, `Character.localized`, `FinalTest.localized`, and `posLabel`. A missing `_en` field falls back to Uzbek. |
| `content/Model.kt` | Nullable `*_en` fields (`= null`), so the strict JSON still accepts every file |
| `content/LessonValidator` | `englishErrors(lesson)` lists missing or blank English fields, and wrong `options_en` sizes |
| `ContentRepository` | Takes `language: () -> AppLanguage`; caches by `(id, lang)` and returns localized objects |
| `ai/Tutor` | Prompts built for an `AppLanguage` |
| `reminders/ReminderPolicy` | Returns a message *kind* plus numbers instead of Uzbek text; the worker formats it from resources |
| `data/FlagRepository` | Report labels come from the language |
| `ui/settings/LanguageRow` | Two chips; a tap saves the choice and restarts |
| `res/values-en/strings.xml` | Every string, in English |

## Content fields (English siblings)

- **Lesson** / **CatalogEntry** (book.json): `title_en`, `topic_en`
- **Word**: `en`, `example_en`
- **Grammar**: `meaning_en`, `explanation_md_en`, `compare_en`
- **Formation**: `condition_en`
- **Example**: `en`
- **Mistake**: `why_en`
- **Dialogue line** / **story line**: `en`
- **Story**: `title_en`
- **ChooseReply**: `prompt_en`, `en`, `why_en`
- **StoryQuiz**: `prompt_en`, `options_en`, `why_en` (the answer maps by index)
- **Exercise** (lessons and final test): `prompt_en`, `source_en`, `hint_en`, `why_en`, `options_en`
  - `options_en` exists only when the options are Uzbek, meaning they contain no Hangul.
  - A non-Korean answer maps to `options_en` by its index.
- **Character**: `name_en`

## Translation work

A one-off script lists every unique Uzbek string of a file in reading order. I translate it into
a `uz → en` dictionary, and the script merges the dictionary back as `_en` siblings. A string with
no translation fails the merge.

The script is a scratch tool and is not committed. After the merge, the validator test is what
keeps the content complete.

Translation style:

- Plain, natural learner English.
- Korean stays Korean.
- Uzbek-specific explanations get English equivalents. For example, "aka (o'g'il bola uchun)"
  becomes "older brother (said by a male)".
- Honorific nuance is kept: "(honorific)", "polite".

## Error handling

- Missing English in a shipped file is a test failure. At runtime it falls back to Uzbek, so the
  app never shows a blank.
- An unknown stored language code means Uzbek.

## Testing

- `AppLanguageTest`, `LanguageStore` round trip (Robolectric).
- `Localized.wrap(ctx, EN).getString(R.string.lang_code) == "en"`; with UZ it is `"uz"`, even
  when the default locale is English.
- `LocalizeTest`:
  - Every field swaps, and a missing value falls back to Uzbek.
  - Options swap by index; answers follow.
  - The part of speech maps.
- `ContentAssetsTest`: `englishErrors` is empty for every lesson, the final test, book.json and
  characters.json.
- `StringsParityTest`: `values-en` has every string and plural of `values`, with the same format
  arguments.
- Tutor prompt tests for both languages; ReminderPolicy kinds.
- Emulator check in both languages: Home, Map, Lesson (all stages), Story, Session, Vocab,
  Settings, Final test, Tutor.
