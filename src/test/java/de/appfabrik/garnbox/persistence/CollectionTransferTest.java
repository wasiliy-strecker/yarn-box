package de.appfabrik.garnbox.persistence;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.garnbox.application.CollectionExport;
import de.appfabrik.garnbox.application.CollectionImport;
import de.appfabrik.garnbox.application.CollectionLimitException;
import de.appfabrik.garnbox.application.CollectionLimits;
import de.appfabrik.garnbox.application.ImportException;
import de.appfabrik.garnbox.application.StorageException;
import de.appfabrik.garnbox.domain.Photo;
import de.appfabrik.garnbox.domain.YarnDraft;
import de.appfabrik.garnbox.support.TestImages;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CollectionTransferTest {
  @TempDir Path directory;

  private SqliteYarnRepository repository(String name) {
    var repository = new SqliteYarnRepository(directory.resolve(name + ".db"));
    repository.initialize();
    return repository;
  }

  private static YarnDraft draft(String name) {
    return new YarnDraft(name, "Élise", "#12345678", "Regal 日本語", "Zeile 1\n📚", null);
  }

  private void seed(int count) throws Exception {
    try (var connection =
            DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("source.db"));
        var insert = connection.prepareStatement("INSERT INTO yarns(name) VALUES (?)")) {
      connection.setAutoCommit(false);
      for (int i = 0; i < count; i++) {
        insert.setString(1, "Yarn " + i);
        insert.addBatch();
      }
      insert.executeBatch();
      connection.commit();
    }
  }

  @Test
  void restoresUnicodeColorsExactPhotosAndMultiplicityWithoutDuplicateReimports() throws Exception {
    var source = repository("source");
    var image = new Photo(TestImages.image("png", 120, 80, 0x234567));
    var withPhoto = new YarnDraft("Été 日本語 📚", "", "#12345678", "", "\"\\\n\t", image);
    source.create(withPhoto);
    source.create(withPhoto);
    source.create(draft("No photo"));
    Path file = directory.resolve("backup.json");
    var files = new JsonCollectionFile();
    assertEquals(3, new CollectionExport(source, files).export(file));
    var target = repository("target");
    var imports = new CollectionImport(target, files);
    var first = imports.importFile(file);
    assertEquals(3, first.added());
    assertEquals(0, first.skipped());
    assertEquals(image, target.findPhoto(first.yarns().getFirst().id()));
    assertEquals(withPhoto.name(), first.yarns().getFirst().name());
    assertEquals(withPhoto.note(), first.yarns().getFirst().note());
    assertEquals(withPhoto.color(), first.yarns().getFirst().color());
    var repeated = imports.importFile(file);
    assertEquals(0, repeated.added());
    assertEquals(3, repeated.skipped());
    assertEquals(first.yarns(), repeated.yarns());
    source.create(draft("No photo"));
    Path extra = directory.resolve("extra.json");
    new CollectionExport(source, files).export(extra);
    assertEquals(1, imports.importFile(extra).added());
  }

  @Test
  void allowsTenThousandEntriesAndRejectsNextCreateWithoutPreventingEditingOrDeletion()
      throws Exception {
    var source = repository("source");
    seed(CollectionLimits.MAX_ENTRIES - 1);
    var last = source.create(draft("Last"));
    assertEquals(CollectionLimits.MAX_ENTRIES, source.findAll().size());
    assertEquals(
        CollectionLimitException.Code.ENTRIES,
        assertThrows(CollectionLimitException.class, () -> source.create(draft("Too many")))
            .code());
    source.update(last.id(), draft("Updated at limit"));
    source.delete(last.id());
    source.create(draft("Room again"));
    assertEquals(CollectionLimits.MAX_ENTRIES, source.findAll().size());
    Path backup = directory.resolve("full.json");
    var files = new JsonCollectionFile();
    assertEquals(10_000, new CollectionExport(source, files).export(backup));
    var restored = repository("restored");
    assertEquals(10_000, new CollectionImport(restored, files).importFile(backup).added());
  }

  @Test
  void simultaneousCreatesCannotExceedTheCollectionLimit() throws Exception {
    var source = repository("source");
    seed(CollectionLimits.MAX_ENTRIES - 1);
    var start = new java.util.concurrent.CountDownLatch(1);
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      java.util.concurrent.Callable<Boolean> create =
          () -> {
            start.await();
            try {
              source.create(draft("Concurrent addition"));
              return true;
            } catch (CollectionLimitException limit) {
              assertEquals(CollectionLimitException.Code.ENTRIES, limit.code());
              return false;
            }
          };
      var first = executor.submit(create);
      var second = executor.submit(create);
      start.countDown();
      assertNotEquals(
          first.get(10, java.util.concurrent.TimeUnit.SECONDS),
          second.get(10, java.util.concurrent.TimeUnit.SECONDS));
    }
    assertEquals(10_000, source.findAll().size());
  }

  @Test
  void importAtCapacitySkipsExistingCopiesAndRollsBackEveryNewEntryIfMergeWouldOverflow()
      throws Exception {
    var source = repository("source");
    seed(CollectionLimits.MAX_ENTRIES - 1);
    var first = new YarnDraft("Yarn 0", "", "", "", "", null);
    Path file = directory.resolve("merge.json");
    var files = new JsonCollectionFile();
    files.write(file, List.of(first, draft("New one"), draft("New two")));
    var before = source.findAll();
    assertEquals(
        CollectionLimitException.Code.ENTRIES,
        assertThrows(
                CollectionLimitException.class,
                () -> new CollectionImport(source, files).importFile(file))
            .code());
    assertEquals(before, source.findAll());
    source.create(draft("Last"));
    Path duplicate = directory.resolve("duplicate.json");
    files.write(duplicate, List.of(first));
    var skipped = new CollectionImport(source, files).importFile(duplicate);
    assertEquals(0, skipped.added());
    assertEquals(1, skipped.skipped());
    assertEquals(10_000, skipped.yarns().size());
  }

  @Test
  void preservesOversizedLegacyCollectionsAndRejectsTheirExportExplicitly() throws Exception {
    var source = repository("source");
    seed(CollectionLimits.MAX_ENTRIES + 1);
    source.initialize();
    assertEquals(10_001, source.findAll().size());
    source.update(source.findAll().getFirst().id(), draft("Still editable"));
    Path export = directory.resolve("legacy.json");
    assertEquals(
        CollectionLimitException.Code.ENTRIES,
        assertThrows(
                CollectionLimitException.class,
                () -> new CollectionExport(source, new JsonCollectionFile()).export(export))
            .code());
    assertFalse(Files.exists(export));
    assertNoTemporaryFiles();
    assertEquals(10_001, source.findAll().size());
    source.delete(source.findAll().getFirst().id());
    assertEquals(10_000, source.findAll().size());
  }

  @Test
  void textAtTheBoundaryRoundTripsAndOversizedTextCannotBeSavedOrExported() throws Exception {
    var source = repository("source");
    String note = "ä".repeat(CollectionLimits.MAX_TEXT_LENGTH);
    var allowed = new YarnDraft("Boundary", "", "", "", note, null);
    var saved = source.create(allowed);
    var files = new JsonCollectionFile();
    Path backup = directory.resolve("long.json");
    new CollectionExport(source, files).export(backup);
    var read = new ArrayList<YarnDraft>();
    files.read(backup, read::add);
    assertEquals(List.of(allowed), read);
    var oversized = new YarnDraft("Too long", "", "", "", note + "x", null);
    assertEquals(
        CollectionLimitException.Code.TEXT,
        assertThrows(CollectionLimitException.class, () -> source.create(oversized)).code());
    assertThrows(CollectionLimitException.class, () -> source.update(saved.id(), oversized));
    assertEquals(saved, source.findAll().getFirst());
    Path rejected = directory.resolve("too-long.json");
    assertThrows(CollectionLimitException.class, () -> files.write(rejected, List.of(oversized)));
    assertFalse(Files.exists(rejected));
    assertNoTemporaryFiles();
  }

  @Test
  void byteBudgetCountsUtf8AndRejectsOversizedOutputBeforePublishing() throws Exception {
    Path sample = directory.resolve("sample.json");
    var entries = List.of(draft("Multibyte é 日本語"));
    new JsonCollectionFile().write(sample, entries);
    long bytes = Files.size(sample);
    var boundary = new JsonCollectionFile(bytes);
    Path accepted = directory.resolve("accepted.json");
    boundary.write(accepted, entries);
    var result = new ArrayList<YarnDraft>();
    boundary.read(accepted, result::add);
    assertEquals(entries, result);
    Path rejected = directory.resolve("rejected.json");
    var tooSmall = new JsonCollectionFile(bytes - 1);
    assertEquals(
        CollectionLimitException.Code.FILE_SIZE,
        assertThrows(CollectionLimitException.class, () -> tooSmall.write(rejected, entries))
            .code());
    assertFalse(Files.exists(rejected));
    assertEquals(
        ImportException.Code.LIMIT,
        assertThrows(ImportException.class, () -> tooSmall.read(accepted, ignored -> fail()))
            .code());
    assertNoTemporaryFiles();
  }

  @Test
  void failedWritesNeverLeavePartialFilesOrReplaceExistingExports() throws Exception {
    var files = new JsonCollectionFile();
    Path target = directory.resolve("keep.json");
    Files.writeString(target, "existing backup");
    assertThrows(
        FileAlreadyExistsException.class, () -> files.write(target, List.of(draft("New"))));
    assertEquals("existing backup", Files.readString(target));
    Path rejected = directory.resolve("too-many.json");
    assertThrows(
        CollectionLimitException.class,
        () -> files.write(rejected, Collections.nCopies(10_001, draft("Repeated"))));
    assertFalse(Files.exists(rejected));
    assertNoTemporaryFiles();
  }

  @Test
  void lateMalformedInputOrSqlFailureRollsBackTheWholeImport() throws Exception {
    var target = repository("target");
    var original = target.create(draft("Existing"));
    var files = new JsonCollectionFile();
    Path valid = directory.resolve("valid.json");
    files.write(valid, List.of(draft("New one"), draft("New two")));
    String json = Files.readString(valid);
    for (String invalid :
        List.of(
            json + "trailing",
            json.replace("\"version\": 1", "\"version\": 2"),
            json.replace("\"photoBase64\": null", "\"photoBase64\": \"invalid\""),
            json.replace("\"format\": \"yarn-box.collection\",", ""))) {
      Path file = directory.resolve("bad.json");
      Files.writeString(file, invalid);
      assertThrows(
          ImportException.class, () -> new CollectionImport(target, files).importFile(file));
      assertEquals(List.of(original), target.findAll());
    }
    try (var connection =
            DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("target.db"));
        var statement = connection.createStatement()) {
      statement.execute(
          "CREATE TRIGGER fail_import BEFORE INSERT ON yarns WHEN NEW.name = 'New two' BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END");
    }
    assertThrows(
        StorageException.class, () -> new CollectionImport(target, files).importFile(valid));
    assertEquals(List.of(original), target.findAll());
  }

  private void assertNoTemporaryFiles() throws Exception {
    try (var paths = Files.list(directory)) {
      assertTrue(paths.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
    }
  }
}
