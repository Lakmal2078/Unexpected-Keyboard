package juloo.cdict;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Pure Java implementation of libcdict Radix Tree dictionary reader.
 * Enables fast word lookup, prefix/suffix suggestions, and typo distance corrections
 * without requiring native C/NDK compilation.
 */
public class Cdict {
  public static final class Result {
    public final boolean found;
    public final int index;
    public final long prefix_ptr;
    public final int original_index;

    public Result(boolean found, int index, long prefix_ptr, int original_index) {
      this.found = found;
      this.index = index;
      this.prefix_ptr = prefix_ptr;
      this.original_index = original_index;
    }

    public Result(boolean found, int index) {
      this(found, index, 0, index);
    }
  }

  public final String name;
  private final byte[] _data;
  private final int _root_node;
  private final int _freq_off;
  private final int _aliases_header;
  private final int _aliases_length;
  private final int _aliases_keys;
  private final int _aliases_values;

  private Cdict(String name, byte[] data, int root_node, int freq_off,
                int aliases_header, int aliases_length, int aliases_keys, int aliases_values) {
    this.name = name;
    this._data = data;
    this._root_node = root_node;
    this._freq_off = freq_off;
    this._aliases_header = aliases_header;
    this._aliases_length = aliases_length;
    this._aliases_keys = aliases_keys;
    this._aliases_values = aliases_values;
  }

  public static int format_version() {
    return 1;
  }

  // Validate all externally supplied offsets before exposing a dictionary to lookups.
  // The file is capped by the downloader, but the parser must not rely on that cap
  // for memory safety: malformed offsets can still point outside the byte array.
  private static void requireRange(byte[] data, int off, int size, String what)
      throws ConstructionError {
    if (off < 0 || size < 0 || off > data.length - size) {
      throw new ConstructionError("Invalid " + what + " bounds");
    }
  }

  private static int packedArraySize(int format, int count) throws ConstructionError {
    if ((format != 1 && format != 2 && format != 4 && format != 6) || count < 0) {
      throw new ConstructionError("Invalid packed integer format");
    }
    return (format * count + 1) / 2;
  }

  private static int readSignedPacked(byte[] data, int format, int i, int off)
      throws ConstructionError {
    int idx = (format * i) / 2;
    if (format == 1) {
      requireRange(data, off + idx, 1, "packed integer");
      int b = data[off + idx] & 0xFF;
      int n = ((i & 1) != 0) ? (b >> 4) : (b & 0x0F);
      return (n & 8) != 0 ? (n | ~0x0F) : n;
    }
    if (format == 2) {
      requireRange(data, off + i, 1, "packed integer");
      return data[off + i];
    }
    if (format == 4) {
      requireRange(data, off + 2 * i, 2, "packed integer");
      return (data[off + 2 * i] << 8) | (data[off + 2 * i + 1] & 0xFF);
    }
    requireRange(data, off + idx, 3, "packed integer");
    return (data[off + idx] << 16)
        | ((data[off + idx + 1] & 0xFF) << 8)
        | (data[off + idx + 2] & 0xFF);
  }

  private static int readInt24Checked(byte[] data, int off) throws ConstructionError {
    requireRange(data, off, 3, "24-bit integer");
    return (data[off] << 16) | ((data[off + 1] & 0xFF) << 8) | (data[off + 2] & 0xFF);
  }

