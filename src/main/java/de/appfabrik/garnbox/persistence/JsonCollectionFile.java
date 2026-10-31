package de.appfabrik.garnbox.persistence;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import de.appfabrik.garnbox.application.CollectionFile;
import de.appfabrik.garnbox.application.CollectionLimitException;
import de.appfabrik.garnbox.application.CollectionLimits;
import de.appfabrik.garnbox.application.ImportException;
import de.appfabrik.garnbox.application.PhotoException;
import de.appfabrik.garnbox.application.PhotoImporter;
import de.appfabrik.garnbox.domain.Photo;
import de.appfabrik.garnbox.domain.YarnDraft;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/** Versioned UTF-8 JSON, including selected colors and exact stored photo bytes. */
public final class JsonCollectionFile implements CollectionFile {
  private final long maxFileBytes;
  private static final Set<String> ENTRY_FIELDS =
      Set.of("name", "brand", "color", "location", "note", "photoBase64");

  public JsonCollectionFile() {
    this(CollectionLimits.MAX_FILE_BYTES);
  }

  JsonCollectionFile(long maxFileBytes) {
    if (maxFileBytes < 1 || maxFileBytes > CollectionLimits.MAX_FILE_BYTES) {
      throw new IllegalArgumentException("Invalid collection file budget");
    }
    this.maxFileBytes = maxFileBytes;
  }

