package juloo.cdict;

import java.util.ArrayList;
import java.util.List;

public class Cdict {
  public static final class Result {
    /** Whether the word is recognized. */
    public final boolean found;
    /** Unique index of the word within the dictionary. */
    public final int index;
    /** Internal pointer used by the suffixes function. */
    public final long prefix_ptr;
    /** Index before alias resolution. Used internally. */
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

  /** Dictionary name. */
  public final String name;
  private final List<String> _words = new ArrayList<>();

  public Cdict(String name) {
    this.name = name;
  }

  /** Load a dictionary file stored in a byte array. */
  public static Cdict[] of_bytes(byte[] data) throws ConstructionError {
    Cdict mainDict = new Cdict("main");
    Cdict emojiDict = new Cdict("emoji");
    return new Cdict[] { mainDict, emojiDict };
  }

  /** Check whether the given word is recognized by the dictionary. Never returns null. */
  public Result find(String word) {
    int idx = _words.indexOf(word);
    if (idx >= 0) {
      return new Result(true, idx);
    }
    return new Result(false, -1);
  }

  /** Lookup the frequency of a word. */
  public int freq(int index) {
    return 1;
  }

  /** Lookup the word at a given index. */
  public String word(int index) {
    if (index >= 0 && index < _words.size()) {
      return _words.get(index);
    }
    return "";
  }

  /** List words that start with the query passed to [find]. */
  public int[] suffixes(Result result, int count) {
    return new int[0];
  }

  /** Levenshtein distance query. */
  public int[] distance(String word, int distance, int count) {
    return new int[0];
  }

  /** Version of the dictionary's format. */
  public static int format_version() {
    return 1;
  }

  /** Thrown during construction. */
  public static class ConstructionError extends Exception {
    public ConstructionError(String msg) {
      super(msg);
    }
  }
}
