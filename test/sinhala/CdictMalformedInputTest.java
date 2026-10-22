package juloo.cdict;

import org.junit.Test;
import static org.junit.Assert.*;

public class CdictMalformedInputTest {
  @Test public void rejectsTruncatedHeader() {
    try { Cdict.of_bytes(new byte[] {'D', 'i', 'c', 1}); fail("must reject"); }
    catch (Cdict.ConstructionError expected) { }
  }
}
