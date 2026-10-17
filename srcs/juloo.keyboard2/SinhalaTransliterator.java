package juloo.keyboard2;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phonetic transliteration engine for Sinhala (Singlish to Sinhala Unicode).
 * Translates phonetic Latin text (e.g., "amma", "sinhala", "kohomada")
 * into proper Sinhala Unicode text ("අම්මා", "සිංහල", "කොහොමද").
 */
public final class SinhalaTransliterator
{
  private static final Map<String, String> COMMON_WORDS = new HashMap<>();

  static
  {
    // High-frequency colloquial Singlish words and names mapping (Helakuru-like)
    COMMON_WORDS.put("amma", "අම්මා");
    COMMON_WORDS.put("thaththa", "තාත්තා");
    COMMON_WORDS.put("thatha", "තාත්තා");
    COMMON_WORDS.put("akka", "අක්කා");
    COMMON_WORDS.put("aiyya", "අයියා");
    COMMON_WORDS.put("aiya", "අයියා");
    COMMON_WORDS.put("malli", "මල්ලි");
    COMMON_WORDS.put("nangi", "නංගි");
    COMMON_WORDS.put("sinhala", "සිංහල");
    COMMON_WORDS.put("singlish", "සිංග්ලිෂ්");
    COMMON_WORDS.put("bhashawa", "භාෂාව");
    COMMON_WORDS.put("bhaashawa", "භාෂාව");
    COMMON_WORDS.put("bhashaawa", "භාෂාව");
    COMMON_WORDS.put("kohomada", "කොහොමද");
    COMMON_WORDS.put("mokada", "මොකද");
    COMMON_WORDS.put("monawada", "මොනවද");
    COMMON_WORDS.put("kawadda", "කවද්ද");
    COMMON_WORDS.put("koheda", "කොහෙද");
    COMMON_WORDS.put("kothanada", "කොතනද");
    COMMON_WORDS.put("mokakda", "මොකක්ද");
    COMMON_WORDS.put("aeyi", "ඇයි");
    COMMON_WORDS.put("ayi", "ඇයි");
    COMMON_WORDS.put("lanka", "ලංකා");
    COMMON_WORDS.put("lankawa", "ලංකාව");
    COMMON_WORDS.put("sri", "ශ්‍රී");
    COMMON_WORDS.put("suba", "සුබ");
    COMMON_WORDS.put("udaesanak", "උදෑසනක්");
    COMMON_WORDS.put("udasanak", "උදෑසනක්");
    COMMON_WORDS.put("dawasak", "දවසක්");
    COMMON_WORDS.put("dhavasak", "දවසක්");
    COMMON_WORDS.put("sandawak", "සන්ධ්‍යාවක්");
    COMMON_WORDS.put("sandhawak", "සන්ධ්‍යාවක්");
    COMMON_WORDS.put("rathriyak", "රාත්‍රියක්");
    COMMON_WORDS.put("ayubowan", "ආයුබෝවන්");
    COMMON_WORDS.put("sthuthi", "ස්තූතියි");
    COMMON_WORDS.put("sthutiyi", "ස්තූතියි");
    COMMON_WORDS.put("karunakarala", "කරුණාකරලා");
    COMMON_WORDS.put("elakiri", "එළකිරි");
    COMMON_WORDS.put("jayawewa", "ජයවේවා");
    COMMON_WORDS.put("parissamen", "පරිස්සමෙන්");
    COMMON_WORDS.put("budusaranai", "බුදුසරණයි");
    COMMON_WORDS.put("theruwansaranai", "තෙරුවන් සරණයි");

    // Common Sri Lankan names
    COMMON_WORDS.put("lakmal", "ලක්මාල්");
    COMMON_WORDS.put("lakmaal", "ලක්මාල්");
    COMMON_WORDS.put("nirmal", "නිර්මාල්");
    COMMON_WORDS.put("ajmal", "අජ්මාල්");
    COMMON_WORDS.put("kamal", "කමල්");
    COMMON_WORDS.put("nimal", "නිමල්");
    COMMON_WORDS.put("sunil", "සුනිල්");
    COMMON_WORDS.put("kasun", "කසුන්");
    COMMON_WORDS.put("nuwan", "නුවන්");
    COMMON_WORDS.put("ruwan", "රුවන්");
    COMMON_WORDS.put("chamara", "චාමර");
    COMMON_WORDS.put("roshan", "රොෂාන්");
    COMMON_WORDS.put("saman", "සමන්");
    COMMON_WORDS.put("sandun", "සඳුන්");
    COMMON_WORDS.put("chathura", "චතුර");
    COMMON_WORDS.put("lahiru", "ළහිරු");
    COMMON_WORDS.put("isuru", "ඉසුරු");
    COMMON_WORDS.put("thilina", "තිළිණ");
    COMMON_WORDS.put("dinesh", "දිනේෂ්");
    COMMON_WORDS.put("mahesh", "මහේෂ්");
    COMMON_WORDS.put("sampath", "සම්පත්");
    COMMON_WORDS.put("bandara", "බණ්ඩාර");
    COMMON_WORDS.put("kumara", "කුමාර");
    COMMON_WORDS.put("perera", "පෙරේරා");
    COMMON_WORDS.put("silva", "සිල්වා");
    COMMON_WORDS.put("fernando", "ප්‍රනාන්දු");
    COMMON_WORDS.put("namal", "නාමල්");
    COMMON_WORDS.put("ajith", "අජිත්");
    COMMON_WORDS.put("ranil", "රනිල්");
    COMMON_WORDS.put("sajith", "සජිත්");
    COMMON_WORDS.put("anura", "අනුර");
    COMMON_WORDS.put("mahinda", "මහින්ද");
    COMMON_WORDS.put("priyantha", "ප්‍රියන්ත");
    COMMON_WORDS.put("sugath", "සුගත්");
    COMMON_WORDS.put("sarath", "සරත්");
    COMMON_WORDS.put("danushka", "ධනුෂ්ක");
    COMMON_WORDS.put("dhanushka", "ධනුෂ්ක");
    COMMON_WORDS.put("lasith", "ලසිත්");
    COMMON_WORDS.put("sanath", "සනත්");
    COMMON_WORDS.put("kusal", "කුසල්");
    COMMON_WORDS.put("wanindu", "වනිඳු");
    COMMON_WORDS.put("pathum", "පැතුම්");
    COMMON_WORDS.put("charith", "චරිත්");
    COMMON_WORDS.put("dimuth", "දිමුත්");
    COMMON_WORDS.put("dhananjaya", "ධනංජය");
    COMMON_WORDS.put("dananjaya", "ධනංජය");
    COMMON_WORDS.put("bhanuka", "භානුක");
    COMMON_WORDS.put("avishka", "අවිශ්ක");
    COMMON_WORDS.put("chamika", "චාමික");
    COMMON_WORDS.put("mahela", "මහේල");
    COMMON_WORDS.put("kumar", "කුමාර්");

    // Common places
    COMMON_WORDS.put("colombo", "කොළඹ");
    COMMON_WORDS.put("kandy", "මහනුවර");
    COMMON_WORDS.put("galle", "ගාල්ල");
    COMMON_WORDS.put("jaffna", "යාපනය");
    COMMON_WORDS.put("matara", "මාතර");
    COMMON_WORDS.put("kurunegala", "කුරුණෑගල");
    COMMON_WORDS.put("gampaha", "ගම්පහ");
    COMMON_WORDS.put("anuradhapura", "අනුරාධපුර");

    // Everyday pronouns, verbs, and nouns
    COMMON_WORDS.put("mama", "මම");
    COMMON_WORDS.put("oya", "ඔයා");
    COMMON_WORDS.put("oyaa", "ඔයා");
    COMMON_WORDS.put("api", "අපි");
    COMMON_WORDS.put("eyaa", "එයා");
    COMMON_WORDS.put("eya", "එයා");
    COMMON_WORDS.put("mata", "මට");
    COMMON_WORDS.put("oyata", "ඔයාට");
    COMMON_WORDS.put("apita", "අපිට");
    COMMON_WORDS.put("eyata", "එයාට");
    COMMON_WORDS.put("mage", "මගේ");
    COMMON_WORDS.put("ape", "අපේ");
    COMMON_WORDS.put("oyage", "ඔයාගේ");
    COMMON_WORDS.put("oyaage", "ඔයාගේ");
    COMMON_WORDS.put("eyage", "එයාගේ");
    COMMON_WORDS.put("me", "මේ");
    COMMON_WORDS.put("eka", "එක");
    COMMON_WORDS.put("meka", "මේක");
    COMMON_WORDS.put("araka", "අරක");
    COMMON_WORDS.put("gedara", "ගෙදර");
    COMMON_WORDS.put("wada", "වැඩ");
    COMMON_WORDS.put("wadak", "වැඩක්");
    COMMON_WORDS.put("wade", "වැඩේ");
    COMMON_WORDS.put("gamage", "ගමගේ");
    COMMON_WORDS.put("gamagee", "ගමගේ");
    COMMON_WORDS.put("salli", "සල්ලි");
    COMMON_WORDS.put("poth", "පොත්");
    COMMON_WORDS.put("kema", "කෑම");
    COMMON_WORDS.put("kaema", "කෑම");
    COMMON_WORDS.put("wathura", "වතුර");
    COMMON_WORDS.put("karanawa", "කරනවා");
    COMMON_WORDS.put("karanna", "කරන්න");
    COMMON_WORDS.put("kala", "කළා");
    COMMON_WORDS.put("kalaa", "කළා");
    COMMON_WORDS.put("balanawa", "බලනවා");
    COMMON_WORDS.put("balanna", "බලන්න");
    COMMON_WORDS.put("yanawa", "යනවා");
    COMMON_WORDS.put("yanna", "යන්න");
    COMMON_WORDS.put("giya", "ගියා");
    COMMON_WORDS.put("enawa", "එනවා");
    COMMON_WORDS.put("enna", "එන්න");
    COMMON_WORDS.put("aawa", "ආවා");
    COMMON_WORDS.put("kanawa", "කනවා");
    COMMON_WORDS.put("kanna", "කන්න");
    COMMON_WORDS.put("bonawa", "බොනවා");
    COMMON_WORDS.put("bonna", "බොන්න");
    COMMON_WORDS.put("hithanawa", "හිතනවා");
    COMMON_WORDS.put("hithanna", "හිතන්න");
    COMMON_WORDS.put("dannawa", "දන්නවා");
    COMMON_WORDS.put("danna", "දන්න");
    COMMON_WORDS.put("kiyanna", "කියන්න");
    COMMON_WORDS.put("kiyala", "කියලා");
    COMMON_WORDS.put("kiwwa", "කිව්වා");
    COMMON_WORDS.put("lassana", "ලස්සන");
    COMMON_WORDS.put("honda", "හොඳ");
    COMMON_WORDS.put("naraka", "නරක");
    COMMON_WORDS.put("godak", "ගොඩක්");
    COMMON_WORDS.put("tika", "ටික");
    COMMON_WORDS.put("tikak", "ටිකක්");
    COMMON_WORDS.put("podda", "පොඩ්ඩ");
    COMMON_WORDS.put("poddak", "පොඩ්ඩක්");
    COMMON_WORDS.put("hari", "හරි");
    COMMON_WORDS.put("puluwan", "පුළුවන්");
    COMMON_WORDS.put("puluwanda", "පුළුවන්ද");
    COMMON_WORDS.put("ba", "බෑ");
    COMMON_WORDS.put("bae", "බෑ");
    COMMON_WORDS.put("baeri", "බෑ");
    COMMON_WORDS.put("na", "නෑ");
    COMMON_WORDS.put("ne", "නෑ");
    COMMON_WORDS.put("nehe", "නෑ");
    COMMON_WORDS.put("ow", "ඔව්");
    COMMON_WORDS.put("heta", "හෙට");
    COMMON_WORDS.put("ada", "අද");
    COMMON_WORDS.put("iye", "ඊයේ");
    COMMON_WORDS.put("passe", "පස්සේ");
    COMMON_WORDS.put("kalin", "කලින්");
    COMMON_WORDS.put("dan", "දැන්");
    COMMON_WORDS.put("dhan", "දැන්");
    COMMON_WORDS.put("nethuwa", "නැතුව");
    COMMON_WORDS.put("netuwa", "නැතුව");
    COMMON_WORDS.put("kelinma", "කෙලින්ම");
    COMMON_WORDS.put("widiyata", "විදියට");
    COMMON_WORDS.put("widiya", "විදිය");
    COMMON_WORDS.put("vidiya", "විදිය");
    COMMON_WORDS.put("hadanna", "හදන්න");
    COMMON_WORDS.put("hadanawa", "හදනවා");
    COMMON_WORDS.put("pera", "පෙර");
    COMMON_WORDS.put("visaduma", "විසඳුම");
    COMMON_WORDS.put("visadu", "විසඳූ");
    COMMON_WORDS.put("getaluwa", "ගැටලුව");
    COMMON_WORDS.put("gataluwa", "ගැටලුව");
    COMMON_WORDS.put("gihin", "ගිහින්");
    COMMON_WORDS.put("samahara", "සමහර");
    COMMON_WORDS.put("wachana", "වචන");
    COMMON_WORDS.put("witharayi", "විතරයි");
    COMMON_WORDS.put("vitharayi", "විතරයි");
    COMMON_WORDS.put("witharai", "විතරයි");
    COMMON_WORDS.put("vitharai", "විතරයි");
    COMMON_WORDS.put("hariyata", "හරියට");
    COMMON_WORDS.put("wenne", "වෙන්නේ");
    COMMON_WORDS.put("wenawa", "වෙනවා");
    COMMON_WORDS.put("type", "ටයිප්");
    COMMON_WORDS.put("karala", "කරලා");
    COMMON_WORDS.put("balala", "බලලා");
    COMMON_WORDS.put("awilla", "ඇවිල්ලා");
    COMMON_WORDS.put("hithala", "හිතලා");
    COMMON_WORDS.put("thiyenawa", "තියෙනවා");
    COMMON_WORDS.put("thiyanawa", "තියනවා");
    COMMON_WORDS.put("thiyenne", "තියෙන්නේ");
    COMMON_WORDS.put("oyalata", "ඔයාලට");
    COMMON_WORDS.put("eyalata", "එයාලට");
    COMMON_WORDS.put("oyala", "ඔයාලා");
    COMMON_WORDS.put("eyala", "එයාලා");
    COMMON_WORDS.put("aththa", "ඇත්ත");
    COMMON_WORDS.put("boruwak", "බොරුවක්");
    COMMON_WORDS.put("boru", "බොරු");
    COMMON_WORDS.put("inna", "ඉන්න");
    COMMON_WORDS.put("hitiya", "හිටියා");
    COMMON_WORDS.put("hitya", "හිටියා");
    COMMON_WORDS.put("onna", "ඔන්න");
    COMMON_WORDS.put("menna", "මෙන්න");
    COMMON_WORDS.put("kamak", "කමක්");
    COMMON_WORDS.put("kamakne", "කමක් නෑ");
    COMMON_WORDS.put("kollo", "කොල්ලෝ");
    COMMON_WORDS.put("kello", "කෙල්ලෝ");
    COMMON_WORDS.put("kollanta", "කොල්ලන්ට");
    COMMON_WORDS.put("kellanta", "කෙල්ලන්ට");
    COMMON_WORDS.put("rata", "රට");
    COMMON_WORDS.put("pitarata", "පිටරට");
    COMMON_WORDS.put("sindu", "සින්දු");
    COMMON_WORDS.put("sinduwa", "සින්දුව");
    COMMON_WORDS.put("kathawa", "කතාව");
    COMMON_WORDS.put("katakawa", "කටකතාව");
  }

