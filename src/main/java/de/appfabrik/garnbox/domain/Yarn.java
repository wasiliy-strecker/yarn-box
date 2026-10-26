package de.appfabrik.garnbox.domain;

/** Lightweight list entry. Image bytes are loaded only for the selected entry. */
public record Yarn(
    long id,
    String name,
    String brand,
    String color,
    String location,
    String note,
    boolean hasPhoto) {}
