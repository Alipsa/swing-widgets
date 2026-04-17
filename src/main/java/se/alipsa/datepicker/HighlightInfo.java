package se.alipsa.datepicker;

import java.awt.Color;
import java.util.Objects;

/** Holds optional highlight styling for a calendar cell. */
public final class HighlightInfo {

  private final Color backgroundColor;
  private final String tooltip;

  /**
   * Creates a new highlight descriptor.
   *
   * @param backgroundColor the background color to apply, or {@code null} to keep the default color
   * @param tooltip the tooltip to show for the date, or {@code null} for no tooltip
   */
  public HighlightInfo(Color backgroundColor, String tooltip) {
    this.backgroundColor = backgroundColor;
    this.tooltip = tooltip;
  }

  /**
   * Returns the background color for the date.
   *
   * @return the background color, or {@code null} if no override is defined
   */
  public Color getBackgroundColor() {
    return backgroundColor;
  }

  /**
   * Returns the tooltip for the date.
   *
   * @return the tooltip text, or {@code null} if no tooltip is defined
   */
  public String getTooltip() {
    return tooltip;
  }

  /**
   * Compares this highlight info with another object.
   *
   * @param o the object to compare with
   * @return {@code true} when both objects describe the same highlight information
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HighlightInfo)) return false;
    HighlightInfo that = (HighlightInfo) o;
    return Objects.equals(backgroundColor, that.backgroundColor)
        && Objects.equals(tooltip, that.tooltip);
  }

  /**
   * Returns a hash code for this highlight info.
   *
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return Objects.hash(backgroundColor, tooltip);
  }

  /**
   * Returns a string representation of this highlight info.
   *
   * @return a string representation
   */
  @Override
  public String toString() {
    return "HighlightInfo{backgroundColor=" + backgroundColor + ", tooltip='" + tooltip + "'}";
  }
}
