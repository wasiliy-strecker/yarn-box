package de.appfabrik.garnbox.application;

/** Transactional import port, separate from ordinary single-entry operations. */
public interface CollectionImportRepository {
  ImportResult importYarns(YarnImportSource source);
}
