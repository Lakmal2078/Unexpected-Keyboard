package juloo.keyboard2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Context-aware n-gram prediction engine for Sinhala input.
 * Supports trigram sentence continuations, high-frequency static bigrams,
 * and user-personalized dynamic predictions from SinhalaLearner.
 */
public final class SinhalaPredictionEngine
{
  public static final int MAX_PREDICTIONS = 3;

  private static final Map<String, List<String>> TRIGRAM_MAP = new HashMap<>();
  private static final Map<String, List<String>> BIGRAM_MAP = new HashMap<>();

  static
  {
    // ==========================================
    // 1. HIGH-FREQUENCY TRIGRAM CONTEXTS
    // ==========================================
    putTrigram("මම ගෙදර", "යනවා", "ආවා", "ඉන්නේ", "නැහැ");
    putTrigram("මම ඔයාට", "ආදරෙයි", "කතා කරන්නම්", "කියන්නම්", "එවන්නම්");
    putTrigram("මම අද", "එන්නේ නැහැ", "කාර්යාලයේ", "වැඩ", "නිවාඩු");
    putTrigram("මම හෙට", "එන්නම්", "යන්නම්", "කතා කරන්නම්");
    putTrigram("මම දැන්", "යනවා", "වැඩ", "කනවා", "එනවා");
    putTrigram("ඔයා ගෙදර", "ආවද", "ඉන්නවද", "ගියාද", "යනවද");
    putTrigram("ඔයා කොහෙද", "ඉන්නේ", "යන්නේ", "ගියේ");
    putTrigram("ඔයාට කොහොමද", "යාලුවා", "දැන්", "විස්තර");
    putTrigram("ඔයා මොකද", "කරන්නේ", "හිතන්නේ", "කියන්නේ");
    putTrigram("අපි හෙට", "හමුවෙමු", "යමු", "කතා කරමු");
    putTrigram("අපි දැන්", "යමු", "කමු", "කරමු");
    putTrigram("සුබ අලුත්", "අවුරුද්දක් වේවා", "වසරක් වේවා", "දවසක්");
    putTrigram("සුබ උපන්", "දිනයක් වේවා", "දවසක් වේවා");
    putTrigram("සුබ උදෑසනක්", "වේවා", "යාලුවා", "ප්‍රාර්ථනා කරනවා");
    putTrigram("සුබ රාත්‍රියක්", "වේවා", "යාලුවා");
    putTrigram("බොහොම ස්තූතියි", "ඔබට", "යාලුවා", "සියලු දෙනාටම");
    putTrigram("තෙරුවන් සරණයි", "ඔබට", "ප්‍රාර්ථනා කරනවා", "සියලු දෙනාටම");
    putTrigram("බුදු සරණයි", "ඔබට", "දෙවි පිහිටයි");
    putTrigram("ගොඩක් පින්", "ඔබට", "සිද්ධ වේවා");
    putTrigram("මොකක්ද මේ", "ප්‍රශ්නය", "සිද්ධිය", "කරන්නේ");
    putTrigram("මොකද වෙන්නේ", "යාලුවා", "අද", "දැන්");
    putTrigram("වැඩ ඉවර", "වෙලා", "කරන්න", "දැන්");
    putTrigram("කෑම කෑවද", "ඔයා", "දැන්", "අද");
    putTrigram("පරිස්සමෙන් ගෙදර", "යන්න", "එන්න");
    putTrigram("කරුණාකර මට", "උදව් කරන්න", "කියන්න", "දෙන්න");
    putTrigram("මට දැන්", "තේරෙනවා", "පුළුවන්", "ඕනේ");
    putTrigram("මට උදව්", "කරන්න", "ඕනේ", "පුළුවන්ද");
    putTrigram("එයා ආවේ", "නැහැ", "කවද්ද", "ඊයේ");

    // ==========================================
    // 2. CURATED HIGH-FREQUENCY BIGRAM CONTEXTS
    // ==========================================
    // Greetings & Expressions
    putBigram("සුබ", "උදෑසනක්", "දවසක්", "රාත්‍රියක්", "අලුත්");
    putBigram("ස්තූතියි", "බොහොම", "ඔබට", "යාලුවා", "හැමෝටම");
    putBigram("ආයුබෝවන්", "ඔබට", "සියලු දෙනාටම", "සුබ", "මිත්‍රයා");
    putBigram("තෙරුවන්", "සරණයි", "ආශිර්වාදය", "පිහිටයි");
    putBigram("බුදු", "සරණයි", "දහම්", "ගුණ", "පිහිටයි");
    putBigram("දෙවි", "පිහිටයි", "ආශිර්වාදය");
    putBigram("ජය", "වේවා", "නියතයි", "ගමු", "මඟුල්");
    putBigram("සාදු", "සාදු", "කාරුණික");
    putBigram("ගොඩක්", "ස්තූතියි", "හොඳයි", "පින්", "ආදරෙයි");
    putBigram("පරිස්සමෙන්", "යන්න", "ඉන්න", "එන්න", "ගෙදර");
    putBigram("කරුණාකර", "සවන් දෙන්න", "පැමිණෙන්න", "බලන්න", "මට");
    putBigram("සමාවෙන්න", "මට", "යාලුවා", "පමා වීමට");

    // Pronouns & Agents
    putBigram("මම", "යනවා", "කරන්නම්", "දන්නවා", "ගෙදර", "ඔයාට");
    putBigram("ඔයා", "කොහොමද", "මොකද", "එනවද", "ගෙදර", "දන්නවද");
    putBigram("අපි", "යමු", "කරමු", "බලමු", "හෙට", "කමු");
    putBigram("එයා", "ආවා", "ගියා", "එනවද", "කිව්වා");
    putBigram("ඔබට", "ස්තූතියි", "සුබ", "තෙරුවන් සරණයි", "සුබ පැතුම්");
    putBigram("ඔයාලා", "එනවද", "කොහෙද", "මොකද කරන්නේ");
    putBigram("මේක", "හරි", "හොඳයි", "ලස්සනයි", "මගේ");
    putBigram("ඒක", "හරි", "ඇත්ත", "තමයි", "ලස්සනයි");
    putBigram("මගේ", "අම්මා", "යාලුවා", "රට", "නම", "පවුල");
    putBigram("ඔයාගේ", "නම", "ගම", "වැඩ", "විස්තර");

    // Interrogatives & Question Words
    putBigram("කොහොමද", "ඔයාට", "යාලුවා", "විස්තර", "දැන්", "වැඩ");
    putBigram("මොකද", "කරන්නේ", "විස්තර", "හිතන්නේ", "වෙන්නේ");
    putBigram("කොහෙද", "යන්නේ", "ඉන්නේ", "ගියේ", "තියෙන්නේ");
    putBigram("කවද්ද", "එන්නේ", "යන්නේ", "හමුවෙන්නේ", "දෙන්නේ");
    putBigram("මොනවද", "විස්තර", "කරන්නේ", "ඕන", "කන්නේ");
    putBigram("මොකක්ද", "ප්‍රශ්නය", "වෙන්නේ", "කරන්නේ", "මේ");
    putBigram("ඇයි", "එහෙම", "නැත්තේ", "කියන්නේ", "ආවේ");
    putBigram("කවුද", "ආවේ", "කිව්වේ", "එන්නේ", "ඉන්නේ");
    putBigram("කීයද", "වෙලාව", "ගාන", "මිල");

    // Common Verbs & Actions
    putBigram("එන්න", "පුළුවන්ද", "කෝ", "ගෙදර", "හෙට");
    putBigram("යන්න", "ඕනේ", "කෝ", "එපා", "පුළුවන්ද");
    putBigram("කන්න", "ඕනේ", "යමු", "මොනවද", "පුළුවන්ද");
    putBigram("බොන්න", "ඕනේ", "යමු", "මොනවද", "තේ");
    putBigram("බලන්න", "ඕනේ", "යමු", "මොනවද", "පුළුවන්ද");
    putBigram("කරන්න", "පුළුවන්ද", "ඕනේ", "එපා", "යමු");
    putBigram("ගන්න", "ඕනේ", "පුළුවන්ද", "එපා", "යන්න");
    putBigram("දෙන්න", "පුළුවන්ද", "කෝ", "එපා", "මට");
    putBigram("කතා", "කරන්න", "කරමු", "කරලා", "කළා");
    putBigram("වැඩ", "ඉවරද", "කොහොමද", "කරන්න", "නැහැ");
    putBigram("ගෙදර", "යනවා", "ආවා", "ඉන්නේ", "ගියා");
    putBigram("නිදා", "ගන්න", "ගන්නවා", "වැටුණා");

    // Time, Modifiers & Adverbs
    putBigram("දැන්", "මොකද", "යමු", "හරි", "එන්න", "ඕනේ");
    putBigram("අද", "දවසේ", "මොකද", "යනවද", "හවස", "රෑ");
    putBigram("හෙට", "හමුවෙමු", "එන්නම්", "යමු", "උදේ");
    putBigram("ඊයේ", "රෑ", "හවස", "මොකද", "ආවා");
    putBigram("උදේ", "කෑවද", "නැගිට්ටා", "ආවා", "තේ");
    putBigram("රෑ", "වෙලා", "කෑම", "නිදාගන්න", "හමුවෙමු");
    putBigram("හරි", "මචං", "යාලුවා", "එහෙනම්", "දැන්");
    putBigram("පොඩ්ඩක්", "ඉන්න", "බලන්න", "අහන්න", "දෙන්න");
    putBigram("ඉක්මනට", "එන්න", "යන්න", "කරන්න", "කියන්න");
    putBigram("ලංකා", "දීප", "බැංකුව", "මාතාව", "රජය");
    putBigram("ශ්‍රී", "ලංකා", "ලංකාව", "සුභ");
    putBigram("අලුත්", "අවුරුද්දක්", "වැඩක්", "දවසක්", "පුවත්");
    putBigram("ලස්සන", "දවසක්", "මලක්", "ගීතයක්", "රූපයක්");
    putBigram("හොඳ", "දවසක්", "යාලුවෙක්", "වැඩක්", "ළමයෙක්");
  }