  private static void validateTrie(byte[] data, int root) throws ConstructionError {
    // Iterative traversal avoids attacker-controlled recursion depth and detects cycles.
    java.util.ArrayDeque<Integer> pending = new java.util.ArrayDeque<>();
    java.util.HashSet<Integer> visited = new java.util.HashSet<>();
    pending.add(root);
    while (!pending.isEmpty()) {
      int node = pending.removeFirst();
      if (node < 0 || node >= data.length) {
        throw new ConstructionError("Trie node offset out of bounds");
      }
      if (!visited.add(node)) {
        throw new ConstructionError("Trie contains a cycle or shared node");
      }
      if (visited.size() > data.length / 2 + 1) {
        throw new ConstructionError("Trie node count exceeds file size");
      }
      requireRange(data, node, 2, "trie node header");
      int header = data[node] & 0xFF;
      if ((header & 1) != 0) {
        int prefixLength = header >>> 1;
        requireRange(data, node, 4 + prefixLength, "trie prefix node");
        int next = readInt24Checked(data, node + 1);
        int relative = next & ~1;
        if (relative == 0) {
          if ((next & 1) == 0) {
            throw new ConstructionError("Invalid zero trie pointer");
          }
          continue; // A final-word marker can point to the current node.
        }
        long target = (long) node + relative;
        if (target < 0 || target >= data.length) {
          throw new ConstructionError("Trie prefix pointer out of bounds");
        }
        pending.add((int) target);
      } else {
        int count = data[node + 1] & 0xFF;
        // Empty nodes are valid terminal leaves; the parent pointer carries
        // the final-word bit, so no child arrays are needed here.
        if (count == 0) continue;
        int branchFormat = (header >>> 1) & 0x07;
        int numberFormat = (header >>> 4) & 0x07;
        int branchBytes = packedArraySize(branchFormat, count);
        int numberBytes = packedArraySize(numberFormat, count);
        int labels = node + 2;
        int branches = labels + count;
        int numbers = branches + branchBytes;
        requireRange(data, labels, count, "trie labels");
        requireRange(data, branches, branchBytes, "trie branch pointers");
        requireRange(data, numbers, numberBytes, "trie branch numbers");
        for (int i = 0; i < count; i++) {
          int rawPointer = readSignedPacked(data, branchFormat, i, branches);
          int relative = rawPointer & ~1;
          if (relative == 0) {
            if ((rawPointer & 1) != 0) continue;
            throw new ConstructionError("Invalid zero trie branch pointer");
          }
          long target = (long) node + relative;
          if (target < 0 || target >= data.length) {
            throw new ConstructionError("Trie branch pointer out of bounds");
          }
          pending.add((int) target);
        }
      }
    }
  }

  private static void validateDictionary(byte[] data, int tableEnd, int nameOffset,
      int rootOffset, int frequencyOffset, int aliasesHeader, int aliasesLength,
      int aliasesKeys, int aliasesValues) throws ConstructionError {
    if (nameOffset < tableEnd || nameOffset >= data.length) {
      throw new ConstructionError("Dictionary name offset out of bounds");
    }
    int nameEnd = nameOffset;
    while (nameEnd < data.length && data[nameEnd] != 0) nameEnd++;
    if (nameEnd == data.length) {
      throw new ConstructionError("Dictionary name is not NUL-terminated");
    }
    if (rootOffset < tableEnd || rootOffset >= data.length) {
      throw new ConstructionError("Trie root offset out of bounds");
    }
    if (frequencyOffset < tableEnd || frequencyOffset >= data.length) {
      throw new ConstructionError("Frequency table offset out of bounds");
    }
    if (aliasesLength < 0 || aliasesLength > 0xFFFFFF) {
      throw new ConstructionError("Invalid alias table length");
    }
    if (aliasesLength > 0) {
      int keyFormat = aliasesHeader & 0x07;
      int valueFormat = (aliasesHeader >>> 3) & 0x07;
      int keyBytes = packedArraySize(keyFormat, aliasesLength);
      int valueBytes = packedArraySize(valueFormat, aliasesLength);
      requireRange(data, aliasesKeys, keyBytes, "alias keys");
      requireRange(data, aliasesValues, valueBytes, "alias values");
    }
    validateTrie(data, rootOffset);
  }

  public static Cdict[] of_bytes(byte[] data) throws ConstructionError {
    if (data == null || data.length < 5) {
      throw new ConstructionError("Invalid dictionary file: too small");
    }
    if (data[0] != 'D' || data[1] != 'i' || data[2] != 'c') {
      throw new ConstructionError("Invalid dictionary magic");
    }
    int version = data[3] & 0xFF;
    if (version != 1) {
      throw new ConstructionError("Unsupported dictionary format version: " + version);
    }
    int dictCount = data[4] & 0xFF;
    long tableEndLong = 5L + 24L * dictCount;
    if (dictCount == 0 || tableEndLong > data.length) {
      throw new ConstructionError("Invalid dictionary table length");
    }
    int tableEnd = (int) tableEndLong;
    Cdict[] dicts = new Cdict[dictCount];
    int off = 5;
    for (int i = 0; i < dictCount; i++) {
      if (off + 24 > data.length) {
        throw new ConstructionError("Truncated dictionary header");
      }
      int name_off = decode_int32(data, off);
      int root_ptr = decode_int32(data, off + 4);
      int freq_off = decode_int32(data, off + 8);
      int aliases_header = data[off + 12] & 0xFF;
      int aliases_length = decode_int24(data, off + 13);
      int aliases_keys = decode_int32(data, off + 16);
      int aliases_values = decode_int32(data, off + 20);

      validateDictionary(data, tableEnd, name_off, root_ptr, freq_off,
          aliases_header, aliases_length, aliases_keys, aliases_values);
      int nameEnd = name_off;
      while (nameEnd < data.length && data[nameEnd] != 0) {
        nameEnd++;
      }
      String dictName = new String(data, name_off, nameEnd - name_off, StandardCharsets.ISO_8859_1);
      dicts[i] = new Cdict(dictName, data, root_ptr, freq_off,
          aliases_header, aliases_length, aliases_keys, aliases_values);
      off += 24;
    }
    return dicts;
  }

