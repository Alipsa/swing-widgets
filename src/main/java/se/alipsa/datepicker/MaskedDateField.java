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
import javax.swing.UIManager;
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
  private LocalDate rangeFrom;
  private LocalDate rangeTo;
  private final List<Consumer<LocalDate>> listeners = new ArrayList<>();
  private boolean internalUpdate = false;

  /**
   * Creates a masked date field for the supplied pattern and locale.
   *
   * <p>The pattern may only contain numeric year ({@code yy}, {@code yyyy}, {@code uu}, {@code
   * uuuu}), month ({@code MM}) and day ({@code dd}) fields, separators, and quoted literals such as
   * {@code 'г'}.
   *
   * @param datePattern the date format pattern used for display and parsing
   * @param locale the locale used for parsing and formatting
   * @throws IllegalArgumentException if the pattern contains fields that cannot be entered as
   *     fixed-width digits
   */
  public MaskedDateField(String datePattern, Locale locale) {
    this.datePattern = datePattern;
    this.locale = locale;
    this.formatter = DateTimeFormatter.ofPattern(datePattern, locale);
    StringBuilder mask = new StringBuilder();
    this.editablePositions = buildMask(datePattern, mask);
    this.maskTemplate = mask.toString();
    this.baseFont = getFont();

    setColumns(maskTemplate.length());
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

  /**
   * Builds the mask template into {@code mask} and returns which mask positions accept digits.
   * Quoted literals (with {@code ''} as an escaped quote) become fixed text, so the mask lines up
   * character by character with the formatter output.
   */
  private static boolean[] buildMask(String pattern, StringBuilder mask) {
    List<Boolean> editable = new ArrayList<>();
    boolean hasYear = false;
    boolean hasMonth = false;
    boolean hasDay = false;
    int i = 0;
    while (i < pattern.length()) {
      char c = pattern.charAt(i);
      if (c == '\'') {
        if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
          mask.append('\'');
          editable.add(false);
          i += 2;
          continue;
        }
        int end = i + 1;
        while (true) {
          if (end >= pattern.length()) {
            throw new IllegalArgumentException("Unterminated quote in date pattern: " + pattern);
          }
          char literal = pattern.charAt(end);
          if (literal == '\'') {
            if (end + 1 < pattern.length() && pattern.charAt(end + 1) == '\'') {
              mask.append('\'');
              editable.add(false);
              end += 2;
              continue;
            }
            break;
          }
          mask.append(literal);
          editable.add(false);
          end++;
        }
        i = end + 1;
      } else if (Character.isLetter(c)) {
        int end = i;
        while (end < pattern.length() && pattern.charAt(end) == c) {
          end++;
        }
        int count = end - i;
        if (!isSupportedField(c, count)) {
          throw new IllegalArgumentException(
              "Unsupported field '"
                  + pattern.substring(i, end)
                  + "' in date pattern '"
                  + pattern
                  + "'; only yy, yyyy, uu, uuuu, MM and dd are supported");
        }
        hasYear |= c == 'y' || c == 'u';
        hasMonth |= c == 'M';
        hasDay |= c == 'd';
        for (int k = 0; k < count; k++) {
          mask.append('_');
          editable.add(true);
        }
        i = end;
      } else if (c == '[' || c == ']') {
        throw new IllegalArgumentException(
            "Optional sections are not supported in date pattern: " + pattern);
      } else {
        mask.append(c);
        editable.add(false);
        i++;
      }
    }
    if (!hasYear || !hasMonth || !hasDay) {
      throw new IllegalArgumentException(
          "Date pattern must contain year, month and day fields: " + pattern);
    }
    boolean[] positions = new boolean[editable.size()];
    for (int k = 0; k < positions.length; k++) {
      positions[k] = editable.get(k);
    }
    return positions;
  }

  private static boolean isSupportedField(char letter, int count) {
    switch (letter) {
      case 'y':
      case 'u':
        return count == 2 || count == 4;
      case 'M':
      case 'd':
        return count == 2;
      default:
        return false;
    }
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
   * Restricts accepted dates to the inclusive range; {@code null} leaves that side open. Used by
   * {@link DatePicker} so typed out-of-range dates are rejected like vetoed ones.
   */
  void setDateRange(LocalDate from, LocalDate to) {
    this.rangeFrom = from;
    this.rangeTo = to;
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
    if ((rangeFrom != null && date.isBefore(rangeFrom))
        || (rangeTo != null && date.isAfter(rangeTo))) {
      return false;
    }
    return vetoPolicy == null || vetoPolicy.isDateAllowed(date);
  }

  private boolean hasUnfilledPositions(String text) {
    if (text == null) {
      return true;
    }
    int length = Math.min(text.length(), editablePositions.length);
    for (int i = 0; i < length; i++) {
      if (editablePositions[i] && text.charAt(i) == '_') {
        return true;
      }
    }
    return false;
  }

  private LocalDate parseText() {
    String text = getText();
    if (hasUnfilledPositions(text)) {
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
    if (hasUnfilledPositions(text)) {
      setForeground(normalForeground());
      setFont(baseFont);
      return;
    }

    LocalDate parsed = parseText();
    if (parsed == null) {
      setForeground(INVALID_COLOR);
      setFont(baseFont);
    } else if (!isDateAllowed(parsed)) {
      setForeground(normalForeground());
      Map<TextAttribute, Object> attrs = new HashMap<>(baseFont.getAttributes());
      attrs.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
      setFont(baseFont.deriveFont(attrs));
    } else {
      setForeground(normalForeground());
      setFont(baseFont);
    }
  }

  private static Color normalForeground() {
    Color color = UIManager.getColor("TextField.foreground");
    return color != null ? color : NORMAL_COLOR;
  }

  /**
   * Sets the font of the field. The supplied font (including its style) is kept as the base font; a
   * strikethrough is only added temporarily to mark disallowed dates.
   *
   * @param font the font to use
   */
  @Override
  public void setFont(Font font) {
    super.setFont(font);
    if (font != null
        && !TextAttribute.STRIKETHROUGH_ON.equals(
            font.getAttributes().get(TextAttribute.STRIKETHROUGH))) {
      baseFont = font;
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

  /**
   * Blanks the editable positions in the current selection.
   *
   * @return {@code true} if there was a selection to clear
   */
  private boolean clearSelectedPositions() {
    int start = getSelectionStart();
    int end = getSelectionEnd();
    if (start == end) {
      return false;
    }
    internalUpdate = true;
    try {
      StringBuilder result = new StringBuilder(getText());
      for (int i = start; i < end && i < result.length() && i < editablePositions.length; i++) {
        if (editablePositions[i]) {
          result.setCharAt(i, '_');
        }
      }
      setText(result.toString());
      updateVisualState();
    } finally {
      internalUpdate = false;
    }
    setCaretPosition(Math.min(start, getText().length()));
    onTextChanged();
    return true;
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

  private class MaskDocumentFilter extends DocumentFilter {

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
        throws BadLocationException {
      if (internalUpdate) {
        super.replace(fb, offset, length, text, attrs);
        return;
      }

      if (!isEditable()) {
        return;
      }

      if (text == null || text.isEmpty()) {
        if (length > 0) {
          remove(fb, offset, length);
        }
        return;
      }

      StringBuilder result =
          new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
      int limit = Math.min(editablePositions.length, result.length());
      // Replacing a selection first blanks it, so leftover digits cannot form a different date.
      for (int i = offset; i < offset + length && i < limit; i++) {
        if (editablePositions[i]) {
          result.setCharAt(i, '_');
        }
      }
      int pos = offset;

      for (char c : text.toCharArray()) {
        if (!Character.isDigit(c)) {
          continue;
        }

        while (pos < limit && !editablePositions[pos]) {
          pos++;
        }
        if (pos >= limit) {
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

      if (!isEditable()) {
        return;
      }

      StringBuilder result =
          new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
      int limit = Math.min(editablePositions.length, result.length());
      for (int i = offset; i < offset + length && i < limit; i++) {
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
      if (!isEditable()) {
        e.consume();
        return;
      }

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
      if (!isEditable()) {
        e.consume();
        return;
      }

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
      } else if ((e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE)
          && clearSelectedPositions()) {
        e.consume();
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
