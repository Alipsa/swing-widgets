package se.alipsa.datepicker;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

public class MaskedDateField extends JTextField {

    private static final Color INVALID_COLOR = Color.RED;
    private static final Color NORMAL_COLOR = Color.BLACK;

    private final String datePattern;
    private final Locale locale;
    private final DateTimeFormatter formatter;
    private final String maskTemplate;
    private final boolean[] editablePositions;

    private LocalDate lastValidDate;
    private DateVetoPolicy vetoPolicy;
    private final List<Consumer<LocalDate>> listeners = new ArrayList<>();
    private boolean internalUpdate = false;

    public MaskedDateField(String datePattern, Locale locale) {
        this.datePattern = datePattern;
        this.locale = locale;
        this.formatter = DateTimeFormatter.ofPattern(datePattern, locale);
        this.maskTemplate = buildMaskTemplate(datePattern);
        this.editablePositions = buildEditablePositions(datePattern);

        setColumns(datePattern.length());
        setText(maskTemplate);

        PlainDocument doc = (PlainDocument) getDocument();
        doc.setDocumentFilter(new MaskDocumentFilter());

        addKeyListener(new MaskKeyListener());
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                revertIfInvalid();
            }
        });
    }

    private static String buildMaskTemplate(String pattern) {
        StringBuilder sb = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            if (Character.isLetter(c)) {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean[] buildEditablePositions(String pattern) {
        boolean[] positions = new boolean[pattern.length()];
        for (int i = 0; i < pattern.length(); i++) {
            positions[i] = Character.isLetter(pattern.charAt(i));
        }
        return positions;
    }

    public LocalDate getDate() {
        return lastValidDate;
    }

    public void setDate(LocalDate date) {
        internalUpdate = true;
        try {
            if (date == null) {
                setText(maskTemplate);
                lastValidDate = null;
            } else {
                String formatted = formatter.format(date);
                setText(formatted);
                if (isDateAllowed(date)) {
                    lastValidDate = date;
                }
            }
            updateVisualState();
        } finally {
            internalUpdate = false;
        }
    }

    public String getDatePattern() {
        return datePattern;
    }

    public String getMaskTemplate() {
        return maskTemplate;
    }

    public boolean isDateValid() {
        LocalDate parsed = parseText();
        return parsed != null && isDateAllowed(parsed);
    }

    public void setVetoPolicy(DateVetoPolicy policy) {
        this.vetoPolicy = policy;
        updateVisualState();
    }

    public void addListener(Consumer<LocalDate> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<LocalDate> listener) {
        listeners.remove(listener);
    }

    private boolean isDateAllowed(LocalDate date) {
        return vetoPolicy == null || vetoPolicy.isDateAllowed(date);
    }

    private LocalDate parseText() {
        String text = getText();
        if (text == null || text.equals(maskTemplate) || text.contains("_")) {
            return null;
        }
        try {
            return LocalDate.parse(text, formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void revertIfInvalid() {
        LocalDate parsed = parseText();
        if (parsed == null || !isDateAllowed(parsed)) {
            internalUpdate = true;
            try {
                if (lastValidDate != null) {
                    setText(formatter.format(lastValidDate));
                } else {
                    setText(maskTemplate);
                }
                updateVisualState();
            } finally {
                internalUpdate = false;
            }
        }
    }

    private void updateVisualState() {
        String text = getText();
        if (text == null || text.equals(maskTemplate) || text.contains("_")) {
            setForeground(NORMAL_COLOR);
            setBackground(Color.WHITE);
            setFont(getFont().deriveFont(Font.PLAIN));
            return;
        }

        LocalDate parsed = parseText();
        if (parsed == null) {
            setForeground(INVALID_COLOR);
            setBackground(Color.WHITE);
            setFont(getFont().deriveFont(Font.PLAIN));
        } else if (!isDateAllowed(parsed)) {
            setForeground(NORMAL_COLOR);
            setBackground(Color.WHITE);
            Font base = getFont().deriveFont(Font.PLAIN);
            Map<TextAttribute, Object> attrs = new HashMap<>(base.getAttributes());
            attrs.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
            setFont(base.deriveFont(attrs));
        } else {
            setForeground(NORMAL_COLOR);
            setBackground(Color.WHITE);
            setFont(getFont().deriveFont(Font.PLAIN));
        }
    }

    private void onTextChanged() {
        if (internalUpdate) return;
        LocalDate parsed = parseText();
        updateVisualState();
        if (parsed != null && isDateAllowed(parsed)) {
            LocalDate previous = lastValidDate;
            lastValidDate = parsed;
            if (previous == null || !previous.equals(parsed)) {
                fireListeners(parsed);
            }
        }
    }

    private void fireListeners(LocalDate date) {
        for (Consumer<LocalDate> listener : new ArrayList<>(listeners)) {
            try {
                listener.accept(date);
            } catch (RuntimeException e) {
                System.err.println("MaskedDateField listener threw exception: " + e.getMessage());
            }
        }
    }

    private class MaskDocumentFilter extends DocumentFilter {

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (internalUpdate) {
                super.replace(fb, offset, length, text, attrs);
                return;
            }

            if (text == null || text.isEmpty()) {
                return;
            }

            StringBuilder result = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
            int pos = offset;

            for (char c : text.toCharArray()) {
                if (!Character.isDigit(c)) continue;

                while (pos < editablePositions.length && !editablePositions[pos]) {
                    pos++;
                }
                if (pos >= editablePositions.length) break;

                result.setCharAt(pos, c);
                pos++;
            }

            internalUpdate = true;
            try {
                fb.replace(0, fb.getDocument().getLength(), result.toString(), attrs);
            } finally {
                internalUpdate = false;
            }

            final int caretPos = pos < editablePositions.length ? pos : editablePositions.length;
            SwingUtilities.invokeLater(() -> {
                setCaretPosition(caretPos);
                onTextChanged();
            });
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            if (internalUpdate) {
                super.remove(fb, offset, length);
                return;
            }

            StringBuilder result = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
            for (int i = offset; i < offset + length && i < editablePositions.length; i++) {
                if (editablePositions[i]) {
                    result.setCharAt(i, '_');
                }
            }

            internalUpdate = true;
            try {
                fb.replace(0, fb.getDocument().getLength(), result.toString(), null);
            } finally {
                internalUpdate = false;
            }

            final int caretPos = offset;
            SwingUtilities.invokeLater(() -> {
                setCaretPosition(caretPos);
                onTextChanged();
            });
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                throws BadLocationException {
            replace(fb, offset, 0, text, attrs);
        }
    }

    private class MaskKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int pos = getCaretPosition();
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                int next = pos + 1;
                while (next < editablePositions.length && !editablePositions[next]) {
                    next++;
                }
                if (next <= editablePositions.length) {
                    setCaretPosition(next);
                    e.consume();
                }
            } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                int prev = pos - 1;
                while (prev >= 0 && !editablePositions[prev]) {
                    prev--;
                }
                if (prev >= 0) {
                    setCaretPosition(prev);
                    e.consume();
                }
            }
        }
    }
}
