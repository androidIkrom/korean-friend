import hashlib
import io
import json
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import audio_gen  # noqa: E402

LESSON = {
    "id": "u02_l1",
    "words": [{"id": "w1", "ko": "옷", "example_ko": "이 옷이 예뻐요."}],
    "grammar": [{"id": "g1", "examples": [{"ko": "입어 보세요.", "uz": "Kiyib ko'ring."}]}],
    "dialogue": {"lines": [{"speaker": "aziz", "ko": "안녕하세요.", "uz": "Salom."}, {"speaker": "minji", "ko": "네.", "uz": "Ha."}]},
    "exercises": [
        {"id": "e1", "type": "listen_question", "audio_text": "뭘 찾으세요?"},
        {"id": "e2", "type": "conjugate"},
    ],
}
CHARACTERS = {"characters": [{"id": "aziz", "voice": "male"}, {"id": "minji", "voice": "female"}]}


class AudioGenTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.assets = self.root / "app/src/main/assets"
        (self.assets / "lessons").mkdir(parents=True)
        (self.assets / "lessons/u02_l1.json").write_text(json.dumps(LESSON, ensure_ascii=False), encoding="utf-8")
        (self.assets / "characters.json").write_text(json.dumps(CHARACTERS), encoding="utf-8")
        self.calls = []

    def tearDown(self):
        self.tmp.cleanup()

    def synth(self, text, voice):
        self.calls.append((text, voice))
        return b"ID3" + text.encode("utf-8")

    def lesson(self):
        return json.loads((self.assets / "lessons/u02_l1.json").read_text(encoding="utf-8"))

    def test_file_name_is_sha1_prefix(self):
        expected = hashlib.sha1("ko-KR-SunHiNeural|옷".encode("utf-8")).hexdigest()[:12] + ".mp3"
        self.assertEqual(expected, audio_gen.file_name("ko-KR-SunHiNeural", "옷"))

    def test_collect_covers_all_fields(self):
        voices = audio_gen.load_voices(CHARACTERS)
        texts = [c.text for c in audio_gen.collect(json.loads(json.dumps(LESSON)), voices)]
        self.assertEqual(["옷", "이 옷이 예뻐요.", "입어 보세요.", "안녕하세요.", "네.", "뭘 찾으세요?"], texts)

    def test_dialogue_uses_character_voice(self):
        voices = audio_gen.load_voices(CHARACTERS)
        clips = {c.text: c.voice for c in audio_gen.collect(json.loads(json.dumps(LESSON)), voices)}
        self.assertEqual(audio_gen.MALE, clips["안녕하세요."])
        self.assertEqual(audio_gen.FEMALE, clips["네."])
        self.assertEqual(audio_gen.FEMALE, clips["옷"])

    def test_run_writes_files_and_json(self):
        report = audio_gen.run(self.root, self.synth, dry_run=False, prune=False)
        self.assertEqual(6, report.new)
        lesson = self.lesson()
        name = lesson["words"][0]["audio"]
        self.assertEqual(audio_gen.file_name(audio_gen.FEMALE, "옷"), name)
        self.assertTrue((self.assets / "audio" / name).is_file())
        self.assertIn("audio", lesson["dialogue"]["lines"][0])
        self.assertIn("audio", lesson["exercises"][0])
        self.assertNotIn("audio", lesson["exercises"][1])
        self.assertIn("example_audio", lesson["words"][0])

    def test_run_skips_existing(self):
        audio_gen.run(self.root, self.synth, dry_run=False, prune=False)
        self.calls.clear()
        report = audio_gen.run(self.root, self.synth, dry_run=False, prune=False)
        self.assertEqual([], self.calls)
        self.assertEqual(0, report.new)
        self.assertEqual(6, report.skipped)

    def test_dry_run_writes_nothing(self):
        report = audio_gen.run(self.root, self.synth, dry_run=True, prune=False)
        self.assertEqual(6, report.new)
        self.assertEqual([], self.calls)
        self.assertNotIn("audio", self.lesson()["words"][0])
        self.assertFalse((self.assets / "audio").exists() and any((self.assets / "audio").iterdir()))

    def test_prune_removes_unreferenced(self):
        audio_gen.run(self.root, self.synth, dry_run=False, prune=False)
        stale = self.assets / "audio/zz.mp3"
        stale.write_bytes(b"x")
        report = audio_gen.run(self.root, self.synth, dry_run=False, prune=True)
        self.assertFalse(stale.exists())
        self.assertEqual(1, report.pruned)
        self.assertTrue((self.assets / "audio" / self.lesson()["words"][0]["audio"]).is_file())

    def test_main_dry_run_needs_no_key(self):
        out = io.StringIO()
        with redirect_stdout(out):
            code = audio_gen.main(["--dry-run"], root=self.root)
        self.assertEqual(0, code)
        self.assertIn("would generate 6", out.getvalue())

    def test_voices_are_edge_korean_neural(self):
        self.assertEqual("ko-KR-SunHiNeural", audio_gen.FEMALE)
        self.assertEqual("ko-KR-InJoonNeural", audio_gen.MALE)


if __name__ == "__main__":
    unittest.main()
