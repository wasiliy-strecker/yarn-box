package de.appfabrik.garnbox.persistence;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.garnbox.application.PhotoImporter;
import de.appfabrik.garnbox.application.StorageException;
import de.appfabrik.garnbox.application.YarnService;
import de.appfabrik.garnbox.domain.Photo;
import de.appfabrik.garnbox.domain.YarnDraft;
import de.appfabrik.garnbox.support.TestImages;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PhotoPersistenceTest {
  @TempDir Path directory;
  private Path database;
  private YarnService service;
  private Photo first;
  private Photo second;

  @BeforeEach
  void setUp() throws Exception {
    database = directory.resolve("garnbox.db");
    service = new YarnService(new SqliteYarnRepository(database));
    service.load();
    first = PhotoImporter.decode(TestImages.image("png", 120, 80, 0x937aa0));
    second = PhotoImporter.decode(TestImages.image("jpeg", 80, 120, 0x8cab98));
  }

  @Test
  void addsReplacesRemovesAndDeletesOneOwnedPhoto() throws Exception {
    var yarn = service.save(null, draft("Merino", null));
    assertFalse(yarn.hasPhoto());
    assertNull(service.photo(yarn.id()));
    Path source = directory.resolve("original.png");
    Files.write(source, first.bytes());
    Photo selectedFirst = PhotoImporter.read(source);
    var withPhoto = service.save(yarn.id(), draft("Merino", selectedFirst));
    assertTrue(withPhoto.hasPhoto());
    Files.delete(source);
    YarnService reopened = new YarnService(new SqliteYarnRepository(database));
    assertEquals(withPhoto, reopened.load().getFirst());
    assertEquals(selectedFirst, reopened.photo(yarn.id()));
    Path untouchedSource = directory.resolve("ersatz.jpg");
    Files.write(untouchedSource, second.bytes());
    Photo selectedSecond = PhotoImporter.read(untouchedSource);
    reopened.save(yarn.id(), draft("Merino geändert", selectedSecond));
    assertEquals(selectedSecond, service.photo(yarn.id()));
    reopened.save(yarn.id(), draft("Nur Text geändert", service.photo(yarn.id())));
    assertEquals(selectedSecond, service.photo(yarn.id()));
    var removed = service.save(yarn.id(), draft("Ohne Foto", null));
    assertFalse(removed.hasPhoto());
    assertNull(reopened.photo(yarn.id()));
    assertArrayEquals(second.bytes(), Files.readAllBytes(untouchedSource));
    service.save(yarn.id(), draft("Mit Foto löschen", second));
    service.delete(yarn.id());
    assertTrue(reopened.load().isEmpty());
    assertThrows(StorageException.class, () -> reopened.photo(yarn.id()));
  }

  @Test
  void lockRejectsCreateReplaceRemoveAndDeleteWithoutPartialChanges() throws Exception {
    var saved = service.save(null, draft("Original", first));
    var replacement = draft("Neue Texte", second);
    try (var lock = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = lock.createStatement()) {
      statement.execute("BEGIN EXCLUSIVE");
      assertThrows(StorageException.class, () -> service.save(null, replacement));
      assertThrows(StorageException.class, () -> service.save(saved.id(), replacement));
      assertThrows(
          StorageException.class, () -> service.save(saved.id(), draft("Entfernen", null)));
      assertThrows(StorageException.class, () -> service.delete(saved.id()));
      statement.execute("ROLLBACK");
    }
    assertEquals(saved, service.load().getFirst());
    assertEquals(1, service.load().size());
    assertEquals(first, service.photo(saved.id()));
    assertEquals(second, replacement.photo());
    service.save(saved.id(), replacement);
    assertEquals(second, service.photo(saved.id()));
  }

  @Test
  void statementFailureRollsBackTextAndBlobTogether() throws Exception {
    var saved = service.save(null, draft("Original", first));
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute(
          "CREATE TRIGGER fail_save AFTER UPDATE ON yarns BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END");
    }
    assertThrows(StorageException.class, () -> service.save(saved.id(), draft("Geändert", second)));
    assertEquals(saved, service.load().getFirst());
    assertEquals(first, service.photo(saved.id()));
  }

  private static YarnDraft draft(String name, Photo photo) {
    return new YarnDraft(name, "Wollwerk", "Grün", "Box 1", "Notiz\nZweite Zeile", photo);
  }
}
