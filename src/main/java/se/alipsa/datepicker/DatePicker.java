package se.alipsa.datepicker;

import static java.awt.Image.SCALE_SMOOTH;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.chrono.IsoChronology;
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

public class DatePicker extends JPanel {

    private final Locale locale;
    private final LocalDate rangeFrom;
    private final LocalDate rangeTo;
    private final String datePattern;

    private LocalDate lastValidDate;
    private DateVetoPolicy vetoPolicy;
    private DateHighlightPolicy highlightPolicy;
    private final List<Consumer<LocalDate>> listeners = new ArrayList<>();
    private TextFieldPosition textFieldPosition = TextFieldPosition.LEFT;

    private MaskedDateField textField;
    private JButton calendarButton;
    private JWindow popupWindow;
    private CalendarPanel calendarPanel;

    public DatePicker() {
        this(LocalDate.now());
    }

    public DatePicker(LocalDate initial) {
        this(initial, Locale.getDefault());
    }

    public DatePicker(LocalDate initial, Locale locale) {
        this(
            initial != null ? initial.minusYears(20) : LocalDate.now().minusYears(20),
            initial != null ? initial.plusYears(20) : LocalDate.now().plusYears(20),
            initial != null ? initial : LocalDate.now(),
            locale
        );
    }

    public DatePicker(LocalDate from, LocalDate to, LocalDate initial) {
        this(from, to, initial, Locale.getDefault());
    }

    public DatePicker(LocalDate from, LocalDate to, LocalDate initial, Locale locale) {
        this(from, to, initial, locale, derivePattern(locale));
    }

    public DatePicker(LocalDate from, LocalDate to, LocalDate initial, Locale locale, String datePattern) {
        this.locale = locale != null ? locale : Locale.getDefault();
        this.rangeFrom = from != null ? from : LocalDate.now().minusYears(20);
        this.rangeTo = to != null ? to : LocalDate.now().plusYears(20);
        this.datePattern = datePattern != null ? datePattern : derivePattern(this.locale);
        this.lastValidDate = initial;

        setLayout(new GridBagLayout());
        createComponents();
        layoutComponents();

        if (initial != null) {
            textField.setDate(initial);
        }
    }

