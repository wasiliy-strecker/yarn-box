package de.appfabrik.garnbox.application;

import de.appfabrik.garnbox.domain.YarnDraft;
import java.io.IOException;
import java.nio.file.Path;

/** Writes a collection to a new local file. Entries may be loaded lazily. */
public interface CollectionFile {
  void write(Path target, Iterable<YarnDraft> yarns) throws IOException;
}
