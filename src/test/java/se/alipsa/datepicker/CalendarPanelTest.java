package se.alipsa.datepicker;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

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
    void testRangeDefaults() {
        LocalDate initial = LocalDate.of(2026, 6, 15);
        CalendarPanel panel = new CalendarPanel(initial);
        LocalDate expectedFrom = initial.minusYears(20);
        panel.setDisplayedYearMonth(YearMonth.from(expectedFrom));
        panel.previousMonth();
        assertEquals(YearMonth.from(expectedFrom), panel.getDisplayedYearMonth());
    }
}
