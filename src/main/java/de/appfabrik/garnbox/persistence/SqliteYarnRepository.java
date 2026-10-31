package de.appfabrik.garnbox.persistence;

import de.appfabrik.garnbox.application.CollectionImportRepository;
import de.appfabrik.garnbox.application.CollectionLimitException;
import de.appfabrik.garnbox.application.CollectionLimits;
import de.appfabrik.garnbox.application.ImportResult;
import de.appfabrik.garnbox.application.PhotoException;
import de.appfabrik.garnbox.application.PhotoImporter;
import de.appfabrik.garnbox.application.StorageException;
import de.appfabrik.garnbox.application.YarnImportSource;
import de.appfabrik.garnbox.application.YarnRepository;
import de.appfabrik.garnbox.domain.Photo;
import de.appfabrik.garnbox.domain.Yarn;
import de.appfabrik.garnbox.domain.YarnDraft;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import org.sqlite.SQLiteConfig;

/** Opens and closes one JDBC connection per operation. No connection escapes this class. */
public final class SqliteYarnRepository implements YarnRepository, CollectionImportRepository {
  private final Path database;

  public SqliteYarnRepository(Path database) {
    this.database = database.toAbsolutePath().normalize();
  }

  private Connection connect() throws SQLException {
    SQLiteConfig config = new SQLiteConfig();
    config.setBusyTimeout(2000);
    return config.createConnection("jdbc:sqlite:" + database);
  }

