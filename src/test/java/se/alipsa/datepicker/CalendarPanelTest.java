package se.alipsa.datepicker;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.font.TextAttribute;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JLabel;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

class CalendarPanelTest {

  @Test
  void testDefaultConstructorUsesToday() {
    CalendarPanel panel = new CalendarPanel();
    assertEquals(LocalDate.now(), panel.getSelectedDate());
    assertEquals(YearMonth.now(), panel.getDisplayedYearMonth());
  }

  @Test
  void testConstructorWithInitialDate() {
    LocalDate initial = LocalDate.of(2026, 6, 15);
    CalendarPanel panel = new CalendarPanel(initial);
    assertEquals(initial, panel.getSelectedDate());
    assertEquals(YearMonth.of(2026, 6), panel.getDisplayedYearMonth());
  }

  @Test
  void testNextMonth() {
    LocalDate initial = LocalDate.of(2026, 3, 10);
    CalendarPanel panel = new CalendarPanel(initial);
    panel.nextMonth();
    assertEquals(YearMonth.of(2026, 4), panel.getDisplayedYearMonth());
  }

  @Test
  void testPreviousMonth() {
    LocalDate initial = LocalDate.of(2026, 3, 10);
    CalendarPanel panel = new CalendarPanel(initial);
    panel.previousMonth();
    assertEquals(YearMonth.of(2026, 2), panel.getDisplayedYearMonth());
  }

  @Test
  void testNextMonthRespectsUpperBound() {
    LocalDate initial = LocalDate.of(2026, 3, 10);
    LocalDate from = LocalDate.of(2025, 1, 1);
    LocalDate to = LocalDate.of(2026, 3, 31);
    CalendarPanel panel = new CalendarPanel(from, to, initial, Locale.getDefault());
    panel.nextMonth();
    assertEquals(YearMonth.of(2026, 3), panel.getDisplayedYearMonth());
  }

  @Test
  void testPreviousMonthRespectsLowerBound() {
    LocalDate initial = LocalDate.of(2025, 1, 15);
    LocalDate from = LocalDate.of(2025, 1, 1);
    LocalDate to = LocalDate.of(2027, 12, 31);
    CalendarPanel panel = new CalendarPanel(from, to, initial, Locale.getDefault());
    panel.previousMonth();
    assertEquals(YearMonth.of(2025, 1), panel.getDisplayedYearMonth());
  }

  @Test
  void testSetSelectedDate() {
    CalendarPanel panel = new CalendarPanel();
    LocalDate date = LocalDate.of(2026, 8, 20);
    panel.setSelectedDate(date);
    assertEquals(date, panel.getSelectedDate());
    assertEquals(YearMonth.of(2026, 8), panel.getDisplayedYearMonth());
  }

  @Test
  void testSetDisplayedYearMonth() {
    CalendarPanel panel = new CalendarPanel();
    panel.setDisplayedYearMonth(YearMonth.of(2027, 1));
    assertEquals(YearMonth.of(2027, 1), panel.getDisplayedYearMonth());
  }

  @Test
  void testListenerFiresOnSetSelectedDate() {
    CalendarPanel panel = new CalendarPanel();
    LocalDate[] received = {null};
    panel.addListener(date -> received[0] = date);
    LocalDate date = LocalDate.of(2026, 5, 1);
    panel.setSelectedDate(date);
    assertEquals(date, received[0]);
  }

  @Test
  void testSetSelectedDateRespectsVeto() {
    CalendarPanel panel = new CalendarPanel();
    panel.setVetoPolicy(date -> date.getDayOfWeek().getValue() != 7); // no Sundays
    LocalDate sunday = LocalDate.of(2026, 4, 19);
    LocalDate before = panel.getSelectedDate();
    panel.setSelectedDate(sunday);
    assertEquals(before, panel.getSelectedDate()); // should not change
  }

  @Test
  void testRangeDefaults() {
    LocalDate initial = LocalDate.of(2026, 6, 15);
    CalendarPanel panel = new CalendarPanel(initial);
    LocalDate expectedFrom = initial.minusYears(20);
    panel.setDisplayedYearMonth(YearMonth.from(expectedFrom));
    panel.previousMonth();
    assertEquals(YearMonth.from(expectedFrom), panel.getDisplayedYearMonth());
  }

  @Test
  void testHighlightPolicyCalledOncePerCell() {
    CalendarPanel panel = new CalendarPanel(LocalDate.of(2026, 4, 17));
    AtomicInteger calls = new AtomicInteger();

    panel.setHighlightPolicy(
        date -> {
          calls.incrementAndGet();
          return null;
        });

    assertEquals(42, calls.get());
  }

  @Test
  void testHighlightTooltipAppliedToSelectedDate() throws Exception {
    LocalDate selected = LocalDate.of(2026, 4, 17);
    CalendarPanel panel = new CalendarPanel(selected);
    panel.setHighlightPolicy(
        date -> date.equals(selected) ? new HighlightInfo(Color.YELLOW, "Selected") : null);

    JLabel selectedCell = findCell(panel, selected);
    assertNotNull(selectedCell);
    assertEquals("Selected", selectedCell.getToolTipText());
  }

