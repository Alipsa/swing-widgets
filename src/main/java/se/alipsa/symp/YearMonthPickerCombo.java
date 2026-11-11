package se.alipsa.symp;

import java.awt.Component;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

/** A combo box for selecting a {@link YearMonth} from a configured range. */
public class YearMonthPickerCombo extends JComboBox<YearMonth> {

  /**
   * Demo entry point for the combo box component.
   *
   * @param args ignored command-line arguments
   */
  public static void main(String[] args) {
    JFrame frame = new JFrame("Year month combo");
    JPanel panel = new JPanel();
    frame.add(panel);
    YearMonthPickerCombo ympc = new YearMonthPickerCombo();
    panel.add(new JLabel("Default Combo: "));
    panel.add(ympc);
    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }

  /** Creates a combo box centered around the current month with a three-year range on each side. */
  public YearMonthPickerCombo() {
    this(YearMonth.now());
  }

  /**
   * Creates a combo box centered around the supplied month with a three-year range on each side.
   *
   * @param initial the initially selected month
   */
  public YearMonthPickerCombo(YearMonth initial) {
    this(
        initial != null ? initial.minusYears(3) : YearMonth.now().minusYears(3),
        initial != null ? initial.plusYears(3) : YearMonth.now().plusYears(3),
        initial);
  }

  /**
   * Creates a combo box with an explicit range using the default locale and format.
   *
   * @param from the first available month
   * @param to the last available month
   * @param initial the initially selected month
   */
  public YearMonthPickerCombo(YearMonth from, YearMonth to, YearMonth initial) {
    this(from, to, initial, Locale.getDefault());
  }

  /**
   * Creates a combo box with an explicit range and locale.
   *
   * @param from the first available month
   * @param to the last available month
   * @param initial the initially selected month
   * @param locale the locale used for rendering
   */
  public YearMonthPickerCombo(YearMonth from, YearMonth to, YearMonth initial, Locale locale) {
    this(from, to, initial, locale, "yyyy-MM");
  }

  /**
   * Creates a combo box with an explicit range, locale, and display format.
   *
   * @param from the first available month, or {@code null} for three years before {@code initial}
   *     (or now)
   * @param to the last available month, or {@code null} for three years after {@code initial} (or
   *     now)
   * @param initial the initially selected month, or {@code null} for no selection; a month outside
   *     the range is not selected
   * @param locale the locale used for rendering, or {@code null} for the default locale
   * @param format the display format for list entries, or {@code null} for {@code yyyy-MM}
   * @throws IllegalArgumentException if {@code from} is after {@code to}
   */
  public YearMonthPickerCombo(
      YearMonth from, YearMonth to, YearMonth initial, Locale locale, String format) {
    YearMonth base = initial != null ? initial : YearMonth.now();
    YearMonth start = from != null ? from : base.minusYears(3);
    YearMonth end = to != null ? to : base.plusYears(3);
    if (start.isAfter(end)) {
      throw new IllegalArgumentException("'from' month must not be after 'to' month");
    }
    YearMonth ym = start;
    while (!ym.isAfter(end)) {
      addItem(ym);
      ym = ym.plusMonths(1);
    }
    boolean initialInRange = initial != null && !initial.isBefore(start) && !initial.isAfter(end);
    getModel().setSelectedItem(initialInRange ? initial : null);
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern(
            format != null ? format : "yyyy-MM", locale != null ? locale : Locale.getDefault());

    setRenderer(
        new DefaultListCellRenderer() {
          @Override
          public Component getListCellRendererComponent(
              JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            // value is null when nothing is selected
            Object display = value instanceof YearMonth ? formatter.format((YearMonth) value) : "";
            return super.getListCellRendererComponent(
                list, display, index, isSelected, cellHasFocus);
          }
        });
  }
}
