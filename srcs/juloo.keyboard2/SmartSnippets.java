package juloo.keyboard2;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class SmartSnippets
{
  public static String getSnippet(String trigger)
  {
    if (trigger == null || trigger.isEmpty())
      return null;

    String lower = trigger.toLowerCase(Locale.ROOT);

    // Dynamic Date & Time snippets
    if (lower.equals("//date") || lower.equals("..date"))
    {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
      return sdf.format(new Date());
    }
    if (lower.equals("//time") || lower.equals("..time"))
    {
      SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
      return sdf.format(new Date());
    }
    if (lower.equals("//datetime") || lower.equals("..datetime"))
    {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
      return sdf.format(new Date());
    }

    // Popular Text Emoticons & Kaomoji
    if (lower.equals("//shrug") || lower.equals("..shrug")) return "¯\\_(ツ)_/¯";
    if (lower.equals("//lenny") || lower.equals("..lenny")) return "( ͡° ͜ʖ ͡°)";
    if (lower.equals("//disapproval") || lower.equals("..disapproval")) return "ಠ_ಠ";
    if (lower.equals("//tableflip") || lower.equals("..tableflip")) return "(╯°□°)╯︵ ┻━┻";
    if (lower.equals("//happy") || lower.equals("..happy")) return "(^‿^)";
    if (lower.equals("//peace") || lower.equals("..peace")) return "✌️";
    if (lower.equals("//heart") || lower.equals("..heart")) return "❤️";

    // Sinhala Quick Phrases
    if (lower.equals("//sinhala") || lower.equals("..sinhala")) return "සිංහල";
    if (lower.equals("//lk") || lower.equals("..lk")) return "ශ්‍රී ලංකා";
    if (lower.equals("//ayubowan") || lower.equals("..ayubowan")) return "ආයුබෝවන්!";
    if (lower.equals("//sthuthi") || lower.equals("..sthuthi")) return "ස්තූතියි!";

    return null;
  }
}
