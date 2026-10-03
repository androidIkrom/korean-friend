"""Synthesize the game's sound effects into app/src/main/res/raw/sfx_*.wav.

Usage:
    python tools/sfx_gen.py

Everything is generated from sine waves and noise with the standard library, so the clips are
original work with no licence question. Output: 22.05 kHz, mono, 16-bit WAV.
"""

import math
import random
import struct
import sys
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "app/src/main/res/raw"
RATE = 22050


def note(freq, dur, vol=0.5, attack=0.005, decay=None, harmonics=(1.0, 0.35, 0.12), glide=0.0, vibrato=0.0):
    """A tone with a few harmonics, an attack and an exponential decay; [glide] bends the pitch by that ratio."""
    n = int(dur * RATE)
    decay = decay or dur / 4
    out = []
    phase = [0.0] * len(harmonics)
    for i in range(n):
        t = i / RATE
        f = freq * (1 + glide * t / dur) * (1 + vibrato * math.sin(2 * math.pi * 6 * t))
        env = min(1.0, t / attack) * math.exp(-t / decay)
        s = 0.0
        for k, amp in enumerate(harmonics):
            phase[k] += 2 * math.pi * f * (k + 1) / RATE
            s += amp * math.sin(phase[k])
        out.append(vol * env * s / sum(harmonics))
    return out


def noise(dur, vol=0.3, decay=0.05, lowpass=0.2, sweep=0.0, seed=7):
    """Filtered white noise; [sweep] opens the filter over the clip (a whoosh)."""
    rng = random.Random(seed)
    n = int(dur * RATE)
    out, y = [], 0.0
    for i in range(n):
        t = i / RATE
        a = min(0.95, lowpass + sweep * t / dur)
        y = a * (rng.random() * 2 - 1) + (1 - a) * y
        out.append(vol * y * math.exp(-t / decay) * min(1.0, t / 0.004))
    return out


def mix(*parts):
    """Each part is (start_seconds, samples)."""
    length = max(int(start * RATE) + len(s) for start, s in parts)
    out = [0.0] * length
    for start, s in parts:
        o = int(start * RATE)
        for i, v in enumerate(s):
            out[o + i] += v
    peak = max(1e-9, max(abs(v) for v in out))
    return [v * min(1.0, 0.89 / peak) for v in out]


def clips():
    return {
        "tap": mix((0, note(1700, 0.05, 0.6, decay=0.012, harmonics=(1.0, 0.2))), (0, noise(0.03, 0.25, decay=0.006, lowpass=0.6))),
        "correct": mix((0, note(1318.5, 0.18, 0.55, decay=0.07)), (0.08, note(1975.5, 0.3, 0.6, decay=0.12))),
        "wrong": mix((0, note(233, 0.32, 0.6, decay=0.12, harmonics=(1.0, 0.6, 0.4, 0.25), glide=-0.3)), (0, noise(0.08, 0.2, decay=0.03))),
        "combo": mix(
            (0, note(1046.5, 0.12, 0.5, decay=0.05)),
            (0.06, note(1318.5, 0.12, 0.5, decay=0.05)),
            (0.12, note(1568.0, 0.22, 0.6, decay=0.09)),
        ),
        "xp": mix((0, note(1500, 0.26, 0.45, decay=0.1, harmonics=(1.0, 0.15), glide=1.0, vibrato=0.01)), (0.05, note(3000, 0.12, 0.2, decay=0.04))),
        "clear": mix(
            (0, note(523.3, 0.5, 0.45, decay=0.25)),
            (0.09, note(659.3, 0.5, 0.45, decay=0.25)),
            (0.18, note(784.0, 0.5, 0.45, decay=0.25)),
            (0.27, note(1046.5, 0.65, 0.6, decay=0.3, vibrato=0.004)),
        ),
        "rank_up": mix(
            (0, noise(0.5, 0.25, decay=0.3, lowpass=0.05, sweep=0.8)),
            (0, note(220, 0.5, 0.35, decay=0.3, glide=1.0)),
            (0.45, note(523.3, 0.6, 0.4, decay=0.3)),
            (0.45, note(659.3, 0.6, 0.35, decay=0.3)),
            (0.45, note(784.0, 0.6, 0.35, decay=0.3)),
            (0.55, note(1046.5, 0.55, 0.45, decay=0.3, vibrato=0.005)),
        ),
        "open": mix((0, noise(0.22, 0.35, decay=0.1, lowpass=0.03, sweep=0.5)), (0.04, note(880, 0.16, 0.25, decay=0.06, glide=0.5))),
    }


def write(path, samples):
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1.0, min(1.0, v)) * 32767)) for v in samples))


def main(out_dir=RAW):
    for name, samples in clips().items():
        write(Path(out_dir) / f"sfx_{name}.wav", samples)
    print(f"wrote {len(clips())} clips to {out_dir}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