    private static String derivePattern(Locale locale) {
        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale
        );
        // Normalize to fixed-width: e.g. "M/d/yy" -> "MM/dd/yyyy"
        pattern = pattern.replace("yyyy", "YYYY_PLACEHOLDER");
        pattern = pattern.replace("yy", "yyyy");
        pattern = pattern.replace("YYYY_PLACEHOLDER", "yyyy");
        pattern = pattern.replaceAll("(?<!M)M(?!M)", "MM");
        pattern = pattern.replaceAll("(?<!d)d(?!d)", "dd");
        return pattern;
    }

    private void createComponents() {
        textField = new MaskedDateField(datePattern, locale);
        textField.addListener(this::onTextFieldDateChanged);

        ImageIcon icon = loadIcon("/calendar.png", 20, 20);
        if (icon != null) {
            calendarButton = new JButton(icon);
        } else {
            calendarButton = new JButton("\u25BC");
        }
        calendarButton.setMargin(new Insets(2, 4, 2, 4));
        calendarButton.addActionListener(e -> togglePopup());
    }

    private void layoutComponents() {
        removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 2, 0, 2);

        switch (textFieldPosition) {
            case LEFT:
                gbc.gridx = 0; gbc.gridy = 0;
                add(textField, gbc);
                gbc.gridx = 1;
                add(calendarButton, gbc);
                break;
            case RIGHT:
                gbc.gridx = 0; gbc.gridy = 0;
                add(calendarButton, gbc);
                gbc.gridx = 1;
                add(textField, gbc);
                break;
            case ABOVE:
                gbc.gridx = 0; gbc.gridy = 0;
                add(textField, gbc);
                gbc.gridy = 1;
                add(calendarButton, gbc);
                break;
            case BELOW:
                gbc.gridx = 0; gbc.gridy = 0;
                add(calendarButton, gbc);
                gbc.gridy = 1;
                add(textField, gbc);
                break;
        }
        revalidate();
        repaint();
    }

    private ImageIcon loadIcon(String path, int width, int height) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) return null;
            BufferedImage img = ImageIO.read(is);
            return new ImageIcon(new ImageIcon(img).getImage().getScaledInstance(width, height, SCALE_SMOOTH));
        } catch (IOException e) {
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
            lastValidDate = date;
            textField.setDate(date);
            closePopup();
            fireListeners(date);
        }
    }

    private boolean isDateAllowed(LocalDate date) {
        if (date.isBefore(rangeFrom) || date.isAfter(rangeTo)) {
            return false;
        }
        return vetoPolicy == null || vetoPolicy.isDateAllowed(date);
    }

    private void fireListeners(LocalDate date) {
        for (Consumer<LocalDate> listener : listeners) {
            listener.accept(date);
        }
    }

    // Public API

    public LocalDate getDate() {
        return lastValidDate;
    }

    public void setDate(LocalDate date) {
        if (date != null && !isDateAllowed(date)) {
            // Do not accept vetoed/out-of-range dates
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

    public void clear() {
        lastValidDate = null;
        textField.setDate(null);
        fireListeners(null);
    }

    public void addListener(Consumer<LocalDate> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<LocalDate> listener) {
        listeners.remove(listener);
    }

    public void openPopup() {
        if (popupWindow != null && popupWindow.isVisible()) {
            return;
        }

        calendarPanel = new CalendarPanel(rangeFrom, rangeTo,
            lastValidDate != null ? lastValidDate : LocalDate.now(), locale);
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

        // Position below the text field
        java.awt.Point loc = textField.getLocationOnScreen();
        int x = loc.x;
        int y = loc.y + textField.getHeight();

        // Keep within screen bounds
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        if (x + popupWindow.getWidth() > screenSize.width) {
            x = screenSize.width - popupWindow.getWidth();
        }
        if (y + popupWindow.getHeight() > screenSize.height) {
            y = loc.y - popupWindow.getHeight();
        }

        popupWindow.setLocation(x, y);
        popupWindow.setVisible(true);

        // Close on focus loss
        popupWindow.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {}

            @Override
            public void windowLostFocus(WindowEvent e) {
                SwingUtilities.invokeLater(() -> closePopup());
            }
        });
    }

    public void closePopup() {
        if (popupWindow != null) {
            popupWindow.setVisible(false);
            popupWindow.dispose();
            popupWindow = null;
            calendarPanel = null;
        }
    }

    public void togglePopup() {
        if (isPopupOpen()) {
            closePopup();
        } else {
            openPopup();
        }
    }

    public boolean isPopupOpen() {
        return popupWindow != null && popupWindow.isVisible();
    }

    public void setVetoPolicy(DateVetoPolicy policy) {
        this.vetoPolicy = policy;
        textField.setVetoPolicy(policy);
        if (calendarPanel != null) {
            calendarPanel.setVetoPolicy(policy);
        }
    }

    public void setHighlightPolicy(DateHighlightPolicy policy) {
        this.highlightPolicy = policy;
        if (calendarPanel != null) {
            calendarPanel.setHighlightPolicy(policy);
        }
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    public LocalDate getStart() {
        return rangeFrom;
    }

    public LocalDate getEnd() {
        return rangeTo;
    }

    public String getDatePattern() {
        return datePattern;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        calendarButton.setEnabled(enabled);
        if (!enabled) {
            closePopup();
        }
    }

    public TextFieldPosition getTextFieldPosition() {
        return textFieldPosition;
    }

    public void setTextFieldPosition(TextFieldPosition position) {
        this.textFieldPosition = position;
        layoutComponents();
    }

    public MaskedDateField getTextField() {
        return textField;
    }

    public JButton getCalendarButton() {
        return calendarButton;
    }

    public static void main(String[] args) {
        javax.swing.JFrame frame = new javax.swing.JFrame("DatePicker Demo");
        JPanel panel = new JPanel();
        frame.add(panel);

        DatePicker picker = new DatePicker();
        picker.addListener(date -> System.out.println("Selected: " + date));
        panel.add(new javax.swing.JLabel("Date: "));
        panel.add(picker);

        frame.pack();
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
