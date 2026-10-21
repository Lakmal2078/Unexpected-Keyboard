package juloo.keyboard2.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import juloo.keyboard2.R;

public class ColorPickerPreference extends DialogPreference
    implements SeekBar.OnSeekBarChangeListener
{
  private int _currentColor;
  private int _selectedColor;
  private int _defaultColor = 0xFF1E1E2E;

  private View _previewSwatch;
  private EditText _hexInput;
  private SeekBar _seekR;
  private SeekBar _seekG;
  private SeekBar _seekB;
  private TextView _textR;
  private TextView _textG;
  private TextView _textB;

  private boolean _isUpdating = false;

  private static final int[] PRESETS = new int[] {
      0xFF000000, // OLED Black
      0xFF1E1E2E, // Catppuccin Dark
      0xFF282C34, // One Dark
      0xFF1A1B26, // Tokyo Night
      0xFF0D1B2A, // Midnight Navy
      0xFF3D5AFE, // Electric Blue
      0xFF00BCD4, // Cyan
      0xFF00E676, // Spring Mint
      0xFF4CAF50, // Material Green
      0xFFFFD600, // Cyber Yellow
      0xFFFF9800, // Vibrant Orange
      0xFFF44336, // Crimson Red
      0xFFFF4081, // Neon Pink
      0xFF9C27B0, // Vivid Purple
      0xFF7C4DFF, // Deep Violet
      0xFFF5E6D3, // Warm Cream
      0xFFE0E0E0, // Light Gray
      0xFFFFFFFF  // Snow White
  };

  public ColorPickerPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setWidgetLayoutResource(R.layout.pref_color_picker_widget);
    setPositiveButtonText(android.R.string.ok);
    setNegativeButtonText(android.R.string.cancel);

    if (attrs != null)
    {
      String defaultVal = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "defaultValue");
      if (defaultVal != null)
      {
        try
        {
          if (defaultVal.startsWith("#"))
            _defaultColor = Color.parseColor(defaultVal);
          else
            _defaultColor = Integer.parseInt(defaultVal);
        }
        catch (Exception e) {}
      }
    }
    _currentColor = _defaultColor;
    _selectedColor = _defaultColor;
  }

  @Override
  protected void onBindView(View view)
  {
    super.onBindView(view);
    ImageView previewWidget = view.findViewById(R.id.color_preview_widget);
    if (previewWidget != null)
    {
      previewWidget.setImageDrawable(createColorCircleDrawable(_currentColor));
      previewWidget.setAlpha(isEnabled() ? 1.0f : 0.35f);
    }
    updateSummaryText();
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index)
  {
    String str = a.getString(index);
    if (str != null && str.startsWith("#"))
    {
      try
      {
        return Color.parseColor(str);
      }
      catch (Exception e) {}
    }
    return a.getInt(index, _defaultColor);
  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
  {
    if (restorePersistedValue)
    {
      _currentColor = getPersistedInt(_defaultColor);
    }
    else if (defaultValue instanceof Integer)
    {
      _currentColor = (Integer) defaultValue;
      _defaultColor = _currentColor;
      persistInt(_currentColor);
    }
    else
    {
      _currentColor = _defaultColor;
      persistInt(_currentColor);
    }
    _selectedColor = _currentColor;
    updateSummaryText();
  }

  private void updateSummaryText()
  {
    String hex = String.format("#%06X", (0xFFFFFF & _currentColor));
    setSummary(hex);
  }

  @Override
  protected View onCreateDialogView()
  {
    _selectedColor = _currentColor;
    Context ctx = getContext();
    int dp16 = dpToPx(ctx, 16);
    int dp8 = dpToPx(ctx, 8);

    ScrollView scrollView = new ScrollView(ctx);
    LinearLayout root = new LinearLayout(ctx);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp16, dp16, dp16, dp16);

    // 1. Color Preview Swatch
    _previewSwatch = new View(ctx);
    LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 50));
    swatchParams.bottomMargin = dp16;
    _previewSwatch.setLayoutParams(swatchParams);
    updatePreviewSwatch(_selectedColor);
    root.addView(_previewSwatch);

    // 2. Hex input row
    LinearLayout hexRow = new LinearLayout(ctx);
    hexRow.setOrientation(LinearLayout.HORIZONTAL);
    hexRow.setGravity(Gravity.CENTER_VERTICAL);
    hexRow.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    TextView hexLabel = new TextView(ctx);
    hexLabel.setText("HEX: ");
    hexLabel.setTextSize(16);
    hexLabel.setPadding(0, 0, dp8, 0);
    hexRow.addView(hexLabel);

    _hexInput = new EditText(ctx);
    _hexInput.setSingleLine(true);
    _hexInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    _hexInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(7) });
    _hexInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    _hexInput.setText(String.format("#%06X", (0xFFFFFF & _selectedColor)));
    _hexInput.addTextChangedListener(new TextWatcher()
    {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {}

      @Override
      public void afterTextChanged(Editable s)
      {
        if (_isUpdating) return;
        String text = s.toString().trim();
        if (!text.startsWith("#") && text.length() == 6)
          text = "#" + text;
        if (text.startsWith("#") && text.length() == 7)
        {
          try
          {
            int color = Color.parseColor(text);
            setColorInternal(color, false);
          }
          catch (Exception ignored) {}
        }
      }
    });
    hexRow.addView(_hexInput);

    Button btnReset = new Button(ctx, null, android.R.attr.borderlessButtonStyle);
    btnReset.setText("Reset");
    btnReset.setOnClickListener(v -> setColorInternal(_defaultColor, true));
    hexRow.addView(btnReset);

    root.addView(hexRow);

    // 3. Quick Presets
    TextView presetsTitle = new TextView(ctx);
    presetsTitle.setText("Presets");
    presetsTitle.setTextSize(14);
    presetsTitle.setPadding(0, dp8, 0, dp8);
    root.addView(presetsTitle);

    HorizontalScrollView presetScroll = new HorizontalScrollView(ctx);
    presetScroll.setHorizontalScrollBarEnabled(false);
    LinearLayout presetContainer = new LinearLayout(ctx);
    presetContainer.setOrientation(LinearLayout.HORIZONTAL);
    int swatchSize = dpToPx(ctx, 36);
    int swatchMargin = dpToPx(ctx, 4);

    for (int preset : PRESETS)
    {
      View pView = new View(ctx);
      LinearLayout.LayoutParams pParams = new LinearLayout.LayoutParams(swatchSize, swatchSize);
      pParams.setMargins(swatchMargin, swatchMargin, swatchMargin, swatchMargin);
      pView.setLayoutParams(pParams);
      pView.setBackground(createColorCircleDrawable(preset));
      pView.setOnClickListener(v -> setColorInternal(preset, true));
      presetContainer.addView(pView);
    }
    presetScroll.addView(presetContainer);
    root.addView(presetScroll);

    // 4. RGB SeekBars
    View divider = new View(ctx);
    LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 1));
    divParams.setMargins(0, dp16, 0, dp16);
    divider.setLayoutParams(divParams);
    divider.setBackgroundColor(0x33888888);
    root.addView(divider);

    _textR = new TextView(ctx);
    _seekR = createColorSeekBar(ctx, Color.red(_selectedColor));
    root.addView(_textR);
    root.addView(_seekR);

    _textG = new TextView(ctx);
    _seekG = createColorSeekBar(ctx, Color.green(_selectedColor));
    root.addView(_textG);
    root.addView(_seekG);

    _textB = new TextView(ctx);
    _seekB = createColorSeekBar(ctx, Color.blue(_selectedColor));
    root.addView(_textB);
    root.addView(_seekB);

    updateRgbLabels();

    scrollView.addView(root);
    return scrollView;
  }

  private SeekBar createColorSeekBar(Context ctx, int initialValue)
  {
    SeekBar bar = new SeekBar(ctx);
    bar.setMax(255);
    bar.setProgress(initialValue);
    bar.setOnSeekBarChangeListener(this);
    bar.setPadding(0, dpToPx(ctx, 6), 0, dpToPx(ctx, 12));
    return bar;
  }

  private void setColorInternal(int color, boolean updateHexField)
  {
    _isUpdating = true;
    _selectedColor = color;
    updatePreviewSwatch(color);

    if (_seekR != null) _seekR.setProgress(Color.red(color));
    if (_seekG != null) _seekG.setProgress(Color.green(color));
    if (_seekB != null) _seekB.setProgress(Color.blue(color));

    updateRgbLabels();

    if (updateHexField && _hexInput != null)
    {
      _hexInput.setText(String.format("#%06X", (0xFFFFFF & color)));
    }
    _isUpdating = false;
  }

  private void updatePreviewSwatch(int color)
  {
    if (_previewSwatch != null)
    {
      GradientDrawable gd = new GradientDrawable();
      gd.setCornerRadius(dpToPx(getContext(), 8));
      gd.setColor(color);
      gd.setStroke(dpToPx(getContext(), 2), 0x55888888);
      _previewSwatch.setBackground(gd);
    }
  }

  private void updateRgbLabels()
  {
    if (_textR != null && _seekR != null)
      _textR.setText("Red: " + _seekR.getProgress());
    if (_textG != null && _seekG != null)
      _textG.setText("Green: " + _seekG.getProgress());
    if (_textB != null && _seekB != null)
      _textB.setText("Blue: " + _seekB.getProgress());
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
  {
    if (!fromUser || _isUpdating) return;
    int r = _seekR.getProgress();
    int g = _seekG.getProgress();
    int b = _seekB.getProgress();
    int color = Color.rgb(r, g, b);
    setColorInternal(color, true);
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {}

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {}

  @Override
  protected void onDialogClosed(boolean positiveResult)
  {
    if (positiveResult)
    {
      if (callChangeListener(_selectedColor))
      {
        _currentColor = _selectedColor;
        persistInt(_currentColor);
        updateSummaryText();
        notifyChanged();
      }
    }
    else
    {
      _selectedColor = _currentColor;
    }
  }

  private static GradientDrawable createColorCircleDrawable(int color)
  {
    GradientDrawable gd = new GradientDrawable();
    gd.setShape(GradientDrawable.OVAL);
    gd.setColor(color);
    gd.setStroke(2, 0x44888888);
    return gd;
  }

  private static int dpToPx(Context context, int dp)
  {
    return (int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
  }
}
