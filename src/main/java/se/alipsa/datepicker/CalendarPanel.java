package se.alipsa.datepicker;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

/** A standalone month calendar component with optional veto and highlight policies. */
public class CalendarPanel extends JPanel {

  // Fallbacks used when the look and feel does not define the corresponding UIManager color.
  private static final Color SELECTED_BG = new Color(163, 184, 204);
  private static final Color SELECTED_BORDER = new Color(99, 130, 191);
  private static final Color OTHER_MONTH_FG = Color.LIGHT_GRAY;
  private static final Color VETOED_BG = new Color(240, 240, 240);
  private static final Color NORMAL_BG = Color.WHITE;
  private static final Color NORMAL_FG = Color.BLACK;
  private static final int CELL_SIZE = 36;
  private static final int ROWS = 6;
  private static final int COLS = 7;
  private static final float HEADER_FONT_DELTA = -1.0f;
  private static final float CELL_FONT_DELTA = -1.0f;

  private final Locale locale;
  private final LocalDate rangeFrom;
  private final LocalDate rangeTo;
  private final DayOfWeek firstDayOfWeek;

  private LocalDate selectedDate;
  private LocalDate keyboardDate;
  private YearMonth displayedYearMonth;
  private Font cellFont;
  private DateVetoPolicy vetoPolicy;
  private DateHighlightPolicy highlightPolicy;
  private final List<Consumer<LocalDate>> listeners = new ArrayList<>();

  private final JLabel[][] dateCells = new JLabel[ROWS][COLS];
  private final LocalDate[][] dateCellValues = new LocalDate[ROWS][COLS];
  private JLabel monthLabel;
  private JLabel yearLabel;
  private JButton prevMonthBtn;
  private JButton nextMonthBtn;

  /** Creates a calendar panel initialized to today using the default locale. */
  public CalendarPanel() {
    this(LocalDate.now());
  }

  /**
   * Creates a calendar panel initialized to the supplied date using the default locale.
   *
   * @param initial the initially selected date
   */
  public CalendarPanel(LocalDate initial) {
    this(initial, Locale.getDefault());
  }

  /**
   * Creates a calendar panel initialized to the supplied date and locale.
   *
   * @param initial the initially selected date
   * @param locale the locale used for month and weekday names
   */
  public CalendarPanel(LocalDate initial, Locale locale) {
    this(null, null, initial, locale);
  }

  /**
   * Creates a calendar panel with an explicit date range.
   *
   * @param from the earliest selectable date, or {@code null} for 20 years before {@code initial}
   *     (or today)
   * @param to the latest selectable date, or {@code null} for 20 years after {@code initial} (or
   *     today)
   * @param initial the initially selected date, or {@code null} for no selection; a date outside
   *     the range is not selected and the closest month in range is displayed instead
   * @param locale the locale used for month and weekday names, or {@code null} for the default
   *     locale
   * @throws IllegalArgumentException if {@code from} is after {@code to}
   */
  public CalendarPanel(LocalDate from, LocalDate to, LocalDate initial, Locale locale) {
    LocalDate base = initial != null ? initial : LocalDate.now();
    this.rangeFrom = from != null ? from : base.minusYears(20);
    this.rangeTo = to != null ? to : base.plusYears(20);
    if (rangeFrom.isAfter(rangeTo)) {
      throw new IllegalArgumentException("'from' date must be before 'to' date");
    }
    this.locale = locale != null ? locale : Locale.getDefault();
    this.selectedDate = initial != null && isInRange(initial) ? initial : null;
    this.displayedYearMonth = clampToRange(YearMonth.from(base));
    this.firstDayOfWeek = WeekFields.of(this.locale).getFirstDayOfWeek();

    setLayout(new GridBagLayout());
    setFocusable(true);
    buildUI();
    installKeyBindings();
    addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent e) {
            drawCalendar();
          }

