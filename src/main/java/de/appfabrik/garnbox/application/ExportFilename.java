package de.appfabrik.garnbox.application;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Keeps a sandbox export inside the directory selected through the native panel. */
public final class ExportFilename {
  private ExportFilename() {}

  public static Path resolve(Path directory, String entered) {
    String name = entered.strip();
    if (name.isBlank()
        || name.equals(".")
        || name.equals("..")
        || name.contains("/")
        || name.contains("\\")
        || name.contains(":")) {
      throw new IllegalArgumentException("Enter a filename, not a path");
    }
    if (name.codePoints().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("Control characters are not filenames");
    }
    if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".json")) {
      name += ".json";
    }
    if (name.getBytes(StandardCharsets.UTF_8).length > 255) {
      throw new IllegalArgumentException("Filename too long");
    }
    return directory.resolve(name);
  }
}
