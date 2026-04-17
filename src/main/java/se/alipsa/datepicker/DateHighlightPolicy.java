package se.alipsa.datepicker;

import java.time.LocalDate;

/** Provides optional visual decoration metadata for dates shown in a calendar. */
@FunctionalInterface
public interface DateHighlightPolicy {

  /**
   * Returns highlight information for the supplied date.
   *
   * @param date the date being rendered
   * @return highlight information for the date, or {@code null} for normal rendering
   */
  HighlightInfo getHighlightInfo(LocalDate date);
}
