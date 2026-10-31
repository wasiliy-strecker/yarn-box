package de.appfabrik.garnbox.ui;

import de.appfabrik.garnbox.application.CollectionExport;
import de.appfabrik.garnbox.application.CollectionImport;
import de.appfabrik.garnbox.application.ImportException;
import de.appfabrik.garnbox.application.YarnService;
import de.appfabrik.garnbox.domain.Yarn;
import de.appfabrik.garnbox.i18n.Messages;
import de.appfabrik.garnbox.persistence.PhotoDirectoryPreferences;
import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

final class MainWindow {
  private final EventHandler<InputEvent> editorInputBlocker = event -> event.consume();
  private final Messages messages;
  private final Stage stage;
  private final YarnService service;
  private final CollectionExport exports;
  private final CollectionImport imports;
  private final AsyncTasks tasks;
  private final PhotoDirectoryPreferences photoDirectories;
  private final TableView<Yarn> table = new TableView<>();
  private final TextField search = new TextField();
  private final Label count = new Label();
  private final Label emptyTitle = new Label();
  private final Label emptyHint = new Label();
  private final Label status = new Label();
  private final TextArea note = new TextArea();
  private final PhotoPreview preview;
  private final de.appfabrik.garnbox.persistence.LanguagePreferences preferences;
  private String languageChoice;
  private final boolean languageReadFailed;
  private final Button language = new Button();
  private final Button add = new Button();
  private final Button edit = new Button();
  private final Button delete = new Button();
  private final Button export = new Button();
  private final Button importYarns = new Button();
  private final List<Yarn> yarns = new ArrayList<>();
  private boolean busy;
  private boolean ready;
  private boolean loadFailed;
  private YarnEditor editor;