  private JLabel findCell(CalendarPanel panel, LocalDate date) throws Exception {
    Field dateCellValuesField = CalendarPanel.class.getDeclaredField("dateCellValues");
    dateCellValuesField.setAccessible(true);
    LocalDate[][] values = (LocalDate[][]) dateCellValuesField.get(panel);

    Field dateCellsField = CalendarPanel.class.getDeclaredField("dateCells");
    dateCellsField.setAccessible(true);
    JLabel[][] cells = (JLabel[][]) dateCellsField.get(panel);

    for (int row = 0; row < values.length; row++) {
      for (int col = 0; col < values[row].length; col++) {
        if (date.equals(values[row][col])) {
          return cells[row][col];
        }
      }
    }
    return null;
  }

  @Test
  void testStrikethroughIsClearedAfterNavigatingAwayFromVetoedDate() throws Exception {
    LocalDate vetoed = LocalDate.of(2026, 11, 12);
    CalendarPanel panel = new CalendarPanel(LocalDate.of(2026, 11, 1), Locale.UK);
    panel.setVetoPolicy(date -> !date.equals(vetoed));
    JLabel cell = findCell(panel, vetoed);
    assertEquals(
        TextAttribute.STRIKETHROUGH_ON,
        cell.getFont().getAttributes().get(TextAttribute.STRIKETHROUGH));

    panel.nextMonth();

    assertNotEquals(
        TextAttribute.STRIKETHROUGH_ON,
        cell.getFont().getAttributes().get(TextAttribute.STRIKETHROUGH));
  }

  @Test
  void testNullArgumentsUseDefaults() {
    CalendarPanel panel = new CalendarPanel(null, null, null, null);
    assertNull(panel.getSelectedDate());
    assertEquals(YearMonth.now(), panel.getDisplayedYearMonth());

    CalendarPanel noInitial = new CalendarPanel((LocalDate) null);
    assertNull(noInitial.getSelectedDate());
  }

  @Test
  void testInitialOutsideRangeShowsClosestMonthWithoutSelection() {
    CalendarPanel panel =
        new CalendarPanel(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 12, 31),
            LocalDate.of(2026, 11, 11),
            Locale.UK);

    assertNull(panel.getSelectedDate());
    assertEquals(YearMonth.of(2020, 12), panel.getDisplayedYearMonth());
  }

  @Test
  void testFromAfterToThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CalendarPanel(LocalDate.of(2021, 1, 1), LocalDate.of(2020, 1, 1), null, Locale.UK));
  }

  @Test
  void testSetSelectedDateNullClearsSelection() {
    CalendarPanel panel = new CalendarPanel(LocalDate.of(2026, 4, 17));
    int[] events = {0};
    panel.addListener(date -> events[0]++);

    panel.setSelectedDate(null);

    assertNull(panel.getSelectedDate());
    assertEquals(1, events[0]);
  }

  @Test
  void testSetSelectedDateSameDateDoesNotRefire() {
    LocalDate date = LocalDate.of(2026, 4, 17);
    CalendarPanel panel = new CalendarPanel(date);
    int[] events = {0};
    panel.addListener(d -> events[0]++);

    panel.setSelectedDate(date);

    assertEquals(0, events[0]);
  }

  @Test
  void testKeyboardNavigationSelectsDate() {
    CalendarPanel panel = new CalendarPanel(LocalDate.of(2026, 4, 17), Locale.UK);
    LocalDate[] received = {null};
    panel.addListener(date -> received[0] = date);

    panel.getActionMap().get("nextDay").actionPerformed(null);
    panel.getActionMap().get("nextWeek").actionPerformed(null);
    panel.getActionMap().get("nextMonth").actionPerformed(null);
    assertEquals(YearMonth.of(2026, 5), panel.getDisplayedYearMonth());
    panel.getActionMap().get("selectKeyboardDate").actionPerformed(null);

    assertEquals(LocalDate.of(2026, 5, 25), received[0]);
    assertEquals(LocalDate.of(2026, 5, 25), panel.getSelectedDate());
  }

  @Test
  void testKeyboardNavigationIsClampedToRange() {
    LocalDate to = LocalDate.of(2026, 4, 20);
    CalendarPanel panel = new CalendarPanel(LocalDate.of(2025, 1, 1), to, to, Locale.UK);

    panel.getActionMap().get("nextMonth").actionPerformed(null);
    panel.getActionMap().get("selectKeyboardDate").actionPerformed(null);

    assertEquals(to, panel.getSelectedDate());
    assertEquals(YearMonth.of(2026, 4), panel.getDisplayedYearMonth());
  }

  @Test
  void testCellsUseLookAndFeelColors() throws Exception {
    CalendarPanel panel = new CalendarPanel(LocalDate.of(2026, 4, 17), Locale.UK);

    JLabel unselected = findCell(panel, LocalDate.of(2026, 4, 16));
    JLabel selected = findCell(panel, LocalDate.of(2026, 4, 17));

    assertEquals(UIManager.getColor("List.background"), unselected.getBackground());
    assertEquals(UIManager.getColor("List.selectionBackground"), selected.getBackground());
  }
}