  private static void putTrigram(String context, String... predictions)
  {
    TRIGRAM_MAP.put(context.trim(), Arrays.asList(predictions));
  }

  private static void putBigram(String word, String... predictions)
  {
    BIGRAM_MAP.put(word.trim(), Arrays.asList(predictions));
  }

  /**
   * Retrieves context-aware next-word predictions based on the preceding text context.
   * Priority order:
   * 1. Dynamic learner predictions for the most recent word
   * 2. Trigram continuations based on the last two words
   * 3. Static high-frequency bigram predictions for the last word
   * 4. Natural conversational fallback openers
   */
  public static List<String> getPredictions(String precedingText, SinhalaLearner learner)
  {
    List<String> results = new ArrayList<>(MAX_PREDICTIONS);
    if (precedingText == null || precedingText.trim().isEmpty())
    {
      // Default conversational openers
      results.add("මම");
      results.add("ඔයා");
      results.add("සුබ");
      return results;
    }

    String[] tokens = extractRecentTokens(precedingText);
    if (tokens.length == 0)
    {
      results.add("මම");
      results.add("ඔයා");
      results.add("සුබ");
      return results;
    }

    String lastToken = tokens[tokens.length - 1];
    String secondLastToken = (tokens.length >= 2) ? tokens[tokens.length - 2] : null;

    // 1. Check user-learned dynamic predictions for the last word
    if (learner != null)
    {
      List<String> learned = learner.getLearnedPredictions(lastToken);
      if (learned != null)
      {
        for (String word : learned)
        {
          if (!results.contains(word))
          {
            results.add(word);
            if (results.size() >= MAX_PREDICTIONS)
              return results;
          }
        }
      }
    }

    // 2. Check 2-word trigram context if available
    if (secondLastToken != null)
    {
      String trigramKey = secondLastToken + " " + lastToken;
      List<String> trigramList = TRIGRAM_MAP.get(trigramKey);
      if (trigramList != null)
      {
        for (String word : trigramList)
        {
          if (!results.contains(word))
          {
            results.add(word);
            if (results.size() >= MAX_PREDICTIONS)
              return results;
          }
        }
      }
    }

    // 3. Check curated bigram map for last word
    List<String> bigramList = BIGRAM_MAP.get(lastToken);
    if (bigramList != null)
    {
      for (String word : bigramList)
      {
        if (!results.contains(word))
        {
          results.add(word);
          if (results.size() >= MAX_PREDICTIONS)
            return results;
        }
      }
    }

    // 4. Fill remaining slots with natural conversational continuations if needed
    String[] fallbacks = { "කරන්න", "යන්න", "පුළුවන්ද" };
    for (String fb : fallbacks)
    {
      if (!results.contains(fb) && !fb.equals(lastToken))
      {
        results.add(fb);
        if (results.size() >= MAX_PREDICTIONS)
          break;
      }
    }

    return results;
  }

  /**
   * Backward-compatible helper method called with a single word.
   */
  public static List<String> getNextWordPredictions(String word)
  {
    return getPredictions(word, null);
  }

  /**
   * Helper to parse and extract the last 1 or 2 clean words from cursor context.
   */
  public static String[] extractRecentTokens(String text)
  {
    if (text == null)
      return new String[0];
    String trimmed = text.trim();
    if (trimmed.isEmpty())
      return new String[0];

    // Split on whitespace or sentence punctuation
    String[] parts = trimmed.split("[\\s\\.,\\?!:;\"'\\(\\)\\[\\]{}]+");
    List<String> clean = new ArrayList<>();
    for (int i = parts.length - 1; i >= 0 && clean.size() < 2; i--)
    {
      String p = parts[i].trim();
      if (!p.isEmpty())
      {
        clean.add(0, p);
      }
    }
    return clean.toArray(new String[0]);
  }
}
