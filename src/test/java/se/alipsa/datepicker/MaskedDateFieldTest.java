package se.alipsa.datepicker;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Locale;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
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
  void testStrikethroughClearsForAllowedDate() {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
    field.setVetoPolicy(date -> date.getDayOfWeek().getValue() != 7);

    field.setDate(LocalDate.of(2026, 4, 19));
    assertEquals(
        TextAttribute.STRIKETHROUGH_ON,
        field.getFont().getAttributes().get(TextAttribute.STRIKETHROUGH));

    field.setDate(LocalDate.of(2026, 4, 20));
    assertNotEquals(
        TextAttribute.STRIKETHROUGH_ON,
        field.getFont().getAttributes().get(TextAttribute.STRIKETHROUGH));
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

  @Test
  void testTypingFullYearAdvancesPastSeparator() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());

    SwingUtilities.invokeAndWait(
        () -> {
          try {
            field.getDocument().insertString(0, "2026", null);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
    SwingUtilities.invokeAndWait(() -> {});

    assertEquals("2026-__-__", field.getText());
    assertEquals(5, field.getCaretPosition());
  }

  @Test
  void testTypingSeparatorJumpsToNextSection() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());

    SwingUtilities.invokeAndWait(
        () -> {
          field.setText("2026-__-__");
          field.setCaretPosition(4);
          KeyEvent dash =
              new KeyEvent(
                  field,
                  KeyEvent.KEY_TYPED,
                  System.currentTimeMillis(),
                  0,
                  KeyEvent.VK_UNDEFINED,
                  '-');
          field.getKeyListeners()[0].keyTyped(dash);
        });

    assertEquals(5, field.getCaretPosition());
  }

  @Test
  void testTypingSlashJumpsToNextSectionRegardlessOfPattern() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());

    SwingUtilities.invokeAndWait(
        () -> {
          field.setCaretPosition(2);
          KeyEvent slash =
              new KeyEvent(
                  field,
                  KeyEvent.KEY_TYPED,
                  System.currentTimeMillis(),
                  0,
                  KeyEvent.VK_UNDEFINED,
                  '/');
          field.getKeyListeners()[0].keyTyped(slash);
        });

    assertEquals(5, field.getCaretPosition());
  }

  @Test
  void testTypingDotJumpsToFollowingSectionRegardlessOfPattern() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());

    SwingUtilities.invokeAndWait(
        () -> {
          field.setCaretPosition(5);
          KeyEvent dot =
              new KeyEvent(
                  field,
                  KeyEvent.KEY_TYPED,
                  System.currentTimeMillis(),
                  0,
                  KeyEvent.VK_UNDEFINED,
                  '.');
          field.getKeyListeners()[0].keyTyped(dot);
        });

    assertEquals(8, field.getCaretPosition());
  }

  @Test
  void testBackspaceDeletesAcrossSeparatorBoundary() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
    field.setDate(LocalDate.of(2026, 4, 18));

    SwingUtilities.invokeAndWait(
        () -> {
          field.setCaretPosition(5);
          KeyEvent backspace =
              new KeyEvent(
                  field,
                  KeyEvent.KEY_PRESSED,
                  System.currentTimeMillis(),
                  0,
                  KeyEvent.VK_BACK_SPACE,
                  KeyEvent.CHAR_UNDEFINED);
          field.getKeyListeners()[0].keyPressed(backspace);
          field.getKeyListeners()[0].keyPressed(backspace);
        });

    assertEquals("202_-_4-18", field.getText());
    assertEquals(3, field.getCaretPosition());
  }

  @Test
  void testNonEditableKeyPressedDoesNotChangeTextOrCaret() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
    field.setDate(LocalDate.of(2026, 4, 18));
    field.setEditable(false);

    SwingUtilities.invokeAndWait(
        () -> {
          field.setCaretPosition(5);
          for (int keyCode :
              new int[] {
                KeyEvent.VK_BACK_SPACE, KeyEvent.VK_DELETE, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT
              }) {
            KeyEvent event =
                new KeyEvent(
                    field,
                    KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(),
                    0,
                    keyCode,
                    KeyEvent.CHAR_UNDEFINED);
            field.getKeyListeners()[0].keyPressed(event);
            assertTrue(event.isConsumed());
          }
        });

    assertEquals("2026-04-18", field.getText());
    assertEquals(5, field.getCaretPosition());
  }

  @Test
  void testNonEditableKeyTypedDoesNotMoveCaret() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
    field.setEditable(false);

    SwingUtilities.invokeAndWait(
        () -> {
          field.setCaretPosition(4);
          KeyEvent dash =
              new KeyEvent(
                  field,
                  KeyEvent.KEY_TYPED,
                  System.currentTimeMillis(),
                  0,
                  KeyEvent.VK_UNDEFINED,
                  '-');
          field.getKeyListeners()[0].keyTyped(dash);
          assertTrue(dash.isConsumed());
        });

    assertEquals(4, field.getCaretPosition());
  }

  @Test
  void testNonEditableDocumentChangesAreIgnoredButSetDateStillWorks() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
    field.setEditable(false);

    SwingUtilities.invokeAndWait(
        () -> {
          try {
            field.getDocument().insertString(0, "20260418", null);
            field.getDocument().remove(0, 1);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
    SwingUtilities.invokeAndWait(() -> {});

    assertEquals("____-__-__", field.getText());

    field.setDate(LocalDate.of(2026, 4, 18));

    assertEquals("2026-04-18", field.getText());
    assertEquals(LocalDate.of(2026, 4, 18), field.getDate());
  }

  @Test
  void testQuotedLiteralIsFixedTextAndDateCanBeTyped() throws Exception {
    MaskedDateField field = new MaskedDateField("dd.MM.yyyy 'г'.", Locale.forLanguageTag("bg-BG"));
    assertEquals("__.__.____ г.", field.getMaskTemplate());

    SwingUtilities.invokeAndWait(() -> field.setText("15012026"));
    SwingUtilities.invokeAndWait(() -> {});

    assertEquals("15.01.2026 г.", field.getText());
    assertEquals(LocalDate.of(2026, 1, 15), field.getDate());
  }

  @Test
  void testEscapedQuoteInPattern() {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd''", Locale.UK);
    assertEquals("____-__-__'", field.getMaskTemplate());

    field.setDate(LocalDate.of(2026, 1, 15));

    assertEquals("2026-01-15'", field.getText());
    assertTrue(field.isDateValid());
  }

  @Test
  void testUnsupportedPatternsAreRejected() {
    for (String pattern :
        new String[] {"dd MMM yyyy", "d/M/yyyy", "yyyy-MM", "yyyy[-MM-dd]", "yyyy-MM-dd 'x"}) {
      assertThrows(
          IllegalArgumentException.class, () -> new MaskedDateField(pattern, Locale.UK), pattern);
    }
  }

  @Test
  void testTypedDateOutsideRangeIsRejectedAndReverted() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.UK);
    field.setDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31));
    field.setDate(LocalDate.of(2020, 6, 1));
    LocalDate[] received = {null};
    field.addListener(date -> received[0] = date);

    SwingUtilities.invokeAndWait(() -> field.setText("2021-06-01"));
    SwingUtilities.invokeAndWait(() -> {});

    assertFalse(field.isDateValid());
    assertEquals(LocalDate.of(2020, 6, 1), field.getDate());
    assertNull(received[0]);
    assertEquals(
        TextAttribute.STRIKETHROUGH_ON,
        field.getFont().getAttributes().get(TextAttribute.STRIKETHROUGH));

    Method revert = MaskedDateField.class.getDeclaredMethod("revertIfInvalid");
    revert.setAccessible(true);
    revert.invoke(field);

    assertEquals("2020-06-01", field.getText());
  }

  @Test
  void testReplacingSelectionBlanksTheSelectedDigits() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.UK);
    field.setDate(LocalDate.of(2026, 6, 15));

    SwingUtilities.invokeAndWait(
        () -> {
          field.selectAll();
          field.replaceSelection("1");
        });
    SwingUtilities.invokeAndWait(() -> {});

    assertEquals("1___-__-__", field.getText());
    assertEquals(LocalDate.of(2026, 6, 15), field.getDate());
  }

  @Test
  void testBackspaceClearsWholeSelection() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.UK);
    field.setDate(LocalDate.of(2026, 6, 15));

    SwingUtilities.invokeAndWait(
        () -> {
          field.select(5, 10);
          KeyEvent backspace =
              new KeyEvent(
                  field,
                  KeyEvent.KEY_PRESSED,
                  System.currentTimeMillis(),
                  0,
                  KeyEvent.VK_BACK_SPACE,
                  KeyEvent.CHAR_UNDEFINED);
          field.getKeyListeners()[0].keyPressed(backspace);
          assertTrue(backspace.isConsumed());
        });

    assertEquals("2026-__-__", field.getText());
    assertEquals(5, field.getCaretPosition());
  }

  @Test
  void testSetTextEmptyClearsTheMask() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.UK);
    field.setDate(LocalDate.of(2026, 6, 15));

    SwingUtilities.invokeAndWait(() -> field.setText(""));
    SwingUtilities.invokeAndWait(() -> {});

    assertEquals("____-__-__", field.getText());
  }

  @Test
  void testCustomFontStyleIsKept() {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.UK);
    field.setFont(field.getFont().deriveFont(Font.BOLD));

    field.setDate(LocalDate.of(2026, 1, 1));

    assertTrue(field.getFont().isBold());
  }

  @Test
  void testTwoDigitYearDefaultsToTwentyFirstCentury() {
    MaskedDateField field = new MaskedDateField("dd/MM/yy", Locale.UK);
    field.setDate(LocalDate.of(2026, 1, 15));

    assertEquals("15/01/26", field.getText());
    assertTrue(field.isDateValid());
  }

  @Test
  void testCustomForegroundIsKeptAcrossEdits() throws Exception {
    MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.UK);
    field.setForeground(Color.BLUE);

    field.setDate(LocalDate.of(2026, 1, 1));
    assertEquals(Color.BLUE, field.getForeground());

    SwingUtilities.invokeAndWait(() -> field.setText("2026-13-01"));
    SwingUtilities.invokeAndWait(() -> {});
    assertNotEquals(Color.BLUE, field.getForeground(), "invalid text is still marked");

    field.setDate(LocalDate.of(2026, 2, 1));
    assertEquals(Color.BLUE, field.getForeground());
  }

  @Test
  void testLookAndFeelChangeWhileInvalidAppliesNewThemeColor() throws Exception {
    LookAndFeel originalLaf = UIManager.getLookAndFeel();
    // Metal only replaces UIResource values on update; some platform themes (e.g. Aqua) always do.
    UIManager.setLookAndFeel(new MetalLookAndFeel());
    Object originalForeground = UIManager.get("TextField.foreground");
    try {
      MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.UK);
      SwingUtilities.invokeAndWait(() -> field.setText("2026-13-01"));
      SwingUtilities.invokeAndWait(() -> {});
      assertEquals(Color.RED, field.getForeground());

      ColorUIResource themeColor = new ColorUIResource(0, 128, 0);
      UIManager.put("TextField.foreground", themeColor);
      field.updateUI();
      field.setDate(LocalDate.of(2026, 1, 1));

      assertEquals(themeColor, field.getForeground());
    } finally {
      UIManager.put("TextField.foreground", originalForeground);
      UIManager.setLookAndFeel(originalLaf);
    }
  }

  @Test
  void testLookAndFeelChangeWhileStruckThroughAppliesNewThemeFont() throws Exception {
    LookAndFeel originalLaf = UIManager.getLookAndFeel();
    UIManager.setLookAndFeel(new MetalLookAndFeel());
    Object originalFont = UIManager.get("TextField.font");
    try {
      MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.UK);
      field.setVetoPolicy(date -> date.getDayOfWeek().getValue() != 7);
      field.setDate(LocalDate.of(2026, 4, 19)); // a Sunday, shown struck through

      FontUIResource themeFont = new FontUIResource("Serif", Font.BOLD, 30);
      UIManager.put("TextField.font", themeFont);
      field.updateUI();
      field.setDate(LocalDate.of(2026, 4, 20));

      assertEquals(themeFont, field.getFont());
    } finally {
      UIManager.put("TextField.font", originalFont);
      UIManager.setLookAndFeel(originalLaf);
    }
  }

  @Test
  void testTwoDigitYearFieldIgnoresDatesOutsideItsWindow() {
    MaskedDateField field = new MaskedDateField("dd/MM/yy", Locale.UK);

    field.setDate(LocalDate.of(1995, 6, 1));

    assertNull(field.getDate());
    assertEquals(field.getMaskTemplate(), field.getText());
  }
}
