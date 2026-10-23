package juloo.keyboard2;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class Theme
{
  // Key colors
  public final int colorKey;
  public final int colorKeyActivated;
  public final int colorKeyAction;
  public final int colorKeySpaceBar;

  // Optional keyboard background gradient
  public final boolean hasKeyboardGradient;
  public final int keyboardGradientStart;
  public final int keyboardGradientEnd;

  // Label colors
  public final int lockedColor;
  public final int activatedColor;
  public final int pressedColor;
  public final int labelColor;
  public final int subLabelColor;
  public final int secondaryLabelColor;
  public final int greyedLabelColor;

  // Key borders
  public final float keyBorderRadius;
  public final float keyBorderWidth;
  public final float keyBorderWidthActivated;
  public final float keyBorderWidthAction;
  public final float keyBorderWidthSpaceBar;
  public final int keyBorderColorLeft;
  public final int keyBorderColorTop;
  public final int keyBorderColorRight;
  public final int keyBorderColorBottom;

  public final int colorNavBar;
  public final boolean isLightNavBar;

  public Theme(Context context, AttributeSet attrs)
  {
    getKeyFont(context); // _key_font will be accessed
    TypedArray s = context.getTheme().obtainStyledAttributes(attrs, R.styleable.keyboard, 0, 0);
    hasKeyboardGradient = s.hasValue(R.styleable.keyboard_keyboardGradientStart) && s.hasValue(R.styleable.keyboard_keyboardGradientEnd);
    keyboardGradientStart = s.getColor(R.styleable.keyboard_keyboardGradientStart, 0);
    keyboardGradientEnd = s.getColor(R.styleable.keyboard_keyboardGradientEnd, 0);
    colorKey = s.getColor(R.styleable.keyboard_colorKey, 0);
    colorKeyActivated = s.getColor(R.styleable.keyboard_colorKeyActivated, 0);
    colorKeyAction = s.getColor(R.styleable.keyboard_colorKeyAction, colorKey);
    colorKeySpaceBar = s.getColor(R.styleable.keyboard_colorKeySpaceBar, colorKey);
    // colorKeyboard = s.getColor(R.styleable.keyboard_colorKeyboard, 0);
    colorNavBar = s.getColor(R.styleable.keyboard_navigationBarColor, 0);
    isLightNavBar = s.getBoolean(R.styleable.keyboard_windowLightNavigationBar, false);
    labelColor = s.getColor(R.styleable.keyboard_colorLabel, 0);
    activatedColor = s.getColor(R.styleable.keyboard_colorLabelActivated, 0);
    pressedColor = s.getColor(R.styleable.keyboard_colorLabelPressed, labelColor);
    lockedColor = s.getColor(R.styleable.keyboard_colorLabelLocked, 0);
    subLabelColor = s.getColor(R.styleable.keyboard_colorSubLabel, 0);
    secondaryLabelColor = adjustLight(labelColor,
        s.getFloat(R.styleable.keyboard_secondaryDimming, 0.25f));
    greyedLabelColor = adjustLight(labelColor,
        s.getFloat(R.styleable.keyboard_greyedDimming, 0.5f));
    keyBorderRadius = s.getDimension(R.styleable.keyboard_keyBorderRadius, 0);
    keyBorderWidth = s.getDimension(R.styleable.keyboard_keyBorderWidth, 0);
    keyBorderWidthActivated = s.getDimension(R.styleable.keyboard_keyBorderWidthActivated, 0);
    keyBorderWidthAction = s.getDimension(R.styleable.keyboard_keyBorderWidthAction, 0);
    keyBorderWidthSpaceBar = s.getDimension(R.styleable.keyboard_keyBorderWidthSpaceBar, 0);
    keyBorderColorLeft = s.getColor(R.styleable.keyboard_keyBorderColorLeft, colorKey);
    keyBorderColorTop = s.getColor(R.styleable.keyboard_keyBorderColorTop, colorKey);
    keyBorderColorRight = s.getColor(R.styleable.keyboard_keyBorderColorRight, colorKey);
    keyBorderColorBottom = s.getColor(R.styleable.keyboard_keyBorderColorBottom, colorKey);
    s.recycle();
  }

  /** Calculate high contrast text color (dark or light) for a given background color. */
  public static int getContrastColor(int color)
  {
    int r = (color >> 16) & 0xFF;
    int g = (color >> 8) & 0xFF;
    int b = color & 0xFF;
    double lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
    return lum > 0.55 ? 0xFF121212 : 0xFFFFFFFF;
  }

  /** Compute a harmonious keycap color relative to custom background. */
  public static int computeKeyColor(int bgColor, int fallbackThemeColor)
  {
    int r = (bgColor >> 16) & 0xFF;
    int g = (bgColor >> 8) & 0xFF;
    int b = bgColor & 0xFF;
    double lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
    if (lum < 0.5)
      return blend(bgColor, 0xFFFFFFFF, 0.14f);
    else
      return blend(bgColor, 0xFF000000, 0.10f);
  }

  public static int blend(int color1, int color2, float ratio)
  {
    float inv = 1f - ratio;
    int r1 = (color1 >> 16) & 0xFF;
    int g1 = (color1 >> 8) & 0xFF;
    int b1 = color1 & 0xFF;

    int r2 = (color2 >> 16) & 0xFF;
    int g2 = (color2 >> 8) & 0xFF;
    int b2 = color2 & 0xFF;

    int r = (int)(r1 * inv + r2 * ratio);
    int g = (int)(g1 * inv + g2 * ratio);
    int b = (int)(b1 * inv + b2 * ratio);
    r = Math.min(255, Math.max(0, r));
    g = Math.min(255, Math.max(0, g));
    b = Math.min(255, Math.max(0, b));
    return 0xFF000000 | (r << 16) | (g << 8) | b;
  }

  /** Interpolate the 'value' component toward its opposite by 'alpha'. */
  public static int adjustLight(int color, float alpha)
  {
    float[] hsv = new float[3];
    Color.colorToHSV(color, hsv);
    float v = hsv[2];
    hsv[2] = alpha - (2 * alpha - 1) * v;
    return Color.HSVToColor(hsv);
  }

  Paint initIndicationPaint(Paint.Align align, Typeface font)
  {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setTextAlign(align);
    if (font != null)
      paint.setTypeface(font);
    return (paint);
  }

  static Typeface _key_font = null;

  static public Typeface getKeyFont(Context context)
  {
    if (_key_font == null)
      _key_font = Typeface.createFromAsset(context.getAssets(), "special_font.ttf");
    return _key_font;
  }

  public static final class Computed
  {
    public final float vertical_margin;
    public final float horizontal_margin;
    public final float margin_top;
    public final float margin_left;
    public final float row_height;
    public final Paint keyboard_background_paint;
    public final Paint indication_paint;

    public final Key key;
    public final Key key_activated;
    public final Key key_action;
    public final Key key_space_bar;
    public final Key key_suggestion;

    public Computed(Theme theme, Config config, float keyWidth, KeyboardData layout)
    {
      // Make sure that the layout isn't higher than the screen. Take the
      // height of the candidates view into account.
      row_height = Math.min(config.keyboard_rows_height_pixels,
          (config.screenHeightPixels - config.keyboard_rows_height_pixels) / layout.keysHeight);
      vertical_margin = config.key_vertical_margin * row_height;
      horizontal_margin = config.key_horizontal_margin * keyWidth;
      // Add half of the key margin on the left and on the top as it's also
      // added on the right and on the bottom of every keys.
      margin_top = config.marginTop + vertical_margin / 2;
      margin_left = horizontal_margin / 2;
      if (config.custom_colors_enabled)
      {
        keyboard_background_paint = new Paint();
        keyboard_background_paint.setColor(config.custom_bg_color);
        keyboard_background_paint.setAlpha(config.keyboardOpacity);
      }
      else
      {
        keyboard_background_paint = init_keyboard_background_paint(theme, margin_top + row_height * layout.keysHeight + config.margin_bottom);
      }
      key = new Key(theme, config, keyWidth, false, KeyboardData.Key.Role.Normal);
      key_action = new Key(theme, config, keyWidth, false, KeyboardData.Key.Role.Action);
      key_space_bar = new Key(theme, config, keyWidth, false, KeyboardData.Key.Role.Space_bar);
      key_activated = new Key(theme, config, keyWidth, true, KeyboardData.Key.Role.Normal);
      key_suggestion = new Key(theme, config, keyWidth, false, KeyboardData.Key.Role.Suggestion);
      indication_paint = init_label_paint(config, null);
      if (config.custom_colors_enabled)
        indication_paint.setColor(adjustLight(getContrastColor(config.custom_bg_color), 0.35f));
      else
        indication_paint.setColor(theme.subLabelColor);
    }

    static Paint init_keyboard_background_paint(Theme theme, float height)
    {
      if (!theme.hasKeyboardGradient)
        return null;

      Paint p = new Paint();
      p.setShader(new LinearGradient(
              0,
              0,
              0,
              height,
              theme.keyboardGradientStart,
              theme.keyboardGradientEnd,
              Shader.TileMode.CLAMP));
      return p;
    }

    public static final class Key
    {
      public final Paint bg_paint = new Paint();
      public final Paint border_left_paint;
      public final Paint border_top_paint;
      public final Paint border_right_paint;
      public final Paint border_bottom_paint;
      public final float border_width;
      public final float border_radius;
      final Paint _label_paint;
      final Paint _special_label_paint;
      final Paint _sublabel_paint;
      final Paint _special_sublabel_paint;
      final int _label_alpha_bits;

      public Key(Theme theme, Config config, float keyWidth, boolean activated,
          KeyboardData.Key.Role role)
      {
        if (config.borderConfig)
          border_radius = config.customBorderRadius * keyWidth;
        else
        {
          switch (config.keyShape)
          {
            case "square": border_radius = 0; break;
            case "rounded": border_radius = keyWidth * 0.16f; break;
            case "pill": border_radius = keyWidth * 0.5f; break;
            default: border_radius = theme.keyBorderRadius; break;
          }
        }
        if (activated)
        {
          border_width = theme.keyBorderWidthActivated;
          int actColor = config.custom_colors_enabled ? config.custom_accent_color : theme.colorKeyActivated;
          bg_paint.setColor(actColor);
          bg_paint.setAlpha(config.keyActivatedOpacity);
        }
        else
        {
          switch (role)
          {
            case Action:
              int actionColor = config.custom_colors_enabled ? config.custom_accent_color : theme.colorKeyAction;
              bg_paint.setColor(actionColor);
              border_width = theme.keyBorderWidthAction;
              break;
            case Space_bar:
              int spaceColor = config.custom_colors_enabled ? computeKeyColor(config.custom_bg_color, theme.colorKeySpaceBar) : theme.colorKeySpaceBar;
              bg_paint.setColor(spaceColor);
              border_width = theme.keyBorderWidthSpaceBar;
              break;
            case Suggestion:
              bg_paint.setColor(0);
              border_width = 0;
              break;
            default:
              int normalKeyColor = config.custom_colors_enabled ? computeKeyColor(config.custom_bg_color, theme.colorKey) : theme.colorKey;
              bg_paint.setColor(normalKeyColor);
              border_width = config.borderConfig ? config.customBorderLineWidth : theme.keyBorderWidth;
              break;
          }
          bg_paint.setAlpha(config.keyOpacity);
        }
        int bColor;
        if (config.custom_colors_enabled)
        {
          int bg = config.custom_bg_color;
          boolean isDark = ((0.299 * ((bg >> 16) & 0xFF) + 0.587 * ((bg >> 8) & 0xFF) + 0.114 * (bg & 0xFF)) / 255.0) < 0.5;
          bColor = blend(config.custom_bg_color, isDark ? 0xFFFFFFFF : 0xFF000000, 0.08f);
        }
        else
        {
          bColor = theme.keyBorderColorLeft;
        }
        border_left_paint = init_border_paint(config, border_width, bColor);
        border_top_paint = init_border_paint(config, border_width, bColor);
        border_right_paint = init_border_paint(config, border_width, bColor);
        border_bottom_paint = init_border_paint(config, border_width, bColor);
        _label_paint = init_label_paint(config, null);
        _special_label_paint = init_label_paint(config, _key_font);
        _sublabel_paint = init_label_paint(config, null);
        _special_sublabel_paint = init_label_paint(config, _key_font);
        _label_alpha_bits = (config.labelBrightness & 0xFF) << 24;
      }

      public Paint label_paint(boolean special_font, int color, float text_size)
      {
        Paint p = special_font ? _special_label_paint : _label_paint;
        p.setColor((color & 0x00FFFFFF) | _label_alpha_bits);
        p.setTextSize(text_size);
        return p;
      }

      public Paint sublabel_paint(boolean special_font, int color, float text_size, Paint.Align align)
      {
        Paint p = special_font ? _special_sublabel_paint : _sublabel_paint;
        p.setColor((color & 0x00FFFFFF) | _label_alpha_bits);
        p.setTextSize(text_size);
        p.setTextAlign(align);
        return p;
      }
    }

    static Paint init_border_paint(Config config, float border_width, int color)
    {
      Paint p = new Paint();
      p.setAlpha(config.keyOpacity);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(border_width);
      p.setColor(color);
      return p;
    }

    static Paint init_label_paint(Config config, Typeface font)
    {
      Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
      p.setTextAlign(Paint.Align.CENTER);
      if (font != null)
        p.setTypeface(font);
      else if (config.custom_font != null)
        p.setTypeface(config.custom_font);
      if (config.high_contrast_labels)
        p.setFakeBoldText(true);
      return p;
    }
  }
}
