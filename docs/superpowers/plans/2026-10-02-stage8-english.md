# Stage 8 — English Language Option Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The learner can switch the whole app (UI, lesson content, notifications, AI tutor) between Uzbek and English in Settings.

**Architecture:** The language is stored in its own SharedPreferences and applied by wrapping the
activity's base context with that locale. Switching restarts the task. English UI strings live in
`values-en`. English content is stored as `*_en` sibling fields in the asset JSON, and
`ContentRepository` copies them into the existing `uz`-named fields at load time, so screens stay
unchanged.

**Tech Stack:** Kotlin, Compose, kotlinx.serialization, Robolectric (sdk 35), Python (scratch translation merge).

**Spec:** `docs/superpowers/specs/2026-10-02-stage8-english-design.md`

## Global Constraints

- Uzbek is the default. The phone's locale is never consulted.
- Language codes are `"uz"` and `"en"`. An unknown code means UZ.
- The language picker labels are always "O'zbekcha" and "English", in both languages.
- Korean text and audio are untouched. No audio regeneration.
- The strict content JSON stays strict: new fields are nullable with a default of `null`.
- Every `values` string and plural exists in `values-en`, with the same format arguments.
- Commits have no Co-Authored-By line. API keys never go in logs or commits. No subagents, no worktrees.
- `strings.xml` apostrophes are written as `\'`. Edit through the Edit tool, not a heredoc.
- Verify with `./gradlew :app:assembleDebug :app:testDebugUnitTest`.

## Review Focus

1. **Switching while a lesson or session is open.** Nothing may still show the old language after
   the restart. Covered because the restart clears the task. Check it on the emulator in Task 7.
2. **The phone in English with the app on Uzbek.** The UI must still be Uzbek.
   `LocalizedTest.uzbekEvenWhenDefaultLocaleIsEnglish` covers this.
3. **A content item whose options are Uzbek** (listen, read, quiz). Grading must still mark the
   right option after the swap. `LocalizeTest.optionsSwapByIndexAndAnswersFollow` covers this.
4. **A plural count of 1 in English** ("1 day", "1 word"). `StringsParityTest` checks that each
   plural has `one` and `other`.
5. **Notifications posted by the worker without an activity.** They must use the chosen language.
   `ReminderTextTest` covers this, formatting through `Localized.wrap`.

---

### Task 1: Language core

**Files:**
- Create: `app/src/main/java/uz/hangulfriend/i18n/AppLanguage.kt` (enum + `LanguageStore` + `Localized`)
- Modify:
  - `MainActivity.kt`: `attachBaseContext` wraps the context; add `restartForLanguage()`
  - `res/values/strings.xml`: `lang_code` = `uz`
  - `res/values-en/strings.xml`: new file, `lang_code` = `en` for now
- Test: `app/src/test/java/uz/hangulfriend/i18n/LocalizedTest.kt`

**Interfaces:**
- **Produces:**
  - `enum class AppLanguage(val code: String) { UZ("uz"), EN("en"); val locale: Locale; companion fun fromCode(code: String?): AppLanguage }`
  - `class LanguageStore(context: Context) { fun get(): AppLanguage; fun set(lang: AppLanguage) }`
    - Prefs file `"language"`, key `"language"`, written with `commit()` so a restart reads the new value.
  - `object Localized { fun wrap(context: Context, lang: AppLanguage): Context }`
- **MainActivity:**
  - `attachBaseContext(base)` calls `super.attachBaseContext(Localized.wrap(base, LanguageStore(base).get()))`.
  - `fun MainActivity.restartForLanguage()` starts `MainActivity` with `NEW_TASK | CLEAR_TASK`, then calls `finish()`.

- [ ] Write tests:
  - `fromCode("en") == EN`; `fromCode("xx") == UZ`; `fromCode(null) == UZ`.
  - `storeRoundTrip`: a new store returns UZ. After `set(EN)`, a new `LanguageStore(ctx).get()` returns EN.
  - `englishWrap`: `wrap(ctx, EN).getString(R.string.lang_code) == "en"`.
  - `uzbekEvenWhenDefaultLocaleIsEnglish`: with `@Config(qualifiers = "en")`, `wrap(ctx, UZ).getString(R.string.lang_code) == "uz"`.
- [ ] Run them and see them fail. Implement. Run them and see them pass, together with the build.
- [ ] Commit `feat(i18n): language store and locale wrapper`.

### Task 2: Localized content model

**Files:**
- Modify:
  - `content/Model.kt`: nullable `*_en` fields as listed in the spec.
    - Kotlin names: `titleEn`, `topicEn`, `en`, `exampleEn`, `meaningEn`, `explanationMdEn`, `compareEn`, `conditionEn`, `whyEn`, `promptEn`, `optionsEn`, `sourceEn`, `hintEn`, `nameEn`.
    - KDoc on `Lesson`: after localizing, the `uz`-named fields hold the learner's language.
  - `content/LessonValidator.kt`: `englishErrors`.
  - `content/ContentRepository.kt`: language parameter, cache key `"$lang/$id"`.
  - `AppContainer.kt`: pass the language provider.
