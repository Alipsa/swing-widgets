package se.alipsa.datepicker;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

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
    void testIsValidWithValidDate() {
        MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
        field.setDate(LocalDate.of(2026, 6, 15));
        assertTrue(field.isValid());
    }

    @Test
    void testIsValidWhenEmpty() {
        MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
        assertFalse(field.isValid());
    }

    @Test
    void testVetoPolicyMarksDateInvalid() {
        MaskedDateField field = new MaskedDateField("yyyy-MM-dd", Locale.getDefault());
        field.setVetoPolicy(date -> !date.getDayOfWeek().name().equals("SUNDAY"));
        LocalDate sunday = LocalDate.of(2026, 4, 19); // a Sunday
        field.setDate(sunday);
        assertFalse(field.isValid());
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
}
