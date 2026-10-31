package de.appfabrik.garnbox.application;

import de.appfabrik.garnbox.domain.YarnDraft;
import java.util.List;

/** Shared bounds for local writes and both directions of the backup format. */
public final class CollectionLimits {
  public static final int MAX_ENTRIES = 10_000;
  public static final int MAX_TEXT_LENGTH = 1_048_576;
  public static final long MAX_FILE_BYTES = 256L * 1024 * 1024;

  private CollectionLimits() {}

  public static void validate(YarnDraft draft) {
    for (String text :
        List.of(draft.name(), draft.brand(), draft.color(), draft.location(), draft.note())) {
      if (text.length() > MAX_TEXT_LENGTH) {
        throw new CollectionLimitException(CollectionLimitException.Code.TEXT);
      }
      for (int i = 0; i < text.length(); i++) {
        char value = text.charAt(i);
        if (Character.isHighSurrogate(value)) {
          if (++i >= text.length() || !Character.isLowSurrogate(text.charAt(i))) {
            throw new IllegalArgumentException("Incomplete Unicode character");
          }
        } else if (Character.isLowSurrogate(value)) {
          throw new IllegalArgumentException("Incomplete Unicode character");
        }
      }
    }
  }
}