  // Standalone vowels (start of word or preceded by vowel)
  private static final String[][] VOWELS_INDEPENDENT = {
    {"aae", "\u0D88"}, // ඈ
    {"ae", "\u0D87"},  // ඇ
    {"aa", "\u0D86"},  // ආ
    {"A", "\u0D86"},   // ආ
    {"ii", "\u0D8A"},  // ඊ
    {"ee", "\u0D92"},  // ඒ
    {"I", "\u0D8A"},   // ඊ
    {"uu", "\u0D8C"},  // ඌ
    {"oo", "\u0D95"},  // ඕ
    {"U", "\u0D8C"},   // ඌ
    {"ai", "\u0D93"},  // ඓ
    {"au", "\u0D96"},  // ඖ
    {"ea", "\u0D92"},  // ඒ
    {"ei", "\u0D92"},  // ඒ
    {"oe", "\u0D95"},  // ඕ
    {"a", "\u0D85"},   // අ
    {"i", "\u0D89"},   // ඉ
    {"u", "\u0D8B"},   // උ
    {"e", "\u0D91"},   // එ
    {"E", "\u0D92"},   // ඒ
    {"o", "\u0D94"},   // ඔ
    {"O", "\u0D95"}    // ඕ
  };

  // Dependent vowel signs (attached to consonants)
  private static final String[][] VOWELS_DEPENDENT = {
    {"aae", "\u0DD1"}, // ෑ
    {"ae", "\u0DD0"},  // ැ
    {"aa", "\u0DCF"},  // ා
    {"A", "\u0DCF"},   // ා
    {"ii", "\u0DD3"},  // ී
    {"ee", "\u0DDA"},  // ේ
    {"I", "\u0DD3"},   // ී
    {"uu", "\u0DD6"},  // ූ
    {"oo", "\u0DDD"},  // ෝ
    {"U", "\u0DD6"},   // ූ
    {"ai", "\u0DDB"},  // ෛ
    {"au", "\u0DDE"},  // ෞ
    {"ea", "\u0DDA"},  // ේ
    {"ei", "\u0DDA"},  // ේ
    {"oe", "\u0DDD"},  // ෝ
    {"a", ""},         // inherent 'a' (no diacritic)
    {"i", "\u0DD2"},   // ි
    {"u", "\u0DD4"},   // ු
    {"e", "\u0DD9"},   // ෙ
    {"E", "\u0DDA"},   // ේ
    {"o", "\u0DDC"},   // ො
    {"O", "\u0DDD"}    // ෝ
  };

