package de.appfabrik.garnbox.application;

import java.nio.file.Path;

/** Restores a collection incrementally without retaining all photos in memory. */
public final class CollectionImport {
  private final CollectionImportRepository repository;
  private final CollectionFile files;

  public CollectionImport(CollectionImportRepository repository, CollectionFile files) {
    this.repository = repository;
    this.files = files;
  }

  public ImportResult importFile(Path path) {
    return repository.importYarns(accept -> files.read(path, accept));
  }
}
