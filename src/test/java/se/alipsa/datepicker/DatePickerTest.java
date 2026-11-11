package se.alipsa.datepicker;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class DatePickerTest {

  @Test
  void testPopupLocationUsesTheAnchorScreenBounds() {
    Point location =
        DatePicker.popupLocation(
            new Point(2100, 500),
            new Point(2100, 450),
            new Dimension(300, 300),
            new Rectangle(1920, 0, 1920, 1080));

    assertEquals(new Point(2100, 500), location);
  }

  @Test
  void testPopupLocationMovesAboveTheAnchorAtBottomOfScreen() {
    Point location =
        DatePicker.popupLocation(
            new Point(2100, 1000),
            new Point(2100, 950),
            new Dimension(300, 300),
            new Rectangle(1920, 0, 1920, 1080));

    assertEquals(new Point(2100, 650), location);
  }

  @Test
  void testDefaultConstructor() {
    DatePicker picker = new DatePicker();
    assertEquals(LocalDate.now(), picker.getDate());
  }

  @Test
  void testConstructorWithInitialDate() {
    LocalDate date = LocalDate.of(2026, 6, 15);
    DatePicker picker = new DatePicker(date);
    assertEquals(date, picker.getDate());
  }

  @Test
  void testSetAndGetDate() {
    DatePicker picker = new DatePicker();
    LocalDate date = LocalDate.of(2026, 12, 25);
    picker.setDate(date);
    assertEquals(date, picker.getDate());
  }

  @Test
  void testClear() {
    DatePicker picker = new DatePicker();
    picker.clear();
    assertNull(picker.getDate());
  }

  @Test
  void testListenerFires() {
    DatePicker picker = new DatePicker();
    LocalDate[] received = {null};
    picker.addListener(date -> received[0] = date);
    LocalDate date = LocalDate.of(2026, 7, 4);
    picker.setDate(date);
    assertEquals(date, received[0]);
  }

  @Test
  void testRemoveListener() {
    DatePicker picker = new DatePicker();
    LocalDate[] received = {null};
    java.util.function.Consumer<LocalDate> listener = date -> received[0] = date;
    picker.addListener(listener);
    picker.removeListener(listener);
    picker.setDate(LocalDate.of(2026, 7, 4));
    assertNull(received[0]);
  }

  @Test
  void testGetStart() {
    LocalDate from = LocalDate.of(2020, 1, 1);
    LocalDate to = LocalDate.of(2030, 12, 31);
    DatePicker picker = new DatePicker(from, to, LocalDate.now());
    assertEquals(from, picker.getStart());
  }

  @Test
  void testGetEnd() {
    LocalDate from = LocalDate.of(2020, 1, 1);
    LocalDate to = LocalDate.of(2030, 12, 31);
    DatePicker picker = new DatePicker(from, to, LocalDate.now());
    assertEquals(to, picker.getEnd());
  }

  @Test
  void testGetDatePattern() {
    DatePicker picker =
        new DatePicker(null, null, LocalDate.now(), Locale.getDefault(), "dd/MM/yyyy");
    assertEquals("dd/MM/yyyy", picker.getDatePattern());
  }

  @Test
  void testVetoPolicyPreventsSelection() {
    DatePicker picker = new DatePicker();
    LocalDate previousDate = picker.getDate();
    picker.setVetoPolicy(date -> date.getDayOfWeek().getValue() != 7);
    LocalDate sunday = LocalDate.of(2026, 4, 19);
    picker.setDate(sunday);
    assertEquals(previousDate, picker.getDate());
  }

  @Test
  void testDefaultTextFieldPosition() {
    DatePicker picker = new DatePicker();
    assertEquals(TextFieldPosition.LEFT, picker.getTextFieldPosition());
  }

  @Test
  void testSetTextFieldPosition() {
    DatePicker picker = new DatePicker();
    picker.setTextFieldPosition(TextFieldPosition.ABOVE);
    assertEquals(TextFieldPosition.ABOVE, picker.getTextFieldPosition());
  }

  @Test
  void testPopupInitiallyClosed() {
    DatePicker picker = new DatePicker();
    assertFalse(picker.isPopupOpen());
  }

  @Test
  void testGetTextField() {
    DatePicker picker = new DatePicker();
    assertNotNull(picker.getTextField());
  }

  @Test
  void testGetCalendarButton() {
    DatePicker picker = new DatePicker();
    assertNotNull(picker.getCalendarButton());
  }

  @Test
  void testSetVetoPolicyClearsNowInvalidSelection() {
    LocalDate selectedDate = LocalDate.of(2026, 4, 20);
    DatePicker picker = new DatePicker(selectedDate);
    LocalDate[] received = {selectedDate};
    int[] eventCount = {0};
    picker.addListener(
        date -> {
          received[0] = date;
          eventCount[0]++;
        });

    picker.setVetoPolicy(date -> !date.equals(selectedDate));

    assertNull(picker.getDate());
    assertNull(picker.getTextField().getDate());
    assertEquals(1, eventCount[0]);
    assertNull(received[0]);
  }

  @Test
  void testCalendarSelectionDoesNotRefireSameDate() throws Exception {
    LocalDate selectedDate = LocalDate.of(2026, 4, 17);
    DatePicker picker = new DatePicker(selectedDate);
    int[] eventCount = {0};
    picker.addListener(date -> eventCount[0]++);

    Method onCalendarDateSelected =
        DatePicker.class.getDeclaredMethod("onCalendarDateSelected", LocalDate.class);
    onCalendarDateSelected.setAccessible(true);
    onCalendarDateSelected.invoke(picker, selectedDate);

    assertEquals(0, eventCount[0]);
  }

  @Test
  void testSetLocaleDerivedPatternUpdates() {
    DatePicker picker = new DatePicker(LocalDate.of(2026, 4, 17), Locale.US);

    picker.setLocale(Locale.GERMANY);

    assertEquals("dd.MM.yyyy", picker.getDatePattern());
  }

  @Test
  void testSwedishLocaleUsesFourDigitYearPattern() {
    DatePicker picker = new DatePicker(LocalDate.of(2026, 4, 17), new Locale("sv", "SE"));

    assertEquals("yyyy-MM-dd", picker.getDatePattern());
  }

  @Test
  void testSetLocaleSwedishUsesFourDigitYearPattern() {
    DatePicker picker = new DatePicker(LocalDate.of(2026, 4, 17), Locale.US);

    picker.setLocale(new Locale("sv", "SE"));

    assertEquals("yyyy-MM-dd", picker.getDatePattern());
  }

  @Test
  void testSetLocaleKeepsExplicitPattern() {
    DatePicker picker =
        new DatePicker(null, null, LocalDate.of(2026, 4, 17), Locale.US, "yyyy-MM-dd");

    picker.setLocale(Locale.GERMANY);

    assertEquals("yyyy-MM-dd", picker.getDatePattern());
  }

  @Test
  void testSetLocalePreservesDate() {
    LocalDate date = LocalDate.of(2026, 12, 25);
    DatePicker picker = new DatePicker(date, Locale.US);

    picker.setLocale(Locale.GERMANY);

    assertEquals(date, picker.getDate());
  }

  @Test
  void testSetLocalePreservesDisabledState() {
    DatePicker picker = new DatePicker(LocalDate.of(2026, 4, 17), Locale.US);
    picker.setEnabled(false);

    picker.setLocale(Locale.GERMANY);

    assertFalse(picker.isEnabled());
    assertFalse(picker.getTextField().isEnabled());
    assertFalse(picker.getCalendarButton().isEnabled());
  }

  @Test
  void testSetLocalePreservesVetoPolicy() {
    DatePicker picker = new DatePicker(LocalDate.of(2026, 4, 17), Locale.US);
    picker.setVetoPolicy(date -> date.getDayOfWeek().getValue() != 7);

    picker.setLocale(Locale.GERMANY);
    picker.setDate(LocalDate.of(2026, 4, 19));

    assertEquals(LocalDate.of(2026, 4, 17), picker.getDate());
  }

  @Test
  void testSetLocaleSameLocaleIsNoOp() {
    DatePicker picker = new DatePicker(LocalDate.of(2026, 4, 17), Locale.US);
    MaskedDateField originalTextField = picker.getTextField();

    picker.setLocale(Locale.US);

    assertSame(originalTextField, picker.getTextField());
  }

  @Test
  void testSetLocaleNullIsIgnored() {
    DatePicker picker = new DatePicker(LocalDate.of(2026, 4, 17), Locale.US);
    MaskedDateField originalTextField = picker.getTextField();

    picker.setLocale(null);

    assertEquals(Locale.US, picker.getLocale());
    assertSame(originalTextField, picker.getTextField());
    assertEquals("MM/dd/yyyy", picker.getDatePattern());
  }

  @Test
  void testTextFieldMinimumSizeMatchesPreferredSize() {
    DatePicker picker = new DatePicker(null, null, null, Locale.US, "yyyy-MM-dd");

    assertEquals(picker.getTextField().getPreferredSize(), picker.getTextField().getMinimumSize());
  }

  @Test
  void testSetLocaleReappliesMinimumSizeToRebuiltTextField() {
    DatePicker picker = new DatePicker(null, null, null, Locale.US, "yyyy-MM-dd");

    picker.setLocale(Locale.GERMANY);

    assertEquals(picker.getTextField().getPreferredSize(), picker.getTextField().getMinimumSize());
  }

  @Test
  void testMinimumSizeKeepsPickerVisibleInNarrowGridBagRow() {
    DatePicker picker = new DatePicker(null, null, null, Locale.US, "yyyy-MM-dd");
    picker.setTextFieldPosition(TextFieldPosition.RIGHT);

    JTextField filler = new JTextField(30);

    JPanel row = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(4, 0, 4, 8);
    row.add(new JLabel("Datum"), gbc);
    gbc.gridx++;
    row.add(picker, gbc);
    gbc.gridx++;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    row.add(filler, gbc);

    row.setSize(row.getPreferredSize().width - 100, row.getPreferredSize().height);
    row.doLayout();
    picker.doLayout();

    assertEquals(picker.getMinimumSize().width, picker.getWidth());
    assertTrue(picker.getTextField().getWidth() > 0);
  }

  @Test
  void testInitialDateOutsideRangeLeavesPickerEmpty() {
    DatePicker picker =
        new DatePicker(
            LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), LocalDate.of(2025, 5, 5));

    assertNull(picker.getDate());
    assertEquals(picker.getTextField().getMaskTemplate(), picker.getTextField().getText());
  }

  @Test
  void testRangeIsValidatedAfterDefaultsAreApplied() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new DatePicker(null, LocalDate.of(1990, 1, 1), null, Locale.UK));
  }

  @Test
  void testTypedDateOutsideRangeIsNotShownAsValue() throws Exception {
    DatePicker picker =
        new DatePicker(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 12, 31),
            LocalDate.of(2020, 6, 1),
            Locale.UK,
            "yyyy-MM-dd");

    SwingUtilities.invokeAndWait(() -> picker.getTextField().setText("2021-06-01"));
    SwingUtilities.invokeAndWait(() -> {});
    Method revert = MaskedDateField.class.getDeclaredMethod("revertIfInvalid");
    revert.setAccessible(true);
    revert.invoke(picker.getTextField());

    assertEquals(LocalDate.of(2020, 6, 1), picker.getDate());
    assertEquals("2020-06-01", picker.getTextField().getText());
  }

  @Test
  void testClearOnEmptyPickerDoesNotNotify() {
    DatePicker picker = new DatePicker(null, null, null, Locale.UK);
    int[] events = {0};
    picker.addListener(date -> events[0]++);

    picker.clear();

    assertEquals(0, events[0]);
  }

  @Test
  void testEscapeActionIsOnlyEnabledWhilePopupIsOpen() {
    DatePicker picker = new DatePicker();
    assertNotNull(picker.getActionMap().get(DatePicker.CLOSE_POPUP_ACTION));
    assertFalse(picker.getActionMap().get(DatePicker.CLOSE_POPUP_ACTION).isEnabled());
  }

  @Test
  void testThrowingListenerDoesNotStopOtherListeners() {
    DatePicker picker = new DatePicker(LocalDate.of(2026, 1, 1), Locale.UK);
    int[] events = {0};
    picker.addListener(
        date -> {
          throw new IllegalStateException("boom (expected in this test)");
        });
    picker.addListener(date -> events[0]++);
    Thread thread = Thread.currentThread();
    Thread.UncaughtExceptionHandler original = thread.getUncaughtExceptionHandler();
    // The exception is logged; an application handler that rethrows must not be involved.
    thread.setUncaughtExceptionHandler(
        (t, e) -> {
          throw new AssertionError("uncaught exception handler must not be called", e);
        });
    try {
      picker.setDate(LocalDate.of(2026, 1, 2));
    } finally {
      thread.setUncaughtExceptionHandler(original);
    }

    assertEquals(1, events[0]);
  }

  @Test
  void testDerivedPatternsRoundTripForAllAvailableLocales() {
    LocalDate date = LocalDate.of(2026, 1, 15);
    for (Locale locale : Locale.getAvailableLocales()) {
      String pattern = DatePicker.derivePattern(locale);
      MaskedDateField field = new MaskedDateField(pattern, locale);
      field.setDate(date);
      String tag = locale.toLanguageTag() + " " + pattern;
      assertTrue(field.isDateValid(), tag);
      assertEquals(field.getMaskTemplate().length(), field.getText().length(), tag);
    }
  }

  @Test
  void testMissingRangeBoundsAreDerivedFromInitialDate() {
    LocalDate initial = LocalDate.of(1990, 1, 1);

    DatePicker open = new DatePicker(null, null, initial, Locale.UK);
    assertEquals(initial, open.getDate());
    assertEquals(initial.minusYears(20), open.getStart());
    assertEquals(initial.plusYears(20), open.getEnd());

    DatePicker onlyTo = new DatePicker(null, LocalDate.of(2000, 1, 1), initial, Locale.UK);
    assertEquals(initial, onlyTo.getDate());
    assertEquals(initial.minusYears(20), onlyTo.getStart());
  }

  @Test
  void testTwoDigitYearPatternUsesWindowCoveringTheRange() throws Exception {
    DatePicker picker =
        new DatePicker(
            LocalDate.of(1970, 1, 1),
            LocalDate.of(2030, 12, 31),
            LocalDate.of(1995, 5, 1),
            Locale.UK,
            "dd/MM/yy");

    assertEquals(LocalDate.of(1995, 5, 1), picker.getDate());
    assertEquals("01/05/95", picker.getTextField().getText());
    assertTrue(picker.getTextField().isDateValid());

    SwingUtilities.invokeAndWait(() -> picker.getTextField().setText("010698"));
    SwingUtilities.invokeAndWait(() -> {});
    assertEquals(LocalDate.of(1998, 6, 1), picker.getDate());

    SwingUtilities.invokeAndWait(() -> picker.getTextField().setText("010625"));
    SwingUtilities.invokeAndWait(() -> {});
    assertEquals(LocalDate.of(2025, 6, 1), picker.getDate());
  }

  @Test
  void testTwoDigitYearPatternRejectsRangesLongerThanOneHundredYears() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DatePicker(
                LocalDate.of(1850, 1, 1),
                LocalDate.of(2100, 12, 31),
                LocalDate.of(2000, 1, 1),
                Locale.UK,
                "dd/MM/yy"));

    DatePicker hundredYears =
        new DatePicker(
            LocalDate.of(1926, 1, 1),
            LocalDate.of(2025, 12, 31),
            LocalDate.of(1926, 6, 1),
            Locale.UK,
            "dd/MM/yy");
    assertEquals(LocalDate.of(1926, 6, 1), hundredYears.getDate());
    assertTrue(hundredYears.getTextField().isDateValid());
  }

  @Test
  void testTextFieldRangeIsManagedByThePicker() {
    DatePicker picker =
        new DatePicker(
            LocalDate.of(2000, 1, 1),
            LocalDate.of(2050, 12, 31),
            LocalDate.of(2026, 1, 1),
            Locale.UK,
            "dd/MM/yy");

    assertThrows(
        IllegalStateException.class,
        () ->
            picker
                .getTextField()
                .setDateRange(LocalDate.of(1900, 1, 1), LocalDate.of(1999, 12, 31)));

    picker.setDate(LocalDate.of(2030, 6, 1));
    assertEquals(LocalDate.of(2030, 6, 1), picker.getDate());
    assertEquals("01/06/30", picker.getTextField().getText());
    assertEquals(LocalDate.of(2030, 6, 1), picker.getTextField().getDate());
  }

  @Test
  void testTextFieldRangeIsStillManagedAfterLocaleChange() {
    DatePicker picker = new DatePicker(LocalDate.of(2026, 1, 1), Locale.US);
    picker.setLocale(Locale.GERMANY);

    assertThrows(IllegalStateException.class, () -> picker.getTextField().setDateRange(null, null));
  }
}
