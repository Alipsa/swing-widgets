package se.alipsa.symp;

import static java.awt.Image.SCALE_SMOOTH;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.event.ListSelectionListener;

/** A year-month picker that combines a label with a popup month selector. */
public class YearMonthPicker extends JPanel {

  private Locale locale;
  private YearMonth startYearMonth;
  private YearMonth endYearMonth;
  private YearMonth initial;
  private JLabel inputField;
  private String monthPattern;
  private Popup popup;
  private final DateTimeFormatter yearMonthFormatter;
  private YearMonth selectedItem;
  private JButton pickerButton;

  JList<YearMonth> listView = new JList<>();

  /**
   * Demo entry point for the picker component.
   *
   * @param args ignored command-line arguments
   */
  public static void main(String[] args) {
    JFrame frame = new JFrame("Year month picker");
    JPanel panel = new JPanel();
    frame.add(panel);
    YearMonthPicker ymp = new YearMonthPicker();
    ymp.addListener(e -> System.out.println(ymp.getValue()));
    panel.add(new JLabel("Default Picker: "));
    panel.add(ymp);
    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }

  /** Creates a picker centered around the current month with a three-year range on each side. */
  public YearMonthPicker() {
    this(YearMonth.now());
  }

  /**
   * Creates a picker centered around the supplied month with a three-year range on each side.
   *
   * @param initial the initially selected month
   */
  public YearMonthPicker(YearMonth initial) {
    this(initial, Locale.getDefault());
  }

  /**
   * Creates a picker centered around the supplied month using the specified locale.
   *
   * @param initial the initially selected month
   * @param locale the locale used for rendering
   */
  public YearMonthPicker(YearMonth initial, Locale locale) {
    this(initial.minusYears(3), initial.plusYears(3), initial, locale, "MMMM");
  }

  /**
   * Creates a picker with an explicit range using the default locale and display pattern.
   *
   * @param from the first available month
   * @param to the last available month
   * @param initial the initially selected month
   */
  public YearMonthPicker(YearMonth from, YearMonth to, YearMonth initial) {
    this(from, to, initial, "yyyy-MMM");
  }

  /**
   * Creates a picker with an explicit range and popup month pattern.
   *
   * @param from the first available month
   * @param to the last available month
   * @param initial the initially selected month
   * @param monthPattern the display pattern used in the popup
   */
  public YearMonthPicker(YearMonth from, YearMonth to, YearMonth initial, String monthPattern) {
    this(from, to, initial, Locale.getDefault(), monthPattern);
  }

  /**
   * Creates a picker with an explicit range, locale, and popup month pattern.
   *
   * @param from the first available month
   * @param to the last available month
   * @param initial the initially selected month
   * @param locale the locale used for rendering
   * @param monthPattern the display pattern used in the popup
   */
  public YearMonthPicker(
      YearMonth from, YearMonth to, YearMonth initial, Locale locale, String monthPattern) {
    this(from, to, initial, locale, monthPattern, "yyyy-MM");
  }

  /**
   * Creates a picker with full control over range and formatting.
   *
   * @param from the first available month
   * @param to the last available month
   * @param initial the initially selected month
   * @param locale the locale used for rendering
   * @param monthPattern the display pattern used in the popup
   * @param yearMonthPattern the display pattern used in the input area
   */
  public YearMonthPicker(
      YearMonth from,
      YearMonth to,
      YearMonth initial,
      Locale locale,
      String monthPattern,
      String yearMonthPattern) {
    setStart(from);
    setEnd(to);
    setInitial(initial);
    setLocale(locale);
    setMonthPattern(monthPattern);
    yearMonthFormatter = DateTimeFormatter.ofPattern(yearMonthPattern, locale);
    createLayout();
  }

  private void createLayout() {
    setLayout(new FlowLayout());

    setSelectedItem(initial);
    String ymVal = initial == null ? "" : yearMonthFormatter.format(initial);
    inputField = new JLabel(ymVal);

    add(inputField);

    ImageIcon icon = icon("/calendar.png", 20, 20);
    if (icon != null) {
      pickerButton = new JButton(icon);
    } else {
      pickerButton = new JButton();
    }
    inputField.setLabelFor(pickerButton);
    pickerButton.addActionListener(a -> showHideSelectBox());
    add(pickerButton);
  }

  private ImageIcon icon(String path, int width, int height) {
    try {
      InputStream is = getClass().getResourceAsStream(path);
      BufferedImage buttonIcon = ImageIO.read(is);
      return new ImageIcon(
          new ImageIcon(buttonIcon).getImage().getScaledInstance(width, height, SCALE_SMOOTH));
    } catch (IOException e) {
      return null;
    }
  }