  public Result find(String word) {
    if (word == null || word.isEmpty()) {
      return new Result(false, 0, 0, 0);
    }
    byte[] wBytes = word.getBytes(StandardCharsets.UTF_8);
    Result r = find_node(_root_node, 0, wBytes, 0, wBytes.length, 0);
    if (r.found) {
      int alias = resolve_alias(r.index);
      if (alias >= 0) {
        return new Result(true, alias, r.prefix_ptr, r.original_index);
      }
    }
    return r;
  }

  public int freq(int index) {
    if (index < 0) return 0;
    int pos = _freq_off + (index / 2);
    if (pos >= _data.length) return 0;
    int f = _data[pos] & 0xFF;
    if ((index & 1) != 0) {
      f = f >> 4;
    }
    return f & 0x0F;
  }

  public String word(int index) {
    if (index < 0) return "";
    byte[] buf = new byte[128];
    int len = word_node(_root_node, 0, index, buf, 0, buf.length);
    if (len <= 0) return "";
    return new String(buf, 0, len, StandardCharsets.UTF_8);
  }

  public int[] suffixes(Result result, int count) {
    if (result == null || result.prefix_ptr == 0 || count <= 0) {
      return new int[0];
    }
    int parent = (int) (result.prefix_ptr >>> 32);
    int ptr = (int) result.prefix_ptr;
    List<WordFreq> list = new ArrayList<>();
    collect_suffixes(parent, ptr, result.original_index, list);
    Collections.sort(list, WORD_FREQ_COMPARATOR);
    int n = Math.min(count, list.size());
    int[] out = new int[n];
    for (int i = 0; i < n; i++) {
      out[i] = list.get(i).index;
    }
    return out;
  }

  public int[] distance(String word, int dist, int count) {
    if (word == null || word.isEmpty() || count <= 0) {
      return new int[0];
    }
    byte[] wBytes = word.getBytes(StandardCharsets.UTF_8);
    List<WordFreq> list = new ArrayList<>();
    collect_distance(_root_node, 0, wBytes, 0, wBytes.length, 0, dist, list);
    Collections.sort(list, WORD_FREQ_COMPARATOR);
    int n = Math.min(count, list.size());
    int[] out = new int[n];
    for (int i = 0; i < n; i++) {
      out[i] = list.get(i).index;
    }
    return out;
  }

  private static final class WordFreq {
    final int freq;
    final int index;

    WordFreq(int freq, int index) {
      this.freq = freq;
      this.index = index;
    }
  }

  private static final Comparator<WordFreq> WORD_FREQ_COMPARATOR = (a, b) -> {
    int d = b.freq - a.freq;
    if (d != 0) return d;
    return a.index - b.index;
  };

  private void add_resolved_word(List<WordFreq> dst, int index) {
    int alias = resolve_alias(index);
    if (alias >= 0) {
      index = alias;
    }
    for (WordFreq wf : dst) {
      if (wf.index == index) return;
    }
    dst.add(new WordFreq(freq(index), index));
  }

  private void collect_suffixes(int parent_node, int ptr, int index, List<WordFreq> dst) {
    int node = parent_node + (ptr & ~1);
    if ((ptr & 1) != 0) {
      add_resolved_word(dst, index);
      index++;
    }
    int kind = _data[node] & 1;
    if (kind == 0) { // BRANCHES
      int length = _data[node + 1] & 0xFF;
      for (int i = 0; i < length; i++) {
        collect_suffixes(node, branch(node, i), index + branch_number(node, i), dst);
      }
    } else { // PREFIX
      int next_ptr = decode_int24(_data, node + 1);
      collect_suffixes(node, next_ptr, index, dst);
    }
  }

