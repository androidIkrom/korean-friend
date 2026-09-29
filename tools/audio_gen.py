"""Generate lesson audio with Google Cloud Text-to-Speech and record file names in the lesson JSON.

Usage:
    python tools/audio_gen.py [--dry-run] [--prune]

Reads the API key from the GOOGLE_TTS_API_KEY environment variable (never from a file).
Only texts without an existing audio file are synthesized. --prune deletes .ogg files
that no lesson references any more.
"""

import argparse
import base64
import hashlib
import json
import os
import sys
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, NamedTuple

ROOT = Path(__file__).resolve().parent.parent
FEMALE = "ko-KR-Neural2-A"
MALE = "ko-KR-Neural2-C"
VOICE_BY_GENDER = {"female": FEMALE, "male": MALE}
API_URL = "https://texttospeech.googleapis.com/v1/text:synthesize?key="
KEY_ENV = "GOOGLE_TTS_API_KEY"


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
    return hashlib.sha1(f"{voice}|{text}".encode("utf-8")).hexdigest()[:12] + ".ogg"


def load_voices(characters: dict) -> dict:
    """Character id -> Google voice name."""
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
    for line in (lesson.get("story") or {}).get("lines", []):
        clips.append(_clip(line, "ko", "audio", voices.get(line["speaker"], FEMALE)))
    for ex in lesson.get("exercises", []):
        if ex.get("type") == "listen_question" and ex.get("audio_text"):
            clips.append(_clip(ex, "audio_text", "audio", FEMALE))
    return clips


def synthesize(text: str, voice: str, api_key: str) -> bytes:
    body = {
        "input": {"text": text},
        "voice": {"languageCode": "ko-KR", "name": voice},
        "audioConfig": {"audioEncoding": "OGG_OPUS"},
    }
    req = urllib.request.Request(
        API_URL + api_key,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json; charset=utf-8"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return base64.b64decode(json.load(resp)["audioContent"])


def run(root: Path, synth: Callable[[str, str], bytes], dry_run: bool, prune: bool) -> Report:
    assets = root / "app/src/main/assets"
    audio_dir = assets / "audio"
    voices = load_voices(json.loads((assets / "characters.json").read_text(encoding="utf-8")))
    report = Report()
    referenced = set()
    for path in sorted((assets / "lessons").glob("*.json")):
        lesson = json.loads(path.read_text(encoding="utf-8"))
        for clip in collect(lesson, voices):
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
    if prune and not dry_run and audio_dir.is_dir():
        for f in audio_dir.glob("*.ogg"):
            if f.name not in referenced:
                f.unlink()
                report.pruned += 1
    return report


def main(argv=None, root: Path = ROOT) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dry-run", action="store_true", help="only count the clips that would be generated")
    parser.add_argument("--prune", action="store_true", help="delete audio files no lesson references")
    args = parser.parse_args(argv)
    key = os.environ.get(KEY_ENV)
    if not args.dry_run and not key:
        print(f"{KEY_ENV} is not set. Set it to a Cloud Text-to-Speech API key.", file=sys.stderr)
        return 2
    report = run(root, lambda text, voice: synthesize(text, voice, key), args.dry_run, args.prune)
    verb = "would generate" if args.dry_run else "generated"
    print(f"{verb} {report.new}, already present {report.skipped}, pruned {report.pruned}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