  // Consonants ordered by prefix length descending
  private static final String[][] CONSONANTS = {
    {"chch", "\u0DA0\u0DCA\u200D\u0DA0"}, // ච්ච
    {"shh", "\u0DC2\u0DCA"}, // ෂ්
    {"chh", "\u0DA1"}, // ඡ
    {"kh", "\u0D9B"},  // ඛ
    {"gh", "\u0D9D"},  // ඝ
    {"ch", "\u0DA0"},  // ච
    {"jh", "\u0DA3"},  // ඣ
    {"Th", "\u0DAE"},  // ථ
    {"Dh", "\u0DAA"},  // ඪ
    {"th", "\u0DAD"},  // ත
    {"dh", "\u0DB0"},  // ධ
    {"ph", "\u0DB5"},  // ඵ
    {"bh", "\u0DB7"},  // භ
    {"sh", "\u0DC1"},  // ශ
    {"Sh", "\u0DC2"},  // ෂ
    {"gn", "\u0DA4"},  // ඤ
    {"kn", "\u0DA4"},  // ඤ
    {"jn", "\u0DA5"},  // ඥ
    {"nd", "\u0DB3"},  // ඳ
    {"nD", "\u0DAC"},  // ඬ
    {"mb", "\u0DB9"},  // ඹ
    {"ng", "\u0D9F"},  // ඟ
    {"k", "\u0D9A"},   // ක
    {"g", "\u0D9C"},   // ග
    {"c", "\u0DA0"},   // ච
    {"j", "\u0DA2"},   // ජ
    {"T", "\u0DA8"},   // ඨ
    {"t", "\u0DA7"},   // ට
    {"D", "\u0DA9"},   // ඩ
    {"d", "\u0DAF"},   // ද
    {"N", "\u0DAB"},   // ණ
    {"n", "\u0DB1"},   // න
    {"p", "\u0DB4"},   // ප
    {"b", "\u0DB6"},   // බ
    {"m", "\u0DB8"},   // ම
    {"y", "\u0DBA"},   // ය
    {"r", "\u0DBB"},   // ර
    {"L", "\u0DC5"},   // ළ
    {"l", "\u0DBD"},   // ල
    {"v", "\u0DC0"},   // ව
    {"w", "\u0DC0"},   // ව
    {"s", "\u0DC3"},   // ස
    {"S", "\u0DC2"},   // ෂ
    {"h", "\u0DC4"},   // හ
    {"f", "\u0DC6"},   // ෆ
    {"x", "\u0D82"}    // ං (Anusvaraya)
  };

