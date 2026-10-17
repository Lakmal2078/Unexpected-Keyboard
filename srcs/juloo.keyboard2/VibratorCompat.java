package juloo.keyboard2;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.View;

public final class VibratorCompat
{
  public static void vibrate(View v, Config config)
  {
    if (config.vibrate_custom)
    {
      if (config.vibrate_duration > 0)
        vibrator_vibrate(v, config.vibrate_duration);
    }
    else
    {
      boolean done = false;
      try
      {
        done = v.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
      }
      catch (Throwable t) {}

      if (!done)
      {
        try
        {
          done = v.performHapticFeedback(
              HapticFeedbackConstants.VIRTUAL_KEY,
              HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
        catch (Throwable t) {}
      }

      // If system haptics failed (e.g. system touch feedback disabled on device, or OEM suppressed it)
      // fallback to gentle hardware vibration (30ms)
      if (!done)
      {
        vibrator_vibrate(v, 30);
      }
    }
  }

  /**
   * Resilient hardware vibration supporting modern Android 10-15 LRA haptic motors,
   * VibrationAttributes (API 30+), AudioAttributes (API 26+), and fallback.
   */
  static void vibrator_vibrate(View v, long duration)
  {
    try
    {
      Context ctx = v != null ? v.getContext() : null;
      Vibrator vib = get_vibrator(ctx);
      if (vib == null || !vib.hasVibrator())
        return;

      // Ensure duration is perceptible (at least 35ms on linear resonant motors)
      long dur = Math.max(duration, 35);

      VibrationEffect effect = null;
      if (Build.VERSION.SDK_INT >= 29)
      {
        // Try hardware-tuned EFFECT_CLICK for small durations (<= 30ms)
        if (duration <= 30)
        {
          try
          {
            effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK);
          }
          catch (Throwable t) {}
        }
      }
      if (effect == null && Build.VERSION.SDK_INT >= 26)
      {
        effect = VibrationEffect.createOneShot(dur, VibrationEffect.DEFAULT_AMPLITUDE);
      }

      // 1. Android 13+ (API 33+) / Android 15: VibrationAttributes with USAGE_TOUCH & bypass interruption
      if (Build.VERSION.SDK_INT >= 33 && effect != null)
      {
        try
        {
          VibrationAttributes attrs = new VibrationAttributes.Builder()
              .setUsage(VibrationAttributes.USAGE_TOUCH)
              .setFlags(VibrationAttributes.FLAG_BYPASS_INTERRUPTION_POLICY,
                        VibrationAttributes.FLAG_BYPASS_INTERRUPTION_POLICY)
              .build();
          vib.vibrate(effect, attrs);
          return;
        }
        catch (Throwable t) {}
      }

      // 2. Android 8.0+ (API 26+): AudioAttributes with USAGE_ASSISTANCE_SONIFICATION
      if (Build.VERSION.SDK_INT >= 26 && effect != null)
      {
        try
        {
          AudioAttributes audioAttrs = new AudioAttributes.Builder()
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
              .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
              .build();
          vib.vibrate(effect, audioAttrs);
          return;
        }
        catch (Throwable t) {}

        // Fallback AudioAttributes with USAGE_ALARM (bypasses silent/DND profile restrictions)
        try
        {
          AudioAttributes alarmAttrs = new AudioAttributes.Builder()
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .setUsage(AudioAttributes.USAGE_ALARM)
              .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
              .build();
          vib.vibrate(effect, alarmAttrs);
          return;
        }
        catch (Throwable t) {}

        // Fallback: direct effect
        try
        {
          vib.vibrate(effect);
          return;
        }
        catch (Throwable t) {}
      }

      // 3. Legacy pre-Oreo fallback
      vib.vibrate(dur);
    }
    catch (Throwable t) {}
  }

  static Vibrator vibrator_service = null;

  static Vibrator get_vibrator(Context ctx)
  {
    if (vibrator_service != null)
      return vibrator_service;

    if (ctx == null)
      return null;

    Context appCtx = ctx.getApplicationContext();
    if (appCtx == null)
      appCtx = ctx;

    if (Build.VERSION.SDK_INT >= 31)
    {
      try
      {
        VibratorManager vm = (VibratorManager) appCtx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
        if (vm != null)
        {
          vibrator_service = vm.getDefaultVibrator();
          if (vibrator_service != null)
            return vibrator_service;
        }
      }
      catch (Throwable t) {}
    }

    try
    {
      vibrator_service = (Vibrator) appCtx.getSystemService(Context.VIBRATOR_SERVICE);
    }
    catch (Throwable t) {}

    return vibrator_service;
  }
}
