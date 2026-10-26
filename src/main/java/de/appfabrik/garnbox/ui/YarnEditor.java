package de.appfabrik.garnbox.ui;

import de.appfabrik.garnbox.application.PhotoImporter;
import de.appfabrik.garnbox.application.YarnService;
import de.appfabrik.garnbox.domain.Photo;
import de.appfabrik.garnbox.domain.Yarn;
import de.appfabrik.garnbox.domain.YarnDraft;
import java.io.File;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

final class YarnEditor {
  private final Stage stage = new Stage();
  private final TextField name = new TextField();
  private final TextField brand = new TextField();
  private final TextField color = new TextField();
  private final TextField location = new TextField();
  private final TextArea note = new TextArea();
  private final Label validation = new Label();
  private final Label error = new Label();
  private final Label progress = new Label();
  private final Button save = new Button("Speichern");
  private final Button cancel = new Button("Abbrechen");
  private final Button choose = new Button("Foto auswählen …");
  private final Button remove = new Button("Foto entfernen");
  private final PhotoPreview preview = new PhotoPreview(232, 170);
  private final YarnService service;
  private final AsyncTasks tasks;
  private final Long id;
  private final Input original;
  private final Consumer<Yarn> onSaved;
  private Photo photo;
  private boolean busy;

  YarnEditor(
      Window owner,
      Yarn existing,
      Photo existingPhoto,
      YarnService service,
      AsyncTasks tasks,
      Consumer<Yarn> onSaved) {
    this.service = service;
    this.tasks = tasks;
    this.onSaved = onSaved;
    id = existing == null ? null : existing.id();
    photo = existingPhoto;
    if (existing != null) {
      name.setText(existing.name());
      brand.setText(existing.brand());
      color.setText(existing.color());
      location.setText(existing.location());
      note.setText(existing.note());
    }
    original = input();
    stage.initOwner(owner);
    stage.initModality(Modality.WINDOW_MODAL);
    stage.setTitle(existing == null ? "Garn hinzufügen" : "Garn bearbeiten");
    stage.setMinWidth(660);
    stage.setMinHeight(540);
    Label heading = new Label(stage.getTitle());
    heading.getStyleClass().add("section-heading");
    Label hint = new Label("Nur die Bezeichnung ist erforderlich.");
    hint.getStyleClass().add("muted");
    name.setId("yarn-name");
    brand.setId("yarn-brand");
    color.setId("yarn-color");
    location.setId("yarn-location");
    note.setId("yarn-note");
    name.setPromptText("Zum Beispiel Merino weich");
    brand.setPromptText("Optional");
    color.setPromptText("Zum Beispiel Salbeigrün");
    location.setPromptText("Zum Beispiel Schrank, Box 2");
    note.setPromptText("Was möchtest du dir zu diesem Garn merken?");
    note.setWrapText(true);
    note.setPrefRowCount(4);
    for (Label label : new Label[] {validation, error, progress}) {
      label.setWrapText(true);
      label.setMinHeight(Region.USE_PREF_SIZE);
      label.setVisible(false);
      label.managedProperty().bind(label.visibleProperty());
    }
    validation.getStyleClass().add("error-text");
    error.getStyleClass().add("error-text");
    progress.getStyleClass().add("muted");
    name.textProperty()
        .addListener((observable, previous, current) -> validation.setVisible(false));
    save.getStyleClass().add("primary");
    save.setId("save-yarn");
    save.setDefaultButton(true);
    save.setOnAction(event -> save());
    cancel.setCancelButton(true);
    cancel.setOnAction(event -> requestClose());
    choose.setId("choose-photo");
    choose.setOnAction(event -> choosePhoto());
    remove.setId("remove-photo");
    remove.setOnAction(
        event -> {
          photo = null;
          preview.showImage(null);
          updatePhotoButtons();
          error.setVisible(false);
        });
    Label photoHint =
        new Label(
            "Ein JPEG oder PNG. Max. 10 MiB, 6000 Pixel je Seite und 16 Millionen Pixel insgesamt.");
    photoHint.setWrapText(true);
    photoHint.setMaxWidth(232);
    photoHint.getStyleClass().add("muted");
    VBox photoForm = new VBox(10, new Label("Foto · optional"), preview, choose, remove, photoHint);
    VBox fields =
        new VBox(
            12,
            field("Bezeichnung *", name),
            validation,
            field("Marke / Hersteller", brand),
            field("Farbe", color),
            field("Aufbewahrungsort", location),
            field("Notiz", note));
    fields.setMinWidth(280);
    HBox.setHgrow(fields, Priority.ALWAYS);
    HBox columns = new HBox(24, fields, photoForm);
    columns.setPadding(new Insets(2));
    ScrollPane scroll = new ScrollPane(columns);
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    HBox actions = new HBox(10, cancel, save);
    actions.setAlignment(Pos.CENTER_RIGHT);
    VBox root = new VBox(14, heading, hint, scroll, progress, error, actions);
    root.setPadding(new Insets(24));
    VBox.setVgrow(scroll, Priority.ALWAYS);
    Scene scene = new Scene(root, 780, 690);
    UiSupport.style(scene);
    scene
        .getAccelerators()
        .put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), this::save);
    note.addEventFilter(
        javafx.scene.input.KeyEvent.KEY_PRESSED,
        event -> {
          if (event.getCode() == KeyCode.ENTER && !event.isShortcutDown() && !event.isAltDown()) {
            note.replaceSelection("\n");
            event.consume();
          }
        });
    stage.setScene(scene);
    stage.setOnCloseRequest(
        event -> {
          event.consume();
          requestClose();
        });
    if (existingPhoto != null) {
      preview.load(() -> existingPhoto, tasks);
    }
    updatePhotoButtons();
  }

  private static VBox field(String text, javafx.scene.control.Control control) {
    Label label = new Label(text);
    label.setLabelFor(control);
    label.getStyleClass().add("field-label");
    return new VBox(6, label, control);
  }

  void show() {
    stage.show();
    Platform.runLater(name::requestFocus);
  }

  boolean isShowing() {
    return stage.isShowing();
  }

  private Input input() {
    return new Input(
        name.getText(),
        brand.getText(),
        color.getText(),
        location.getText(),
        note.getText(),
        photo);
  }

  private void choosePhoto() {
    if (busy) {
      return;
    }
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Foto auswählen");
    chooser
        .getExtensionFilters()
        .add(
            new FileChooser.ExtensionFilter(
                "JPEG- und PNG-Bilder", "*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"));
    File selected = chooser.showOpenDialog(stage);
    if (selected == null) {
      return;
    }
    setBusy(true, "Foto wird geprüft …");
    error.setVisible(false);
    tasks.submit(
        () -> {
          Photo imported = PhotoImporter.read(selected.toPath());
          return new ImportedPhoto(imported, PhotoPreview.decode(imported));
        },
        imported -> {
          photo = imported.photo();
          preview.showImage(imported.image());
          setBusy(false, "");
        },
        failure -> {
          setBusy(false, "");
          showError(failure);
        });
  }

  private void save() {
    if (busy) {
      return;
    }
    YarnDraft draft;
    try {
      Input current = input();
      draft =
          new YarnDraft(
              current.name(),
              current.brand(),
              current.color(),
              current.location(),
              current.note(),
              current.photo());
    } catch (IllegalArgumentException exception) {
      validation.setText(exception.getMessage());
      validation.setVisible(true);
      name.requestFocus();
      return;
    }
    setBusy(true, "Wird gespeichert …");
    error.setVisible(false);
    tasks.submit(
        () -> service.save(id, draft),
        saved -> {
          setBusy(false, "");
          onSaved.accept(saved);
          stage.hide();
        },
        failure -> {
          setBusy(false, "");
          showError(failure);
        });
  }

  private void showError(Throwable failure) {
    error.setText(UiSupport.message(failure));
    error.setVisible(true);
  }

  private void setBusy(boolean value, String message) {
    busy = value;
    save.setDisable(value);
    cancel.setDisable(value);
    name.setDisable(value);
    brand.setDisable(value);
    color.setDisable(value);
    location.setDisable(value);
    note.setDisable(value);
    progress.setText(message);
    progress.setVisible(value);
    updatePhotoButtons();
  }

  private void updatePhotoButtons() {
    choose.setDisable(busy);
    choose.setText(photo == null ? "Foto auswählen …" : "Foto ersetzen …");
    remove.setDisable(busy || photo == null);
  }

  void requestClose() {
    if (busy) {
      return;
    }
    if (input().equals(original)
        || UiSupport.confirm(
            stage,
            "Änderungen verwerfen?",
            "Deine ungespeicherten Text- und Fotoänderungen gehen beim Verwerfen verloren.",
            "Verwerfen",
            "Weiterbearbeiten")) {
      stage.hide();
    }
  }

  private record Input(
      String name, String brand, String color, String location, String note, Photo photo) {}

  private record ImportedPhoto(Photo photo, Image image) {}
}
