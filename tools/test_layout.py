"""Checks that the pieces of the build layout agree with each other.

- the build-logic folder names in shared/mod/settings.gradle.kts (which runs before
  this build is on the classpath) and in BuildLogicNames.kt are the same set;
- versions come from gradle/libs.versions.toml, not from the common TOML;
- nothing in the Kotlin sources still hard-codes the sibling folder's name.
"""
import re
import unittest

from bichal_mod import BUILD_LOGIC, catalog_version, read_toml
from bichal_sync import same_content

SETTINGS_SCRIPT = BUILD_LOGIC / "shared" / "mod" / "settings.gradle.kts"
NAMES_FILE = BUILD_LOGIC / "src" / "main" / "kotlin" / "net" / "bichal" / "gradle" / "BuildLogicNames.kt"
KOTLIN_SOURCES = BUILD_LOGIC / "src" / "main" / "kotlin"


def quoted_in_setof(text: str, pattern: str) -> set[str]:
    m = re.search(pattern, text)
    if not m:
        raise AssertionError(f"pattern not found: {pattern}")
    return set(re.findall(r'"([^"]+)"', m.group(1)))


class LayoutTest(unittest.TestCase):

    def test_build_logic_names_agree(self):
        script = quoted_in_setof(SETTINGS_SCRIPT.read_text(encoding="utf-8"),
                                 r"val buildLogicNames = setOf\(([^)]*)\)")
        kotlin = quoted_in_setof(NAMES_FILE.read_text(encoding="utf-8"),
                                 r"val accepted: Set<String> = setOf\(([^)]*)\)")
        self.assertEqual(script, kotlin)

    def test_versions_come_from_the_catalog(self):
        for key in ("stonecutter", "moddev", "loom", "foojay", "gson", "junit"):
            self.assertTrue(catalog_version(key), key)

    def test_catalog_versions_are_present(self):
        catalog = read_toml(BUILD_LOGIC / "gradle" / "libs.versions.toml")
        self.assertTrue(catalog["versions"])
        self.assertIn("junit-bom", catalog["libraries"])

    def test_common_toml_has_no_plugin_table(self):
        self.assertNotIn("plugins", read_toml(BUILD_LOGIC / "common.stonecutter.properties.toml"))

    def test_crlf_checkout_is_not_drift(self):
        self.assertTrue(same_content(b"a\r\nb\r\n", b"a\nb\n"))
        self.assertTrue(same_content(b"a\nb\n", b"a\nb\n"))
        self.assertFalse(same_content(b"a\nb\n", b"a\nc\n"))
        self.assertFalse(same_content(None, b"a\n"))

    def test_no_hardcoded_sibling_folder_in_kotlin(self):
        for path in KOTLIN_SOURCES.rglob("*.kt"):
            text = path.read_text(encoding="utf-8")
            self.assertNotIn("../bichal-build-logic", text, str(path))


if __name__ == "__main__":
    unittest.main()