  @Override
  public void initialize() {
    try {
      Path parent = database.getParent();
      if (parent == null) {
        throw new StorageException(StorageException.Code.PATH);
      }
      Files.createDirectories(parent);
      try (Connection connection = connect()) {
        connection.setAutoCommit(false);
        try (var statement = connection.createStatement()) {
          int version;
          try (var result = statement.executeQuery("PRAGMA user_version")) {
            result.next();
            version = result.getInt(1);
          }
          if (version > 1) {
            throw new StorageException(StorageException.Code.NEWER_SCHEMA);
          }
          if (version == 0) {
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS yarns (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  name TEXT NOT NULL CHECK (length(trim(name)) > 0),
                  brand TEXT NOT NULL DEFAULT '',
                  color TEXT NOT NULL DEFAULT '',
                  location TEXT NOT NULL DEFAULT '',
                  note TEXT NOT NULL DEFAULT '',
                  photo BLOB CHECK (photo IS NULL OR length(photo) <= 10485760)
                )
                """);
            statement.executeUpdate("PRAGMA user_version = 1");
          }
          connection.commit();
        } catch (SQLException | RuntimeException exception) {
          connection.rollback();
          throw exception;
        }
      }
    } catch (IOException | SQLException exception) {
      throw new StorageException(StorageException.Code.OPEN, exception);
    }
  }

  @Override
  public List<Yarn> findAll() {
    try (Connection connection = connect()) {
      return readYarns(connection);
    } catch (SQLException exception) {
      throw failure(exception);
    }
  }

  private static List<Yarn> readYarns(Connection connection) throws SQLException {
    try (var statement = connection.createStatement();
        var result =
            statement.executeQuery(
                "SELECT id, name, brand, color, location, note, photo IS NOT NULL AS has_photo FROM yarns")) {
      List<Yarn> yarns = new ArrayList<>();
      while (result.next()) {
        yarns.add(
            new Yarn(
                result.getLong("id"),
                result.getString("name"),
                result.getString("brand"),
                result.getString("color"),
                result.getString("location"),
                result.getString("note"),
                result.getBoolean("has_photo")));
      }
      return List.copyOf(yarns);
    }
  }

  @Override
  public ImportResult importYarns(YarnImportSource source) {
    try (Connection connection = connect()) {
      connection.setAutoCommit(false);
      try {
        var available = new HashMap<String, Integer>();
        int[] counts = new int[2];
        long originalCount;
        try (var statement = connection.createStatement();
            var result = statement.executeQuery("SELECT count(*) FROM yarns")) {
          result.next();
          originalCount = result.getLong(1);
        }
        MessageDigest digest = sha256();
        try (var existing =
                connection.prepareStatement(
                    "SELECT count(*) FROM yarns WHERE name = ? AND brand = ? AND color = ?"
                        + " AND location = ? AND note = ? AND photo IS ?");
            var insert =
                connection.prepareStatement(
                    "INSERT INTO yarns (name, brand, color, location, note, photo) VALUES (?, ?, ?, ?, ?, ?)")) {
          source.read(
              draft -> {
                CollectionLimits.validate(draft);
                String key = fingerprint(draft, digest);
                try {
                  Integer copies = available.get(key);
                  if (copies == null) {
                    bind(existing, draft);
                    try (var result = existing.executeQuery()) {
                      if (!result.next()) {
                        throw new SQLException("Missing duplicate count");
                      }
                      copies = result.getInt(1);
                    }
                  }
                  if (copies > 0) {
                    available.put(key, copies - 1);
                    counts[1]++;
                  } else {
                    if (originalCount + counts[0] >= CollectionLimits.MAX_ENTRIES) {
                      throw new CollectionLimitException(CollectionLimitException.Code.ENTRIES);
                    }
                    available.put(key, 0);
                    bind(insert, draft);
                    if (insert.executeUpdate() != 1) {
                      throw new SQLException("Incomplete collection import");
                    }
                    counts[0]++;
                  }
                } catch (SQLException exception) {
                  throw failure(exception);
                }
              });
        }
        var result = new ImportResult(counts[0], counts[1], readYarns(connection));
        connection.commit();
        return result;
      } catch (SQLException | RuntimeException exception) {
        connection.rollback();
        throw exception;
      }
    } catch (SQLException exception) {
      throw failure(exception);
    }
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", impossible);
    }
  }

  private static String fingerprint(YarnDraft draft, MessageDigest digest) {
    digest.reset();
    for (String value :
        List.of(draft.name(), draft.brand(), draft.color(), draft.location(), draft.note())) {
      byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
      digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
      digest.update(bytes);
    }
    byte[] photo = draft.photo() == null ? null : draft.photo().bytes();
    digest.update(
        ByteBuffer.allocate(Integer.BYTES).putInt(photo == null ? -1 : photo.length).array());
    if (photo != null) {
      digest.update(photo);
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  @Override
  public Photo findPhoto(long id) {
    try (Connection connection = connect();
        var statement = connection.prepareStatement("SELECT photo FROM yarns WHERE id = ?")) {
      statement.setLong(1, id);
      try (var result = statement.executeQuery()) {
        if (!result.next()) {
          throw new StorageException(StorageException.Code.MISSING);
        }
        byte[] bytes = result.getBytes(1);
        return bytes == null ? null : PhotoImporter.decode(bytes);
      }
    } catch (SQLException exception) {
      throw failure(exception);
    } catch (PhotoException exception) {
      throw new StorageException(StorageException.Code.PHOTO, exception);
    }
  }

  @Override
  public Yarn create(YarnDraft draft) {
    CollectionLimits.validate(draft);
    try (Connection connection = connect();
        PreparedStatement statement =
            connection.prepareStatement(
                "INSERT INTO yarns (name, brand, color, location, note, photo)"
                    + " SELECT ?, ?, ?, ?, ?, ? WHERE (SELECT count(*) FROM yarns) < ? RETURNING id")) {
      bind(statement, draft);
      statement.setInt(7, CollectionLimits.MAX_ENTRIES);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          throw new CollectionLimitException(CollectionLimitException.Code.ENTRIES);
        }
        return draft.withId(result.getLong(1));
      }
    } catch (SQLException exception) {
      throw failure(exception);
    }
  }

  @Override
  public Yarn update(long id, YarnDraft draft) {
    CollectionLimits.validate(draft);
    try (Connection connection = connect();
        PreparedStatement statement =
            connection.prepareStatement(
                "UPDATE yarns SET name = ?, brand = ?, color = ?, location = ?, note = ?, photo = ? WHERE id = ?")) {
      bind(statement, draft);
      statement.setLong(7, id);
      requireExisting(statement.executeUpdate());
      return draft.withId(id);
    } catch (SQLException exception) {
      throw failure(exception);
    }
  }

  @Override
  public void delete(long id) {
    try (Connection connection = connect();
        PreparedStatement statement =
            connection.prepareStatement("DELETE FROM yarns WHERE id = ?")) {
      statement.setLong(1, id);
      requireExisting(statement.executeUpdate());
    } catch (SQLException exception) {
      throw failure(exception);
    }
  }

  private static void bind(PreparedStatement statement, YarnDraft draft) throws SQLException {
    statement.setString(1, draft.name());
    statement.setString(2, draft.brand());
    statement.setString(3, draft.color());
    statement.setString(4, draft.location());
    statement.setString(5, draft.note());
    statement.setBytes(6, draft.photo() == null ? null : draft.photo().bytes());
  }

  private static void requireExisting(int affected) {
    if (affected != 1) {
      throw new StorageException(StorageException.Code.MISSING);
    }
  }

  private static StorageException failure(SQLException exception) {
    return new StorageException(StorageException.Code.OPERATION, exception);
  }
}
