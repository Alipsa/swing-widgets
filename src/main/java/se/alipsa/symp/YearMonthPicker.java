package se.alipsa.symp;

import static java.awt.Image.SCALE_SMOOTH;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/** A year-month picker that combines a label with a popup month selector. */
public class YearMonthPicker extends JPanel {

  private static final String HIDE_POPUP_ACTION = "hideYearMonthPopup";

  private Locale locale;
  private YearMonth startYearMonth;
  private YearMonth endYearMonth;
  private YearMonth initial;
  private JLabel inputField;
  private String monthPattern;
  private String yearMonthPattern;
  private Popup popup;
  private AWTEventListener outsideClickListener;
  private DateTimeFormatter yearMonthFormatter;
  private DateTimeFormatter monthFormatter;
  private HoverListCellRenderer listRenderer;
  private YearMonth selectedItem;
  private JButton pickerButton;
  private final List<ListSelectionListener> listeners = new ArrayList<>();

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
    this(
        initial != null ? initial.minusYears(3) : YearMonth.now().minusYears(3),
        initial != null ? initial.plusYears(3) : YearMonth.now().plusYears(3),
        initial,
        locale,
        "MMMM");
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
   * @param from the first available month, or {@code null} for three years before {@code initial}
   *     (or now)
   * @param to the last available month, or {@code null} for three years after {@code initial} (or
   *     now)
   * @param initial the initially selected month, or {@code null} for no selection; a month outside
   *     the range is not selected
   * @param locale the locale used for rendering, or {@code null} for the default locale
   * @param monthPattern the display pattern used in the popup, or {@code null} for {@code MMMM}
   * @param yearMonthPattern the display pattern used in the input area, or {@code null} for {@code
   *     yyyy-MM}
   * @throws IllegalArgumentException if {@code from} is after {@code to}
   */
  public YearMonthPicker(
      YearMonth from,
      YearMonth to,
      YearMonth initial,
      Locale locale,
      String monthPattern,
      String yearMonthPattern) {
    YearMonth base = initial != null ? initial : YearMonth.now();
    YearMonth start = from != null ? from : base.minusYears(3);
    YearMonth end = to != null ? to : base.plusYears(3);
    if (start.isAfter(end)) {
      throw new IllegalArgumentException("'from' month must not be after 'to' month");
    }
    setStart(start);
    setEnd(end);
    setInitial(initial);
    this.locale = locale != null ? locale : Locale.getDefault();
    super.setLocale(this.locale);
    setMonthPattern(monthPattern != null ? monthPattern : "MMMM");
    this.yearMonthPattern = yearMonthPattern != null ? yearMonthPattern : "yyyy-MM";
    updateFormatters();
    createLayout();
    installPopupCloseTriggers();
  }

  private void updateFormatters() {
    yearMonthFormatter = DateTimeFormatter.ofPattern(yearMonthPattern, locale);
    monthFormatter = DateTimeFormatter.ofPattern(monthPattern, locale);
    if (listRenderer != null) {
      listRenderer.setFormatter(monthFormatter);
      listView.repaint();
    }
    updateInputField();
  }

  private void updateInputField() {
    if (inputField != null) {
      inputField.setText(selectedItem == null ? "" : yearMonthFormatter.format(selectedItem));
    }
  }

  private void createLayout() {
    setLayout(new FlowLayout());

    setSelectedItem(initial);
    inputField = new JLabel();
    updateInputField();

    add(inputField);

    ImageIcon icon = icon("/calendar.png", 30, 30);
    if (icon != null) {
      pickerButton = new JButton(icon);
    } else {
      pickerButton = new JButton();
    }
    inputField.setLabelFor(pickerButton);
    pickerButton.setMargin(new Insets(0, 1, 0, 1));
    pickerButton.setIconTextGap(0);
    updatePickerButtonSize();
    pickerButton.addActionListener(a -> showHideSelectBox());
    add(pickerButton);

    initListView();
  }

