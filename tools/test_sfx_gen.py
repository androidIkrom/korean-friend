import tempfile
import unittest
import wave
from pathlib import Path

import sfx_gen


class SfxGenTest(unittest.TestCase):
    def test_every_clip_is_a_short_mono_wav(self):
        with tempfile.TemporaryDirectory() as d:
            sfx_gen.main(d)
            files = sorted(Path(d).glob("sfx_*.wav"))
            self.assertEqual(
                ["clear", "combo", "correct", "open", "rank_up", "tap", "wrong", "xp"],
                [f.stem[4:] for f in files],
            )
            for f in files:
                with wave.open(str(f)) as w:
                    self.assertEqual(1, w.getnchannels())
                    self.assertEqual(2, w.getsampwidth())
                    seconds = w.getnframes() / w.getframerate()
                    self.assertTrue(0.04 <= seconds <= 1.2, f"{f.name}: {seconds}s")
                self.assertLess(f.stat().st_size, 60_000, f.name)

    def test_clips_are_not_clipped(self):
        for name, samples in sfx_gen.clips().items():
            self.assertLessEqual(max(abs(v) for v in samples), 0.9, name)


if __name__ == "__main__":
    unittest.main()
