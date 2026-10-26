package de.appfabrik.garnbox.application;

import de.appfabrik.garnbox.domain.Photo;
import de.appfabrik.garnbox.domain.Yarn;
import de.appfabrik.garnbox.domain.YarnDraft;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Small application layer, independent of JavaFX and JDBC. */
public final class YarnService {
  private final YarnRepository repository;

  public YarnService(YarnRepository repository) {
    this.repository = repository;
  }

  public List<Yarn> load() {
    repository.initialize();
    return repository.findAll();
  }

  public Photo photo(long id) {
    return repository.findPhoto(id);
  }

  public Yarn save(Long id, YarnDraft draft) {
    return id == null ? repository.create(draft) : repository.update(id, draft);
  }

  public void delete(long id) {
    repository.delete(id);
  }

  public static List<Yarn> search(List<Yarn> yarns, String query) {
    String needle = normalize(query == null ? "" : query.strip());
    Collator collator = Collator.getInstance(Locale.GERMAN);
    collator.setStrength(Collator.SECONDARY);
    return yarns.stream()
        .filter(yarn -> matches(yarn, needle))
        .sorted(Comparator.comparing(Yarn::name, collator).thenComparingLong(Yarn::id))
        .toList();
  }

  private static boolean matches(Yarn yarn, String needle) {
    return normalize(yarn.name()).contains(needle)
        || normalize(yarn.brand()).contains(needle)
        || normalize(yarn.color()).contains(needle)
        || normalize(yarn.location()).contains(needle)
        || normalize(yarn.note()).contains(needle);
  }

  private static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
  }
}
