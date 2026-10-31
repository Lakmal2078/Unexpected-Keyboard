package juloo.keyboard2.dict;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import juloo.cdict.Cdict;
import juloo.keyboard2.DeviceLocales;
import juloo.keyboard2.Logs;
import juloo.keyboard2.R;

public class DictionaryListView extends LinearLayout
{
  List<DictView> _dict_views;
  Dictionaries _dictionaries;
  Set<String> _pending = new HashSet<String>();

  // Bound decompressed dictionary data so a bad or compromised download cannot exhaust app memory.
  static final int MAX_DICTIONARY_BYTES = 64 * 1024 * 1024;

  public DictionaryListView(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    setOrientation(LinearLayout.VERTICAL);
    _dictionaries = Dictionaries.instance(ctx);
    _dict_views = new ArrayList<DictView>();
    boolean device_locales =
      attrs.getAttributeBooleanValue(null, "device_locales", true);
    if (device_locales)
      inflate_views_device_locales(ctx);
    else
      inflate_views_all(ctx);
    refresh();
  }

  void inflate_views_device_locales(Context ctx)
  {
    SupportedDictionaries ds = SupportedDictionaries.get(ctx.getResources());
    DeviceLocales locales = DeviceLocales.load(ctx);
    for (DeviceLocales.Loc loc : locales.installed)
    {
      if (loc.dictionary != null)
      {
        int idx = ds.find(loc.dictionary);
        if (idx >= 0)
          inflate_item(ctx, ds, idx);
      }
    }
  }

  void inflate_views_all(Context ctx)
  {
    SupportedDictionaries ds = SupportedDictionaries.get(ctx.getResources());
    for (int i = 0; i < ds.length(); i++)
      inflate_item(ctx, ds, i);
  }

  void inflate_item(Context ctx, SupportedDictionaries ds, int i)
  {
    View v = LayoutInflater.from(ctx)
      .inflate(R.layout.dictionary_download_item, this, false);
    _dict_views.add(this.new DictView(v, ds, i));
    addView(v);
  }

  /** Update the "installed" status of item views. Meaning whether the
      "download" or "delete" button is shown. */
  void refresh()
  {
    Set<String> installed = _dictionaries.get_installed();
    for (DictView d : _dict_views)
      d.refresh(installed);
  }

  void toggle_installed(String dict_name)
  {
    run_dictionary_action(dict_name, new Runnable()
        {
          public void run()
          {
            if (_dictionaries.get_installed().contains(dict_name))
              _dictionaries.uninstall(dict_name);
            else if (install_dictionary_from_internet(dict_name))
              post_toast(R.string.dictionaries_download_success);
            else
              post_toast(R.string.dictionaries_download_failed);
          }
        });
  }

  /** Run action [r] for dictionary [name] if no action is already running for
      that dictionary. Calls [refresh] after the action completed. */
  void run_dictionary_action(String name, Runnable r)
  {
    if (_pending.contains(name))
      return;
    _pending.add(name);
    (new Thread()
     {
       public void run()
       {
         r.run();
         post(new Runnable()
             {
               public void run()
               {
                 _pending.remove(name);
                 refresh();
               }
             });
       }
     }).start();
    refresh();
  }

  final class DictView implements View.OnClickListener
  {
    public final String dict_name;
    public final ImageView download_button;

    public DictView(View view, SupportedDictionaries ds, int dict_index)
    {
      dict_name = ds.dict_name(dict_index);
      float size_mb = ds.size(dict_index) / 1048576.f;
      ((TextView)view.findViewById(R.id.dictionary_download_locale))
        .setText(ds.display_name(dict_index));
      ((TextView)view.findViewById(R.id.dictionary_download_size))
        .setText(getContext().getString(R.string.dictionary_download_size_format,
            NumberFormat.getInstance().format(size_mb)));
      download_button =
        (ImageView)view.findViewById(R.id.dictionary_download_button);
      download_button.setOnClickListener(this);
    }

    public void refresh(Set<String> installed)
    {
      boolean is_installed = installed.contains(dict_name);
      int res =
        _pending.contains(dict_name) ? 0 :
        is_installed ? R.drawable.ic_delete :
        R.drawable.ic_download;
      download_button.setImageResource(res);
      download_button.setContentDescription(
          download_button.getContext().getString(
              is_installed ? R.string.dictionaries_delete_button : R.string.dictionaries_download_button));
    }

    @Override
    public void onClick(View v)
    {
      toggle_installed(dict_name);
    }
  }

  static final String DICT_REPO_URL =
    "https://raw.githubusercontent.com/Julow/Unexpected-Keyboard-dictionaries/refs/heads/main";

  static URL url_of_dictionary(String dict_name)
      throws MalformedURLException
  {
    int format_version = 1;
    try
    {
      format_version = Cdict.format_version();
    }
    catch (Throwable t) {}
    return new URL(DICT_REPO_URL + "/v" + format_version + "/" + dict_name
        + ".dict");
  }

  /** Returns [true] on success. */
  boolean install_dictionary_from_internet(String dict_name)
  {
    try
    {
      // Remote files are compressed with gzip at rest. Do not use server side
      // compression and force decompression.
      URLConnection con = url_of_dictionary(dict_name).openConnection();
      con.setConnectTimeout(15000);
      con.setReadTimeout(30000);
      con.setRequestProperty("Accept-Encoding", "identity");
      byte[] data;
      try (InputStream response = con.getInputStream();
           InputStream stream = new GZIPInputStream(response)) {
        data = read_bounded(stream, MAX_DICTIONARY_BYTES);
      }
      Cdict.of_bytes(data); // Structural and cyclic safety validation
      try
      {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        android.util.Log.i("juloo.keyboard2", "Verified dictionary " + dict_name + " SHA-256: " + sb.toString());
      }
      catch (Throwable ignored) {}
      _dictionaries.install(dict_name, data);
      return true;
    }
    catch (Exception e)
    {
      // If remote download is unavailable, check if the dictionary is bundled in assets
      try
      {
        byte[] data;
        try (InputStream inp = getContext().getAssets().open("dict/" + dict_name + ".dict")) {
          data = read_bounded(inp, MAX_DICTIONARY_BYTES);
        }
        Cdict.of_bytes(data);
        _dictionaries.install(dict_name, data);
        return true;
      }
      catch (Exception ignored) {}
      Logs.exn("", e);
      return false;
    }
  }

  /** Read a dictionary with a caller-supplied hard byte limit. Does not close [input]. */
  static byte[] read_bounded(InputStream input, int maxBytes) throws IOException
  {
    if (maxBytes < 0)
      throw new IllegalArgumentException("maxBytes must not be negative");
    ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
    byte[] buffer = new byte[8192];
    int total = 0;
    int read;
    while ((read = input.read(buffer)) != -1)
    {
      if (read > maxBytes - total)
        throw new IOException("Dictionary exceeds maximum size of " + maxBytes + " bytes");
      output.write(buffer, 0, read);
      total += read;
    }
    return output.toByteArray();
  }

  void post_toast(int msg_id)
  {
    post(new Runnable()
        {
          public void run()
          {
            Toast.makeText(getContext(), msg_id, Toast.LENGTH_SHORT).show();
          }
        });
  }
}
