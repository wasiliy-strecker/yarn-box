package de.appfabrik.garnbox.ui;

import de.appfabrik.garnbox.application.ExportFilename;
import de.appfabrik.garnbox.i18n.Messages;
import java.nio.file.Path;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/** A directory grant also covers the temporary sibling used by the atomic writer. */
final class SandboxExportDialog {
  private SandboxExportDialog() {}

  static Path choose(Window owner, Messages messages, String suggestedName) {
    DirectoryChooser chooser = new DirectoryChooser();
    chooser.setTitle(messages.text("transfer.exportFolderTitle"));
    var folder = chooser.showDialog(owner);
    return folder == null ? null : filename(owner, messages, folder.toPath(), suggestedName);
  }

  static Path filename(Window owner, Messages messages, Path folder, String suggestedName) {
    var dialog = new TextInputDialog(suggestedName);
    dialog.initOwner(owner);
    dialog.setTitle(messages.text("export.title"));
    dialog.setHeaderText(messages.text("transfer.exportFilenameHint"));
    dialog.setContentText(messages.text("transfer.exportFilename"));
    dialog.getEditor().setId("export-filename");
    dialog.getDialogPane().setPrefWidth(560);
    UiSupport.style(dialog.getDialogPane().getScene());
    var export = new ButtonType(messages.text("action.export"), ButtonType.OK.getButtonData());
    var cancel = new ButtonType(messages.text("action.cancel"), ButtonType.CANCEL.getButtonData());
    dialog.getDialogPane().getButtonTypes().setAll(export, cancel);
    var button = dialog.getDialogPane().lookupButton(export);
    button.setId("export-filename-confirm");
    Runnable validate =
        () -> {
          try {
            ExportFilename.resolve(folder, dialog.getEditor().getText());
            button.setDisable(false);
          } catch (IllegalArgumentException exception) {
            button.setDisable(true);
          }
        };
    dialog.getEditor().textProperty().addListener(observable -> validate.run());
    dialog.setResultConverter(type -> type == export ? dialog.getEditor().getText() : null);
    validate.run();
    return dialog.showAndWait().map(name -> ExportFilename.resolve(folder, name)).orElse(null);
  }
}
