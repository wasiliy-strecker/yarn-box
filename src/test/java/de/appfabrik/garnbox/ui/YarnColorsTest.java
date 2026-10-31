package de.appfabrik.garnbox.ui;

import static org.junit.jupiter.api.Assertions.*;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class YarnColorsTest {
  @Test
  void preservesOpaqueAndTransparentSelections() {
    for (String value : new String[] {"#ABCDEF", "#12345678", "#00000000", "#000000"}) {
      assertEquals(value, YarnColors.encode(YarnColors.parse(value)));
    }
    assertEquals("#FFFFFF", YarnColors.encode(Color.WHITE));
  }

  @Test
  void leavesLegacyDescriptionsDistinctFromActualSelections() {
    for (String value : new String[] {"Salbeigrün", "", "blue", "#FFF", "#GGGGGG"}) {
      assertNull(YarnColors.parse(value));
    }
    assertNull(YarnColors.parse(null));
  }
}