- Create: `content/Localize.kt`
- Test:
  - `content/LocalizeTest.kt`
  - additions to `LessonValidatorTest.kt` and `ContentRepositoryTest.kt`

**Interfaces:**
- **Produces, in `Localize.kt`:**
  - `fun Lesson.localized(lang: AppLanguage): Lesson`
  - `fun CatalogEntry.localized(lang): CatalogEntry`
  - `fun Character.localized(lang): Character`
  - `fun FinalTest.localized(lang): FinalTest`
  - `fun Exercise.localized(lang): Exercise`
  - `fun posLabelEn(pos: String): String`
  - UZ returns the receiver unchanged.
  - EN copies each non-null `_en` value over its Uzbek field.
  - `options`:
    - Replaced by `optionsEn` when that list is non-null and the same size.
    - Every element of `answers` (or `answer`) that is in `options` maps to the element at the same index.
    - Answers that are not in the options (Korean typed answers) stay unchanged.
  - `pos` maps:
    - `ot` → noun, `fe'l` → verb, `sifat` → adjective, `ravish` → adverb
    - `ibora` → phrase, `olmosh` → pronoun, `aniqlovchi` → determiner
    - Anything else stays unchanged.
  - `uzCompare` gets `compareEn`.
  - `explanationMd` gets `explanationMdEn`.
- **Produces, in `LessonValidator`:**
  - `fun englishErrors(lesson: Lesson): List<String>`. Each message names the item id and the field, for example `"u01_l1_e031: options_en"`.
  - `fun englishErrors(test: FinalTest)`, `fun englishErrorsCatalog(entries)`, `fun englishErrorsCharacters(list)`.
  - A field is required when its Uzbek twin is non-null and non-blank.
  - `options_en` is required when every option has no Hangul. It must then have the same size.
- **Produces, in `ContentRepository`:**
  - Constructor `ContentRepository(source, strict, language: () -> AppLanguage = { AppLanguage.UZ })`.
  - `lesson`, `catalog`, `characters` and `finalTest` return localized values.

- [ ] Write tests:
  - `LocalizeTest.everyFieldSwaps`: builds one small lesson with every `_en` set and asserts each swapped field.
  - `missingEnglishFallsBack`
  - `uzbekIsUnchanged` (`assertSame`)
  - `optionsSwapByIndexAndAnswersFollow`:
    - options `["Buvisi","Onasi"]`, en `["His grandmother","His mother"]`, answers `["Buvisi"]`
    - Afterwards `answers == ["His grandmother"]`.
    - `StoryQuiz` behaves the same way.
  - `koreanOptionsKeep`
  - `posMaps`
  - `LessonValidatorTest.englishErrorsListsMissing`
  - `ContentRepositoryTest.cachesPerLanguage`: the same id loaded under UZ and then EN gives different titles.
- [ ] Run them and see them fail. Implement. Run them and see them pass, with the whole suite.
- [ ] Commit `feat(content): English sibling fields localized at load`.

### Task 3: Code-side text

**Files:**
- Modify:
  - `reminders/ReminderPolicy.kt`, `ReminderWorker.kt`, `ReminderScheduler.kt` (channel name through `Localized.wrap`)
  - `ai/Tutor.kt`, `ai/Gemini.kt` (English diagnostics), `ui/tutor/TutorSheet.kt` and callers (pass the language)
  - `data/FlagRepository.kt`
  - `ui/home/ShareCardButton.kt` uses the activity context; check only.
- Test:
  - `ReminderPolicyTest`, a new `reminders/ReminderTextTest.kt`
  - `TutorTest`, `FlagTest`

**Interfaces:**
- `ReminderPolicy`:
  - `sealed interface Reminder { data class Due(val count: Int, val streak: Int); data class Goal(val streak: Int) }`
  - `fun message(todayXp, goal, dueCount, streak): Reminder?`
  - `fun reminderText(ctx: Context, r: Reminder): String`
    - Uses plurals `reminder_due` and `reminder_streak`, and the string `reminder_goal`.
    - Streak 0 leaves the streak part out.
- `TutorPrompts`:
  - `fun role(lang: AppLanguage): String`
  - `grammarContext(g, lesson, lang)`, `mistakeQuestion(e, answer, lang)`, `translationCheck(e, answer, lang)`
  - The English role says to answer only in English and to give the English translation after each Korean example.
  - `TutorService.checkTranslation(e, answer, lang)`.
  - The JSON key stays `explanation_uz`.
- `FlagRepository`:
  - The report and snapshot labels come from `language: () -> AppLanguage`, a constructor parameter that defaults to UZ.
  - English labels: `"Answer: "`, `"[user] My words"`, `"Unit $unit, lesson $lesson"`, `"Hangul Hunt — content issues"`, `"Date: … · App: … · Total: …"`.

