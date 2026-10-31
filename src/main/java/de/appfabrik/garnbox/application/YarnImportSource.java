package de.appfabrik.garnbox.application;

import de.appfabrik.garnbox.domain.YarnDraft;
import java.util.function.Consumer;

/** Supplies validated entries one at a time. Failures must abort the whole import. */
@FunctionalInterface
public interface YarnImportSource {
  void read(Consumer<YarnDraft> accept);
}
