## 2024-05-24 - Identifying Key Optimization
**Learning:** In Keyboard2View.java, the `onDraw` method loops through all keys and calls `drawKeyFrame`, `drawLabel`, `drawSubLabel` up to 8 times, and `drawIndication` for each key. The `drawKeyFrame` method uses `drawRoundRect` multiple times via `drawBorder` by using `clipRect` and `save()` / `restore()`. Also inside `drawLabel` and `drawSubLabel`, the `k.keys[0]` etc are modified with `modifyKey` over and over during every single draw call.
**Action:** The modification of keys in the draw loop is expensive because it invokes the modifier stack (`KeyModifier.modify`) on every render frame for every visible key and its sublabels, sometimes multiple times. A potential optimization is to compute the modified keys outside the draw loop or cache them. Wait, modifiers change dynamically (e.g., when Shift is pressed). However, maybe `drawKeyFrame` can be optimized, or perhaps we can avoid string operations inside the loop.
## 2024-05-24 - Identifying Key Optimization
**Learning:** In Keyboard2View.java, during `onDraw`, `modifyKey(kv, _mods)` is called up to 9 times per key to apply modifiers (shift, ctrl, fn, etc.). `KeyModifier.modify` iterates over the active modifiers list for every `modify` call, creating new KeyValue objects often, evaluating deep modifier switches. This adds a substantial overhead on every frame, considering there are ~30-40 keys on screen, leading to ~300+ `modifyKey` calls per frame, allocating a large number of temporary KeyValue and object wrappers on Android UI thread. Also, `_tmpRect.set` is used, but drawing labels calls `drawText`.
**Action:** Wait, we could cache the *modified* keys at the KeyboardData.Key level whenever the modifiers change, or we can just optimize `updateFlags()` to update an array/list of rendered keys.
If we can't easily cache, perhaps a simple caching layer inside Keyboard2View: `private final Map<KeyValue, KeyValue> _modifiedKeyCache = new HashMap<>()` which gets cleared inside `updateFlags()`! This would reduce `modifyKey` from running on every draw frame to ONLY running once per key per modifier state.
Actually, if modifiers don't change between draws (e.g. key pressed animation), we are re-evaluating `modifyKey` hundreds of times unnecessarily!
## 2024-05-24 - Identifying Key Optimization
**Learning:** We can memoize or cache the modified `KeyValue` for the labels, or simply cache them in the `KeyboardData.Key` but wait, `KeyboardData.Key` instances might be shared or immutable. A caching layer in `Keyboard2View` mapped by `KeyValue` makes sense because the same `KeyValue` will always modify to the same thing given the same `_mods`. However, what about `updateFlags()` clearing the cache? Yes, `updateFlags()` can clear the cache when `_mods` change! Actually, it's simpler:
```java
  private final Map<KeyValue, KeyValue> _modifiedKeyCache = new HashMap<>();

  private void updateFlags()
  {
    Pointers.Modifiers newMods = _pointers.getModifiers();
    if (!newMods.equals(_mods)) {
        _modifiedKeyCache.clear();
    }
    _mods = newMods;
    _config.handler.mods_changed(_mods);
  }

  public KeyValue modifyKey(KeyValue k, Pointers.Modifiers mods)
  {
    if (mods == _mods) {
      KeyValue cached = _modifiedKeyCache.get(k);
      if (cached != null) return cached;
      KeyValue modified = KeyModifier.modify(k, mods);
      // Wait, HashMap allows null values, but better to use a placeholder or check containsKey.
      if (modified == null) ...
    }
    return KeyModifier.modify(k, mods);
  }
```
Even simpler: `KeyModifier.modify` caches internally? No, modifiers are specific to the view. Caching in Keyboard2View is perfect.
