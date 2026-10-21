package juloo.keyboard2;

import android.text.TextUtils;
import android.view.inputmethod.InputConnection;
import java.text.BreakIterator;

public class SinhalaInputHandler {

    /**
     * Web Browsers / WebViews තුළ Sinhala Unicode Text නිවැරදිව commit කිරීම.
     */
    public static void commitSinhalaSuggestion(InputConnection ic, String suggestionText, int composingLength) {
        if (ic == null || TextUtils.isEmpty(suggestionText)) {
            return;
        }

        // 1. Batch Edit භාවිතයෙන් Browser UI Lag / Flicker අවම කිරීම
        ic.beginBatchEdit();

        try {
            // 2. Composing Region එක Clear කිරීම
            ic.finishComposingText();

            if (composingLength > 0) {
                // 3. Sinhala Complex Unicode Graphemes (පිළි/බැඳි අකුරු/ZWJ) අනුව Delete කිරීම
                CharSequence beforeText = ic.getTextBeforeCursor(composingLength * 4, 0);
                int deleteCodeUnits = calculateGraphemeCodeUnits(beforeText, composingLength);

                if (deleteCodeUnits > 0) {
                    ic.deleteSurroundingText(deleteCodeUnits, 0);
                }
            }

            // 4. Sinhala Unicode Text එක Commit කිරීම
            ic.commitText(suggestionText, 1);

        } finally {
            ic.endBatchEdit();
        }
    }

    /**
     * Sinhala ZWJ (\u200D) සහ Compound Characters සඳහා Grapheme Code Units ගණනය කිරීම.
     */
    private static int calculateGraphemeCodeUnits(CharSequence text, int graphemeCount) {
        if (TextUtils.isEmpty(text)) {
            return 0;
        }

        BreakIterator iterator = BreakIterator.getCharacterInstance();
        iterator.setText(text.toString());

        int end = iterator.last();
        int count = 0;
        int targetOffset = end;

        while (count < graphemeCount && targetOffset != BreakIterator.DONE) {
            targetOffset = iterator.previous();
            if (targetOffset != BreakIterator.DONE) {
                count++;
            }
        }

        return targetOffset == BreakIterator.DONE ? text.length() : (end - targetOffset);
    }
}
