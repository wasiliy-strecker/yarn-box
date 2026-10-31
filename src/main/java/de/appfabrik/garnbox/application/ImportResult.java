package de.appfabrik.garnbox.application;

import de.appfabrik.garnbox.domain.Yarn;
import java.util.List;

/** The committed collection, with counts of new and already present entries. */
public record ImportResult(int added, int skipped, List<Yarn> yarns) {
  public ImportResult {
    yarns = List.copyOf(yarns);
  }
}
