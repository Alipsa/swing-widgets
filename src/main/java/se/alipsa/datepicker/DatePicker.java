package se.alipsa.datepicker;

import static java.awt.Image.SCALE_SMOOTH;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/** A date picker component that combines a masked text field with a popup calendar. */
public class DatePicker extends JPanel {

  private Locale locale;
  private final LocalDate rangeFrom;
  private final LocalDate rangeTo;
  private String datePattern;
  private final boolean usesDerivedPattern;

  private LocalDate lastValidDate;
  private DateVetoPolicy vetoPolicy;
  private DateHighlightPolicy highlightPolicy;
  private final List<Consumer<LocalDate>> listeners = new ArrayList<>();
  private TextFieldPosition textFieldPosition = TextFieldPosition.LEFT;

  private MaskedDateField textField;
  private JButton calendarButton;
  private JWindow popupWindow;
  private CalendarPanel calendarPanel;

  /** Creates a date picker initialized to today using the default locale and date pattern. */
  public DatePicker() {
    this(LocalDate.now());
  }

  /**
   * Creates a date picker initialized to the supplied date using the default locale.
   *
   * @param initial the initial date to show and select
   */
  public DatePicker(LocalDate initial) {
    this(initial, Locale.getDefault());
  }

  /**
   * Creates a date picker initialized to the supplied date and locale.
   *
   * @param initial the initial date to show and select
   * @param locale the locale used for formatting and calendar labels
   */
  public DatePicker(LocalDate initial, Locale locale) {
    this(
        initial != null ? initial.minusYears(20) : LocalDate.now().minusYears(20),
        initial != null ? initial.plusYears(20) : LocalDate.now().plusYears(20),
        initial != null ? initial : LocalDate.now(),
        locale,
        null,
        true);
  }

  /**
   * Creates a date picker with an explicit date range using the default locale.
   *
   * @param from the earliest allowed date
   * @param to the latest allowed date
   * @param initial the initial date to show and select
   */
  public DatePicker(LocalDate from, LocalDate to, LocalDate initial) {
    this(from, to, initial, Locale.getDefault());
  }

  /**
   * Creates a date picker with an explicit date range and locale.
   *
   * @param from the earliest allowed date
   * @param to the latest allowed date
   * @param initial the initial date to show and select
   * @param locale the locale used for formatting and calendar labels
   */
  public DatePicker(LocalDate from, LocalDate to, LocalDate initial, Locale locale) {
    this(from, to, initial, locale, null, true);
  }

  /**
   * Creates a date picker with full control over range, locale, and date pattern.
   *
   * @param from the earliest allowed date
   * @param to the latest allowed date
   * @param initial the initial date to show and select
   * @param locale the locale used for formatting and calendar labels
   * @param datePattern the date pattern used in the text field
   * @throws IllegalArgumentException if {@code from} is after {@code to}
   */
  public DatePicker(
      LocalDate from, LocalDate to, LocalDate initial, Locale locale, String datePattern) {
    this(from, to, initial, locale, datePattern, datePattern == null);
  }

  private DatePicker(
      LocalDate from,
      LocalDate to,
      LocalDate initial,
      Locale locale,
      String datePattern,
      boolean usesDerivedPattern) {
    if (from != null && to != null && from.isAfter(to)) {
      throw new IllegalArgumentException("'from' date must be before 'to' date");
    }
    this.locale = locale != null ? locale : Locale.getDefault();
    this.rangeFrom = from != null ? from : LocalDate.now().minusYears(20);
    this.rangeTo = to != null ? to : LocalDate.now().plusYears(20);
    this.datePattern = datePattern != null ? datePattern : derivePattern(this.locale);
    this.usesDerivedPattern = usesDerivedPattern;
    this.lastValidDate = initial;

    super.setLocale(this.locale);
    setLayout(new GridBagLayout());
    createComponents();
    layoutComponents();

    if (initial != null) {
      textField.setDate(initial);
    }
  }

