-keep public class juloo.cdict.* {
  public protected private *;
}

# Keep the declared input method service entry point and its lifecycle callbacks.
# Android manifest components are normally retained by R8; this explicit rule
# makes the IME entry-point requirement resilient to future build-rule changes.
-keep class juloo.keyboard2.Keyboard2 {
  *;
}
