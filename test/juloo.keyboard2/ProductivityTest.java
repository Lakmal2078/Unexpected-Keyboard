package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProductivityTest
{
  @Test
  public void testQuickCalculatorBasicOperations()
  {
    assertEquals("= 4", QuickCalculator.evaluate("2+2"));
    assertEquals("= 4", QuickCalculator.evaluate("2+2="));
    assertEquals("= 600", QuickCalculator.evaluate("150*4"));
    assertEquals("= 75", QuickCalculator.evaluate("100-25"));
    assertEquals("= 25", QuickCalculator.evaluate("100/4"));
    assertEquals("= 25", QuickCalculator.evaluate("100÷4"));
    assertEquals("= 600", QuickCalculator.evaluate("150×4"));
  }

  @Test
  public void testQuickCalculatorPrecedenceAndParentheses()
  {
    assertEquals("= 14", QuickCalculator.evaluate("2+3*4"));
    assertEquals("= 20", QuickCalculator.evaluate("(2+3)*4"));
    assertEquals("= 256", QuickCalculator.evaluate("2^8"));
    assertEquals("= 5", QuickCalculator.evaluate("17%6"));
  }

  @Test
  public void testQuickCalculatorDecimalsAndPercent()
  {
    assertEquals("= 10", QuickCalculator.evaluate("2.5*4"));
    assertEquals("= 12.35", QuickCalculator.evaluate("10+2.35"));
    assertEquals("= 30", QuickCalculator.evaluate("200*15%"));
    assertEquals("= 0.25", QuickCalculator.evaluate("25%"));
    assertEquals("= 0.25", QuickCalculator.evaluate("(25%)"));
  }

  @Test
  public void testQuickCalculatorFunctions()
  {
    assertEquals("= 12", QuickCalculator.evaluate("sqrt(144)"));
    assertEquals("= 5", QuickCalculator.evaluate("abs(-5)"));
    assertEquals("= 4", QuickCalculator.evaluate("round(3.7)"));
    assertEquals("= 3", QuickCalculator.evaluate("cbrt(27)"));
  }

  @Test
  public void testQuickCalculatorInvalidInputs()
  {
    // Bare numbers should not evaluate
    assertNull(QuickCalculator.evaluate("150"));
    assertNull(QuickCalculator.evaluate("+150"));
    assertNull(QuickCalculator.evaluate("-150"));
    assertNull(QuickCalculator.evaluate("3.1415"));
    for (String literal : new String[] { "(1)", "(2023)", "(-5)", "((2.5))", "(.5)", "(5.)" })
      assertNull(literal, QuickCalculator.evaluate(literal));

    // Incomplete or non-math expressions
    assertNull(QuickCalculator.evaluate("12+"));
    assertNull(QuickCalculator.evaluate("hello world"));
    assertNull(QuickCalculator.evaluate("abc+def"));
    assertNull(QuickCalculator.evaluate("1/0")); // Division by zero
    assertNull(QuickCalculator.evaluate(""));
    assertNull(QuickCalculator.evaluate(null));
  }

  @Test
  public void testSmartSnippets()
  {
    assertEquals("¯\\_(ツ)_/¯", SmartSnippets.getSnippet("//shrug"));
    assertEquals("( ͡° ͜ʖ ͡°)", SmartSnippets.getSnippet("//lenny"));
    assertEquals("සිංහල", SmartSnippets.getSnippet("//sinhala"));
    assertEquals("ශ්‍රී ලංකා", SmartSnippets.getSnippet("//lk"));
    assertEquals("ආයුබෝවන්!", SmartSnippets.getSnippet("//ayubowan"));

    // Dynamic date and time
    assertNotNull(SmartSnippets.getSnippet("//date"));
    assertNotNull(SmartSnippets.getSnippet("//time"));
    assertNotNull(SmartSnippets.getSnippet("//datetime"));

    // Dot prefix support
    assertEquals("¯\\_(ツ)_/¯", SmartSnippets.getSnippet("..shrug"));

    // Unknown snippet
    assertNull(SmartSnippets.getSnippet("//nonexistent"));
  }

  @Test
  public void testQuickCalculatorCommas()
  {
    assertEquals("= 2000", QuickCalculator.evaluate("1,000*2"));
    assertEquals("= 10", QuickCalculator.evaluate("2,5*4"));
  }

  @Test
  public void testCurrentlyTypedWordBrackets()
  {
    assertTrue(CurrentlyTypedWord.is_word_char('('));
    assertTrue(CurrentlyTypedWord.is_word_char(')'));
    assertTrue(CurrentlyTypedWord.is_word_char('a'));
    assertTrue(CurrentlyTypedWord.is_word_char('1'));
  }
}
