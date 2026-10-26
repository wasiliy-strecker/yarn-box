package de.appfabrik.garnbox.ui;

import de.appfabrik.garnbox.application.YarnService;
import de.appfabrik.garnbox.persistence.AppDataDirectory;
import de.appfabrik.garnbox.persistence.SqliteYarnRepository;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public final class YarnsApplication extends Application {
  private final AsyncTasks tasks = new AsyncTasks();

  @Override
  public void start(Stage stage) {
    try {
      String configured = getParameters().getNamed().get("data-dir");
      Path directory = configured == null ? AppDataDirectory.current() : Path.of(configured);
      if (configured != null && configured.isBlank()) {
        throw new IllegalArgumentException("Empty data directory");
      }
      YarnService service =
          new YarnService(new SqliteYarnRepository(directory.resolve("garnbox.db")));
      new MainWindow(stage, service, tasks).show();
    } catch (RuntimeException exception) {
      Alert alert =
          new Alert(
              Alert.AlertType.ERROR,
              "GarnBox konnte nicht gestartet werden. Bitte prüfe den angegebenen Datenpfad.");
      alert.setHeaderText("Start nicht möglich");
      alert.showAndWait();
      javafx.application.Platform.exit();
    }
  }

  @Override
  public void stop() {
    tasks.close();
  }
}
