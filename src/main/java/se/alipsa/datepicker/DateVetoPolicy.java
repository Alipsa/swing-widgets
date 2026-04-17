package se.alipsa.datepicker;

import java.time.LocalDate;

/** Determines whether a date may be selected or entered. */
@FunctionalInterface
public interface DateVetoPolicy {

  /**
   * Tests whether the supplied date is allowed.
   *
   * @param date the date to validate
   * @return {@code true} if the date is allowed, otherwise {@code false}
   */
  boolean isDateAllowed(LocalDate date);
}
