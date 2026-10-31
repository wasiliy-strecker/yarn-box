package de.appfabrik.garnbox.persistence;

import de.appfabrik.garnbox.application.CollectionFile;
import de.appfabrik.garnbox.domain.YarnDraft;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

/** Versioned UTF-8 JSON, including selected colors and original photo bytes. */
public final class JsonCollectionFile implements CollectionFile {
  @Override
  public void write(Path target, Iterable<YarnDraft> yarns) throws IOException {
    Path destination = target.toAbsolutePath().normalize();
    if (destination.getParent() == null
        || !destination.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
      throw new IOException("Choose a JSON file in an existing directory");
    }
    if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
      throw new FileAlreadyExistsException(destination.toString());
    }
    Path temporary = Files.createTempFile(destination.getParent(), ".yarn-box-", ".json.tmp");
    try {
      try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
        writer.write(
            "{\n  \"format\": \"yarn-box.collection\",\n  \"version\": 1,\n  \"yarns\": [");
        boolean first = true;
        for (YarnDraft yarn : yarns) {
          writer.write(first ? "\n" : ",\n");
          first = false;
          writer.write("    {\n      \"name\": ");
          string(writer, yarn.name());
          writer.write(",\n      \"brand\": ");
          string(writer, yarn.brand());
          writer.write(",\n      \"color\": ");
          string(writer, yarn.color());
          writer.write(",\n      \"location\": ");
          string(writer, yarn.location());
          writer.write(",\n      \"note\": ");
          string(writer, yarn.note());
          writer.write(",\n      \"photoBase64\": ");
          if (yarn.photo() == null) {
            writer.write("null");
          } else {
            string(writer, Base64.getEncoder().encodeToString(yarn.photo().bytes()));
          }
          writer.write("\n    }");
        }
        writer.write("\n  ]\n}\n");
      }
      // Publish only the complete file, without replacing an existing target.
      Files.move(temporary, destination);
    } finally {
      try {
        Files.deleteIfExists(temporary);
      } catch (IOException | SecurityException ignored) {
        // Cleanup must not turn an already completed export into an error.
      }
    }
  }

  private static void string(Writer writer, String value) throws IOException {
    writer.write('"');
    for (int i = 0; i < value.length(); i++) {
      char character = value.charAt(i);
      switch (character) {
        case '"' -> writer.write("\\\"");
        case '\\' -> writer.write("\\\\");
        case '\n' -> writer.write("\\n");
        case '\r' -> writer.write("\\r");
        case '\t' -> writer.write("\\t");
        default -> {
          if (character < 0x20 || Character.isSurrogate(character)) {
            writer.write(String.format(Locale.ROOT, "\\u%04x", (int) character));
          } else {
            writer.write(character);
          }
        }
      }
    }
    writer.write('"');
  }
}
