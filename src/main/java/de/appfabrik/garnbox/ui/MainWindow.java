package de.appfabrik.garnbox.ui;

import de.appfabrik.garnbox.application.YarnService;
import de.appfabrik.garnbox.domain.Yarn;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

final class MainWindow {
  private final Stage stage;
  private final YarnService service;
  private final AsyncTasks tasks;
  private final TableView<Yarn> table = new TableView<>();
  private final TextField search = new TextField();
  private final Label count = new Label();
  private final Label emptyTitle = new Label();
  private final Label emptyHint = new Label();
  private final Label status = new Label();
  private final TextArea note = new TextArea();
  private final PhotoPreview preview = new PhotoPreview(240, 180);
  private final Button add = new Button("+  Garn hinzufügen");
  private final Button edit = new Button("Bearbeiten");
  private final Button delete = new Button("Löschen");
  private final Button reload = new Button("Neu laden");
  private final List<Yarn> yarns = new ArrayList<>();
  private boolean busy;
  private boolean ready;
  private boolean loadFailed;
  private YarnEditor editor;

  MainWindow(Stage stage, YarnService service, AsyncTasks tasks) {
    this.stage = stage;
    this.service = service;
    this.tasks = tasks;
    Label heading = new Label("GarnBox");
    heading.getStyleClass().add("app-heading");
    Label subname = new Label("Deine Garne. Gut aufgehoben.");
    subname.getStyleClass().add("muted");
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    add.setId("add-yarn");
    add.getStyleClass().add("primary");
    add.setOnAction(event -> openEditor(null));
    HBox header = new HBox(20, new VBox(4, heading, subname), spacer, add);
    header.setAlignment(Pos.CENTER_LEFT);
    search.setId("search-yarns");
    search.setPromptText("Bezeichnung, Marke, Farbe, Aufbewahrungsort oder Notiz suchen");
    search.setAccessibleText("Garne durchsuchen");
    search.textProperty().addListener((observable, previous, current) -> refresh(null));
    HBox.setHgrow(search, Priority.ALWAYS);
    Button clear = new Button("Suche leeren");
    clear.setOnAction(
        event -> {
          search.clear();
          search.requestFocus();
        });
    clear.disableProperty().bind(search.textProperty().isEmpty());
    HBox searchBar = new HBox(10, search, clear);
    table.setId("yarns-table");
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.getColumns().add(column("Bezeichnung", Yarn::name, 360));
    table.getColumns().add(column("Marke", Yarn::brand, 190));
    table.getColumns().add(column("Farbe", Yarn::color, 170));
    table.getColumns().add(column("Aufbewahrungsort", Yarn::location, 240));
    table
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, previous, selected) -> {
              note.setText(selected == null ? "" : selected.note());
              note.setPromptText(
                  selected == null
                      ? "Wähle ein Garn aus, um seine Notiz zu lesen."
                      : "Für dieses Garn ist keine Notiz hinterlegt.");
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
    Label noteHeading = new Label("Notiz");
    noteHeading.getStyleClass().add("field-label");
    note.setEditable(false);
    note.setWrapText(true);
    note.setPrefRowCount(3);
    note.setPromptText("Wähle ein Garn aus, um seine Notiz zu lesen.");
    note.setAccessibleText("Notiz des ausgewählten Garnes");
    status.setWrapText(true);
    status.setMinHeight(Region.USE_PREF_SIZE);
    status.getStyleClass().add("muted");
    status.setMaxWidth(Double.MAX_VALUE);
    status.setMinWidth(0);
    HBox.setHgrow(status, Priority.ALWAYS);
    reload.setOnAction(event -> load());
    reload.setPrefWidth(120);
    reload.setMinWidth(Region.USE_PREF_SIZE);
    HBox footer = new HBox(16, status, reload);
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
    showStatus("Sammlung wird geöffnet …", false);
    refresh(null);
    tasks.submit(
        service::load,
        loaded -> {
          yarns.clear();
          yarns.addAll(loaded);
          ready = true;
          setBusy(false);
          refresh(null);
          showStatus("Lokal auf diesem Computer gespeichert", false);
        },
        failure -> {
          loadFailed = true;
          setBusy(false);
          refresh(null);
          showStatus(UiSupport.message(failure), true);
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
        !ready
            ? ""
            : search.getText().isBlank()
                ? yarns.size() + (yarns.size() == 1 ? " Garneintrag" : " Garneinträge")
                : matches.size() + " von " + yarns.size() + " Garneinträgen");
    if (loadFailed) {
      emptyTitle.setText("Sammlung nicht geladen");
      emptyHint.setText("Bitte beachte den Hinweis unten und versuche es mit „Neu laden“ erneut.");
    } else if (!ready) {
      emptyTitle.setText("Deine Sammlung wird geöffnet …");
      emptyHint.setText("");
    } else if (yarns.isEmpty()) {
      emptyTitle.setText("Platz für dein erstes Garn");
      emptyHint.setText(
          "Mit „Garn hinzufügen“ legst du deine Sammlung an. Eine Bezeichnung genügt für den Anfang.");
    } else {
      emptyTitle.setText("Keine Garne gefunden");
      emptyHint.setText(
          "Versuche einen anderen Suchbegriff oder leere die Suche, um alle Garne zu sehen.");
    }
    updateButtons();
  }

  private void openEditor(Yarn yarn) {
    if (!ready || busy || loadFailed || (editor != null && editor.isShowing())) {
      return;
    }
    setBusy(true);
    tasks.submit(
        () -> yarn == null ? null : service.photo(yarn.id()),
        photo -> {
          setBusy(false);
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
                            ? "Garn gespeichert. Es passt nicht zur aktuellen Suche."
                            : "Lokal auf diesem Computer gespeichert",
                        false);
                  });
          editor.show();
        },
        failure -> {
          setBusy(false);
          showStatus(UiSupport.message(failure), true);
        });
  }

  private void editSelected() {
    Yarn selected = table.getSelectionModel().getSelectedItem();
    if (selected != null) {
      openEditor(selected);
    }
  }

  private void deleteSelected() {
    Yarn selected = table.getSelectionModel().getSelectedItem();
    if (selected == null || busy || loadFailed) {
      return;
    }
    if (!UiSupport.confirm(
        stage,
        "Garn löschen?",
        "„" + selected.name() + "“ wird dauerhaft aus deiner Sammlung entfernt.",
        "Löschen",
        "Abbrechen")) {
      return;
    }
    setBusy(true);
    showStatus("Garn wird gelöscht …", false);
    tasks.submit(
        () -> {
          service.delete(selected.id());
          return selected;
        },
        deleted -> {
          yarns.removeIf(yarn -> yarn.id() == deleted.id());
          setBusy(false);
          refresh(null);
          showStatus("Lokal auf diesem Computer gespeichert", false);
        },
        failure -> {
          setBusy(false);
          showStatus(UiSupport.message(failure), true);
        });
  }

  private void setBusy(boolean value) {
    busy = value;
    table.setDisable(value);
    reload.setDisable(value);
    updateButtons();
  }

  private void updateButtons() {
    add.setDisable(busy || !ready || loadFailed);
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
