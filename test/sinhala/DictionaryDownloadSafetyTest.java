package juloo.keyboard2.dict;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class DictionaryDownloadSafetyTest
{
  @Test
  public void acceptsDictionaryBytesWithinLimit() throws Exception
  {
    byte[] expected = "dict".getBytes(StandardCharsets.UTF_8);
    assertArrayEquals(expected, DictionaryListView.read_bounded(
        new ByteArrayInputStream(expected), expected.length));
  }

  @Test
  public void rejectsDictionaryBytesOverLimit()
  {
    byte[] input = "1234".getBytes(StandardCharsets.UTF_8);
    try
    {
      DictionaryListView.read_bounded(new ByteArrayInputStream(input), 3);
      fail("Expected a dictionary-size IOException");
    }
    catch (IOException expected)
    {
      assertTrue(expected.getMessage().contains("maximum size"));
    }
  }
}
