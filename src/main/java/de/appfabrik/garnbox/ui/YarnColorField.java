package de.appfabrik.garnbox.ui;

import de.appfabrik.garnbox.i18n.Messages;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

/** Optional color selection. Opening an old entry never changes its saved text. */
final class YarnColorField extends HBox {
  private final ColorPicker picker = new ColorPicker();
  private final Label description = new Label();
  private final Button clear = new Button("×");
  private final Messages messages;
  private String value;
  private boolean updating;

  YarnColorField(String initialValue, Messages messages) {
    super(10);
    this.messages = messages;
    value = initialValue == null ? "" : initialValue;
    setAlignment(Pos.CENTER_LEFT);
    picker.setId("yarn-color");
    picker.getStyleClass().add("yarn-color-picker");
    picker.setMinWidth(64);
    picker.setPrefWidth(64);
    new CustomColorDialogSizing(picker);
    picker.setTooltip(new Tooltip(messages.text("color.choose")));
    description.setMinWidth(0);
    description.setMaxWidth(Double.MAX_VALUE);
    description.setWrapText(true);
    HBox.setHgrow(description, Priority.ALWAYS);
    clear.setId("clear-yarn-color");
    clear.getStyleClass().add("color-clear");
    clear.setAccessibleText(messages.text("color.clear"));
    clear.setTooltip(new Tooltip(messages.text("color.clear")));
    picker.setOnAction(
        event -> {
          if (!updating) {
            value = YarnColors.encode(picker.getValue());
            refresh();
          }
        });
    clear.setOnAction(
        event -> {
          value = "";
          refresh();
        });
    getChildren().addAll(picker, description, clear);
    refresh();
  }

  String value() {
    return value;
  }

  private void refresh() {
    Color selected = YarnColors.parse(value);
    updating = true;
    try {
      picker.setValue(selected == null ? Color.TRANSPARENT : selected);
    } finally {
      updating = false;
    }
    String label =
        value.isEmpty()
            ? messages.text("color.none")
            : selected == null ? messages.text("color.legacy", value) : value;
    description.setText(label);
    picker.setAccessibleText(messages.text("color.choose") + ": " + label);
    clear.setDisable(value.isEmpty());
  }
}
