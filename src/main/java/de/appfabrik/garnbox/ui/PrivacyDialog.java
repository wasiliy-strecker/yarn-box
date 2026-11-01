package de.appfabrik.garnbox.ui;

import de.appfabrik.garnbox.i18n.Messages;
import de.appfabrik.garnbox.i18n.PrivacyNotice;
import javafx.stage.Window;

final class PrivacyDialog {
  private final InfoDialog dialog;

  PrivacyDialog(Window owner, Messages messages) {
    dialog =
        new InfoDialog(
            owner,
            messages,
            messages.text("privacy.title"),
            PrivacyNotice.text(messages),
            "privacy");
  }

  void show() {
    dialog.show();
  }
}
