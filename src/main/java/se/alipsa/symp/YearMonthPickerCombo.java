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
    this(initial.minusYears(3), initial.plusYears(3), initial);
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
   * @param from the first available month
   * @param to the last available month
   * @param initial the initially selected month
   * @param locale the locale used for rendering
   * @param format the display format for list entries
   */
  public YearMonthPickerCombo(
      YearMonth from, YearMonth to, YearMonth initial, Locale locale, String format) {
    YearMonth ym = from;
    while (!ym.isAfter(to)) {
      addItem(ym);
      ym = ym.plusMonths(1);
    }
    getModel().setSelectedItem(initial);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format, locale);

    setRenderer(
        new DefaultListCellRenderer() {
          @Override
          public Component getListCellRendererComponent(
              JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            return super.getListCellRendererComponent(
                list, formatter.format((YearMonth) value), index, isSelected, cellHasFocus);
          }
        });
  }
}
