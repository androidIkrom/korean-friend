"""Generate lesson and Hangul audio with edge-tts (Microsoft neural voices) and record file names in the lesson JSON.

Usage:
    python tools/audio_gen.py [--dry-run] [--prune]

Needs `pip install edge-tts`; no API key. Only texts without an existing audio file are
synthesized. --prune deletes audio files that no lesson references any more.
"""

import argparse
import asyncio
import hashlib
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, NamedTuple

ROOT = Path(__file__).resolve().parent.parent
FEMALE = "ko-KR-SunHiNeural"
MALE = "ko-KR-InJoonNeural"
VOICE_BY_GENDER = {"female": FEMALE, "male": MALE}
EXT = ".mp3"  # edge-tts streams MP3 only


class Clip(NamedTuple):
    text: str
    voice: str
    set: Callable[[str], None]


@dataclass
class Report:
    new: int = 0
    skipped: int = 0
    pruned: int = 0


def file_name(voice: str, text: str) -> str:
    return hashlib.sha1(f"{voice}|{text}".encode("utf-8")).hexdigest()[:12] + EXT


def load_voices(characters: dict) -> dict:
    """Character id -> voice name."""
    return {c["id"]: VOICE_BY_GENDER.get(c.get("voice"), FEMALE) for c in characters.get("characters", [])}


def _clip(obj: dict, text_key: str, audio_key: str, voice: str) -> Clip:
    return Clip(obj[text_key], voice, lambda name: obj.__setitem__(audio_key, name))


def collect(lesson: dict, voices: dict) -> list:
    """Every speakable text of a lesson, in document order, with a setter for its audio field."""
    clips = []
    for w in lesson.get("words", []):
        clips.append(_clip(w, "ko", "audio", FEMALE))
        if w.get("example_ko"):
            clips.append(_clip(w, "example_ko", "example_audio", FEMALE))
    for g in lesson.get("grammar", []):
        for e in g.get("examples", []):
            clips.append(_clip(e, "ko", "audio", FEMALE))
    for line in lesson.get("dialogue", {}).get("lines", []):
        clips.append(_clip(line, "ko", "audio", voices.get(line["speaker"], FEMALE)))
    for step in (lesson.get("story") or {}).get("steps", []):
        voice = voices.get(step.get("speaker"), FEMALE)
        if step.get("type") == "line":
            clips.append(_clip(step, "ko", "audio", voice))
        elif step.get("type") == "choose_reply":
            clips.append(_clip(step, "answer", "audio", voice))
    for ex in lesson.get("exercises", []):
        if ex.get("type") == "listen_question" and ex.get("audio_text"):
            clips.append(_clip(ex, "audio_text", "audio", FEMALE))
    return clips


def collect_hangul(course: dict) -> list:
    """Hangul course (assets/hangul.json): each letter's syllable (`say`) and its example word."""
    clips = []
    for letter in course.get("letters", []):
        clips.append(_clip(letter, "say", "audio", FEMALE))
        clips.append(_clip(letter["example"], "ko", "audio", FEMALE))
    return clips


def dialogue_file_name(lines: list) -> str:
    """Name of a joined multi-voice clip; `lines` is [(voice, text), …]."""
    key = "|".join(f"{voice}|{text}" for voice, text in lines)
    return hashlib.sha1(key.encode("utf-8")).hexdigest()[:12] + EXT


def dialogue_lines(ex: dict) -> list:
    """A two-person dialogue: lines alternate female, male, female, …"""
    return [(FEMALE if i % 2 == 0 else MALE, text) for i, text in enumerate(ex["audio_dialogue"])]


def collect_final(test: dict) -> list:
    """Listening clips of the final test (assets/final_test.json) as (lines, setter);
    a dialogue becomes one clip whose lines are synthesized separately and joined."""
    clips = []
    for ex in test.get("listening", []):
        if ex.get("audio_dialogue"):
            lines = dialogue_lines(ex)
        elif ex.get("audio_text"):
            lines = [(FEMALE, ex["audio_text"])]
        else:
            continue
        clips.append((lines, lambda name, ex=ex: ex.__setitem__("audio", name)))
    return clips


def synthesize(text: str, voice: str) -> bytes:
    import edge_tts  # imported here so tests and --dry-run work without the package

    async def fetch() -> bytes:
        chunks = []
        async for chunk in edge_tts.Communicate(text, voice).stream():
            if chunk["type"] == "audio":
                chunks.append(chunk["data"])
        return b"".join(chunks)

    data = asyncio.run(fetch())
    if not data:
        raise RuntimeError(f"edge-tts returned no audio for {text!r}")
    return data


def run(root: Path, synth: Callable[[str, str], bytes], dry_run: bool, prune: bool) -> Report:
    assets = root / "app/src/main/assets"
    audio_dir = assets / "audio"
    voices = load_voices(json.loads((assets / "characters.json").read_text(encoding="utf-8")))
    report = Report()
    referenced = set()
    documents = [(path, lambda doc: collect(doc, voices)) for path in sorted((assets / "lessons").glob("*.json"))]
    if (assets / "hangul.json").is_file():
        documents.append((assets / "hangul.json", collect_hangul))
    for path, collector in documents:
        lesson = json.loads(path.read_text(encoding="utf-8"))
        for clip in collector(lesson):
            name = file_name(clip.voice, clip.text)
            referenced.add(name)
            target = audio_dir / name
            if target.is_file():
                report.skipped += 1
            else:
                report.new += 1
                if dry_run:
                    continue
                audio_dir.mkdir(parents=True, exist_ok=True)
                target.write_bytes(synth(clip.text, clip.voice))
            clip.set(name)
        if not dry_run:
            path.write_text(json.dumps(lesson, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    final_path = assets / "final_test.json"
    if final_path.is_file():
        test = json.loads(final_path.read_text(encoding="utf-8"))
        for lines, set_name in collect_final(test):
            # A single line keeps the plain clip name so existing files stay valid.
            name = file_name(*lines[0]) if len(lines) == 1 else dialogue_file_name(lines)
            referenced.add(name)
            target = audio_dir / name
            if target.is_file():
                report.skipped += 1
            else:
                report.new += 1
                if dry_run:
                    continue
                audio_dir.mkdir(parents=True, exist_ok=True)
                # edge-tts streams the same MP3 format for every voice, so the parts can be joined byte by byte.
                target.write_bytes(b"".join(synth(text, voice) for voice, text in lines))
            set_name(name)
        if not dry_run:
            final_path.write_text(json.dumps(test, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    if prune and not dry_run and audio_dir.is_dir():
        for f in audio_dir.iterdir():
            if f.suffix in (".mp3", ".ogg") and f.name not in referenced:
                f.unlink()
                report.pruned += 1
    return report


def main(argv=None, root: Path = ROOT) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dry-run", action="store_true", help="only count the clips that would be generated")
    parser.add_argument("--prune", action="store_true", help="delete audio files no lesson references")
    args = parser.parse_args(argv)
    report = run(root, synthesize, args.dry_run, args.prune)
    verb = "would generate" if args.dry_run else "generated"
    print(f"{verb} {report.new}, already present {report.skipped}, pruned {report.pruned}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
