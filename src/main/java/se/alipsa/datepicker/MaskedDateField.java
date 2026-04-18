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

/** A text field that enforces a date mask and parses valid input into {@link LocalDate} values. */
public class MaskedDateField extends JTextField {

  private static final Color INVALID_COLOR = Color.RED;
  private static final Color NORMAL_COLOR = Color.BLACK;

  private final String datePattern;
  private final Locale locale;
  private final DateTimeFormatter formatter;
  private final String maskTemplate;
  private final boolean[] editablePositions;
  private Font baseFont;

  private LocalDate lastValidDate;
  private DateVetoPolicy vetoPolicy;
  private final List<Consumer<LocalDate>> listeners = new ArrayList<>();
  private boolean internalUpdate = false;

  /**
   * Creates a masked date field for the supplied pattern and locale.
   *
   * @param datePattern the date format pattern used for display and parsing
   * @param locale the locale used for parsing and formatting
   */
  public MaskedDateField(String datePattern, Locale locale) {
    this.datePattern = datePattern;
    this.locale = locale;
    this.formatter = DateTimeFormatter.ofPattern(datePattern, locale);
    this.maskTemplate = buildMaskTemplate(datePattern);
    this.editablePositions = buildEditablePositions(datePattern);
    this.baseFont = getFont().deriveFont(Font.PLAIN);

    setColumns(datePattern.length());
    setText(maskTemplate);

    PlainDocument doc = (PlainDocument) getDocument();
    doc.setDocumentFilter(new MaskDocumentFilter());

    addKeyListener(new MaskKeyListener());
    addFocusListener(
        new FocusAdapter() {
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

  /**
   * Returns the last valid date parsed or assigned to the field.
   *
   * @return the current valid date, or {@code null} if none is available
   */
  public LocalDate getDate() {
    return lastValidDate;
  }

  /**
   * Sets the field to the supplied date.
   *
   * @param date the date to display, or {@code null} to clear the field
   */
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

  /**
   * Returns the date pattern used by the field.
   *
   * @return the date pattern
   */
  public String getDatePattern() {
    return datePattern;
  }

  /**
   * Returns the mask template shown when the field is empty.
   *
   * @return the mask template
   */
  public String getMaskTemplate() {
    return maskTemplate;
  }

  /**
   * Indicates whether the current text is a complete, allowed date.
   *
   * @return {@code true} if the current text parses to an allowed date, otherwise {@code false}
   */
  public boolean isDateValid() {
    LocalDate parsed = parseText();
    return parsed != null && isDateAllowed(parsed);
  }

  /**
   * Applies a veto policy used to reject disallowed dates.
   *
   * @param policy the veto policy to apply, or {@code null} to allow all dates
   */
  public void setVetoPolicy(DateVetoPolicy policy) {
    this.vetoPolicy = policy;
    updateVisualState();
  }

  /**
   * Registers a listener that is notified when a new valid date is entered.
   *
   * @param listener the listener to add
   */
  public void addListener(Consumer<LocalDate> listener) {
    listeners.add(listener);
  }

  /**
   * Removes a previously registered listener.
   *
   * @param listener the listener to remove
   */
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
      setFont(baseFont);
      return;
    }

    LocalDate parsed = parseText();
    if (parsed == null) {
      setForeground(INVALID_COLOR);
      setBackground(Color.WHITE);
      setFont(baseFont);
    } else if (!isDateAllowed(parsed)) {
      setForeground(NORMAL_COLOR);
      setBackground(Color.WHITE);
      Map<TextAttribute, Object> attrs = new HashMap<>(baseFont.getAttributes());
      attrs.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
      setFont(baseFont.deriveFont(attrs));
    } else {
      setForeground(NORMAL_COLOR);
      setBackground(Color.WHITE);
      setFont(baseFont);
    }
  }

  @Override
  public void setFont(Font font) {
    super.setFont(font);
    if (font != null
        && !TextAttribute.STRIKETHROUGH_ON.equals(
            font.getAttributes().get(TextAttribute.STRIKETHROUGH))) {
      baseFont = font.deriveFont(Font.PLAIN);
    }
  }

  private void onTextChanged() {
    if (internalUpdate) {
      return;
    }
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

  private boolean isEditablePosition(int pos) {
    return pos >= 0 && pos < editablePositions.length && editablePositions[pos];
  }

  private int findNextEditablePosition(int pos) {
    int next = Math.max(0, pos);
    while (next < editablePositions.length && !editablePositions[next]) {
      next++;
    }
    return Math.min(next, editablePositions.length);
  }

  private int findPreviousEditablePosition(int pos) {
    int prev = Math.min(pos, editablePositions.length - 1);
    while (prev >= 0 && !editablePositions[prev]) {
      prev--;
    }
    return prev;
  }

  private int findNextSectionStart(int pos) {
    int next = Math.max(0, pos);
    while (next < editablePositions.length && editablePositions[next]) {
      next++;
    }
    return findNextEditablePosition(next);
  }

  private boolean isSectionSeparatorKey(char typedChar) {
    return typedChar == '-' || typedChar == '/' || typedChar == '.';
  }

  private void clearEditablePosition(int pos) {
    if (!isEditablePosition(pos)) {
      return;
    }

    internalUpdate = true;
    try {
      StringBuilder result = new StringBuilder(getText());
      result.setCharAt(pos, '_');
      setText(result.toString());
      updateVisualState();
    } finally {
      internalUpdate = false;
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

      StringBuilder result =
          new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
      int pos = offset;

      for (char c : text.toCharArray()) {
        if (!Character.isDigit(c)) {
          continue;
        }

        while (pos < editablePositions.length && !editablePositions[pos]) {
          pos++;
        }
        if (pos >= editablePositions.length) {
          break;
        }

        result.setCharAt(pos, c);
        pos++;
      }

      internalUpdate = true;
      try {
        fb.replace(0, fb.getDocument().getLength(), result.toString(), attrs);
      } finally {
        internalUpdate = false;
      }

      final int caretPos = findNextEditablePosition(pos);
      SwingUtilities.invokeLater(
          () -> {
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

      StringBuilder result =
          new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
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
      SwingUtilities.invokeLater(
          () -> {
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
    public void keyTyped(KeyEvent e) {
      char typedChar = e.getKeyChar();
      int pos = getCaretPosition();
      if (isSectionSeparatorKey(typedChar)) {
        int nextSectionStart = findNextSectionStart(pos);
        if (nextSectionStart > pos && nextSectionStart <= editablePositions.length) {
          setCaretPosition(nextSectionStart);
        }
        e.consume();
      }
    }

    @Override
    public void keyPressed(KeyEvent e) {
      int pos = getCaretPosition();
      if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
        int next = findNextEditablePosition(pos + 1);
        if (next <= editablePositions.length) {
          setCaretPosition(next);
          e.consume();
        }
      } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
        int prev = findPreviousEditablePosition(pos - 1);
        if (prev >= 0) {
          setCaretPosition(prev);
          e.consume();
        }
      } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
        int target = -1;
        if (isEditablePosition(pos) && getText().charAt(pos) != '_') {
          target = pos;
        } else {
          target = findPreviousEditablePosition(pos - 1);
        }
        if (target >= 0) {
          clearEditablePosition(target);
          setCaretPosition(target);
          onTextChanged();
        }
        e.consume();
      } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
        int target = isEditablePosition(pos) ? pos : findNextEditablePosition(pos);
        if (target >= 0 && target < editablePositions.length) {
          clearEditablePosition(target);
          setCaretPosition(target);
          onTextChanged();
        }
        e.consume();
      }
    }
  }
}
