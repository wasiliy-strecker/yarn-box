package de.appfabrik.garnbox;

import de.appfabrik.garnbox.ui.YarnsApplication;
import javafx.application.Application;

/** Separate launcher also supports a non-modular, self-contained jpackage image. */
public final class Launcher {
  private Launcher() {}

  public static void main(String[] args) {
    if (args.length == 1 && "--version".equals(args[0])) {
      String version = Launcher.class.getPackage().getImplementationVersion();
      System.out.println("Yarn Box " + (version == null ? "development" : version));
      return;
    }
    Application.launch(YarnsApplication.class, args);
  }
}
