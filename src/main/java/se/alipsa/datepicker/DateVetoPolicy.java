package se.alipsa.datepicker;

import java.time.LocalDate;

@FunctionalInterface
public interface DateVetoPolicy {
    boolean isDateAllowed(LocalDate date);
}
