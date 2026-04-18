package se.alipsa.datepicker;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
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
import java.util.function.Consumer;
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
import javax.swing.border.LineBorder;

/** A standalone month calendar component with optional veto and highlight policies. */
public class CalendarPanel extends JPanel {

  private static final Color SELECTED_BG = new Color(163, 184, 204);
  private static final Color SELECTED_BORDER = new Color(99, 130, 191);
  private static final Color OTHER_MONTH_FG = Color.LIGHT_GRAY;
  private static final Color VETOED_BG = new Color(240, 240, 240);
  private static final Color HEADER_BG = new Color(240, 240, 240);
  private static final Color NORMAL_BG = Color.WHITE;
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
  private YearMonth displayedYearMonth;
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
    this(initial.minusYears(20), initial.plusYears(20), initial, locale);
  }

  /**
   * Creates a calendar panel with an explicit date range.
   *
   * @param from the earliest selectable date
   * @param to the latest selectable date
   * @param initial the initially selected date
   * @param locale the locale used for month and weekday names
   * @throws IllegalArgumentException if {@code from} is after {@code to}
   */
  public CalendarPanel(LocalDate from, LocalDate to, LocalDate initial, Locale locale) {
    if (from != null && to != null && from.isAfter(to)) {
      throw new IllegalArgumentException("'from' date must be before 'to' date");
    }
    this.locale = locale;
    this.rangeFrom = from;
    this.rangeTo = to;
    this.selectedDate = initial;
    this.displayedYearMonth = YearMonth.from(initial);
    this.firstDayOfWeek = WeekFields.of(locale).getFirstDayOfWeek();

    setLayout(new GridBagLayout());
    setBackground(HEADER_BG);
    buildUI();
    drawCalendar();
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
      header.setOpaque(true);
      header.setBackground(HEADER_BG);
      add(header, gbc);
    }

    for (int row = 0; row < ROWS; row++) {
      gbc.gridy = row + 2;
      for (int col = 0; col < COLS; col++) {
        gbc.gridx = col;
        JLabel cell = new JLabel("", SwingConstants.CENTER);
        cell.setOpaque(true);
        cell.setBackground(NORMAL_BG);
        cell.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
        cell.setFont(cell.getFont().deriveFont(cell.getFont().getSize2D() + CELL_FONT_DELTA));
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final int r = row;
        final int c = col;
        cell.addMouseListener(
            new MouseAdapter() {
              @Override
              public void mouseClicked(MouseEvent e) {
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

    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLS; col++) {
        dateCellValues[row][col] = cellDate;
        JLabel cell = dateCells[row][col];
        cell.setText(String.valueOf(cellDate.getDayOfMonth()));
        cell.setBorder(null);
        cell.setToolTipText(null);

        boolean isCurrentMonth = YearMonth.from(cellDate).equals(displayedYearMonth);
        boolean isSelected = cellDate.equals(selectedDate);
        boolean isVetoed = !isDateAllowed(cellDate);
        HighlightInfo highlightInfo =
            highlightPolicy != null ? highlightPolicy.getHighlightInfo(cellDate) : null;

        if (isSelected) {
          cell.setBackground(SELECTED_BG);
          cell.setForeground(Color.BLACK);
          cell.setBorder(new LineBorder(SELECTED_BORDER, 1));
          cell.setFont(cell.getFont().deriveFont(Font.PLAIN));
        } else if (isVetoed) {
          cell.setBackground(VETOED_BG);
          cell.setForeground(isCurrentMonth ? Color.BLACK : OTHER_MONTH_FG);
          Font base = cell.getFont().deriveFont(Font.PLAIN);
          Map<TextAttribute, Object> attrs = new HashMap<>(base.getAttributes());
          attrs.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
          cell.setFont(base.deriveFont(attrs));
        } else if (!isCurrentMonth) {
          cell.setBackground(NORMAL_BG);
          cell.setForeground(OTHER_MONTH_FG);
          cell.setFont(cell.getFont().deriveFont(Font.PLAIN));
        } else {
          cell.setBackground(NORMAL_BG);
          cell.setForeground(Color.BLACK);
          cell.setFont(cell.getFont().deriveFont(Font.PLAIN));

          if (highlightInfo != null && highlightInfo.getBackgroundColor() != null) {
            cell.setBackground(highlightInfo.getBackgroundColor());
          }
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
    LocalDate date = dateCellValues[row][col];
    if (!isDateAllowed(date)) {
      return;
    }
    selectedDate = date;
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
    JSpinner spinner =
        new JSpinner(
            new SpinnerNumberModel(
                displayedYearMonth.getYear(), rangeFrom.getYear(), rangeTo.getYear(), 1));
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
                updateDisplayedYear((Integer) spinner.getValue());
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
                  updateDisplayedYear((Integer) spinner.getValue());
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

  private void fireListeners(LocalDate date) {
    for (Consumer<LocalDate> listener : new ArrayList<>(listeners)) {
      try {
        listener.accept(date);
      } catch (RuntimeException e) {
        System.err.println("CalendarPanel listener threw exception: " + e.getMessage());
      }
    }
  }

  /**
   * Returns the currently selected date.
   *
   * @return the selected date
   */
  public LocalDate getSelectedDate() {
    return selectedDate;
  }

  /**
   * Selects the supplied date if it is within range and not vetoed.
   *
   * @param date the date to select
   */
  public void setSelectedDate(LocalDate date) {
    if (!isDateAllowed(date)) {
      return;
    }
    this.selectedDate = date;
    this.displayedYearMonth = YearMonth.from(date);
    drawCalendar();
    fireListeners(date);
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