  private static String derivePattern(Locale locale) {
    String pattern =
        DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale);
    // Normalize to fixed-width: e.g. "M/d/yy" -> "MM/dd/yyyy"
    pattern = pattern.replace("yyyy", "YYYY_PLACEHOLDER");
    pattern = pattern.replace("yy", "yyyy");
    pattern = pattern.replace("YYYY_PLACEHOLDER", "yyyy");
    pattern = pattern.replaceAll("(?<!M)M(?!M)", "MM");
    pattern = pattern.replaceAll("(?<!d)d(?!d)", "dd");
    return pattern;
  }

  private void createComponents() {
    rebuildTextField(null, isEnabled());

    ImageIcon icon = loadIcon("/calendar.png", 20, 20);
    if (icon != null) {
      calendarButton = new JButton(icon);
    } else {
      calendarButton = new JButton("\u25BC");
    }
    calendarButton.setMargin(new Insets(2, 4, 2, 4));
    calendarButton.addActionListener(e -> togglePopup());
  }

  private void rebuildTextField(LocalDate date, boolean enabled) {
    textField = new MaskedDateField(datePattern, locale);
    textField.addListener(this::onTextFieldDateChanged);
    textField.setVetoPolicy(vetoPolicy);
    if (date != null) {
      textField.setDate(date);
    }
    textField.setEnabled(enabled);
  }

  private void layoutComponents() {
    removeAll();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(0, 2, 0, 2);

    switch (textFieldPosition) {
      case LEFT:
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(textField, gbc);
        gbc.gridx = 1;
        add(calendarButton, gbc);
        break;
      case RIGHT:
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(calendarButton, gbc);
        gbc.gridx = 1;
        add(textField, gbc);
        break;
      case ABOVE:
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(textField, gbc);
        gbc.gridy = 1;
        add(calendarButton, gbc);
        break;
      case BELOW:
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(calendarButton, gbc);
        gbc.gridy = 1;
        add(textField, gbc);
        break;
    }
    revalidate();
    repaint();
  }

  private ImageIcon loadIcon(String path, int width, int height) {
    try (InputStream is = getClass().getResourceAsStream(path)) {
      if (is == null) {
        return null;
      }
      BufferedImage img = ImageIO.read(is);
      return new ImageIcon(
          new ImageIcon(img).getImage().getScaledInstance(width, height, SCALE_SMOOTH));
    } catch (IOException e) {
      System.err.println("Failed to load icon: " + path);
      return null;
    }
  }

  private void onTextFieldDateChanged(LocalDate date) {
    if (date != null && isDateAllowed(date)) {
      LocalDate previous = lastValidDate;
      lastValidDate = date;
      if (previous == null || !previous.equals(date)) {
        fireListeners(date);
      }
    }
  }

  private void onCalendarDateSelected(LocalDate date) {
    if (date != null && isDateAllowed(date)) {
      LocalDate previous = lastValidDate;
      lastValidDate = date;
      textField.setDate(date);
      closePopup();
      if (previous == null || !previous.equals(date)) {
        fireListeners(date);
      }
    }
  }

  private boolean isDateAllowed(LocalDate date) {
    if (date.isBefore(rangeFrom) || date.isAfter(rangeTo)) {
      return false;
    }
    return vetoPolicy == null || vetoPolicy.isDateAllowed(date);
  }

  private void fireListeners(LocalDate date) {
    for (Consumer<LocalDate> listener : new ArrayList<>(listeners)) {
      try {
        listener.accept(date);
      } catch (RuntimeException e) {
        System.err.println("DatePicker listener threw exception: " + e.getMessage());
      }
    }
  }

  /**
   * Returns the last valid date selected or entered.
   *
   * @return the current date, or {@code null} if the picker is empty
   */
  public LocalDate getDate() {
    return lastValidDate;
  }

  /**
   * Sets the current date when it is within range and not vetoed.
   *
   * @param date the date to select, or {@code null} to clear the value
   */
  public void setDate(LocalDate date) {
    if (date != null && !isDateAllowed(date)) {
      return;
    }
    LocalDate previous = lastValidDate;
    lastValidDate = date;
    if (date != null) {
      textField.setDate(date);
    } else {
      textField.setDate(null);
    }
    if (previous == null ? date != null : !previous.equals(date)) {
      fireListeners(date);
    }
  }

  /** Clears the current date and notifies listeners. */
  public void clear() {
    lastValidDate = null;
    textField.setDate(null);
    fireListeners(null);
  }

  /**
   * Registers a listener that is notified when the selected date changes.
   *
   * @param listener the listener to add
   */
  public void addListener(Consumer<LocalDate> listener) {
    listeners.add(listener);
  }

  /**
   * Removes a previously registered date listener.
   *
   * @param listener the listener to remove
   */
  public void removeListener(Consumer<LocalDate> listener) {
    listeners.remove(listener);
  }

  /** Opens the popup calendar if it is not already visible. */
  public void openPopup() {
    if (popupWindow != null && popupWindow.isVisible()) {
      return;
    }
    if (!textField.isShowing()) {
      return;
    }

    calendarPanel =
        new CalendarPanel(
            rangeFrom, rangeTo, lastValidDate != null ? lastValidDate : LocalDate.now(), locale);
    if (vetoPolicy != null) {
      calendarPanel.setVetoPolicy(vetoPolicy);
    }
    if (highlightPolicy != null) {
      calendarPanel.setHighlightPolicy(highlightPolicy);
    }
    calendarPanel.addListener(this::onCalendarDateSelected);

    Window parentWindow = SwingUtilities.getWindowAncestor(this);
    popupWindow = new JWindow(parentWindow);
    popupWindow.setFocusableWindowState(true);
    popupWindow.getContentPane().add(calendarPanel);
    popupWindow.pack();

    Point loc = textField.getLocationOnScreen();
    int x = loc.x;
    int y = loc.y + textField.getHeight();

    Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
    if (x + popupWindow.getWidth() > screenSize.width) {
      x = screenSize.width - popupWindow.getWidth();
    }
    if (y + popupWindow.getHeight() > screenSize.height) {
      y = loc.y - popupWindow.getHeight();
    }

    popupWindow.setLocation(x, y);
    popupWindow.setVisible(true);

    popupWindow.addWindowFocusListener(
        new WindowFocusListener() {
          @Override
          public void windowGainedFocus(WindowEvent e) {}

          @Override
          public void windowLostFocus(WindowEvent e) {
            SwingUtilities.invokeLater(() -> closePopup());
          }
        });
  }

  /** Closes the popup calendar if it is open. */
  public void closePopup() {
    if (popupWindow != null) {
      popupWindow.setVisible(false);
      popupWindow.dispose();
      popupWindow = null;
      calendarPanel = null;
    }
  }

  /** Toggles the visibility of the popup calendar. */
  public void togglePopup() {
    if (isPopupOpen()) {
      closePopup();
    } else {
      openPopup();
    }
  }

  /**
   * Indicates whether the popup calendar is currently visible.
   *
   * @return {@code true} if the popup is open, otherwise {@code false}
   */
  public boolean isPopupOpen() {
    return popupWindow != null && popupWindow.isVisible();
  }

  /**
   * Applies a veto policy used to reject disallowed dates.
   *
   * @param policy the veto policy to apply, or {@code null} to allow all dates
   */
  public void setVetoPolicy(DateVetoPolicy policy) {
    this.vetoPolicy = policy;
    textField.setVetoPolicy(policy);
    if (calendarPanel != null) {
      calendarPanel.setVetoPolicy(policy);
    }
    if (lastValidDate != null && !isDateAllowed(lastValidDate)) {
      lastValidDate = null;
      textField.setDate(null);
      if (calendarPanel != null) {
        closePopup();
      }
      fireListeners(null);
    }
  }

  /**
   * Applies a highlight policy to the popup calendar.
   *
   * @param policy the highlight policy to apply, or {@code null} to remove highlighting
   */
  public void setHighlightPolicy(DateHighlightPolicy policy) {
    this.highlightPolicy = policy;
    if (calendarPanel != null) {
      calendarPanel.setHighlightPolicy(policy);
    }
  }

  /**
   * Returns the locale used by this picker.
   *
   * @return the picker locale
   */
  @Override
  public Locale getLocale() {
    return locale;
  }

  /**
   * Updates the picker locale without recreating the component.
   *
   * @param newLocale the new locale to apply; {@code null} is ignored
   */
  @Override
  public void setLocale(Locale newLocale) {
    if (newLocale == null || newLocale.equals(locale)) {
      return;
    }

    LocalDate date = getDate();
    boolean enabled = isEnabled();

    closePopup();
    super.setLocale(newLocale);
    this.locale = newLocale;
    if (usesDerivedPattern) {
      this.datePattern = derivePattern(newLocale);
    }
    rebuildTextField(date, enabled);
    layoutComponents();
  }

  /**
   * Returns the first selectable date.
   *
   * @return the start of the allowed range
   */
  public LocalDate getStart() {
    return rangeFrom;
  }

  /**
   * Returns the last selectable date.
   *
   * @return the end of the allowed range
   */
  public LocalDate getEnd() {
    return rangeTo;
  }

  /**
   * Returns the date pattern used by the text field.
   *
   * @return the date pattern
   */
  public String getDatePattern() {
    return datePattern;
  }

  /**
   * Enables or disables the picker and closes the popup when disabling.
   *
   * @param enabled {@code true} to enable the picker, otherwise {@code false}
   */
  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    textField.setEnabled(enabled);
    calendarButton.setEnabled(enabled);
    if (!enabled) {
      closePopup();
    }
  }

  /**
   * Returns the current layout position of the text field.
   *
   * @return the text field position
   */
  public TextFieldPosition getTextFieldPosition() {
    return textFieldPosition;
  }

  /**
   * Updates the layout position of the text field relative to the button.
   *
   * @param position the desired text field position
   */
  public void setTextFieldPosition(TextFieldPosition position) {
    this.textFieldPosition = position;
    layoutComponents();
  }

  /**
   * Returns the masked text field used by the picker.
   *
   * <p>The returned instance is replaced when {@link #setLocale(Locale)} rebuilds the field, so
   * callers must not cache it across locale changes.
   *
   * @return the current text field component
   */
  public MaskedDateField getTextField() {
    return textField;
  }

  /**
   * Returns the button used to open the popup calendar.
   *
   * @return the calendar button
   */
  public JButton getCalendarButton() {
    return calendarButton;
  }
}