  MainWindow(
      Stage stage,
      YarnService service,
      CollectionExport exports,
      CollectionImport imports,
      AsyncTasks tasks,
      Messages messages,
      de.appfabrik.garnbox.persistence.LanguagePreferences preferences,
      PhotoDirectoryPreferences photoDirectories,
      String languageChoice,
      boolean languageReadFailed) {
    this.messages = messages;
    this.preferences = preferences;
    this.photoDirectories = photoDirectories;
    this.languageChoice = languageChoice;
    this.languageReadFailed = languageReadFailed;
    preview = new PhotoPreview(240, 180, messages);
    preview.setId("selected-photo");
    add.setText("+  " + messages.text("yarn.add"));
    edit.setText(messages.text("action.edit"));
    delete.setText(messages.text("action.delete"));
    export.setText(messages.text("action.export"));
    export.setId("export-yarns");
    export.setMinWidth(Region.USE_PREF_SIZE);
    export.setOnAction(event -> exportCollection());
    importYarns.setText(messages.text("action.import"));
    importYarns.setId("import-yarns");
    importYarns.setMinWidth(Region.USE_PREF_SIZE);
    importYarns.setOnAction(event -> importCollection());
    language.setText(messages.text("language.title"));
    language.setId("language-settings");
    language.setOnAction(
        event ->
            new LanguageDialog(
                    stage,
                    messages,
                    tasks,
                    preferences,
                    this.languageChoice,
                    choice -> {
                      this.languageChoice = choice;
                      showStatus(messages.text("language.saved"), false);
                    })
                .show());
    UiSupport.textMenu(search, messages);
    UiSupport.textMenu(note, messages);
    note.setId("selected-note");
    status.setId("status-message");
    count.setId("yarn-count");
    edit.setId("edit-yarn");
    delete.setId("delete-yarn");
    this.stage = stage;
    this.service = service;
    this.exports = exports;
    this.imports = imports;
    this.tasks = tasks;
    Label heading = new Label("GarnBox");
    heading.getStyleClass().add("app-heading");
    Label subname = new Label(messages.text("app.subtitle"));
    subname.getStyleClass().add("muted");
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    add.setId("add-yarn");
    add.getStyleClass().add("primary");
    add.setOnAction(event -> openEditor(null));
    HBox header = new HBox(20, new VBox(4, heading, subname), spacer, add);
    header.setAlignment(Pos.CENTER_LEFT);
    search.setId("search-yarns");
    search.setPromptText(messages.text("search.prompt"));
    search.setAccessibleText(messages.text("search.accessible"));
    search.textProperty().addListener((observable, previous, current) -> refresh(null));
    HBox.setHgrow(search, Priority.ALWAYS);
    Button clear = new Button(messages.text("search.clear"));
    clear.setId("clear-search");
    clear.setOnAction(
        event -> {
          search.clear();
          search.requestFocus();
        });
    clear.disableProperty().bind(search.textProperty().isEmpty());
    HBox searchBar = new HBox(10, search, clear);
    table.setId("yarns-table");
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.getColumns().add(column(messages.text("field.name"), Yarn::name, 360));
    table.getColumns().add(column(messages.text("field.brandShort"), Yarn::brand, 190));
    table.getColumns().add(colorColumn());
    table.getColumns().add(column(messages.text("field.location"), Yarn::location, 240));
    table
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, previous, selected) -> {
              note.setText(selected == null ? "" : selected.note());
              note.setPromptText(
                  selected == null ? messages.text("note.select") : messages.text("note.empty"));
              if (selected == null || !selected.hasPhoto()) {
                preview.showImage(null);
              } else {
                preview.load(() -> service.photo(selected.id()), tasks);
              }
              updateButtons();
            });
    table.setOnMouseClicked(
        event -> {
          if (event.getClickCount() == 2 && event.getTarget() instanceof javafx.scene.Node node) {
            while (node != null && !(node instanceof javafx.scene.control.TableRow<?>)) {
              node = node.getParent();
            }
            if (node instanceof javafx.scene.control.TableRow<?> row && !row.isEmpty()) {
              editSelected();
            }
          }
        });
    table.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER) {
            editSelected();
            event.consume();
          }
          if (event.getCode() == KeyCode.DELETE) {
            deleteSelected();
            event.consume();
          }
        });
    emptyTitle.getStyleClass().add("section-heading");
    emptyHint.getStyleClass().add("muted");
    emptyHint.setWrapText(true);
    emptyHint.setMaxWidth(470);
    VBox placeholder = new VBox(12, emptyTitle, emptyHint);
    placeholder.setAlignment(Pos.CENTER);
    placeholder.setPadding(new Insets(30));
    table.setPlaceholder(placeholder);
    count.getStyleClass().add("muted");
    edit.setOnAction(event -> editSelected());
    delete.setOnAction(event -> deleteSelected());
    delete.getStyleClass().add("danger");
    Region actionsSpacer = new Region();
    HBox.setHgrow(actionsSpacer, Priority.ALWAYS);
    HBox actions = new HBox(10, count, actionsSpacer, edit, delete);
    actions.setAlignment(Pos.CENTER_LEFT);
    Label noteHeading = new Label(messages.text("field.note"));
    noteHeading.getStyleClass().add("field-label");
    note.setEditable(false);
    note.setWrapText(true);
    note.setPrefRowCount(3);
    note.setPromptText(messages.text("note.select"));
    note.setAccessibleText(messages.text("note.accessible"));
    status.setWrapText(true);
    status.setMinHeight(Region.USE_PREF_SIZE);
    status.getStyleClass().add("muted");
    status.setMaxWidth(Double.MAX_VALUE);
    status.setMinWidth(0);
    HBox.setHgrow(status, Priority.ALWAYS);
    MenuItem privacy = new MenuItem(messages.text("privacy.title"));
    privacy.setOnAction(event -> new PrivacyDialog(stage, messages).show());
    MenuButton info = new MenuButton(messages.text("info.title"), null, privacy);
    info.setId("app-info");
    info.setMinWidth(Region.USE_PREF_SIZE);
    language.setMinWidth(Region.USE_PREF_SIZE);
    HBox footer = new HBox(12, status, info, language, export, importYarns);
    footer.setAlignment(Pos.CENTER_LEFT);
    VBox details = new VBox(6, noteHeading, note);
    HBox.setHgrow(details, Priority.ALWAYS);
    VBox root =
        new VBox(18, header, searchBar, table, actions, new HBox(22, preview, details), footer);
    root.setPadding(new Insets(30));
    VBox.setVgrow(table, Priority.ALWAYS);
    Scene scene = new Scene(root, 1080, 800);
    UiSupport.style(scene);
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
            () -> openEditor(null));
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
            () -> {
              search.requestFocus();
              search.selectAll();
            });
    stage.setTitle("GarnBox");
    UiSupport.icon(stage);
    stage.setMinWidth(860);
    stage.setMinHeight(700);
    stage.setScene(scene);
    stage.setOnCloseRequest(
        event -> {
          if (busy) {
            event.consume();
          } else if (editor != null && editor.isShowing()) {
            event.consume();
            editor.requestClose();
          }
        });
  }

  private static TableColumn<Yarn, String> column(
      String name, Function<Yarn, String> value, int width) {
    TableColumn<Yarn, String> column = new TableColumn<>(name);
    column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
    column.setPrefWidth(width);
    column.setSortable(false);
    return column;
  }

  private TableColumn<Yarn, String> colorColumn() {
    TableColumn<Yarn, String> column = column(messages.text("field.color"), Yarn::color, 170);
    column.setCellFactory(
        ignored ->
            new TableCell<>() {
              private final Rectangle swatch = new Rectangle(22, 22);
              private final Label code = new Label();
              private final HBox content = new HBox(8, swatch, code);

              {
                swatch.setArcWidth(6);
                swatch.setArcHeight(6);
                swatch.setStroke(Color.web("#a7aaa3"));
                content.setAlignment(Pos.CENTER_LEFT);
                code.getStyleClass().add("color-code");
              }

              @Override
              protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                setGraphic(null);
                setTooltip(null);
                setAccessibleText(null);
                if (empty || value == null) {
                  return;
                }
                Color selected = YarnColors.parse(value);
                String label = value.isEmpty() ? messages.text("color.none") : value;
                setAccessibleText(messages.text("field.color") + ": " + label);
                setTooltip(new Tooltip(label));
                if (selected == null) {
                  setText(value.isEmpty() ? "—" : value);
                } else {
                  swatch.setFill(selected);
                  code.setText(value);
                  setGraphic(content);
                }
              }
            });
    return column;
  }

  void show() {
    stage.show();
    load();
  }

  private void load() {
    if (busy) {
      return;
    }
    setBusy(true);
    loadFailed = false;
    showStatus(messages.text("loading"), false);
    refresh(null);
    tasks.submit(
        service::load,
        loaded -> {
          yarns.clear();
          yarns.addAll(loaded);
          ready = true;
          setBusy(false);
          refresh(null);
          showStatus(messages.text(languageReadFailed ? "language.loadError" : "stored"), false);
        },
        failure -> {
          loadFailed = true;
          setBusy(false);
          refresh(null);
          showStatus(UiSupport.message(failure, messages), true);
        });
  }

  private void refresh(Long preferredId) {
    Yarn selected = table.getSelectionModel().getSelectedItem();
    Long selection = preferredId != null ? preferredId : selected == null ? null : selected.id();
    List<Yarn> matches = YarnService.search(yarns, search.getText());
    table.getItems().setAll(matches);
    if (selection != null) {
      matches.stream()
          .filter(yarn -> yarn.id() == selection)
          .findFirst()
          .ifPresent(
              yarn -> {
                table.getSelectionModel().select(yarn);
                if (preferredId != null) {
                  table.scrollTo(yarn);
                }
              });
    }
    count.setText(
        !ready ? "" : messages.count(matches.size(), yarns.size(), !search.getText().isBlank()));
    if (loadFailed) {
      emptyTitle.setText(messages.text("empty.failed"));
      emptyHint.setText(messages.text("empty.failedHint"));
    } else if (!ready) {
      emptyTitle.setText(messages.text("loading"));
      emptyHint.setText("");
    } else if (yarns.isEmpty()) {
      emptyTitle.setText(messages.text("empty.title"));
      emptyHint.setText(messages.text("empty.hint"));
    } else {
      emptyTitle.setText(messages.text("empty.search"));
      emptyHint.setText(messages.text("empty.searchHint"));
    }
    updateButtons();
  }

  private void openEditor(Yarn yarn) {
    if (!ready || busy || loadFailed || (editor != null && editor.isShowing())) {
      return;
    }
    setEditorOpening(true);
    tasks.submit(
        () -> yarn == null ? null : service.photo(yarn.id()),
        photo -> {
          setEditorOpening(false);
          editor =
              new YarnEditor(
                  stage,
                  yarn,
                  photo,
                  service,
                  tasks,
                  saved -> {
                    yarns.removeIf(existing -> existing.id() == saved.id());
                    yarns.add(saved);
                    refresh(saved.id());
                    showStatus(
                        YarnService.search(List.of(saved), search.getText()).isEmpty()
                            ? messages.text("saved.filtered")
                            : messages.text("stored"),
                        false);
                  },
                  messages,
                  photoDirectories);
          editor.show();
        },
        failure -> {
          setEditorOpening(false);
          showStatus(UiSupport.message(failure, messages), true);
        });
  }

  private void setEditorOpening(boolean value) {
    busy = value;
    // Block duplicate actions without flashing the list's disabled appearance.
    if (value) {
      stage.getScene().addEventFilter(InputEvent.ANY, editorInputBlocker);
    } else {
      stage.getScene().removeEventFilter(InputEvent.ANY, editorInputBlocker);
    }
  }

  private void editSelected() {
    Yarn selected = table.getSelectionModel().getSelectedItem();
    if (selected != null) {
      openEditor(selected);
    }
  }

  private void exportCollection() {
    if (busy || !ready || loadFailed || (editor != null && editor.isShowing())) {
      return;
    }
    FileChooser chooser = new FileChooser();
    chooser.setTitle(messages.text("export.title"));
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
    chooser.setInitialFileName(
        "yarn-box-"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            + ".json");
    File selected = chooser.showSaveDialog(stage);
    if (selected == null) {
      return;
    }
    File target =
        selected.getName().toLowerCase(Locale.ROOT).endsWith(".json")
            ? selected
            : new File(selected.getPath() + ".json");
    setBusy(true);
    showStatus(messages.text("export.busy"), false);
    tasks.submit(
        () -> exports.export(target.toPath()),
        total -> {
          setBusy(false);
          showStatus(messages.text("export.saved", total, target.getName()), false);
        },
        failure -> {
          setBusy(false);
          showStatus(
              messages.text(
                  failure instanceof FileAlreadyExistsException
                      ? "export.exists"
                      : "export.failed"),
              true);
        });
  }

  private void importCollection() {
    if (busy || !ready || loadFailed || (editor != null && editor.isShowing())) {
      return;
    }
    FileChooser chooser = new FileChooser();
    chooser.setTitle(messages.text("import.title"));
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
    File selected = chooser.showOpenDialog(stage);
    if (selected == null) {
      return;
    }
    setBusy(true);
    showStatus(messages.text("import.busy"), false);
    tasks.submit(
        () -> imports.importFile(selected.toPath()),
        result -> {
          yarns.clear();
          yarns.addAll(result.yarns());
          setBusy(false);
          search.clear();
          refresh(null);
          showStatus(messages.text("import.saved", result.added(), result.skipped()), false);
        },
        failure -> {
          setBusy(false);
          showStatus(
              messages.text(
                  failure instanceof ImportException problem
                      ? "import.error." + problem.code().name()
                      : "import.failed"),
              true);
        });
  }

  private void deleteSelected() {
    Yarn selected = table.getSelectionModel().getSelectedItem();
    if (selected == null || busy || loadFailed) {
      return;
    }
    if (!UiSupport.confirm(
        stage,
        messages.text("delete.title"),
        messages.text("delete.detail", selected.name()),
        messages.text("action.delete"),
        messages.text("action.cancel"))) {
      return;
    }
    setBusy(true);
    showStatus(messages.text("delete.busy"), false);
    tasks.submit(
        () -> {
          service.delete(selected.id());
          return selected;
        },
        deleted -> {
          yarns.removeIf(yarn -> yarn.id() == deleted.id());
          setBusy(false);
          refresh(null);
          showStatus(messages.text("stored"), false);
        },
        failure -> {
          setBusy(false);
          showStatus(UiSupport.message(failure, messages), true);
        });
  }

  private void setBusy(boolean value) {
    busy = value;
    table.setDisable(value);
    updateButtons();
  }

  private void updateButtons() {
    importYarns.setDisable(busy || !ready || loadFailed);
    export.setDisable(busy || !ready || loadFailed);
    add.setDisable(busy || !ready || loadFailed);
    language.setDisable(busy || !ready || loadFailed);
    boolean unavailable = busy || loadFailed || table.getSelectionModel().getSelectedItem() == null;
    edit.setDisable(unavailable);
    delete.setDisable(unavailable);
  }

  private void showStatus(String text, boolean error) {
    status.setText(text);
    status.getStyleClass().removeAll("muted", "error-text");
    status.getStyleClass().add(error ? "error-text" : "muted");
  }
}
