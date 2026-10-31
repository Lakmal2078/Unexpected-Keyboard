package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import java.util.Map;
import java.util.Set;

public final class DirectBootAwarePreferences
{
  /* On API >= 24, preferences are read from device-protected storage. This
   * storage is accessible during boot (before user credentials are entered) to
   * allow typing the storage password. Settings and bounded on-device learned bigrams
   * stored here never leave the device, are strictly bounded in size, and can be
   * cleared by the user. Sensitive inputs (passwords, credit cards, private mode)
   * are strictly excluded from learning. */
  public static SharedPreferences get_shared_preferences(Context context)
  {
    if (VERSION.SDK_INT < 24)
      return PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences prefs = get_protected_prefs(context);
    check_need_migration(context, prefs);
    return prefs;
  }

  /** Load the default preferences. */
  @androidx.annotation.RequiresApi(24)
  static SharedPreferences get_protected_prefs(Context context)
  {
    String pref_name =
      PreferenceManager.getDefaultSharedPreferencesName(context);
    return get_protected_prefs(context, pref_name);
  }

  /** Load the specified preferences. */
  public static SharedPreferences get_protected_prefs(Context context,
      String pref_name)
  {
    if (VERSION.SDK_INT >= 24)
      context = context.createDeviceProtectedStorageContext();
    return context.getSharedPreferences(pref_name, Context.MODE_PRIVATE);
  }

  static void check_need_migration(Context app_context,
      SharedPreferences protected_prefs)
  {
    if (!protected_prefs.getBoolean("need_migration", true))
      return;
    SharedPreferences prefs;
    try
    {
      prefs = PreferenceManager.getDefaultSharedPreferences(app_context);
    }
    catch (Exception e)
    {
      // Device is locked, migrate later.
      return;
    }
    prefs.edit().putBoolean("need_migration", false).apply();
    copy_shared_preferences(prefs, protected_prefs);
  }

  static void copy_shared_preferences(SharedPreferences src, SharedPreferences dst)
  {
    SharedPreferences.Editor e = dst.edit();
    Map<String, ?> entries = src.getAll();
    for (String k : entries.keySet())
    {
      Object v = entries.get(k);
      if (v instanceof Boolean)
        e.putBoolean(k, (Boolean)v);
      else if (v instanceof Float)
        e.putFloat(k, (Float)v);
      else if (v instanceof Integer)
        e.putInt(k, (Integer)v);
      else if (v instanceof Long)
        e.putLong(k, (Long)v);
      else if (v instanceof String)
        e.putString(k, (String)v);
      else if (v instanceof Set)
        e.putStringSet(k, (Set<String>)v);
    }
    e.apply();
  }
}