  private void initListView() {
    listRenderer = HoverListCellRenderer.register(listView);
    listRenderer.setFormatter(monthFormatter);
    listView.addListSelectionListener(
        l -> {
          // Ignore the intermediate events of a mouse press and the event fired when the model
          // is replaced (which clears the selection).
          if (l.getValueIsAdjusting()) {
            return;
          }
          YearMonth newYearMonth = listView.getSelectedValue();
          if (newYearMonth == null || !isMonthAllowed(newYearMonth)) {
            return;
          }
          YearMonth previous = selectedItem;
          setSelectedItem(newYearMonth);
          hidePopup();
          if (!newYearMonth.equals(previous)) {
            fireListeners(
                new ListSelectionEvent(listView, l.getFirstIndex(), l.getLastIndex(), false));
          }
        });
  }

  private void fireListeners(ListSelectionEvent event) {
    for (ListSelectionListener listener : new ArrayList<>(listeners)) {
      try {
        listener.valueChanged(event);
      } catch (RuntimeException e) {
        Thread thread = Thread.currentThread();
        thread.getUncaughtExceptionHandler().uncaughtException(thread, e);
      }
    }
  }

  private void installPopupCloseTriggers() {
    getInputMap(WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), HIDE_POPUP_ACTION);
    getActionMap()
        .put(
            HIDE_POPUP_ACTION,
            new AbstractAction() {
              @Override
              public boolean isEnabled() {
                // Disabled while closed so Escape still reaches other components.
                return popup != null;
              }

              @Override
              public void actionPerformed(ActionEvent e) {
                hidePopup();
              }
            });
    addHierarchyListener(
        e -> {
          if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !isShowing()) {
            hidePopup();
          }
        });
    addHierarchyBoundsListener(
        new HierarchyBoundsAdapter() {
          @Override
          public void ancestorMoved(HierarchyEvent e) {
            hidePopup();
          }

          @Override
          public void ancestorResized(HierarchyEvent e) {
            hidePopup();
          }
        });
  }

  private void hidePopup() {
    if (popup == null) {
      return;
    }
    popup.hide();
    popup = null;
    if (outsideClickListener != null) {
      Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
      outsideClickListener = null;
    }
  }

  /** Closes the popup when the mouse is pressed anywhere outside it or the picker button. */
  private void listenForOutsideClicks(JComponent selectBox) {
    outsideClickListener =
        event -> {
          if (event.getID() != MouseEvent.MOUSE_PRESSED) {
            return;
          }
          Object source = event.getSource();
          if (source instanceof Component
              && (SwingUtilities.isDescendingFrom((Component) source, selectBox)
                  || SwingUtilities.isDescendingFrom((Component) source, pickerButton))) {
            return;
          }
          hidePopup();
        };
    try {
      Toolkit.getDefaultToolkit()
          .addAWTEventListener(outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
    } catch (SecurityException e) {
      // Not permitted under a restrictive security manager; the other close triggers still work.
      outsideClickListener = null;
    }
  }

  /** Returns the months of {@code year} that are inside the configured range. */
  List<YearMonth> monthsInRange(int year) {
    List<YearMonth> months = new ArrayList<>();
    for (int i = 1; i <= 12; i++) {
      YearMonth val = YearMonth.of(year, i);
      if (isMonthAllowed(val)) {
        months.add(val);
      }
    }
    return months;
  }

  /** The month whose year the popup opens on: the selection, or now clamped to the range. */
  YearMonth popupBaseMonth() {
    if (selectedItem != null) {
      return selectedItem;
    }
    YearMonth now = YearMonth.now();
    if (now.isBefore(startYearMonth)) {
      return startYearMonth;
    }
    return now.isAfter(endYearMonth) ? endYearMonth : now;
  }

  static Point popupLocation(
      Point preferredLocation, int anchorTop, Dimension popupSize, Rectangle usableBounds) {
    int x =
        Math.max(
            usableBounds.x,
            Math.min(preferredLocation.x, usableBounds.x + usableBounds.width - popupSize.width));
    int y = preferredLocation.y;
    if (y + popupSize.height > usableBounds.y + usableBounds.height) {
      y = anchorTop - popupSize.height;
    }
    y =
        Math.max(
            usableBounds.y, Math.min(y, usableBounds.y + usableBounds.height - popupSize.height));
    return new Point(x, y);
  }

  private void updatePickerButtonSize() {
    Dimension inputSize = inputField.getPreferredSize();
    int side = Math.max(inputSize.height, 34);
    Dimension buttonSize = new Dimension(side, side);
    pickerButton.setPreferredSize(buttonSize);
    pickerButton.setMinimumSize(buttonSize);
  }

  private ImageIcon icon(String path, int width, int height) {
    try (InputStream is = getClass().getResourceAsStream(path)) {
      if (is == null) {
        return null;
      }
      BufferedImage buttonIcon = ImageIO.read(is);
      return new ImageIcon(
          new ImageIcon(buttonIcon).getImage().getScaledInstance(width, height, SCALE_SMOOTH));
    } catch (IOException e) {
      return null;
    }
  }

  private void showHideSelectBox() {

    if (popup != null) {
      hidePopup();
      return;
    }

    JPanel selectBox = new JPanel(new BorderLayout());
    int[] shownYear = {popupBaseMonth().getYear()};
    JLabel yearLabel = new JLabel(String.valueOf(shownYear[0]));

    DefaultListModel<YearMonth> listModel = new DefaultListModel<>();
    monthsInRange(shownYear[0]).forEach(listModel::addElement);
    listView.setModel(listModel);

    JPanel top = new JPanel(new FlowLayout());
    selectBox.add(top, BorderLayout.NORTH);

    JButton yearBackButton = new JButton("<");
    yearBackButton.addActionListener(
        e -> {
          if (shownYear[0] - 1 < startYearMonth.getYear()) {
            return;
          }
          shownYear[0]--;
          yearLabel.setText(String.valueOf(shownYear[0]));
          listModel.removeAllElements();
          monthsInRange(shownYear[0]).forEach(listModel::addElement);
        });
    JButton yearForwardButton = new JButton(">");
    yearForwardButton.addActionListener(
        e -> {
          if (shownYear[0] + 1 > endYearMonth.getYear()) {
            return;
          }
          shownYear[0]++;
          yearLabel.setText(String.valueOf(shownYear[0]));
          listModel.removeAllElements();
          monthsInRange(shownYear[0]).forEach(listModel::addElement);
        });
    top.add(yearBackButton);
    top.add(yearLabel);
    top.add(yearForwardButton);

    selectBox.add(listView, BorderLayout.CENTER);

    // Open below the picker button, kept inside the usable bounds of the monitor showing it.
    Point buttonLocation = pickerButton.getLocationOnScreen();
    GraphicsConfiguration graphicsConfiguration = pickerButton.getGraphicsConfiguration();
    Rectangle bounds = graphicsConfiguration.getBounds();
    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);
    Rectangle usableBounds =
        new Rectangle(
            bounds.x + insets.left,
            bounds.y + insets.top,
            bounds.width - insets.left - insets.right,
            bounds.height - insets.top - insets.bottom);
    Point location =
        popupLocation(
            new Point(buttonLocation.x, buttonLocation.y + pickerButton.getHeight()),
            buttonLocation.y,
            selectBox.getPreferredSize(),
            usableBounds);
    PopupFactory factory = PopupFactory.getSharedInstance();
    popup = factory.getPopup(this, selectBox, location.x, location.y);
    popup.show();
    listenForOutsideClicks(selectBox);
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
   * Sets the locale used by this picker and re-renders the displayed value and popup months.
   *
   * @param locale the locale to use; {@code null} is ignored
   */
  @Override
  public void setLocale(Locale locale) {
    if (locale == null || locale.equals(this.locale)) {
      return;
    }
    hidePopup();
    super.setLocale(locale);
    this.locale = locale;
    updateFormatters();
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
   * Updates the currently selected month and the displayed value. Months outside the configured
   * range are ignored. Listeners are not notified of programmatic changes.
   *
   * @param selectedItem the month to select, or {@code null} to clear the selection
   */
  public void setSelectedItem(YearMonth selectedItem) {
    if (selectedItem != null && !isMonthAllowed(selectedItem)) {
      return;
    }
    this.selectedItem = selectedItem;
    updateInputField();
  }

  private boolean isMonthAllowed(YearMonth yearMonth) {
    return !yearMonth.isBefore(startYearMonth) && !yearMonth.isAfter(endYearMonth);
  }

  /**
   * Registers a listener that is notified when the user picks a different month in the popup.
   *
   * @param listener the listener to add
   */
  public void addListener(ListSelectionListener listener) {
    listeners.add(listener);
  }
}
