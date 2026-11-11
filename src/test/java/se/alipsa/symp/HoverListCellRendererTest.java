package se.alipsa.symp;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import org.junit.jupiter.api.Test;

class HoverListCellRendererTest {

  @Test
  void testMouseMovedOverEmptyListDoesNotThrow() {
    JList<YearMonth> list = new JList<>(new DefaultListModel<>());
    MouseEvent moved = new MouseEvent(list, MouseEvent.MOUSE_MOVED, 0, 0, 5, 5, 0, false);

    assertDoesNotThrow(() -> new HoverListCellRenderer().getHandler(list).mouseMoved(moved));
  }

  @Test
  void testFormatterIsUsedForDateValues() {
    JList<YearMonth> list = new JList<>();
    HoverListCellRenderer renderer = HoverListCellRenderer.register(list);
    renderer.setFormatter(DateTimeFormatter.ofPattern("MMM yyyy", Locale.UK));

    JLabel label =
        (JLabel)
            renderer.getListCellRendererComponent(list, YearMonth.of(2026, 3), 0, false, false);

    assertEquals("Mar 2026", label.getText());
  }

  @Test
  void testWithoutFormatterValuesUseToString() {
    JList<YearMonth> list = new JList<>();
    JLabel label =
        (JLabel)
            new HoverListCellRenderer()
                .getListCellRendererComponent(list, YearMonth.of(2026, 3), 0, false, false);

    assertEquals("2026-03", label.getText());
  }

  @Test
  void testHoverColorIsDerivedFromTheListColors() {
    JList<YearMonth> list = new JList<>(new YearMonth[] {YearMonth.of(2026, 1)});
    list.setBackground(Color.BLACK);
    list.setSelectionBackground(Color.WHITE);
    list.setSize(100, 100);
    HoverListCellRenderer renderer = HoverListCellRenderer.register(list);
    MouseEvent moved = new MouseEvent(list, MouseEvent.MOUSE_MOVED, 0, 0, 2, 2, 0, false);
    renderer.getHandler(list).mouseMoved(moved);

    JLabel hovered =
        (JLabel)
            renderer.getListCellRendererComponent(list, YearMonth.of(2026, 1), 0, false, false);

    assertEquals(new Color(128, 128, 128), hovered.getBackground());
  }
}
