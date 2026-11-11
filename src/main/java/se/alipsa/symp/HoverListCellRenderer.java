package se.alipsa.symp;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/** List cell renderer that highlights the list item under the mouse cursor. */
public class HoverListCellRenderer extends DefaultListCellRenderer {

  private int hoverIndex = -1;
  private DateTimeFormatter formatter;

  /** Creates a hover-aware list cell renderer. */
  public HoverListCellRenderer() {}

  static HoverListCellRenderer register(JList<YearMonth> l) {
    HoverListCellRenderer renderer = new HoverListCellRenderer();
    l.setCellRenderer(renderer);
    l.addMouseListener(renderer.getHandler(l));
    l.addMouseMotionListener(renderer.getHandler(l));
    return renderer;
  }

  /** Sets the formatter used for date values; {@code null} renders values with toString(). */
  void setFormatter(DateTimeFormatter formatter) {
    this.formatter = formatter;
  }

  /**
   * Returns the component used to render a list cell with hover-aware background styling.
   *
   * @param list the list being rendered
   * @param value the cell value to render
   * @param index the list index being rendered
   * @param isSelected whether the cell is selected
   * @param cellHasFocus whether the cell currently has focus
   * @return the configured renderer component
   */
  @Override
  public Component getListCellRendererComponent(
      JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    Object display =
        formatter != null && value instanceof TemporalAccessor
            ? formatter.format((TemporalAccessor) value)
            : value;
    super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);

    if (!isSelected) {
      setBackground(
          index == hoverIndex
              ? blend(list.getBackground(), list.getSelectionBackground())
              : list.getBackground());
    }
    return this;
  }

  /** Halfway between the list background and its selection color, so it suits any theme. */
  private static Color blend(Color background, Color selection) {
    if (background == null || selection == null) {
      return selection != null ? selection : background;
    }
    return new Color(
        (background.getRed() + selection.getRed() + 1) / 2,
        (background.getGreen() + selection.getGreen() + 1) / 2,
        (background.getBlue() + selection.getBlue() + 1) / 2);
  }

  /**
   * Returns the mouse handler used to track hover state for the list.
   *
   * @param list the list to monitor
   * @return a mouse handler for the list
   */
  public MouseAdapter getHandler(JList<YearMonth> list) {
    return new HoverMouseHandler(list);
  }

  class HoverMouseHandler extends MouseAdapter {

    private final JList<YearMonth> list;

    /**
     * Creates a handler that updates hover state for the supplied list.
     *
     * @param list the list whose hover state should be tracked
     */
    public HoverMouseHandler(JList<YearMonth> list) {
      this.list = list;
    }

    /**
     * Clears the hover state when the mouse leaves the list.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseExited(MouseEvent e) {
      setHoverIndex(-1);
    }

    /**
     * Updates the hover state to match the item currently under the cursor.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseMoved(MouseEvent e) {
      int index = list.locationToIndex(e.getPoint());
      Rectangle bounds = index >= 0 ? list.getCellBounds(index, index) : null;
      setHoverIndex(bounds != null && bounds.contains(e.getPoint()) ? index : -1);
    }

    private void setHoverIndex(int index) {
      if (hoverIndex == index) {
        return;
      }
      hoverIndex = index;
      list.repaint();
    }
  }
}
