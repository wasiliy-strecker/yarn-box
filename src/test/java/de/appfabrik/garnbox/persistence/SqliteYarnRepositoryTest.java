package de.appfabrik.garnbox.persistence;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.garnbox.application.StorageException;
import de.appfabrik.garnbox.application.YarnService;
import de.appfabrik.garnbox.domain.Yarn;
import de.appfabrik.garnbox.domain.YarnDraft;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteYarnRepositoryTest {
  @TempDir Path directory;
  private Path database;
  private SqliteYarnRepository repository;
  private YarnService service;

  @BeforeEach
  void setUp() {
    database = directory.resolve("Garne mit Leerzeichen/sammlung.db");
    repository = new SqliteYarnRepository(database);
    service = new YarnService(repository);
    assertTrue(service.load().isEmpty());
  }

  @Test
  void createsEditsReopensAndDeletesIndependentYarns() {
    Yarn first = service.save(null, draft("Der Garten", "Mia", "Regal 2", "Erste\nNotiz"));
    Yarn second = service.save(null, draft(first));
    assertNotEquals(first.id(), second.id());
    Yarn edited = service.save(first.id(), draft("Der Garten", "Nora", "Regal 3", "Neue\nNotiz"));

    YarnService reopened = new YarnService(new SqliteYarnRepository(database));
    assertEquals(List.of(edited, second), YarnService.search(reopened.load(), ""));
    assertEquals(List.of(edited), YarnService.search(reopened.load(), "REGAL 3"));
    assertEquals(List.of(edited, second), YarnService.search(reopened.load(), "garten"));
    reopened.delete(edited.id());
    assertEquals(List.of(second), service.load());
    reopened.delete(second.id());
    assertTrue(new YarnService(new SqliteYarnRepository(database)).load().isEmpty());
  }

  @Test
  void storesQuotesSqlTextUnicodeAndLongMultilineNotesAsData() {
    YarnDraft draft =
        draft("Garn'); DROP TABLE yarns; --", "O'Connor", "Küche", "ÄÖÜ 日本語 📚\n".repeat(500));
    Yarn created = repository.create(draft);
    assertEquals(List.of(created), repository.findAll());
    assertEquals(draft, draft(repository.findAll().getFirst()));
    Yarn emptyOptional = repository.create(draft("Nur Titel", null, null, null));
    assertEquals("", emptyOptional.note());
    assertEquals(2, repository.findAll().size());
  }

  @Test
  void doesNotResurrectMissingEntriesOrTreatMissingDeletesAsSuccess() {
    Yarn yarn = repository.create(draft("Atlas", "", "", ""));
    repository.delete(yarn.id());
    assertThrows(StorageException.class, () -> repository.update(yarn.id(), draft(yarn)));
    assertThrows(StorageException.class, () -> repository.delete(yarn.id()));
    assertTrue(repository.findAll().isEmpty());
  }

  @Test
  void lockedDatabasePreservesPersistedYarnAndRejectsChanges() throws Exception {
    Yarn original = repository.create(draft("Original", "", "", "Erhalten"));
    try (var blocker = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = blocker.createStatement()) {
      statement.execute("BEGIN EXCLUSIVE");
      assertThrows(StorageException.class, () -> repository.create(draft("Neu", "", "", "")));
      assertThrows(
          StorageException.class,
          () -> repository.update(original.id(), draft("Geändert", "", "", "")));
      assertThrows(StorageException.class, () -> repository.delete(original.id()));
      statement.execute("ROLLBACK");
    }
    assertEquals(List.of(original), repository.findAll());
  }

  @Test
  void rejectsNewerSchemaWithoutChangingIt() throws Exception {
    Yarn original = repository.create(draft("Vorhanden", "", "", ""));
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute("PRAGMA user_version = 2");
    }
    assertThrows(StorageException.class, repository::initialize);
    assertEquals(List.of(original), repository.findAll());
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement();
        var result = statement.executeQuery("PRAGMA user_version")) {
      assertTrue(result.next());
      assertEquals(2, result.getInt(1));
    }
  }

  @Test
  void refusesCorruptedDatabaseInsteadOfReplacingIt() throws Exception {
    Path corrupted = directory.resolve("corrupted.db");
    byte[] original = "Keine SQLite-Datenbank".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    Files.write(corrupted, original);
    assertThrows(StorageException.class, () -> new SqliteYarnRepository(corrupted).initialize());
    assertArrayEquals(original, Files.readAllBytes(corrupted));
  }

  @Test
  void reportsInvalidDataDirectory() throws Exception {
    Path file = directory.resolve("file");
    Files.writeString(file, "blockiert");
    assertThrows(
        StorageException.class,
        () -> new SqliteYarnRepository(file.resolve("yarns.db")).initialize());
  }

  private static YarnDraft draft(String name, String brand, String location, String note) {
    return new YarnDraft(name, brand, "Blau", location, note, null);
  }

  private static YarnDraft draft(Yarn yarn) {
    return new YarnDraft(
        yarn.name(), yarn.brand(), yarn.color(), yarn.location(), yarn.note(), null);
  }
}