  private static final String VIRAMA = "\u0DCA"; // ්
  private static final String ZWJ = "\u200D";    // Zero-Width Joiner
  private static final String ANUSVARA = "\u0D82"; // ං (\u0D82)

  private SinhalaTransliterator() {}

  /**
   * Transliterate a full text string (word, sentence, or phrase)
   * into Sinhala Unicode.
   */
  public static String transliterate(String text)
  {
    if (text == null || text.isEmpty())
      return text;

    StringBuilder result = new StringBuilder();
    StringBuilder wordBuffer = new StringBuilder();

    for (int i = 0; i < text.length(); i++)
    {
      char c = text.charAt(i);
      if (Character.isLetter(c))
      {
        wordBuffer.append(c);
      }
      else
      {
        if (wordBuffer.length() > 0)
        {
          result.append(transliterateWord(wordBuffer.toString()));
          wordBuffer.setLength(0);
        }
        result.append(c);
      }
    }

    if (wordBuffer.length() > 0)
    {
      result.append(transliterateWord(wordBuffer.toString()));
    }

    return result.toString();
  }

  /**
   * Transliterate a single word from Singlish to Sinhala.
   */
  public static String transliterateWord(String word)
  {
    if (word == null || word.isEmpty())
      return "";

    String lower = word.toLowerCase();
    if (COMMON_WORDS.containsKey(lower))
    {
      return COMMON_WORDS.get(lower);
    }

    return parsePhonetic(word);
  }

