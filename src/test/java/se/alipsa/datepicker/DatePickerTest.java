package se.alipsa.datepicker;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class DatePickerTest {

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
}
