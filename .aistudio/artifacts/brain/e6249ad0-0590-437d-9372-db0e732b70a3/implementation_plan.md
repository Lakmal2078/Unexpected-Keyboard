# Categorized Theme Gallery & Automatic Day/Night Switching

A comprehensive plan to reorganize and streamline Unexpected Keyboard's theme ecosystem with a categorized preview gallery, instant category filtering pills, and automatic system Day/Night switching.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following directions were confirmed during interactive clarification:

- **Confirmed Decision 1**: Organize theme selection into a **Categorized gallery with live keyboard previews**.
- **Confirmed Decision 2**: Prioritize **Automatic Day and Night switching following system mode**.
- **Architectural Decision (Instant Responsiveness)**: Add category filter chips (`All`, `🌟 Adaptive/System`, `🌙 Dark`, `☀️ Light`, `🎨 Vibrant`) to `ThemePicker.java` that filter the gallery dynamically without reloading or dialog stutter.
- **Architectural Decision (Configuration Lifecycle)**: Hook `onConfigurationChanged` in `Keyboard2.java` to seamlessly switch themes when Android toggles dark/light mode, eliminating manual reloads.

---

## 1. Overview & Core Concept

- **What It Does**: Transforms the 50+ theme selection from a long, unorganized list into an intuitive, categorized visual gallery with real-time interactive preview cards. It enhances the `system` and `monet` themes to automatically switch between daytime light palettes and nighttime dark palettes in sync with Android OS dark mode.
- **Target Audience**: Users wanting an aesthetically pleasing keyboard that matches their device theme and is effortless to customize.
- **Key Value**: Reduces visual clutter, makes finding favorite themes effortless, and provides automated dark mode switching so users never get blinded by bright themes at night.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Browsing Categorized Themes in Settings**:
   - User opens **Settings -> Theme**.
   - A modern gallery dialog appears with category filter chips at the top:
     - `All` (All 51 themes)
     - `🌟 System & Adaptive` (System Auto Day/Night, Monet Material You, High Contrast)
     - `🌙 Dark & AMOLED` (Graphite Pro, Dark, Pure Black, OLED, Dracula, Tokyo Night, Nord, Catppuccin Mocha)
     - `☀️ Light & Minimal` (Light, White, Catppuccin Latte, Everforest, Sepia Warm, Zen Minimal)
     - `🎨 Vibrant & Heritage` (Lanka Maroon, Lanka Blue, Royal Gold, Sinhala Sand, Emerald Forest, Cyberpunk Neon)
   - Tapping any chip filters the cards smoothly.
2. **Live Keyboard Previews**:
   - Each card displays the theme's name, category tag, selected checkmark `✓`, and an accurate scaled 4-row keyboard preview showing background, keycaps, and action accents.
   - An interactive "Key borders" toggle switch at the bottom previews with/without borders in real time.
   - Tapping "Apply" commits the selection without flickering.
3. **Automatic System Day/Night Adaptation**:
   - When "System" or "Material You (Monet)" is active, turning on Android Dark Mode instantly switches the keyboard to the dark variant.
   - Turning off Dark Mode during the daytime switches it back to the light variant without restarting the keyboard or losing typed text.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Category Filter Chips vs Multi-Page Pager**
  - *Chosen Approach*: Horizontal chip selector at the top of the gallery dialog with instant view filtering.
  - *Why*: Fast, intuitive, lightweight, and works on all Android screen sizes without heavy ViewPager dependencies.

- **Decision 2: Automatic Lifecycle Day/Night Hook**
  - *Chosen Approach*: Override `onConfigurationChanged` in `Keyboard2.java` to invoke `refresh_config()`.
  - *Why*: Android broadcasts configuration changes (such as `uiMode` night/day transitions) to active InputMethodServices; handling it directly provides instant, zero-touch switching.

- **Decision 3: Backward-Compatible Color Extraction**
  - *Chosen Approach*: Use `Theme.obtainStyledAttributes` via safe `newTheme()` so all 50+ themes correctly render their styled preview colors in unit tests and on physical devices.

---

## 4. Technical Architecture & Component Mapping

```
┌────────────────────────────────────────────────────────┐
│                   Android OS System                    │
│        (Dark Mode Toggle / System Configuration)       │
└───────────────────────────┬────────────────────────────┘
                            │ onConfigurationChanged(newConfig)
                            ▼
┌────────────────────────────────────────────────────────┐
│                     Keyboard2.java                     │
│  - Catches OS Night/Day transitions                    │
│  - Calls refresh_config()                              │
│  - Compares prev_theme != current_theme                │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                      Config.java                       │
│  - getThemeId(res, theme_name)                         │
│  - Resolves "system" -> Light (day) / Dark (night)     │
│  - Resolves "monet" -> MonetLight / MonetDark          │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                    ThemePicker.java                    │
│  - Category Filter Bar: [All|System|Dark|Light|Vibrant]│
│  - Filtered Gallery of live keyboard preview cards     │
│  - Real-time "Key borders" toggle                      │
│  - "Apply" / "Cancel" actions                          │
└────────────────────────────────────────────────────────┘
```

### Component Breakdown & Execution Plan

1. **`ThemePicker.java`**:
   - Define a clean `ThemeCategory` enumeration (`SYSTEM`, `DARK`, `LIGHT`, `VIBRANT`).
   - Map all 51 theme keys to their respective categories.
   - Add horizontal chip selector above the gallery.
   - Implement category filtering so only relevant cards are shown while maintaining selected state.
   - Preserve test contracts (`ThemePicker.create(builder, resources, prefs)` and `ThemePickerTest.java`).
2. **`Keyboard2.java`**:
   - Implement `onConfigurationChanged(Configuration newConfig)`.
   - Call `refresh_config()` when UI mode night mask changes.
3. **`Config.java`**:
   - Validate and ensure `system` and `monet` mappings cleanly check `Configuration.UI_MODE_NIGHT_MASK`.
4. **Validation & Testing**:
   - Run `gradle test` to verify `ThemePickerTest` and all 148 unit tests.
   - Verify `compile_applet`.
   - Run `python3 check_theme_contrast.py` to ensure all themes maintain WCAG AA contrast compliance.