- [ ] Write tests:
  - `ReminderPolicyTest` returns kinds.
  - `ReminderTextTest`:
    - `wrap(ctx, EN)` with `Due(1, 0)` gives `"1 card is waiting today."`.
    - `Due(3, 2)` contains `"2-day streak"`.
    - The Uzbek text is unchanged from today's wording.
  - `TutorTest`:
    - The English role contains `"English"`.
    - The English mistake question contains `"Correct answer:"`.
    - The Uzbek prompt is unchanged.
  - `FlagTest`: the English report header.
- [ ] Run them and see them fail. Implement. Run them and see them pass.
- [ ] Commit `feat(i18n): reminders, tutor prompts and flag report follow the language`.

### Task 4: English strings

**Files:**
- Modify:
  - `res/values/strings.xml`: counts that need plurals become `<plurals>`, with Uzbek `other` only. Their callers switch to `pluralStringResource`.
    - The counts are `home_streak_days`, `vocab_count`, `settings_daily_new`, `flags_title`, plus the reminder plurals.
- Create: `res/values-en/strings.xml`, every string in English.
- Test: `app/src/test/java/uz/hangulfriend/i18n/StringsParityTest.kt`
  - Parses both XML files from `src/main/res`.

**Interfaces:**
- `StringsParityTest`:
  - `sameKeys`: string and plural names are equal.
  - `sameFormatArgs`: the multiset of `%n$x` specifiers per key is equal.
  - `englishPluralsHaveOne`: every English plural has `one` and `other`.
  - `noUzbekLeftInEnglish`: no English value contains `o'`, `g'` or `ʻ`, except the language label `O'zbekcha`.

Copy choices:

- The `uz_compare` card title is "Compared with English".
- The own-word meaning field label is "Meaning".
- Rank and game words stay in game English ("Rank", "Boss", "Combo").

- [ ] Write the test. Run it and see it fail. Write the strings. Run it and see it pass, with the build.
- [ ] Commit `feat(i18n): English UI strings and plurals`.

### Task 5: Language picker

**Files:**
- Create: `ui/settings/LanguageRow.kt`
- Modify:
  - `ui/settings/Settings.kt`: the row goes first in the appearance group.
  - `ui/onboarding/Onboarding.kt`: the row at the top of the first screen.
  - `res/values*/strings.xml`: `settings_language` = "Til" / "Language".

**Interfaces:**
- `@Composable fun LanguageRow(current: AppLanguage, onPick: (AppLanguage) -> Unit)`
  - Two selectable chips labelled "O'zbekcha" and "English".
- The caller:
  - `current` comes from `AppLanguage.fromCode(stringResource(R.string.lang_code))`.
  - `onPick` saves with `LanguageStore`, then calls `(context as MainActivity).restartForLanguage()` when the choice differs.
  - Find the activity through the `ContextWrapper` chain.

- [ ] Build, then check on the emulator: switching lands on Home in English, and switching back gives Uzbek.
- [ ] Commit `feat(settings): language picker`.

### Task 6: English content

**Files:**
- Modify:
  - `assets/book.json`, `assets/characters.json`
  - `assets/lessons/*.json` (18 files)
  - `assets/final_test.json`
- Scratch only (not committed):
  - `scratchpad/i18n/extract.py` lists the unique Uzbek strings of a file in reading order.
  - `scratchpad/i18n/merge.py` reads `<file>.en.json` (`{uz: en}`), writes the `_en` siblings next to each Uzbek field, and keeps key order.
    - It fails on a missing translation.
    - `compare_en` and `explanation_md_en` come from the same dictionary.
    - `options_en` is the per-option lookup, written only when the options have no Hangul.
  - The merge writes JSON with `ensure_ascii=False, indent=2`, a trailing newline and `\n` line endings, like `audio_gen.py`.
- Test: `ContentAssetsTest.everyFileHasEnglish`
  - `englishErrors` is empty for all lessons, the final test, the catalog and the characters.

Translation rules:

- Use natural learner English.
- Honorific nuance becomes "(honorific)" or "polite".
- Uzbek kinship words are explained the English way, for example "older brother (said by a male)".
- `compare_en` compares the Korean grammar with English. It is not a translation of the Uzbek note.
- `explanation_md_en` keeps the markdown structure, the Korean text and the tables. A remark about Uzbek becomes the matching remark about English.

- [ ] Write the test. Run it and see it fail, listing the files.
- [ ] Do book.json and characters.json, then u01…u09 one file at a time, then final_test. Run `ContentAssetsTest` after each unit.
- [ ] Commit per two units: `content(en): units N–M`.

### Task 7: Verify and ship

- [ ] Run `./gradlew :app:assembleDebug :app:testDebugUnitTest`: all green.
- [ ] On the emulator, in English, go through:
  - Home, Map, a lesson (words, grammar with the compare card, dialogue, exercises, test)
  - Story episode with choose_reply and quiz
  - Review session, Vocab with an own word, Games, Achievements, Mistakes
  - Final test, Settings, Tutor (one question only)
- [ ] Switch to Uzbek and spot-check the same screens.
- [ ] Update the main spec's stage list and the memory file. Push the branch and open the PR.
