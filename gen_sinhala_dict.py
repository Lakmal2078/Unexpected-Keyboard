#!/usr/bin/env python3
"""
gen_sinhala_dict.py

Compiles a comprehensive Sinhala dictionary in libcdict (Cdict) binary format
for Unexpected Keyboard.
"""

import gzip
import os
import re
import struct
import sys
import urllib.request

# Offline fallback list of core high-frequency Sinhala words in case internet is not reachable
CORE_SINHALA_WORDS = [
    ("මම", 15), ("ඔයා", 15), ("අපි", 15), ("එයා", 15), ("ඔබ", 15),
    ("මට", 15), ("ඔයාට", 15), ("අපිට", 15), ("එයාට", 15), ("ඔබට", 15),
    ("මගේ", 15), ("ඔයාගේ", 15), ("අපේ", 15), ("එයාගේ", 15), ("ඔබගේ", 15),
    ("මේ", 15), ("මේක", 15), ("එක", 15), ("අරක", 15), ("ඒක", 15),
    ("ගෙදර", 15), ("වැඩ", 15), ("වැඩක්", 15), ("වැඩේ", 15), ("සල්ලි", 15),
    ("පොත", 15), ("පොත්", 15), ("කෑම", 15), ("වතුර", 15), ("රට", 15),
    ("ලංකා", 15), ("ශ්‍රී", 15), ("ලංකාව", 15), ("සිංහල", 15), ("සිංහලය", 15),
    ("ස්තූතියි", 15), ("බොහොම", 15), ("ආයුබෝවන්", 15), ("කරුණාකර", 15),
    ("සුබ", 15), ("උදෑසනක්", 15), ("දවසක්", 15), ("රාත්‍රියක්", 15),
    ("යනවා", 15), ("යන්න", 15), ("ගියා", 15), ("එනවා", 15), ("එන්න", 15),
    ("ආවා", 15), ("කරනවා", 15), ("කරන්න", 15), ("කළා", 15), ("බලනවා", 15),
    ("බලන්න", 15), ("කනවා", 15), ("කන්න", 15), ("බොනවා", 15), ("බොන්න", 15),
    ("හිතනවා", 15), ("හිතන්න", 15), ("දන්නවා", 15), ("දන්න", 15),
    ("කියන්න", 15), ("කියලා", 15), ("කිව්වා", 15), ("තියෙනවා", 15), ("තියෙන්නේ", 15),
    ("ලස්සන", 15), ("හොඳ", 15), ("නරක", 15), ("ගොඩක්", 15), ("ටිකක්", 15),
    ("පොඩ්ඩක්", 15), ("හරි", 15), ("පුළුවන්", 15), ("පුළුවන්ද", 15), ("බැහැ", 15),
    ("බෑ", 15), ("නැහැ", 15), ("නෑ", 15), ("ඔව්", 15), ("හෙට", 15),
    ("අද", 15), ("ඊයේ", 15), ("දැන්", 15), ("පස්සේ", 15), ("කලින්", 15),
    ("මොකද", 15), ("කොහොමද", 15), ("කොහෙද", 15), ("කවදාද", 15), ("මොකක්ද", 15),
    ("මොනවද", 15), ("ඇයි", 15), ("කවුද", 15), ("කාටද", 15), ("කාගේද", 15),
    ("පාසල", 14), ("ඉස්කෝලේ", 14), ("විශ්වවිද්‍යාලය", 14), ("කැම්පස්", 14),
    ("දුරකථනය", 14), ("ෆෝන්", 14), ("පරිගණකය", 14), ("කීබෝඩ්", 14),
    ("පණිවිඩය", 14), ("මැසේජ්", 14), ("ඡායාරූපය", 14), ("ෆොටෝ", 14),
    ("ක්‍රිකට්", 14), ("මැච්", 14), ("චිත්‍රපටය", 14), ("සින්දුව", 14),
    ("තෙරුවන්", 15), ("සරණයි", 15), ("බුදු", 15), ("දහම්", 15), ("සඟ", 15),
    ("පරිස්සමෙන්", 15), ("කාරුණික", 14), ("ඉල්ලීමක්", 14), ("දැනුම්දීමක්", 14),
    ("ප්‍රශ්නය", 14), ("විසඳුම", 14), ("ගැටලුව", 14), ("සාකච්ඡාව", 14),
]


