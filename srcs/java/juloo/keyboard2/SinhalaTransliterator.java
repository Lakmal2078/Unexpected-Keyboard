package juloo.keyboard2;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
  private static final Map<String, List<String>> NEXT_WORD_PREDICTIONS = new HashMap<>();
  // Prefix suggestions need a stable key order; build it once instead of sorting on every keypress.
  private static final List<Map.Entry<String, String>> COMMON_WORDS_PREFIX_ORDER = new java.util.ArrayList<>();

  static
  {
    // High-frequency colloquial Singlish words and names mapping (Helakuru-like)
    COMMON_WORDS.put("amma", "අම්මා");
    COMMON_WORDS.put("ammaa", "අම්මා");
    COMMON_WORDS.put("thaththa", "තාත්තා");
    COMMON_WORDS.put("thatha", "තාත්තා");
    COMMON_WORDS.put("thaaththaa", "තාත්තා");
    COMMON_WORDS.put("thaththaa", "තාත්තා");
    COMMON_WORDS.put("oba", "ඔබ");
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
    COMMON_WORDS.put("kohomadha", "කොහොමද");
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

    // Common places, towns, and villages
    COMMON_WORDS.put("colombo", "කොළඹ");
    COMMON_WORDS.put("kolamba", "කොළඹ");
    COMMON_WORDS.put("kandy", "මහනුවර");
    COMMON_WORDS.put("mahanuwara", "මහනුවර");
    COMMON_WORDS.put("nuwara", "නුවර");
    COMMON_WORDS.put("galle", "ගාල්ල");
    COMMON_WORDS.put("jaffna", "යාපනය");
    COMMON_WORDS.put("yapane", "යාපනය");
    COMMON_WORDS.put("matara", "මාතර");
    COMMON_WORDS.put("kurunegala", "කුරුණෑගල");
    COMMON_WORDS.put("gampaha", "ගම්පහ");
    COMMON_WORDS.put("kalutara", "කළුතර");
    COMMON_WORDS.put("negombo", "මීගමුව");
    COMMON_WORDS.put("meegamuwa", "මීගමුව");
    COMMON_WORDS.put("anuradhapura", "අනුරාධපුරය");
    COMMON_WORDS.put("anuradhapuraya", "අනුරාධපුරය");
    COMMON_WORDS.put("polonnaruwa", "පොළොන්නරුව");
    COMMON_WORDS.put("badulla", "බදුල්ල");
    COMMON_WORDS.put("bandarawela", "බණ්ඩාරවෙල");
    COMMON_WORDS.put("ratnapura", "රත්නපුර");
    COMMON_WORDS.put("kegalle", "කෑගල්ල");
    COMMON_WORDS.put("trincomalee", "ත්‍රිකුණාමලය");
    COMMON_WORDS.put("thrikunamalaya", "ත්‍රිකුණාමලය");
    COMMON_WORDS.put("batticaloa", "මඩකලපුව");
    COMMON_WORDS.put("madakalapuwa", "මඩකලපුව");
    COMMON_WORDS.put("ampara", "අම්පාර");
    COMMON_WORDS.put("matale", "මාතලේ");
    COMMON_WORDS.put("nuwaraeliya", "නුවරඑළිය");
    COMMON_WORDS.put("monaragala", "මොනරාගල");
    COMMON_WORDS.put("puttalam", "පුත්තලම");
    COMMON_WORDS.put("puththalama", "පුත්තලම");
    COMMON_WORDS.put("mannar", "මන්නාරම");
    COMMON_WORDS.put("mannarama", "මන්නාරම");
    COMMON_WORDS.put("vavuniya", "වවුනියාව");
    COMMON_WORDS.put("kilinochchi", "කිලිනොච්චිය");
    COMMON_WORDS.put("mullaitivu", "මුලතිව්");
    COMMON_WORDS.put("moratuwa", "මොරටුව");
    COMMON_WORDS.put("panadura", "පානදුර");
    COMMON_WORDS.put("horana", "හොරණ");
    COMMON_WORDS.put("maharagama", "මහරගම");
    COMMON_WORDS.put("nugegoda", "නුගේගොඩ");
    COMMON_WORDS.put("kottawa", "කොට්ටාව");
    COMMON_WORDS.put("homagama", "හෝමාගම");
    COMMON_WORDS.put("kaduwela", "කඩුවෙල");
    COMMON_WORDS.put("malabe", "මාලබේ");
    COMMON_WORDS.put("battaramulla", "බත්තරමුල්ල");
    COMMON_WORDS.put("dehiwala", "දෙහිවල");
    COMMON_WORDS.put("mountlavinia", "ගල්කිස්ස");
    COMMON_WORDS.put("galkissa", "ගල්කිස්ස");
    COMMON_WORDS.put("wattala", "වත්තල");
    COMMON_WORDS.put("jaela", "ජාඇල");
    COMMON_WORDS.put("chilaw", "හලාවත");
    COMMON_WORDS.put("halawatha", "හලාවත");
    COMMON_WORDS.put("tangalle", "තංගල්ල");
    COMMON_WORDS.put("avissawella", "අවිස්සාවේල්ල");
    COMMON_WORDS.put("beruwala", "බේරුවල");
    COMMON_WORDS.put("aluthgama", "අලුත්ගම");
    COMMON_WORDS.put("balangoda", "බලන්ගොඩ");
    COMMON_WORDS.put("embilipitiya", "ඇඹිලිපිටිය");
    COMMON_WORDS.put("kuliyapitiya", "කුලියපිටිය");
    COMMON_WORDS.put("katunayake", "කටුනායක");
    COMMON_WORDS.put("dambulla", "දඹුල්ල");
    COMMON_WORDS.put("sigiriya", "සීගිරිය");
    COMMON_WORDS.put("ella", "ඇල්ල");
    COMMON_WORDS.put("mirissa", "මිරිස්ස");
    COMMON_WORDS.put("unawatuna", "උණවටුන");
    COMMON_WORDS.put("hikkaduwa", "හික්කඩුව");
    COMMON_WORDS.put("bentota", "බෙන්තොට");
    COMMON_WORDS.put("peradeniya", "පේරාදෙණිය");
    COMMON_WORDS.put("weligama", "වැලිගම");

    // Countries & Regions
    COMMON_WORDS.put("srilanka", "ශ්‍රී ලංකාව");
    COMMON_WORDS.put("india", "ඉන්දියාව");
    COMMON_WORDS.put("indiyawa", "ඉන්දියාව");
    COMMON_WORDS.put("america", "ඇමරිකාව");
    COMMON_WORDS.put("amerikawa", "ඇමරිකාව");
    COMMON_WORDS.put("england", "එංගලන්තය");
    COMMON_WORDS.put("englanthaya", "එංගලන්තය");
    COMMON_WORDS.put("australia", "ඕස්ට්‍රේලියාව");
    COMMON_WORDS.put("ostreliyawa", "ඕස්ට්‍රේලියාව");
    COMMON_WORDS.put("canada", "කැනඩාව");
    COMMON_WORDS.put("kanadawa", "කැනඩාව");
    COMMON_WORDS.put("japan", "ජපානය");
    COMMON_WORDS.put("japanaya", "ජපානය");
    COMMON_WORDS.put("china", "චීනය");
    COMMON_WORDS.put("chinaya", "චීනය");
    COMMON_WORDS.put("russia", "රුසියාව");
    COMMON_WORDS.put("rushiyawa", "රුසියාව");
    COMMON_WORDS.put("dubai", "ඩුබායි");
    COMMON_WORDS.put("italy", "ඉතාලිය");
    COMMON_WORDS.put("ithaliya", "ඉතාලිය");
    COMMON_WORDS.put("france", "ප්‍රංශය");
    COMMON_WORDS.put("pranshaya", "ප්‍රංශය");
    COMMON_WORDS.put("germany", "ජර්මනිය");
    COMMON_WORDS.put("jarmaniya", "ජර්මනිය");
    COMMON_WORDS.put("singapore", "සිංගප්පූරුව");
    COMMON_WORDS.put("singappuruwa", "සිංගප්පූරුව");
    COMMON_WORDS.put("malaysia", "මැලේසියාව");
    COMMON_WORDS.put("maleshiyawa", "මැලේසියාව");
    COMMON_WORDS.put("korea", "කොරියාව");
    COMMON_WORDS.put("koriyawa", "කොරියාව");
    COMMON_WORDS.put("pakistan", "පාකිස්ථානය");
    COMMON_WORDS.put("pakisthanaya", "පාකිස්ථානය");
    COMMON_WORDS.put("bangladesh", "බංග්ලාදේශය");
    COMMON_WORDS.put("saudi", "සවුදි අරාබිය");
    COMMON_WORDS.put("saudiya", "සවුදි අරාබිය");
    COMMON_WORDS.put("qatar", "කටාර්");
    COMMON_WORDS.put("kuwait", "කුවේට්");
    COMMON_WORDS.put("oman", "ඕමානය");
    COMMON_WORDS.put("newzealand", "නවසීලන්තය");
    COMMON_WORDS.put("switzerland", "ස්විට්සර්ලන්තය");
    COMMON_WORDS.put("thailand", "තායිලන්තය");
    COMMON_WORDS.put("maldives", "මාලදිවයින");

    // Additional popular personal names
    COMMON_WORDS.put("nadeeka", "නදීකා");
    COMMON_WORDS.put("sanduni", "සඳුනි");
    COMMON_WORDS.put("anusha", "අනුෂා");
    COMMON_WORDS.put("dilani", "දිලානි");
    COMMON_WORDS.put("chathurika", "චතුරිකා");
    COMMON_WORDS.put("ishara", "ඉෂාරා");
    COMMON_WORDS.put("malsha", "මල්ෂා");
    COMMON_WORDS.put("kavindi", "කාවින්දි");
    COMMON_WORDS.put("dinithi", "දිනිති");
    COMMON_WORDS.put("oshadi", "ඔෂධී");
    COMMON_WORDS.put("chamari", "චමරි");
    COMMON_WORDS.put("thilini", "තිළිණි");
    COMMON_WORDS.put("piyumi", "පියුමි");
    COMMON_WORDS.put("hasini", "හසිනි");
    COMMON_WORDS.put("chalani", "චලනි");
    COMMON_WORDS.put("kavinda", "කාවින්ද");
    COMMON_WORDS.put("sachin", "සචින්");
    COMMON_WORDS.put("akila", "අකිල");
    COMMON_WORDS.put("malinda", "මලින්ද");
    COMMON_WORDS.put("ashan", "අශාන්");
    COMMON_WORDS.put("tharaka", "තාරක");
    COMMON_WORDS.put("madhava", "මාධව");
    COMMON_WORDS.put("pansal", "පන්සල්");
    COMMON_WORDS.put("sanda", "සඳ");
    COMMON_WORDS.put("harsha", "හර්ෂ");
    COMMON_WORDS.put("indika", "ඉන්දික");
    COMMON_WORDS.put("janaka", "ජනක");
    COMMON_WORDS.put("manoj", "මනෝජ්");
    COMMON_WORDS.put("asanka", "අසංක");
    COMMON_WORDS.put("suranga", "සුරංග");
    COMMON_WORDS.put("gayan", "ගයාන්");
    COMMON_WORDS.put("chanaka", "චානක");
    COMMON_WORDS.put("nadeesh", "නදීෂ්");
    COMMON_WORDS.put("supun", "සුපුන්");
    COMMON_WORDS.put("hasitha", "හසිත");

    // Frequent words with 'සැ' / 'සෑ' / 'ැ' / 'ෑ'
    COMMON_WORDS.put("sae", "සැ");
    COMMON_WORDS.put("saae", "සෑ");
    COMMON_WORDS.put("saepa", "සැප");
    COMMON_WORDS.put("sepa", "සැප");
    COMMON_WORDS.put("saepak", "සැපක්");
    COMMON_WORDS.put("sepak", "සැපක්");
    COMMON_WORDS.put("saema", "සැම");
    COMMON_WORDS.put("sema", "සැම");
    COMMON_WORDS.put("saemata", "සැමට");
    COMMON_WORDS.put("semata", "සැමට");
    COMMON_WORDS.put("saemiya", "සැමියා");
    COMMON_WORDS.put("semiya", "සැමියා");
    COMMON_WORDS.put("saemiyage", "සැමියාගේ");
    COMMON_WORDS.put("semiyage", "සැමියාගේ");
    COMMON_WORDS.put("saera", "සැර");
    COMMON_WORDS.put("sera", "සැර");
    COMMON_WORDS.put("saerayak", "සැරයක්");
    COMMON_WORDS.put("serayak", "සැරයක්");
    COMMON_WORDS.put("saeka", "සැක");
    COMMON_WORDS.put("seka", "සැක");
    COMMON_WORDS.put("saekayak", "සැකයක්");
    COMMON_WORDS.put("sekayak", "සැකයක්");
    COMMON_WORDS.put("saelasuma", "සැලසුම");
    COMMON_WORDS.put("selasuma", "සැලසුම");
    COMMON_WORDS.put("saelasum", "සැලසුම්");
    COMMON_WORDS.put("selasum", "සැලසුම්");
    COMMON_WORDS.put("saehenna", "සෑහෙන්න");
    COMMON_WORDS.put("sehenna", "සෑහෙන්න");
    COMMON_WORDS.put("saehena", "සෑහෙන");
    COMMON_WORDS.put("sehena", "සෑහෙන");
    COMMON_WORDS.put("saehaellu", "සැහැල්ලු");
    COMMON_WORDS.put("sehallu", "සැහැල්ලු");
    COMMON_WORDS.put("saendawa", "සැන්දෑව");
    COMMON_WORDS.put("sendawa", "සැන්දෑව");
    COMMON_WORDS.put("saepthembar", "සැප්තැම්බර්");
    COMMON_WORDS.put("september", "සැප්තැම්බර්");
    COMMON_WORDS.put("waeda", "වැඩ");
    COMMON_WORDS.put("waedak", "වැඩක්");
    COMMON_WORDS.put("baehari", "බැහැරි");
    COMMON_WORDS.put("baeri", "බැරි");
    COMMON_WORDS.put("baeha", "බැහැ");
    COMMON_WORDS.put("naeha", "නැහැ");
    COMMON_WORDS.put("aethi", "ඇති");
    COMMON_WORDS.put("aethak", "ඈතක්");
    COMMON_WORDS.put("aetha", "ඈත");
    COMMON_WORDS.put("paeni", "පැණි");
    COMMON_WORDS.put("paeththe", "පැත්තේ");
    COMMON_WORDS.put("paeththa", "පැත්ත");
    COMMON_WORDS.put("haema", "හැම");
    COMMON_WORDS.put("haemoma", "හැමෝම");
    COMMON_WORDS.put("daen", "දැන්");

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
    COMMON_WORDS.put("gedhara", "ගෙදර");
    COMMON_WORDS.put("vidhyaava", "විද්‍යාව");
    COMMON_WORDS.put("vidyaava", "විද්‍යාව");
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

    // Questions & Conversations
    COMMON_WORDS.put("kawadada", "කවදාද");
    COMMON_WORDS.put("kageda", "කාගේද");
    COMMON_WORDS.put("katada", "කාටද");
    COMMON_WORDS.put("mokak", "මොකක්");
    COMMON_WORDS.put("monada", "මොනවද");
    COMMON_WORDS.put("kiyada", "කීයද");
    COMMON_WORDS.put("apiwa", "අපිව");
    COMMON_WORDS.put("oyawa", "ඔයාව");
    COMMON_WORDS.put("eyawa", "එයාව");
    COMMON_WORDS.put("karamu", "කරමු");
    COMMON_WORDS.put("karannada", "කරන්නද");
    COMMON_WORDS.put("karannam", "කරන්නම්");
    COMMON_WORDS.put("karanne", "කරන්නේ");
    COMMON_WORDS.put("yamu", "යමු");
    COMMON_WORDS.put("balamu", "බලමු");
    COMMON_WORDS.put("denawa", "දෙනවා");
    COMMON_WORDS.put("denna", "දෙන්න");
    COMMON_WORDS.put("dunna", "දුන්නා");
    COMMON_WORDS.put("gannawa", "ගන්නවා");
    COMMON_WORDS.put("ganna", "ගන්න");
    COMMON_WORDS.put("gaththa", "ගත්තා");
    COMMON_WORDS.put("inawa", "ඉන්නවා");
    COMMON_WORDS.put("innawa", "ඉන්නවා");
    COMMON_WORDS.put("hitanawa", "හිතනවා");
    COMMON_WORDS.put("una", "වුණා");
    COMMON_WORDS.put("wuna", "වුණා");
    COMMON_WORDS.put("baerida", "බැරිද");
    COMMON_WORDS.put("barida", "බැරිද");
    COMMON_WORDS.put("epa", "එපා");
    COMMON_WORDS.put("epaa", "එපා");
    COMMON_WORDS.put("aluth", "අලුත්");
    COMMON_WORDS.put("awuruddak", "අවුරුද්දක්");
    COMMON_WORDS.put("vesak", "වෙසක්");
    COMMON_WORDS.put("poson", "පොසොන්");
    COMMON_WORDS.put("mithura", "මිතුරා");
    COMMON_WORDS.put("yaluwa", "යාලුවා");
    COMMON_WORDS.put("yaaluwa", "යාලුවා");
    COMMON_WORDS.put("machan", "මචං");
    COMMON_WORDS.put("patta", "පට්ට");
    COMMON_WORDS.put("maru", "මරු");
    COMMON_WORDS.put("nikan", "නිකන්");
    COMMON_WORDS.put("aththatama", "ඇත්තටම");
    COMMON_WORDS.put("aniwa", "අනිවා");
    COMMON_WORDS.put("aniva", "අනිවා");
    COMMON_WORDS.put("aniwaryen", "අනිවාර්යයෙන්");
    COMMON_WORDS.put("samawenna", "සමාවෙන්න");
    COMMON_WORDS.put("phone", "ෆෝන්");
    COMMON_WORDS.put("mobile", "මොබයිල්");
    COMMON_WORDS.put("app", "ඇප්");
    COMMON_WORDS.put("keyboard", "කීබෝඩ්");
    COMMON_WORDS.put("message", "මැසේජ්");
    COMMON_WORDS.put("call", "කෝල්");
    COMMON_WORDS.put("link", "ලින්ක්");
    COMMON_WORDS.put("group", "ගෲප්");
    COMMON_WORDS.put("post", "පෝස්ට්");
    COMMON_WORDS.put("video", "වීඩියෝ");
    COMMON_WORDS.put("photo", "ෆොටෝ");
    COMMON_WORDS.put("camera", "කැමරා");
    COMMON_WORDS.put("battery", "බැටරි");
    COMMON_WORDS.put("charge", "චාජ්");
    COMMON_WORDS.put("sim", "සිම්");
    COMMON_WORDS.put("signal", "සිග්නල්");
    COMMON_WORDS.put("net", "නෙට්");
    COMMON_WORDS.put("data", "ඩේටා");
    COMMON_WORDS.put("wifi", "වයිෆයි");
    COMMON_WORDS.put("online", "ඔන්ලයින්");
    COMMON_WORDS.put("offline", "ඕෆ්ලයින්");
    COMMON_WORDS.put("office", "ඔෆිස්");
    COMMON_WORDS.put("job", "ජොබ්");
    COMMON_WORDS.put("school", "ඉස්කෝලේ");
    COMMON_WORDS.put("iskole", "ඉස්කෝලේ");
    COMMON_WORDS.put("campus", "කැම්පස්");
    COMMON_WORDS.put("class", "ක්ලාස්");
    COMMON_WORDS.put("cricket", "ක්‍රිකට්");
    COMMON_WORDS.put("match", "මැච්");

    // High frequency Next-word predictions
    NEXT_WORD_PREDICTIONS.put("සුබ", Arrays.asList("උදෑසනක්", "දවසක්", "රාත්‍රියක්"));
    NEXT_WORD_PREDICTIONS.put("ස්තූතියි", Arrays.asList("බොහොම", "ඔබට", "යාලුවා"));
    NEXT_WORD_PREDICTIONS.put("ආයුබෝවන්", Arrays.asList("ඔබට", "සියලු දෙනාටම", "සුබ"));
    NEXT_WORD_PREDICTIONS.put("මම", Arrays.asList("යනවා", "කරන්නම්", "දන්නවා"));
    NEXT_WORD_PREDICTIONS.put("ඔයා", Arrays.asList("කොහොමද", "මොකද", "එනවද"));
    NEXT_WORD_PREDICTIONS.put("අපි", Arrays.asList("යමු", "කරමු", "බලමු"));
    NEXT_WORD_PREDICTIONS.put("තෙරුවන්", Arrays.asList("සරණයි", "ආශිර්වාදය"));
    NEXT_WORD_PREDICTIONS.put("බුදු", Arrays.asList("සරණයි", "දහම්", "ගුණ"));
    NEXT_WORD_PREDICTIONS.put("ජය", Arrays.asList("වේවා", "නියතයි", "ගමු"));
    NEXT_WORD_PREDICTIONS.put("ලංකා", Arrays.asList("දීප", "බැංකුව", "මාතාව"));
    NEXT_WORD_PREDICTIONS.put("ගොඩක්", Arrays.asList("ස්තූතියි", "හොඳයි", "පින්"));
    NEXT_WORD_PREDICTIONS.put("පරිස්සමෙන්", Arrays.asList("යන්න", "ඉන්න", "එන්න"));
    NEXT_WORD_PREDICTIONS.put("හරි", Arrays.asList("මචං", "යාලුවා", "එහෙනම්"));
    NEXT_WORD_PREDICTIONS.put("දැන්", Arrays.asList("මොකද", "යමු", "හරි"));
    NEXT_WORD_PREDICTIONS.put("හෙට", Arrays.asList("හමුවෙමු", "එන්නම්", "යමු"));
    NEXT_WORD_PREDICTIONS.put("කොහොමද", Arrays.asList("ඔයාට", "යාලුවා", "විස්තර"));
    NEXT_WORD_PREDICTIONS.put("මොකද", Arrays.asList("කරන්නේ", "විස්තර", "හිතන්නේ"));
    NEXT_WORD_PREDICTIONS.put("කාරුණික", Arrays.asList("ඉල්ලීමක්", "දැනුම්දීමක්"));
    NEXT_WORD_PREDICTIONS.put("කරුණාකර", Arrays.asList("සවන් දෙන්න", "පැමිණෙන්න", "බලන්න"));
    NEXT_WORD_PREDICTIONS.put("අද", Arrays.asList("දවසේ", "මොකද", "යනවද"));
    NEXT_WORD_PREDICTIONS.put("ඊයේ", Arrays.asList("රෑ", "හවස", "මොකද"));
    NEXT_WORD_PREDICTIONS.put("කොහෙද", Arrays.asList("යන්නේ", "ඉන්නේ", "ගියේ"));
    NEXT_WORD_PREDICTIONS.put("කවද්ද", Arrays.asList("එන්නේ", "යන්නේ", "හමුවෙන්නේ"));
    NEXT_WORD_PREDICTIONS.put("මොනවද", Arrays.asList("විස්තර", "කරන්නේ", "ඕන"));

    // Additional expanded suggestions
    NEXT_WORD_PREDICTIONS.put("එන්න", Arrays.asList("පුළුවන්ද", "කෝ", "ගෙදර"));
    NEXT_WORD_PREDICTIONS.put("යන්න", Arrays.asList("ඕනේ", "කෝ", "එපා"));
    NEXT_WORD_PREDICTIONS.put("කන්න", Arrays.asList("ඕනේ", "යමු", "මොනවද"));
    NEXT_WORD_PREDICTIONS.put("බොන්න", Arrays.asList("ඕනේ", "යමු", "මොනවද"));
    NEXT_WORD_PREDICTIONS.put("බලන්න", Arrays.asList("ඕනේ", "යමු", "මොනවද"));
    NEXT_WORD_PREDICTIONS.put("කරන්න", Arrays.asList("පුළුවන්ද", "ඕනේ", "එපා"));
    NEXT_WORD_PREDICTIONS.put("ගන්න", Arrays.asList("ඕනේ", "පුළුවන්ද", "එපා"));
    NEXT_WORD_PREDICTIONS.put("දෙන්න", Arrays.asList("පුළුවන්ද", "කෝ", "එපා"));
    NEXT_WORD_PREDICTIONS.put("මගේ", Arrays.asList("අම්මා", "යාලුවා", "රට"));
    NEXT_WORD_PREDICTIONS.put("ඔයාගේ", Arrays.asList("නම", "ගම", "වැඩ"));
    NEXT_WORD_PREDICTIONS.put("එයා", Arrays.asList("ආවා", "ගියා", "එනවද"));
    NEXT_WORD_PREDICTIONS.put("පොඩ්ඩක්", Arrays.asList("ඉන්න", "බලන්න", "අහන්න"));
    NEXT_WORD_PREDICTIONS.put("කතා", Arrays.asList("කරන්න", "කරමු", "කරලා"));
    NEXT_WORD_PREDICTIONS.put("වැඩ", Arrays.asList("ඉවරද", "කොහොමද", "කරන්න"));
    NEXT_WORD_PREDICTIONS.put("ගෙදර", Arrays.asList("යනවා", "ආවා", "ඉන්නේ"));
    NEXT_WORD_PREDICTIONS.put("උදේ", Arrays.asList("කෑවද", "නැගිට්ටා", "ආවා"));
    NEXT_WORD_PREDICTIONS.put("රෑ", Arrays.asList("වෙලා", "කෑම", "නිදාගන්න"));
    NEXT_WORD_PREDICTIONS.put("මොකක්ද", Arrays.asList("ප්‍රශ්නය", "වෙන්නේ", "කරන්නේ"));

    COMMON_WORDS_PREFIX_ORDER.addAll(COMMON_WORDS.entrySet());
    Collections.sort(COMMON_WORDS_PREFIX_ORDER, (a, b) -> {
      int lengthComparison = Integer.compare(a.getKey().length(), b.getKey().length());
      return lengthComparison != 0 ? lengthComparison : a.getKey().compareTo(b.getKey());
    });
  }

  /**
   * Return contextual next-word predictions for a given Sinhala word.
   */
  public static List<String> getNextWordPredictions(String word)
  {
    if (word == null || word.isEmpty())
      return java.util.Collections.emptyList();

    // Suggestions are requested with the committed candidate, which may include
    // sentence punctuation or autocapitalisation. Normalize only the token edges
    // so ordinary punctuation does not disable an otherwise valid prediction.
    String trimmed = word.trim();
    int start = 0;
    int end = trimmed.length();
    while (start < end && isBoundaryPunctuation(trimmed.charAt(start)))
      start++;
    while (end > start && isBoundaryPunctuation(trimmed.charAt(end - 1)))
      end--;
    if (start == end)
      return java.util.Collections.emptyList();

    trimmed = trimmed.substring(start, end);
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (NEXT_WORD_PREDICTIONS.containsKey(trimmed))
      return NEXT_WORD_PREDICTIONS.get(trimmed);
    if (NEXT_WORD_PREDICTIONS.containsKey(lower))
      return NEXT_WORD_PREDICTIONS.get(lower);

    String transliterated = transliterateWord(lower);
    if (NEXT_WORD_PREDICTIONS.containsKey(transliterated))
      return NEXT_WORD_PREDICTIONS.get(transliterated);

    List<String> enginePreds = SinhalaPredictionEngine.getPredictions(trimmed, null);
    if (!enginePreds.isEmpty())
      return enginePreds;
    return SinhalaPredictionEngine.getPredictions(transliterated, null);
  }

  public static List<String> getPredictionsForContext(String precedingText, SinhalaLearner learner)
  {
    return SinhalaPredictionEngine.getPredictions(precedingText, learner);
  }

  private static boolean isBoundaryPunctuation(char c)
  {
    int type = Character.getType(c);
    return Character.isWhitespace(c)
        || type == Character.CONNECTOR_PUNCTUATION
        || type == Character.DASH_PUNCTUATION
        || type == Character.START_PUNCTUATION
        || type == Character.END_PUNCTUATION
        || type == Character.INITIAL_QUOTE_PUNCTUATION
        || type == Character.FINAL_QUOTE_PUNCTUATION
        || type == Character.OTHER_PUNCTUATION;
  }

  // Standalone vowels (start of word or preceded by vowel)
  private static final String[][] VOWELS_INDEPENDENT = {
    {"aae", "\u0D88"}, // ඈ
    {"aau", "\u0D96"}, // ඖ
    {"ae", "\u0D87"},  // ඇ
    {"æ", "\u0D87"},   // ඇ
    {"ǣ", "\u0D88"},   // ඈ
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
    {"aau", "\u0DDE"}, // ෞ
    {"ae", "\u0DD0"},  // ැ
    {"æ", "\u0DD0"},   // ැ
    {"ǣ", "\u0DD1"},   // ෑ
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
    {"nndh", "\u0DB3"}, // ඳ
    {"shh", "\u0DC2\u0DCA"}, // ෂ්
    {"chh", "\u0DA1"}, // ඡ
    {"nng", "\u0D9F"},  // ඟ
    {"mmb", "\u0DB9"},  // ඹ
    {"nnd", "\u0DAC"},  // ඬ
    {"kh", "\u0D9B"},  // ඛ
    {"Kh", "\u0D9B"},  // ඛ
    {"gh", "\u0D9D"},  // ඝ
    {"Gh", "\u0D9D"},  // ඝ
    {"Ch", "\u0DA1"},  // ඡ
    {"ch", "\u0DA0"},  // ච
    {"jh", "\u0DA3"},  // ඣ
    {"Jh", "\u0DA3"},  // ඣ
    {"Th", "\u0DAE"},  // ථ
    {"Dh", "\u0DAA"},  // ඪ
    {"th", "\u0DAD"},  // ත
    {"dh", "\u0DB0"},  // ධ
    {"ph", "\u0DB5"},  // ඵ
    {"Ph", "\u0DB5"},  // ඵ
    {"bh", "\u0DB7"},  // භ
    {"Bh", "\u0DB7"},  // භ
    {"sh", "\u0DC1"},  // ශ
    {"Sh", "\u0DC2"},  // ෂ
    {"ny", "\u0DA4"},  // ඤ
    {"gn", "\u0DA4"},  // ඤ
    {"kn", "\u0DA4"},  // ඤ
    {"jn", "\u0DA5"},  // ඥ
    {"nD", "\u0DAC"},  // ඬ
    {"mb", "\u0DB9"},  // ඹ
    {"ng", "\u0D9F"},  // ඟ
    {"k", "\u0D9A"},   // ක
    {"K", "\u0D9A"},   // ක
    {"g", "\u0D9C"},   // ග
    {"G", "\u0D9C"},   // ග
    {"c", "\u0DA0"},   // ච
    {"C", "\u0DA0"},   // ච
    {"j", "\u0DA2"},   // ජ
    {"J", "\u0DA2"},   // ජ
    {"T", "\u0DA8"},   // ඨ
    {"t", "\u0DA7"},   // ට
    {"D", "\u0DA9"},   // ඩ
    {"d", "\u0DAF"},   // ද
    {"N", "\u0DAB"},   // ණ
    {"n", "\u0DB1"},   // න
    {"p", "\u0DB4"},   // ප
    {"P", "\u0DB4"},   // ප
    {"b", "\u0DB6"},   // බ
    {"B", "\u0DB6"},   // බ
    {"m", "\u0DB8"},   // ම
    {"M", "\u0DB8"},   // ම
    {"y", "\u0DBA"},   // ය
    {"Y", "\u0DBA"},   // ය
    {"r", "\u0DBB"},   // ර
    {"R", "\u0DBB"},   // ර
    {"L", "\u0DC5"},   // ළ
    {"l", "\u0DBD"},   // ල
    {"v", "\u0DC0"},   // ව
    {"V", "\u0DC0"},   // ව
    {"w", "\u0DC0"},   // ව
    {"W", "\u0DC0"},   // ව
    {"s", "\u0DC3"},   // ස
    {"S", "\u0DC2"},   // ෂ
    {"h", "\u0DC4"},   // හ
    {"H", "\u0DC4"},   // හ
    {"f", "\u0DC6"},   // ෆ
    {"F", "\u0DC6"},   // ෆ
    {"x", "\u0D82"},   // ං (Anusvaraya)
    {"X", "\u0D82"}    // ං (Anusvaraya)
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

    String lower = word.toLowerCase(Locale.ROOT);
    if (COMMON_WORDS.containsKey(lower))
    {
      return COMMON_WORDS.get(lower);
    }

    // Handle initial capital letter caused by autocapitalisation (e.g. Mama, Kohomada, Lanka, Api, Oya, Suba)
    if (word.length() > 1 && Character.isUpperCase(word.charAt(0)) && Character.isLowerCase(word.charAt(1)))
    {
      String lowerFirst = Character.toLowerCase(word.charAt(0)) + word.substring(1);
      if (COMMON_WORDS.containsKey(lowerFirst.toLowerCase(Locale.ROOT)))
      {
        return COMMON_WORDS.get(lowerFirst.toLowerCase(Locale.ROOT));
      }
      return parsePhonetic(lowerFirst);
    }

    return parsePhonetic(word);
  }

  /**
   * Return candidate suggestions for a typed word (Helakuru-like):
   * 0: Best prefix completion or primary transliteration (e.g. කොළඹ for colom, ලංකාව for lank)
   * 1: Phonetic variation or literal typed transliteration (e.g. සැ for sae, සෑ for saae)
   * 2: Original typed English word (e.g. colom, sae)
   */
  public static java.util.List<String> getSuggestions(String word)
  {
    java.util.List<String> list = new java.util.ArrayList<>();
    if (word == null || word.isEmpty())
      return list;

    String trimmed = word.trim();
    String lower = trimmed.toLowerCase(Locale.ROOT);

    // If word starts with capital letter, also consider lowercased version for suggestions
    String lowerFirst = (trimmed.length() > 1 && Character.isUpperCase(trimmed.charAt(0)))
        ? Character.toLowerCase(trimmed.charAt(0)) + trimmed.substring(1)
        : null;

    // 1. Direct match in COMMON_WORDS
    if (COMMON_WORDS.containsKey(lower))
    {
      list.add(COMMON_WORDS.get(lower));
    }

    // 2. Direct transliteration (primary interpretation)
    String primary = transliterateWord(trimmed);
    if (!list.contains(primary))
    {
      list.add(primary);
    }

    // 3. Prefix autocompletion from dictionary for places, countries, names, and frequent words
    // e.g. "colom" -> "කොළඹ", "lank" -> "ලංකාව", "kand" -> "මහනුවර", "saep" -> "සැප"
    if (lower.length() >= 2)
    {
      // Preserve deterministic priority without per-keystroke sorting and list allocations.
      for (Map.Entry<String, String> entry : COMMON_WORDS_PREFIX_ORDER)
      {
        String k = entry.getKey();
        if (k.length() > lower.length() && k.startsWith(lower))
        {
          String cand = entry.getValue();
          if (!list.contains(cand))
          {
            // If prefix matches a full word, give it prominent placement
            if (list.size() == 1 && !COMMON_WORDS.containsKey(lower))
            {
              list.add(0, cand);
            }
            else
            {
              list.add(cand);
            }
          }
          if (list.size() >= 4)
            break;
        }
      }
    }

    // 4. Phonetic alternative variations
    String phonetic = parsePhonetic(trimmed);
    if (!phonetic.isEmpty() && !list.contains(phonetic))
    {
      list.add(phonetic);
    }
    if (lowerFirst != null)
    {
      String phoneticLower = parsePhonetic(lowerFirst);
      if (!phoneticLower.isEmpty() && !list.contains(phoneticLower))
      {
        list.add(phoneticLower);
      }
    }

    // Special vowel variations for 'ැ' / 'ෑ' (e.g. sae -> සැ / සෑ, se -> සෙ / සැ)
    if (primary.contains("\u0DD0")) // contains ැ
    {
      String altAe = primary.replace("\u0DD0", "\u0DD1"); // replace ැ with ෑ
      if (!list.contains(altAe))
        list.add(altAe);
    }
    else if (primary.contains("\u0DD9")) // contains ෙ
    {
      String altE = primary.replace("\u0DD9", "\u0DD0"); // suggest ැ variant (e.g. sepa -> සැප)
      if (!list.contains(altE))
        list.add(altE);
    }
    else if (primary.equals("\u0D85")) // අ -> ආ
    {
      if (!list.contains("\u0D86")) list.add("\u0D86");
    }
    else if (primary.equals("\u0D86")) // ආ -> අ
    {
      if (!list.contains("\u0D85")) list.add("\u0D85");
    }
    else if (primary.equals("\u0D87")) // ඇ -> ඈ
    {
      if (!list.contains("\u0D88")) list.add("\u0D88");
    }
    else if (primary.equals("\u0DB8\u0DCA")) // ම් -> ම
    {
      if (!list.contains("\u0DB8")) list.add(0, "\u0DB8");
    }
    else if (primary.equals("\u0D9A\u0DCA")) // ක් -> ක
    {
      if (!list.contains("\u0D9A")) list.add(0, "\u0D9A");
    }

    // 5. Always include original typed English word if not present
    if (!list.contains(trimmed))
    {
      list.add(trimmed);
    }

    return list;
  }

  /**
   * Pure systematic phonetic parser (independent of common word dictionary).
   */
  public static String parsePhonetic(String word)
  {
    if (word == null || word.isEmpty())
      return "";

    // Preserve standalone aspirated consonant input as a full Sinhala letter.
    if ("dh".equals(word))
      return "\\u0DB0"; // ධ

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
      // Must not happen at the beginning of a word as Anusvara cannot start a word
      if (i > 0 && out.length() > 0 && i + 1 < len && (word.charAt(i) == 'n' || word.charAt(i) == 'N'))
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
