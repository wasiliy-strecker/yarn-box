package de.appfabrik.garnbox.ui;

import de.appfabrik.garnbox.domain.Photo;
import de.appfabrik.garnbox.domain.YarnDraft;
import de.appfabrik.garnbox.i18n.AppLanguage;
import de.appfabrik.garnbox.i18n.Messages;
import de.appfabrik.garnbox.persistence.LanguagePreferences;
import de.appfabrik.garnbox.persistence.SqliteYarnRepository;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;

/** Drives the production application with actual mouse/keyboard events on an isolated display. */
public final class GuiJourney {
  private static final List<String> checks = new ArrayList<>();
  private static String failure;

  private GuiJourney() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      throw new IllegalArgumentException("data-dir output-dir locale mode");
    }
    Path data = Path.of(args[0]).toAbsolutePath();
    Path output = Path.of(args[1]).toAbsolutePath();
    String tag = args[2];
    String mode = args[3];
    if (!Files.isRegularFile(data.resolve(".synthetic-yarnbox-demo"))) {
      throw new IllegalArgumentException(
          "An isolated, explicitly marked synthetic data directory is required");
    }
    Files.createDirectories(output);
    AppLanguage language =
        mode.equals("startup") ? AppLanguage.ENGLISH : AppLanguage.resolve(tag, Locale.US);
    Messages messages = new Messages(language);
    Properties demo = new Properties();
    try (var reader =
        Files.newBufferedReader(
            Path.of("assets/store_demo", tag + ".properties"), StandardCharsets.UTF_8)) {
      demo.load(reader);
    }
    if (!mode.equals("restart")) {
      if (Files.exists(data.resolve("garnbox.db"))) {
        throw new IllegalArgumentException("Fresh demo directory required");
      }
      var repository = new SqliteYarnRepository(data.resolve("garnbox.db"));
      repository.initialize();
      for (int i = 1; i <= 5; i++) {
        repository.create(
            new YarnDraft(
                demo.getProperty("yarn." + i + ".name"),
                demo.getProperty("yarn." + i + ".brand"),
                demo.getProperty("yarn." + i + ".color"),
                demo.getProperty("yarn." + i + ".location"),
                demo.getProperty("yarn." + i + ".note"),
                demo.getProperty("yarn." + i + ".photo", "").isEmpty()
                    ? null
                    : new Photo(
                        Files.readAllBytes(Path.of(demo.getProperty("yarn." + i + ".photo"))))));
      }
      if (!mode.equals("startup")) {
        new LanguagePreferences(data).save(tag);
      }
    }
    // A deliberately different system language proves the saved preference wins at real startup.
    Locale.setDefault(mode.equals("startup") ? Locale.GERMANY : Locale.JAPAN);
    Thread driver =
        new Thread(
            () -> {
              try {
                waitFor(
                    () -> find("yarn-count") instanceof Label label && !label.getText().isEmpty());
                check(
                    fx(() -> ((Button) find("add-yarn")).getText())
                        .equals("+  " + messages.text("yarn.add")),
                    "saved language applied at startup");
                if (mode.equals("startup")) {
                  check(
                      !Files.exists(data.resolve("language.txt")),
                      "first start does not write a language choice");
                  check(
                      new LanguagePreferences(data).load().equals("en-US"),
                      "first start uses English on a German system");
                  screenshot(output.resolve("startup.png"));
                } else if (mode.equals("capture")) {
                  captureJourney(output, messages, demo);
                } else if (mode.equals("verify")) {
                  verifyJourney(data, output, messages);
                } else if (mode.equals("restart")) {
                  check(
                      new LanguagePreferences(data).load().equals(tag),
                      "preference survived process restart");
                  var yarns = new SqliteYarnRepository(data.resolve("garnbox.db")).findAll();
                  check(
                      yarns.stream()
                          .anyMatch(
                              yarn ->
                                  yarn.name().equals("L'été d'Álvaro – ação – Bücher")
                                      && yarn.note().contains("Line 30")),
                      "Unicode and multiline draft survived restart");
                  var saved =
                      yarns.stream()
                          .filter(yarn -> yarn.name().equals("L'été d'Álvaro – ação – Bücher"))
                          .findFirst()
                          .orElseThrow();
                  check(
                      Arrays.equals(
                          new SqliteYarnRepository(data.resolve("garnbox.db"))
                              .findPhoto(saved.id())
                              .bytes(),
                          de.appfabrik.garnbox.application.PhotoImporter.read(
                                  Path.of("assets/store_demo/images/sage.png"))
                              .bytes()),
                      "photo bytes survive language change and restart");
                  enter("search-yarns", "ÁLVARO");
                  waitFor(() -> ((TableView<?>) find("yarns-table")).getItems().size() == 1);
                  clickTableFirstRow();
                  waitFor(() -> photoVisible("selected-photo"));
                  check(
                      fx(
                          () ->
                              find("yarns-table").lookupAll(".color-code").stream()
                                  .filter(
                                      node ->
                                          node instanceof Label label && !label.getText().isEmpty())
                                  .allMatch(
                                      node ->
                                          javafx.scene.paint.Color.web("#253c34")
                                              .equals(((Label) node).getTextFill()))),
                      "selected color code keeps dark readable text");
                  screenshot(output.resolve("restart.png"));
                } else {
                  throw new IllegalArgumentException("Unknown mode");
                }
                Files.write(output.resolve("checks.txt"), checks, StandardCharsets.UTF_8);
              } catch (Exception | AssertionError exception) {
                failure = exception.toString();
                exception.printStackTrace();
                try {
                  screenshot(output.resolve("failure.png"));
                } catch (Exception ignored) {
                  /* Toolkit may be unavailable. */
                }
              } finally {
                Platform.runLater(
                    () -> {
                      for (Window window : List.copyOf(Window.getWindows())) {
                        window.hide();
                      }
                      Platform.exit();
                    });
              }
            },
            "yarnbox-gui-driver");
    driver.setDaemon(true);
    driver.start();
    Application.launch(YarnsApplication.class, "--data-dir=" + data);
    driver.join(10000);
    if (failure != null) {
      throw new IllegalStateException(failure);
    }
    if (driver.isAlive()) {
      throw new IllegalStateException("GUI driver did not finish");
    }
    System.out.println("GUI " + mode + " passed: " + tag + " (" + checks.size() + " checks)");
  }

  private static void captureJourney(Path output, Messages messages, Properties demo)
      throws Exception {
    fx(
        () -> {
          Stage main = (Stage) find("add-yarn").getScene().getWindow();
          main.setX(0);
          main.setY(0);
          main.setWidth(1920);
          main.setHeight(1080);
          return null;
        });
    pause();
    clickTableFirstRow();
    waitFor(() -> photoVisible("selected-photo"));
    screenshot(output.resolve("01-overview.png"));
    click("edit-yarn");
    waitFor(() -> find("yarn-name") != null);
    waitFor(() -> photoVisible("editor-photo"));
    screenshot(output.resolve("02-edit.png"));
    click("cancel-yarn");
    waitFor(() -> find("yarn-name") == null);
    enter("search-yarns", demo.getProperty("search"));
    waitFor(() -> ((TableView<?>) find("yarns-table")).getItems().size() == 1);
    clickTableFirstRow();
    waitFor(() -> photoVisible("selected-photo"));
    screenshot(output.resolve("03-search.png"));
    click("clear-search");
    click("language-settings");
    waitFor(() -> find("language-choice") != null);
    click("language-choice");
    screenshot(output.resolve("04-language.png"));
    key(KeyCode.ESCAPE);
    click("language-cancel");
    check(true, "four real 1920x1080 screen captures");
    check(
        fx(() -> ((Label) find("yarn-count")).getText()).equals(messages.count(5, 5, false)),
        "localized collection count");
  }

  private static void verifyJourney(Path data, Path output, Messages messages) throws Exception {
    fx(
        () -> {
          Stage main = (Stage) find("add-yarn").getScene().getWindow();
          main.setWidth(860);
          main.setHeight(700);
          return null;
        });
    pause();
    checkButtonsFit();
    screenshot(output.resolve("minimum-main.png"));
    click("add-yarn");
    waitFor(() -> find("save-yarn") != null);
    click("save-yarn");
    waitFor(() -> find("name-validation") instanceof Label label && label.isVisible());
    check(
        fx(() -> ((Label) find("name-validation")).getText())
            .equals(messages.text("validation.name")),
        "localized required-name validation");
    String title = "L'été d'Álvaro – ação – Bücher";
    enter("yarn-name", title);
    enter("yarn-brand", "Élise O'Connor");
    click("yarn-color");
    fx(
        () -> {
          Bounds bounds = find("yarn-color").localToScreen(find("yarn-color").getBoundsInLocal());
          Robot robot = new Robot();
          robot.mouseMove(bounds.getMinX() + 60, bounds.getMaxY() + 68);
          robot.mouseClick(MouseButton.PRIMARY);
          return null;
        });
    pause();
    check(
        fx(() -> ((javafx.scene.control.ColorPicker) find("yarn-color")).getValue().getOpacity())
            > 0,
        "actual palette selection");
    enter("yarn-location", "São Paulo · étagère 12");
    enter(
        "yarn-note",
        "Line 1 – Grüße, coração, français\\n".replace("\\n", "\n")
            + "A long note with accents.\n".repeat(28)
            + "Line 30");
    importPhoto(Path.of("assets/store_demo/images/sage.png"), messages);
    waitFor(() -> photoVisible("editor-photo"));
    Path invalid = data.resolve("invalid.png");
    Files.writeString(invalid, "This is not a photograph");
    importPhoto(invalid, messages);
    waitFor(() -> find("save-error") instanceof Label label && label.isVisible());
    check(
        fx(() -> ((Label) find("save-error")).getText())
            .equals(messages.text("photoError.INVALID")),
        "invalid photo message is localized");
    check(fx(() -> photoVisible("editor-photo")), "invalid import retains the previous preview");
    importPhoto(Path.of("assets/store_demo/images/lilac.jpg"), messages);
    waitFor(() -> photoVisible("editor-photo") && !find("choose-photo").isDisabled());
    key(KeyCode.ESCAPE);
    waitFor(() -> confirmationButton(messages.text("action.keep")) != null);
    check(
        fx(() -> confirmationButton(messages.text("action.keep")).isDefaultButton()),
        "keep editing is the safe default");
    key(KeyCode.ENTER);
    waitFor(() -> confirmationButton(messages.text("action.keep")) == null);
    check(
        fx(() -> ((TextInputControl) find("yarn-name")).getText()).equals(title),
        "draft survives Escape and continue");
    // Force a real SQLite write failure while preserving the entire edit form.
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + data.resolve("garnbox.db"));
        var statement = connection.createStatement()) {
      statement.execute("BEGIN EXCLUSIVE");
      click("save-yarn");
      waitFor(() -> find("save-error") instanceof Label label && label.isVisible());
      check(
          fx(() -> ((Label) find("save-error")).getText())
              .equals(messages.text("storage.OPERATION")),
          "localized storage failure");
      check(
          fx(() -> ((TextInputControl) find("yarn-note")).getText()).contains("Line 30"),
          "full draft survives database lock");
      fx(
          () -> {
            Stage editor = (Stage) find("yarn-name").getScene().getWindow();
            editor.setWidth(660);
            editor.setHeight(540);
            return null;
          });
      pause();
      checkButtonsFit();
      check(fx(() -> photoVisible("editor-photo")), "photo draft survives database lock");
      screenshot(output.resolve("minimum-editor-error.png"));
      statement.execute("ROLLBACK");
    }
    click("save-yarn");
    waitFor(() -> find("save-yarn") == null);
    check(
        new SqliteYarnRepository(data.resolve("garnbox.db")).findAll().size() == 6,
        "retry saves exactly one yarn");
    enter("search-yarns", "ÁLVARO");
    waitFor(() -> ((TableView<?>) find("yarns-table")).getItems().size() == 1);
    clickTableFirstRow();
    click("delete-yarn");
    waitFor(() -> confirmationButton(messages.text("action.cancel")) != null);
    check(
        fx(() -> confirmationButton(messages.text("action.cancel")).isDefaultButton()),
        "delete defaults to cancel");
    key(KeyCode.ESCAPE);
    waitFor(() -> confirmationButton(messages.text("action.cancel")) == null);
    check(
        new SqliteYarnRepository(data.resolve("garnbox.db")).findAll().size() == 6,
        "cancelled deletion preserves data");
    var repository = new SqliteYarnRepository(data.resolve("garnbox.db"));
    long savedId =
        repository.findAll().stream()
            .filter(yarn -> yarn.name().equals(title))
            .findFirst()
            .orElseThrow()
            .id();
    check(
        Arrays.equals(
            repository.findPhoto(savedId).bytes(),
            de.appfabrik.garnbox.application.PhotoImporter.read(
                    Path.of("assets/store_demo/images/lilac.jpg"))
                .bytes()),
        "JPEG replacement saved atomically with text after retry");
    click("edit-yarn");
    waitFor(() -> find("remove-photo") != null && !find("remove-photo").isDisabled());
    click("remove-photo");
    check(!fx(() -> photoVisible("editor-photo")), "photo removal clears preview");
    key(KeyCode.ESCAPE);
    waitFor(() -> confirmationButton(messages.text("action.discard")) != null);
    fx(
        () -> {
          mouse(confirmationButton(messages.text("action.discard")));
          return null;
        });
    waitFor(() -> find("save-yarn") == null);
    check(repository.findPhoto(savedId) != null, "discarding photo removal preserves stored image");
    click("edit-yarn");
    waitFor(() -> find("remove-photo") != null && !find("remove-photo").isDisabled());
    click("remove-photo");
    click("save-yarn");
    waitFor(() -> find("save-yarn") == null);
    check(repository.findPhoto(savedId) == null, "saved photo removal clears the blob");
    click("edit-yarn");
    waitFor(() -> find("choose-photo") != null);
    importPhoto(Path.of("assets/store_demo/images/sage.png"), messages);
    waitFor(() -> photoVisible("editor-photo") && !find("choose-photo").isDisabled());
    click("save-yarn");
    waitFor(() -> find("save-yarn") == null);
    check(
        Arrays.equals(
            repository.findPhoto(savedId).bytes(),
            de.appfabrik.garnbox.application.PhotoImporter.read(
                    Path.of("assets/store_demo/images/sage.png"))
                .bytes()),
        "new PNG saved after removal");
    click("clear-search");
    click("language-settings");
    waitFor(() -> find("language-choice") != null);
    Path preference = data.resolve("language.txt");
    Path backup = data.resolve("language.before-test");
    Files.move(preference, backup);
    Files.createDirectory(preference);
    Files.writeString(preference.resolve("blocker"), "synthetic");
    click("language-save");
    waitFor(() -> find("language-error") instanceof Label label && label.isVisible());
    check(
        fx(() -> ((Label) find("language-error")).getText())
            .equals(messages.text("language.saveError")),
        "settings write failure preserves dialog");
    Files.delete(preference.resolve("blocker"));
    Files.delete(preference);
    Files.move(backup, preference);
    click("language-choice");
    key(KeyCode.HOME);
    // automatic, German, English. Switch every language to another language for the restart test.
    String next = messages.locale().getLanguage().equals("en") ? "de-DE" : "en-US";
    key(KeyCode.DOWN);
    if (next.equals("en-US")) {
      key(KeyCode.DOWN);
    }
    key(KeyCode.ENTER);
    click("language-save");
    waitFor(() -> find("language-choice") == null);
    check(new LanguagePreferences(data).load().equals(next), "new language selection saved");
    check(
        fx(() -> ((Label) find("status-message")).getText())
            .equals(messages.text("language.saved")),
        "restart notice in current language");
    Files.writeString(output.resolve("restart-locale.txt"), next + "\n");
  }

  private static boolean photoVisible(String previewId) {
    Node preview = find(previewId);
    return preview != null
        && preview.lookup("#photo-image") instanceof ImageView view
        && view.getImage() != null;
  }

  private static void importPhoto(Path path, Messages messages) throws Exception {
    click("choose-photo");
    String window = null;
    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    while (window == null && System.nanoTime() < deadline) {
      Process search =
          new ProcessBuilder(
                  "xdotool",
                  "search",
                  "--onlyvisible",
                  "--name",
                  "^" + messages.text("photo.chooserTitle") + "$")
              .redirectError(ProcessBuilder.Redirect.DISCARD)
              .start();
      String result =
          new String(search.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      if (search.waitFor() == 0 && !result.isEmpty()) {
        window = result.lines().findFirst().orElseThrow();
      } else {
        pause();
      }
    }
    if (window == null) {
      throw new AssertionError("Native photo chooser did not open");
    }
    xdo("windowfocus", "--sync", window);
    xdo("key", "--clearmodifiers", "ctrl+l");
    pause();
    xdo("type", "--clearmodifiers", "--delay", "1", path.toAbsolutePath().toString());
    xdo("key", "--clearmodifiers", "Return");
    Thread.sleep(600);
  }

  private static void xdo(String... arguments) throws Exception {
    var command = new ArrayList<String>();
    command.add("xdotool");
    command.addAll(List.of(arguments));
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
      process.destroyForcibly();
      throw new AssertionError("X11 input failed: " + output);
    }
  }

  private static void checkButtonsFit() throws Exception {
    fx(
        () -> {
          for (Window window : Window.getWindows()) {
            if (!window.isShowing() || window.getScene() == null) {
              continue;
            }
            for (Node node : window.getScene().getRoot().lookupAll(".button")) {
              if (node instanceof Button button && button.isVisible()) {
                check(
                    button.getWidth() + 1 >= button.prefWidth(-1),
                    "button fits: " + button.getText());
                Bounds bounds = button.localToScene(button.getBoundsInLocal());
                check(
                    bounds.getMaxY() <= window.getScene().getHeight() + 1,
                    "button is within window");
              }
            }
          }
          return null;
        });
  }

  private static void clickTableFirstRow() throws Exception {
    fx(
        () -> {
          Node row =
              find("yarns-table").lookupAll(".table-row-cell").stream()
                  .filter(
                      node ->
                          node instanceof TableRow<?> candidate
                              && candidate.getIndex() == 0
                              && !candidate.isEmpty())
                  .findFirst()
                  .orElse(null);
          if (row == null) {
            throw new AssertionError("No visible table row");
          }
          mouse(row);
          return null;
        });
    pause();
  }

  private static Button confirmationButton(String text) {
    for (Window window : Window.getWindows()) {
      if (window.isShowing() && window.getScene() != null) {
        for (Node node : window.getScene().getRoot().lookupAll(".button")) {
          if (node instanceof Button button
              && button.getText().equals(text)
              && button.getScene().getRoot().getStyleClass().contains("dialog-pane")) {
            return button;
          }
        }
      }
    }
    return null;
  }

  private static Node find(String id) {
    for (Window window : Window.getWindows()) {
      if (window.isShowing() && window.getScene() != null) {
        Node node = window.getScene().lookup("#" + id);
        if (node != null && node.isVisible()) {
          return node;
        }
      }
    }
    return null;
  }

  private static void click(String id) throws Exception {
    fx(
        () -> {
          Node node = find(id);
          if (node == null || node.isDisabled()) {
            throw new AssertionError("Control unavailable: " + id);
          }
          mouse(node);
          return null;
        });
    pause();
  }

  private static void mouse(Node node) {
    Bounds b = node.localToScreen(node.getBoundsInLocal());
    Robot robot = new Robot();
    robot.mouseMove(b.getCenterX(), b.getCenterY());
    robot.mouseClick(MouseButton.PRIMARY);
  }

  private static void enter(String id, String text) throws Exception {
    click(id);
    fx(
        () -> {
          ClipboardContent content = new ClipboardContent();
          content.putString(text);
          Clipboard.getSystemClipboard().setContent(content);
          Robot robot = new Robot();
          robot.keyPress(KeyCode.CONTROL);
          robot.keyType(KeyCode.A);
          robot.keyType(KeyCode.V);
          robot.keyRelease(KeyCode.CONTROL);
          return null;
        });
    pause();
    check(fx(() -> ((TextInputControl) find(id)).getText()).equals(text), "keyboard input: " + id);
  }

  private static void key(KeyCode key) throws Exception {
    fx(
        () -> {
          new Robot().keyType(key);
          return null;
        });
    pause();
  }

  private static void screenshot(Path file) throws Exception {
    pause();
    BufferedImage image =
        fx(
            () -> {
              var capture = new Robot().getScreenCapture(null, 0, 0, 1920, 1080, false);
              int[] pixels = new int[1920 * 1080];
              capture
                  .getPixelReader()
                  .getPixels(0, 0, 1920, 1080, PixelFormat.getIntArgbInstance(), pixels, 0, 1920);
              BufferedImage result = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
              result.setRGB(0, 0, 1920, 1080, pixels, 0, 1920);
              return result;
            });
    if (!ImageIO.write(image, "png", file.toFile())) {
      throw new IllegalStateException("PNG encoder missing");
    }
  }

  private static <T> T fx(Callable<T> operation) throws Exception {
    FutureTask<T> task = new FutureTask<>(operation);
    Platform.runLater(task);
    return task.get(10, TimeUnit.SECONDS);
  }

  private static void waitFor(Callable<Boolean> condition) throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    while (System.nanoTime() < deadline) {
      try {
        if (fx(condition)) {
          return;
        }
      } catch (IllegalStateException notStarted) {
        /* JavaFX startup is asynchronous. */
      }
      Thread.sleep(100);
    }
    throw new AssertionError("Timed out waiting for GUI state");
  }

  private static void pause() throws InterruptedException {
    Thread.sleep(250);
  }

  private static void check(boolean passed, String description) {
    if (!passed) {
      throw new AssertionError(description);
    }
    checks.add(description);
  }
}