  private void showHideSelectBox() {

    if (popup != null) {
      popup.hide();
      popup = null;
      return;
    }

    JPanel selectBox = new JPanel(new BorderLayout());
    JLabel yearLabel = new JLabel(String.valueOf(getSelectedItem().getYear()));

    DefaultListModel<YearMonth> listModel = new DefaultListModel<>();
    for (int i = 1; i <= 12; i++) {
      listModel.addElement(YearMonth.of(Integer.parseInt(yearLabel.getText()), i));
    }
    listView.setModel(listModel);
    HoverListCellRenderer.register(listView);

    JPanel top = new JPanel(new FlowLayout());
    selectBox.add(top, BorderLayout.NORTH);

    JButton yearBackButton = new JButton("<");
    yearBackButton.addActionListener(
        e -> {
          int yearNum = Year.parse(yearLabel.getText()).minusYears(1).getValue();
          if (yearNum < startYearMonth.getYear()) {
            return;
          }
          yearLabel.setText(String.valueOf(yearNum));
          listModel.removeAllElements();
          for (int i = 1; i <= 12; i++) {
            YearMonth val = YearMonth.of(yearNum, i);
            if (!val.isBefore(startYearMonth)) {
              listModel.addElement(YearMonth.of(yearNum, i));
            }
          }
        });
    JButton yearForwardButton = new JButton(">");
    yearForwardButton.addActionListener(
        e -> {
          int yearNum = Year.parse(yearLabel.getText()).plusYears(1).getValue();
          if (yearNum > endYearMonth.getYear()) {
            return;
          }
          yearLabel.setText(String.valueOf(yearNum));
          listModel.removeAllElements();
          for (int i = 1; i <= 12; i++) {
            YearMonth val = YearMonth.of(yearNum, i);
            if (!val.isAfter(endYearMonth)) {
              listModel.addElement(val);
            }
          }
        });
    top.add(yearBackButton);
    top.add(yearLabel);
    top.add(yearForwardButton);

    selectBox.add(listView, BorderLayout.CENTER);

    listView.addListSelectionListener(
        l -> {
          YearMonth newYearMonth = listView.getSelectedValue();
          if (newYearMonth == null) {
            return;
          }
          inputField.setText(yearMonthFormatter.format(newYearMonth));
          setSelectedItem(newYearMonth);
          if (popup != null) {
            popup.hide();
            popup = null;
          }
        });

    int layoutX = (int) (getLocationOnScreen().getX() + pickerButton.getBounds().getX());
    int layoutY = (int) (getLocationOnScreen().getY() + pickerButton.getBounds().getY());
    PopupFactory factory = PopupFactory.getSharedInstance();
    popup = factory.getPopup(this, selectBox, layoutX, layoutY);
    popup.show();
  }

  /**
   * Returns the locale used by this picker.
   *
   * @return the picker locale
   */
  public Locale getLocale() {
    return locale;
  }

  /**
   * Sets the locale used by this picker.
   *
   * @param locale the locale to use
   */
  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  /**
   * Returns the first selectable month.
   *
   * @return the start of the allowed range
   */
  public YearMonth getStart() {
    return startYearMonth;
  }

  private void setStart(YearMonth start) {
    this.startYearMonth = start;
  }

  /**
   * Returns the last selectable month.
   *
   * @return the end of the allowed range
   */
  public YearMonth getEnd() {
    return endYearMonth;
  }

  private void setEnd(YearMonth end) {
    this.endYearMonth = end;
  }

  /**
   * Returns the initial month configured for the picker.
   *
   * @return the initial month
   */
  public YearMonth getInitial() {
    return initial;
  }

  private void setInitial(YearMonth initial) {
    this.initial = initial;
  }

  /**
   * Returns the popup month display pattern.
   *
   * @return the month display pattern
   */
  public String getMonthPattern() {
    return monthPattern;
  }

  private void setMonthPattern(String monthPattern) {
    this.monthPattern = monthPattern;
  }

  /**
   * Returns the currently selected month.
   *
   * @return the selected month
   */
  public YearMonth getSelectedItem() {
    return selectedItem;
  }

  /**
   * Returns the currently selected month.
   *
   * @return the selected month
   */
  public YearMonth getValue() {
    return getSelectedItem();
  }

  /**
   * Updates the currently selected month.
   *
   * @param selectedItem the month to select
   */
  public void setSelectedItem(YearMonth selectedItem) {
    this.selectedItem = selectedItem;
  }

  /**
   * Registers a listener that is notified when the popup selection changes.
   *
   * @param listener the listener to add
   */
  public void addListener(ListSelectionListener listener) {
    listView.addListSelectionListener(listener);
  }
}
