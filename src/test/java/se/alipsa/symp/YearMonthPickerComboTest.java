package se.alipsa.symp;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.YearMonth;
import java.util.Locale;
import javax.swing.JList;
import org.junit.jupiter.api.Test;

class YearMonthPickerComboTest {

  @Test
  void testNullInitialCanBeRendered() {
    YearMonthPickerCombo combo = new YearMonthPickerCombo(null);
    assertNull(combo.getSelectedItem());

    assertDoesNotThrow(
        () ->
            combo
                .getRenderer()
                .getListCellRendererComponent(new JList<>(), null, -1, false, false));
    combo.setSize(combo.getPreferredSize());
    combo.doLayout();
    BufferedImage image = new BufferedImage(300, 50, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    try {
      assertDoesNotThrow(() -> combo.paint(graphics));
    } finally {
      graphics.dispose();
    }
  }

  @Test
  void testInitialOutsideRangeIsNotSelected() {
    YearMonthPickerCombo combo =
        new YearMonthPickerCombo(
            YearMonth.of(2020, 1), YearMonth.of(2020, 12), YearMonth.of(2025, 5));

    assertNull(combo.getSelectedItem());
    assertEquals(12, combo.getItemCount());
  }

  @Test
  void testInitialInsideRangeIsSelected() {
    YearMonthPickerCombo combo =
        new YearMonthPickerCombo(
            YearMonth.of(2020, 1), YearMonth.of(2020, 12), YearMonth.of(2020, 5));

    assertEquals(YearMonth.of(2020, 5), combo.getSelectedItem());
  }

  @Test
  void testNullArgumentsUseDefaults() {
    YearMonth initial = YearMonth.of(2026, 11);
    YearMonthPickerCombo combo = new YearMonthPickerCombo(null, null, initial, null, null);

    assertEquals(73, combo.getItemCount());
    assertEquals(initial.minusYears(3), combo.getItemAt(0));
    assertEquals(initial, combo.getSelectedItem());
  }

  @Test
  void testFromAfterToThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new YearMonthPickerCombo(
                YearMonth.of(2021, 1), YearMonth.of(2020, 12), null, Locale.UK));
  }
}
