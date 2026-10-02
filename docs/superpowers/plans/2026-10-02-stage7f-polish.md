# Stage 7f — Rotation State, Episode Replay, Two-Voice Listening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans.

**Spec:** `docs/superpowers/specs/2026-10-02-stage7f-polish-design.md`. **Test:** `./gradlew :app:assembleDebug :app:testDebugUnitTest`; `python -m unittest test_audio_gen` in `tools/`. No subagents, no worktrees, no `Co-Authored-By`.

### Task 1: Savers and saveable exercise state
- Add `ui/exercise/Savers.kt` with `val FeedbackInfoSaver: Saver<FeedbackInfo?, Any>` and `val IntListSaver: Saver<SnapshotStateList<Int>, Any>`; also a string-list variant, `StringListSaver`.
- [ ] Write failing round-trip tests (`SaversTest`), then implement. Convert the views listed in spec §1 to `rememberSaveable`. Build, run the suite, and commit `fix(exercise): keep exercise state across rotation`.

### Task 2: Episode plays each clip once
- `data class SpeakEvent(val id: Int, val file: String)`; `class PlayOnce { fun shouldPlay(id: Int): Boolean }` (true only for ids not seen before).
- `EpisodeViewModel` emits `SpeakEvent` with an increasing id. The screen calls `LaunchedEffect(event?.id) { if (vm.playOnce.shouldPlay(id)) play }`.
- [ ] Write the failing test `PlayOnceTest`, implement, commit `fix(story): do not replay the clip on rotation`.

### Task 3: Two-voice listening dialogues
- `Exercise.audioDialogue: List<String>? = null` (`@SerialName("audio_dialogue")`). The validator accepts a `listen_question` that has either `audio_text` or `audio_dialogue`.
- `audio_gen.py`: in `collect_final`, an exercise with `audio_dialogue` becomes one joined clip. Line i uses FEMALE when i is even and MALE when it is odd. The name is `sha1("|".join(f"{voice}|{text}"))[:12] + .mp3`.
- Content: in `final_test.py`, each listening entry gets `audio_dialogue` lines. Regenerate audio and prune the replaced files.
- [ ] Failing tests:
  - `test_dialogue_alternates_voices_and_joins`;
  - `ContentAssetsTest.finalTestIsValid` extended with the dialogue rule.

  Implement and regenerate the audio, then commit `content: final test listening as two-voice dialogues`.

### Task 4: Docs