  /**
   * Return up to 3 candidate suggestions for a typed word (Helakuru-like):
   * 0: Best transliteration (e.g. ලක්මාල් for lakmal)
   * 1: Alternative phonetic variation (e.g. ලක්මල්)
   * 2: Original typed English word (e.g. lakmal)
   */
  public static java.util.List<String> getSuggestions(String word)
  {
    java.util.List<String> list = new java.util.ArrayList<>();
    if (word == null || word.isEmpty())
      return list;

    String primary = transliterateWord(word);
    list.add(primary);

    String phonetic = parsePhonetic(word);
    if (!phonetic.isEmpty() && !phonetic.equals(primary) && !list.contains(phonetic))
    {
      list.add(phonetic);
    }
    else
    {
      String alt = null;
      if (primary.equals("\u0D85")) // අ -> ආ
      {
        alt = "\u0D86";
      }
      else if (primary.equals("\u0D86")) // ආ -> අ
      {
        alt = "\u0D85";
      }
      else if (primary.equals("\u0D87")) // ඇ -> ඈ
      {
        alt = "\u0D88";
      }
      else if (primary.equals("\u0D89")) // ඉ -> ඊ
      {
        alt = "\u0D8A";
      }
      else if (primary.equals("\u0D8B")) // උ -> ඌ
      {
        alt = "\u0D8C";
      }
      else if (primary.equals("\u0D91")) // එ -> ඒ
      {
        alt = "\u0D92";
      }
      else if (primary.equals("\u0D94")) // ඔ -> ඕ
      {
        alt = "\u0D95";
      }
      else if (primary.endsWith("\u0DCF")) // ends with ා
      {
        alt = primary.substring(0, primary.length() - 1);
      }
      else if (!primary.endsWith("\u0DCA")
          && !(primary.length() > 0 && isVowelDiacritic(primary.charAt(primary.length() - 1)))
          && !(primary.length() > 0 && isIndependentVowel(primary.charAt(primary.length() - 1))))
      {
        alt = primary + "\u0DCF";
      }
      if (alt != null && !alt.isEmpty() && !alt.equals(primary) && !list.contains(alt))
      {
        list.add(alt);
      }
    }

    if (!list.contains(word))
    {
      list.add(word);
    }

    return list;
  }

