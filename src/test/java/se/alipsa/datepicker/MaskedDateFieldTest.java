package se.alipsa.datepicker;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.Locale;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class MaskedDateFieldTest {

  @Test
  void testSetAndGetDate() {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
    LocalDate date = LocalDate.of(2026, 4, 17);
    field.setDate(date);
    assertEquals(date, field.getDate());
    assertEquals("2026-04-17", field.getText());
  }

  @Test
  void testSetDateWithSlashPattern() {
    MaskedDateField field = new MaskedDateField("MM/dd/yyyy", Locale.US);
    LocalDate date = LocalDate.of(2026, 12, 25);
    field.setDate(date);
    assertEquals(date, field.getDate());
    assertEquals("12/25/2026", field.getText());
  }

  @Test
  void testEmptyFieldReturnsNull() {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
    assertNull(field.getDate());
  }

  @Test
  void testGetDatePattern() {
    MaskedDateField field = new MaskedDateField("dd/MM/yyyy", Locale.UK);
    assertEquals("dd/MM/yyyy", field.getDatePattern());
  }

  @Test
  void testIsDateValidWithValidDate() {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
    field.setDate(LocalDate.of(2026, 6, 15));
    assertTrue(field.isDateValid());
  }

  @Test
  void testIsDateValidWhenEmpty() {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
    assertFalse(field.isDateValid());
  }

  @Test
  void testVetoPolicyMarksDateInvalid() {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
    field.setVetoPolicy(date -> !date.getDayOfWeek().name().equals("SUNDAY"));
    LocalDate sunday = LocalDate.of(2026, 4, 19); // a Sunday
    field.setDate(sunday);
    assertFalse(field.isDateValid());
  }

  @Test
  void testMaskTemplate() {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
    assertEquals("____-__-__", field.getMaskTemplate());
  }

  @Test
  void testMaskTemplateSlashFormat() {
    MaskedDateField field = new MaskedDateField("MM/dd/yyyy", Locale.US);
    assertEquals("__/__/____", field.getMaskTemplate());
  }

  @Test
  void testSetDateNull() {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
    field.setDate(LocalDate.of(2026, 1, 1));
    field.setDate(null);
    assertNull(field.getDate());
  }

  @Test
  void testMaskFiltersNonDigits() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());

    SwingUtilities.invokeAndWait(
        () -> {
          try {
            field.getDocument().insertString(0, "20a26-04-17", null);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
    SwingUtilities.invokeAndWait(() -> {});

    assertEquals("2026-04-17", field.getText());
    assertEquals(LocalDate.of(2026, 4, 17), field.getDate());
  }

  @Test
  void testArrowKeysSkipSeparators() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());

    SwingUtilities.invokeAndWait(
        () -> {
          field.setCaretPosition(3);
          KeyEvent right =
              new KeyEvent(
                  field,
                  KeyEvent.KEY_PRESSED,
                  System.currentTimeMillis(),
                  0,
                  KeyEvent.VK_RIGHT,
                  KeyEvent.CHAR_UNDEFINED);
          field.getKeyListeners()[0].keyPressed(right);
        });
    assertEquals(5, field.getCaretPosition());

    SwingUtilities.invokeAndWait(
        () -> {
          KeyEvent left =
              new KeyEvent(
                  field,
                  KeyEvent.KEY_PRESSED,
                  System.currentTimeMillis(),
                  0,
                  KeyEvent.VK_LEFT,
                  KeyEvent.CHAR_UNDEFINED);
          field.getKeyListeners()[0].keyPressed(left);
        });
    assertEquals(3, field.getCaretPosition());
  }
}
