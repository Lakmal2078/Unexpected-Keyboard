package juloo.keyboard2;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class QuickCalculator
{
  private final String str;
  private int pos = -1;
  private int ch;

  private QuickCalculator(String str)
  {
    this.str = str;
  }

  private void nextChar()
  {
    ch = (++pos < str.length()) ? str.charAt(pos) : -1;
  }

  private boolean eat(int charToEat)
  {
    while (ch == ' ') nextChar();
    if (ch == charToEat)
    {
      nextChar();
      return true;
    }
    return false;
  }

  static boolean isFunction(String name)
  {
    switch (name.toLowerCase(Locale.ROOT))
    {
      case "sqrt": case "cbrt": case "abs": case "round":
      case "sin": case "cos": case "tan": case "log": case "ln":
        return true;
      default: return false;
    }
  }

  public static String evaluate(String input)
  {
    if (input == null) return null;
    String s = input.trim();
    if (s.endsWith("="))
      s = s.substring(0, s.length() - 1).trim();
    if (s.length() < 2 || s.length() > 60)
      return null;

    // Must have at least one digit
    boolean hasDigit = false;
    for (int i = 0; i < s.length(); i++)
    {
      if (Character.isDigit(s.charAt(i)))
      {
        hasDigit = true;
        break;
      }
    }
    if (!hasDigit) return null;

    // Normalize unicode math symbols and commas
    s = s.replace('×', '*').replace('÷', '/');
    if (s.contains(","))
    {
      if (s.matches(".*\\d+,\\d{3}(\\D.*|$)") && !s.contains("."))
        s = s.replace(",", "");
      else
        s = s.replace(',', '.');
    }

    // Brackets alone do not make a number a calculation: preserve numbered
    // lists and citations such as (1) and (2023) when space auto-completes.
    String unwrapped = s.replaceAll("[()\\s]", "");
    if (unwrapped.matches("^[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)$"))
      return null;

    // Quick sanity check on allowed characters
    for (int i = 0; i < s.length(); i++)
    {
      char c = s.charAt(i);
      if (!Character.isDigit(c) && !Character.isWhitespace(c)
          && "+-*/^%().,".indexOf(c) == -1
          && !Character.isLetter(c))
      {
        return null;
      }
    }

    try
    {
      QuickCalculator parser = new QuickCalculator(s);
      parser.nextChar();
      double result = parser.parseExpression();
      if (parser.pos < parser.str.length())
        return null; // Did not consume entire expression
      if (Double.isNaN(result) || Double.isInfinite(result))
        return null;

      // Format nicely
      DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
      DecimalFormat df = new DecimalFormat("0.######", symbols);
      df.setGroupingUsed(false);
      String formatted = df.format(result);
      return "= " + formatted;
    }
    catch (Throwable t)
    {
      return null;
    }
  }

  private double parseExpression()
  {
    double x = parseTerm();
    for (;;)
    {
      if (eat('+')) x += parseTerm();
      else if (eat('-')) x -= parseTerm();
      else return x;
    }
  }

  private double parseTerm()
  {
    double x = parseFactor();
    for (;;)
    {
      if (eat('*')) x *= parseFactor();
      else if (eat('/'))
      {
        double divisor = parseFactor();
        if (divisor == 0) throw new ArithmeticException("Division by zero");
        x /= divisor;
      }
      else if (eat('%'))
      {
        // If modulo
        double mod = parseFactor();
        if (mod == 0) throw new ArithmeticException("Division by zero");
        x %= mod;
      }
      else return x;
    }
  }

  private double parseFactor()
  {
    if (eat('+')) return +parseFactor();
    if (eat('-')) return -parseFactor();

    double x;
    int startPos = this.pos;
    if (eat('('))
    {
      x = parseExpression();
      if (!eat(')')) throw new RuntimeException("Missing ')'");
    }
    else if ((ch >= '0' && ch <= '9') || ch == '.')
    {
      while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
      x = Double.parseDouble(str.substring(startPos, this.pos));
    }
    else if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z')
    {
      while (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') nextChar();
      String func = str.substring(startPos, this.pos).toLowerCase(Locale.ROOT);
      if (func.equals("pi"))
      {
        x = Math.PI;
      }
      else if (func.equals("e"))
      {
        x = Math.E;
      }
      else
      {
        if (!eat('(')) throw new RuntimeException("Expected '(' after function: " + func);
        double arg = parseExpression();
        if (!eat(')')) throw new RuntimeException("Missing ')' after function: " + func);
        switch (func)
        {
          case "sqrt": x = Math.sqrt(arg); break;
          case "cbrt": x = Math.cbrt(arg); break;
          case "abs": x = Math.abs(arg); break;
          case "round": x = Math.round(arg); break;
          case "sin": x = Math.sin(Math.toRadians(arg)); break;
          case "cos": x = Math.cos(Math.toRadians(arg)); break;
          case "tan": x = Math.tan(Math.toRadians(arg)); break;
          case "log": x = Math.log10(arg); break;
          case "ln": x = Math.log(arg); break;
          default: throw new RuntimeException("Unknown function: " + func);
        }
      }
    }
    else
    {
      throw new RuntimeException("Unexpected character: " + (char)ch);
    }

    // Postfix percent: e.g. 15% -> 0.15, or 200 * 15%
    // Only if NOT followed by an operand (which would make it modulo: 17 % 6)
    if (ch == '%')
    {
      int nextIdx = pos + 1;
      while (nextIdx < str.length() && str.charAt(nextIdx) == ' ')
        nextIdx++;
      boolean hasOperandAfter = false;
      if (nextIdx < str.length())
      {
        char cNext = str.charAt(nextIdx);
        if (Character.isDigit(cNext) || Character.isLetter(cNext) || cNext == '(' || cNext == '.')
          hasOperandAfter = true;
      }
      if (!hasOperandAfter)
      {
        eat('%');
        x = x / 100.0;
      }
    }

    // Power
    if (eat('^'))
    {
      x = Math.pow(x, parseFactor());
    }

    return x;
  }
}
