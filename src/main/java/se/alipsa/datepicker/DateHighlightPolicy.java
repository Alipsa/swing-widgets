package se.alipsa.datepicker;

import java.time.LocalDate;

@FunctionalInterface
public interface DateHighlightPolicy {
    HighlightInfo getHighlightInfo(LocalDate date);
}
