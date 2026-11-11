package se.alipsa.symp;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.time.YearMonth;
import java.util.Locale;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import org.junit.jupiter.api.Test;

class YearMonthPickerTest {

  private static final YearMonth FROM = YearMonth.of(2020, 3);
  private static final YearMonth TO = YearMonth.of(2021, 9);

  @Test
  void testPopupUsesMonthPatternAndLocale() {
    YearMonthPicker picker = new YearMonthPicker(YearMonth.of(2026, 11), Locale.UK);

    assertEquals("March", render(picker, YearMonth.of(2026, 3)));
  }

  @Test
  void testSetLocaleUpdatesLabelAndPopup() {
    YearMonthPicker picker =
        new YearMonthPicker(FROM, TO, YearMonth.of(2020, 11), Locale.UK, "MMMM", "MMMM yyyy");
    assertEquals("November 2020", label(picker));

    picker.setLocale(new Locale("sv", "SE"));

    assertEquals("november 2020", label(picker));
    assertEquals("mars", render(picker, YearMonth.of(2020, 3)));
  }

  @Test
  void testSetLocaleNullIsIgnored() {
    YearMonthPicker picker = new YearMonthPicker(YearMonth.of(2026, 11), Locale.UK);

    picker.setLocale(null);

    assertEquals(Locale.UK, picker.getLocale());
  }

  @Test
  void testSetSelectedItemUpdatesLabel() {
    YearMonthPicker picker = new YearMonthPicker(YearMonth.of(2026, 11), Locale.UK);

    picker.setSelectedItem(YearMonth.of(2025, 1));

    assertEquals(YearMonth.of(2025, 1), picker.getValue());
    assertEquals("2025-01", label(picker));
  }

  @Test
  void testSetSelectedItemOutsideRangeIsIgnored() {
    YearMonthPicker picker = new YearMonthPicker(FROM, TO, YearMonth.of(2020, 5));

    picker.setSelectedItem(YearMonth.of(2025, 1));

    assertEquals(YearMonth.of(2020, 5), picker.getValue());
    assertEquals("2020-05", label(picker));
  }

  @Test
  void testInitialOutsideRangeLeavesPickerEmpty() {
    YearMonthPicker picker = new YearMonthPicker(FROM, TO, YearMonth.of(2025, 5));

    assertNull(picker.getValue());
    assertEquals("", label(picker));
  }

  @Test
  void testNullArgumentsUseDefaults() {
    YearMonth initial = YearMonth.of(2026, 11);
    YearMonthPicker picker = new YearMonthPicker(null, null, initial, null, null, null);

    assertEquals(initial.minusYears(3), picker.getStart());
    assertEquals(initial.plusYears(3), picker.getEnd());
    assertEquals(Locale.getDefault(), picker.getLocale());
    assertEquals("MMMM", picker.getMonthPattern());
    assertEquals("2026-11", label(picker));
  }

  @Test
  void testFromAfterToThrows() {
    assertThrows(IllegalArgumentException.class, () -> new YearMonthPicker(TO, FROM, null));
  }

  @Test
  void testMonthsInRangeRespectsBothBounds() {
    YearMonthPicker picker = new YearMonthPicker(FROM, TO, null);

    assertEquals(10, picker.monthsInRange(2020).size());
    assertEquals(FROM, picker.monthsInRange(2020).get(0));
    assertEquals(9, picker.monthsInRange(2021).size());
    assertEquals(TO, picker.monthsInRange(2021).get(8));
    assertTrue(picker.monthsInRange(2025).isEmpty());
  }

  @Test
  void testPopupOpensInsideRangeWhenNothingIsSelected() {
    YearMonthPicker past = new YearMonthPicker(FROM, TO, null);
    assertEquals(TO, past.popupBaseMonth());

    YearMonth future = YearMonth.now().plusYears(5);
    YearMonthPicker upcoming = new YearMonthPicker(future, future.plusYears(1), null);
    assertEquals(future, upcoming.popupBaseMonth());
  }

  @Test
  void testListenersOnlyFireForCompletedUserSelectionChanges() {
    YearMonthPicker picker = new YearMonthPicker(FROM, TO, YearMonth.of(2020, 5));
    int[] events = {0};
    picker.addListener(e -> events[0]++);
    DefaultListModel<YearMonth> model = new DefaultListModel<>();
    picker.monthsInRange(2020).forEach(model::addElement);
    picker.listView.setModel(model);
    ListSelectionModel selection = picker.listView.getSelectionModel();

    selection.setValueIsAdjusting(true);
    picker.listView.setSelectedIndex(1);
    assertEquals(0, events[0], "adjusting events are ignored");
    selection.setValueIsAdjusting(false);
    assertEquals(1, events[0]);
    assertEquals(YearMonth.of(2020, 4), picker.getValue());

    // Reopening the popup replaces the model, which clears the selection.
    picker.listView.setModel(model);
    assertEquals(1, events[0]);

    // Picking the month that is already selected is not a change.
    picker.listView.setSelectedIndex(1);
    assertEquals(1, events[0]);
  }

  @Test
  void testEscapeActionIsDisabledWhilePopupIsClosed() {
    YearMonthPicker picker = new YearMonthPicker();

    assertFalse(picker.getActionMap().get("hideYearMonthPopup").isEnabled());
  }

  @Test
  void testPopupLocationStaysOnScreen() {
    Rectangle screen = new Rectangle(1920, 0, 1920, 1080);
    Dimension size = new Dimension(200, 300);

    assertEquals(
        new Point(2000, 540),
        YearMonthPicker.popupLocation(new Point(2000, 540), 500, size, screen));
    assertEquals(
        new Point(3640, 650),
        YearMonthPicker.popupLocation(new Point(3800, 1000), 950, size, screen));
  }

  private static String label(YearMonthPicker picker) {
    try {
      Field field = YearMonthPicker.class.getDeclaredField("inputField");
      field.setAccessible(true);
      return ((JLabel) field.get(picker)).getText();
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private static String render(YearMonthPicker picker, YearMonth value) {
    return ((JLabel)
            picker
                .listView
                .getCellRenderer()
                .getListCellRendererComponent(picker.listView, value, 0, false, false))
        .getText();
  }
}
