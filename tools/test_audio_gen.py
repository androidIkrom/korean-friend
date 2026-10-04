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

    def test_collect_story_steps(self):
        voices = audio_gen.load_voices(CHARACTERS)
        lesson = {"story": {"title_uz": "T", "steps": [
            {"type": "line", "speaker": "minji", "ko": "어서 와요.", "uz": "Keling."},
            {"type": "choose_reply", "speaker": "aziz", "prompt_uz": "P", "options": ["네", "아", "오"],
             "answer": "네", "uz": "Ha", "why_uz": "W"},
            {"type": "quiz", "prompt_uz": "Q", "options": ["x", "y", "z"], "answer": "x", "why_uz": "W"},
        ]}}
        clips = audio_gen.collect(lesson, voices)
        self.assertEqual([("어서 와요.", audio_gen.FEMALE), ("네", audio_gen.MALE)], [(c.text, c.voice) for c in clips])

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

    def test_hangul_clips(self):
        course = {"letters": [{"id": "g", "say": "가", "example": {"ko": "가구", "roman": "gagu", "uz": "mebel"}}], "lessons": []}
        (self.assets / "hangul.json").write_text(json.dumps(course, ensure_ascii=False), encoding="utf-8")
        audio_gen.run(self.root, lambda text, voice: text.encode("utf-8"), dry_run=False, prune=False)
        saved = json.loads((self.assets / "hangul.json").read_text(encoding="utf-8"))["letters"][0]
        self.assertEqual(audio_gen.file_name(audio_gen.FEMALE, "가"), saved["audio"])
        self.assertEqual(audio_gen.file_name(audio_gen.FEMALE, "가구"), saved["example"]["audio"])
        self.assertTrue((self.assets / "audio" / saved["audio"]).is_file())

    def test_other_books_final_tests(self):
        test = {"listening": [{"id": "final_b1_l01", "audio_dialogue": ["뭐예요?", "책이에요."]}], "reading": []}
        (self.assets / "final_test_b1.json").write_text(json.dumps(test, ensure_ascii=False), encoding="utf-8")
        audio_gen.run(self.root, self.synth, dry_run=False, prune=False)
        saved = json.loads((self.assets / "final_test_b1.json").read_text(encoding="utf-8"))["listening"][0]
        self.assertTrue((self.assets / "audio" / saved["audio"]).is_file())

    def test_final_test_clips(self):
        final = {"listening": [{"id": "final_l01", "type": "listen_question", "audio_text": "얼마예요?"}],
                 "reading": [{"id": "final_r01", "type": "read_choice", "sentence": "읽기"}]}
        (self.assets / "final_test.json").write_text(json.dumps(final, ensure_ascii=False), encoding="utf-8")
        report = audio_gen.run(self.root, self.synth, dry_run=False, prune=True)
        self.assertEqual(7, report.new)
        saved = json.loads((self.assets / "final_test.json").read_text(encoding="utf-8"))
        name = saved["listening"][0]["audio"]
        self.assertEqual(audio_gen.file_name(audio_gen.FEMALE, "얼마예요?"), name)
        self.assertTrue((self.assets / "audio" / name).is_file())
        self.assertNotIn("audio", saved["reading"][0])

    def test_dialogue_alternates_voices_and_joins(self):
        final = {"listening": [{"id": "final_l01", "type": "listen_question", "audio_text": "가 나",
                                "audio_dialogue": ["가?", "나.", "다!"]}], "reading": []}
        (self.assets / "final_test.json").write_text(json.dumps(final, ensure_ascii=False), encoding="utf-8")
        audio_gen.run(self.root, self.synth, dry_run=False, prune=False)
        self.assertIn(("가?", audio_gen.FEMALE), self.calls)
        self.assertIn(("나.", audio_gen.MALE), self.calls)
        self.assertIn(("다!", audio_gen.FEMALE), self.calls)
        saved = json.loads((self.assets / "final_test.json").read_text(encoding="utf-8"))
        name = saved["listening"][0]["audio"]
        lines = [(audio_gen.FEMALE, "가?"), (audio_gen.MALE, "나."), (audio_gen.FEMALE, "다!")]
        self.assertEqual(audio_gen.dialogue_file_name(lines), name)
        data = (self.assets / "audio" / name).read_bytes()
        self.assertEqual(b"ID3" + "가?".encode() + b"ID3" + "나.".encode() + b"ID3" + "다!".encode(), data)

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