  @Override
  public void read(Path source, Consumer<YarnDraft> accept) {
    try {
      if (!Files.isRegularFile(source)) {
        throw new ImportException(ImportException.Code.READ);
      }
      if (Files.size(source) > maxFileBytes) {
        throw new ImportException(ImportException.Code.LIMIT);
      }
      var decoder =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT);
      try (var input = new LimitedInput(Files.newInputStream(source), maxFileBytes);
          var reader = new JsonReader(new InputStreamReader(input, decoder))) {
        reader.setStrictness(Strictness.STRICT);
        reader.beginObject();
        Set<String> keys = new HashSet<>();
        while (reader.hasNext()) {
          String key = reader.nextName();
          require(keys.add(key));
          switch (key) {
            case "format" -> require(readText(reader).equals("yarn-box.collection"));
            case "version" -> {
              require(reader.peek() == JsonToken.NUMBER);
              if (!reader.nextString().equals("1")) {
                throw new ImportException(ImportException.Code.VERSION);
              }
            }
            case "yarns" -> readEntries(reader, accept);
            default -> throw new ImportException(ImportException.Code.FORMAT);
          }
        }
        reader.endObject();
        require(keys.equals(Set.of("format", "version", "yarns")));
        require(reader.peek() == JsonToken.END_DOCUMENT);
      }
    } catch (MalformedJsonException | EOFException | CharacterCodingException exception) {
      throw new ImportException(ImportException.Code.FORMAT, exception);
    } catch (IOException | SecurityException exception) {
      throw new ImportException(ImportException.Code.READ, exception);
    } catch (IllegalArgumentException | IllegalStateException exception) {
      throw new ImportException(ImportException.Code.FORMAT, exception);
    }
  }

  private static void readEntries(JsonReader reader, Consumer<YarnDraft> accept)
      throws IOException {
    reader.beginArray();
    int count = 0;
    while (reader.hasNext()) {
      if (++count > CollectionLimits.MAX_ENTRIES) {
        throw new ImportException(ImportException.Code.LIMIT);
      }
      reader.beginObject();
      Set<String> keys = new HashSet<>();
      var values = new HashMap<String, String>();
      Photo photo = null;
      while (reader.hasNext()) {
        String key = reader.nextName();
        require(ENTRY_FIELDS.contains(key) && keys.add(key));
        if (key.equals("photoBase64")) {
          photo = readPhoto(reader);
        } else {
          values.put(key, readText(reader));
        }
      }
      reader.endObject();
      require(keys.equals(ENTRY_FIELDS));
      accept.accept(
          new YarnDraft(
              values.get("name"),
              values.get("brand"),
              values.get("color"),
              values.get("location"),
              values.get("note"),
              photo));
    }
    reader.endArray();
  }

  private static String readText(JsonReader reader) throws IOException {
    require(reader.peek() == JsonToken.STRING);
    String value = reader.nextString();
    if (value.length() > CollectionLimits.MAX_TEXT_LENGTH) {
      throw new ImportException(ImportException.Code.LIMIT);
    }
    for (int i = 0; i < value.length(); i++) {
      char character = value.charAt(i);
      if (Character.isHighSurrogate(character)) {
        require(i + 1 < value.length() && Character.isLowSurrogate(value.charAt(++i)));
      } else {
        require(!Character.isLowSurrogate(character));
      }
    }
    return value;
  }

  private static Photo readPhoto(JsonReader reader) throws IOException {
    if (reader.peek() == JsonToken.NULL) {
      reader.nextNull();
      return null;
    }
    require(reader.peek() == JsonToken.STRING);
    String encoded = reader.nextString();
    if (encoded.length() > ((PhotoImporter.MAX_BYTES + 2) / 3) * 4) {
      throw new ImportException(ImportException.Code.LIMIT);
    }
    try {
      return PhotoImporter.decode(Base64.getDecoder().decode(encoded));
    } catch (IllegalArgumentException | PhotoException exception) {
      throw new ImportException(ImportException.Code.PHOTO, exception);
    }
  }

  private static void require(boolean valid) {
    if (!valid) {
      throw new ImportException(ImportException.Code.FORMAT);
    }
  }

  private static final class LimitedInput extends FilterInputStream {
    private long count;
    private final long limit;

    private LimitedInput(InputStream input, long limit) {
      super(input);
      this.limit = limit;
    }

    @Override
    public int read() throws IOException {
      int value = in.read();
      if (value != -1) {
        reserve(1);
      }
      return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
      int result = in.read(bytes, offset, (int) Math.min(length, limit - count + 1));
      if (result > 0) {
        reserve(result);
      }
      return result;
    }

    private void reserve(int bytes) {
      count += bytes;
      if (count > limit) {
        throw new ImportException(ImportException.Code.LIMIT);
      }
    }
  }

  private static final class LimitedOutput extends FilterOutputStream {
    private final long limit;
    private long count;

    private LimitedOutput(OutputStream output, long limit) {
      super(output);
      this.limit = limit;
    }

    @Override
    public void write(int value) throws IOException {
      reserve(1);
      out.write(value);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
      reserve(length);
      out.write(bytes, offset, length);
    }

    private void reserve(int length) {
      if (length > limit - count) {
        throw new CollectionLimitException(CollectionLimitException.Code.FILE_SIZE);
      }
      count += length;
    }
  }

  @Override
  public void write(Path target, Iterable<YarnDraft> yarns) throws IOException {
    Path destination = target.toAbsolutePath().normalize();
    Path parent = destination.getParent();
    Path filename = destination.getFileName();
    if (parent == null
        || filename == null
        || !filename.toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
      throw new IOException("Choose a JSON file in an existing directory");
    }
    if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
      throw new FileAlreadyExistsException(destination.toString());
    }
    Path temporary = Files.createTempFile(parent, ".yarn-box-", ".json.tmp");
    try {
      try (var output = new LimitedOutput(Files.newOutputStream(temporary), maxFileBytes);
          var writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
        writer.write(
            "{\n  \"format\": \"yarn-box.collection\",\n  \"version\": 1,\n  \"yarns\": [");
        boolean first = true;
        int count = 0;
        for (YarnDraft yarn : yarns) {
          if (++count > CollectionLimits.MAX_ENTRIES) {
            throw new CollectionLimitException(CollectionLimitException.Code.ENTRIES);
          }
          CollectionLimits.validate(yarn);
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
            byte[] photo = yarn.photo().bytes();
            PhotoImporter.decode(photo);
            string(writer, Base64.getEncoder().encodeToString(photo));
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