def load_words_from_transliterator(file_path):
    words = set()
    if os.path.exists(file_path):
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()
            for m in re.finditer(r"\"([\u0D80-\u0DFF\u200D]+)\"", content):
                w = m.group(1).strip()
                if w:
                    words.add(w)
    return words


def load_words_from_libreoffice():
    url = "https://raw.githubusercontent.com/LibreOffice/dictionaries/master/si_LK/si_LK.dic"
    words = set()
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            content = resp.read().decode("utf-8")
            for line in content.splitlines()[1:]:
                line = line.strip()
                if not line:
                    continue
                word = line.split("/")[0].strip()
                # Include Sinhala letters and ZWJ (Zero Width Joiner for Yansaya/Rakaransaya)
                if re.match(r"^[\u0D80-\u0DFF\u200D]+$", word):
                    words.add(word)
        print(f"Loaded {len(words)} words from LibreOffice repository.")
    except Exception as e:
        print(f"Notice: Could not load remote LibreOffice dictionary ({e}). Using local/embedded wordlist.")
    return words


def build_trie_dict(word_freq_map, dict_name="main"):
    # Filter and sort
    items = sorted(word_freq_map.items(), key=lambda x: x[0].encode("utf-8"))

    class TrieNode:
        __slots__ = ("is_final", "freq", "children", "pos", "total_words", "word_index")
        def __init__(self):
            self.is_final = False
            self.freq = 0
            self.children = {}
            self.pos = 0
            self.total_words = 0
            self.word_index = -1

    root = TrieNode()

    for w, f in items:
        cur = root
        for b in w.encode("utf-8"):
            if b not in cur.children:
                cur.children[b] = TrieNode()
            cur = cur.children[b]
        cur.is_final = True
        cur.freq = f

    # Assign alphabetical word indexes
    words_in_order = []
    def assign_indexes(n, current_bytes):
        if n.is_final:
            n.word_index = len(words_in_order)
            words_in_order.append((bytes(current_bytes), n.freq))
        for b in sorted(n.children.keys()):
            current_bytes.append(b)
            assign_indexes(n.children[b], current_bytes)
            current_bytes.pop()

    assign_indexes(root, bytearray())

    # Count reachable words
    def count_words(n):
        c = 1 if n.is_final else 0
        for b in sorted(n.children.keys()):
            c += count_words(n.children[b])
        n.total_words = c
        return c

    count_words(root)

    BRANCHES_FMT = 6  # 24-bit signed
    NUMBERS_FMT = 6   # 24-bit unsigned

    def calc_node_size(n):
        L = len(n.children)
        raw_size = 2 + 7 * L
        # 2-byte alignment ensures all offsets are even
        return (raw_size + 1) & ~1

    nodes_order = []
    def layout_postorder(n):
        for b in sorted(n.children.keys()):
            layout_postorder(n.children[b])
        nodes_order.append(n)

    layout_postorder(root)

    # 5 bytes file header + 24 bytes dict header + 1 byte pad = 30 bytes
    current_offset = 30
    for n in nodes_order:
        n.pos = current_offset
        current_offset += calc_node_size(n)

    root_ptr = root.pos

    def make_complete_tree(branches_list):
        L = len(branches_list)
        if L == 0:
            return []
        tree = [None] * L
        def width_of_last_full_level(len_):
            s = 1
            while (s * 2 + 1) <= len_:
                s = s * 2 + 1
            return (s // 2) + 1, len_ - s

        def pivot(len_):
            width, last_level = width_of_last_full_level(len_)
            if last_level <= width:
                return (len_ + last_level) // 2
            else:
                return (width * 2) - 1

        def loop(lo, hi, dsti):
            if lo > hi:
                return
            mid = lo + pivot(hi - lo + 1)
            tree[dsti] = branches_list[mid]
            loop(lo, mid - 1, dsti * 2 + 1)
            loop(mid + 1, hi, dsti * 2 + 2)

        loop(0, L - 1, 0)
        return tree

    nodes_bytes = bytearray()
    for n in nodes_order:
        L = len(n.children)
        sorted_branches = []
        cum_words = 0
        for b in sorted(n.children.keys()):
            child = n.children[b]
            sorted_branches.append((b, child, cum_words))
            cum_words += child.total_words

        tree = make_complete_tree(sorted_branches)

        header = (NUMBERS_FMT << 4) | (BRANCHES_FMT << 1) | 0
        node_buf = bytearray()
        node_buf.append(header)
        node_buf.append(L)

        # Labels (complete binary tree)
        for item in tree:
            node_buf.append(item[0])

        # Branches
        for item in tree:
            child = item[1]
            offset = child.pos - n.pos
            assert offset % 2 == 0, f"Offset {offset} must be even"
            ptr = offset | (1 if child.is_final else 0)
            ptr_u = ptr & 0xFFFFFF
            node_buf.append((ptr_u >> 16) & 0xFF)
            node_buf.append((ptr_u >> 8) & 0xFF)
            node_buf.append(ptr_u & 0xFF)

        # Numbers
        for item in tree:
            num = item[2]
            num_u = num & 0xFFFFFF
            node_buf.append((num_u >> 16) & 0xFF)
            node_buf.append((num_u >> 8) & 0xFF)
            node_buf.append(num_u & 0xFF)

        if len(node_buf) < calc_node_size(n):
            node_buf.append(0)

        assert len(node_buf) == calc_node_size(n)
        nodes_bytes.extend(node_buf)

    freq_off = current_offset
    total_w = len(words_in_order)
    freq_size = (total_w + 1) // 2
    freq_buf = bytearray(freq_size)
    for i, (w_bytes, f) in enumerate(words_in_order):
        nibble = min(15, max(0, f)) & 0x0F
        if (i & 1) == 0:
            freq_buf[i >> 1] |= nibble
        else:
            freq_buf[i >> 1] |= (nibble << 4)

    current_offset += freq_size

    name_off = current_offset
    name_bytes = dict_name.encode("latin1") + b"\0"
    current_offset += len(name_bytes)

    # File Header
    hdr = bytearray()
    hdr.extend(b"Dic")
    hdr.append(1)  # Version 1
    hdr.append(1)  # Dict count 1
    hdr.extend(struct.pack(">III", name_off, root_ptr, freq_off))
    hdr.append(0)  # Aliases header
    hdr.extend(b"\0\0\0")  # Aliases length
    hdr.extend(struct.pack(">II", 0, 0))  # Aliases keys, values
    hdr.append(0)  # Pad to 30 bytes

    final_data = bytes(hdr) + bytes(nodes_bytes) + bytes(freq_buf) + name_bytes
    return final_data


def main():
    sys.setrecursionlimit(150000)
    word_freq = {}

    # 1. Base core words
    for w, f in CORE_SINHALA_WORDS:
        word_freq[w] = f

    # 2. Transliterator words
    trans_path = os.path.join(os.path.dirname(__file__), "srcs/java/juloo/keyboard2/SinhalaTransliterator.java")
    trans_words = load_words_from_transliterator(trans_path)
    for w in trans_words:
        word_freq[w] = 15

    # 3. LibreOffice Sinhala dictionary
    lo_words = load_words_from_libreoffice()
    for w in lo_words:
        if w not in word_freq:
            word_freq[w] = 8

    print(f"Total vocabulary size: {len(word_freq)} words.")
    dict_bytes = build_trie_dict(word_freq, "main")
    output_dir = os.path.join(os.path.dirname(__file__), "assets/dict")
    os.makedirs(output_dir, exist_ok=True)
    out_file = os.path.join(output_dir, "si.dict")

    with open(out_file, "wb") as f:
        f.write(dict_bytes)

    compressed = gzip.compress(dict_bytes)
    print(f"Generated {out_file}")
    print(f"Uncompressed size: {len(dict_bytes)} bytes")
    print(f"Gzip compressed size: {len(compressed)} bytes")


if __name__ == "__main__":
    main()
