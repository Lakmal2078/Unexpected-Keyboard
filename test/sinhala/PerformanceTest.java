package juloo.keyboard2;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PerformanceTest
{
  @Test
  public void repeatedPrefixLookupsRemainStable()
  {
    List<String> expected = SinhalaTransliterator.getSuggestions("ka");
    assertFalse(expected.isEmpty());

    long startedAt = System.nanoTime();
    for (int i = 0; i < 1_000; i++)
    {
      assertEquals(expected, SinhalaTransliterator.getSuggestions("ka"));
    }
    long elapsedNanos = System.nanoTime() - startedAt;

    // Generous smoke-test ceiling to catch accidental unbounded work, not a microbenchmark.
    assertTrue("1,000 prefix lookups should finish within 15 seconds",
        elapsedNanos < 15_000_000_000L);
  }
}
