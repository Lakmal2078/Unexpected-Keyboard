package juloo.cdict;

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class CdictMalformedInputTest {
  private static byte[] header(int nameOffset, int rootOffset, int frequencyOffset) {
    byte[] data = new byte[64];
    data[0] = 'D'; data[1] = 'i'; data[2] = 'c'; data[3] = 1; data[4] = 1;
    putInt(data, 5, nameOffset);
    putInt(data, 9, rootOffset);
    putInt(data, 13, frequencyOffset);
    data[17] = 0;
    data[18] = data[19] = data[20] = 0;
    putInt(data, 21, 0); putInt(data, 25, 0);
    if (nameOffset >= 0 && nameOffset + 1 < data.length) {
      data[nameOffset] = 'x'; data[nameOffset + 1] = 0;
    }
    return data;
  }

  private static void putInt(byte[] data, int off, int value) {
    data[off] = (byte)(value >>> 24);
    data[off + 1] = (byte)(value >>> 16);
    data[off + 2] = (byte)(value >>> 8);
    data[off + 3] = (byte)value;
  }

  private static void assertRejected(byte[] input) {
    try { Cdict.of_bytes(input); fail("Malformed dictionary must be rejected"); }
    catch (Cdict.ConstructionError expected) { }
  }

  @Test public void rejectsTruncatedAndInvalidHeaders() {
    assertRejected(new byte[0]);
    assertRejected(new byte[] {'D', 'i', 'c', 1});
    assertRejected(new byte[] {'D', 'i', 'c', 2, 1});
    assertRejected(header(-1, 32, 60));
    assertRejected(header(62, 1000, 60));
    assertRejected(header(62, 32, 1000));
  }

  @Test public void rejectsUnterminatedDictionaryName() {
    byte[] data = header(63, 32, 60);
    data[63] = 'x';
    assertRejected(data);
  }

  @Test public void rejectsOutOfBoundsTrieChildPointer() {
    byte[] data = header(62, 32, 60);
    data[32] = (byte)(2 | (2 << 4));
    data[33] = 1;
    data[34] = 'a';
    data[35] = 100;
    data[36] = 0;
    assertRejected(data);
  }

  @Test public void deterministicMalformedHeaderFuzzIsRejected() {
    Random random = new Random(0x51A6A);
    for (int i = 0; i < 500; i++) {
      byte[] data = header(29, 32, 60);
      random.nextBytes(data);
      data[0] = 'D'; data[1] = 'i'; data[2] = 'c'; data[3] = 1; data[4] = 1;
      putInt(data, 5, -1);
      putInt(data, 9, 32);
      putInt(data, 13, 60);
      assertRejected(data);
    }
  }
}
