package se.alipsa.datepicker;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

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
        DatePicker picker = new DatePicker(null, null, LocalDate.now(), Locale.getDefault(), "dd/MM/yyyy");
        assertEquals("dd/MM/yyyy", picker.getDatePattern());
    }

    @Test
    void testVetoPolicyPreventsSelection() {
        DatePicker picker = new DatePicker();
        picker.setVetoPolicy(date -> date.getDayOfWeek().getValue() != 7);
        LocalDate sunday = LocalDate.of(2026, 4, 19);
        picker.setDate(sunday);
        assertNotEquals(sunday, picker.getDate());
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
}
