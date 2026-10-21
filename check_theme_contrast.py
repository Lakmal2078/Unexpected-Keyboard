#!/usr/bin/env python3
"""Check WCAG contrast for statically resolvable keyboard theme color pairs.

Dynamic Android system palette references (for example Monet colors) are skipped
because their concrete RGB values are supplied by the device at runtime.
"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
THEMES_PATH = ROOT / "res" / "values" / "themes.xml"
HEX_RE = re.compile(r"^#[0-9a-fA-F]{3,8}$")
TEXT_MINIMUM = 4.5
GRAPHIC_MINIMUM = 3.0


def read_styles(path):
    root = ET.parse(path).getroot()
    styles = {}
    for node in root.findall("style"):
        name = node.attrib["name"]
        parent = node.attrib.get("parent", "").removeprefix("@style/")
        items = {
            item.attrib["name"]: (item.text or "").strip()
            for item in node.findall("item")
            if "name" in item.attrib
        }
        styles[name] = {"parent": parent, "items": items}
    return styles


STYLES = read_styles(THEMES_PATH)


def raw_item(style_name, attribute, visited=None):
    visited = set() if visited is None else visited
    if style_name in visited or style_name not in STYLES:
        return None
    visited = visited | {style_name}
    style = STYLES[style_name]
    if attribute in style["items"]:
        return style["items"][attribute]
    if style["parent"]:
        return raw_item(style["parent"], attribute, visited)
    return None


def parse_android_color(value):
    if not value or not HEX_RE.match(value):
        return None
    value = value[1:]
    if len(value) == 3:
        value = "".join(channel * 2 for channel in value)
    if len(value) == 4:
        value = "".join(channel * 2 for channel in value)
    if len(value) == 6:
        value = "FF" + value
    if len(value) != 8:
        return None
    return tuple(int(value[offset:offset + 2], 16) for offset in (2, 4, 6, 0))


def resolve_color(style_name, attribute, visited=None):
    visited = set() if visited is None else visited
    marker = (style_name, attribute)
    if marker in visited:
        return None
    visited = visited | {marker}
    value = raw_item(style_name, attribute)
    if value is None:
        return None
    if value.startswith("?"):
        reference = value[1:].removeprefix("attr/")
        return resolve_color(style_name, reference, visited)
    return parse_android_color(value)


def composite(color, background):
    red, green, blue, alpha = color
    opacity = alpha / 255.0
    return (
        red * opacity + background[0] * (1.0 - opacity),
        green * opacity + background[1] * (1.0 - opacity),
        blue * opacity + background[2] * (1.0 - opacity),
    )


def visible_pair(style_name, foreground_attr, background_attr):
    foreground = resolve_color(style_name, foreground_attr)
    background = resolve_color(style_name, background_attr)
    if foreground is None or background is None:
        return None

    background_rgb = background[:3]
    if background[3] < 255:
        underlay = resolve_color(style_name, "colorKeyboard")
        if underlay is None:
            return None
        background_rgb = composite(background, underlay[:3])
    foreground_rgb = composite(foreground, background_rgb)
    return foreground_rgb, background_rgb


def relative_luminance(rgb):
    channels = []
    for channel in rgb:
        value = channel / 255.0
        channels.append(value / 12.92 if value <= 0.04045 else ((value + 0.055) / 1.055) ** 2.4)
    return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]


def contrast_ratio(foreground, background):
    foreground_luminance = relative_luminance(foreground)
    background_luminance = relative_luminance(background)
    lighter = max(foreground_luminance, background_luminance)
    darker = min(foreground_luminance, background_luminance)
    return (lighter + 0.05) / (darker + 0.05)


def main():
    failures = []
    checked = 0
    skipped = 0

    for style_name in sorted(STYLES):
        checks = [
            ("primary key labels", "colorLabel", "colorKey", TEXT_MINIMUM),
            ("secondary key labels", "colorSubLabel", "colorKey", TEXT_MINIMUM),
            ("activated key labels", "colorLabelActivated", "colorKeyActivated", TEXT_MINIMUM),
            ("clipboard action icons", "colorLabel", "colorKeyboard", GRAPHIC_MINIMUM),
        ]
        pressed_value = raw_item(style_name, "colorLabelPressed")
        pressed_attr = "colorLabelPressed" if pressed_value is not None else "colorLabel"
        checks.append(("pressed key labels", pressed_attr, "colorKeyActivated", TEXT_MINIMUM))

        for description, foreground_attr, background_attr, minimum in checks:
            pair = visible_pair(style_name, foreground_attr, background_attr)
            if pair is None:
                skipped += 1
                continue
            checked += 1
            ratio = contrast_ratio(*pair)
            if ratio + 1e-9 < minimum:
                failures.append(
                    f"{style_name}: {description} {ratio:.2f}:1 "
                    f"(requires {minimum:.1f}:1; {foreground_attr} on {background_attr})"
                )

    if failures:
        print(f"Theme contrast check failed: {len(failures)} pair(s) below minimum.")
        for failure in failures:
            print(f"  - {failure}")
        print(f"Checked {checked} pairs; skipped {skipped} dynamic or unresolved pairs.")
        return 1

    print(
        f"Theme contrast check passed: {checked} color pairs meet WCAG thresholds; "
        f"{skipped} dynamic or unresolved pairs skipped."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
