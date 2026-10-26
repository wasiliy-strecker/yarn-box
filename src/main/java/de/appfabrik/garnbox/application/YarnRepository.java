package de.appfabrik.garnbox.application;

import de.appfabrik.garnbox.domain.Photo;
import de.appfabrik.garnbox.domain.Yarn;
import de.appfabrik.garnbox.domain.YarnDraft;
import java.util.List;

/** Storage port. Methods complete only after the database operation succeeds. */
public interface YarnRepository {
  void initialize();

  List<Yarn> findAll();

  Photo findPhoto(long id);

  Yarn create(YarnDraft draft);

  Yarn update(long id, YarnDraft draft);

  void delete(long id);
}
