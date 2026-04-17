package se.alipsa.datepicker;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

public class CalendarPanel extends JPanel {

    private static final Color SELECTED_BG = new Color(163, 184, 204);
    private static final Color SELECTED_BORDER = new Color(99, 130, 191);
    private static final Color OTHER_MONTH_FG = Color.LIGHT_GRAY;
    private static final Color VETOED_BG = new Color(240, 240, 240);
    private static final Color HEADER_BG = new Color(240, 240, 240);
    private static final Color NORMAL_BG = Color.WHITE;
    private static final int CELL_SIZE = 30;
    private static final int ROWS = 6;
    private static final int COLS = 7;

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

    public CalendarPanel() {
        this(LocalDate.now());
    }

    public CalendarPanel(LocalDate initial) {
        this(initial, Locale.getDefault());
    }

    public CalendarPanel(LocalDate initial, Locale locale) {
        this(initial.minusYears(20), initial.plusYears(20), initial, locale);
    }

    public CalendarPanel(LocalDate from, LocalDate to, LocalDate initial, Locale locale) {
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

        // Row 0: Navigation
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        gbc.gridx = 0;
        prevMonthBtn = new JButton("\u25C0");
        prevMonthBtn.setMargin(new Insets(1, 4, 1, 4));
        prevMonthBtn.addActionListener(e -> previousMonth());
        add(prevMonthBtn, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        monthLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showMonthMenu();
            }
        });
        add(monthLabel, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 2;
        yearLabel = new JLabel("", SwingConstants.CENTER);
        yearLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        yearLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showYearMenu();
            }
        });
        add(yearLabel, gbc);

        gbc.gridx = 6;
        gbc.gridwidth = 1;
        nextMonthBtn = new JButton("\u25B6");
        nextMonthBtn.setMargin(new Insets(1, 4, 1, 4));
        nextMonthBtn.addActionListener(e -> nextMonth());
        add(nextMonthBtn, gbc);

        // Row 1: Weekday headers
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.BOTH;
        for (int col = 0; col < COLS; col++) {
            gbc.gridx = col;
            DayOfWeek dow = firstDayOfWeek.plus(col);
            String name = dow.getDisplayName(TextStyle.SHORT, locale);
            JLabel header = new JLabel(name, SwingConstants.CENTER);
            header.setFont(header.getFont().deriveFont(Font.BOLD));
            header.setOpaque(true);
            header.setBackground(HEADER_BG);
            header.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
            add(header, gbc);
        }

        // Rows 2-7: Date cells
        for (int row = 0; row < ROWS; row++) {
            gbc.gridy = row + 2;
            for (int col = 0; col < COLS; col++) {
                gbc.gridx = col;
                JLabel cell = new JLabel("", SwingConstants.CENTER);
                cell.setOpaque(true);
                cell.setBackground(NORMAL_BG);
                cell.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
                cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                final int r = row;
                final int c = col;
                cell.addMouseListener(new MouseAdapter() {
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

                if (isSelected) {
                    cell.setBackground(SELECTED_BG);
                    cell.setForeground(Color.BLACK);
                    cell.setBorder(new LineBorder(SELECTED_BORDER, 1));
                    cell.setFont(cell.getFont().deriveFont(Font.PLAIN));
                } else if (isVetoed) {
                    cell.setBackground(VETOED_BG);
                    cell.setForeground(isCurrentMonth ? Color.BLACK : OTHER_MONTH_FG);
                    Font base = cell.getFont().deriveFont(Font.PLAIN);
                    java.util.Map<java.awt.font.TextAttribute, Object> attrs = new java.util.HashMap<>(base.getAttributes());
                    attrs.put(java.awt.font.TextAttribute.STRIKETHROUGH, java.awt.font.TextAttribute.STRIKETHROUGH_ON);
                    cell.setFont(base.deriveFont(attrs));
                } else if (!isCurrentMonth) {
                    cell.setBackground(NORMAL_BG);
                    cell.setForeground(OTHER_MONTH_FG);
                    cell.setFont(cell.getFont().deriveFont(Font.PLAIN));
                } else {
                    cell.setBackground(NORMAL_BG);
                    cell.setForeground(Color.BLACK);
                    cell.setFont(cell.getFont().deriveFont(Font.PLAIN));

                    // Apply highlight policy
                    if (highlightPolicy != null) {
                        HighlightInfo info = highlightPolicy.getHighlightInfo(cellDate);
                        if (info != null) {
                            if (info.getBackgroundColor() != null) {
                                cell.setBackground(info.getBackgroundColor());
                            }
                            if (info.getTooltip() != null) {
                                cell.setToolTipText(info.getTooltip());
                            }
                        }
                    }
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
            if (ym.isBefore(fromYm) || ym.isAfter(toYm)) continue;
            String name = ym.getMonth().getDisplayName(TextStyle.FULL, locale);
            JMenuItem item = new JMenuItem(name);
            final int month = m;
            item.addActionListener(e -> {
                displayedYearMonth = YearMonth.of(displayedYearMonth.getYear(), month);
                drawCalendar();
            });
            menu.add(item);
        }
        menu.show(monthLabel, 0, monthLabel.getHeight());
    }

    private void showYearMenu() {
        JPopupMenu menu = new JPopupMenu();
        int fromYear = rangeFrom.getYear();
        int toYear = rangeTo.getYear();
        for (int y = fromYear; y <= toYear; y++) {
            JMenuItem item = new JMenuItem(String.valueOf(y));
            final int year = y;
            item.addActionListener(e -> {
                displayedYearMonth = YearMonth.of(year, displayedYearMonth.getMonthValue());
                drawCalendar();
            });
            menu.add(item);
        }
        menu.show(yearLabel, 0, yearLabel.getHeight());
    }

    private void fireListeners(LocalDate date) {
        for (Consumer<LocalDate> listener : listeners) {
            listener.accept(date);
        }
    }

    // Public API

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
        this.displayedYearMonth = YearMonth.from(date);
        drawCalendar();
        fireListeners(date);
    }

    public YearMonth getDisplayedYearMonth() {
        return displayedYearMonth;
    }

    public void setDisplayedYearMonth(YearMonth yearMonth) {
        YearMonth fromYm = YearMonth.from(rangeFrom);
        YearMonth toYm = YearMonth.from(rangeTo);
        if (yearMonth.isBefore(fromYm) || yearMonth.isAfter(toYm)) {
            return;
        }
        this.displayedYearMonth = yearMonth;
        drawCalendar();
    }

    public void addListener(Consumer<LocalDate> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<LocalDate> listener) {
        listeners.remove(listener);
    }

    public void setVetoPolicy(DateVetoPolicy policy) {
        this.vetoPolicy = policy;
        drawCalendar();
    }

    public void setHighlightPolicy(DateHighlightPolicy policy) {
        this.highlightPolicy = policy;
        drawCalendar();
    }

    public void nextMonth() {
        YearMonth next = displayedYearMonth.plusMonths(1);
        YearMonth toYm = YearMonth.from(rangeTo);
        if (!next.isAfter(toYm)) {
            displayedYearMonth = next;
            drawCalendar();
        }
    }

    public void previousMonth() {
        YearMonth prev = displayedYearMonth.minusMonths(1);
        YearMonth fromYm = YearMonth.from(rangeFrom);
        if (!prev.isBefore(fromYm)) {
            displayedYearMonth = prev;
            drawCalendar();
        }
    }
}
