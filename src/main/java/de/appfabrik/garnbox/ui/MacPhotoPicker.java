package de.appfabrik.garnbox.ui;

import java.io.File;
import java.nio.file.Path;

/** Store-only Cocoa panel. A remembered path is a navigation hint, never an access grant. */
final class MacPhotoPicker {
  private static boolean loaded;

  private MacPhotoPicker() {}

  static File choose(String title, Path initialDirectory) {
    if (!Boolean.getBoolean("yarnbox.macAppStore")) {
      throw new IllegalStateException("The native picker is restricted to the Mac Store build");
    }
    if (!loaded) {
      System.loadLibrary("yarnboxdialogs");
      loaded = true;
    }
    String selected =
        choosePhoto(title, initialDirectory == null ? null : initialDirectory.toString());
    return selected == null ? null : new File(selected);
  }

  private static native String choosePhoto(String title, String initialDirectory);
}
