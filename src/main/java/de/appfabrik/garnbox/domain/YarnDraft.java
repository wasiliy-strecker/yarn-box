package de.appfabrik.garnbox.domain;

/** Complete replacement, including one optional immutable photo. */
public record YarnDraft(
    String name, String brand, String color, String location, String note, Photo photo) {
  public YarnDraft {
    name = name == null ? "" : name.strip();
    if (name.codePoints().noneMatch(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c))) {
      throw new IllegalArgumentException("Bitte gib eine Bezeichnung ein.");
    }
    brand = brand == null ? "" : brand.strip();
    color = color == null ? "" : color.strip();
    location = location == null ? "" : location.strip();
    note = note == null ? "" : note;
  }

  public Yarn withId(long id) {
    return new Yarn(id, name, brand, color, location, note, photo != null);
  }
}
