import importlib.util
from pathlib import Path
import tempfile
import unittest

REPO_ROOT = Path(__file__).resolve().parents[2]
SCRIPT = REPO_ROOT / "gen_sinhala_dict.py"
SPEC = importlib.util.spec_from_file_location("sinhala_dictionary_generator", SCRIPT)
assert SPEC and SPEC.loader

generator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(generator)


class SinhalaDictionaryGeneratorTest(unittest.TestCase):
    def test_phrase_mapping_contributes_each_sinhala_word(self):
        with tempfile.TemporaryDirectory() as tmp:
            source = Path(tmp) / "SinhalaTransliterator.java"
            source.write_text(
                'COMMON_WORDS.put("saudi", "සවුදි අරාබිය");\n'
                'COMMON_WORDS.put("temple", "පන්සල්");\n',
                encoding="utf-8",
            )
            words = generator.load_words_from_transliterator(str(source))
        self.assertIn("සවුදි", words)
        self.assertIn("අරාබිය", words)
        self.assertIn("පන්සල්", words)

    def test_cleaner_keeps_valid_sinhala_and_conjuncts(self):
        for word in ("සිංහල", "ක්‍රමය", "විද්‍යාව", "ආයුබෝවන්", "සවුදි"):
            with self.subTest(word=word):
                self.assertEqual(word, generator.clean_sinhala_word(word))

    def test_cleaner_rejects_malformed_repeated_marks(self):
        for word in ("කිිසිසේත්", "අවස්්ථාවක", "ආර්්ථික", "කෑෑමට"):
            with self.subTest(word=word):
                self.assertIsNone(generator.clean_sinhala_word(word))

    def test_cleaner_rejects_non_sinhala_and_leading_marks(self):
        self.assertIsNone(generator.clean_sinhala_word("hello"))
        self.assertIsNone(generator.clean_sinhala_word("\u0DD2සිංහල"))
        self.assertIsNone(generator.clean_sinhala_word("\u0DCA"))

    def test_trie_builder_cleans_malformed_entries(self):
        data = generator.build_trie_dict({"සිංහල": 15, "කිිසිසේත්": 8}, "main")
        self.assertTrue(data.startswith(b"Dic"))
        self.assertGreater(len(data), 30)


if __name__ == "__main__":
    unittest.main()