          @Override
          public void focusLost(FocusEvent e) {
            drawCalendar();
          }
        });
    drawCalendar();
  }

  private boolean isInRange(LocalDate date) {
    return !date.isBefore(rangeFrom) && !date.isAfter(rangeTo);
  }

  private YearMonth clampToRange(YearMonth yearMonth) {
    YearMonth fromYm = YearMonth.from(rangeFrom);
    YearMonth toYm = YearMonth.from(rangeTo);
    if (yearMonth.isBefore(fromYm)) {
      return fromYm;
    }
    return yearMonth.isAfter(toYm) ? toYm : yearMonth;
  }

  private LocalDate clampToRange(LocalDate date) {
    if (date.isBefore(rangeFrom)) {
      return rangeFrom;
    }
    return date.isAfter(rangeTo) ? rangeTo : date;
  }

  private static Color uiColor(String key, Color fallback) {
    Color color = UIManager.getColor(key);
    return color != null ? color : fallback;
  }

  private void installKeyBindings() {
    bindKey(KeyEvent.VK_LEFT, "previousDay", () -> moveKeyboardDate(keyboardDate.minusDays(1)));
    bindKey(KeyEvent.VK_RIGHT, "nextDay", () -> moveKeyboardDate(keyboardDate.plusDays(1)));
    bindKey(KeyEvent.VK_UP, "previousWeek", () -> moveKeyboardDate(keyboardDate.minusWeeks(1)));
    bindKey(KeyEvent.VK_DOWN, "nextWeek", () -> moveKeyboardDate(keyboardDate.plusWeeks(1)));
    bindKey(
        KeyEvent.VK_PAGE_UP, "previousMonth", () -> moveKeyboardDate(keyboardDate.minusMonths(1)));
    bindKey(KeyEvent.VK_PAGE_DOWN, "nextMonth", () -> moveKeyboardDate(keyboardDate.plusMonths(1)));
    bindKey(
        KeyEvent.VK_HOME,
        "firstDayOfMonth",
        () -> moveKeyboardDate(keyboardDate.withDayOfMonth(1)));
    bindKey(
        KeyEvent.VK_END,
        "lastDayOfMonth",
        () -> moveKeyboardDate(keyboardDate.withDayOfMonth(keyboardDate.lengthOfMonth())));
    bindKey(KeyEvent.VK_ENTER, "selectKeyboardDate", () -> selectDate(keyboardDate));
    bindKey(KeyEvent.VK_SPACE, "selectKeyboardDate", () -> selectDate(keyboardDate));
  }

  private void bindKey(int keyCode, String name, Runnable action) {
    getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyCode, 0), name);
    getActionMap()
        .put(
            name,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                action.run();
              }
            });
  }

  private void moveKeyboardDate(LocalDate target) {
    keyboardDate = clampToRange(target);
    displayedYearMonth = YearMonth.from(keyboardDate);
    drawCalendar();
  }

  /**
   * Keeps the keyboard cursor inside the displayed month, preferring the selected date and
   * otherwise the same day of month as before.
   */
  private void syncKeyboardDate() {
    if (keyboardDate != null && YearMonth.from(keyboardDate).equals(displayedYearMonth)) {
      return;
    }
    if (selectedDate != null && YearMonth.from(selectedDate).equals(displayedYearMonth)) {
      keyboardDate = selectedDate;
      return;
    }
    int day = keyboardDate != null ? keyboardDate.getDayOfMonth() : 1;
    keyboardDate =
        clampToRange(displayedYearMonth.atDay(Math.min(day, displayedYearMonth.lengthOfMonth())));
  }

  private void buildUI() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(2, 2, 2, 2);

    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.CENTER;

    gbc.gridx = 0;
    prevMonthBtn = new JButton("\u25C0");
    prevMonthBtn.setMargin(new Insets(1, 3, 1, 3));
    // Keyboard users navigate months with Page Up/Page Down on the panel itself.
    prevMonthBtn.setFocusable(false);
    prevMonthBtn.addActionListener(e -> previousMonth());
    add(prevMonthBtn, gbc);

    gbc.gridx = 1;
    gbc.gridwidth = 3;
    monthLabel = new JLabel("", SwingConstants.CENTER);
    monthLabel.setFont(
        monthLabel.getFont().deriveFont(monthLabel.getFont().getSize2D() + HEADER_FONT_DELTA));
    monthLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    monthLabel.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            showMonthMenu();
          }
        });
    add(monthLabel, gbc);

    gbc.gridx = 4;
    gbc.gridwidth = 2;
    yearLabel = new JLabel("", SwingConstants.CENTER);
    yearLabel.setFont(
        yearLabel.getFont().deriveFont(yearLabel.getFont().getSize2D() + HEADER_FONT_DELTA));
    yearLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    yearLabel.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            showYearPopup();
          }
        });
    add(yearLabel, gbc);

    gbc.gridx = 6;
    gbc.gridwidth = 1;
    nextMonthBtn = new JButton("\u25B6");
    nextMonthBtn.setMargin(new Insets(1, 3, 1, 3));
    nextMonthBtn.setFocusable(false);
    nextMonthBtn.addActionListener(e -> nextMonth());
    add(nextMonthBtn, gbc);

    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.BOTH;
    for (int col = 0; col < COLS; col++) {
      gbc.gridx = col;
      DayOfWeek dow = firstDayOfWeek.plus(col);
      String name = dow.getDisplayName(TextStyle.SHORT, locale);
      JLabel header = new JLabel(name, SwingConstants.CENTER);
      header.setFont(
          header.getFont().deriveFont(Font.BOLD, header.getFont().getSize2D() + HEADER_FONT_DELTA));
      add(header, gbc);
    }

    for (int row = 0; row < ROWS; row++) {
      gbc.gridy = row + 2;
      for (int col = 0; col < COLS; col++) {
        gbc.gridx = col;
        JLabel cell = new JLabel("", SwingConstants.CENTER);
        cell.setOpaque(true);
        cell.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
        if (cellFont == null) {
          Font labelFont = cell.getFont();
          cellFont = labelFont.deriveFont(Font.PLAIN, labelFont.getSize2D() + CELL_FONT_DELTA);
        }
        cell.setFont(cellFont);
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final int r = row;
        final int c = col;
        cell.addMouseListener(
            new MouseAdapter() {
              @Override
              public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
                onCellClicked(r, c);
              }
            });
        dateCells[row][col] = cell;
        add(cell, gbc);
      }
    }
  }

  private void drawCalendar() {
    monthLabel.setText(displayedYearMonth.getMonth().getDisplayName(TextStyle.FULL, locale));
    yearLabel.setText(String.valueOf(displayedYearMonth.getYear()));

    LocalDate firstOfMonth = displayedYearMonth.atDay(1);
    int dayOfWeekValue = firstOfMonth.getDayOfWeek().getValue();
    int firstDayValue = firstDayOfWeek.getValue();
    int offset = (dayOfWeekValue - firstDayValue + 7) % 7;

    LocalDate cellDate = firstOfMonth.minusDays(offset);

    syncKeyboardDate();
    Color normalBg = uiColor("List.background", NORMAL_BG);
    Color normalFg = uiColor("List.foreground", NORMAL_FG);
    Color selectedBg = uiColor("List.selectionBackground", SELECTED_BG);
    Color selectedFg = uiColor("List.selectionForeground", NORMAL_FG);
    Color otherMonthFg = uiColor("Label.disabledForeground", OTHER_MONTH_FG);
    Color vetoedBg = uiColor("Panel.background", VETOED_BG);
    Map<TextAttribute, Object> strikeAttrs = new HashMap<>(cellFont.getAttributes());
    strikeAttrs.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
    Font vetoedFont = cellFont.deriveFont(strikeAttrs);
    boolean showKeyboardCursor = isFocusOwner();

    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        dateCellValues[row][col] = cellDate;
        JLabel cell = dateCells[row][col];
        cell.setText(String.valueOf(cellDate.getDayOfMonth()));
        cell.setBorder(null);
        cell.setToolTipText(null);
        cell.setFont(cellFont);

        boolean isCurrentMonth = YearMonth.from(cellDate).equals(displayedYearMonth);
        boolean isSelected = cellDate.equals(selectedDate);
        boolean isVetoed = !isDateAllowed(cellDate);
        HighlightInfo highlightInfo =
            highlightPolicy != null ? highlightPolicy.getHighlightInfo(cellDate) : null;

        if (isSelected) {
          cell.setBackground(selectedBg);
          cell.setForeground(selectedFg);
          cell.setBorder(new LineBorder(SELECTED_BORDER, 1));
        } else if (isVetoed) {
          cell.setBackground(vetoedBg);
          cell.setForeground(isCurrentMonth ? normalFg : otherMonthFg);
          cell.setFont(vetoedFont);
        } else if (!isCurrentMonth) {
          cell.setBackground(normalBg);
          cell.setForeground(otherMonthFg);
        } else {
          cell.setBackground(normalBg);
          cell.setForeground(normalFg);

          if (highlightInfo != null && highlightInfo.getBackgroundColor() != null) {
            cell.setBackground(highlightInfo.getBackgroundColor());
          }
        }

        if (showKeyboardCursor && cellDate.equals(keyboardDate)) {
          cell.setBorder(BorderFactory.createDashedBorder(normalFg, 2f, 2f));
        }

        if (highlightInfo != null && highlightInfo.getTooltip() != null) {
          cell.setToolTipText(highlightInfo.getTooltip());
        }

        cellDate = cellDate.plusDays(1);
      }
    }

    updateNavigationButtons();
  }

  private void updateNavigationButtons() {
    YearMonth fromYm = YearMonth.from(rangeFrom);
    YearMonth toYm = YearMonth.from(rangeTo);
    prevMonthBtn.setEnabled(displayedYearMonth.isAfter(fromYm));
    nextMonthBtn.setEnabled(displayedYearMonth.isBefore(toYm));
  }

  private boolean isDateAllowed(LocalDate date) {
    if (date.isBefore(rangeFrom) || date.isAfter(rangeTo)) {
      return false;
    }
    return vetoPolicy == null || vetoPolicy.isDateAllowed(date);
  }

  private void onCellClicked(int row, int col) {
    selectDate(dateCellValues[row][col]);
  }

  /** Selects a date chosen by the user; listeners are notified even if it is already selected. */
  private void selectDate(LocalDate date) {
    if (date == null || !isDateAllowed(date)) {
      return;
    }
    selectedDate = date;
    keyboardDate = date;
    displayedYearMonth = YearMonth.from(date);
    drawCalendar();
    fireListeners(date);
  }

  private void showMonthMenu() {
    JPopupMenu menu = new JPopupMenu();
    YearMonth fromYm = YearMonth.from(rangeFrom);
    YearMonth toYm = YearMonth.from(rangeTo);
    for (int m = 1; m <= 12; m++) {
      YearMonth ym = YearMonth.of(displayedYearMonth.getYear(), m);
      if (ym.isBefore(fromYm) || ym.isAfter(toYm)) {
        continue;
      }
      String name = ym.getMonth().getDisplayName(TextStyle.FULL, locale);
      JMenuItem item = new JMenuItem(name);
      final int month = m;
      item.addActionListener(
          e -> {
            displayedYearMonth = YearMonth.of(displayedYearMonth.getYear(), month);
            drawCalendar();
          });
      menu.add(item);
    }
    menu.show(monthLabel, 0, monthLabel.getHeight());
  }

  private void showYearPopup() {
    JPopupMenu popup = new JPopupMenu();
    int minYear = rangeFrom.getYear();
    int maxYear = rangeTo.getYear();
    int year = Math.max(minYear, Math.min(maxYear, displayedYearMonth.getYear()));
    JSpinner spinner = new JSpinner(new SpinnerNumberModel(year, minYear, maxYear, 1));
    JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "####");
    spinner.setEditor(editor);
    Font bigFont = yearLabel.getFont().deriveFont(Font.PLAIN, yearLabel.getFont().getSize2D() + 6f);
    editor.getTextField().setFont(bigFont);
    editor.getTextField().setColumns(6);
    editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
    spinner.setAlignmentX(Component.CENTER_ALIGNMENT);
    spinner.setPreferredSize(new Dimension(130, 42));
    for (Component child : spinner.getComponents()) {
      if (child instanceof JButton) {
        child.setPreferredSize(new Dimension(26, 20));
      }
    }

    spinner.addChangeListener(e -> updateDisplayedYear((Integer) spinner.getValue()));

    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

    JLabel title = new JLabel("Select year", SwingConstants.CENTER);
    title.setAlignmentX(Component.CENTER_ALIGNMENT);
    title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 1f));

    JLabel hint = new JLabel("Type or use arrows", SwingConstants.CENTER);
    hint.setAlignmentX(Component.CENTER_ALIGNMENT);
    hint.setFont(hint.getFont().deriveFont(hint.getFont().getSize2D() - 1f));

    content.add(title);
    content.add(Box.createVerticalStrut(8));
    content.add(spinner);
    content.add(Box.createVerticalStrut(6));
    content.add(hint);

    popup.add(content);

    editor.getTextField().getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "applyYear");
    editor
        .getTextField()
        .getActionMap()
        .put(
            "applyYear",
            new javax.swing.AbstractAction() {
              @Override
              public void actionPerformed(java.awt.event.ActionEvent e) {
                updateDisplayedYear(committedYear(spinner));
                popup.setVisible(false);
              }
            });
    editor.getTextField().getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "dismissYearPopup");
    editor
        .getTextField()
        .getActionMap()
        .put(
            "dismissYearPopup",
            new javax.swing.AbstractAction() {
              @Override
              public void actionPerformed(java.awt.event.ActionEvent e) {
                popup.setVisible(false);
              }
            });
    spinner
        .getInputMap(JSpinner.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke("ESCAPE"), "dismissYearPopup");
    spinner
        .getActionMap()
        .put(
            "dismissYearPopup",
            new javax.swing.AbstractAction() {
              @Override
              public void actionPerformed(java.awt.event.ActionEvent e) {
                popup.setVisible(false);
              }
            });

    editor
        .getTextField()
        .addKeyListener(
            new java.awt.event.KeyAdapter() {
              @Override
              public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                  updateDisplayedYear(committedYear(spinner));
                }
              }
            });
    popup.show(yearLabel, 0, yearLabel.getHeight());
    SwingUtilities.invokeLater(() -> editor.getTextField().requestFocusInWindow());
  }

  private void updateDisplayedYear(int year) {
    YearMonth candidate = YearMonth.of(year, displayedYearMonth.getMonthValue());
    YearMonth fromYm = YearMonth.from(rangeFrom);
    YearMonth toYm = YearMonth.from(rangeTo);
    if (candidate.isBefore(fromYm)) {
      candidate = fromYm;
    }
    if (candidate.isAfter(toYm)) {
      candidate = toYm;
    }
    displayedYearMonth = candidate;
    drawCalendar();
  }

  /** Commits a typed but not yet committed spinner value before reading it. */
  private static int committedYear(JSpinner spinner) {
    try {
      spinner.commitEdit();
    } catch (ParseException e) {
      // Invalid or out-of-range text: keep the last valid value.
    }
    return (Integer) spinner.getValue();
  }

  private void fireListeners(LocalDate date) {
    for (Consumer<LocalDate> listener : new ArrayList<>(listeners)) {
      try {
        listener.accept(date);
      } catch (RuntimeException e) {
        Thread thread = Thread.currentThread();
        thread.getUncaughtExceptionHandler().uncaughtException(thread, e);
      }
    }
  }

  /**
   * Returns the currently selected date.
   *
   * @return the selected date, or {@code null} if no date is selected
   */
  public LocalDate getSelectedDate() {
    return selectedDate;
  }

  /**
   * Selects the supplied date if it is within range and not vetoed. Listeners are notified only
   * when the selection actually changes.
   *
   * @param date the date to select, or {@code null} to clear the selection
   */
  public void setSelectedDate(LocalDate date) {
    if (date != null && !isDateAllowed(date)) {
      return;
    }
    LocalDate previous = selectedDate;
    this.selectedDate = date;
    if (date != null) {
      this.keyboardDate = date;
      this.displayedYearMonth = YearMonth.from(date);
    }
    drawCalendar();
    if (!Objects.equals(previous, date)) {
      fireListeners(date);
    }
  }

  /**
   * Returns the year-month currently displayed in the calendar.
   *
   * @return the displayed year-month
   */
  public YearMonth getDisplayedYearMonth() {
    return displayedYearMonth;
  }

  /**
   * Changes the displayed month if it is within the configured range.
   *
   * @param yearMonth the year-month to display
   */
  public void setDisplayedYearMonth(YearMonth yearMonth) {
    YearMonth fromYm = YearMonth.from(rangeFrom);
    YearMonth toYm = YearMonth.from(rangeTo);
    if (yearMonth.isBefore(fromYm) || yearMonth.isAfter(toYm)) {
      return;
    }
    this.displayedYearMonth = yearMonth;
    drawCalendar();
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
   * Removes a previously registered selection listener.
   *
   * @param listener the listener to remove
   */
  public void removeListener(Consumer<LocalDate> listener) {
    listeners.remove(listener);
  }

  /**
   * Applies a veto policy used to disable dates in the calendar.
   *
   * @param policy the veto policy to apply, or {@code null} to allow all dates
   */
  public void setVetoPolicy(DateVetoPolicy policy) {
    this.vetoPolicy = policy;
    drawCalendar();
  }

  /**
   * Applies a highlight policy used to decorate dates in the calendar.
   *
   * @param policy the highlight policy to apply, or {@code null} to remove highlighting
   */
  public void setHighlightPolicy(DateHighlightPolicy policy) {
    this.highlightPolicy = policy;
    drawCalendar();
  }

  /** Advances the displayed month by one month when still within range. */
  public void nextMonth() {
    YearMonth next = displayedYearMonth.plusMonths(1);
    YearMonth toYm = YearMonth.from(rangeTo);
    if (!next.isAfter(toYm)) {
      displayedYearMonth = next;
      drawCalendar();
    }
  }

  /** Moves the displayed month back by one month when still within range. */
  public void previousMonth() {
    YearMonth prev = displayedYearMonth.minusMonths(1);
    YearMonth fromYm = YearMonth.from(rangeFrom);
    if (!prev.isBefore(fromYm)) {
      displayedYearMonth = prev;
      drawCalendar();
    }
  }
}