  private static boolean isVowelDiacritic(char c)
  {
    return (c >= 0x0DCF && c <= 0x0DDF) || (c >= 0x0DF2 && c <= 0x0DF3);
  }

  private static boolean isIndependentVowel(char c)
  {
    return (c >= 0x0D85 && c <= 0x0D96);
  }

  /**
   * Pure systematic phonetic parser (independent of common word dictionary).
   */
  public static String parsePhonetic(String word)
  {
    if (word == null || word.isEmpty())
      return "";

    StringBuilder out = new StringBuilder();
    int len = word.length();
    int i = 0;

    while (i < len)
    {
      // Check for -nawa at end of word (length >= 5) e.g. karanawa -> කරනවා
      if (i == len - 4 && len >= 5 && word.substring(i).equalsIgnoreCase("nawa"))
      {
        out.append("\u0DB1\u0DC0\u0DCF"); // නවා
        break;
      }

      // Check for -ge at end of word (length >= 3 and i > 0) e.g. gamage -> ගමගේ
      if (i == len - 2 && i > 0 && word.substring(i).equalsIgnoreCase("ge"))
      {
        out.append("\u0D9C\u0DDA"); // ගේ
        break;
      }

      // Special Sinhala phonetic rule: 'n' before h, j, s, c, k turns 'n' to Anusvara (ං)
      // e.g. sinhala -> සිංහල, vyanjana -> ව්‍යංජන, lanka -> ලංකා, sansaara -> සංසාර
      if (i + 1 < len && (word.charAt(i) == 'n' || word.charAt(i) == 'N'))
      {
        char next = Character.toLowerCase(word.charAt(i + 1));
        if (next == 'h' || next == 'j' || next == 's' || next == 'c' || next == 'k')
        {
          out.append(ANUSVARA);
          i++; // skip 'n', next consonant will be parsed in next loop iteration
          continue;
        }
      }

      // Check for consonant match
      String matchedConsonant = null;
      int matchedConsonantLen = 0;
      for (String[] pair : CONSONANTS)
      {
        if (word.startsWith(pair[0], i))
        {
          matchedConsonant = pair[1];
          matchedConsonantLen = pair[0].length();
          break;
        }
      }

      if (matchedConsonant != null)
      {
        i += matchedConsonantLen;

        // If consonant is Anusvara directly
        if (matchedConsonant.equals(ANUSVARA))
        {
          out.append(ANUSVARA);
          continue;
        }

        // Check for conjuncts: Yanshaya (C + y + V) or Rakaranshaya (C + r + V)
        boolean hasConjunct = false;
        if (i < len)
        {
          char nextChar = word.charAt(i);
          if ((nextChar == 'y' || nextChar == 'Y') && i + 1 < len && isVowel(word.charAt(i + 1)))
          {
            // Yanshaya: Consonant + ් + ZWJ + ය
            out.append(matchedConsonant).append(VIRAMA).append(ZWJ).append("\u0DBA");
            i++;
            hasConjunct = true;
          }
          else if ((nextChar == 'r' || nextChar == 'R') && i + 1 < len && isVowel(word.charAt(i + 1)))
          {
            // Rakaranshaya: Consonant + ් + ZWJ + ර
            out.append(matchedConsonant).append(VIRAMA).append(ZWJ).append("\u0DBB");
            i++;
            hasConjunct = true;
          }
        }

        // Check following vowel
        String matchedVowelSign = null;
        int matchedVowelLen = 0;
        for (String[] pair : VOWELS_DEPENDENT)
        {
          if (word.startsWith(pair[0], i))
          {
            matchedVowelSign = pair[1];
            matchedVowelLen = pair[0].length();
            break;
          }
        }

        if (matchedVowelSign != null)
        {
          if (!hasConjunct)
          {
            out.append(matchedConsonant);
          }
          out.append(matchedVowelSign);
          i += matchedVowelLen;
        }
        else
        {
          // No vowel following: attach Virama (Al-Lakuna: ්)
          if (!hasConjunct)
          {
            out.append(matchedConsonant);
          }
          out.append(VIRAMA);
        }
      }
      else
      {
        // Try matching independent vowel
        String matchedVowel = null;
        int matchedVowelLen = 0;
        for (String[] pair : VOWELS_INDEPENDENT)
        {
          if (word.startsWith(pair[0], i))
          {
            matchedVowel = pair[1];
            matchedVowelLen = pair[0].length();
            break;
          }
        }

        if (matchedVowel != null)
        {
          out.append(matchedVowel);
          i += matchedVowelLen;
        }
        else
        {
          // Any other character
          out.append(word.charAt(i));
          i++;
        }
      }
    }

    return out.toString();
  }

  private static boolean isVowel(char c)
  {
    c = Character.toLowerCase(c);
    return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
  }
}
