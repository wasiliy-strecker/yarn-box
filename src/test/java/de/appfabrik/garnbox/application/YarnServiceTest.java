package de.appfabrik.garnbox.application;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.garnbox.domain.Yarn;
import de.appfabrik.garnbox.domain.YarnDraft;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class YarnServiceTest {
  private final Yarn first =
      new Yarn(
          1,
          "Garne im Frühling",
          "Änne Müller",
          "Grün",
          "Wohnzimmer, Regal 2",
          "Geschenk\nvon Ömer. 100% schön_",
          false);
  private final Yarn duplicate = new Yarn(2, first.name(), "", "", "", "", false);
  private final Yarn other = new Yarn(3, "Alpaka", "Ben", "Rot", "Flur", "", false);

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n", "\u2003", "\u00a0"})
  void rejectsMissingOrWhitespaceOnlyTitle(String name) {
    assertThrows(IllegalArgumentException.class, () -> draft(name, "", "", ""));
  }

  @Test
  void normalizesSingleLineInputButPreservesNoteFormatting() {
    var draft = draft("  Atlas  ", null, " Flur ", "  Notiz\n\nEnde  ");
    assertEquals("Atlas", draft.name());
    assertEquals("", draft.brand());
    var colored = new YarnDraft("Merino", null, " Grün ", null, null, null);
    assertEquals("Grün", colored.color());
    assertEquals("", new YarnDraft("Merino", null, null, null, null, null).color());
    assertEquals("Flur", draft.location());
    assertEquals("  Notiz\n\nEnde  ", draft.note());
    assertEquals("", draft("Atlas", null, null, null).note());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "GARNE",
        "äNNE",
        "MÜLLER",
        "WOHNZIMMER",
        "regal 2",
        "öMER",
        "geschenk",
        "100%",
        "schön_",
        "FRU\u0308HLING",
        "GRÜN"
      })
  void searchesEveryFieldWithUnicodeCaseHandlingAndLiteralSubstrings(String query) {
    assertEquals(List.of(first), YarnService.search(List.of(first, other), query));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\n\t"})
  void emptyQueryShowsEntireCollectionSortedByTitleThenId(String query) {
    assertEquals(
        List.of(other, first, duplicate),
        YarnService.search(List.of(duplicate, first, other), query));
  }

  @Test
  void preservesDuplicateIdentityAndDoesNotModifyInput() {
    List<Yarn> input = List.of(duplicate, other, first);
    assertEquals(List.of(first, duplicate), YarnService.search(input, "Garne"));
    assertEquals(List.of(duplicate, other, first), input);
    assertTrue(YarnService.search(input, "nicht vorhanden").isEmpty());
  }

  @Test
  void propagatesFailuresWithoutReportingSuccessfulSaveOrDelete() {
    YarnService service = new YarnService(new FailingRepository());
    YarnDraft draft =
        draft("Unveränderte Eingabe", "Marke / Hersteller", "Ort", "Mehrzeilig\nbleibt erhalten");
    assertThrows(StorageException.class, service::load);
    assertThrows(StorageException.class, () -> service.save(null, draft));
    assertThrows(StorageException.class, () -> service.save(1L, draft));
    assertThrows(StorageException.class, () -> service.delete(1));
    assertEquals("Mehrzeilig\nbleibt erhalten", draft.note());
  }

  private static YarnDraft draft(String name, String brand, String location, String note) {
    return new YarnDraft(name, brand, "", location, note, null);
  }

  private static final class FailingRepository implements YarnRepository {
    @Override
    public void initialize() {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public List<Yarn> findAll() {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public de.appfabrik.garnbox.domain.Photo findPhoto(long id) {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public Yarn create(YarnDraft draft) {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public Yarn update(long id, YarnDraft draft) {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public void delete(long id) {
      throw new StorageException(StorageException.Code.OPERATION);
    }
  }
}
