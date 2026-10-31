package de.appfabrik.garnbox.application;

import de.appfabrik.garnbox.domain.Yarn;
import de.appfabrik.garnbox.domain.YarnDraft;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Exports every saved entry, independent of the current search and selection. */
public final class CollectionExport {
  private final YarnRepository repository;
  private final CollectionFile files;

  public CollectionExport(YarnRepository repository, CollectionFile files) {
    this.repository = repository;
    this.files = files;
  }

  public int export(Path target) throws IOException {
    List<Yarn> saved = repository.findAll();
    Iterable<YarnDraft> entries =
        () ->
            saved.stream()
                .map(
                    yarn ->
                        new YarnDraft(
                            yarn.name(),
                            yarn.brand(),
                            yarn.color(),
                            yarn.location(),
                            yarn.note(),
                            yarn.hasPhoto() ? repository.findPhoto(yarn.id()) : null))
                .iterator();
    files.write(target, entries);
    return saved.size();
  }
}
