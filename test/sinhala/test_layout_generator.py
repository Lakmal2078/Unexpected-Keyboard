import importlib.util
from pathlib import Path
import subprocess
import sys
import unittest
import xml.etree.ElementTree as ET

REPO_ROOT = Path(__file__).resolve().parents[2]
SCRIPT = REPO_ROOT / "gen_sinhala_phonetic_layout.py"
SPEC = importlib.util.spec_from_file_location("sinhala_layout_generator", SCRIPT)
assert SPEC and SPEC.loader
generator = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = generator
SPEC.loader.exec_module(generator)


class SinhalaLayoutGeneratorTest(unittest.TestCase):
    def test_escape_ranges_cover_sinhala_combining_signs(self):
        self.assertTrue(generator.is_in_escape_list("\u0DCA"))
        self.assertTrue(generator.is_in_escape_list("\u0DD0"))
        self.assertFalse(generator.is_in_escape_list("a"))

    def test_invalid_escape_list_entry_raises(self):
        original = generator.ESCAPE_LIST
        try:
            generator.ESCAPE_LIST = [object()]
            with self.assertRaises(TypeError):
                generator.is_in_escape_list("a")
        finally:
            generator.ESCAPE_LIST = original

    def test_generated_xml_is_parseable_and_deterministic(self):
        command = [sys.executable, str(SCRIPT), "--output", "-"]
        first = subprocess.run(command, check=True, capture_output=True, text=True, encoding="utf-8")
        second = subprocess.run(command, check=True, capture_output=True, text=True, encoding="utf-8")
        self.assertEqual(first.stdout, second.stdout)
        root = ET.fromstring(first.stdout)
        self.assertEqual("keyboard", root.tag)
        self.assertEqual("sinhala", root.attrib.get("script"))
        self.assertEqual("සිංහල", root.attrib.get("name"))


if __name__ == "__main__":
    unittest.main()