  private void collect_distance(int parent_node, int ptr, byte[] word, int word_idx,
                                int word_end, int index, int dist, List<WordFreq> dst) {
    if (dist == 0) {
      Result r = find_node(parent_node, ptr, word, word_idx, word_end, index);
      if (r.found) {
        add_resolved_word(dst, r.index);
      }
      return;
    }
    int node = parent_node + (ptr & ~1);
    if ((ptr & 1) != 0) {
      index++;
    }
    if (word_idx == word_end) {
      if (dist == 1) {
        collect_suffixes(node, 0, index, dst);
      }
      return;
    }
    int kind = _data[node] & 1;
    if (kind == 0) { // BRANCHES
      collect_distance(node, 0, word, word_idx + 1, word_end, index, dist - 1, dst);
      int length = _data[node + 1] & 0xFF;
      int c = word[word_idx] & 0xFF;
      for (int i = 0; i < length; i++) {
        int next_idx = index + branch_number(node, i);
        int bi = branch(node, i);
        int label = _data[node + 2 + i] & 0xFF;
        if (c == label) {
          collect_distance(node, bi, word, word_idx + 1, word_end, next_idx, dist, dst);
        } else {
          collect_distance(node, bi, word, word_idx + 1, word_end, next_idx, dist - 1, dst);
          collect_distance(node, bi, word, word_idx, word_end, next_idx, dist - 1, dst);
        }
      }
    } else { // PREFIX
      int p_len = (_data[node] & 0xFF) >> 1;
      int next_ptr = decode_int24(_data, node + 1);
      int word_len = word_end - word_idx;
      int common_len = Math.min(word_len, p_len);
      int pdist = levenshtein(word, word_idx, common_len, _data, node + 4, common_len);
      if (word_len < p_len) {
        if (pdist + 1 == dist) {
          collect_suffixes(node, next_ptr, index, dst);
          return;
        }
        pdist += p_len - word_len;
      }
      int next_dist = dist - pdist;
      for (int i = common_len; i >= 0 && next_dist >= 0; i--) {
        collect_distance(node, next_ptr, word, word_idx + i, word_end, index, next_dist, dst);
        next_dist--;
      }
    }
  }

  private static int levenshtein(byte[] a, int a_off, int a_len, byte[] b, int b_off, int b_len) {
    int[] v0 = new int[b_len + 1];
    int[] v1 = new int[b_len + 1];
    for (int i = 0; i <= b_len; i++) v0[i] = i;
    for (int i = 0; i < a_len; i++) {
      v1[0] = i + 1;
      for (int j = 0; j < b_len; j++) {
        int del = v0[j + 1] + 1;
        int ins = v1[j] + 1;
        int rpl = v0[j] + ((a[a_off + i] == b[b_off + j]) ? 0 : 1);
        v1[j + 1] = Math.min(del, Math.min(ins, rpl));
      }
      int[] tmp = v0;
      v0 = v1;
      v1 = tmp;
    }
    return v0[b_len];
  }

  private Result find_node(int parent_node, int ptr, byte[] word, int word_idx, int word_end, int index) {
    boolean is_final = (ptr & 1) != 0;
    int node = parent_node + (ptr & ~1);
    if (word_idx == word_end) {
      long packedPtr = (((long) parent_node) << 32) | (ptr & 0xFFFFFFFFL);
      return new Result(is_final, index, packedPtr, index);
    }
    if (is_final) {
      index++;
    }
    int kind = _data[node] & 1;
    if (kind == 0) { // BRANCHES
      int c = word[word_idx] & 0xFF;
      int length = _data[node + 1] & 0xFF;
      int i = 0;
      while (i < length) {
        int l = _data[node + 2 + i] & 0xFF;
        if (c == l) {
          index += branch_number(node, i);
          return find_node(node, branch(node, i), word, word_idx + 1, word_end, index);
        } else if (c < l) {
          i = i * 2 + 1;
        } else {
          i = i * 2 + 2;
        }
      }
      return new Result(false, -1, 0, -1);
    } else { // PREFIX
      int p_len = (_data[node] & 0xFF) >> 1;
      int next_ptr = decode_int24(_data, node + 1);
      for (int j = 0; j < p_len; j++) {
        if (word_idx + j == word_end) {
          long packedPtr = (((long) node) << 32) | (next_ptr & 0xFFFFFFFFL);
          return new Result(false, index, packedPtr, index);
        }
        if ((word[word_idx + j] & 0xFF) != (_data[node + 4 + j] & 0xFF)) {
          return new Result(false, -1, 0, -1);
        }
      }
      return find_node(node, next_ptr, word, word_idx + p_len, word_end, index);
    }
  }

