package de.appfabrik.garnbox.ui;

import de.appfabrik.garnbox.application.CollectionImport;
import de.appfabrik.garnbox.i18n.AppLanguage;
import de.appfabrik.garnbox.i18n.Messages;
import de.appfabrik.garnbox.persistence.JsonCollectionFile;
import de.appfabrik.garnbox.persistence.LanguagePreferences;
import de.appfabrik.garnbox.persistence.SqliteYarnRepository;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Asset capture only. No test assertions, personal data or behavior-verification suite. */
public final class MacStoreDrafts {
  private static volatile Throwable failure;

  public static void main(String[] args) throws Exception {
    Path data = Path.of(args[0]);
    Path output = Path.of(args[1]);
    if (!Files.isRegularFile(data.resolve(".synthetic-yarnbox-demo"))
        || Files.exists(data.resolve("garnbox.db"))) {
      throw new IllegalArgumentException("A new marked synthetic data folder is required");
    }
    var repository = new SqliteYarnRepository(data.resolve("garnbox.db"));
    repository.initialize();
    new CollectionImport(repository, new JsonCollectionFile())
        .importFile(Path.of("assets/store_demo/yarn-box-de-DE.json"));
    new LanguagePreferences(data).save("de-DE");
    Thread capture = new Thread(() -> {
      try {
        await(() -> find("add-yarn") != null && !find("add-yarn").isDisabled());
        Stage main = fx(() -> (Stage) find("add-yarn").getScene().getWindow());
        fx(() -> {
          var screen = Screen.getPrimary().getBounds();
          main.setX(0);
          main.setY(0);
          main.setWidth(screen.getWidth());
          main.setHeight(screen.getHeight());
          ((TableView<?>) find("yarns-table")).getSelectionModel().selectFirst();
          return null;
        });
        await(() -> photoVisible("selected-photo"));
        screenshot(output.resolve("01-overview.png"));
        press("edit-yarn");
        await(() -> find("yarn-name") != null && photoVisible("editor-photo"));
        fx(() -> { ((ColorPicker) find("yarn-color")).show(); return null; });
        screenshot(output.resolve("02-edit.png"));
        fx(() -> { ((ColorPicker) find("yarn-color")).hide(); return null; });
        press("cancel-yarn");
        fx(() -> { ((TextInputControl) find("search-yarns")).setText("Strickjacke"); return null; });
        Thread.sleep(600);
        fx(() -> { ((TableView<?>) find("yarns-table")).getSelectionModel().selectFirst(); return null; });
        await(() -> photoVisible("selected-photo"));
        screenshot(output.resolve("03-search.png"));
        press("clear-search");
        // Show the actual Store filename dialog, without pretending this is a Mac build.
        Platform.runLater(() -> SandboxExportDialog.filename(main, new Messages(AppLanguage.GERMAN),
            data, "Meine Garne.json"));
        await(() -> find("export-filename") != null);
        screenshot(output.resolve("04-json-export.png"));
      } catch (Throwable problem) {
        failure = problem;
      } finally {
        Platform.runLater(() -> {
          for (Window window : java.util.List.copyOf(Window.getWindows())) window.hide();
          Platform.exit();
        });
      }
    }, "store-asset-capture");
    capture.setDaemon(true);
    capture.start();
    Application.launch(YarnsApplication.class, "--data-dir=" + data);
    if (failure != null) throw new IllegalStateException("Asset capture failed", failure);
  }

  private static Node find(String id) {
    for (Window window : Window.getWindows()) {
      if (window.isShowing() && window.getScene() != null) {
        Node node = window.getScene().lookup("#" + id);
        if (node != null && node.isVisible()) return node;
      }
    }
    return null;
  }

  private static boolean photoVisible(String id) {
    Node node = find(id);
    return node != null && node.lookup("#photo-image") instanceof ImageView view
        && view.getImage() != null;
  }

  private static void press(String id) throws Exception {
    fx(() -> { ((Button) find(id)).fire(); return null; });
    Thread.sleep(350);
  }

  private static <T> T fx(Callable<T> action) throws Exception {
    FutureTask<T> task = new FutureTask<>(action);
    Platform.runLater(task);
    return task.get(15, TimeUnit.SECONDS);
  }

  private static void await(Callable<Boolean> available) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
    while (System.nanoTime() < deadline) {
      try {
        if (fx(available)) return;
      } catch (IllegalStateException starting) {
        // Application.launch has not completed platform startup yet.
      }
      Thread.sleep(100);
    }
    throw new IllegalStateException("The view needed for the screenshot did not become available");
  }

  private static void screenshot(Path destination) throws Exception {
    Thread.sleep(800);
    BufferedImage pixels = fx(() -> {
      var bounds = Screen.getPrimary().getBounds();
      var capture = new Robot().getScreenCapture(null, 0, 0,
          bounds.getWidth(), bounds.getHeight(), false);
      int width = (int) capture.getWidth();
      int height = (int) capture.getHeight();
      if (width != 2560 || height != 1600) throw new IllegalStateException("Wrong capture size");
      int[] rgb = new int[width * height];
      capture.getPixelReader().getPixels(0, 0, width, height,
          PixelFormat.getIntArgbInstance(), rgb, 0, width);
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      image.setRGB(0, 0, width, height, rgb, 0, width);
      return image;
    });
    if (!ImageIO.write(pixels, "png", destination.toFile())) {
      throw new IllegalStateException("PNG writer unavailable");
    }
  }
}