  private int word_node(int parent_node, int ptr, int index, byte[] dst, int dsti, int max_len) {
    if (dsti >= max_len) return dsti;
    int node = parent_node + (ptr & ~1);
    if ((ptr & 1) != 0) {
      if (index == 0) return dsti;
      index--;
    }
    int kind = _data[node] & 1;
    if (kind == 0) { // BRANCHES
      int length = _data[node + 1] & 0xFF;
      int i = 0;
      int next_b = 0;
      int next_num = 0;
      byte label = 0;
      while (i < length) {
        int ni = branch_number(node, i);
        int bi = branch(node, i);
        if (ni > index) {
          i = i * 2 + 1;
        } else {
          next_b = bi;
          next_num = ni;
          label = _data[node + 2 + i];
          i = i * 2 + 2;
        }
      }
      if (next_b == 0) return dsti;
      dst[dsti] = label;
      return word_node(node, next_b, index - next_num, dst, dsti + 1, max_len);
    } else { // PREFIX
      int p_len = (_data[node] & 0xFF) >> 1;
      int next_ptr = decode_int24(_data, node + 1);
      int end = Math.min(dsti + p_len, max_len);
      int pi = 0;
      while (dsti < end) {
        dst[dsti++] = _data[node + 4 + pi++];
      }
      return word_node(node, next_ptr, index, dst, end, max_len);
    }
  }

  private int branch(int node_offset, int i) {
    int header = _data[node_offset] & 0xFF;
    int length = _data[node_offset + 1] & 0xFF;
    int branches_fmt = (header >> 1) & 0x07;
    int ar_off = node_offset + 2 + length;
    return sized_int_signed(_data, branches_fmt, i, ar_off);
  }

  private int branch_number(int node_offset, int i) {
    int header = _data[node_offset] & 0xFF;
    int length = _data[node_offset + 1] & 0xFF;
    int branches_fmt = (header >> 1) & 0x07;
    int numbers_fmt = (header >> 4) & 0x07;
    int ar_size = ((branches_fmt * length) + 1) >> 1;
    int ar_off = node_offset + 2 + length + ar_size;
    return sized_int_unsigned(_data, numbers_fmt, i, ar_off);
  }

  private int resolve_alias(int index) {
    if (_aliases_length == 0) return -1;
    int k_fmt = _aliases_header & 0x07;
    int v_fmt = (_aliases_header >> 3) & 0x07;
    int i = complete_tree_search_unsigned(_data, _aliases_keys, k_fmt, _aliases_length, index);
    if (i < 0) return -1;
    return sized_int_unsigned(_data, v_fmt, i, _aliases_values);
  }

  private static int complete_tree_search_unsigned(byte[] ar, int off, int fmt, int length, int key) {
    int i = 0;
    while (i < length) {
      int val = sized_int_unsigned(ar, fmt, i, off);
      if (val == key) return i;
      i = i * 2 + ((key < val) ? 1 : 2);
    }
    return -1;
  }

  private static int sized_int_unsigned(byte[] ar, int fmt, int i, int off) {
    if (fmt == 2) {
      return ar[off + i] & 0xFF;
    }
    int idx = (fmt * i) >> 1;
    int ar0 = ar[off + idx] & 0xFF;
    if (fmt == 1) {
      return (i & 1) != 0 ? (ar0 >> 4) : (ar0 & 0x0F);
    }
    if (fmt == 4) {
      return (ar0 << 8) | (ar[off + idx + 1] & 0xFF);
    }
    return (ar0 << 16) | ((ar[off + idx + 1] & 0xFF) << 8) | (ar[off + idx + 2] & 0xFF);
  }

  private static int sized_int_signed(byte[] ar, int fmt, int i, int off) {
    if (fmt == 2) {
      return ar[off + i];
    }
    int idx = (fmt * i) >> 1;
    int ar0 = ar[off + idx];
    if (fmt == 1) {
      if ((i & 1) != 0) return ar0 >> 4;
      return (ar0 & 0x08) != 0 ? (ar0 | ~0x0F) : (ar0 & 0x07);
    }
    if (fmt == 4) {
      return (ar0 << 8) | (ar[off + idx + 1] & 0xFF);
    }
    return (ar0 << 16) | ((ar[off + idx + 1] & 0xFF) << 8) | (ar[off + idx + 2] & 0xFF);
  }

  private static int decode_int24(byte[] ar, int off) {
    return (ar[off] << 16) | ((ar[off + 1] & 0xFF) << 8) | (ar[off + 2] & 0xFF);
  }

  private static int decode_int32(byte[] ar, int off) {
    return (ar[off] << 24) | ((ar[off + 1] & 0xFF) << 16) | ((ar[off + 2] & 0xFF) << 8) | (ar[off + 3] & 0xFF);
  }

  public static class ConstructionError extends Exception {
    public ConstructionError(String msg) {
      super(msg);
    }
  }
}
